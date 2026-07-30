# Frente 2 — Exposición de datos sensibles

> Auditoría de seguridad de Gym Planner, previa a la carga de datos reales de clientes (que incluyen datos de salud). Este documento cierra el Frente 2. El Frente 1 (aislamiento multi-tenant + IDOR) está cerrado y mergeado a `main`.

## Alcance

Verificar que cuatro clases de dato sensible no se filtren por ninguna superficie de salida:

- **S1** — `routine.internalNotes`
- **S2** — `routine.closureNotes`
- **S3** — `StudentInjury` (todos sus campos: `bodyArea`, `description`, `notes`) — dato de salud
- **S4** — `StudentNote.content`

Regla del proyecto, no negociable: S1–S4 nunca deben aparecer en PDF, texto de WhatsApp, logs ni superficies no autorizadas. En la API autenticada del profesor son visibles por diseño (validado en el Frente 1); el foco de este frente es **dónde más aparecen**.

Superficies auditadas:

- (a) Respuestas de API, incluyendo anidadas y de listado/summary.
- (b) PDF (Flying Saucer + Thymeleaf).
- (c) Texto de WhatsApp.
- (d) Logs (mensajes de log, excepciones, SQL/bind logging de Hibernate).
- (e) Mensajes de error (incluido el rejected value de Bean Validation).
- (f) Superficies visibles para roles que no deberían verlas.

Explícitamente **fuera de alcance** de este frente (derivados a otros): SSRF vía `logoUrl` y escaping general de Thymeleaf (Frente 4); configuración general de logging y CORS/actuator (Frente 5). El ángulo "¿el binding SQL logea contenido de datos sensibles?" sí se auditó acá por ser fuga de dato sensible, aunque su fix de configuración caiga naturalmente en la línea del Frente 5.

## Metodología

Se ejecutó un Paso 0 de lectura (solo lectura, sin editar) que inventarió: todos los DTOs de response y su contenido S1–S4 directo/transitivo, los mappers de Routine y Student, el contexto de Thymeleaf del PDF, el service de WhatsApp, todas las llamadas a `log.*`, las excepciones de dominio, el `GlobalExceptionHandler`, la configuración de errores por perfil, el inventario exhaustivo de `permitAll()` y la cobertura de tests de privacidad existente. Sobre esa evidencia se priorizaron los hallazgos y se implementaron los fixes.

## Resultado central

**No se encontraron fugas activas de S1–S4 hacia PDF, WhatsApp, logs ni rutas públicas. Cero hallazgos críticos.** Todo lo detectado fueron fragilidades de configuración o gaps de test de severidad media/baja, que se convirtieron en guardias de regresión y hardening de configuración.

Aciertos de diseño confirmados durante la auditoría (no solo ausencia de bug):

- **PDF y WhatsApp comparten un único punto de proyección**, `RoutinePdfService.buildDto(routineId, gymId)`. Las dos superficies de entrega se alimentan del mismo DTO gym-scoped, lo que protege ambas de una sola vez. Es el patrón "proyectar antes de renderizar": al contexto de Thymeleaf llega `PdfRoutineDto`, no la entidad `Routine`, de modo que la barrera de privacidad vive en el código, no en una convención del template.
- **Los mappers de Routine y Student son manuales** (constructores explícitos), no MapStruct. No existe mapeo automático por coincidencia de nombre que pudiera arrastrar un campo sensible a un summary por descuido. `RoutineMapper.toSummary` y `StudentMapper.toSummary` no incluyen S1–S4.
- **Ningún controller devuelve entidades JPA**; todos devuelven DTOs.

## Hallazgos

