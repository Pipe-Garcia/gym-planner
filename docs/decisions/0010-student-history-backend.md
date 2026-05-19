# 0010 - Student history backend

## Decision

Student history is exposed through dedicated read-only endpoints under `/api/students/{studentId}/history`.

## Context

The routines tab is operational: create, edit, duplicate, download, finish, and archive routines. Previous loads are contextual assistance inside the routine editor. Student history has a different goal: analyze the student's evolution across cycles and exercises.

## Rationale

- Separate endpoints keep each history section independently pageable and cheap to load.
- Timeline and exercise occurrences use DTOs only; JPA entities are never returned.
- `DRAFT` routines are excluded because they do not represent real training history.
- The 4.3 `previous-loads` implementation remains untouched to avoid changing a stable editor feature during 4.4.A.
- Exercise occurrences use a two-step query: page `RoutineExercise` IDs first, then fetch context and sets. This avoids paginating directly over fetched collections.

## Privacy

The history API never exposes routine `internalNotes`, student injuries, student notes, or medical/private data. `closureNotes` are exposed only in the private timeline endpoint. Occurrence responses intentionally omit `closureNotes`.
