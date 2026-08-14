# Frente 4 — Inyección y SSRF

> Auditoría de seguridad de Gym Planner, previa a la carga de datos reales.
> Documento de cierre del cuarto de seis frentes.

| | |
|---|---|
| **Frente** | 4 — Inyección y SSRF |
| **Estado** | CERRADO |
| **Veredicto** | Sin bloqueantes de producción. **Cero cambios de código.** |
| **Método** | Auditoría de lectura (Paso 0 + Paso 0.1). Sin edición, sin ejecución. |
| **Superficies auditadas** | SSRF vía `logoUrl` · SQL/JPQL/HQL · Thymeleaf/PDF · logs, CSV, WhatsApp |
| **Frentes previos** | 1 (aislamiento multi-tenant), 2 (exposición de datos sensibles), 3 (auth y sesión, incl. F3-3.1) — cerrados y mergeados |

---

## 1. Resumen ejecutivo

La pregunta central del frente era: **¿el servidor hace una request HTTP saliente a una URL provista por el usuario?** De esa respuesta dependía todo el peso del frente.

La respuesta es **no**, y está confirmada por dos vías independientes:

1. **No hay ningún cliente HTTP saliente en el backend.** No existe `RestTemplate`, `WebClient`, `HttpClient`, `URL.openStream/openConnection`, OkHttp, Apache HttpClient, `ImageIO.read(URL)` ni helper de descarga que consuma `logoUrl` ni ninguna otra URL de usuario.
2. **El pipeline de PDF no resuelve recursos externos.** Existe un único template (`pdf/routine.html`) y un único punto de render (`PdfGenerator.htmlToPdf`). El template no contiene `<img>`, `src=`, `<link>`, `url(...)`, `@font-face`, `@import` ni ninguna URL absoluta. `logoUrl` viaja hasta el `Context` de Thymeleaf dentro de `PdfGymDto`, pero **el template nunca lo referencia**.

No se cumple la cadena `input controlado → URI resoluble → fetch server-side`. **No hay SSRF.**

El resto de las superficies dio NO BUG: las queries están parametrizadas (incluida `unaccent`), no hay `th:utext` ni construcción manual de HTML, y no existe superficie de export CSV.

Quedan **dos residuales de hardening diferidos** (ordenamiento dinámico sin allowlist, y schemes no restringidos en `imageUrl`/`videoUrl`), ambos sin brecha demostrada y ambos con un trigger explícito de revaluación documentado en la sección 7.

---

## 2. Método y alcance

Se aplicó el método vigente de la auditoría: **un prompt = un concern**, Paso 0 de lectura bloqueante antes de tocar nada, evidencia con archivo + línea, y "NO ENCONTRADO" explícito en lugar de inferencia.

El frente se ejecutó en dos pasos de lectura:

- **Paso 0** — barrido completo de las cuatro superficies (SSRF, SQL, template/PDF, superficies rápidas).
- **Paso 0.1** — verificación puntual de la afirmación negativa más cargada del Paso 0 ("ningún template referencia `logoUrl`"). Se pidió porque una conclusión de este peso no puede descansar sobre la mención de un único archivo: si existiera un segundo template alimentando al `ITextRenderer`, o un recurso externo en el CSS embebido, la conclusión del frente cambiaba.

Ninguno de los dos pasos modificó, creó ni ejecutó nada.

**Criterio de clasificación aplicado:** se arregla antes de producción solo lo que constituye brecha activa o defensa ausente explotable. Hardening, riesgo aceptado y deuda se documentan con su trigger y se difieren. No se implementan defensas para superficies hipotéticas.

---

## 3. S1 — SSRF vía `logoUrl` (foco principal)

### 3.1 Recorrido del dato de punta a punta

`logoUrl` se recibe en el request autenticado de actualización del gym, se valida **únicamente por longitud** (máx. 500), se persiste en la entidad `Gym` y se devuelve en las respuestas de la API.

Consumos encontrados tras la persistencia:

