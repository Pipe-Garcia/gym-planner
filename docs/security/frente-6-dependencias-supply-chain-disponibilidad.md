# Frente 6 — Dependencias, supply chain y disponibilidad

> Auditoría de seguridad de Gym Planner, previa a la carga de datos reales.
> Documento de cierre del **sexto y último** frente, y consolidación de la auditoría completa.

| | |
|---|---|
| **Frente** | 6 — Dependencias, supply chain y disponibilidad |
| **Estado** | CERRADO |
| **Veredicto** | Sin brechas activas. **Con remediaciones aplicadas** (primer frente con cambios de código desde F3) |
| **Método** | Paso 0 con ejecución autorizada de herramientas de análisis + remediaciones en tres tandas (F6-01, F6-02, F6-03) + regresión completa |
| **Frentes previos** | 1, 2, 3, 4 y 5 — cerrados y mergeados |
| **Consecuencia** | Cierra la **auditoría mínima pre-go-live**. No autoriza por sí sola la carga de datos reales |

---

## 1. Resumen ejecutivo

Este frente audita lo que **no es código propio**: dependencias de terceros, la cadena de build, y la capacidad del sistema de seguir funcionando bajo carga o ante fallas.

Es el único frente en el que el Paso 0 requirió **ejecutar herramientas** en lugar de solo leer. El motivo es el mismo que justificó el Paso 0-bis del Frente 5: la pregunta no se puede contestar leyendo un archivo. El `pom.xml` declara unas veinte dependencias directas; el árbol resuelto tiene ~155 artefactos, y las versiones efectivas las decide Maven en tiempo de resolución.

También es el primer frente desde el 3 que **terminó en cambios de código**. Se encontraron y corrigieron vulnerabilidades reales:

- **Backend:** driver JDBC de PostgreSQL en versión afectada → actualizado.
- **Frontend:** Axios, React Router, Vite, PostCSS y varias transitivas en versiones afectadas → actualizadas.
- **CI:** `GITHUB_TOKEN` con permisos implícitos, acciones de terceros pinneadas por tag mutable, credenciales de Git persistidas innecesariamente → endurecido.

Resultado final:

- **0 brechas activas.**
- **0 CVEs con explotabilidad demostrada (Nivel 3) pendientes.**
- **0 alertas de Dependabot abiertas** (68 cerradas).
- **353 tests verdes**, incluidas las suites de regresión de seguridad de F1–F3.
- **1 defensa ausente** que no bloquea el cierre del frente pero **sí bloquea la carga de datos reales**: no existe un mecanismo de backup y restore implementado y probado (sección 13).

---

## 2. Criterio y método

### 2.1 Criterio de clasificación

Se mantuvo el vigente desde el Frente 4: BRECHA ACTIVA / DEFENSA AUSENTE → arreglar antes de producción; HARDENING / RIESGO ACEPTADO / DEUDA → documentar con trigger y diferir; NO BUG → cerrar.

Con una regla adicional propia de este frente:

> **Un CVE en una dependencia no es una vulnerabilidad de Gym Planner.**

Para cada CVE relevante se aplicaron los mismos tres niveles que se usaron para SSRF en el Frente 4:

1. **Capacidad** — la librería afectada está en el árbol, en una versión afectada.
2. **Alcanzabilidad** — Gym Planner usa la funcionalidad afectada, directamente o a través de un componente del framework que sí activamos.
3. **Explotabilidad** — existe un input controlable que alcanza ese camino, y con qué exposición.

Un CVE que solo cumple (1) es **deuda de actualización**, no vulnerabilidad. El CVSS publicado corresponde al proyecto upstream, no a este sistema.

Esta disciplina fue determinante: de 64 alertas iniciales en el frontend, ninguna alcanzó Nivel 3 demostrado. Se actualizaron igual —eran actualizaciones de bajo riesgo, sin cambio de major— pero la clasificación honesta es "deuda de actualización resuelta", no "vulnerabilidades críticas corregidas".

### 2.2 Límites de la ejecución autorizada

Se autorizó ejecutar: `mvn dependency:tree`, `dependency:analyze`, invocación directa del plugin de OWASP dependency-check, `npm ci` / `npm audit`, comandos de git de solo lectura, y la suite de tests.

Se mantuvo prohibido: editar archivos durante el Paso 0, hacer bumps durante el diagnóstico, regenerar lockfiles, y cualquier comando que moviera el árbol de git. Las remediaciones se hicieron **después** del diagnóstico, en tandas separadas y con validación entre cada una.

---

## 3. Inventario de dependencias

### 3.1 Backend

Árbol Maven resuelto: **~155 artefactos**.