| ID | Severidad | Prioridad | Hallazgo | Estado |
|---|---|---|---|---|
| F2-01 | Media | Pre-producción | Los records de request/response sensibles tienen `toString()` autogenerado con S1–S4. No hay hoy ningún log/excepción que lo invoque. | Mitigado por política + test de regresión (ver decisión abajo) |
| F2-02 | Baja (condicional) | Pre-producción | El logger de bind de JDBC no estaba fijado; un `LOG_LEVEL=TRACE` externo podía habilitar el logueo de valores por herencia de `root`. | Cerrado (F2-1) |
| F2-03 | Baja | Pre-producción | `server.error.include-*` dependía de los defaults de Boot 3.5.13 en vez de estar fijado explícitamente. | Cerrado (F2-1) |
| F2-04 | Baja | Pre-producción | El PDF no tenía test de regresión para S2 (`closureNotes`). | Cerrado (F2-2) |
| F2-05 | Baja | Pre-producción | WhatsApp no tenía test de regresión para S2 (`closureNotes`). | Cerrado (F2-2) |
| F2-06 | Baja | Pre-producción | No había tests de privacidad para logs ni para propagación de secretos por errores. | Cerrado (F2-2) |

Nota sobre severidad vs. prioridad: varios hallazgos son de severidad baja o condicional como vulnerabilidad actual, pero se trataron como prioridad pre-producción por bajo costo de fix y alto impacto potencial una vez que la base tenga datos de salud reales. Un hallazgo puede ser "baja severidad hoy" y a la vez "hacer antes de cargar datos reales".

## Fixes aplicados

### F2-1 — Hardening de configuración de logging y errores en producción

Cambios en `application-prod.yml` (solo configuración, sin lógica):

- Loggers de Hibernate que pueden emitir SQL y valores fijados a `WARN` de forma no parametrizable, para que un bump de `LOG_LEVEL` en `root` no los pueda cascadear a DEBUG/TRACE: `org.hibernate.SQL`, `org.hibernate.orm.jdbc.bind`, `org.hibernate.orm.jdbc.extract`.
- Se omitieron deliberadamente los loggers legacy de Hibernate 5 (`BasicBinder`/`BasicExtractor`): el proyecto corre Hibernate 6, donde están inertes; agregarlos sería configuración muerta.
- `server.error.include-message`, `include-stacktrace`, `include-binding-errors` fijados en `never`, e `include-exception` en `false` — fijando explícitamente lo que hoy es default seguro, para sobrevivir a un futuro upgrade de Boot que cambie un default.
- `spring.jpa.show-sql: false` dejado explícito en el perfil.

Verificado: el deploy en Render corre con perfil `prod`, por lo que el fix protege el perfil que efectivamente tendrá los datos reales.

Distinción registrada: existen **dos sinks de fuga a logs distintos**, con mitigaciones distintas. El sink de Hibernate logueando SQL/bind/extract se mitiga con esta configuración (F2-1). El sink de código de aplicación logueando un DTO/entidad/request se mitiga con el test de regresión de F2-2. Uno no cubre al otro.

### F2-2 — Tests de privacidad (regresión)

Siete tests nuevos, sin modificar código productivo:

- **T1 — `closureNotes` (S2) fuera de PDF y WhatsApp.** Un test nuevo en `RoutinePdfServiceTest` y otro en `WhatsAppTextServiceTest`. Cada uno finaliza la rutina con un `closureNotes` centinela pasando por las transiciones de dominio válidas (sin forzar estado por repository), hace una **comprobación positiva** de que el centinela quedó persistido en la superficie autenticada, y recién entonces verifica que no aparece en la salida. La comprobación positiva evita el test que pasa trivialmente por un dato que nunca se guardó.
- **T2 — S1–S4 fuera de los logs de aplicación.** Clase nueva `SensitiveDataLoggingTest`, con cuatro métodos focalizados (uno por dato, para que el fallo sea específico). Captura con `ListAppender` de Logback sobre el logger `com.gymplanner` forzado a `TRACE`, de modo que un futuro `log.debug(dto)` quede capturado aunque la consola esté en INFO. El assert inspecciona `getFormattedMessage()` (el mensaje ya renderizado con los argumentos resueltos, no el template) más el mensaje del throwable. Cada método ejercita el flujo real con un centinela único y hace comprobación positiva de que el dato entró antes de verificar que no se logueó. Setup/teardown en `@BeforeEach`/`@AfterEach` restaura el nivel y desengancha el appender aunque el test falle. Cubre logs de aplicación; el logging de Hibernate queda cubierto por F2-1.
- **T3 — El handler de validación no refleja el rejected value.** Un test en `GlobalExceptionHandlerTest` que construye un `FieldError` con `rejectedValue` centinela y `defaultMessage` seguro, llama directo al handler (sin MockMvc) y verifica que el `ApiError` contiene solo el mensaje seguro y que su `toString()` completo no contiene el centinela. Guardia contra una futura "mejora" que agregue el valor rechazado a los mensajes de error.

