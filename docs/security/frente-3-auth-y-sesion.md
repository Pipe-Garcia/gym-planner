# Frente 3 — Auth y sesión

> Auditoría de seguridad de Gym Planner, previa a la carga de datos reales de clientes (que incluyen datos de salud). Este documento cierra el Frente 3. Los Frentes 1 (aislamiento multi-tenant + IDOR) y 2 (exposición de datos sensibles) están cerrados y mergeados a `main`.

## Alcance

Auditar la superficie de autenticación y sesión: login público, ciclo de vida del JWT, enumeración de usuarios, y resistencia a fuerza bruta.

Contexto de uso que calibra la severidad: en V1 hay **un solo usuario OWNER por gimnasio** (no hay TRAINERs; un segundo cliente es otro Gym con su propio OWNER). El login es una ruta pública expuesta a internet (backend en Render). El token JWT dura 12h, se guarda en localStorage, y no hay refresh ni logout server-side (decisiones conscientes de diseño V1).

Explícitamente fuera de alcance (derivado a otros frentes): CORS y headers de seguridad (Frente 5), dónde se guardan los secretos en producción (Frente 5). La *validación* de que el secreto JWT tiene largo suficiente sí se auditó acá.

## Resultado central

La autenticación estaba, en su núcleo, bien construida. Confirmado durante el Paso 0:

- La firma y la expiración del JWT se validan en cada request.
- `alg:none` es rechazado (no se acepta un token sin firma).
- **El `gymId` se recarga de la base en cada request, no se confía en el claim del token.** Esto conecta con el Frente 1: aunque alguien manipulara el token, no podría cambiar su `gymId` para acceder a otro gimnasio. El aislamiento no depende de confiar en el token.
- BCrypt strength 12.
- El frontend limpia el token correctamente ante logout y 401, sin bucles.
- `check-phone` resultó ser búsqueda interna legítima (autenticada, gym-scoped), no enumeración pública: clasificado NO BUG.

No hubo que rediseñar la autenticación. Se encontraron cuatro cosas concretas que corregir, resueltas en tres fixes.

## Hallazgos

| ID | Clasificación | Exposición | Hallazgo | Estado |
|---|---|---|---|---|
| F3-AUTH-04 | Brecha activa | Pública | Seed (V2) crea un OWNER con credenciales conocidas y publicadas (`admin@gymplanner.local` / `admin123`, password en el README). Corre en cualquier base nueva, incluida producción. | Cerrado (F3-1) |
| F3-AUTH-03 | Brecha activa | Autenticada | Un usuario desactivado (`active=false`) conservaba acceso con un JWT vigente hasta la expiración (12h): el filtro construía la autenticación sin verificar `isEnabled()`. | Cerrado (F3-2) |
| F3-AUTH-02 | Defensa ausente | Pública | El login no tenía ningún límite de intentos. Con un solo OWNER por gym, ese OWNER es un blanco único de fuerza bruta. | Cerrado (F3-3) |
| F3-AUTH-01 | Fuga de timing | Pública | El login respondía más rápido cuando el email no existía (cortaba antes de hashear) que cuando existía con password incorrecta, permitiendo enumerar usuarios. | Cerrado (F3-3) |
| — | Hardening | Autenticada | Un JWT válido de un usuario eliminado hacía que `UsernameNotFoundException` se escapara del filtro y pudiera terminar en 500. | Cerrado (F3-2) |
| — | Hallazgo del Paso 0 de F3-3 | Prod | `server.forward-headers-strategy` no estaba configurado: Spring veía la IP del proxy de Render, no la del cliente. Sin arreglarlo, el rate limiting habría agrupado a todos los usuarios bajo una IP. | Cerrado (F3-3) |

## Fixes aplicados

### F3-2 — Validez actual del usuario en el filtro JWT (kill-switch)

`JwtAuthenticationFilter` ahora verifica `isEnabled()` antes de establecer la autenticación: un usuario desactivado con token vigente termina en 401 limpio, de inmediato (no espera la expiración del token). Además, `UsernameNotFoundException` se incluyó en el manejo de errores del filtro, de modo que un token válido de un usuario eliminado también termina en 401 limpio en vez de escaparse como 500.

