# Sistema de Gestión de Entrenamiento — Documento de Diseño (v2)

> Documento maestro del proyecto Gym Planner. Contiene visión, alcance, stack, modelo de dominio, arquitectura, plan de desarrollo, estrategia de deploy y referencia a los prompts de scaffolding para Codex. Se versiona en el repo como `docs/design.md`.
>
> **Cambios v2 (revisión post-feedback):**
> - Corrección de retención de PITR en Neon Free (no son 7 días).
> - `ON DELETE SET NULL` agregado en `routines.source_template_id`.
> - UNIQUE constraints de `order_index` y `set_number` en tablas dependientes.
> - UNIQUE en `(gym_id, document_id)` para alumnos.
> - Nueva sección de requisito UX responsive (notebook + celular).
> - Nuevo caso de uso: ajuste porcentual de pesos con redondeo configurable.
> - Privacidad explícita de lesiones, limitaciones y notas internas.
> - Versiones del stack actualizadas a 2026 (Spring Boot 3.5.x, React 19, Vite 8).
> - Estrategia de prompts a Codex segmentada en 4 etapas, no un mega prompt.
> - Generación automática de sets para casos simples (3×10×20 kg) en la UI.

---

## 1. Visión del producto

Sistema web para que un dueño/profesor de gimnasio pueda planificar, asignar y dar seguimiento a rutinas personalizadas. La V1 reemplaza el flujo actual de papel/Excel/WhatsApp desorganizado por una herramienta profesional donde el profesor puede armar plantillas reutilizables por deporte u objetivo, asignarlas a alumnos con personalización por persona, consultar historial de cargas anteriores y entregar la rutina al alumno como PDF profesional vía WhatsApp.

El sistema **no es un mini-ERP de gimnasio**. El núcleo es **gestión profesional de entrenamiento**. Cuotas, pagos, asistencia y mantenimiento de máquinas son explícitamente fuera de alcance de V1 y se reservan para versiones futuras.

### Diferenciadores clave para este cliente

Este gimnasio no es uno convencional. Trabaja con varias particularidades que el sistema debe contemplar nativamente, no como hacks o casos extremos:

- Sistemas de entrenamiento avanzados: pirámide invertida, drop sets, rest-pause, cluster sets, al fallo.
- Bloques tipo circuito por tiempo total ("circuito de 12 minutos con tres ejercicios rotando").
- Ejercicios polivalentes que pertenecen a múltiples categorías (un swing con mancuerna trabaja tren superior + core + activación).
- Estructura por bloques semánticos (entrada en calor, activación, fuerza, circuito, vuelta a la calma) que el alumno tiene que poder distinguir claramente en el PDF.
- Ajuste rápido de pesos por porcentaje sobre una rutina ya armada (subirle 5% al tren inferior cuando el alumno progresó), con redondeo a discos disponibles.

---

## 2. Stack tecnológico (versiones 2026)

### Backend

| Tecnología | Versión sugerida | Rol |
|---|---|---|
| Java | 21 LTS | Lenguaje |
| Spring Boot | 3.5.x latest patch | Framework |
| Spring Web | — | Controllers REST |
| Spring Data JPA | — | Persistencia |
| Hibernate | 6.x | ORM |
| Spring Security | — | Autenticación / autorización |
| JJWT | 0.12.x | Generación y validación de JWT |
| Bean Validation | Jakarta | Validaciones declarativas |
| Flyway | 10.x | Migraciones de base de datos |
| Maven | 3.9+ | Build |
| MapStruct | 1.5.x | Mapeo DTO ↔ Entity |
| Lombok | última | Reducción de boilerplate |
| Flying Saucer + Thymeleaf | — | Generación de PDF desde HTML |
| springdoc-openapi | 2.x | Swagger UI / OpenAPI 3 |
| Spring Boot Actuator | — | Healthchecks |

### Testing backend

| Tecnología | Rol |
|---|---|
| JUnit 5 | Framework de tests |
| Mockito | Mocks |
| AssertJ | Assertions expresivas |
| Spring MockMvc | Tests de controllers |
| Testcontainers | Tests de integración (V2 — opcional en V1) |

### Frontend

| Tecnología | Versión sugerida | Rol |
|---|---|---|
| React | 19.x | Librería UI |
| TypeScript | 5.x | Tipado estático |
| Vite | 8.x | Bundler / dev server |
| React Router | 6.x o sucesor estable | Navegación |
| TanStack Query | 5.x | Server state, cache, mutations |
| React Hook Form | 7.x | Formularios |
| Zod | 3.x | Validación de esquemas |
| Tailwind CSS | 3.x o 4.x | Estilos utilitarios |
| shadcn/ui | última | Componentes base |
| Axios | 1.x | Cliente HTTP |
| date-fns | 3.x | Manejo de fechas |
| lucide-react | última | Íconos |

> **Nota:** al iniciar el proyecto, Codex debe usar la última versión estable disponible al momento de generar el scaffolding. Las versiones de la tabla son una referencia mínima.

### Base de datos

PostgreSQL 16 o superior. Local en Docker, producción en Neon (plan Launch sugerido para retención adecuada — ver sección 12).

### Infraestructura y DevOps

| Servicio | Rol | Costo/mes (referencia) |
|---|---|---|
| GitHub | Repo, CI/CD, issues | Free |
| GitHub Actions | Automatización | Free para repos privados pequeños |
| Vercel | Deploy frontend | Free (Hobby) |
| Render | Deploy backend | ~$7 USD (Starter, sin sleep) |
| Neon | PostgreSQL administrado | Free (limitado) o ~$15 (Launch) |
| UptimeRobot | Monitoreo | Free |
| **Total operativo** | | **~$7-25 USD/mes** según plan de DB |

---

## 3. Arquitectura general

```
┌──────────────────────┐         ┌──────────────────────┐         ┌──────────────────────┐
│   Navegador cliente  │  HTTPS  │     Vercel (CDN)     │         │      GitHub (repo)   │
│   (PC o móvil)       │◄───────►│     React SPA        │         │   gym-planner repo   │
└──────────┬───────────┘         └──────────────────────┘         └──────────┬───────────┘
           │                                                                  │
           │ JWT en Authorization header                              git push │ → deploys
           ▼                                                                  ▼
┌──────────────────────┐                                          ┌──────────────────────┐
│   Render (backend)   │◄─────────────────────────────────────────│  GitHub Actions CI   │
│   Spring Boot API    │                                          │  (build + tests)     │
└──────────┬───────────┘                                          └──────────────────────┘
           │
           │ JDBC + SSL
           ▼
┌──────────────────────┐
│   Neon (PostgreSQL)  │
│   gym_planner_db     │
└──────────────────────┘
           ▲
           │ healthcheck cada 5 min
┌──────────┴───────────┐
│    UptimeRobot       │
└──────────────────────┘
```

