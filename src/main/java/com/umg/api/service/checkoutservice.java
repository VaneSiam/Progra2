package com.umg.api.service;

import com.umg.api.config.ApiConfig;
import com.umg.api.model.ventas;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registrar una venta. La API lo hace en dos pasos:
 *   1. POST /api/ventas/create          -> crea la venta con total 0
 *   2. POST /api/detalle_ventas/create  -> agrega los asientos (id_inventario); la API calcula el total,
 *                                          descuenta el inventario y marca el asiento como ocupado
 * Cada id_inventario es un asiento, por eso cada boleto es de cantidad 1.
 */
public class checkoutservice {

    public record Item(int id_inventario, int cantidad) {}

    public ventas registrarVenta(int idVendedor, Integer idCliente, List<Item> items) throws Exception {
        if (idCliente == null) {
            throw new Exception("Selecciona un cliente para la venta.");
        }

        if (items == null || items.isEmpty()) throw new Exception("Selecciona al menos un boleto.");
        java.util.Set<Integer> unicos = new java.util.HashSet<>();
        for (Item item : items) {
            if (item.id_inventario() < 1 || item.cantidad() != 1 || !unicos.add(item.id_inventario()))
                throw new Exception("Selecciona asientos válidos y sin repetir; esta API vende uno por detalle.");
        }
        // Revisa el cliente actualizado antes de crear la cabecera de la compra.
        var clienteActual = HttpJson.get("/cliente/" + idCliente);
        com.umg.api.validacion.Validaciones.correo(clienteActual.path("correo").asText(""));
        com.umg.api.validacion.Validaciones.telefono(clienteActual.path("telefono").asText(""));
        // 1. crear la venta
        ventas nueva = new ventas(0, idVendedor, Instant.now().toString(), 0);
        ventas creada = new ventasservice().createVentas(nueva);

        // 2. agregar los asientos a la venta
        List<Integer> ids = new ArrayList<>();
        for (Item it : items) {
            ids.add(it.id_inventario());
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("id_venta", creada.getId_venta());
        cuerpo.put("id_cliente", idCliente);
        cuerpo.put("id_inventario", ids);
        final com.fasterxml.jackson.databind.JsonNode detalles;
        try {
            detalles = HttpJson.post(ApiConfig.url("detalle_ventas") + "/create", cuerpo,
                    com.fasterxml.jackson.databind.JsonNode.class);
        } catch (Exception ex) {
            throw new Exception("Se creó la venta #" + creada.getId_venta()
                    + ", pero no se confirmó el guardado de boletos. Revisa esa venta antes de repetir la compra. "
                    + ex.getMessage(), ex);
        }
        // La respuesta contiene los importes persistidos; evita que una consulta posterior
        // fallida haga aparecer como fallida una compra que sí se guardó.
        if (detalles == null || !detalles.isArray() || detalles.size() != items.size())
            throw new Exception("Venta #" + creada.getId_venta() + ": respuesta de detalles incompleta. No repitas la compra; revísala.");
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (var detalle : detalles) {
            if (detalle.path("id_venta").asInt() != creada.getId_venta() || !detalle.hasNonNull("subtotal"))
                throw new Exception("Venta #" + creada.getId_venta() + ": detalles inesperados. Revisa la compra sin repetirla.");
            total = total.add(new java.math.BigDecimal(detalle.get("subtotal").asText()));
        }
        creada.setTotal_venta(total.floatValue());
        return creada;
    }
}
