# Frente 5 — Configuración productiva e infraestructura

> Auditoría de seguridad de Gym Planner, previa a la carga de datos reales.
> Documento de cierre del quinto de seis frentes.

| | |
|---|---|
| **Frente** | 5 — Configuración productiva e infraestructura |
| **Estado** | CERRADO |
| **Veredicto** | Sin brechas activas. Sin bloqueantes de la demo. **Cero cambios de código.** |
| **Método** | Paso 0 (lectura del repo, Codex) + Paso 0-bis (verificación runtime manual contra Render, Neon y Vercel) |
| **Infraestructura auditada** | **DEMO**: Render Free (backend, Docker, branch `main`) · Neon Free (PostgreSQL 16, us-east-1) · Vercel Hobby (frontend) |
| **Frentes previos** | 1, 2, 3 (incl. F3-3.1) y 4 — cerrados y mergeados |

---

## 1. Resumen ejecutivo

Este frente no audita código de aplicación: audita **configuración**, es decir, lo que puede volver insegura una app cuyo código está bien.

La diferencia metodológica central con los frentes anteriores es que acá **la configuración declarada en el repositorio no demuestra nada por sí sola**. Un `application-prod.yml` impecable es irrelevante si el perfil `prod` no se activa en el servidor. Por eso el frente se ejecutó en dos pasos y **ninguna conclusión relevante se cerró solo con lectura**.

Resultado:

- **Cero brechas activas.**
- **Cero defensas ausentes explotables en la demo.**
- **Cero bloqueantes actuales.**
- **Cero cambios de código obligatorios para cerrar el frente.**

Las invariantes críticas quedaron verificadas contra el runtime real: el perfil `prod` está activo, el `JWT_SECRET` es externo, Swagger no está expuesto, los endpoints sensibles de Actuator no devuelven datos **ni de forma anónima ni con un usuario autenticado**, CORS rechaza orígenes no autorizados con un preflight real, y la conexión a Neon usa TLS obligatorio con channel binding.

Los fixes de los Frentes 2 y 3 **sobrevivieron sin regresión**.

Quedan residuales de hardening y un conjunto de **gates de go-live** (sección 8), que no bloquean el cierre de este frente pero sí bloquean la carga de datos reales.

---

## 2. Qué cierra y qué no cierra este frente

Esta distinción es la parte más importante del documento y conviene que quede sin ambigüedad.

**F5 cierra:** que la configuración declarada en el repositorio es correcta, y que la infraestructura **de demo** realmente en uso no presenta una brecha demostrada. Las invariantes de seguridad fueron verificadas en runtime, no solo leídas.

**F5 NO cierra:** la aprobación de la infraestructura productiva definitiva, que **todavía no existe**. El plan es migrar:

| Componente | Demo actual | Producción prevista |
|---|---|---|
| Backend | Render Free | Render Starter |
| Base de datos | Neon Free | Neon Launch |
| Frontend | Vercel Hobby | Cloudflare Pages |

La ausencia actual de Cloudflare Pages **no es un hallazgo de este frente**: es una migración planificada.

Por eso el cierre de F5 se acompaña de una sección de **Go-Live Gates** (sección 8). Esos gates se verifican en una instancia breve y acotada —el **Go-Live Security Check**— *después* de cerrar el Frente 6 y de montar la infraestructura definitiva. El Go-Live Security Check **no reabre F1–F6**: verifica que las invariantes ya definidas siguen valiendo sobre la infraestructura nueva.

Secuencia acordada:

```
F1 ✓ → F2 ✓ → F3 ✓ → F4 ✓ → F5 ✓ → F6 → infraestructura definitiva
    → Go-Live Security Check → carga de datos reales
```

---

## 3. Método

**Paso 0 (Codex, solo lectura):** auditoría del repositorio sin editar, crear ni ejecutar build/tests/aplicación. Toda afirmación con archivo + línea, "NO ENCONTRADO" explícito en lugar de inferencia. Se le prohibió expresamente deducir qué variables están seteadas en Render o cómo está configurado Neon/Vercel: cuando una conclusión dependía de un valor externo, debía declararlo como no verificable desde el repo.

**Paso 0-bis (Felipe, manual):** verificación contra las consolas reales y pruebas HTTP contra la demo desplegada, incluyendo pruebas anónimas y autenticadas.

Esta separación es aplicación directa de la lección de **F3-3.1**: un fix o una garantía que depende de infraestructura no se cierra con evidencia estática. Se cierra reproduciendo el comportamiento real.

---

## 4. Paso 0 — Evidencia estática (repositorio)

### 4.1 Perfiles y activación

`application.yml` declara:

```yaml
spring.profiles.active: ${SPRING_PROFILES_ACTIVE:dev}
```

Es decir: **sin variable externa, la aplicación cae a `dev`**.

Diferencias relevantes entre perfiles:

| Aspecto | `dev` | `prod` |
|---|---|---|
| JWT secret | conocido, de desarrollo | obligatorio, externo |
| Base de datos | local, credenciales de desarrollo | externa, obligatoria |
| Swagger / OpenAPI | habilitado | `api-docs.enabled=false`, `swagger-ui.enabled=false` |
| CORS | localhost | `${CORS_ALLOWED_ORIGINS}`, sin fallback |
| Detalle de errores | ampliado | `never` / `false` |
| Hibernate SQL/bind/extract | verboso | `WARN` |
| `forward-headers-strategy` | — | `framework` |
| `client-ip-source` | `remote-address` | `cloudflare` |

