# Auditoría de Seguridad — Frente 1: Aislamiento Multi-Tenant e IDOR

> Acta de cierre del Frente 1 de la auditoría de seguridad de Gym Planner,
> realizada antes de cargar datos reales de clientes (incluye datos de salud:
> lesiones y notas). Documenta qué se auditó, qué se corrigió y qué deuda
> quedó registrada para el futuro.
>
> Rama de trabajo: `feature/security-tenant-hardening`
> Estado: **cerrado**. Próximo: Frente 2 (exposición de datos sensibles).

## 1. Alcance y objetivo

El Frente 1 verificó que **ningún gimnasio pueda ver ni modificar datos de
otro gimnasio**, y que los recursos anidados validen la cadena completa de
pertenencia (recurso → padre → gym), no solo el gym. Multi-tenancy en Gym
Planner es por `gymId`, obtenido del `GymPrincipal` del usuario autenticado.
El JWT contiene un claim `gymId`, pero el flujo de autenticación actual extrae
el email del token y carga desde la base el usuario y su gym.

## 2. Diagnóstico: enforcement AD-HOC

La conclusión estructural de la auditoría (Paso 0 de lectura) fue que el
aislamiento por gym se aplica de forma **ad-hoc**: existe representación
centralizada del tenant (`CustomUserDetailsService.GymPrincipal`, cuyo `gymId`
se obtiene del usuario persistido), pero **cada service y cada query filtra por
`gymId` manualmente**. No hay mecanismo central de enforcement: no hay
Hibernate `@Filter`, ni Row-Level Security (RLS) en PostgreSQL, ni base
repository, ni interceptor/aspect de tenant.

**Implicancia:** cada endpoint queda bien o mal según su propia
implementación. No hay una red de seguridad global que atrape un método nuevo
que se olvide de filtrar por gym. Por eso el entregable principal de este
frente NO fue una refactorización, sino una **suite de tests de regresión
cross-gym** (sección 4).

No se encontró ninguna fuga cross-gym directa en el código auditado. El
aislamiento estaba correctamente aplicado, solo que sin protección automática
contra regresiones futuras.

## 3. Correcciones aplicadas

### 3.1 Query-scoping de `getFull` (commit: refactor de query scoping)

`RoutineService.getFull` y `TemplateService.getFull` cargaban la entidad por
ID con una query sin filtro de gym y validaban el gym **después**, en memoria
(check post-load con 404 si no coincidía). No era una fuga (el dato nunca salía
del servidor), pero era frágil: si alguien borraba ese `if` en un refactor, la
entidad de otro gym se expondría.

Se movió el filtro a la propia query (`findByIdWithFullStructure(id, gymId)`),
replicando el patrón que `RoutinePdfService` ya usaba. En routine la sobrecarga
scopeada ya existía y solo se cambió la llamada; en template se agregó,
filtrando por el path directo `t.gym.id = :gymId`.

### 3.2 Hardening del lifecycle en `RoutineService.update` (commit: fix lifecycle)

`update` presentaba mass assignment de integridad de dominio (no de tenant, no
de fuga de datos):

- `setFinishedDate(request.finishedDate())` era **incondicional**, permitiendo
  dejar una rutina ACTIVE/DRAFT con `finishedDate` incoherente (futuro, o
  seteada sin corresponder).
- Se podía transicionar a FINISHED/ARCHIVED por la vía de `update`,
  salteándose los endpoints de lifecycle (`/finish`, `/archive`) que setean la
  auditoría de cierre (`finishedByUser`, `finishedAt`, `closureNotes`).

Corrección: `update` ahora solo mantiene el estado actual o permite la
transición DRAFT→ACTIVE (que preserva el invariante de una sola rutina ACTIVE
por alumno vía `finishPreviousActive`). Cualquier otra transición se rechaza
con 422. `finishedDate` dejó de ser asignable por `update` y se removió del
`UpdateRoutineRequest`.

Nota: el invariante "una sola rutina ACTIVE por alumno" ya estaba protegido
correctamente por `finishPreviousActive` en todos los caminos; el mass
assignment NO lo rompía. Solo afectaba coherencia de `finishedDate` y
trazabilidad del cierre.

## 4. Red de regresión: tests cross-gym

El entregable central. Cubre dos capas:

### 4.1 Nivel service (PostgreSQL real vía Testcontainers)

Extienden `PostgresIntegrationTest`. Verifican que cada service filtra por el
`gymId` recibido. Un gym A no puede acceder a recursos de un gym B (se espera
**404**, no 403, para no revelar existencia cross-gym).