### Capas del backend

```
controller    →   recibe HTTP, delega al service, devuelve DTO
service       →   lógica de negocio, transacciones, orquestación
repository    →   acceso a datos vía Spring Data JPA
entity        →   mapeo a tablas con JPA/Hibernate
dto           →   contratos de entrada/salida (request/response)
mapper        →   conversión entity ↔ dto (MapStruct)
config        →   configuración de Spring (Security, CORS, OpenAPI, etc.)
exception     →   excepciones de dominio + handlers globales
```

Regla dura: los controllers nunca tocan entities directamente, siempre DTOs. Los services nunca devuelven entities a la capa de controller, siempre DTOs.

### Organización por feature, no por capa técnica

```
com.gymplanner/
├── auth/
├── user/
├── gym/
├── student/
├── exercise/
├── template/
├── routine/
├── pdf/
├── shared/
└── config/
```

---

## 4. Requisito UX: responsive notebook + celular

La V1 **debe ser usable desde notebook y desde celular**. La experiencia principal se optimiza para notebook (donde el profesor arma plantillas y rutinas largas), pero las pantallas críticas deben funcionar correctamente en mobile:

- Login.
- Búsqueda y consulta de alumnos.
- Ficha del alumno (datos, lesiones, rutina actual, historial).
- Ver rutina actual y descargar PDF.
- Copiar texto WhatsApp.
- Consultar historial de cargas.
- Ajustes simples a rutinas (cambiar peso o repeticiones de un ejercicio puntual).

El **constructor avanzado de plantillas y rutinas** puede priorizar desktop/notebook (drag-and-drop, tablas grandes), pero en mobile no debe romperse: layout vertical, botones grandes, sin depender exclusivamente de hover ni de drag-and-drop.

Implicaciones técnicas:
- Diseño mobile-first en componentes de consulta y operación rápida.
- Sidebar colapsable a hamburger menu en mobile.
- Tablas con scroll horizontal cuando son densas, no truncado.
- Botones con tap target mínimo de 44px.
- Modales que no excedan la altura de la pantalla en mobile.

---

## 5. Modelo de dominio

### Entidades principales y relaciones

```
Gym (1) ──────────── (N) User
Gym (1) ──────────── (N) Student
Gym (1) ──────────── (N) Exercise
Gym (1) ──────────── (N) TrainingTemplate

Student (1) ──────── (N) Routine
Student (1) ──────── (N) StudentInjury
Student (1) ──────── (N) StudentNote

Exercise (N) ───────── (N) ExerciseTag           [tabla pivote ExerciseTagAssignment]

TrainingTemplate (1) ─ (N) TemplateBlock
TemplateBlock (1) ─── (N) TemplateExercise
TemplateExercise (1) ─ (N) TemplateExerciseSet

Routine (1) ────────── (N) RoutineBlock
RoutineBlock (1) ───── (N) RoutineExercise
RoutineExercise (1) ── (N) RoutineExerciseSet

Routine (N) ────────── (1) TrainingTemplate    [opcional, ON DELETE SET NULL]
TemplateExercise (N) ─ (1) Exercise            [referencia al catálogo]
RoutineExercise (N) ── (1) Exercise            [referencia al catálogo]
```

### Decisión clave: clasificación de ejercicios por tags

**No es jerárquica, es por tags múltiples**. Un ejercicio puede tener varios tags de cada tipo. El profesor filtra por uno o varios al buscar.

Tipos de tag (enum `TagType`):
- `BODY_AREA` — tren superior, zona media, tren inferior, cuerpo completo
- `MUSCLE_GROUP` — pecho, espalda, hombros, bíceps, tríceps, cuádriceps, isquios, glúteos, gemelos, core, etc.
- `MOVEMENT_PATTERN` — empuje vertical, empuje horizontal, tirón vertical, tirón horizontal, sentadilla, bisagra de cadera, zancada, rotación, anti-rotación, locomoción, isométrico, salto
- `OBJECTIVE` — fuerza, hipertrofia, potencia, resistencia, movilidad, prevención, técnica, activación
- `LEVEL` — iniciación, intermedio, avanzado
- `EQUIPMENT` — peso libre, mancuernas, barra, polea, máquina, banda, kettlebell, peso corporal, cajón, soga

Cada tag tiene `name`, `type`, `slug`. Un ejercicio se relaciona N:N con tags.

Para la UI esto se presenta como un **buscador con filtros laterales**, no como un árbol expandible.

### Decisión clave: bloques tipados con dos clasificaciones ortogonales

Cada bloque dentro de una rutina o plantilla tiene dos clasificaciones independientes:

1. **Tipo estructural** (`BlockStructuralType`) — define cómo se ejecutan los ejercicios:
   - `STANDARD` — series × reps tradicional
   - `CIRCUIT` — N ejercicios rotando durante un tiempo total
   - `PYRAMID` — peso/reps escalan ascendente
   - `REVERSE_PYRAMID` — peso/reps descienden
   - `DROP_SET` — al fallo + bajadas inmediatas
   - `REST_PAUSE` — micro-descansos dentro de la serie
   - `CLUSTER` — series fragmentadas

2. **Propósito semántico** (`BlockPurpose`) — para qué sirve el bloque dentro del día:
   - `WARMUP` — entrada en calor
   - `ACTIVATION` — activación neuromuscular
   - `MAIN_LIFT` — ejercicio principal
   - `ACCESSORY` — accesorios
   - `CONDITIONING` — acondicionamiento metabólico
   - `CORE` — zona media
   - `COOLDOWN` — vuelta a la calma
   - `OTHER` — texto libre del profesor

El **título del bloque** es texto libre que el profesor escribe ("Tren superior con pirámide", "Circuito 12 min", "Activación de glúteos"). Eso es lo que el alumno ve en el PDF.

### Decisión clave: sets explícitos para soportar pirámide y drop sets

Un `RoutineExercise` no tiene "series" como entero, sino una colección de `RoutineExerciseSet`, donde cada set puede tener parámetros distintos:

```
RoutineExercise (Sentadilla)
├── Set 1: 6 reps, 80 kg, descanso 90s
├── Set 2: 8 reps, 70 kg, descanso 90s
└── Set 3: 10 reps, 60 kg, descanso final
```

**UX para casos simples**: cuando el profesor está creando un ejercicio en un bloque STANDARD, la UI le ofrece un modo rápido:

```
[ Modo simple ]    Series: 3   Reps: 10   Peso: 20 kg   Descanso: 60s
                   [Generar sets]

[ Modo avanzado ]  Editar cada set individualmente (pirámide, drop set, etc.)
```

