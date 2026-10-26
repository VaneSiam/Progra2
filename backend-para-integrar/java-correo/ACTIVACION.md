# Comprobante JasperReports generado en Java y enviado por Gmail

Java registra la compra con el flujo existente. Una vez confirmados los detalles,
genera un único PDF (comprobante y páginas de boletos QR), lo envía en base64 y
Node lo adjunta al correo del cliente consultado en PostgreSQL. No se acepta un
destinatario indicado por Java. No se genera otro PDF en Node.

## Activación en Render

1. Publicar esta rama del backend y ejecutar `npm ci` y `npm test`.
2. Configurar las variables de entorno en el panel del servicio (nunca en Git ni en Java):

   - `COMPROBANTE_JAVA_ENABLED=true`
   - `COMPROBANTE_SESSION_SECRET`: secreto aleatorio de 32 bytes o más. Generar con `node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"` en un equipo de confianza.
   - `MAIL_FROM`: dirección Gmail del remitente, sin nombre ni corchetes.
   - `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REFRESH_TOKEN`: credenciales OAuth de esa cuenta con autorización `https://www.googleapis.com/auth/gmail.send`.

3. Habilitar Gmail API en el proyecto de Google Cloud; configurar la pantalla de consentimiento y un cliente OAuth. Autorizar la cuenta remitente con acceso offline y obtener el refresh token. Si se usa OAuth Playground con credenciales propias, registrar `https://developers.google.com/oauthplayground` como URI de retorno. No compartir el token en el chat. Las aplicaciones externas en modo de prueba pueden emitir refresh tokens con vigencia de siete días; revisar el estado del consentimiento para uso sostenido.
4. Redesplegar. El arranque espera `sequelize.sync()` y crea únicamente la tabla nueva `comprobantes_correo` si no existe; no usa `force` ni `alter`. La cuenta PostgreSQL necesita permiso para crearla. La sincronización previa de las demás tablas conserva el comportamiento existente.
5. Usar el cliente Java actualizado y volver a iniciar sesión para recibir el token (ocho horas).
6. Con una cuenta de correo de prueba controlada, registrar una compra, revisar el mensaje de aceptación, la tabla `comprobantes_correo` y la bandeja/spam. La aceptación de Gmail no garantiza entrega.

Con `COMPROBANTE_JAVA_ENABLED=true` se desactiva únicamente el correo antiguo del
controlador de detalles para evitar el segundo mensaje y su BCC anterior. El
nuevo correo no incluye BCC. Todos los puntos de venta deben actualizar Java
antes de activar el cambio, pues el cliente antiguo no sube su PDF.

## Endpoints

- `POST /api/comprobantes/login`: `{nombre_usuario, contrasena}`. Devuelve token y datos públicos del usuario. Acepta las contraseñas actuales y bcrypt. Limita intentos por IP. Detrás del proxy de Render, revisar la configuración de `trust proxy` para que el límite use la IP real según la topología del servicio.
- `GET /api/comprobantes/:id/correo`: devuelve `pendiente`, `enviando`, `aceptado` o `incierto`.
- `POST /api/comprobantes/:id/correo`: `{pdf_base64}`. PDF de hasta 5 MiB; autenticación Bearer. Vendedor: solo sus propias ventas. Administrador: todas.

El PDF se valida por tamaño, base64 y marcadores PDF, no se analiza ni se compara
su contenido visual con la BD. El cliente Java oficial genera los importes y QR
a partir de la venta; este endpoint se destina a vendedores autorizados.

## Errores y reintentos

Una fila por venta y una actualización condicional en PostgreSQL reservan el envío.
Un comprobante `aceptado` no se envía otra vez. El registro sobrevive reinicios.
Si falta configuración, correo o detalles, no se intenta enviar. Un rechazo
confirmado permite volver a intentar desde Comprobantes y boletos. Un timeout,
error de red o resultado desconocido queda `incierto`, sin reintento automático.
Un proceso detenido durante el envío puede dejar `enviando`: tampoco se reenvía.

Antes de desbloquear uno de esos casos, el administrador debe verificar Gmail
Enviados y, si existe, `message_id`. Solo cuando confirme que no se envió,
puede cambiar manualmente esa fila a `pendiente` y reintentar desde Java.
El sistema no ofrece un botón que pueda duplicar un envío incierto. No hay cola
automática ni garantía de entrega exactamente una vez entre Gmail y PostgreSQL.

El fallo de correo nunca crea otra venta ni vuelve a descontar inventario. Java
informa el número de venta y permite consultar/reintentar el correo sin cobrar.
El PDF se procesa en memoria; el servidor conserva estado, hash SHA-256 y message_id,
no guarda archivos PDF ni contraseñas OAuth en la BD.

## Límites heredados del proyecto

Esta integración NO convierte en atómica la compra de dos solicitudes existente.
La validación de aforo, tarifas y transacción SQL de venta sigue en el controlador
existente. Java advierte el ID si falla el segundo paso para evitar repetir cobros.
Los demás endpoints CRUD heredados aún carecen de autorización y exponen datos
sensibles de usuarios: deben protegerse antes de operar con datos reales. La nueva
sesión protege las rutas nuevas, no sustituye esa corrección general de seguridad.
No se migran ni reescriben las contraseñas existentes.

## Verificación reproducible sin correos reales

`npm test`: nueve casos, incluyendo composición MIME real y Gmail simulado,
propiedad de ventas, concurrencia, reintentos, totales, PDF y errores.
Para la integración completa, compilar Java y `verificacion/PruebaCorreoJava.java`
y ejecutar `node test/java-correo.cjs '<classpath Java con clases de prueba>' '<directorio temporal>'`.
La prueba levanta Express en loopback, registra una compra ficticia y recibe un
PDF real generado por JasperReports: tres páginas, dos boletos QR, Q250.00.
No se accede a PostgreSQL compartido ni se envían correos reales. Las pruebas usan
un repositorio simulado; la migración y concurrencia deben comprobarse además en
PostgreSQL de pruebas antes de publicar en producción.

Las reglas de todos los formularios y las limitaciones del QR se describen en `VALIDACION_CAMPOS.md`.
