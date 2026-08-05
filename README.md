# Gym Planner

Gym Planner es una aplicacion web fullstack para gestionar la operacion de un gimnasio. Este scaffolding inicial deja lista la base tecnica con autenticacion, datos del gimnasio y una estructura preparada para los modulos de negocio futuros.

## Stack

- Backend: Java 21, Spring Boot 3.5.13, Spring Web, Spring Data JPA, Spring Security, Actuator, Flyway, PostgreSQL 16, JJWT 0.12.7, Lombok, MapStruct, springdoc-openapi.
- Frontend: React 19.1.1, TypeScript 5.9.3, Vite 7.1.4, Tailwind CSS, shadcn/ui-style components, React Router, TanStack Query, React Hook Form, Zod, Axios, date-fns, lucide-react.
- Infraestructura: Docker Compose para PostgreSQL local y GitHub Actions para CI.

## Estructura

```text
backend/     API Spring Boot
frontend/    App React + TypeScript + Vite
docs/        Documentacion y decisiones tecnicas
```

## Requisitos Locales

- Java 21.
- Node.js 20 o superior.
- Docker Desktop o Docker Engine.
- npm para el frontend.

## Setup Local

1. Copiar variables de entorno si queres personalizarlas:

```bash
cp .env.example .env
cp frontend/.env.example frontend/.env
```

2. Levantar PostgreSQL:

```bash
docker compose up -d
```

Si ya tenes PostgreSQL usando el puerto 5432, defini `POSTGRES_PORT=5433` en tu `.env` local y ajusta `DB_URL=jdbc:postgresql://localhost:5433/gym_planner`.

3. Correr backend:

```bash
cd backend
./mvnw spring-boot:run
```

En Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

4. Correr frontend:

```bash
cd frontend
npm install
npm run dev
```

El frontend queda disponible en `http://localhost:5173` y el backend en `http://localhost:8080`.

## Provisioning del primer OWNER

Una instalacion nueva no incluye credenciales utilizables. El primer OWNER se
provisiona manualmente siguiendo el
[runbook de provisioning](docs/runbook/provisioning-primer-owner.md).
No cargues datos reales antes de completar y verificar ese procedimiento.

## Variables de Entorno

Backend:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS`
- `LOG_LEVEL`

PostgreSQL local:

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `POSTGRES_PORT`

Frontend:

- `VITE_API_URL`

Spring Boot no lee automaticamente archivos `.env`; las variables reales deben estar disponibles en el entorno del proceso.

## Tests y Build

Backend:

```bash
cd backend
./mvnw clean install
```

Frontend:

```bash
cd frontend
npm install
npm run typecheck
npm run lint
npm run build
```

## Endpoints Iniciales

- `POST /api/auth/login`
- `GET /api/me`
- `GET /api/gym/current`
- `PUT /api/gym/current`
- `GET /actuator/health`
- Swagger en dev: `/swagger-ui/index.html`

Ejemplo login:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"OWNER_EMAIL","password":"OWNER_PASSWORD"}'
```

## Proximos Modulos

Los siguientes prompts deberian implementar, en orden, alumnos y ejercicios. Este scaffolding no incluye CRUDs de alumnos, ejercicios, plantillas, rutinas, PDF, WhatsApp, pagos, cuotas, asistencia, mantenimiento de maquinas ni portal de alumnos.