Al hacer click en "Generar sets", la UI crea internamente 3 sets idénticos. El profesor nunca ve esa complejidad si no la necesita.

Para CIRCUIT, el bloque tiene `totalDurationSeconds` y los ejercicios tienen un único set con el target (6 reps de salto al cajón) — el alumno los rota durante el tiempo del bloque.

Para REST_PAUSE y DROP_SET, los sets representan los micro-bloques dentro de la serie principal.

### Campos de `RoutineExerciseSet`

- `setNumber` (orden, 1-based)
- `setKind` — NORMAL, WARMUP, FAILURE, DROP, REST_PAUSE_PORTION
- `targetReps` (Integer, nullable)
- `targetRepsMin` / `targetRepsMax` (rango opcional)
- `targetWeightKg` (Decimal, nullable)
- `targetTimeSeconds` (Integer, nullable, para isométricos)
- `targetDistanceMeters` (Decimal, nullable)
- `restAfterSeconds` (Integer, nullable)
- `tempo` (String, ej: "3-1-1-0")
- `rpe` (Integer 1-10, opcional)
- `notes` (String, opcional)
- `toFailure` (boolean, si "al fallo" reemplaza targetReps)

---

## 6. Modelo de base de datos completo

Migraciones en orden de ejecución. Cada `V<N>__<descripción>.sql` en `backend/src/main/resources/db/migration/`.

### V1__initial_schema.sql

```sql
-- Gimnasio (single-row en V1, preparado para multi-tenant en V3)
CREATE TABLE gyms (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    owner_name      VARCHAR(150),
    phone           VARCHAR(50),
    email           VARCHAR(150),
    address         VARCHAR(255),
    logo_url        VARCHAR(500),
    primary_color   VARCHAR(7),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Usuarios del sistema (profesores/dueños)
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    gym_id          BIGINT NOT NULL REFERENCES gyms(id),
    email           VARCHAR(150) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'TRAINER')),
    active          BOOLEAN NOT NULL DEFAULT true,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_gym ON users(gym_id);

-- Alumnos
CREATE TABLE students (
    id              BIGSERIAL PRIMARY KEY,
    gym_id          BIGINT NOT NULL REFERENCES gyms(id),
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    document_id     VARCHAR(50),
    phone           VARCHAR(50),
    email           VARCHAR(150),
    birth_date      DATE,
    sport           VARCHAR(100),
    objective       VARCHAR(150),
    level           VARCHAR(50),
    general_notes   TEXT,
    active          BOOLEAN NOT NULL DEFAULT true,
    started_at      DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- evita cargar dos veces el mismo alumno por DNI
    -- (PostgreSQL permite múltiples NULL, así que document_id sigue siendo opcional)
    CONSTRAINT uk_students_gym_doc UNIQUE (gym_id, document_id)
);

CREATE INDEX idx_students_gym ON students(gym_id);
CREATE INDEX idx_students_active ON students(gym_id, active);
CREATE INDEX idx_students_search ON students(gym_id, last_name, first_name);

-- Lesiones del alumno
-- IMPORTANTE: estos datos son sensibles y NO se incluyen en PDF/WhatsApp por defecto
CREATE TABLE student_injuries (
    id              BIGSERIAL PRIMARY KEY,
    student_id      BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    body_area       VARCHAR(100) NOT NULL,
    description     TEXT NOT NULL,
    severity        VARCHAR(20) CHECK (severity IN ('LEVE', 'MODERADA', 'GRAVE')),
    started_at      DATE,
    resolved_at     DATE,
    active          BOOLEAN NOT NULL DEFAULT true,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_injuries_student ON student_injuries(student_id);

-- Notas internas del profesor sobre el alumno
-- IMPORTANTE: nunca se incluyen en PDF/WhatsApp
CREATE TABLE student_notes (
    id              BIGSERIAL PRIMARY KEY,
    student_id      BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    author_user_id  BIGINT NOT NULL REFERENCES users(id),
    content         TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_student_notes_student ON student_notes(student_id, created_at DESC);
```

### V2__exercise_catalog.sql

```sql
-- Catálogo de ejercicios
CREATE TABLE exercises (
    id                  BIGSERIAL PRIMARY KEY,
    gym_id              BIGINT NOT NULL REFERENCES gyms(id),
    name                VARCHAR(150) NOT NULL,
    description         TEXT,
    technical_notes     TEXT,
    default_measurement VARCHAR(30) NOT NULL DEFAULT 'REPS_WEIGHT',
        -- REPS_WEIGHT, REPS_ONLY, TIME, DISTANCE, CIRCUIT_REPS
    video_url           VARCHAR(500),
    image_url           VARCHAR(500),
    active              BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_exercises_gym_active ON exercises(gym_id, active);
CREATE INDEX idx_exercises_search ON exercises(gym_id, name);

-- Tags del catálogo
CREATE TABLE exercise_tags (
    id          BIGSERIAL PRIMARY KEY,
    gym_id      BIGINT NOT NULL REFERENCES gyms(id),
    type        VARCHAR(30) NOT NULL,
        -- BODY_AREA, MUSCLE_GROUP, MOVEMENT_PATTERN, OBJECTIVE, LEVEL, EQUIPMENT
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL,
    UNIQUE (gym_id, type, slug)
);

CREATE INDEX idx_tags_type ON exercise_tags(gym_id, type);

-- Asignación many-to-many ejercicio ↔ tag
CREATE TABLE exercise_tag_assignments (
    exercise_id     BIGINT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    tag_id          BIGINT NOT NULL REFERENCES exercise_tags(id) ON DELETE CASCADE,
    PRIMARY KEY (exercise_id, tag_id)
);

CREATE INDEX idx_eta_tag ON exercise_tag_assignments(tag_id);
```

### V3__templates.sql

