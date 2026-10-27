# Validación de formularios

Reglas aplicadas en los formularios JavaFX activos y también en el parche de la API:

| Campo | Regla |
| --- | --- |
| Nombre, apellido y nombre completo | Obligatorios, letras Unicode (tildes y ñ), sin dígitos. |
| Nombre artístico y género | Obligatorios, sin dígitos. Se permite puntuación como AC/DC, R&B y apóstrofos. |
| País | Opcional, letras sin dígitos si se completa. |
| Ciudad y tipo de evento | Obligatorios, letras sin dígitos. |
| Correo del cliente | Obligatorio, formato local@dominio.extensión, sin espacios. No confirma existencia del buzón. |
| Teléfono | Exactamente 8 dígitos de Guatemala. Permite espacios y guiones; se guardan solo los 8 números. |
| NIT | El campo admite solo números. Para CF se marca Consumidor final, sin escribir letras. |
| Precio | Mayor que cero, hasta ocho enteros y dos decimales. Rechaza NaN, infinito y notación científica. Java acepta punto o coma decimal y envía número. |
| Usuario de acceso | Letras ASCII, números, punto, guion y guion bajo. |
| Título, recinto y dirección | Admiten números: Gira 2026, Estadio 2, Zona 10. Longitud limitada y sin controles. |
| Fecha y horario | Fecha válida y hora obligatoria en formato HH:mm. |
| Rol y estado | Selección de las opciones permitidas. |

Las longitudes se limitan según cada campo. La contraseña conserva su política de
al menos seis y hasta 200 caracteres y admite números y símbolos.

Las restricciones de nombres y teléfono se aplican al escribir y pegar en campos
con foco. Nombre y apellido no muestran mensajes rojos; el NIT vacío muestra solo «Obligatorio». Los valores heredados se muestran al seleccionar un registro, aunque
sean inválidos, para poder corregirlos. Guardar siempre valida el valor completo:
no se borra ni se modifica automáticamente ningún registro existente.

Antes de crear una venta, el servicio Java vuelve a consultar el cliente y exige
correo válido y teléfono correcto. El formulario de venta también lo comprueba
y explica que deben corregirse en Clientes. La API comprueba ambos datos antes
de escribir detalles. Un error no debe resolverse repitiendo una compra incierta.

En la API, crear valida todos los campos obligatorios. Editar combina los datos
existentes con los cambios antes de validar, de modo que tampoco se pueda dejar
un cliente antiguo con correo inválido mediante una edición parcial. Las
lecturas y eliminaciones no se alteran.

## Comprobante y QR actuales

El comprobante contiene número y fecha de venta (hora de Guatemala), vendedor,
cliente, teléfono, evento, tipo de entrada, cantidades, precios, subtotales,
total y observaciones. Cada boleto incluye evento, fecha, recinto, localidad y
asiento, precio, referencia de boleto/venta y QR.

En esta integración el QR abre el registro de detalle existente en la API. Si la
API proporciona `qr_url`, se usa esa URL validando su dominio. La página visual
y la firma del QR de la integración antigua del backend no están activadas por
este parche de correo. Escanear no registra ingreso ni consumo del boleto. La
lista adicional que la usuaria haya indicado en otra conversación debe
confirmarse antes de cambiar el contenido del QR.

## Verificación

Compilación Maven, `verificacion/PruebaValidaciones.java`, nueve casos Node y
regresión de integración Java → Express con el PDF real y correo simulado.
La API debe publicar el parche actualizado para aplicar reglas a solicitudes
remotas. La rama de Java por sí sola no modifica Render.
