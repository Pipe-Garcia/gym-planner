# ADR 0011 — Indicación por serie (`executionCue`) y bloques de series agrupadas (`GROUPED_SET`)

> Estado: **Aceptado** · Fecha: 2026-06 · Afecta: dominio de rutinas y plantillas, PDF, WhatsApp, historial, cargas previas, editor.
>
> Este documento es la **fuente única de verdad** de estas dos features. Tanto los prompts a Codex como las revisiones manuales se hacen contra este doc. Si algo del código contradice este doc, gana el doc (o se actualiza el doc con una decisión explícita).

---

## 1. Problema / contexto

Después de la demo, el cliente (profesor) pidió dos capacidades que el modelo actual no representa bien:

1. **"Brazo extendido, mitad, corto"** — para un mismo ejercicio, poder indicar que cada serie se ejecuta con un rango/indicación distinta, aunque reps y peso sean iguales. Es la familia de las **repeticiones parciales / rango parcial de movimiento** (incluye el método "21s"). El cliente lo usa como una **indicación por serie**.

2. **Biseries / triseries / superseries** — agrupar varios ejercicios que se ejecutan como una unidad, por **vueltas** (no por tiempo como el circuito).

Estas dos cosas viven en **niveles distintos del dominio**, y esa es la decisión que ordena todo:

- La indicación por serie cambia **entre series de un mismo ejercicio** → vive en el **set**.
- Las series agrupadas cambian la relación **entre varios ejercicios** → viven en el **bloque**.

---

## 2. Decisiones cerradas (resumen)

| Tema | Decisión |
|---|---|
| Indicación por serie | Campo `executionCue` (String opcional) en el set, en plantilla y rutina. |
| Nombre en la UI | "Indicación" (con ayuda contextual). **No** llamarlo "nota". |
| Tipo de dato | String libre. **Sin enum rígido.** UI ofrece chips de sugerencia. |
| Dónde se edita | **Solo en modo avanzado** de series. El modo simple nunca la setea. |
| Regla de colapso | La indicación **participa** en la comparación de igualdad de series. |
| Series agrupadas | Nuevo `structuralType = GROUPED_SET` (enum interno neutro). |
| Label en la UI | Derivado del conteo: 2 → Biserie, 3 → Triserie, 4+ → Superserie. Título editable. |
| Vueltas | Se reusa `targetRounds` del bloque. |
| Descanso (V1) | Sin descanso entre ejercicios; descanso al terminar cada vuelta. |
| PDF de series agrupadas | Numeración simple (1, 2, 3). **No** usar A1/A2/A3 en PDF. |
| Editor de series agrupadas | Puede usar A1/A2/A3 o numeración para ordenar (uso interno del profesor). |

---

## 3. Feature 1 — Indicación por serie (`executionCue`)

### 3.1 Qué es

Un texto corto y opcional asociado a **cada serie** que describe cómo se ejecuta esa serie puntual: "recorrido completo", "parcial largo", "parcial corto", "pausa abajo", "explosivo", "isométrico 2s", etc. El profesor lo escribe libremente.

### 3.2 Dónde vive

- `RoutineExerciseSet.executionCue : String` (opcional, nullable).
- `TemplateExerciseSet.executionCue : String` (opcional, nullable).

**Las dos entidades.** Si solo se agrega en rutina, las plantillas no pueden guardar la indicación y se pierde al crear rutina desde plantilla.

- Columna DB sugerida: `execution_cue VARCHAR(120)`, nullable, en `template_exercise_sets` y `routine_exercise_sets`.

### 3.3 UI

- Label: **"Indicación"** con ayuda contextual (placeholder/tooltip): *"Ej: recorrido completo, parcial largo, parcial corto"*.
- **Solo visible/editable en modo avanzado.** En modo simple no aparece (el modo simple sigue generando N series idénticas y colapsables).
- Chips de sugerencia no obligatorios que autocompletan el input: `Completo` · `Parcial largo` · `Parcial medio` · `Parcial corto` · `+ Otra`. El profesor puede ignorarlos y escribir lo que quiera.

