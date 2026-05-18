# 0009 - Cargas previas en editor de rutinas

## Contexto
Cuando el profesor crea o edita rutinas, necesita saber que pesos manejo el alumno
en su ultima aparicion con ese ejercicio para decidir la progresion. Hoy debe abrir
rutinas anteriores manualmente, lo cual es lento y frustra el flujo de "finalizar
ciclo -> crear proximo".

## Decision
Implementar un endpoint de solo lectura que devuelve la(s) ultima(s) aparicion(es)
de un ejercicio para un alumno dado, con contexto suficiente (rutina, dia, bloque,
sets) para que el profesor pueda decidir sin abrir otra pantalla.

El frontend (en una proxima entrega, seccion 4.3.B) lo mostrara como panel
colapsable en cada ExerciseInBlockRow del editor de rutinas. No aplica a plantillas
porque no tienen alumno asociado.

## Alcance
- Backend: GET /api/students/{studentId}/exercises/{exerciseId}/previous-loads
- Lectura pura, sin cambios de schema, sin migraciones.
- Soporta limit (default 1, max 3) y excludeRoutineId.

## Reglas
- Excluye routines en estado DRAFT.
- Excluye la routine actual cuando excludeRoutineId esta presente.
- Multi-tenancy estricto: filtra por gymId del usuario autenticado.
- No expone internalNotes, closureNotes, lesiones ni studentNotes.
- Compara por exerciseId (no por nombre).
- Orden por COALESCE(finishedDate, assignedDate) DESC, id DESC.
- Implementacion en dos queries para evitar problemas de fetch join + limit.

## Lo que NO se hace en 4.3
- No filtra por purpose del bloque (cualquier aparicion vale en V1).
- No autocompleta valores en el set actual (informa, no decide).
- No genera graficos ni timeline (eso sera 4.4).

## Consecuencias
- Facilita decisiones de progresion sin abrir multiples rutinas.
- Sienta la base de datos consultable para el historial completo (4.4).
- Permite extender a limit>1 desde el dia uno sin cambios estructurales.