Los seis casos de borde terminan en el **mismo** 401 con el mismo mensaje, de modo que son indistinguibles entre sí: token válido + usuario activo → 200; inactivo → 401; eliminado → 401; expirado → 401; malformado → 401; firma inválida → 401. Tests de integración con Bearer real atravesando el filtro (no `.with(user(...))`, que saltearía el filtro).

Conexión importante: F3-2 es lo que hace que "desactivar un usuario" sea una acción de seguridad efectiva. Sin él, el kill-switch no cerraría de inmediato, y la neutralización de F3-1 no sería instantánea.

### F3-1 — Neutralización de la credencial bootstrap conocida

El hallazgo real no era la cuenta en la demo (descartable), sino que el seed corre en cualquier base nueva, incluida producción.

Estrategia forward-only, sin tocar V2 (editarla cambiaría su checksum y Flyway fallaría al arrancar). Se agregó una migración nueva (V14) con **predicado estrecho**: neutraliza únicamente la fila que todavía conserve la credencial insegura, identificada por email conocido **Y** password_hash conocido (el string exacto de V2), no por `id=1`. Si la password ya fue rotada, o la fila ya fue reclamada por el cliente real, el hash no coincide y la migración no toca nada. Esto la hace segura de correr en bases nuevas, en la demo con password rotada, y en una base ya reclamada.

Para la fila que coincide: `active=false`, email cambiado a un tombstone bajo dominio `.invalid` (invalida JWT viejos con ese email y evita reactivación accidental), password_hash reemplazado por un BCrypt de una password aleatoria de alta entropía que no se almacena en ningún lado. La fila se conserva (no se borra) para preservar integridad referencial: el `gym=1` sostiene el catálogo de tags, y el `user=1` es autor de registros.

Además: credenciales eliminadas del README, prefill del email vaciado en el LoginPage, y test de regresión sobre Testcontainers (cadena PostgreSQL real) que verifica que `gym=1` y sus tags siguen existiendo, que `admin123` ya no autentica, y que no queda la combinación conocida activa.

Se verificó que desactivar `user=1` no rompe los tests que lo usan como autor: los servicios lo cargan con `getReferenceById` sin chequear `active`. Los tests que usaban el email conocido como fixture se actualizaron a una identidad de test separada (`owner@test.local`), desacoplando la identidad de test de la credencial insegura sin reactivar nada inseguro.

### F3-3 — Antiabuso del login (IP real + rate limiting + timing constante)

Tres partes:

1. **IP real (solo prod):** `server.forward-headers-strategy: framework` en `application-prod.yml` para que Spring procese `X-Forwarded-For` detrás del proxy de Render. Se agregó un extractor de IP (`ClientIpExtractor`) que toma la IP confiable de la cadena de forwarded headers (la que antepone Render, no una posición controlable por el cliente), con fallback a `getRemoteAddr()` en entornos sin proxy. Configurado solo en prod a propósito: habilitar el parsing de forwarded headers en un entorno sin proxy confiable permitiría spoofing de IP.

2. **Rate limiting** con Bucket4j en memoria (sin Redis), por IP, solo sobre el endpoint de login, con comportamiento **fail-open**: si el rate limiter falla internamente, la request pasa (el login sigue funcionando). El rate limiting es defensa secundaria y nunca debe poder dejar al OWNER afuera de su propio sistema por un error propio. Al superar el límite: HTTP 429 con `Retry-After` y mensaje genérico. Por IP y no por email, y sin account lockout, para no habilitar un denial-of-service contra el único OWNER.

3. **Timing constante:** cuando el email no existe, el login ahora ejecuta igualmente una comparación BCrypt contra un hash señuelo (BCrypt real de cost 12), de modo que el tiempo de respuesta es indistinguible del caso password incorrecta. Los mensajes de error ya eran idénticos entre ambos casos.