| Componente | Versión resuelta |
|---|---|
| Spring Boot | 3.5.13 |
| Spring Framework | 6.2.17 |
| Spring Security | 6.5.9 |
| Tomcat Embedded | 10.1.53 |
| Hibernate Core | 6.6.45.Final |
| Jackson Databind | 2.21.2 |
| SnakeYAML | 2.4 |
| **PostgreSQL JDBC** | **42.7.13** *(tras remediación F6-01)* |
| HikariCP | 6.3.3 |
| Flyway | 10.22.0 |
| JJWT | 0.12.7 |
| Bucket4j | 8.19.0 |
| Caffeine | 3.2.3 |
| Flying Saucer | 9.4.0 |
| OpenPDF | 1.3.35 |
| Thymeleaf | 3.1.3.RELEASE |
| springdoc-openapi | 2.8.17 |
| MapStruct | 1.6.3 |
| Lombok | 1.18.38 |
| Logback | 1.5.32 |
| SLF4J | 2.0.17 |

Las dependencias de test —H2, Testcontainers, PDFBox, spring-security-test— tienen scope `test` y **no forman parte del artefacto desplegado**.

**springdoc permanece en el jar de producción.** No se removió. Sigue neutralizado por el perfil `prod`, condición verificada en runtime durante el Frente 5. Se mantiene como decisión consciente: quitarlo obligaría a un perfil de build distinto entre dev y prod, lo que introduce su propia clase de riesgo (que el artefacto probado no sea el artefacto desplegado).

### 3.2 Frontend

Gestor: **npm**, con `package-lock.json` trackeado.

Runtime, tras remediaciones:

| Dependencia | Versión |
|---|---|
| React / React DOM | 19.1.1 |
| React Router / DOM | 7.18.2 |
| TanStack Query | 5.85.5 |
| Axios | 1.19.0 |
| Zod | 4.1.5 |
| React Hook Form | 7.62.0 |

Tooling: Vite 7.3.6 · TypeScript 5.9.3 · PostCSS 8.5.26 · Tailwind 3.4.17.

---

## 4. Escaneo SCA — qué corrió y qué no

**Esta sección es una limitación del frente, no un resultado, y conviene leerla como tal.**

### 4.1 Backend — el escáner general no corrió

Se intentó OWASP dependency-check por invocación directa del plugin. Falló:

```text
NvdApiException: Invalid API Key
NoDataException: No documents exist
BUILD FAILURE
```

Es un modo de falla conocido: desde que la NVD exige API key, la primera carga de la base falla o se rate-limitea sin ella.

**No se inventaron ni estimaron resultados.** Se dejó registrado como NO EJECUTABLE.

### 4.2 Con qué se compensó

- Revisión manual dirigida sobre las dependencias de mayor superficie (driver JDBC, pipeline de PDF, Thymeleaf, Tomcat, Jackson).
- Advisories oficiales sobre las versiones inventariadas en 3.1.
- Dependabot.

Esa combinación encontró el hallazgo real de backend (sección 5), lo que valida el enfoque pero **no lo equipara a un escaneo completo**.

### 4.3 El hueco que queda — y la acción concreta

Las 68 alertas cerradas de Dependabot correspondían al ecosistema **npm**. Si Dependabot no tiene habilitado también el ecosistema **Maven** sobre `backend/pom.xml`, entonces:

> **El backend queda sin cobertura continua de vulnerabilidades.** El único CVE de backend de esta auditoría se encontró por revisión manual dirigida, no por herramienta. Una revisión manual detecta lo que se busca; no detecta lo que aparece la semana que viene.

Verificación concreta, de dos minutos, registrada como gate **GL-11**: confirmar en Settings → Code security que Dependabot alerts cubre el ecosistema Maven, y que aparecen alertas —o cero alertas— sobre las dependencias del backend, no solo sobre las de npm.

Alternativa más robusta, si se quiere resolver de raíz: agregar dependency-check al CI con una API key de NVD en secrets, o incorporar OSV-Scanner, que no depende de la NVD. Es **deuda de proceso**, no bloqueante, y se registra como residual 15.7.

---

## 5. F6-01 — Driver JDBC de PostgreSQL

### 5.1 Hallazgo

Versión inicialmente resuelta: `org.postgresql:postgresql:42.7.10`, heredada del dependency management de Spring Boot 3.5.13.

Advisory identificado: **GHSA-j92g-9f8w-j867 / CVE-2026-54291**. Rango afectado: `>= 42.7.4 y < 42.7.12`.

Análisis por niveles:

| Nivel | Resultado |
|---|---|
| 1 — Capacidad | **SÍ.** Versión dentro del rango afectado. |
| 2 — Alcanzabilidad | **SÍ.** La conexión productiva a Neon opera con `sslmode=require` y `channel_binding=require`, condición verificada en runtime durante el Frente 5. |
| 3 — Explotabilidad | **No demostrada.** Requeriría posición de red entre Render y Neon. |

**Remediación requerida antes del go-live**, pese a no alcanzar Nivel 3: la defensa afectada es precisamente la que protege el canal por el que viajan los datos de salud, y la corrección es de costo casi nulo.

> **Nota de trazabilidad.** Este advisory es posterior al corte de conocimiento del asistente que redactó este documento, por lo que sus detalles técnicos se registran **tal como fueron reportados** por Codex y Dependabot, sin verificación independiente. La decisión de remediar no depende de esa verificación: actualizar dentro de la misma línea de parches es correcto en cualquier escenario.

### 5.2 Verificación de que no se reabre el Frente 4

