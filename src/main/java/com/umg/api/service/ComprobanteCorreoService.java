package com.umg.api.service;

import com.umg.api.config.ApiConfig;
import com.umg.api.config.Sesion;
import com.umg.api.reportes.BoletosPdf;
import java.util.Base64;
import java.util.Map;

/** Solo consulta y envía una venta existente: nunca registra ni cobra boletos. */
public class ComprobanteCorreoService {
    public String enviar(int id, Integer vendedorPermitido) throws Exception {
        if (!Sesion.activa()) throw new IllegalStateException("Inicia sesión primero.");
        // El equipo incorporó SMTP directo: conservarlo si no hay sesión del endpoint de correo.
        // Nunca cambiar de transporte tras un fallo: podría duplicarse un envío ya aceptado.
        if (Sesion.token() == null) {
            return enviarLocal(id,vendedorPermitido,false);
        }
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
    /** Reenvío solicitado por el operador; utiliza el Gmail configurado en este equipo. */
    public String reenviar(int id) throws Exception {
        if (!Sesion.activa()) throw new IllegalStateException("Inicia sesión primero.");
        Integer vendedor = Sesion.esAdmin() ? null : Sesion.actual().getId_usuario();
        return enviarLocal(id,vendedor,true);
    }
    private String enviarLocal(int id,Integer vendedorPermitido,boolean reenvio) throws Exception {
            if(!CorreoService.configurado()) throw new IllegalStateException("Configura Gmail antes de enviar el comprobante desde Comprobantes y QR > Configurar Gmail.");
            var documento=new ComprobanteService().cargar(id,vendedorPermitido);
            String destino=com.umg.api.validacion.Validaciones.correo(documento.correo());
            // Generar antes de reservar, para que errores de datos o PDF no bloqueen el envío.
            java.nio.file.Path pdf=java.nio.file.Files.createTempFile("boletos-venta-"+id+"-",".pdf");
            try {
                BoletosPdf.exportar(documento,pdf);
                CorreoService smtp=new CorreoService();
                RegistroCorreoLocal.Envio accion = ()->smtp.enviar(destino,
                    "Comprobante de tu compra · Venta #"+id,
                    "Hola "+documento.cliente()+". Adjuntamos el comprobante y tus boletos con QR. Total pagado: Q"+documento.total().setScale(2)+". Documento académico, no fiscal.",
                    pdf,"boletos-venta-"+id+".pdf");
                return reenvio ? RegistroCorreoLocal.reenviar(id,accion) : RegistroCorreoLocal.ejecutar(id,accion);
            } finally {java.nio.file.Files.deleteIfExists(pdf);}
    }

}