**Clasificación estática:** riesgo *condicional* alto. Todo el endurecimiento de `prod` depende de una única variable de entorno externa. Requería verificación runtime obligatoria → resuelto en 5.1.

### 4.2 Swagger / OpenAPI

`SecurityConfig` permite acceso anónimo a `/v3/api-docs/**`, `/swagger-ui/**` y `/swagger-ui.html`. Aislado, eso expondría el mapa completo de la API sin autenticación.

Pero `application-prod.yml` desactiva springdoc explícitamente (`springdoc.api-docs.enabled=false`, `springdoc.swagger-ui.enabled=false`).

Es decir: **la protección no es el `SecurityConfig`, es el perfil.** Los matchers `permitAll()` siguen ahí; lo que impide la exposición es que no haya nada registrado detrás de ellos. Correcto en la práctica, pero es una defensa de una sola capa que depende del mismo punto único que 4.1. Verificado en runtime → 5.4.

### 4.3 Actuator

- Exposición declarada: únicamente `health`.
- `SecurityConfig` permite anónimamente solo `/actuator/health` — **no** hay `permitAll()` global sobre `/actuator/**`.
- No se encontraron `HealthIndicator` ni endpoints custom.

**Sin hallazgo estático.** Verificado en runtime, anónima y autenticadamente → 5.5.

### 4.4 CORS

- `allowedOrigins` desde property (no `allowedOriginPatterns`).
- Sin wildcard `*`.
- `allowCredentials = false`.
- Métodos explícitos: GET, POST, PUT, PATCH, DELETE, OPTIONS.
- Allowed headers: `Authorization`, `Content-Type`, `Accept`.
- Exposed headers: `Authorization`, `Content-Disposition`.
- Sin `@CrossOrigin` a nivel de controller o método que contradiga la configuración global.
- En `prod`: `${CORS_ALLOWED_ORIGINS}` **sin fallback**.

Dos observaciones que conviene dejar escritas:

**`allowCredentials = false` es la decisión correcta acá, y refuerza la postura.** El frontend autentica con Bearer token en el header `Authorization`, no con cookies. Con credentials desactivado, aunque en el futuro alguien ampliara los orígenes permitidos, el navegador no adjuntaría cookies de sesión en requests cross-origin. Es una capa de contención adicional que conviene no perder por descuido.

**La ausencia de fallback en `prod` también es correcta.** Un default permisivo que se activara al faltar la variable habría sido un hallazgo; acá el diseño falla explícito en lugar de fallar abierto.

### 4.5 Headers de seguridad

No existe bloque `.headers(...)` explícito en `SecurityConfig`. Codex **no infirió** los defaults de Spring Security (decisión correcta: los defaults dependen de versión y configuración). Quedó pendiente de verificación runtime → 5.6.

### 4.6 Secretos

- **No se encontraron secretos reales** en archivos trackeados.
- **No se encontraron secretos reales de infraestructura en la historia de Git.** Este punto importa especialmente: un secreto removido en un commit posterior sigue estando en la historia y sigue comprometido. No es el caso.
- Lo único presente: placeholders, credenciales locales de dev/test, hashes BCrypt y referencias a variables de entorno.
- Únicos `.env*` trackeados: `.env.example` y `frontend/.env.example` — que es exactamente lo que corresponde.
- La credencial bootstrap histórica ya fue neutralizada en **F3-1** (migración V14 con predicado estrecho). No se reabre.

**`.gitignore`:** `.env` y `.env.local` están ignorados, pero **`.env.production` y `frontend/.env.production` no quedarían cubiertos**. No hay fuga actual —esos archivos no existen— pero es una puerta abierta a un commit accidental futuro. Residual 7.1.

### 4.7 JPA / Flyway

- `ddl-auto = validate` (nunca `create`/`update`).
- `show-sql = false`.
- Flyway habilitado, migraciones en orden.

Sin hallazgo.

### 4.8 Frontend

- Única variable custom: `VITE_API_URL`, con fallback en código a `http://localhost:8080`. El fallback es inofensivo: en un bundle desplegado apuntaría al localhost del visitante, que no responde. No filtra nada.
- Se detectaron algunos `console.error(error)` sobre objetos de error completos en flows productivos. Residual 7.4.
- No se encontró configuración explícita de sourcemaps. Verificado en runtime → 5.9.

Recordatorio permanente: **todo lo que entre en una variable `VITE_*` queda embebido en el bundle público.** Hoy solo hay una URL de API, que es información pública por definición.

### 4.9 Docker

Dockerfile multistage, Java 21, copia únicamente el jar al stage de runtime, sin secretos ni flags de debug. **Sin directiva `USER`**: el proceso corre como root dentro del contenedor. Residual 7.2.

---

## 5. Paso 0-bis — Verificación runtime (infraestructura de demo)

### 5.1 Perfil activo — la verificación más importante del frente