El Frente 4 concluyó NO BUG en inyección SQL apoyándose en que todo está parametrizado. Existe una familia distinta de CVEs de pgJDBC (la más conocida, **CVE-2024-1597**) en la que la parametrización deja de proteger si la conexión opera en *simple query mode*. Se verificó explícitamente:

- CVE-2024-1597 ya estaba corregido en la versión en uso.
- `preferQueryMode` **no aparece** en ningún punto del repositorio: ni en `application*.yml`, ni en la connection string, ni en `docker-compose.yml`, ni en `.env.example`. Rige el default del driver, que es `extended`.

**La conclusión del Frente 4 se mantiene íntegra.**

### 5.3 Remediación aplicada

Override idiomático de la propiedad del BOM, sin tocar el resto del stack:

```xml
<postgresql.version>42.7.13</postgresql.version>
```

Resultado: `org.postgresql:postgresql:jar:42.7.13:runtime`, versión única en el árbol.

**No se modificaron:** Spring Boot, Hibernate, Flyway, HikariCP, `DB_URL`, `sslmode`, `channel_binding` ni ninguna migración.

Validación: `353 tests · 0 failures · 0 errors · 0 skipped · BUILD SUCCESS`.

**Clasificación: REMEDIADO.**

---

## 6. F6-02A — Axios y React Router

Estado inicial: Axios 1.11.0 · React Router (y DOM) 7.8.2.

Antes de actualizar se analizó la **alcanzabilidad real** de las advisories dentro de este proyecto:

**Axios** se usa exclusivamente como cliente en el navegador: `baseURL`, interceptor de JWT, manejo de 401, requests JSON. **No se encontraron**: proxy Node, adapter HTTP explícito, `maxRedirects` custom, agentes Node, `FormData`, ni configuración de Axios controlable por el usuario.

**React Router** se usa como SPA declarativa: `BrowserRouter`, `Routes`, `Route`, navegación cliente, rutas internas. **No se encontraron**: SSR, RSC, loaders, actions, `ServerRouter`, `createStaticHandler`, ni redirects externos controlados por el usuario.

**Ninguna advisory alcanzó Nivel 3.** Se actualizó igual porque el costo era bajo y no había cambio de major.

Aplicado: Axios 1.11.0 → **1.19.0** · React Router y DOM 7.8.2 → **7.18.2** · `form-data` corregido transitivamente.

Validación: `npm ci` · `typecheck` · `lint` · `build` → todos PASS. Sin cambios funcionales.

Efecto: **Dependabot 64 → 20 alertas.**

**Clasificación: REMEDIADO.**

---

## 7. F6-02B — Tooling de build del frontend

Las 20 alertas restantes correspondían a herramientas de desarrollo y build: Vite, PostCSS, Babel, js-yaml, brace-expansion, nanoid, launch-editor.

**Estas dependencias no forman parte del artefacto servido.** El frontend desplegado es estático: HTML, CSS y JS compilados. Vite no corre en producción. Ninguna alcanzó Nivel 3.

Aplicado: Vite 7.1.4 → **7.3.6** · PostCSS 8.5.6 → **8.5.26**, más transitivas (`@babel/core` 7.29.7, `js-yaml` 4.3.1, `brace-expansion` 1.1.18 / 2.1.4, `nanoid` 3.3.18) y el subárbol necesario de esbuild.

No se requirió migrar a Vite 8. Sin cambios de major ni de código.

Validación local: `npm ci` → 0 vulnerabilities · `npm audit` → 0 vulnerabilities · typecheck / lint / build → PASS · `npm run dev` levanta correctamente.

Dependabot tras push y rescan: **0 abiertas, 68 cerradas.**

**Clasificación: REMEDIADO.**

---

## 8. F6-03 — Supply chain y CI

### 8.1 Estado inicial

Dos workflows: `backend-ci.yml` y `frontend-ci.yml`, disparados por `push` y `pull_request` sobre `main` y `develop`.

**No se encontraron** los patrones de riesgo alto: `pull_request_target`, `workflow_run`, ejecución de PRs con contexto privilegiado, secrets en los workflows, impresión de variables sensibles, releases, deployments, pushes, publicación de paquetes ni registries alternativos.

El frontend ya usaba `npm ci` (instalación desde lockfile). El backend, Maven Wrapper.

### 8.2 Permisos del `GITHUB_TOKEN`

Ningún workflow declaraba bloque `permissions:`, de modo que el permiso efectivo dependía de la configuración por defecto del repositorio —que históricamente es amplia y puede incluir escritura.

Aplicado en ambos workflows:

```yaml
permissions:
  contents: read
```

**Clasificación: HARDENING DE SUPPLY CHAIN — REMEDIADO.**

### 8.3 Pinning de acciones de terceros

Estado inicial: `actions/checkout@v4`, `actions/setup-java@v4`, `actions/setup-node@v4` — **tags mutables**. Un tag puede reapuntarse a otro commit sin que el consumidor se entere; es el vector clásico de compromiso de acciones de terceros.

Aplicado — pinning por SHA completo, verificado contra los releases oficiales:

