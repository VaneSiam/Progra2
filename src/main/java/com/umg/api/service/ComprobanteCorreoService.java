package com.umg.api.service;

import com.umg.api.config.ApiConfig;
import com.umg.api.config.Sesion;
import com.umg.api.reportes.BoletosPdf;
import java.util.Base64;
import java.util.Map;

/** Solo consulta y envía una venta existente: nunca registra ni cobra boletos. */
public class ComprobanteCorreoService {
    public String enviar(int id, Integer vendedorPermitido) throws Exception {
        if (!Sesion.activa() || Sesion.token() == null)
            throw new IllegalStateException("Falta activar el backend de correo o iniciar sesión nuevamente. El comprobante queda pendiente.");
        String ruta = "/comprobantes/" + id + "/correo";
        String estado = HttpJson.get(ruta).path("estado").asText();
        if ("aceptado".equals(estado)) return "Gmail ya había aceptado el comprobante; no se duplicó el envío.";
        if (!"pendiente".equals(estado))
            throw new IllegalStateException("Envío en proceso o resultado incierto. Revisa Gmail antes de reenviar.");
        byte[] pdf = BoletosPdf.generar(new ComprobanteService().cargar(id, vendedorPermitido));
        if (pdf.length > 5 * 1024 * 1024) throw new IllegalArgumentException("El PDF supera 5 MiB. Contacta al administrador.");
        var respuesta = HttpJson.post(ApiConfig.url("comprobantes/" + id + "/correo"),
                Map.of("pdf_base64", Base64.getEncoder().encodeToString(pdf)), com.fasterxml.jackson.databind.JsonNode.class);
        if (!"aceptado".equals(respuesta.path("estado").asText()))
            throw new IllegalStateException("No se confirmó el envío. Consulta su estado antes de reintentar.");
        return "Gmail aceptó el comprobante y los boletos con QR para su envío.";
    }
}