Variables de entorno presentes en el servicio de Render: `SPRING_PROFILES_ACTIVE`, `JWT_SECRET`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CORS_ALLOWED_ORIGINS`. Los valores sensibles no se expusieron durante la verificación.

**Confirmado: `SPRING_PROFILES_ACTIVE=prod`.**

Con eso queda **descartado** el escenario que el Paso 0 había marcado como riesgo condicional alto: arranque con perfil `dev` → JWT secret de desarrollo (presente en el repo) + Swagger habilitado + errores detallados + CORS de localhost.

`JWT_SECRET` y las tres variables de base de datos existen como variables externas. `CORS_ALLOWED_ORIGINS` está seteado exactamente en `https://gym-planner-theta.vercel.app`.

No hay override de `LOG_LEVEL`, de modo que rige el fallback `INFO` del perfil `prod` (relevante porque el hardening de logging del Frente 2 depende de que los loggers de Hibernate estén fijados en `WARN` — ver 6.1).

Servicio: Web Service, Docker, branch `main`, plan Free, auto-deploy activo, **Health Check Path sin configurar** (residual operativo 7.5).

> **Nota sobre la fragilidad estructural de este punto — ver gate GL-01.** El resultado es correcto hoy, pero toda la postura de seguridad del backend cuelga de una única variable de entorno cuyo default es `dev`. Si esa variable se borrara, se perdiera al recrear el servicio, o se olvidara al provisionar un entorno nuevo, la configuración degradaría silenciosamente.
>
> Es probable que en la práctica el arranque **falle** en lugar de degradar, porque el perfil `dev` apunta a una base de datos local que en Render no existe, y HikariCP/Flyway abortarían el arranque antes de servir el primer request. Pero eso sería **fallar cerrado por accidente, no por diseño**, y esa suposición no fue verificada. La decisión sobre un mecanismo de fail-fast explícito queda registrada como gate de go-live, no como fix de este frente.

### 5.2 Neon (base de datos)

| Aspecto | Estado verificado |
|---|---|
| Plan | Free |
| Branch default | `production` |
| Versión | PostgreSQL 16 |
| Región | AWS us-east-1 |
| Autosuspend de compute | 5 minutos |
| Data API | deshabilitada |
| VPC | no configurada |
| IP restrictions | ninguna |
| Retención de historia | ~6 horas |
| Connection pooling | desactivado |
| Roles | uno solo: `neondb_owner`, owner de `neondb` |

**TLS — verificado y correcto.** La connection string de Neon incluye `sslmode=require` y `channel_binding=require`. La conexión Render → Neon usa TLS obligatorio con channel binding, lo que protege además contra escenarios de relay de credenciales. **OK / NO BUG.**

**Rol de base de datos.** Existe un único rol y es el owner. Si `DB_USERNAME` corresponde a `neondb_owner` —lo esperable, dado que es el único—, la aplicación opera con permisos de owner: puede hacer DDL arbitrario, incluido `DROP`. Separar un usuario de migraciones (con DDL) de un usuario de runtime (solo DML) es la práctica de mínimo privilegio correcta, pero no es bloqueante para V1 y agrega complejidad operativa al ciclo de Flyway. Residual 7.3.

**IP restrictions ausentes — merece precisión.** El endpoint de Neon es alcanzable desde cualquier IP de internet; la única defensa es credencial + TLS. Eso *es* defensa real, no una brecha: no hay acceso sin credencial válida. Pero significa que la credencial de base de datos es un punto único de falla sin segunda capa de contención, y el sistema va a almacenar **datos de salud** (lesiones de alumnos, notas internas del profesor). Gate de go-live GL-05.

**Retención de ~6 horas** en el plan Free es insuficiente para operación real con cliente. Es disponibilidad/DR más que seguridad, y corresponde al Frente 6 y al go-live.

### 5.3 Vercel (frontend de demo)

Plan Hobby, production branch `main`, dominio `https://gym-planner-theta.vercel.app`, deployment en estado Ready. Única variable de entorno: `VITE_API_URL`, aplicada a Production y Preview. Sin otras variables. Sin hallazgos de seguridad en deployment settings. Un solo dominio, el esperado.

### 5.4 Swagger / OpenAPI — prueba runtime

Se solicitaron `/v3/api-docs` y las rutas de Swagger UI contra la demo desplegada.

Respuesta: **JSON genérico, status 500**, `"An unexpected error occurred."`

No se devuelve OpenAPI JSON, ni Swagger UI, ni listado de endpoints, ni datos internos, ni stacktrace.

**Conclusión: Swagger/OpenAPI NO está expuesto. OK.**

Dos lecturas adicionales de esta prueba, que valen más que el resultado literal:

1. **Es confirmación independiente de que el perfil `prod` está activo.** Si estuviera corriendo `dev`, esas rutas habrían devuelto la documentación.
2. **El 500 es genérico gracias al hardening del Frente 2.** Con `server.error.include-message=never` e `include-stacktrace=never`, la excepción interna no filtra información. Sin ese fix, este mismo 500 podría haber revelado clases internas o rutas.

El hecho de que devuelva 500 en lugar de 404 resultó ser un comportamiento **global** de la aplicación ante rutas no mapeadas, no algo específico de springdoc — ver 5.5 y residual 7.6.