Confirmado en la revisión del código: `finishRoutine` sobre una rutina `ACTIVE` es transición válida (`RoutineService` solo rechaza estados distintos de `ACTIVE`/`DRAFT`), por lo que la comprobación positiva de T2 pasa por un flujo legítimo.

Suite completa en verde (328 tests, con Docker/Testcontainers operativo). Cero cambios en `backend/src/main`.

## Decisión de política: F2-01 (toString de records)

Se decidió **no** customizar un `toString()` redactado en los ~10 records que transportan S1–S4. Redactar en cada record es verborrágico, fácil de olvidar en records nuevos y no escala como política. En su lugar se ataca el **sink**, no la fuente:

- El test de regresión de logs (T2) falla si algún flujo llega a loguear un DTO/entidad/request con datos sensibles.
- Política registrada para el futuro: cuando se agregue logging operativo/de errores (hoy inexistente, necesario para operar en serio), ese logging debe registrar tipo de excepción y stacktrace, **nunca** el DTO/request completo.

La implementación del logging de errores en sí es un concern aparte (operabilidad / Frente 6), no de este frente.

## Re-confirmaciones arrastradas del Frente 1

El Frente 1 dejó marcadas, para re-confirmar con lupa acá, varias reclasificaciones como no-bug. Confirmadas correctas:

- **`internalNotes` (S1) en `RoutineResponse`**: es superficie autenticada del profesor, va por diseño. No es fuga.
- **`activeInjuries` / notas en las respuestas de alumno**: el profesor las debe ver; diseño correcto.
- **`closureNotes` (S2) en `StudentRoutineTimelineItemResponse`** dentro del timeline paginado: es la superficie privada del historial donde S2 va por diseño; endpoint autenticado y gym-scoped. No es fuga.
- **Responses compuestos** (`CreateNextRoutineResponse`, `FinishAndCreateNextResponse`) que embeben un `RoutineResponse` completo con S1/S2: salen de endpoints autenticados del profesor; correctos.
- **Inventario de `permitAll()`**: `/api/auth/login`, `/actuator/health`, `/api/public/ping` y swagger (solo en dev). Ninguna ruta pública toca datos de dominio S1–S4.

## Anotado para otros frentes

- **`PhoneCheckResponse` devuelve `studentName`** → superficie de enumeración (probar teléfonos y obtener nombres de clientes). No es S1–S4. Va al **Frente 3** (auth/enumeración). Confirmar además si el teléfono viaja como query param, porque los query strings caen en access logs.
- **Actuator `health` en `permitAll()`**: solo se expone `health`; falta confirmar `show-details` explícito (el default de Boot es `never`). Va al **Frente 5** (config de producción).
- **Backup con datos de salud**: cuando se definan backups de producción, un dump con lesiones reales es una superficie sensible (cifrado, ubicación, acceso). Va al frente de infraestructura / pre-producción. Conecta con la rotación pendiente de credenciales de Neon y la separación de infra demo/producción.

## Estado

Frente 2 **cerrado**. Fixes en la rama `security/frente-2-exposicion-datos-sensibles`:

- `chore(security): harden production error and SQL logging config` (F2-1)
- `test(security): add sensitive data regression coverage` (F2-2)

Merge a `main` con `--no-ff` (unidad coherente de historia, igual que el Frente 1). Siguiente: Frente 3 — auth y sesión (fuerza bruta, ciclo de vida del JWT, enumeración de usuarios), donde espera el `PhoneCheckResponse` anotado arriba.
