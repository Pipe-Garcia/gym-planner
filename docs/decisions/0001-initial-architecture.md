# 0001 - Initial Architecture

## Estado

Aceptada

## Decision

Gym Planner se organiza como monorepo con `backend/` y `frontend/`.

El backend usa Spring Boot 3, Java 21 y PostgreSQL. La persistencia se versiona con Flyway desde el inicio y Hibernate queda en `ddl-auto=validate`.

El frontend usa React, TypeScript, Vite, Tailwind CSS y componentes de estilo shadcn/ui.

La organizacion del backend sera por feature. En este primer corte existen solo `auth`, `gym`, `user`, `config` y `shared`.

La autenticacion inicial usa JWT stateless con HS256 y expiracion de 12 horas. No se incluyen refresh tokens, cookies httpOnly ni logout server-side en esta etapa.

## Consecuencias

- El monorepo simplifica cambios fullstack coordinados y CI por area.
- Flyway evita depender de generacion automatica de schema.
- La organizacion por feature facilita sumar alumnos, ejercicios, plantillas y rutinas en prompts posteriores.
- JWT en localStorage es suficiente para esta V1 inicial, con el costo conocido de endurecer seguridad en una version posterior.