### 5.5 Actuator — pruebas runtime anónima y autenticada

**`/actuator/health` (anónimo):** 200 OK. Body con `status: UP` y los grupos `liveness` / `readiness`. No expone base de datos, versión, componentes internos, credenciales ni stacktrace. **OK / NO BUG.**

**Endpoints sensibles, prueba anónima** — `/actuator/env`, `/actuator/beans`, `/actuator/configprops`, `/actuator/metrics`: todos devuelven **401 Unauthorized**.

Ese 401, por sí solo, **no era prueba suficiente**, y conviene dejar registrado por qué: la regla `anyRequest().authenticated()` de Spring Security intercepta el request *antes* de que Spring MVC resuelva que la ruta no existe. Un path inexistente bajo `/actuator/**` y uno existente-pero-protegido producen respuestas anónimas idénticas. El 401 demuestra que nada es legible sin autenticación, pero no distingue "no registrado" de "registrado y protegido" — y esa distinción importa, porque si los endpoints estuvieran registrados, cualquier usuario autenticado del sistema podría leer `JWT_SECRET` y `DB_PASSWORD` desde `/actuator/env`.

**Prueba autenticada — ejecutada con JWT válido de OWNER.** Control previo: `GET /api/me` → **200**, confirmando que el token autentica correctamente (sin este control, un 401 en Actuator podría deberse a un token inválido y no a la protección).

Resultado sobre los diez endpoints sensibles:

| Endpoint | Status autenticado |
|---|---|
| `env` | 500 |
| `configprops` | 500 |
| `beans` | 500 |
| `heapdump` | 500 |
| `threaddump` | 500 |
| `mappings` | 500 |
| `loggers` | 500 |
| `metrics` | 500 |
| `httpexchanges` | 500 |
| `caches` | 500 |

Body en todos los casos: el mismo JSON genérico (`"An unexpected error occurred."`), sin payload de Actuator, sin stacktrace, sin nombres de clases ni de properties.

**Conclusión: ninguno de los endpoints sensibles de Actuator está registrado. Cero exposición de datos. OK / NO BUG.**

El razonamiento que sostiene la conclusión es el **contraste**, no el status code en sí:

- `/actuator/health` responde **200 con body válido** → la infraestructura de Actuator está activa y funcionando.
- Los otros diez responden **500 sin ningún payload**, con un token que la propia aplicación acepta como válido.

Si estuvieran registrados y solo protegidos por autenticación, un OWNER autenticado habría obtenido **200 con contenido**. En particular `heapdump` habría empezado a transferir un volcado de memoria. No ocurrió en ninguno. Esto coincide exactamente con la evidencia estática (`management.endpoints.web.exposure.include: health`), y las dos fuentes se confirman mutuamente.

El 500 —en lugar del 404 semánticamente correcto— **no es específico de Actuator**: es el mismo comportamiento observado en las rutas de Swagger (5.4). La aplicación devuelve un 500 genérico ante cualquier ruta no mapeada, porque el `GlobalExceptionHandler` captura la excepción resultante y la traduce a error interno. Residual 7.6.

### 5.6 Headers HTTP del backend — prueba runtime

Headers efectivos observados:

| Header | Valor |
|---|---|
| `strict-transport-security` | `max-age=31536000; includeSubDomains` |
| `x-content-type-options` | `nosniff` |
| `x-frame-options` | `DENY` |
| `x-xss-protection` | `0` |
| `cache-control` | `no-cache, no-store, max-age=0, must-revalidate` |
| `pragma` | `no-cache` |
| `expires` | `0` |
| `vary` | `Origin`, `Access-Control-Request-Method`, `Access-Control-Request-Headers`, `Accept-Encoding` |
| `server` | `cloudflare` |
| `x-render-origin-server` | `Render` |

No observados: `Content-Security-Policy`, `Referrer-Policy`, `Permissions-Policy`.

**Interpretación.** HSTS, anti-sniffing y anti-framing están presentes en runtime pese a que no hay bloque `.headers(...)` explícito: provienen de los defaults de Spring Security más la capa de edge.

`x-xss-protection: 0` **es el valor correcto y deseado**, no una omisión. El filtro XSS legacy de los navegadores introducía vulnerabilidades propias y está deprecado; Spring Security lo desactiva explícitamente por defecto desde hace varias versiones. Se anota para que no se lea como hallazgo en una revisión futura.

La ausencia de CSP sobre una API que devuelve JSON no es relevante — CSP gobierna la carga de recursos en un contexto de documento, y acá no hay documento. `Referrer-Policy` y `Permissions-Policy` tampoco aportan sobre respuestas JSON consumidas por `fetch`/axios.

**Los headers que sí importan son los del frontend**, que es donde hay documento HTML y contexto de navegación → 5.8.

`server: cloudflare` + `x-render-origin-server: Render` confirman la cadena de proxy sobre la que descansa el fix de F3-3.1 (uso de `CF-Connecting-IP` como fuente única de IP en producción). Consistente, sin regresión → pero ver el gate GL-03, que es donde esa cadena puede cambiar.

### 5.7 CORS — preflights reales

