package com.umg.api.fx.view;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
public class ComprobantesView extends VBox {
    private final Label estadoCorreo=new Label();
    private void refrescarEstado() {
        estadoCorreo.setText(com.umg.api.config.Sesion.token()!=null
            ? "Correo: mediante API. El servidor administra el envío."
            : com.umg.api.service.CorreoService.configurado()
                ? "Gmail configurado en este equipo. Se conserva al actualizar el proyecto."
                : "Configura una vez el Gmail remitente para enviar los comprobantes.");
    }
    public ComprobantesView() {
        setSpacing(15);setPadding(new Insets(20));
        Label titulo=new Label("Comprobantes y boletos");
        titulo.setStyle("-fx-font-size:20px;-fx-font-weight:bold;");
        Label ayuda=new Label("Guarda el comprobante de una venta existente y sus boletos con QR, sin volver a cobrar.");
        ayuda.setWrapText(true);
        Button guardar=new Button("Guardar comprobante y boletos PDF");
        guardar.getStyleClass().add("boton-primario");
        guardar.setOnAction(e->com.umg.api.reportes.BoletosPdf.guardar(getScene().getWindow()));
        Button enviar=new Button("Enviar comprobante por correo / consultar estado");
        enviar.setOnAction(e -> {
            if (!com.umg.api.config.Sesion.activa()) { com.umg.api.fx.util.Ui.error("Inicia sesión primero."); return; }
            if(com.umg.api.config.Sesion.token()==null && !com.umg.api.service.CorreoService.configurado()) {
                if(!com.umg.api.fx.util.ConfigurarGmail.mostrar(getScene().getWindow()))return;
            }
            refrescarEstado();
            var pregunta = new javafx.scene.control.TextInputDialog();
            pregunta.initOwner(getScene().getWindow());
            pregunta.setTitle("Comprobante por correo");
            pregunta.setHeaderText("Número de venta registrada. Se enviará al correo del cliente.");
            var entrada = pregunta.showAndWait();
            if (entrada.isEmpty()) return;
            final int id;
            try { id=Integer.parseInt(entrada.get().trim()); if(id<1)throw new NumberFormatException(); }
            catch(NumberFormatException ex) { com.umg.api.fx.util.Ui.error("Número de venta inválido."); return; }
            Integer vendedor = com.umg.api.config.Sesion.esAdmin() ? null : com.umg.api.config.Sesion.actual().getId_usuario();
            enviar.setDisable(true);
            com.umg.api.fx.util.Async.run(
                () -> new com.umg.api.service.ComprobanteCorreoService().enviar(id,vendedor),
                mensaje -> { enviar.setDisable(false); com.umg.api.fx.util.Ui.info(mensaje); },
                mensaje -> { enviar.setDisable(false); com.umg.api.fx.util.Ui.error(mensaje); });
        });
        Button reenviar=new Button("Reenviar comprobante al cliente");
        reenviar.setOnAction(e -> {
            if (!com.umg.api.config.Sesion.activa()) { com.umg.api.fx.util.Ui.error("Inicia sesión primero."); return; }
            if (!com.umg.api.service.CorreoService.configurado()
                    && !com.umg.api.fx.util.ConfigurarGmail.mostrar(getScene().getWindow())) return;
            int ultima=com.umg.api.fx.util.UltimaVenta.numeroProperty().get();
            var pregunta=new javafx.scene.control.TextInputDialog(ultima>0?String.valueOf(ultima):"");
            pregunta.initOwner(getScene().getWindow());
            pregunta.setTitle("Reenviar comprobante");
            pregunta.setHeaderText("Número de la venta existente");
            var entrada=pregunta.showAndWait();
            if(entrada.isEmpty())return;
            final int id;
            try {id=Integer.parseInt(entrada.get().trim());if(id<1)throw new NumberFormatException();}
            catch(NumberFormatException ex){com.umg.api.fx.util.Ui.error("Número de venta inválido.");return;}
            if(!com.umg.api.fx.util.Ui.confirmar("¿Reenviar el comprobante de la venta #"+id+" al correo registrado del cliente?\nSe enviará otra copia con los mismos boletos. No se cobrará otra vez."))return;
            reenviar.setDisable(true);enviar.setDisable(true);
            com.umg.api.fx.util.Async.run(
                () -> new com.umg.api.service.ComprobanteCorreoService().reenviar(id),
                msg -> {reenviar.setDisable(false);enviar.setDisable(false);com.umg.api.fx.util.Ui.info(msg);},
                msg -> {reenviar.setDisable(false);enviar.setDisable(false);com.umg.api.fx.util.Ui.error(msg);});
        });
        Label nota = new Label("El comprobante se envía automáticamente después de vender. Aquí puedes consultar o reintentar sin volver a cobrar.");
        nota.setWrapText(true);
        estadoCorreo.setWrapText(true);
        Button configurar = new Button("Configurar Gmail en este equipo");
        configurar.setOnAction(e -> {
            if(com.umg.api.fx.util.ConfigurarGmail.mostrar(getScene().getWindow()))refrescarEstado();
        });
        Button probar=new Button("Probar conexión Gmail (sin enviar correo)");
        probar.setOnAction(e -> {
            if(!com.umg.api.service.CorreoService.configurado() && !com.umg.api.fx.util.ConfigurarGmail.mostrar(getScene().getWindow()))return;
            refrescarEstado();probar.setDisable(true);estadoCorreo.setText("Comprobando conexión segura y autenticación con Gmail…");
            com.umg.api.fx.util.Async.run(()->{new com.umg.api.service.CorreoService().comprobarConexion();return true;},
                ok->{probar.setDisable(false);estadoCorreo.setText("Conexión y autenticación con Gmail correctas. Puedes enviar tu comprobante.");},
                msg->{probar.setDisable(false);estadoCorreo.setText(msg);});
        });
        refrescarEstado();
        getChildren().addAll(titulo,ayuda,guardar,nota,estadoCorreo,configurar,probar,enviar,reenviar);
    }
}