```sql
CREATE TABLE training_templates (
    id              BIGSERIAL PRIMARY KEY,
    gym_id          BIGINT NOT NULL REFERENCES gyms(id),
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    sport           VARCHAR(100),
    objective       VARCHAR(150),
    level           VARCHAR(50),
    estimated_duration_minutes  INTEGER,
    general_notes   TEXT,
    active          BOOLEAN NOT NULL DEFAULT true,
    created_by_user_id  BIGINT NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_templates_gym_active ON training_templates(gym_id, active);

CREATE TABLE template_blocks (
    id                      BIGSERIAL PRIMARY KEY,
    template_id             BIGINT NOT NULL REFERENCES training_templates(id) ON DELETE CASCADE,
    order_index             INTEGER NOT NULL,
    title                   VARCHAR(150) NOT NULL,
    structural_type         VARCHAR(30) NOT NULL,
    purpose                 VARCHAR(30),
    total_duration_seconds  INTEGER,
    target_rounds           INTEGER,
    block_notes             TEXT,
    -- evita dos bloques con el mismo orden dentro de la misma plantilla
    CONSTRAINT uk_tb_template_order UNIQUE (template_id, order_index)
);

CREATE INDEX idx_template_blocks_template ON template_blocks(template_id, order_index);

CREATE TABLE template_exercises (
    id                  BIGSERIAL PRIMARY KEY,
    block_id            BIGINT NOT NULL REFERENCES template_blocks(id) ON DELETE CASCADE,
    exercise_id         BIGINT NOT NULL REFERENCES exercises(id),
    order_index         INTEGER NOT NULL,
    exercise_notes      TEXT,
    CONSTRAINT uk_te_block_order UNIQUE (block_id, order_index)
);

CREATE INDEX idx_template_exercises_block ON template_exercises(block_id, order_index);

CREATE TABLE template_exercise_sets (
    id                      BIGSERIAL PRIMARY KEY,
    template_exercise_id    BIGINT NOT NULL REFERENCES template_exercises(id) ON DELETE CASCADE,
    set_number              INTEGER NOT NULL,
    set_kind                VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
    target_reps             INTEGER,
    target_reps_min         INTEGER,
    target_reps_max         INTEGER,
    target_weight_kg        NUMERIC(6,2),
    target_time_seconds     INTEGER,
    target_distance_meters  NUMERIC(7,2),
    rest_after_seconds      INTEGER,
    tempo                   VARCHAR(20),
    rpe                     INTEGER CHECK (rpe BETWEEN 1 AND 10),
    notes                   TEXT,
    to_failure              BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uk_tes_te_setnum UNIQUE (template_exercise_id, set_number)
);

CREATE INDEX idx_tes_te ON template_exercise_sets(template_exercise_id, set_number);
```

### V4__routines.sql

Idéntica estructura a templates pero independiente. Una rutina creada desde plantilla **copia toda la estructura** y queda desacoplada.

```sql
CREATE TABLE routines (
    id                  BIGSERIAL PRIMARY KEY,
    student_id          BIGINT NOT NULL REFERENCES students(id),
    name                VARCHAR(150) NOT NULL,
    objective           VARCHAR(150),
    -- ON DELETE SET NULL: si se borra la plantilla origen, la rutina sigue existiendo sin esa referencia
    source_template_id  BIGINT REFERENCES training_templates(id) ON DELETE SET NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE', 'FINISHED', 'ARCHIVED', 'DRAFT')),
    assigned_date       DATE NOT NULL,
    finished_date       DATE,
    general_notes       TEXT,
    -- internal_notes NUNCA se incluye en PDF ni WhatsApp
    internal_notes      TEXT,
    created_by_user_id  BIGINT NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_routines_student ON routines(student_id, assigned_date DESC);
CREATE INDEX idx_routines_status ON routines(student_id, status);

CREATE TABLE routine_blocks (
    id                      BIGSERIAL PRIMARY KEY,
    routine_id              BIGINT NOT NULL REFERENCES routines(id) ON DELETE CASCADE,
    order_index             INTEGER NOT NULL,
    title                   VARCHAR(150) NOT NULL,
    structural_type         VARCHAR(30) NOT NULL,
    purpose                 VARCHAR(30),
    total_duration_seconds  INTEGER,
    target_rounds           INTEGER,
    block_notes             TEXT,
    CONSTRAINT uk_rb_routine_order UNIQUE (routine_id, order_index)
);

CREATE INDEX idx_routine_blocks_routine ON routine_blocks(routine_id, order_index);

CREATE TABLE routine_exercises (
    id                  BIGSERIAL PRIMARY KEY,
    block_id            BIGINT NOT NULL REFERENCES routine_blocks(id) ON DELETE CASCADE,
    exercise_id         BIGINT NOT NULL REFERENCES exercises(id),
    order_index         INTEGER NOT NULL,
    exercise_notes      TEXT,
    CONSTRAINT uk_re_block_order UNIQUE (block_id, order_index)
);

CREATE INDEX idx_routine_exercises_block ON routine_exercises(block_id, order_index);

CREATE TABLE routine_exercise_sets (
    id                      BIGSERIAL PRIMARY KEY,
    routine_exercise_id     BIGINT NOT NULL REFERENCES routine_exercises(id) ON DELETE CASCADE,
    set_number              INTEGER NOT NULL,
    set_kind                VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
    target_reps             INTEGER,
    target_reps_min         INTEGER,
    target_reps_max         INTEGER,
    target_weight_kg        NUMERIC(6,2),
    target_time_seconds     INTEGER,
    target_distance_meters  NUMERIC(7,2),
    rest_after_seconds      INTEGER,
    tempo                   VARCHAR(20),
    rpe                     INTEGER CHECK (rpe BETWEEN 1 AND 10),
    notes                   TEXT,
    to_failure              BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uk_res_re_setnum UNIQUE (routine_exercise_id, set_number)
);

CREATE INDEX idx_res_re ON routine_exercise_sets(routine_exercise_id, set_number);
```

### V5__seed_default_tags.sql

Inserta los tags base (zonas, grupos musculares, patrones, objetivos, niveles, equipamiento) para el primer gym al instalar. Ver lista completa en sección 19.

---

## 7. Reglas de negocio críticas

Estas reglas viven en services y deben tener tests que las verifiquen explícitamente.

1. **Copia profunda al crear rutina desde plantilla.** Cuando se crea una rutina desde un template, se duplican bloques, ejercicios y sets. El campo `source_template_id` queda solo como referencia. Editar el template después no afecta rutinas creadas. Borrar el template no borra rutinas (FK con `ON DELETE SET NULL`).

2. **Estados de rutina.** Una rutina activa de un alumno es la última con `status = 'ACTIVE'`. Si se asigna una nueva rutina, la anterior pasa a `FINISHED` automáticamente con `finished_date = today`.

3. **Soft-delete obligatorio.** Alumnos, ejercicios, plantillas y rutinas no se borran físicamente. Solo se cambia `active = false` o `status = 'ARCHIVED'`. Esto preserva el historial.

4. **Endpoints de eliminación claros.** Como `DELETE` no borra físicamente, conviene exponer endpoints más explícitos:
   - `DELETE /api/students/{id}` → archive (cambia active a false)
   - `PATCH /api/students/{id}/deactivate` (alternativa más explícita, opcional)
   - `PATCH /api/students/{id}/reactivate`

5. **Ejercicios inactivos.** No aparecen en buscadores ni selectores nuevos, pero siguen visibles en rutinas que ya los referencian.

