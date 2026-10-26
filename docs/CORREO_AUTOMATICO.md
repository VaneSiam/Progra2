# Comprobante automático por correo

Después de confirmar la compra, el cliente genera con JasperReports el mismo PDF
que se puede guardar manualmente: comprobante y boletos con QR. Lo sube al endpoint
`POST /api/comprobantes/:id/correo` y el backend consulta el correo del cliente en
PostgreSQL y envía el adjunto mediante Gmail API HTTPS.

La pantalla confirma primero el número y total de la venta. Si falla el correo,
indica revisar Comprobantes y boletos y no repetir la compra. El botón
«Enviar comprobante por correo / consultar estado» utiliza una venta existente,
consulta si ya está enviada y solo intenta enviar las pendientes. El servidor
controla permisos y evita envíos duplicados. Si hay un resultado incierto, se
requiere revisar Gmail antes de desbloquearlo.

## Activar el backend

La integración completa para el repositorio de API está incluida en
`backend-para-integrar/java-correo/0001-correo-pdf-java.patch`, basada en el commit
`28381a89419bafc40190613e201725ca1989e9e2`. En un checkout limpio de esa versión,
aplicar con `git am /ruta/0001-correo-pdf-java.patch`, luego `npm ci` y `npm test`. El archivo actualizado contiene tres commits: correo, validaciones y desglose histórico de recargos.
Si se publica la rama de API que contiene el mismo commit, NO aplicar el parche
otra vez. No mezclar este parche con la integración anterior que genera PDF en Node.

Ver `backend-para-integrar/java-correo/ACTIVACION.md` para variables de Render,
OAuth de Gmail, estados persistentes, límites heredados de seguridad y pruebas.
No se necesita guardar una contraseña de correo ni una clave compartida en Java.
El usuario inicia sesión y recibe un token de ocho horas del backend actualizado.
Con la API antigua o sin activar se conserva el login anterior, pero el envío
explica que está pendiente de activar. Volver a iniciar sesión tras el despliegue.

Actualizar todos los clientes antes de activar `COMPROBANTE_JAVA_ENABLED=true`:
se apaga el correo anterior al guardar detalles para que no lleguen dos mensajes.
La API acepta un adjunto PDF de hasta 5 MiB; no se agrega destinatario ni BCC desde
Java. Gmail aceptado no significa entrega confirmada.

## Compilar y verificar

Compilar con el JDK compatible con JavaFX 26 del proyecto:

```sh
mvn -Dmaven.compiler.source=26 -Dmaven.compiler.target=26 package
```

Con las dependencias Maven y `target/classes` en el classpath, compilar
`verificacion/PruebaCorreoJava.java` y ejecutar la prueba Node `test/java-correo.cjs`
del backend pasando ese classpath y un directorio temporal. La prueba usa Express,
JasperReports y lectura de QR reales con datos ficticios; no manda correos reales.
Las reglas nuevas se documentan en `docs/VALIDACION_CAMPOS.md`.
Se verificó un PDF de tres páginas, Q1,600.00, dos boletos; una sola compra y un solo
envío aunque se consulte otra vez. La configuración de Gmail y el despliegue deben
validarse finalmente con una cuenta de prueba controlada.

## Precio base, recargos y estado del comprobante

El punto de venta muestra base y recargo por entrada, sumas de base y recargos,
y total final. Se conservan las tarifas del backend: VIP +45%, Plata +25% y
General sin recargo. Un VIP con base Q1,000 y una General Q150 suman Q1,600.
Las nuevas ventas guardan el desglose por boleto y el comprobante lo imprime.
Las antiguas sin desglose mantienen su precio final y explican que no está
registrado; no se reconstruye con el inventario actual.

El PDF muestra «PAGADO · venta registrada» y «EMITIDO · comprobante» tras validar
la compra. Pagado corresponde al cobro confirmado por el vendedor, no verifica
bancos ni pasarelas. Emitido indica generación de PDF, no entrega del correo.
El QR conserva la consulta del registro existente en la API.

El backend debe aplicar la migración aditiva al arrancar (tres columnas nuevas
con `ADD COLUMN IF NOT EXISTS`, sin rellenar registros antiguos). La cuenta
PostgreSQL necesita permiso ALTER TABLE. Revisar ACTIVACION.md antes de desplegar.
