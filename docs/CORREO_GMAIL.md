# Configurar Gmail una vez en esta computadora

La aplicación conserva el envío SMTP directo que agregó el equipo. Si la API
entrega una sesión del servicio de correo, usa ese servicio; no cambia de modo
automáticamente después de fallar un envío.

## Pasos

1. En la cuenta Gmail que ENVIARÁ los comprobantes, activar verificación en dos pasos.
2. Abrir https://myaccount.google.com/apppasswords y crear una contraseña de aplicación.
3. En Java abrir **Comprobantes y QR → Configurar Gmail en este equipo**.
4. Escribir el Gmail remitente y esa contraseña de aplicación de 16 letras. No es la contraseña normal de Google. No compartirla por chat.
5. Pulsar **Probar conexión Gmail (sin enviar correo)**. Comprueba red, STARTTLS y autenticación sin crear una venta ni enviar un mensaje.
6. Usar **Enviar comprobante por correo / consultar estado** con una venta propia existente, o con cualquier venta si es administrador. Para recuperar la venta #45 no se vuelve a cobrar.
7. Revisar Enviados del remitente y bandeja/spam del destinatario. Que el servidor acepte el envío no garantiza entrega en bandeja.

Si falta configurar Gmail, se solicita antes de crear una compra nueva. Cancelar
esa configuración no crea la venta ni modifica el carrito. No se oculta un fallo
de correo ni se informa que fue enviado cuando no se pudo confirmar.

## Configuración que se conserva al descargar otro ZIP

En Windows se guarda en `%APPDATA%\SVB-GUA\correo.properties`, fuera del proyecto.
El registro de envíos queda en la subcarpeta `envios`, para evitar reenvíos al
abrir otra copia del programa en el mismo usuario de la computadora. En otros
sistemas, se usa `~/.config/SVB-GUA`. No se incluyen claves en el ZIP ni en Git.

Se sigue admitiendo el archivo antiguo `correo.properties` en la carpeta actual
del proyecto, y las variables GMAIL_USUARIO/GMAIL_CLAVE. Si solo existía el archivo
antiguo en otra carpeta, usar Configurar Gmail una vez para guardarlo en la
ubicación permanente; no se buscan contraseñas en otras carpetas.

## Errores

- **Gmail rechazó la cuenta o clave:** comprobar la contraseña de aplicación, no la normal.
- **No se pudo conectar:** comprobar internet y permiso de salida SMTP 587 en esa red.
- **Cliente con datos pendientes:** corregirlo en Clientes, donde se muestran los errores por campo antes de guardar.
- **Envío incierto:** revisar Gmail Enviados antes de reintentar. Nunca repetir el cobro ni borrar el registro para intentar enviarlo otra vez.

La prueba automatizada usa SMTP local cifrado y credenciales ficticias. La entrega
por Gmail real requiere la cuenta de su propietario configurada en esa computadora.