- `StudentCrossTenantIntegrationTest` + `StudentInjuryNoteCrossTenantIntegrationTest`
- `ExerciseCrossTenantIntegrationTest` + `ExerciseTagCrossTenantIntegrationTest`
- `TemplateCrossTenantIntegrationTest`
- `RoutineCrossTenantIntegrationTest` + `RoutineHistoryPdfCrossTenantIntegrationTest`

Casos cubiertos por módulo: acceso directo a recurso ajeno (get/update/
delete/etc.), IDOR de recurso anidado (student propio + recurso hijo ajeno),
referencias cruzadas en el body (tagIds/exerciseIds/templateId/studentId de
otro gym), aislamiento de listados (con grupo de control `.contains(propio)` +
`.doesNotContain(ajeno)`), y no-interferencia (unicidad scopeada por gym).

### 4.2 Nivel HTTP (controller, con principal simulado)

Siguen el patrón de los controller tests existentes (`@AutoConfigureMockMvc`,
`.with(user(principal()))`, perfil H2). Verifican el cableado
controller → principal → service (que el `gymId` salga del principal), que la
capa service no cubre.

- `RoutineControllerCrossTenantTest` (get / pdf / text)
- `StudentControllerCrossTenantTest`
- (tags ya cubierto en `ExerciseTagControllerTest`)

Todos incluyen control positivo (200 con recurso propio). El anti-falso-verde
posterior al 404 (verificar que el recurso sigue existiendo) está en
`StudentControllerCrossTenantTest` y en los casos cross-gym de
`ExerciseTagControllerTest`.

## 5. Falsos positivos reclasificados

Hallazgos del reporte inicial que NO son bugs de este frente:

- **`internalNotes` en `RoutineResponse`**: es la superficie autenticada del
  profesor (la edita en el editor). La regla no-negociable aplica a API
  pública / PDF / WhatsApp, y ahí se usa `generalNotes`, no `internalNotes`.
- **`activeInjuries` / notes en respuestas de alumno**: superficie autenticada
  correcta (los tabs de la ficha). No filtran a PDF/WhatsApp.
- **403 en delete note intra-gym**: no revela existencia nueva (el usuario ya
  puede listar esas notas). La regla 404-vs-403 aplica a cross-gym, no
  intra-gym.

Estas superficies se re-confirman con lupa en el **Frente 2**.

## 6. Deuda registrada

### Seguridad / arquitectura

- **Enforcement AD-HOC**: sin filtro central. Dirección post-V1: evaluar RLS en
  PostgreSQL, que haría las fugas cross-tenant estructuralmente imposibles.
- **Query `findByIdWithFullStructure(id)` sin gym, viva**: quedó en
  `RoutineRepository` porque la usan tres clases de test de rutina. Método
  inseguro latente. Acción: migrar esos tests a la sobrecarga con `gymId` y
  eliminarla.
- **Asimetría de validación de autor**: `StudentNoteService.create` valida el
  `authorUserId` contra el gym; `TemplateService`/`RoutineService` no validan
  `createdByUser` contra el gym. Hoy no explotable (el userId viene del
  principal autenticado), pero inconsistente defensivamente.

### Infraestructura de tests

- **Tests de rutina pre-auditoría en H2**: `RoutineLifecycleServiceTest`,
  `RoutineDuplicateServiceTest`, `RoutineFromTemplateServiceTest` usan H2 (modo
  PostgreSQL), no Postgres real. Considerar migrar a Testcontainers.
- **Seed con id explícito desincroniza la secuencia de identidad en H2**: los
  controller tests lo esquivan con inserts nativos `MAX(id)+1` repetidos.
  Considerar `ALTER SEQUENCE ... RESTART` en el seed de test o un helper
  compartido.

### Drifts doc-vs-código (para sincronizar el design doc, no de seguridad)

- `adjust-weights` se implementó como `WeightAdjustmentInput(percentage,
  roundingStepKg)` vía `create-next`, no como el `scopeType/scopeId/preview`
  del doc (CU-11).
- `RoutineValidator`/`TemplateValidator` exigen secciones canónicas
  (warmup+main+cooldown por día); el doc solo pide "al menos un bloque".
- `DELETE injuries` hace soft-delete (`deactivate`); el design doc pide CRUD,
  pero no documenta si la eliminación debe ser física o lógica.
- `targetWeightKg` se descarta en silencio en plantillas (`addSets` fuerza
  `null`); el DTO conserva el campo.

## 7. Próximo frente

**Frente 2 — Exposición de datos sensibles**: verificar con lupa que
`internalNotes`, `StudentInjury` y `StudentNote` no se filtren en respuestas de
API, PDF, texto WhatsApp ni logs. Es el frente que toca directamente la regla
no-negociable de privacidad y los datos de salud.
