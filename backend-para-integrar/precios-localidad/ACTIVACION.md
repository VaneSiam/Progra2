# Activar precios por localidad para la presentación

Parche independiente de los cambios anteriores de correo. Se basa en
`28381a89419bafc40190613e201725ca1989e9e2` de Proyectoapi2026 y añade un módulo,
una llamada de registro en localidades.route.js y pruebas. No cambia el correo ni las contraseñas. Conserva las tasas de recargo y añade tres columnas numéricas opcionales para guardar su desglose histórico.

1. Guardar cambios pendientes y crear una rama desde la versión actual de la API.
2. Aplicar `git am /ruta/0001-precios-por-localidad.patch`. Si hay conflictos,
   resolverlos conservando los cambios del equipo; no forzar ni restaurar archivos.
3. Ejecutar `npm ci` y `npm run test:precios` (cuatro pruebas con BD simulada).
4. Publicar la rama, revisar/fusionar a la rama desplegada en Render y redesplegar.
5. Verificar `GET https://proyectoapi2026.onrender.com/api/localidades/configuracion-precios`:
   debe responder `precios_por_localidad: true`.
6. Abrir el Java nuevo: Generar asientos permite precios base VIP, Platea y General.

El nombre interno histórico `Plata` equivale a Platea en la interfaz. No cambiar
ese identificador sin migrar también la lógica de recargos del equipo.

Nuevos endpoints:
- GET /api/localidades/configuracion-precios: capacidad, moneda y localidades.
- POST /api/localidades/generar-por-localidad: id_concierto y precios {VIP,Plata,General}.
- PUT /api/localidades/precios/:id: precios {VIP,Plata,General}; solo asientos disponibles.

Se crean 100 asientos: 10 VIP, 20 Platea y 70 General. La generación usa una
transacción y bloquea el concierto para evitar duplicados concurrentes. No se
permite generar otro lote si el concierto ya tiene inventario. La actualización
no toca inventario vendido ni detalles de ventas; aplica el precio nuevo a ventas
futuras de los asientos disponibles. Las cuatro pruebas usan repositorio simulado;
la transacción real PostgreSQL debe verificarse en la BD de pruebas del equipo.

Estos endpoints conservan el modelo de acceso de las rutas heredadas de catálogos;
la autorización general del backend sigue siendo responsabilidad de su integración.
No se necesita desplegar el parche OAuth/correo para usar esta función.

El arranque añade precio_base, recargo_porcentaje y recargo_monto con ALTER TABLE
ADD COLUMN IF NOT EXISTS. Se requiere permiso ALTER en PostgreSQL; no borra datos
ni rellena ventas anteriores con precios actuales. El servicio espera la migración
antes de aceptar solicitudes. Las ventas nuevas conservan el desglose que imprime
el PDF actualizado. Las antiguas sin esos campos indican que no quedó registrado.