6. **Validaciones estructurales:**
   - Una rutina debe tener al menos un bloque (excepto en `DRAFT`).
   - Un bloque debe tener al menos un ejercicio.
   - Un ejercicio en bloque STANDARD debe tener al menos un set.
   - Un bloque CIRCUIT debe tener `total_duration_seconds`.
   - Un set con `to_failure = true` puede tener `target_reps` null.

7. **Privacidad estricta**: NUNCA se incluye en PDF ni WhatsApp:
   - `routine.internal_notes`
   - `student_injuries.*` (todas las lesiones)
   - `student_notes.*` (todas las notas internas del profesor)

   Sí se incluye:
   - `routine.general_notes`
   - `routine_block.block_notes`
   - `routine_exercise.exercise_notes`

8. **Multi-tenancy.** Toda query filtra por `gym_id` del usuario autenticado. En V1 hay un solo gym, pero la arquitectura está lista para V3.

9. **Inmutabilidad del historial.** Las rutinas en `FINISHED` o `ARCHIVED` no se editan. Para cambiar algo, se duplica y crea una nueva.

10. **Permisos por rol** (preparado para V3):
    - `OWNER` puede todo.
    - `TRAINER` puede gestionar alumnos, ejercicios, plantillas y rutinas. No puede ver datos financieros (V2+) ni gestionar usuarios.

11. **Auditoría mínima.** Toda entidad tiene `created_at`, `updated_at` y donde corresponde `created_by_user_id`. Spring Data lo completa automáticamente con `@CreatedDate` y `@LastModifiedDate`.

12. **Unidad de peso por defecto.** Kilogramos. No se contemplan libras en V1.

13. **Redondeo de pesos en ajustes**: ver caso de uso CU-11.

---

## 8. Casos de uso principales (V1)

### CU-01: Login del profesor
**Actor:** OWNER  
**Flujo:** ingresa email + contraseña → backend valida → devuelve JWT + datos del usuario → frontend guarda token → redirige a home.

### CU-02: Crear alumno
**Actor:** OWNER  
**Flujo:** ingresa nombre, apellido, datos básicos, deporte, objetivo, nivel → guarda. Lesiones y notas se cargan después en la ficha.

### CU-03: Crear ejercicio
**Actor:** OWNER  
**Flujo:** nombre, descripción, tipo de medición default, selecciona tags (puede marcar varios de cada tipo), opcionalmente sube imagen.

### CU-04: Crear plantilla con bloques mixtos
**Actor:** OWNER  
**Flujo:**
1. Nombra la plantilla, elige deporte/objetivo/nivel.
2. Agrega bloque 1 "Entrada en calor" (tipo STANDARD, propósito WARMUP).
3. Agrega ejercicios al bloque, define sets default (modo simple: 3×10).
4. Agrega bloque 2 "Tren superior" (tipo PYRAMID).
5. Para cada ejercicio define los sets distintos en modo avanzado.
6. Agrega bloque 3 "Circuito" (tipo CIRCUIT, duración 12 min).
7. Agrega 3 ejercicios con reps target cada uno.
8. Guarda.

### CU-05: Asignar rutina a alumno desde plantilla
**Actor:** OWNER  
**Flujo:**
1. Entra a la ficha del alumno.
2. "Nueva rutina" → "Desde plantilla" → elige plantilla.
3. Backend crea Routine con copia profunda de bloques/ejercicios/sets.
4. Frontend abre editor con todo precargado.
5. Profesor ajusta pesos según el alumno, agrega notas.
6. Guarda. Rutina queda `ACTIVE`. La anterior pasa a `FINISHED`.

### CU-06: Generar PDF y compartir
**Actor:** OWNER  
**Flujo:**
1. En la rutina, click "Generar PDF".
2. Backend genera PDF (Flying Saucer + Thymeleaf) con: header con logo y datos del gym, datos del alumno (sin lesiones), fecha, objetivo, tabla por bloque con título, tipo, ejercicios con grupo muscular y patrón, sets, descanso, notas para el alumno (no internas).
3. Frontend recibe el archivo, ofrece descarga.
4. Profesor adjunta a WhatsApp manualmente, o usa "Copiar texto resumido" para enviar versión rápida por chat.

### CU-07: Consultar historial del alumno
**Actor:** OWNER  
**Flujo:** entra a ficha → tab "Historial" → ve lista cronológica de rutinas → click en cualquiera → ve detalle completo de esa rutina pasada.

### CU-08: Ver últimas cargas de un ejercicio
**Actor:** OWNER  
**Flujo:** mientras edita una rutina, hace hover/click en un ejercicio → tooltip muestra "Última carga de este alumno: 3×10 con 22 kg el 15/04/2026". Evita abrir otra pantalla.

### CU-09: Buscar alumno
**Actor:** OWNER  
**Flujo:** barra de búsqueda en header → escribe → debounce 300ms → backend devuelve match por nombre, apellido, DNI o teléfono → click en resultado va a la ficha.

### CU-10: Setup inicial del gimnasio
**Actor:** OWNER (primera vez)  
**Flujo:** pantalla de configuración → carga nombre del gym, dueño, teléfono, dirección, sube logo → guarda.

### CU-11: Ajustar pesos por porcentaje (NUEVO)

**Actor:** OWNER  
**Precondición:** rutina asignada con sets que tengan pesos definidos.  
**Flujo:**
1. El profesor está editando una rutina o ya la abrió.
2. Click "Ajustar pesos".
3. Elige alcance:
   - Toda la rutina.
   - Un bloque específico.
   - Un ejercicio específico.
4. Ingresa porcentaje: +5%, -10%, etc.
5. Elige redondeo:
   - Sin redondeo (queda con decimales exactos).
   - 0.5 kg.
   - 1 kg.
   - 2.5 kg (típico de discos).
   - 5 kg.
6. El sistema muestra preview: "Sentadilla S1: 80 kg → 84 kg (+5%)".
7. El profesor confirma o cancela.
8. El backend actualiza solo los sets con `target_weight_kg` no null. Tiempo, reps, distancia no se tocan.

**Endpoint:**
```
POST /api/routines/{id}/adjust-weights
Body: {
  scopeType: "ROUTINE" | "BLOCK" | "EXERCISE",
  scopeId?: number,        // requerido si scope no es ROUTINE
  percentage: number,      // ej 5 = +5%, -10 = -10%
  roundingStepKg?: number  // 0.5, 1, 2.5, 5, o null para sin redondeo
}
Response: {
  setsUpdated: number,
  preview: [{ exerciseName, setNumber, oldWeight, newWeight }, ...]
}
```

**Modo preview vs confirm:**
- Recomendado implementar como dos endpoints o un flag `?preview=true` que devuelve el cambio sin aplicarlo.

---