```text
actions/checkout    11d5960a326750d5838078e36cf38b85af677262  # v4.4.0
actions/setup-java  cf277c60eb25467037889841efdb72551f06f6c3  # v4.9.1
actions/setup-node  49933ea5288caeca8642d1e84afbd3f7d6820020  # v4.4.0
```

Se agregó además `persist-credentials: false` en el checkout, ya que ningún step posterior necesita credenciales de Git en el runner.

**Clasificación: HARDENING DE SUPPLY CHAIN — REMEDIADO.**

**Nota de mantenimiento:** el pinning por SHA congela la versión. Sin Dependabot configurado también para `github-actions`, las acciones quedan sin actualizar indefinidamente, incluidos sus parches de seguridad. Se registra en el gate GL-11.

### 8.4 Validación en CI real

Tras el push, con `contents: read`, acciones pinneadas y `persist-credentials: false`:

```text
backend-ci   PASS
frontend-ci  PASS
```

El endurecimiento no rompió la cadena.

---

## 9. Pipeline de PDF — cierre de pendientes de F4 y F5

Versiones: Flying Saucer **9.4.0** · OpenPDF **1.3.35** · Thymeleaf **3.1.3.RELEASE**.

**DOCTYPE — pendiente derivado del Frente 5.** `routine.html` comienza con `<!DOCTYPE html>`. **No declara DTD externa.** Queda descartada la resolución de DTD por red durante el parseo del XHTML: ni request saliente en tiempo de render, ni superficie de XXE por esa vía.

**Configuración del renderer.** Se usa `setDocumentFromString(xhtml)` sin base URI. **No se encontraron**: parser XML custom, `UserAgentCallback`, `ReplacedElementFactory`, `EntityResolver` ni resource loader remoto.

**Conclusiones del Frente 4 revalidadas contra las versiones actuales:** sin `th:utext`, sin segundo procesamiento de plantillas, sin `logoUrl` dentro del XHTML, sin fetch server-side de URLs controladas, sin pipeline de recursos externos.

**Clasificación: NO BUG. Sin nueva ruta explotable demostrada.**

---

## 10. Disponibilidad y resiliencia

### 10.1 Generación de PDF

La generación es **síncrona, en el thread del request**: construye el XHTML completo como String, lo renderiza sobre un `ByteArrayOutputStream` y convierte el buffer a `byte[]`. Los streams se gestionan con try-with-resources (liberación correcta ante excepción).

**No existe** timeout específico, límite de tamaño funcional de la rutina antes del render, ni rate limiting sobre el endpoint. El rate limiting introducido en el Frente 3 protege **específicamente el login**, no los endpoints de PDF.

O sea: un usuario autenticado puede disparar generaciones concurrentes de PDF, cada una con el documento completo en memoria, sobre una instancia con RAM acotada.

**Clasificación: HARDENING / DEUDA DE DISPONIBILIDAD — exposición AUTENTICADA.** No es brecha activa en V1: el único actor con credenciales es el propietario del gimnasio, que no tiene incentivo para tumbar su propio sistema.

**Trigger:** habilitación del rol `TRAINER` u otros actores · aumento de concurrencia · rutinas significativamente más grandes · evidencia real de consumo problemático de CPU o memoria.

### 10.2 Límites de payload

No se encontraron **máximos** sobre las colecciones anidadas principales (días, bloques, ejercicios, sets). Los validadores actuales imponen reglas funcionales y mínimos, no cotas superiores. Los services recorren todos los elementos y construyen las entidades correspondientes.

Cadena: usuario autenticado → payload artificialmente grande → consumo elevado de CPU, memoria y base.

**Clasificación: HARDENING — exposición AUTENTICADA.** Mismo razonamiento y mismo trigger que 10.1.

### 10.3 HikariCP, timeouts y shutdown

No existe configuración explícita de: `maximum-pool-size`, `minimum-idle`, `max-lifetime`, `connection-timeout`, `validation-timeout`, timeout de query, timeout de transacción, `server.shutdown=graceful` ni `spring.lifecycle.timeout-per-shutdown-phase`.

**Los defaults no se clasificaron como vulnerabilidad**, siguiendo el criterio de no inferir hallazgos.

Contexto operativo relevante: Neon Free con autosuspend a los 5 minutos y pooling de Neon desactivado. La combinación "arranque en frío de la base + timeouts por defecto" es exactamente el escenario donde estos valores dejan de ser indiferentes.

**Clasificación: DEUDA OPERATIVA / HARDENING.** A revisar con la infraestructura definitiva y con carga real, no antes: afinar un pool contra una demo Free no produce números trasladables.

### 10.4 Monitoreo

Dos superficies: `/api/public/ping`, que devuelve estado fijo y **no consulta la base**, y `/actuator/health`, validado en runtime durante el Frente 5 y que **sí** la toca vía `DataSourceHealthIndicator`.

Render sigue **sin Health Check Path configurado**. La elección entre ambos endpoints no es cosmética: uno mantiene la base despierta consumiendo compute de Neon, el otro no.

**Clasificación: DEUDA OPERATIVA / GATE DE GO-LIVE (GL-06).**

---

