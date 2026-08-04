# Provisioning manual del primer OWNER

## Contexto

Una base nueva ejecuta la migración V14 y arranca con el usuario bootstrap
neutralizado. La aplicación puede iniciar, pero no existe un OWNER utilizable
hasta completar este procedimiento.

El primer cliente debe reclamar `gym.id=1` y `user.id=1`. Los tags default se
siembran únicamente para `gym.id=1`; crear otro gimnasio en esta etapa lo
dejaría sin esos tags. Esta es una restricción bootstrap temporal, no la
arquitectura definitiva del producto.

No cargar datos reales antes de completar el provisioning y verificar el
acceso del OWNER.

## Requisitos

- Acceso administrativo directo a la base PostgreSQL.
- Datos reales y confirmados del gimnasio y del OWNER.
- Una herramienta offline confiable para generar BCrypt con cost 12.
- Una ventana de trabajo en la que todavía no se hayan cargado datos reales.

## Procedimiento

1. Generar offline un hash BCrypt cost 12 de la contraseña real del OWNER.
   Usar una herramienta que solicite la contraseña de forma interactiva y
   confirmar que el resultado indique cost 12. No pasar la contraseña como
   argumento de línea de comandos ni guardarla en texto plano.
2. Abrir una transacción administrativa y actualizar `gyms.id=1` con los datos
   reales: `name`, `owner_name`, `phone`, `email`, `address`, `logo_url`,
   `primary_color` y `updated_at`, según corresponda.
3. En la misma transacción, actualizar `users.id=1` con el email y nombre
   reales, el `password_hash` generado en el paso anterior, `active=true` y
   `updated_at=now()`. Antes de confirmar, verificar que la fila encontrada sea
   el bootstrap neutralizado y pertenezca a `gym_id=1`.
4. Confirmar la transacción. No guardar en Git, documentación, tickets ni
   scripts compartidos ninguna copia del SQL ya completado con datos sensibles.
5. Verificar el login con las credenciales del OWNER real.
6. Consultar `GET /api/me` con el JWT obtenido y confirmar los datos del OWNER,
   su rol y `gymId=1`.
7. Confirmar que la credencial bootstrap histórica documentada en V2 ya no
   autentica.
8. Registrar fuera de Git la fecha, el entorno y la persona responsable de la
   provisión. No registrar la contraseña ni su texto plano.
9. Recién después de estas verificaciones habilitar la carga de datos reales.

## Entorno demo existente

Antes de desplegar V14 a una demo que todavía use la credencial bootstrap,
rotar manualmente su contraseña si se desea conservar el acceso. Al cambiar el
hash antes de V14, el predicado estrecho de la migración no modifica esa
cuenta. Si no se rota, V14 la neutraliza y la demo deberá ser reclamada mediante
este mismo procedimiento.

## Deuda antes del segundo cliente

Antes de provisionar un segundo cliente se necesita un mecanismo programático,
repetible y auditable que cree Gym + OWNER y copie los tags default. También
debe decidirse explícitamente si los tags serán globales o pertenecerán a cada
tenant. Esa evolución queda fuera de F3-1.


## Notas prácticas (aprendidas del ensayo en local)

- **El reclamo de la cuenta se hace con un cliente gráfico (DBeaver), NO con
  psql en Git Bash.** En Windows, el psql nativo + Git Bash ignora los
  argumentos de línea de comandos (`-c` y `-f`), lo que hace inviable ejecutar
  scripts SQL desde esa terminal. DBeaver conectado a la base (local o Neon en
  producción) evita el problema y además permite ver la fila antes y después
  del cambio.

- **Dry-run obligatorio antes del UPDATE:** ejecutar primero un SELECT con el
  mismo WHERE para confirmar visualmente qué fila se va a tocar. Recién después,
  el UPDATE. DBeaver pide confirmación mostrando cuántas filas se modificarán:
  debe decir exactamente 1.

- **Generación del hash:** se usó un test JUnit temporal (BCryptPasswordEncoder
  cost 12, password recibida por variable de entorno, no hardcodeada). El
  archivo del generador se BORRA después de usarlo y nunca se commitea. La
  password en texto plano no queda en ningún archivo del repo.

- En producción, además, la password del OWNER real la elige el cliente y NO se
  pega en ningún canal (chat, archivo, historial de terminal). Para el hash en
  producción, usar input oculto para que el texto plano no quede ni en el
  historial de la terminal.