### 3.4 Regla de colapso (decisión central)

Hoy, las series idénticas de un ejercicio se colapsan en PDF/WhatsApp (ej: `3×12 · 20 kg`). La regla nueva agrega `executionCue` a la comparación:

> Dos series solo se colapsan si coinciden en **todos** los campos comparados, **incluida `executionCue`**.

- Si todas las series son idénticas **y** ninguna tiene `executionCue` (todas null/vacías) → **se colapsan** (comportamiento de hoy, intacto).
- Si las series difieren en `executionCue` (o alguna la tiene) → **se muestran serie por serie**, igual que ya se hace con las pirámides.

Implementación: agregar `executionCue` a la clave de comparación existente. **No** se reescribe la lógica de colapso; se le suma un campo. Antes de tocarla, el implementador debe ver el método de comparación actual y reportar qué campos compara hoy.

### 3.5 Salida en PDF y WhatsApp

Se **reusa el camino de expansión que ya existe para pirámides**. No se construye un render nuevo. Cuando las series se expanden, se pinta la indicación al final de cada línea.

PDF (ejemplo):
```
Curl con barra
1ra · 12 reps · 20 kg · recorrido completo
2da · 12 reps · 20 kg · parcial largo
3ra · 12 reps · 20 kg · parcial corto
```

WhatsApp (ejemplo):
```
▶ Curl con barra
1ra · 12 · 20kg · completo
2da · 12 · 20kg · parcial largo
3ra · 12 · 20kg · parcial corto
```

Si no hay indicaciones y las series son iguales, se sigue mostrando `3×12 · 20 kg`.

### 3.6 Reglas de copia (no negociable)

`executionCue` debe sobrevivir a **los tres caminos de copia**:
- Crear rutina desde plantilla (copia profunda).
- Duplicar rutina.
- Finalizar y crear próximo ciclo.

Y el **ajuste porcentual de pesos NO debe tocar `executionCue`** (no es un dato de carga).

### 3.7 Cargas previas e historial

Si una serie tiene `executionCue`, debe mostrarse (solo lectura) en cargas previas y en el historial por ejercicio. No necesita el mismo protagonismo en todos lados, pero si existe, no se pierde.

---

## 4. Feature 2 — Series agrupadas (`GROUPED_SET`)

> Esta feature se implementa **después** de `executionCue`, en su propia rama. Acá quedan fijadas las decisiones; los detalles finos de persistencia del descanso se cierran al iniciar `feature/grouped-set-blocks`.

### 4.1 Qué es

Un tipo de bloque donde varios ejercicios se ejecutan como una unidad, por vueltas. Es "como el circuito pero por series en vez de por tiempo".

### 4.2 Modelo

- Nuevo valor `GROUPED_SET` en el enum `BlockStructuralType` (interno, neutro).
- Se **reusa la estructura de bloque que ya existe**: un bloque ya contiene varios ejercicios y ya tiene `targetRounds`, `title` (texto libre) y `blockNotes`.
- Cada ejercicio del grupo lleva **una sola fila de objetivo** (ej: 10 reps · 30 kg). Las vueltas viven en `targetRounds`.
- Migración Flyway para actualizar el `CHECK constraint` de `structural_type` en **`template_blocks` y `routine_blocks`** (las dos).

### 4.3 Label derivado del conteo

La UI deriva el subtipo de la cantidad de ejercicios:
- 2 → **Biserie**
- 3 → **Triserie**
- 4+ → **Superserie**

El `title` del bloque sigue siendo editable, así el profesor escribe "Triserie — Hombros" sin pelear con definiciones académicas. (Nota: "Superserie = 4+" es terminología propia de esta app; en la jerga estándar "superserie" suele ser 2. Como el título es libre, no genera problema.)