## 11. Enmienda al Frente 5 — rutas no mapeadas

El documento de cierre del Frente 5 (sección 7.6) afirmó que **"la aplicación devuelve un 500 genérico ante cualquier ruta no mapeada"**. Ese enunciado es **impreciso** y se corrige acá.

Comportamiento real, confirmado en el código:

- Los paths de springdoc están en `permitAll()`. Con springdoc deshabilitado en `prod`, no hay handler detrás → la excepción llega al `GlobalExceptionHandler`, que la traduce a **500 genérico**.
- Una ruta inexistente **no** cubierta por `permitAll()` es interceptada por `anyRequest().authenticated()` **antes** del routing → **401**, no 500.
- El 500 en los diez endpoints de Actuator observado durante el Frente 5 se explica porque esas pruebas se hicieron **con token válido**: al pasar la autenticación, el request llega al routing y encuentra el mismo camino que springdoc.

**Consecuencia práctica: el problema de observabilidad que F5 planteó es menor de lo que se documentó.** Un escaneo anónimo de paths —que es la fuente masiva de ruido— recibe 401, no 500. El volumen de 500s espurios es acotado.

La deuda subsiste, pero baja de prioridad. **Clasificación: DEUDA / ROBUSTEZ BAJA. No bloqueante.**

Se deja registrado que esta enmienda **no invalida ninguna conclusión de seguridad del Frente 5**: la ausencia de exposición de datos por parte de Actuator y de springdoc se verificó por contenido de las respuestas, no por su status code.

---

## 12. Dump histórico `backup_pre_v8.sql`

Se encontró en la historia de Git un archivo posteriormente eliminado, `backup_pre_v8.sql`, con datos en tablas `students`, `student_injuries`, `student_notes`, `routines` y `users`.

**Verificación manual: los datos eran íntegramente ficticios/sintéticos**, generados antes del deploy de la demo para poblar Neon, facilitar el setup local y compartir datos de prueba.

**Clasificación: NO BREACH**, con dos precisiones que conviene dejar escritas:

1. **La clasificación depende enteramente de esa verificación manual.** No es una propiedad del código: es un hecho constatado por quien conoce el origen de esos datos. Si en el futuro alguien revisa la historia del repo, encontrará un dump con `student_injuries` y `student_notes`, y sin esta nota no tendría cómo saber que era sintético.
2. **El archivo sigue en la historia de forma permanente.** Borrarlo en un commit posterior no lo elimina. Si hubiera contenido datos reales, la única remediación posible sería una reescritura de historia con rotación de todo lo expuesto — un procedimiento costoso y disruptivo.

**Verificación pendiente, barata:** el dump incluía la tabla `users`, o sea hashes BCrypt. Confirmar que corresponden a la credencial bootstrap ya neutralizada en **F3-1** (migración V14) y no a una contraseña que siga en uso. Si correspondieran a una contraseña vigente, ese hash está en la historia del repositorio y la contraseña debe rotarse.

**Regla operativa que queda establecida:** no versionar dumps de base en Git, ni siquiera sintéticos. Para datos de prueba compartidos, usar migraciones de seed con perfil de test o scripts que generen los datos, nunca volcados. El costo de la disciplina es cero; el costo del error es irreversible.

---

## 13. Backup y restore — defensa ausente

**No se encontró implementación alguna de:** workflow de backup programado, script de `pg_dump`, script de restore, cifrado de dumps, destino de almacenamiento externo, política de retención definida, ni procedimiento operativo de restore probado. El runbook no documenta un procedimiento completo.

El design doc contemplaba un `backup-weekly.yml`. **No se implementó.** Esto tiene una lectura positiva y una negativa:

- **Positiva:** no existe el riesgo que la sección F del Paso 0 buscaba descartar. No hay dumps de la base almacenados como artefactos de GitHub, donde su accesibilidad habría dependido de la visibilidad del repositorio y de la configuración de los releases. Un dump de esta base contiene datos de salud; que no exista ese mecanismo elimina esa superficie por completo.
- **Negativa:** **no hay recuperación.** El plan Free de Neon retiene ~6 horas de historia. Un borrado accidental, una migración destructiva o un incidente detectado un día después no tienen vuelta atrás.

**Clasificación: DEFENSA AUSENTE — GATE DE GO-LIVE (GL-09).**

Esto **no impide cerrar el Frente 6**: hoy no hay datos reales que perder, y la ausencia de backup no es una vulnerabilidad explotable por un atacante.

Esto **sí impide cargar datos reales.** Antes de que entre el primer alumno de Sergio Carrión Gym debe existir: política de retención adecuada al plan contratado, mecanismo de recuperación, procedimiento documentado en el runbook, y **un restore efectivamente probado al menos una vez**. Un backup no verificado no es un backup.

No se prescribe la solución. Neon Launch ofrece PITR de 7 días, que probablemente cubra el requisito sin necesidad de un workflow propio de `pg_dump`. La decisión se toma junto con la contratación del plan definitivo. **Si se opta por dumps propios, la decisión de dónde almacenarlos vuelve a abrir la superficie descrita arriba y debe tratarse con ese peso.**

