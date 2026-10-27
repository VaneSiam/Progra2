package com.umg.api.fx.view;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
public class ComprobantesView extends VBox {
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
        Label nota = new Label("El comprobante se envía automáticamente después de vender. Aquí puedes consultar o reintentar sin volver a cobrar.");
        nota.setWrapText(true);
        Label estadoCorreo = new Label();
        estadoCorreo.setWrapText(true);
        Runnable refrescarEstado = () -> estadoCorreo.setText(com.umg.api.config.Sesion.token()!=null
            ? "Correo: mediante API. El servidor administra el envío."
            : (com.umg.api.service.CorreoService.configurado() ? "Gmail local configurado. " : "Gmail local sin configurar. ")
                + "Archivo: " + com.umg.api.service.CorreoService.archivoConfiguracion());
        Button configurar = new Button("Configurar Gmail en este equipo");
        configurar.setOnAction(e -> {
            javafx.scene.control.Dialog<javafx.util.Pair<String,String>> dialogo = new javafx.scene.control.Dialog<>();
            dialogo.initOwner(getScene().getWindow()); dialogo.setTitle("Configurar Gmail");
            dialogo.setHeaderText("Cuenta remitente y contraseña de aplicación de Gmail (16 letras)");
            var usuario = new javafx.scene.control.TextField(); usuario.setPromptText("remitente@gmail.com");
            var clave = new javafx.scene.control.PasswordField(); clave.setPromptText("Contraseña de aplicación, no tu contraseña normal");
            var aviso = new Label("Activa la verificación en dos pasos y crea la clave en myaccount.google.com/apppasswords. Se guarda solo en este equipo.");aviso.setWrapText(true);
            dialogo.getDialogPane().setContent(new VBox(10,aviso,usuario,clave));
            dialogo.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK,javafx.scene.control.ButtonType.CANCEL);
            dialogo.setResultConverter(b -> b==javafx.scene.control.ButtonType.OK ? new javafx.util.Pair<>(usuario.getText(),clave.getText()) : null);
            dialogo.showAndWait().ifPresent(datos -> {
                try {com.umg.api.service.CorreoService.configurar(datos.getKey(),datos.getValue());refrescarEstado.run();com.umg.api.fx.util.Ui.info("Configuración guardada. Envía una venta de prueba para comprobar Gmail.");}
                catch(Exception ex){com.umg.api.fx.util.Ui.error(ex.getMessage());}
            });
            clave.clear();
        });
        refrescarEstado.run();
        getChildren().addAll(titulo,ayuda,guardar,nota,estadoCorreo,configurar,enviar);
    }
}