**Origen legítimo** (`https://gym-planner-theta.vercel.app`), `OPTIONS /api/public/ping`:

- 200 OK
- `access-control-allow-origin: https://gym-planner-theta.vercel.app`
- métodos permitidos explícitos
- `access-control-allow-headers: Content-Type`
- expose: `Authorization`, `Content-Disposition`

**Origen falso** (`https://evil-example.com`):

- **403 Forbidden**, `Invalid CORS request`
- **no** aparece `Access-Control-Allow-Origin` para ese dominio

**Conclusión: CORS funciona correctamente en runtime, no solo en la configuración declarada. OK.**

Aclaración para lecturas futuras: que el preflight legítimo haya respondido `access-control-allow-headers: Content-Type` **no indica** que `Authorization` esté ausente de la configuración. El preflight solo pidió `Content-Type`, y Spring responde reflejando lo solicitado dentro de lo permitido. La configuración incluye `Authorization` (ver 4.4) y el frontend autentica correctamente contra la API, lo que lo demuestra de hecho.

**Nota menor:** `Retry-After` no figura en `exposedHeaders`. Solo sería relevante si el frontend quisiera leer programáticamente el tiempo de espera devuelto por el rate limiter del Frente 3 para mostrarlo en pantalla. Es funcional, no de seguridad. No reabre F3.

### 5.8 Headers del frontend — prueba runtime

`curl -I https://gym-planner-theta.vercel.app`:

- 200 OK
- `Strict-Transport-Security: max-age=63072000; includeSubDomains; preload`
- `Access-Control-Allow-Origin: *`
- `Cache-Control: public, max-age=0, must-revalidate`
- `Server: Vercel`