| Consumidor | ¿Fetch server-side? |
|---|---|
| Respuesta de la API (`GymResponse`) | No. Texto plano hacia el cliente. |
| Frontend (Settings) | No. Hoy es solo un campo editable; **ni siquiera se usa como `<img>`**. |
| `PdfGymDto.logoUrl` → `Context` de Thymeleaf | No. El dato llega al contexto pero **el template no lo referencia**. |

### 3.2 El pipeline de PDF, en detalle

```
Gym.logoUrl
  → PdfGymDto.logoUrl
  → Context de Thymeleaf        (RoutinePdfService.renderHtml)
  → pdf/routine.html            ← NO referencia logoUrl
  → XHTML sin ninguna URL
  → PdfGenerator.htmlToPdf      → ITextRenderer.setDocumentFromString(xhtml)
```

Confirmaciones del Paso 0.1:

- **Un solo template.** El barrido recursivo de `backend/src/main/resources/templates/**` devuelve exclusivamente `pdf/routine.html`.
- **Un solo punto de render.** Única instanciación de `ITextRenderer`, en `PdfGenerator`. No hay otras llamadas a `setDocumentFromString`, ni a `setDocument`, ni a las sobrecargas que reciben base URL.
- **Sin base URI.** Se usa `setDocumentFromString(xhtml)` sin segundo argumento: no se le entrega al renderer una base contra la cual resolver URIs relativas.
- **Sin componentes custom.** No hay `UserAgentCallback`, `ReplacedElementFactory` ni `ResourceLoader` propios que alteren la resolución de recursos.
- **Sin recursos externos en el template.** `logoUrl`, `<img`, `src=`, `<link`, `@font-face`, `url(`, `background-image`, `@import`: todos NO ENCONTRADOS. Las ocho apariciones de `background:` son colores hexadecimales literales.
- **Sin URLs absolutas.** La única cadena `http://` del template es el namespace XML `xmlns:th="http://www.thymeleaf.org"`, que no es un contexto de carga de recursos.

### 3.3 Clasificación

**NO BUG.** No existe SSRF server-side. No hay capacidad de request saliente hacia recursos internos (`localhost`, rangos privados, link-local, servicios internos) originada en input de usuario.

### 3.4 Residual: `logoUrl` inerte dentro de `PdfGymDto`

El dato viaja hasta el `Context` de Thymeleaf sin ser consumido. **Esto no es una defensa faltante** — eliminarlo del DTO no aportaría seguridad material hoy. Es deuda de limpieza menor.

Lo que sí importa registrar es **por qué este residual merece una decisión anticipada** (sección 7.1): con el dato ya disponible en el contexto, incorporar el logo al PDF es un `<img th:src="${data.gym.logoUrl}">` de una sola línea, sin ninguna fricción que obligue a detenerse a pensar en la implicancia.

### 3.5 Nota: divergencia con el design doc

La ausencia del logo en el PDF **contradice la especificación**. El Prompt 4 del design doc incluía el logo en el header del template (`<img th:if="${gym.logoUrl}" th:src="${gym.logoUrl}"/>`), lo listaba como criterio de calidad ("El PDF tiene logo + datos del gym...") y anotaba textualmente que *"si es una URL externa, Flying Saucer la descargará"*.

O sea: **el feature está pendiente, no descartado**, y la spec original ya describía —sin nombrarlo así— exactamente el comportamiento de fetch server-side que este frente vino a auditar. El campo existe en el formulario de Settings esperando que alguien lo complete.

Consecuencia para la auditoría: el trigger de reauditoría de la sección 7.1 **no es hipotético ni remoto**. Es un feature que el cliente va a pedir. Por eso el documento deja la decisión de diseño ya tomada, en lugar de un recordatorio genérico de "reauditar".

### 3.6 Otros campos de URL: `imageUrl` / `videoUrl`

`CreateExerciseRequest` / `UpdateExerciseRequest` aceptan cualquier string de hasta 500 caracteres. El backend los persiste y **nunca los fetchea**. El frontend los usa como `<a href={...} target="_blank" rel="noreferrer">`. No aparecen en PDF ni en WhatsApp.

