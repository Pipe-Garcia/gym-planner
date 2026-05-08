# Gym Planner Runbook

## Deploy Futuro

- Frontend: Vercel o Netlify.
- Backend: Render, Railway o una VM con Java 21.
- Base de datos: Neon, Supabase o PostgreSQL administrado.

## Variables Necesarias

Backend:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS`
- `LOG_LEVEL`
- `SPRING_PROFILES_ACTIVE=prod`

Frontend:

- `VITE_API_URL`

## Healthcheck

El backend expone:

```text
GET /actuator/health
```

En produccion, el proveedor de hosting deberia usar ese endpoint para healthchecks.

## Backups

Pendiente definir:

- Frecuencia de backup de PostgreSQL.
- Retencion minima.
- Procedimiento de restore.
- Pruebas periodicas de recuperacion.