**Ausentes:** `Content-Security-Policy`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy`, `X-Content-Type-Options`.

**El `Access-Control-Allow-Origin: *` no es un hallazgo.** Corresponde al HTML estático público servido por Vercel, no a la API. No representa bypass alguno del CORS del backend: los permisos de lectura cross-origin sobre la API los define exclusivamente el backend, que ya se verificó restrictivo en 5.7.

**No corresponde inflar esto en cinco vulnerabilidades separadas.** El residual materialmente relevante es uno solo:

> **Falta de protección anti-framing en el frontend.** Sin `X-Frame-Options` ni `frame-ancestors` en CSP, la SPA puede ser embebida en un iframe de un sitio de terceros, habilitando clickjacking sobre acciones destructivas o sobre el formulario de login.

CSP es el segundo en relevancia, y va junto con el anterior porque `frame-ancestors` es la forma moderna de resolver el primero.

**No tiene sentido configurar esto sobre Vercel si el frontend definitivo será Cloudflare Pages.** Va como gate de go-live GL-04, no como fix ahora.

### 5.9 Sourcemaps — prueba runtime

DevTools → Network → filtro `.map` → recarga completa: **no aparecieron archivos `*.js.map`**. No hay sourcemaps públicos en la demo. **OK.**

### 5.10 `console.error` de Axios — intento de reproducción

El Paso 0 había detectado `console.error(error)` sobre objetos de error completos. Un objeto de error de Axios puede contener `config.headers.Authorization` y el cuerpo de la respuesta.

Se intentó reproducir: navegación offline, acciones que requieren API, apertura de rutina existente, generación de PDF con red desconectada.

Resultado: los datos no cargan (correcto), la generación de PDF queda colgada en "Generando PDF...", **no apareció `AxiosError` visible en consola**, y no se pudo confirmar exposición de `Authorization`, request body, response ni `internalNotes`.

**Hallazgo estático no reproducido en runtime.**

Clasificación: **HARDENING BAJO**, y conviene explicitar *por qué* es bajo y no medio: aun en el peor caso, lo que se imprimiría es un token en la consola del navegador **del mismo usuario que ya tiene ese token en `localStorage`**. No hay boundary víctima/atacante — es autoexposición, no fuga. Residual 7.4.

---

## 6. Regresión de frentes anteriores

Verificación de que los fixes previos siguen vigentes en `application-prod.yml` tras los merges sucesivos. Esto no reabre esos frentes: confirma que no se perdieron.

### 6.1 Frente 2 — exposición de datos sensibles

| Property | Estado |
|---|---|
| `server.error.include-message` | `never` ✓ |
| `server.error.include-stacktrace` | `never` ✓ |
| `server.error.include-binding-errors` | `never` ✓ |
| `server.error.include-exception` | `false` ✓ |
| Loggers Hibernate SQL / bind / extract | `WARN` ✓ |

**Sin regresión.** Refuerzo relevante desde el runtime: no hay override de `LOG_LEVEL` en Render, y aunque lo hubiera, el pineo de los loggers de Hibernate en `WARN` es exactamente la defensa diseñada en F2 para que un `LOG_LEVEL=DEBUG/TRACE` no cascadee valores de parámetros a los logs.

Confirmación adicional obtenida en este frente: los múltiples 500 provocados durante las pruebas de 5.4 y 5.5 devolvieron **siempre** el mismo cuerpo genérico, sin mensaje de excepción ni stacktrace. Es evidencia empírica de que el hardening de F2 funciona en el sink real, no solo en la configuración declarada.

### 6.2 Frente 3 — auth y sesión

| Property | Estado |
|---|---|
| `server.forward-headers-strategy` | `framework` (prod) ✓ |
| `gymplanner.security.client-ip-source` | `cloudflare` (prod) / `remote-address` (dev, test) ✓ |

**Sin regresión.** La cadena de proxy que hace válido este diseño quedó confirmada en runtime por los headers `server: cloudflare` y `x-render-origin-server: Render` (5.6). Ver gate GL-03 para la condición que la invalidaría.

---

## 7. Residuales diferidos

Ninguno se implementa en este frente. Cada uno con su condición concreta de revaluación.

### 7.1 `.env.production` no cubierto por `.gitignore`

**Estado:** `.env` y `.env.local` están ignorados; `.env.production` y `frontend/.env.production` no. No existen esos archivos, no hay fuga.
**Clasificación:** HARDENING BAJO.
**Riesgo:** commit accidental futuro de un archivo con credenciales de producción — que, una vez commiteado, queda en la historia de Git aunque se borre después.
**Trigger:** al agrupar hardenings baratos (candidato natural para el Frente 6), o antes de que alguien genere un `.env.production` local.
**Fix previsto:** ampliar el patrón de `.gitignore` a `.env.*` con excepción explícita de `.env.example`.

### 7.2 Dockerfile sin directiva `USER`

**Estado:** el proceso corre como root dentro del contenedor.
**Clasificación:** HARDENING BAJO.
**Por qué no es bloqueante:** no hay exploit demostrado; correr como root agrava un escape de contenedor o una ejecución arbitraria, pero no es en sí una vía de entrada. Ninguno de los cinco frentes anteriores encontró un vector de ejecución.
**Trigger:** al agrupar hardenings baratos, o si aparece cualquier hallazgo que implique ejecución de código en el contenedor.
**Fix previsto:** usuario no privilegiado en el stage de runtime del Dockerfile.

### 7.3 La aplicación usa el rol owner de la base de datos

**Estado:** un único rol (`neondb_owner`), owner de `neondb`. La aplicación opera con permisos de DDL completo.
**Clasificación:** HARDENING / mínimo privilegio.
**Por qué no es bloqueante en V1:** no hay SQL injection (verificado en el Frente 4) ni vector conocido que permita ejecutar SQL arbitrario. El permiso excesivo amplifica un hallazgo futuro; no crea uno.
**Costo del fix:** no trivial — hay que separar el ciclo de Flyway (necesita DDL) del runtime (solo DML), lo que implica dos credenciales y ajustes de deploy.
**Trigger:** migración a Neon Launch (momento natural para reconfigurar roles), o aparición de cualquier hallazgo que involucre SQL dinámico.

### 7.4 `console.error` con objetos de error completos en el frontend

**Estado:** ver 5.10. Detectado estáticamente, no reproducido en runtime.
**Clasificación:** HARDENING BAJO. Sin fuga remota demostrada y sin boundary víctima/atacante.
**Trigger — este es el que importa:** si se incorpora **observabilidad remota del frontend** (Sentry, LogRocket, cualquier agregador de logs de navegador), la clasificación cambia de inmediato: lo que hoy queda en la consola local del propio usuario pasaría a viajar a un tercero, y ahí sí podría incluir tokens y datos de alumnos. En ese momento hay que reemplazar el logging de objetos completos por logging selectivo.

### 7.5 Health Check Path sin configurar en Render

**Estado:** el servicio no tiene Health Check Path definido.
**Clasificación:** hardening operativo, **no seguridad**.
**Trigger:** go-live (GL-06).
**Nota:** el endpoint elegido debe ser coherente con lo ya decidido en infraestructura — `/api/public/ping` no toca la base de datos, mientras que `/actuator/health` sí la toca vía `DataSourceHealthIndicator`. La elección tiene implicancias de consumo de compute en Neon y de comportamiento ante autosuspend.

### 7.6 Rutas no mapeadas devuelven 500 en lugar de 404

**Estado:** verificado empíricamente sobre las rutas de springdoc (5.4) y sobre los diez endpoints de Actuator no registrados (5.5). El `GlobalExceptionHandler` captura la excepción que produce una ruta sin handler y la traduce a un 500 genérico.

**Alcance real:** no es un comportamiento específico de Swagger ni de Actuator. Afecta a **cualquier** ruta no mapeada de la aplicación.

**Clasificación:** DEUDA BAJA de error handling. **No es un problema de seguridad**: el cuerpo es genérico y no filtra nada (gracias al hardening de F2), y un 500 no le da a un atacante información útil que un 404 no le daría.

**Por qué igual conviene arreglarlo, y por qué corresponde al Frente 6:** el impacto real es de **observabilidad y disponibilidad**, no de confidencialidad. Cualquier bot escaneando rutas comunes —cosa que ocurre permanentemente contra cualquier host público— genera un flujo constante de 500s. Eso contamina logs y métricas, y sobre todo **hace mucho más difícil detectar un 500 genuino** entre el ruido. Si en algún momento se configuran alertas por tasa de error, este comportamiento las vuelve inútiles.

**Efecto lateral menor, no buscado:** la uniformidad de respuesta dificulta marginalmente la enumeración de rutas, porque no se distingue "existe" de "no existe". No es una defensa diseñada y no debe contarse como tal.

**Trigger:** Frente 6, junto con el resto del trabajo de disponibilidad y monitoreo. Alternativa que resolvería esto y la deuda de springdoc de una sola vez: manejar explícitamente la excepción de ruta no encontrada en el `GlobalExceptionHandler` devolviendo 404, y quitar del `SecurityConfig` los matchers `permitAll()` de springdoc, que en `prod` no protegen nada porque no hay nada registrado detrás.

---

## 8. Go-Live Gates

Verificaciones que **no bloquean el cierre de F5** pero que **sí bloquean la carga de datos reales**. Se ejecutan en el Go-Live Security Check, después del Frente 6 y sobre la infraestructura definitiva.

El Go-Live Security Check **no reaudita**: verifica invariantes ya definidas sobre infraestructura nueva.

| ID | Gate | Por qué | Criterio de aprobación |
|---|---|---|---|
| **GL-01** | `SPRING_PROFILES_ACTIVE=prod` en el entorno definitivo | Toda la postura de seguridad del backend cuelga de esta variable; el default es `dev`, cuyo JWT secret está en el repo | Variable presente y verificada. **Además: decidir si se agrega un mecanismo de fail-fast explícito** que impida arrancar en un entorno desplegado sin perfil `prod`, en lugar de depender de que la config de `dev` reviente por casualidad (ver 5.1) |
| **GL-02** | Actuator sensible con usuario autenticado — **CERRADO en este frente** (ver 5.5) | Se conserva el criterio escrito porque debe reverificarse sobre la infraestructura definitiva dentro de GL-10 | Con JWT válido de OWNER, ningún endpoint sensible devuelve payload de Actuator. **404 y 500 genérico son ambos resultados válidos; un 200 con contenido es bloqueante inmediato.** El discriminador es el contraste con `/actuator/health`, que sí debe responder 200 |
| **GL-03** | Cadena de proxy y `CF-Connecting-IP` | El fix de F3-3.1 asume que el header lo setea el Cloudflare **de Render**. Si se agrega un dominio propio proxeado por una cuenta **propia** de Cloudflare delante de Render, el Cloudflare de Render vería como cliente al edge de la cuenta propia: todos los usuarios legítimos colapsarían en pocas IPs y **compartirían bucket de rate limiting**, bloqueando gente inocente | Reproducir la prueba runtime de F3-3.1 sobre la infraestructura definitiva: XFF falsificado no evade, dos clientes reales distintos obtienen buckets distintos. **No cerrar por lectura de configuración** |
| **GL-04** | Headers de seguridad del frontend definitivo | Anti-framing ausente hoy (ver 5.8). No tiene sentido configurarlo sobre Vercel si el destino es Cloudflare Pages | En Cloudflare Pages: anti-framing (`frame-ancestors 'none'` o `X-Frame-Options: DENY`), CSP, `X-Content-Type-Options`, `Referrer-Policy`. HSTS ya presente |
| **GL-05** | Restricción de acceso de red a Neon | El endpoint de base de datos es alcanzable desde internet; única defensa es credencial + TLS. El sistema va a guardar datos de salud | IP allowlist o VPC según lo que habilite Neon Launch. Si no se implementa, **dejar por escrito la aceptación explícita del riesgo**, no dejarlo implícito |
| **GL-06** | Health Check Path en Render | Ver 7.5 | Endpoint configurado y coherente con la decisión sobre consumo de compute en Neon |
| **GL-07** | CORS con el dominio frontend definitivo | `CORS_ALLOWED_ORIGINS` apunta hoy al dominio de Vercel | Actualizado al dominio definitivo y **verificado con preflight real**, origen legítimo y origen falso, como en 5.7 |
| **GL-08** | `VITE_API_URL` en el frontend definitivo | Cambia con la plataforma | Apunta al backend correcto; el bundle no contiene ninguna otra variable |
| **GL-09** | Backup, restore y retención | Neon Free retiene ~6 horas; insuficiente para operación con cliente | Retención acorde al plan definitivo y **restore probado al menos una vez**, no solo configurado |
| **GL-10** | Reverificación de las invariantes ya cerradas | Cambia la infraestructura, no el código | Swagger no expuesto · `/actuator/health` mínimo · endpoints sensibles de Actuator sin payload (anónimo y autenticado) · TLS a Neon · headers del backend · JWT secret real y externo |

**Nota no relacionada con seguridad, pero relevante para el go-live:** el plan Hobby de Vercel restringe el uso comercial. La migración a Cloudflare Pages ya está prevista por otros motivos y resuelve también esa cuestión; se deja anotado para que la decisión no dependa solo de criterios técnicos.

---

## 9. Clasificación final

| # | Ítem | Clasificación | Evidencia | Acción |
|---|---|---|---|---|
| 1 | Perfil `prod` activo en Render | OK / NO BUG | Runtime (5.1) | Cerrado · gate GL-01 |
| 2 | `JWT_SECRET` externo, sin default inseguro | OK / NO BUG | Repo + runtime (4.1, 5.1) | Cerrado |
| 3 | Credenciales de DB externas | OK / NO BUG | Repo + runtime (5.1) | Cerrado |
| 4 | Swagger / OpenAPI en producción | OK / NO BUG | Runtime (5.4) | Cerrado |
| 5 | `/actuator/health` público y mínimo | OK / NO BUG | Runtime (5.5) | Cerrado |
| 6 | Actuator sensible: sin payload anónimo **ni autenticado** | OK / NO BUG | Repo + runtime anónimo y autenticado (5.5) | **Cerrado** |
| 7 | CORS restrictivo verificado con preflight real | OK / NO BUG | Runtime (5.7) | Cerrado · gate GL-07 |
| 8 | Headers de seguridad del backend | OK / NO BUG | Runtime (5.6) | Cerrado |
| 9 | TLS + channel binding hacia Neon | OK / NO BUG | Runtime (5.2) | Cerrado |
| 10 | Secretos en working tree e historia de Git | OK / NO BUG | Repo (4.6) | Cerrado |
| 11 | Regresión F2 (errores y logging) | Sin regresión | Repo + runtime (6.1) | Cerrado |
| 12 | Regresión F3 (forward headers, client IP) | Sin regresión | Repo + runtime (6.2) | Cerrado · gate GL-03 |
| 13 | `ddl-auto=validate`, Flyway, `show-sql=false` | OK / NO BUG | Repo (4.7) | Cerrado |
| 14 | Sourcemaps no publicados | OK / NO BUG | Runtime (5.9) | Cerrado |
| 15 | Anti-framing y CSP del frontend | HARDENING MEDIO | Runtime (5.8) | Diferido · gate GL-04 |
| 16 | Neon sin IP restrictions / VPC | HARDENING MEDIO | Runtime (5.2) | Diferido · gate GL-05 |
| 17 | Rol de DB con permisos de owner | HARDENING | Runtime (5.2) | Diferido (7.3) |
| 18 | `.env.production` fuera de `.gitignore` | HARDENING BAJO | Repo (4.6) | Diferido (7.1) |
| 19 | Dockerfile sin `USER` | HARDENING BAJO | Repo (4.9) | Diferido (7.2) |
| 20 | `console.error` con objetos completos | HARDENING BAJO | Repo + runtime (5.10) | Diferido (7.4) |
| 21 | Health Check Path vacío en Render | Operativo, no seguridad | Runtime (5.1) | Diferido · gate GL-06 |
| 22 | Rutas no mapeadas devuelven 500 en lugar de 404 | DEUDA BAJA (observabilidad) | Runtime (5.4, 5.5) | Diferido a F6 (7.6) |
| 23 | `Retry-After` no expuesto en CORS | Funcional, no seguridad | Runtime (5.7) | Diferido |

**Totales: 0 brechas activas · 0 defensas ausentes explotables · 0 bloqueantes de la demo · 0 cambios de código.**

---

## 10. Limitaciones de este frente

1. **La configuración verificada es la de la infraestructura de demo**, que va a ser reemplazada. Por eso existe la sección 8: F5 valida un estado que se sabe transitorio, y deja las invariantes escritas para revalidarlas sobre el estado definitivo.
2. **Algunas verificaciones son observaciones puntuales en el tiempo**, no garantías continuas. Una variable de entorno puede cambiarse en cualquier momento desde la consola de Render sin dejar rastro en el repositorio. Contra eso no hay auditoría estática posible: hay disciplina operativa y un chequeo de go-live.
3. **La conclusión sobre Actuator se apoya en un razonamiento por contraste**, no en un 404 explícito (ver 5.5). La combinación de evidencia estática (`exposure.include: health`), respuesta correcta de `/actuator/health` y ausencia total de payload en los diez endpoints restantes con token válido es consistente y suficiente, pero el comportamiento de error handling descrito en 7.6 impide obtener la señal semántica directa. Resolver 7.6 haría esta verificación trivialmente legible en el futuro.
4. **No se auditaron dependencias ni supply chain.** Corresponde al Frente 6.
5. **El alcance excluye deliberadamente** lo cerrado en F1–F4. Este frente solo verificó que la configuración de F2 y F3 no hubiera sufrido regresión.

---

## 11. Conclusión

**No se encontraron brechas activas ni defensas ausentes explotables en el Frente 5.**

El frente cierra **sin cambios de código**. Las invariantes críticas de configuración productiva fueron verificadas contra el runtime real de la demo, no solo leídas del repositorio: perfil `prod` activo, secretos externos, Swagger no expuesto, Actuator sin exposición de datos ni anónima ni autenticada, CORS restrictivo con preflight real, TLS obligatorio hacia la base de datos, y los fixes de F2 y F3 vigentes sin regresión.

Quedan seis residuales de hardening diferidos con trigger explícito (sección 7) y diez gates de go-live (sección 8), de los cuales GL-02 quedó resuelto dentro de este mismo frente.

**Este cierre no habilita la carga de datos reales.** Falta el Frente 6, el montaje de la infraestructura definitiva y el Go-Live Security Check.

**Siguiente paso: Frente 6 — dependencias, supply chain, disponibilidad y cierre final de la auditoría.**