**NO BUG respecto de SSRF.** Residual de hardening diferido en sección 7.2.

---

## 4. S2 — Inyección SQL / JPQL / HQL

Encontrado: `@Query` fijas, una única `nativeQuery` fija y parametrizada, Criteria API, Specifications, `@Param`, `LIKE` parametrizados.

NO ENCONTRADO: `EntityManager.createQuery`/`createNativeQuery` dinámicos, `JdbcTemplate` productivo, `JpaSort.unsafe`, SQL/JPQL/HQL concatenado, `ORDER BY` manual, nombres de columna/tabla o `LIMIT`/`OFFSET` concatenados.

### 4.1 `unaccent`

Se usa vía `CriteriaBuilder.function` sobre **columnas fijas**. El input del usuario se convierte en el **valor** del `LIKE` (`"%" + valorNormalizado + "%"`), no en estructura de la query.

Esto merece una aclaración explícita porque es una fuente clásica de falso positivo: la concatenación de `%` es **visual, en Java**, no en el string del SQL. Hibernate parametriza ese valor completo como un bind. Lo peligroso sería `"... LIKE '%" + userInput + "%'"` construido dentro del SQL, y eso no existe en el repo.

**NO BUG.**

### 4.2 Ordenamiento dinámico (`sort`)

`StudentController`, `ExerciseController`, `TemplateController` y `RoutineController` parsean el parámetro `sort` así:

```java
String[] parts = sort.split(",", 2);
Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("desc")
        ? Sort.Direction.DESC : Sort.Direction.ASC;
Sort.by(direction, parts[0]);
```

La **dirección** está acotada a dos valores. La **propiedad** viene del request sin allowlist.

**No es SQL injection.** Spring Data resuelve `Sort` contra propiedades de la entidad, no concatena el string en el SQL; una propiedad inexistente no se ejecuta, falla al resolverse.

El residual real —y conviene nombrarlo con precisión, porque no es "quedaría más prolijo"— es doble:

1. **Robustez:** una propiedad inválida produce `PropertyReferenceException`, que se traduce en un **500 en lugar de un 400**. Es un error de cliente devuelto como error de servidor.
2. **Oráculo de ordenamiento:** se puede ordenar por campos que la API no devuelve en el DTO de respuesta. Con paginación, el orden de los resultados filtra información parcial sobre un campo no expuesto.

Ambos efectos quedan **acotados dentro del mismo tenant** por el filtrado por `gymId` verificado en el Frente 1: el atacante solo obtendría señal sobre datos de su propio gimnasio, a los que ya tiene acceso legítimo por otras vías.

**Clasificación: HARDENING BAJO.** Diferido (sección 7.3).

---

## 5. S3 — Thymeleaf / PDF / template injection

- **`th:utext`: NO ENCONTRADO** en ningún template. Todo el contenido dinámico usa `th:text`, que escapa por defecto.
- Datos de usuario rastreados hasta el PDF, todos vía `th:text`: nombre de rutina, `generalNotes`, nombres de días, títulos de bloques, `blockNotes`, `exerciseNotes`, nombre del alumno, nombre del gym.
- **NO ENCONTRADO:** construcción manual de HTML (`StringBuilder`, `String.format`, `formatted`), text blocks HTML, sustitución manual de placeholders, segundo procesamiento de Thymeleaf sobre contenido almacenado, reevaluación de expresiones (SSTI).
- `primaryColor` se inserta en un `th:style`, pero está restringido por regex `^#[0-9A-Fa-f]{6}$` tanto en el request como en la entidad.
- `section.kind` usa valores internos de enum. `rowspan` es `int`.

**NO BUG.** No hay template injection ni HTML injection demostrada.