## 9. Mapa de pantallas frontend

```
/login                            → Login
/                                 → Dashboard
/students                         → Lista alumnos con búsqueda y filtros
/students/new                     → Alta de alumno
/students/:id                     → Ficha (tabs: datos, lesiones, rutina actual, historial)
/students/:id/edit                → Editar alumno
/students/:id/routines/new        → Crear rutina (desde plantilla o desde cero)
/students/:id/routines/:routineId → Ver/editar rutina específica

/exercises                        → Catálogo con buscador y filtros
/exercises/new                    → Crear ejercicio
/exercises/:id                    → Editar ejercicio

/templates                        → Lista de plantillas
/templates/new                    → Constructor de plantilla
/templates/:id                    → Ver/editar plantilla

/settings                         → Configuración del gimnasio
/settings/account                 → Datos del usuario, cambio de contraseña
```

### Componentes UI clave

- `<RoutineEditor>` — editor visual con drag-and-drop de bloques y ejercicios.
- `<BlockCard>` — render de un bloque con su tipo, título, ejercicios.
- `<SetTable>` — tabla editable de sets para un ejercicio (con modo simple / avanzado).
- `<ExercisePicker>` — modal de selección con buscador y filtros por tags.
- `<TagFilter>` — filtros laterales por tipo de tag.
- `<StudentSearchCommand>` — búsqueda global tipo Cmd+K.
- `<RoutinePreview>` — vista previa de cómo se verá el PDF.
- `<WeightAdjustDialog>` — diálogo de ajuste porcentual con preview.

---

## 10. Seguridad

### Autenticación con JWT

Flujo:
1. `POST /api/auth/login` con `{email, password}`.
2. Backend valida con BCrypt strength 12.
3. Si OK: genera JWT con claims `{sub: userId, email, role, gymId, exp: now+12h}` firmado con HS256 + secret de 256 bits.
4. Devuelve `{token, user: {...}}`.
5. Frontend guarda en `localStorage` (V2+ se evalúa httpOnly cookie).
6. Frontend envía `Authorization: Bearer <token>` en cada request.
7. Backend valida con `JwtAuthenticationFilter` y rellena el `SecurityContext`.

### Reglas de Spring Security

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/login", "/actuator/health").permitAll()
    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll() // solo dev
    .requestMatchers("/api/admin/**").hasRole("OWNER")
    .anyRequest().authenticated())
.sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
.csrf(AbstractHttpConfigurer::disable)
.cors(...);
```

### CORS

- Dev: `http://localhost:5173` permitido.
- Prod: lista de orígenes permitidos en variable `CORS_ALLOWED_ORIGINS`.

### Otras medidas

- Contraseñas con BCrypt strength 12.
- Validación estricta de DTOs con Bean Validation.
- HTTPS obligatorio en prod (Render y Vercel lo proveen).
- Variables sensibles solo en variables de entorno.
- Headers de seguridad: HSTS, X-Frame-Options DENY, CSP básica.

---

## 11. Generación de PDF

### Decisión técnica: Flying Saucer + Thymeleaf

La rutina se renderiza como template HTML con Thymeleaf y Flying Saucer (`org.xhtmlrenderer:flying-saucer-pdf`) lo convierte a PDF.

Ventajas: layout en HTML/CSS, fácil de iterar visualmente, soporta logo, tablas, paginación.

### Estructura del template HTML para PDF

`backend/src/main/resources/templates/pdf/routine.html`

Contenido:
- Header: logo del gym, datos del gym (nombre, teléfono, dirección).
- Banda con datos del alumno: nombre, fecha, objetivo (sin lesiones).
- Por cada bloque:
  - Título del bloque + tag de tipo estructural.
  - Si CIRCUIT: nota destacada "Realizar durante X minutos rotando".
  - Tabla con: Grupo muscular | Patrón | Ejercicio | Series | Reps | Peso | Descanso | Notas.
- Footer: paginación y datos de contacto.

### Endpoints

```
GET /api/routines/{id}/pdf
→ produces: application/pdf
→ filename: rutina_{nombre_alumno}_{fecha}.pdf

GET /api/routines/{id}/text
→ produces: text/plain; charset=UTF-8
→ string formateado para WhatsApp
```

---

## 12. Backup y mantenimiento

### Realidad de Neon Free vs Launch

**Plan Free de Neon:**
- 0.5 GB de storage por proyecto.
- 100 CU-hours por mes.
- PITR (Point-in-Time Recovery) limitado a una ventana corta (entre 6 y 24 horas según política vigente — verificar al deployar). NO 7 días.
- Sin backups automáticos schedulados.
- Sirve para desarrollo, demo y V1 muy temprana.

**Plan Launch (~$15/mes):**
- 7 días de PITR.
- Mucho más margen de cómputo y storage.
- Soporte estándar.
- Apropiado para operación real con cliente.

### Estrategia recomendada

Para presentar al cliente con confianza:
- **Opción A**: Neon Free + backup semanal manual con `pg_dump` programado (GitHub Action) hacia GitHub Releases privados. Costo: $0 + esfuerzo de mantenimiento.
- **Opción B**: Neon Launch. Costo: ~$15/mes, restore de 7 días automático.
- **Opción C** (alternativa): Supabase Pro. Costo: ~$25/mes, backups diarios incluidos, ecosistema más amplio.

Para V1 técnica/demo: Opción A está bien.  
Para producción operando: Opción B.

### Backup manual semanal

```bash
#!/bin/bash
# scripts/backup.sh
DATE=$(date +%Y%m%d_%H%M%S)
pg_dump $DATABASE_URL > backups/gym_planner_$DATE.sql
gzip backups/gym_planner_$DATE.sql
ls -t backups/*.sql.gz | tail -n +5 | xargs rm -f
```

Ejecutar via GitHub Action programada semanalmente.

### Documentación de restore

Mantener `docs/runbook.md` con:
1. Cómo restaurar desde backup en Neon.
2. Cómo aplicar un dump SQL.
3. Cómo bajar a una versión anterior si una migración Flyway sale mal.

### Monitoreo

- UptimeRobot pinguea `/actuator/health` cada 5 minutos.
- Alerta a email si el backend cae.
- Render notifica si un deploy falla.

### Mantenimiento mensual

Checklist mensual:
- Revisar logs de Render.
- Verificar uso de espacio en Neon.
- Verificar alertas de UptimeRobot.
- Actualizar dependencias menores.
- Hacer backup manual y verificar restore.

---

## 13. Estructura de carpetas del proyecto