---

## 14. Licencias — riesgo comercial

Gym Planner se entrega a un cliente como producto comercial, de modo que una licencia copyleft fuerte en el árbol crearía obligaciones legales reales.

**No se encontraron dependencias con AGPL, GPL fuerte ni SSPL.**

En particular, se confirmó que el motor de PDF es **OpenPDF 1.3.35** (MPL-2.0 / LGPL-2.1+) y **no** iText 5+ o iText 7, que son AGPL e incompatibles con software comercial cerrado sin licencia paga. Este era el riesgo puntual que la sección G del Paso 0 buscaba descartar, y quedó descartado.

Copyleft débil presente:

| Dependencia | Licencia |
|---|---|
| Hibernate Core | LGPL-2.1+ |
| Flying Saucer | LGPL-2.1+ |
| OpenPDF | MPL-2.0 / LGPL-2.1+ |

**Consideración para revisión legal, no resuelta acá:** la LGPL es compatible con uso comercial siempre que la librería no se modifique y se preserve la capacidad del usuario de reemplazarla. El empaquetado como fat jar de Spring Boot es la práctica habitual del ecosistema y generalmente se considera aceptable, pero es una interpretación jurídica, no técnica, y excede el alcance de una auditoría de seguridad.

El repositorio **no contiene** `LICENSE`, `NOTICE` ni `COPYING`. Antes de la entrega formal conviene definir bajo qué términos se entrega el software al cliente —lo que además se cruza con la discusión pendiente sobre licencia versus venta y exclusividad.

**Clasificación: REVISIÓN LEGAL / COMERCIAL. No es hallazgo de seguridad.**

---

## 15. Build, wrapper y residuales del frente

### 15.1 Docker

Imágenes base referenciadas por **tag**, no por digest: `maven:3.9-eclipse-temurin-21` y `eclipse-temurin:21-jre`.

Es un **trade-off, no un defecto**: el tag flotante incorpora automáticamente los parches de la imagen base en cada rebuild, a costa de que el build no sea reproducible bit a bit. Para un proyecto de este tamaño y con rebuilds frecuentes, priorizar los parches es defendible.

El stage final recibe únicamente el jar. No se copian código fuente, `.m2`, tests ni `.env`. No hay ARG ni ENV con secretos.

**Clasificación: RIESGO ACEPTADO / trade-off consciente.**

Persiste el residual del Frente 5: contenedor de runtime **sin directiva `USER`** (corre como root). HARDENING BAJO, no bloqueante.

### 15.2 Maven Wrapper

El CI de backend usa el wrapper, con distribución declarada por HTTPS fijando Maven 3.9.11. **No se declaró checksum del wrapper** (`distributionSha256Sum`).

**Clasificación: HARDENING DE SUPPLY CHAIN RESIDUAL.** No bloqueante: la descarga es por HTTPS desde el origen oficial. El checksum agrega defensa contra un compromiso del origen o del transporte.

---

## 16. Regresión final

`./mvnw clean verify` con Docker y Testcontainers disponibles:

```text
Tests run: 353 · Failures: 0 · Errors: 0 · Skipped: 0 · BUILD SUCCESS
```

Suites de regresión de seguridad verdes: aislamiento multi-tenant (F1) · no exposición de datos sensibles (F2) · auth y JWT, rate limiting, fuente de client IP (F3) · PostgreSQL vía Testcontainers · PDF · texto de WhatsApp · manejo seguro de excepciones.

Frontend: `npm audit` 0 vulnerabilities · typecheck PASS · lint PASS · build PASS.

CI real sobre `main`: `backend-ci` PASS · `frontend-ci` PASS.

**Las remediaciones de este frente no introdujeron regresiones funcionales ni de seguridad.**

---

## 17. Remediaciones aplicadas en F6

| ID | Hallazgo | Antes | Después |
|---|---|---|---|
| F6-01 | Driver pgJDBC en versión afectada | 42.7.10 | **42.7.13** |
| F6-02A | Axios en versión afectada | 1.11.0 | **1.19.0** |
| F6-02A | React Router / DOM en versión afectada | 7.8.2 | **7.18.2** |
| F6-02B | Vite en versión afectada | 7.1.4 | **7.3.6** |
| F6-02B | PostCSS en versión afectada | 8.5.6 | **8.5.26** |
| F6-02B | Transitivas de build afectadas | varias | corregidas |
| F6-03 | `GITHUB_TOKEN` con permisos implícitos | default del repo | `contents: read` |
| F6-03 | Acciones de terceros por tag mutable | `@v4` | SHA completo |
| F6-03 | Credenciales de Git persistidas en runner | default | `persist-credentials: false` |
| — | Alertas de Dependabot | 68 históricas | **0 abiertas** |

---

## 18. Residuales del Frente 6