Vale registrar el encadenamiento que se auditó explícitamente: aunque hubiera existido `th:utext` con input de usuario, la consecuencia relevante **no habría sido XSS** (es un PDF server-side, no un navegador), sino inyección de markup que Flying Saucer podría resolver — es decir, un segundo vector hacia el SSRF de la sección 3, independiente del campo `logoUrl`. Al no existir `th:utext` ni resolución de recursos externos, ambas puertas están cerradas.

---

## 6. S4 — Superficies rápidas

| Superficie | Resultado |
|---|---|
| **Log injection** | Los logs registran IDs, cantidades y nombres de clase. Sin input de usuario nuevo respecto de lo ya cubierto en el Frente 2. **NO BUG.** |
| **CSV / Excel injection** | No existe superficie de export. **NO ENCONTRADO / sin superficie.** |
| **WhatsApp** | Los datos de rutina se emiten como texto plano, pasan por `encodeURIComponent` y el hostname de destino es fijo. **NO BUG.** |

---

## 7. Residuales diferidos y triggers de revaluación

Ninguno de estos ítems se implementa en este frente. Se documentan con la condición precisa que obliga a retomarlos.

### 7.1 Trigger — incorporación del logo al PDF (decisión ya tomada)

**Condición:** cuando se implemente el logo del gimnasio en el PDF (feature pendiente del design doc, esperado a corto plazo).

**Decisión de diseño tomada por anticipado — no reabrir el debate en ese momento:**

> **No dejar que Flying Saucer resuelva una URL remota.** El logo se valida y se descarga en el service, con controles explícitos, y al template se le pasa un recurso local o los bytes embebidos (data URI). El template **nunca** debe recibir una URL controlada por el usuario en un contexto de carga de recurso (`src`, `href`, `url(...)`, `@font-face`).

Motivo de dejarlo escrito así: sin esta decisión previa, el camino natural de implementación es un `th:src` de una línea que reintroduce el SSRF de manera silenciosa, sin que nadie perciba que se cruzó un límite de seguridad. Un recordatorio de "reauditar" depende de que alguien se acuerde; una decisión escrita no.

Si al implementarlo se opta igual por descarga server-side, los controles a definir en ese momento incluyen: allowlist de schemes (solo `https`), bloqueo de destinos internos (`localhost`, `127.0.0.0/8`, rangos privados RFC1918, link-local `169.254.0.0/16`, IPv6 equivalentes), validación **sobre la IP resuelta** y no solo sobre el hostname, política de redirects (no seguir, o revalidar el destino en cada salto), y timeout + límite de tamaño de descarga.

### 7.2 Trigger — schemes de `imageUrl` / `videoUrl`

**Estado actual:** campos semánticamente URL, sin restricción de scheme. El residual es la posibilidad de almacenar `javascript:` o `data:` y que se rendericen en un `<a href>` del frontend.

**Por qué se difiere:** en V1 el mismo usuario `OWNER` autenticado crea, modifica y consume esos enlaces. No hay boundary víctima/atacante — es self-XSS, que no constituye brecha.

**Condición de revaluación:** cuando exista un segundo actor con capacidad de crear o modificar ejercicios que otro actor consuma. En concreto, cuando se habilite el rol **`TRAINER`** (previsto en el enum `UserRole` desde el Prompt 1, hoy sin uso), o cualquier forma de compartir catálogo de ejercicios entre gimnasios o entre usuarios. Ahí aparece el boundary y el residual se convierte en XSS almacenado.

**Fix previsto:** allowlist de schemes (`https`, y `http` solo si se decide aceptarlo) validada en el request DTO.

### 7.3 Trigger — allowlist de propiedades en `sort`

**Estado actual:** ver sección 4.2.

**Por qué se difiere:** no hay SQL injection; el impacto está acotado al propio tenant por el aislamiento verificado en el Frente 1.

**Condición de revaluación:** si se agregan a las entidades campos sensibles no expuestos en los DTOs de respuesta (el oráculo de ordenamiento gana valor real), o si los 500 por `sort` inválido aparecen en los logs de producción (deja de ser teórico).

**Fix previsto:** allowlist de propiedades ordenables por endpoint, con 400 ante propiedad no permitida.