Nota de implementación: durante F3-3 el matching de ruta del filtro usaba `getServletPath()`, inconsistente entre MockMvc y el contenedor real. Se corrigió a `getRequestURI()` menos el context path, consistente en test y en producción. La corrección fue en el filtro (matching robusto), no en los tests: se eliminó una preparación artificial de path del test unitario, de modo que los tests de integración validan el filtro real.

Suite completa en verde: 344 tests.

## Decisiones de diseño registradas

- **Rate limiting por IP, no por email; sin account lockout.** Bloquear por email o cuenta permitiría a un atacante hacer denial-of-service contra el único OWNER fallando a propósito. Rate limiting por IP es la mitigación correcta para un sistema de OWNER único.
- **Rate limiting in-memory (Bucket4j), no Redis.** Proporcional a V1 (un backend, un OWNER). La pérdida del contador ante un reinicio del backend es un trade-off aceptable, mitigado por el timing constante que encarece cada intento.
- **Fail-open.** El rate limiting es defensa secundaria; su falla no debe bloquear el login legítimo.
- **Provisioning del primer cliente = procedimiento manual, no código.** Se crea un solo OWNER real para la primera entrega; automatizarlo sería over-engineering. Documentado en el runbook y ensayado en local (ver abajo).
- **Riesgos aceptados de V1** (documentados, no se tocan): JWT en localStorage, duración de 12h, sin refresh tokens, sin blacklist/revocación, logout solo en el cliente, cambio de password/role no revoca tokens ya emitidos.

## Ensayo del runbook (provisioning)

El procedimiento de reclamo de la cuenta bootstrap se ensayó en la base local antes de aplicarlo en producción, validando el runbook. Baches encontrados y registrados:

- El reclamo se hace con un cliente gráfico (DBeaver), no con `psql` en Git Bash: en Windows, `psql` nativo + Git Bash ignora los argumentos de línea de comandos.
- Dry-run obligatorio: SELECT con el mismo WHERE antes del UPDATE, para confirmar visualmente qué fila se toca. DBeaver confirma cuántas filas se modifican (debe ser 1).
- Generación del hash con un test JUnit temporal (BCrypt cost 12, password por variable de entorno, nunca hardcodeada), que se borra después y nunca se commitea.

## Deuda registrada (fuera de este frente)

**Requisito antes del SEGUNDO cliente (bloqueante para onboarding multi-tenant):**
- Provisioning programático repetible que cree Gym + OWNER + **copia del catálogo de tags default** para el nuevo gym. Hoy los tags están sembrados solo en `gym=1`; un gimnasio nuevo nacería sin tags. El primer cliente (Sergio) reclama `gym=1` justamente para heredar esos tags. El procedimiento manual del runbook NO alcanza para un segundo cliente.
- Decisión de fondo asociada: tags globales del sistema vs. tags por-tenant.

**Evolución futura del rate limiting (no urgente, no bloquea producción):**
- Observabilidad/métricas del rate limiting (cuántas veces se activa, IPs bloqueadas).
- Persistencia del contador entre reinicios (migrar Bucket4j a un backend distribuido si algún día hay Redis).
- Rate limiting en otros endpoints sensibles además del login.

**Operativo (pre-producción):**
- El `forward-headers-strategy` y el rate limiting solo entran en juego en el perfil prod (detrás del proxy de Render), por lo que se validan plenamente al desplegar a producción, no en local. Conecta con la lista de infra/pre-producción (Neon Launch, rotación de credenciales, backups).

## Estado

Frente 3 **cerrado**. Commits en la rama `security/frente-3-auth-y-sesion`:

- `fix(security): enforce user validity in JWT filter` (F3-2)
- `fix(security): neutralize known bootstrap owner credentials` (F3-1)
- `docs(runbook): add practical notes from local provisioning rehearsal` (ensayo)
- `fix(security): add login rate limiting and constant-time auth` (F3-3)

Merge a `main` con `--no-ff` (unidad coherente de historia, como los Frentes 1 y 2). Siguiente: Frente 4 — inyección y SSRF (unaccent, logoUrl, escaping Thymeleaf).