| # | Ítem | Clasificación | Trigger |
|---|---|---|---|
| 15.1 | PDF síncrono sin límite ni rate limiting | HARDENING (autenticada) | Nuevo actor (TRAINER) · mayor concurrencia · evidencia de consumo |
| 15.2 | Colecciones anidadas sin máximos | HARDENING (autenticada) | Nuevo actor · abuso · crecimiento de payloads |
| 15.3 | Hikari y timeouts sin configurar | DEUDA OPERATIVA | Infraestructura definitiva + carga real |
| 15.4 | Graceful shutdown sin configurar | HARDENING | Producción / disponibilidad |
| 15.5 | Maven Wrapper sin checksum | HARDENING SUPPLY CHAIN | Endurecimiento futuro |
| 15.6 | Imágenes Docker por tag, sin digest | RIESGO ACEPTADO | Cambio de estrategia de build |
| 15.7 | Sin SCA automatizado del backend | DEUDA DE PROCESO | Confirmar cobertura Maven de Dependabot (GL-11) |
| 15.8 | Rutas no mapeadas → 500 (alcance acotado) | DEUDA / ROBUSTEZ BAJA | Mejora de semántica de errores |
| 15.9 | Contenedor de runtime sin `USER` | HARDENING BAJO | Agrupar hardenings baratos |
| 15.10 | Sin `LICENSE` en el repositorio | REVISIÓN COMERCIAL | Antes de la entrega formal |
| 15.11 | Backup y restore inexistentes | **DEFENSA AUSENTE** | **Antes de datos reales (GL-09)** |

Salvo el gate de backup/restore, ninguno constituye una brecha activa demostrada.

---

## 19. Deuda consolidada de la auditoría (F1–F6)

Inventario completo de lo diferido en los seis frentes. Es el insumo para futuras revisiones: cada ítem tiene su condición explícita de reactivación.

| Origen | Ítem | Clasificación | Trigger |
|---|---|---|---|
| F1 | Enforcement multi-tenant AD-HOC (sin repositorio base, RLS ni `@Filter`) | DEUDA ARQUITECTÓNICA | Fase multi-tenant formal · incorporación de nuevos servicios |
| F1 | Query sin scope de gym sostenida por tests sobre H2 | DEUDA | Migración de esos tests a Postgres real |
| F3 | `Retry-After` no expuesto en CORS | FUNCIONAL | Si el frontend necesita mostrar el tiempo de espera |
| F4 | `logoUrl` inerte dentro de `PdfGymDto` | DEUDA MENOR + **decisión pre-tomada** | Al implementar el logo en el PDF: no resolver URL remota desde Flying Saucer |
| F4 | `sort` sin allowlist de propiedades | HARDENING BAJO | Campos sensibles no expuestos en DTOs · 500s por sort inválido en producción |
| F4 | Schemes no restringidos en `imageUrl` / `videoUrl` | HARDENING BAJO | Habilitación del rol `TRAINER` u otro actor que consuma contenido ajeno |
| F5 | `.env.production` fuera de `.gitignore` | HARDENING BAJO | Agrupación de hardenings baratos |
| F5 | Contenedor sin `USER` | HARDENING BAJO | Agrupación de hardenings baratos |
| F5 | Aplicación con rol owner de la base | HARDENING / mínimo privilegio | Migración a Neon Launch · hallazgo con SQL dinámico |
| F5 | `console.error` con objetos completos en frontend | HARDENING BAJO | **Incorporación de observabilidad remota** (Sentry, etc.) |
| F5 | Anti-framing y CSP del frontend | HARDENING MEDIO | **Gate GL-04** |
| F5 | Neon sin IP restrictions ni VPC | HARDENING MEDIO | **Gate GL-05** |
| F5 | Health Check Path sin configurar | OPERATIVO | **Gate GL-06** |
| F6 | PDF sin límites ni rate limiting | HARDENING | Nuevo actor · concurrencia |
| F6 | Colecciones anidadas sin máximos | HARDENING | Nuevo actor · abuso |
| F6 | Hikari, timeouts, graceful shutdown | DEUDA OPERATIVA | Infraestructura definitiva |
| F6 | Maven Wrapper sin checksum | HARDENING | Endurecimiento futuro |
| F6 | Sin SCA automatizado del backend | DEUDA DE PROCESO | **Gate GL-11** |
| F6 | Sin `LICENSE` | COMERCIAL | Entrega formal |
| F6 | Backup y restore | **DEFENSA AUSENTE** | **Gate GL-09 — bloquea datos reales** |

**Un patrón que conviene notar:** varios triggers apuntan al mismo evento — la **habilitación del rol `TRAINER`**. Cuando eso ocurra, hay que revisar en bloque los residuales de F4 (schemes de URL) y F6 (PDF y payloads sin límites), porque todos dependen de que hoy exista un solo actor confiable. No es una revisión frente por frente: es una revalidación del modelo de amenazas.

---

## 20. Go-Live Security Check consolidado

Los gates definidos en el Frente 5 se mantienen con su numeración original, más dos que aporta este frente. **El Go-Live Security Check no reaudita**: verifica invariantes ya establecidas sobre la infraestructura definitiva.