### 7.4 Nota para el Frente 6 — dependencias de red en tiempo de render

`PdfGenerator` llama a `setDocumentFromString(xhtml)` sin base URI y el template no tiene recursos externos: hoy la generación de PDF **no depende de la red**. Conviene que siga siendo así, porque un recurso remoto en el template convertiría cada generación de PDF en una request saliente sincrónica — un problema de latencia y disponibilidad, no de seguridad.

Punto a verificar en el Frente 6: que el DOCTYPE de `routine.html` sea `<!DOCTYPE html>` simple y no declare una DTD externa, ya que el parseo del XHTML podría intentar resolverla por red.

---

## 8. Tabla de clasificación final

| # | Ítem | Clasificación | Exposición | Acción |
|---|---|---|---|---|
| 1 | SSRF vía `logoUrl` | NO BUG | Autenticada | Cerrado |
| 2 | Cliente HTTP saliente en backend | NO BUG / sin superficie | — | Cerrado |
| 3 | `logoUrl` inerte en `PdfGymDto` | NO BUG / deuda menor | — | Documentado (7.1) |
| 4 | Inyección SQL / JPQL / HQL | NO BUG | Autenticada | Cerrado |
| 5 | `unaccent` y búsquedas | NO BUG | Autenticada | Cerrado |
| 6 | `sort` sin allowlist | HARDENING BAJO | Autenticada | Diferido (7.3) |
| 7 | Template / PDF injection | NO BUG | Autenticada | Cerrado |
| 8 | Schemes de `imageUrl` / `videoUrl` | HARDENING BAJO | Autenticada | Diferido (7.2) |
| 9 | Log injection nueva | NO BUG | — | Cerrado |
| 10 | CSV / Excel injection | Sin superficie | — | Cerrado |
| 11 | Texto de WhatsApp | NO BUG | Autenticada | Cerrado |

---

## 9. Limitaciones de este frente

Registradas por honestidad metodológica, para que quien lea este documento en el futuro sepa qué **no** garantiza:

1. **Es auditoría de lectura estática, no pentest.** No se ejecutaron payloads contra la aplicación. La confianza descansa en la reconstrucción del data flow con evidencia de archivo + línea.
2. **La conclusión principal es una afirmación negativa.** "No existe fetch server-side" es más frágil que una afirmación positiva: basta un template nuevo, una dependencia que resuelva recursos, o un `th:src` agregado sin revisión para invalidarla. Por eso se ejecutó el Paso 0.1 (inventario exhaustivo de templates y de puntos de render) y por eso la sección 7.1 fija una decisión de diseño en lugar de un recordatorio.
3. **A diferencia del Frente 3, no hubo cierre por verificación runtime.** La lección de F3-3.1 —"un fix que depende de infraestructura no se cierra con tests verdes, se cierra reproduciendo el ataque contra Render"— **no aplica acá, y conviene decir por qué**: en este frente no hubo fix. No hay ningún cambio de comportamiento que pudiera comportarse distinto en producción que en local. Lo que se verificó es la ausencia de una superficie, y eso es una propiedad del código fuente, no del entorno de despliegue.
4. **El alcance excluye deliberadamente** lo cubierto en frentes anteriores (aislamiento multi-tenant, exposición de datos sensibles, auth y sesión) y lo que corresponde a los frentes siguientes (configuración productiva, dependencias).

---

## 10. Conclusión

**No se encontraron brechas activas ni defensas ausentes que bloqueen la puesta en producción en el Frente 4.**

El frente se cierra **sin cambios de código**. Quedan tres residuales documentados con su trigger de revaluación (7.1, 7.2, 7.3) y una nota derivada al Frente 6 (7.4).

Esto **no habilita la carga de datos reales**: restan los Frentes 5 (configuración productiva e infraestructura) y 6 (dependencias y disponibilidad), y la evaluación conjunta F1–F6.

**Siguiente paso:** Frente 5 — configuración de producción (CORS, actuator, headers de seguridad, gestión de secretos).
