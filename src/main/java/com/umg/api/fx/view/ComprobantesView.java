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
        getChildren().addAll(titulo,ayuda,guardar,nota,enviar);
    }
}
