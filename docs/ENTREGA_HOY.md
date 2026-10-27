# Versión integrada para la presentación

## Abrir el proyecto correcto

Descargar el ZIP de la rama main de VaneSiam/Progra2 y extraerlo en una carpeta nueva y abrir esa carpeta desde
NetBeans (la que contiene pom.xml). Requiere JDK 26 para JavaFX 26. Ejecutar
Clean and Build y luego Run Project. No mezclar archivos con un ZIP anterior.
La versión de `VaneSiam/Progra2` incorpora los cambios de Gmail y mapa del equipo
(`aruizv5-commits/proyectoNeatbeans2026`, commit bc7a1cd), junto con validaciones,
desglose de cargos, nuevos precios y búsqueda de clientes. No incorpora el archivo
privado correo.properties de ningún compañero.

## Precios de asientos

Localidades e inventario tiene tres precios base: VIP, Platea y General.
Generar crea 10 VIP, 20 Platea y 70 General. Si el concierto ya tiene asientos,
usar Actualizar precios disponibles: los boletos vendidos no se modifican.
Los precios son base: al vender se conservan los recargos existentes VIP 45%,
Platea 25%, General 0%. El punto de venta muestra el desglose de la entrada
seleccionada y la suma de base y recargos. El mapa permite agregar/quitar asientos.

**Requiere una actualización pequeña en Render.** El backend que estaba publicado
al revisar no implementaba precios separados. Entregar al compañero
`backend-para-integrar/precios-localidad/0001-precios-por-localidad.patch` y su
ACTIVACION.md. Este parche es independiente: no requiere OAuth, configuración SMTP ni el parche anterior de correo. Añade tres columnas opcionales para conservar el desglose de cargos; necesita permiso ALTER TABLE en PostgreSQL. Si la API no está actualizada,
Java avisa y no envía escrituras que creen los 100 asientos con un precio erróneo.
No aplicar ambos parches de precios y correo sin revisar la base y lo ya integrado.

Pasos del compañero en Git Bash dentro de Proyectoapi2026 (guardar cambios antes):

```sh
git fetch origin
git switch -c precios-localidad origin/main
curl -L https://raw.githubusercontent.com/VaneSiam/Progra2/main/backend-para-integrar/precios-localidad/0001-precios-por-localidad.patch -o /tmp/precios.patch
git am /tmp/precios.patch
npm ci
npm run test:precios
git push -u origin precios-localidad
```

Revisar/fusionar la rama y redesplegar el servicio. Verificar en el navegador:
`https://proyectoapi2026.onrender.com/api/localidades/configuracion-precios`.
Debe responder `precios_por_localidad: true`. Si git am muestra conflicto,
detenerse para resolverlo, no forzar ni reemplazar archivos del equipo.

## Buscar clientes

En Punto de venta escribir nombre, apellido o NIT en el campo de búsqueda.
Tolera tildes y el guion del NIT. Elegir el cliente de la lista filtrada.
Si no coincide nadie, muestra «No se encontró cliente…». Si el cliente no tiene
NIT o tiene CF, muestra «El cliente no contiene NIT registrado (consumidor final)».
La búsqueda no crea clientes ni altera sus datos.

## Gmail: la causa de «solo Venta registrada»

El equipo implementó SMTP directamente en Java, no en Render. En su versión,
si faltaba la configuración local, se omitía el correo sin mostrar aviso.
La versión integrada informa que el correo queda pendiente y cómo configurarlo.
No se promete entrega si no se configuró y verificó Gmail en esa computadora.

1. En el Gmail que ENVIARÁ los comprobantes, activar verificación en dos pasos.
2. Abrir https://myaccount.google.com/apppasswords y crear una contraseña de aplicación.
3. En Java ir a Comprobantes y QR > Configurar Gmail en este equipo.
4. Ingresar correo remitente y contraseña de aplicación de 16 letras. No usar la contraseña normal de Gmail. No pegarla en el chat.
5. Pulsar Enviar comprobante por correo / consultar estado e ingresar una venta propia ya registrada (por ejemplo la #43, si se está usando el vendedor correcto o administrador). No volver a cobrar.
6. Revisar tanto Gmail Enviados del remitente como bandeja/spam del destinatario.

La configuración queda en correo.properties en la ruta que muestra el módulo.
No se sube a GitHub ni se incluye en el ZIP. Gmail requiere salida SMTP 587 desde
la computadora; si la red institucional lo bloquea, el error aparecerá y debe
comprobarse desde una red que permita SMTP. Render no participa en este modo.

Si al iniciar sesión la API entrega un token del endpoint de comprobantes, Java
utiliza el envío por API ya implementado. No cambia a SMTP después de un error
para no duplicar un mensaje aceptado. La pantalla muestra cuál modo está activo.
El modo local registra aceptaciones en .svb-correo dentro de esta instalación:
evita doble clic y reenvíos en ese equipo, pero no coordina distintas computadoras.
Un timeout queda incierto y requiere revisar Enviados antes de reenviar. No borrar
ese registro ni repetir el cobro para intentar corregir el envío.

## Comprobaciones realizadas

- Clean and Build Maven sin parámetros especiales de compilación: correcto con JDK 26.
- Pruebas Java de validaciones, búsquedas, avisos NIT, configuración local sin claves reales y registro de envíos.
- Prueba visual JavaFX de navegación, mapa, carrito persistente, filtro, aviso NIT y controles en ventana compacta.
- 14 pruebas Node del backend combinado; cinco pruebas independientes del parche pequeño de precios.
- Contrato de precios Java → Express: payload separado y API antigua bloqueada antes de escribir.
- Regresión Java → Express de compra, PDF JasperReports, QR legible y un envío simulado.
- Consulta de la API publicada: venta #43 con tres detalles y total Q5,550.00.

No se crearon ventas ni asientos de prueba en la BD compartida ni se enviaron
correos reales. La entrega SMTP debe probarse con la cuenta configurada por su
propietario; el parche de precios requiere despliegue y prueba PostgreSQL del equipo.