```
gym-planner/
├── README.md
├── docker-compose.yml
├── .env.example
├── .gitignore
│
├── docs/
│   ├── design.md                    (este documento)
│   ├── runbook.md
│   ├── api.md
│   └── decisions/                   (ADRs)
│       ├── 0001-stack.md
│       ├── 0002-block-typing.md
│       ├── 0003-pdf-generation.md
│       └── 0004-tags-not-hierarchy.md
│
├── .github/
│   └── workflows/
│       ├── backend-ci.yml
│       ├── frontend-ci.yml
│       └── backup-weekly.yml
│
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   ├── .dockerignore
│   ├── mvnw, mvnw.cmd
│   └── src/
│       ├── main/
│       │   ├── java/com/gymplanner/
│       │   │   ├── GymPlannerApplication.java
│       │   │   ├── config/
│       │   │   ├── auth/
│       │   │   ├── user/
│       │   │   ├── gym/
│       │   │   ├── student/
│       │   │   │   ├── injury/
│       │   │   │   └── note/
│       │   │   ├── exercise/
│       │   │   ├── template/
│       │   │   ├── routine/
│       │   │   ├── pdf/
│       │   │   └── shared/
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       ├── application-prod.yml
│       │       ├── db/migration/
│       │       └── templates/pdf/
│       └── test/
│
└── frontend/
    ├── package.json
    ├── vite.config.ts
    ├── tsconfig.json
    ├── tailwind.config.ts
    ├── components.json
    ├── .env.example
    └── src/
        ├── main.tsx
        ├── App.tsx
        ├── api/
        ├── components/
        │   ├── ui/
        │   ├── layout/
        │   ├── routine/
        │   ├── student/
        │   └── shared/
        ├── pages/
        ├── hooks/
        ├── lib/
        ├── types/
        ├── schemas/
        └── routes/
```

---

## 14. Estrategia de Git y GitHub

### Branching

- `main` → siempre deployable.
- `develop` → branch de integración.
- `feature/<nombre>` → cada feature.
- `bugfix/<nombre>` → arreglos.
- `hotfix/<nombre>` → desde main para producción.

### Conventional Commits

```
feat(student): add injury tracking endpoint
fix(routine): copy template sets correctly
chore(deps): bump spring-boot to 3.5.5
docs(design): update Neon backup strategy
test(routine): cover from-template deep copy
refactor(exercise): extract tag assignment logic
```

### Pull requests

Cada PR debe tener: título claro, descripción, checklist de tests, captura si toca UI, referencia a issue.

CI ejecuta automáticamente: build backend, tests backend, build frontend, typecheck frontend, lint frontend.

### Tags de versión

- `v0.1.0` → primera versión deployada.
- `v0.x.x` → durante desarrollo de V1.
- `v1.0.0` → entrega oficial al cliente.
- `v1.0.x` → bugfixes en V1.
- `v2.0.0` → V2 con módulo administrativo.

### GitHub Project

Kanban con columnas: Backlog, Todo, In Progress, Review, Done. Cada feature/bug es un issue con labels.

---

## 15. Plan de desarrollo cronológico

Estimación con dos personas trabajando, una más enfocada en backend y otra en frontend.

### Sprint 0 — Setup inicial (5 días)
**Prompt 1 a Codex**.
Repo, monorepo, Spring Boot inicial, React+Vite inicial, Docker Compose, deploys "hello world", CI básico, login funcional, Gym/User.  
**Entregable:** la app dice "Hola" en Vercel, backend responde 200 en `/actuator/health`, login funciona con usuario demo.

### Sprint 1 — Alumnos (7 días)
**Prompt 2 (parte 1) a Codex**.
Entidades Student, StudentInjury, StudentNote. CRUD endpoints. Frontend: lista, búsqueda, ficha con tabs.  
**Entregable:** gestión completa de alumnos.

### Sprint 2 — Catálogo de ejercicios (5 días)
**Prompt 2 (parte 2) a Codex**.
Entidades Exercise, ExerciseTag, ExerciseTagAssignment. Migración con seed de tags. CRUD con multi-select de tags y filtros.  
**Entregable:** carga de ejercicios con clasificaciones.

### Sprint 3 — Plantillas (10 días)
**Prompt 3 (parte 1) a Codex**.
Entidades TrainingTemplate, TemplateBlock, TemplateExercise, TemplateExerciseSet. Constructor con drag-and-drop, modo simple/avanzado de sets.  
**Entregable:** plantillas operativas con todos los tipos de bloque.

### Sprint 4 — Rutinas (8 días)
**Prompt 3 (parte 2) a Codex**.
Routine y derivadas. Endpoint "from-template" con copia profunda (TEST CRÍTICO). Editor de rutina. Cierre automático de rutina anterior.  
**Entregable:** rutinas reales asignadas a alumnos.

### Sprint 5 — PDF, historial, ajuste de pesos, WhatsApp (7 días)
**Prompt 4 a Codex**.
Template Thymeleaf, endpoint PDF, endpoint texto WhatsApp, tab de historial, tooltip de últimas cargas, endpoint de ajuste porcentual.  
**Entregable:** rutinas profesionales por WhatsApp + historial + ajustes rápidos.

### Sprint 6 — Polish, testing y entrega (5 días)
Subir cobertura, revisar UX, configurar UptimeRobot, runbook, capacitación al cliente, tag `v1.0.0`.

**Total:** ~47 días hábiles ≈ 9-10 semanas calendario solo, ≈ 6-7 semanas con compañero.

---

## 16. V1 operativa mínima vs V1.1

Para no inflar el alcance, se separan dos sub-versiones dentro de V1.

### V1.0 — entregable mínimo

1. Login del dueño/profesor.
2. Gestión de alumnos.
3. Catálogo de ejercicios con tags.
4. Creación de plantillas con bloques (todos los tipos estructurales).
5. Creación de rutina desde plantilla con copia profunda.
6. Edición básica de rutina asignada (modo simple).
7. Historial de rutinas por alumno.
8. PDF profesional + texto WhatsApp.
9. Configuración del gimnasio.
10. Deploy frontend/backend/DB.
11. Backup documentado.

### V1.1 — mejoras posteriores

1. Ajuste porcentual de pesos avanzado (con preview elaborado).
2. Modo avanzado de sets ultra completo (drop sets visuales, rest-pause con timer).
3. Drag-and-drop muy pulido en mobile.
4. Preview WYSIWYG perfecto del PDF antes de generar.
5. Tooltip de últimas cargas con mini-gráfico.
6. Búsqueda global tipo Cmd+K.
7. Atajos de teclado.

---

## 17. Versiones futuras

### V2 — Administración (3-4 semanas)
- Gestión de cuotas mensuales.
- Estado de pago, deudores.
- Dashboard de ingresos.
- Reportes simples.
- Asistencia básica.
- Gráficos de evolución.
- Migración a Testcontainers.