### 4.4 Descanso (regla V1)

- **Sin descanso entre ejercicios** (implícito).
- **Descanso al terminar cada vuelta.**
- Si un caso necesita descanso entre ejercicios, se aclara en `blockNotes` por ahora.
- El mecanismo exacto de persistencia del descanso de vuelta se define al iniciar `feature/grouped-set-blocks` (sin agregar campos nuevos si se puede evitar). Más adelante, si el cliente usa mucho la variante con descanso entre ejercicios, se evalúa un campo dedicado.

### 4.5 Salida en PDF y WhatsApp

Numeración simple, **sin A1/A2/A3** (puede confundir al alumno):
```
Triserie — Hombros · 3 vueltas

1. Press militar · 10 reps · 30 kg
2. Elevaciones laterales · 12 reps · 8 kg
3. Pájaros · 12 reps · 6 kg

Sin descanso entre ejercicios. Descansar 90s al terminar cada vuelta.
```

El **editor** sí puede usar A1/A2/A3 o numeración visual, porque ahí ayuda al profesor a entender el orden del grupo.

### 4.6 Copia profunda

`structuralType` y `targetRounds` ya se copian hoy (son campos del bloque), así que las series agrupadas vienen casi gratis al reusar la estructura. Igual hay que **verificarlo con tests** en los tres caminos de copia.

---

## 5. Criterios de aceptación (se vuelven tests)

### `executionCue`
- AC1 — Un `RoutineExerciseSet` y un `TemplateExerciseSet` pueden guardar `executionCue` opcional.
- AC2 — Crear rutina desde plantilla copia `executionCue` en cada serie.
- AC3 — Duplicar rutina copia `executionCue`.
- AC4 — Finalizar y crear próximo ciclo copia `executionCue`.
- AC5 — El ajuste porcentual de pesos **no** modifica `executionCue`.
- AC6 — 3 series idénticas **sin** `executionCue` → se colapsan a `3×12 · 20 kg` en PDF y WhatsApp.
- AC7 — 3 series con mismo reps/peso/descanso pero **distinto** `executionCue` → se muestran serie por serie, cada una con su indicación, en PDF y WhatsApp.
- AC8 — `executionCue` solo se setea en modo avanzado; el modo simple nunca la genera.
- AC9 — `executionCue`, si existe, aparece (solo lectura) en cargas previas e historial por ejercicio.

### `GROUPED_SET`
- AC10 — Un bloque puede tener `structuralType = GROUPED_SET`, persistido con el CHECK constraint actualizado en `template_blocks` y `routine_blocks`.
- AC11 — La UI deriva el label por conteo (2/3/4+ → Biserie/Triserie/Superserie); el título queda editable.
- AC12 — PDF/WhatsApp renderizan el bloque con numeración simple (1, 2, 3), las vueltas y el descanso de vuelta. Sin A1/A2/A3 en PDF.
- AC13 — Copia profunda, duplicar y próximo ciclo preservan `structuralType` y `targetRounds`.

---

## 6. Fuera de alcance

- Enum cerrado de indicaciones (queda como string libre).
- Descanso entre ejercicios configurable en `GROUPED_SET` (V1 = sin descanso entre ejercicios).
- A1/A2/A3 en el PDF del alumno.
- 21s "puro" (7+7+7 dentro de una sola serie) como estructura especial — se cubre con `executionCue` por serie.
- CRUD de tags, notas compactas, cabecera PDF, WhatsApp al alumno — son otras mejoras, no parte de este ADR.

---

## 7. Orden de implementación

1. `feature/execution-cue`: backend → editor → salidas (PDF/WhatsApp/historial/cargas previas) → QA.
2. `feature/grouped-set-blocks`: backend → editor → salidas → cargas previas/historial → QA.

Quick wins (Ver/Editar, redondeo) son independientes del dominio y se intercalan donde convenga.