| ID | Gate | Criterio de aprobación |
|---|---|---|
| **GL-01** | Perfil de producción activo | `SPRING_PROFILES_ACTIVE=prod` verificado. Decidir si se agrega fail-fast explícito ante ausencia de la variable |
| **GL-02** | Actuator sensible con usuario autenticado | Ningún endpoint devuelve payload de Actuator. 404 o 500 genérico son válidos; **200 con contenido es bloqueante** |
| **GL-03** | Cadena de proxy y `CF-Connecting-IP` | Si se agrega Cloudflare propio delante de Render, **reproducir la prueba runtime de F3-3.1**: XFF falsificado no evade, y dos clientes distintos obtienen buckets distintos |
| **GL-04** | Headers del frontend definitivo | Anti-framing (`frame-ancestors 'none'` o `X-Frame-Options: DENY`), CSP, `X-Content-Type-Options`, `Referrer-Policy` |
| **GL-05** | Acceso de red a Neon | IP allowlist o VPC. Si no se implementa, **aceptación explícita del riesgo por escrito** |
| **GL-06** | Health Check Path en Render | Configurado, con decisión consciente sobre el consumo de compute de Neon |
| **GL-07** | CORS con el dominio definitivo | Actualizado y **verificado con preflight real**: origen legítimo permitido, origen falso rechazado |
| **GL-08** | `VITE_API_URL` | Apunta al backend correcto; el bundle no contiene ninguna otra variable |
| **GL-09** | **Backup, restore y retención** | Retención acorde al plan · procedimiento documentado en el runbook · **restore efectivamente probado**. Bloqueante duro |
| **GL-10** | Reverificación de invariantes | Swagger no expuesto · `/actuator/health` mínimo · TLS y channel binding hacia Neon **con el driver 42.7.13** · headers del backend · JWT secret real y externo |
| **GL-11** | Cobertura de escaneo de dependencias | Dependabot activo sobre **Maven, npm y github-actions** · cero alertas abiertas relevantes · CI de `main` en verde |
| **GL-12** | Términos de entrega | `LICENSE` definido y revisión de las obligaciones LGPL antes de la entrega formal *(comercial, no de seguridad)* |

---

## 21. Limitaciones de la auditoría completa

Registradas para que quien lea estos seis documentos sepa qué **no** garantizan:

1. **No fue un pentest.** Ningún frente ejecutó explotación real contra la aplicación. Las conclusiones surgen de reconstrucción de flujos de datos con evidencia de archivo y línea, más verificaciones puntuales en runtime.
2. **El backend no tuvo escaneo SCA general.** El escáner de CVEs nunca corrió (sección 4). El hallazgo de backend se encontró por revisión manual dirigida. Ver GL-11.
3. **El alcance fueron seis frentes definidos de antemano.** No se auditaron: lógica de negocio más allá de sus implicancias de seguridad, corrección funcional, accesibilidad, ni cumplimiento normativo de protección de datos de salud —que, tratándose de un sistema que almacena lesiones de alumnos en Argentina, es una consulta que conviene hacer por separado.
4. **No hubo pruebas de carga.** Los hallazgos de disponibilidad de la sección 10 son análisis estructural, no medición.
5. **La infraestructura verificada es la de demo.** Render Free, Neon Free y Vercel Hobby serán reemplazados. De ahí el Go-Live Security Check.
6. **Varias conclusiones son afirmaciones negativas**, estructuralmente más frágiles que las positivas. Se sostuvieron exigiendo inventarios exhaustivos antes de aceptar un "no se encontró".

---

## 22. Conclusión del Frente 6

**El Frente 6 encontró y corrigió vulnerabilidades reales de dependencias, endureció la cadena de CI, y confirmó mediante regresión completa que las remediaciones no introdujeron fallos.**

No existe evidencia de: dependencia vulnerable pendiente que bloquee producción · reapertura de la conclusión de inyección SQL del Frente 4 · XXE o SSRF nueva en el pipeline de PDF · permisos excesivos en CI · acciones mutables pendientes · dump histórico con datos reales · regresión de seguridad · alerta de Dependabot abierta.

**Un único hallazgo de clase DEFENSA AUSENTE queda abierto —backup y restore— y está registrado como gate bloqueante para la carga de datos reales, no para el cierre de este frente.**

---

## 23. Estado global de la auditoría

```
F1 — Aislamiento multi-tenant / IDOR      CERRADO
F2 — Exposición de datos sensibles        CERRADO
F3 — Autenticación y sesión               CERRADO
F4 — Inyección y SSRF                     CERRADO
F5 — Configuración productiva              CERRADO
F6 — Dependencias y supply chain          CERRADO
```

## **AUDITORÍA MÍNIMA PRE-GO-LIVE — CERRADA**

El proyecto pasa de **fase de auditoría** a **fase de preparación de go-live**.

**Esto no autoriza la carga de datos reales.** Restan:

1. Contratar y configurar la infraestructura definitiva (Render Starter · Neon Launch · frontend productivo).
2. Ejecutar el **Go-Live Security Check** de la sección 20, con especial atención a **GL-09 (backup y restore probado)**, que es el único gate que corresponde a una defensa ausente y no a una revalidación.
3. Recién entonces: **datos reales autorizados**.

---

*Documento de cierre del Frente 6 y de la auditoría de seguridad F1–F6 de Gym Planner.*