### V3 — Escalabilidad operativa (4-6 semanas)
- Roles completos (OWNER + TRAINER).
- Asignación de alumnos a profesores.
- Mantenimiento de máquinas.
- Inventario de equipamiento.
- Portal del alumno.
- Notificaciones automáticas.
- Multi-gym.

### V4 — Visión a futuro
- Multi-sucursal.
- Mercado Pago.
- App móvil/PWA.
- Evaluaciones físicas.
- Integración con wearables.
- Periodización multi-semana.

---

## 18. Términos comerciales sugeridos

### V1 entrega
- Precio: USD 700-800 (rango de mercado AR para sistema de este alcance).
- Estructura: 30% al iniciar, 40% al entregar V1.0, 30% al cerrar capacitación + garantía.
- Garantía: 30 días post-entrega para bugs sin costo.

### Mantenimiento mensual
- Precio: USD 30-40/mes.
- Incluye: hosting, DB, backups semanales, monitoreo, hasta 2 hs/mes de soporte, updates de seguridad.
- No incluye: nuevas features (se cotizan aparte).

### Recomendación
Dividir explícitamente V1.0 (alcance acotado) y V1.1 (mejoras) para protegerse de scope creep.

---

## 19. Apéndice: tags default para el seed

```
BODY_AREA:
  - Tren superior, Zona media, Tren inferior, Cuerpo completo

MUSCLE_GROUP:
  - Pecho, Espalda, Hombros, Bíceps, Tríceps, Antebrazos
  - Cuádriceps, Isquiotibiales, Glúteos, Aductores, Gemelos
  - Core, Lumbares, Oblicuos
  - Cuello

MOVEMENT_PATTERN:
  - Empuje vertical, Empuje horizontal
  - Tirón vertical, Tirón horizontal
  - Sentadilla, Bisagra de cadera, Zancada
  - Rotación, Anti-rotación
  - Locomoción, Salto, Isométrico

OBJECTIVE:
  - Fuerza, Hipertrofia, Potencia
  - Resistencia muscular, Resistencia cardiovascular
  - Movilidad, Flexibilidad
  - Prevención, Rehabilitación
  - Técnica, Activación, Coordinación

LEVEL:
  - Iniciación, Intermedio, Avanzado

EQUIPMENT:
  - Peso corporal, Mancuernas, Barra olímpica, Barra Z
  - Polea, Máquina guiada, Banda elástica
  - Kettlebell, Cajón, Soga (battle ropes), TRX
  - Bicicleta, Elíptica, Cinta, Remo
  - Disco, Slam ball, Medicine ball, Bosu, Fitball
```

---

## 20. Apéndice: rutina de ejemplo modelada

```yaml
Routine:
  name: "Lunes — Fútbol potencia"
  student: Martín Gómez
  status: ACTIVE
  blocks:
    - order: 1
      title: "Entrada en calor"
      structuralType: STANDARD
      purpose: WARMUP
      exercises:
        - exercise: Movilidad de cadera
          sets: [{setNumber: 1, targetTimeSeconds: 60}]
        - exercise: Skipping bajo
          sets: [{setNumber: 1, targetTimeSeconds: 30}, {setNumber: 2, targetTimeSeconds: 30}]

    - order: 2
      title: "Tren superior — Pirámide invertida"
      structuralType: REVERSE_PYRAMID
      purpose: MAIN_LIFT
      exercises:
        - exercise: Press banca
          sets:
            - {setNumber: 1, targetReps: 6,  targetWeightKg: 80, restAfterSeconds: 120}
            - {setNumber: 2, targetReps: 8,  targetWeightKg: 70, restAfterSeconds: 90}
            - {setNumber: 3, targetReps: 10, targetWeightKg: 60, restAfterSeconds: 90}

    - order: 3
      title: "Circuito 12 minutos"
      structuralType: CIRCUIT
      purpose: CONDITIONING
      totalDurationSeconds: 720
      blockNotes: "Rotar sin descanso entre ejercicios."
      exercises:
        - exercise: Salto al cajón
          sets: [{setNumber: 1, targetReps: 6}]
        - exercise: Abdominales
          sets: [{setNumber: 1, targetReps: 15}]
        - exercise: Tríceps polea
          sets: [{setNumber: 1, targetReps: 10, notes: "cada brazo"}]

    - order: 4
      title: "Tren inferior"
      structuralType: STANDARD
      purpose: ACCESSORY
      exercises:
        - exercise: Sentadilla goblet
          sets:
            - {setNumber: 1, targetReps: 10, targetWeightKg: 22, restAfterSeconds: 60}
            - {setNumber: 2, targetReps: 10, targetWeightKg: 22, restAfterSeconds: 60}
            - {setNumber: 3, targetReps: 10, targetWeightKg: 22, restAfterSeconds: 60}

    - order: 5
      title: "Zona media y vuelta a la calma"
      structuralType: STANDARD
      purpose: COOLDOWN
      exercises:
        - exercise: Plancha frontal
          sets:
            - {setNumber: 1, targetTimeSeconds: 40, restAfterSeconds: 30}
            - {setNumber: 2, targetTimeSeconds: 40, restAfterSeconds: 30}
            - {setNumber: 3, targetTimeSeconds: 40}
```

---

## 21. Estrategia de prompts a Codex

**No usar un solo mega-prompt.** Riesgo: si se equivoca temprano (por ejemplo en una entidad), arrastra el error a todo el resto. Imposible de revisar.

**Estrategia: 4 prompts segmentados con checkpoint humano entre cada uno.**

| Prompt | Alcance | Sprint |
|---|---|---|
| **Prompt 1** | Scaffolding técnico, auth, Gym/User, login funcional | Sprint 0 |
| **Prompt 2** | Alumnos + ejercicios + tags | Sprint 1-2 |
| **Prompt 3** | Plantillas + rutinas + copia profunda | Sprint 3-4 |
| **Prompt 4** | PDF + WhatsApp + historial + ajuste de pesos | Sprint 5 |

Entre cada prompt:
1. Validar localmente que todo compila y funciona.
2. Hacer commit con tag de etapa (ej: `v0.1-scaffolding`, `v0.2-students`).
3. Revisar diff manual antes de commitear.
4. Si Codex se desvió de la spec, le respondés con la sección del documento que tiene que respetar y le pedís que ajuste.

Los prompts están en archivos separados en `docs/codex-prompts/`.

---

## Cierre

Este documento es la fuente única de verdad del proyecto. Cuando aparezca una decisión nueva, actualizá la sección correspondiente y creá un ADR en `docs/decisions/`.

Volvé a este documento al empezar cada sprint, antes de cotizar trabajo adicional, o cuando un problema técnico te haga dudar de cómo se modeló algo.