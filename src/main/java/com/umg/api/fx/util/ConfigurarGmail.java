package com.umg.api.fx.util;
import com.umg.api.service.CorreoService;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public final class ConfigurarGmail {
    private ConfigurarGmail() {}
    /** Se utiliza antes de vender y desde Comprobantes. Cancelar no crea una venta. */
    public static boolean mostrar(Window ventana) {
        Dialog<Boolean> dialogo=new Dialog<>();dialogo.initOwner(ventana);dialogo.setTitle("Configurar Gmail");
        dialogo.setHeaderText("Configura el correo remitente para enviar los comprobantes");
        TextField usuario=new TextField();usuario.setPromptText("remitente@gmail.com");
        String actual=CorreoService.remitenteConfigurado();if(actual!=null)usuario.setText(actual);
        PasswordField clave=new PasswordField();clave.setPromptText("Contraseña de aplicación de Google");
        Label instrucciones=new Label("Usa la contraseña de aplicación generada por Google. La configuración queda guardada en este equipo.");
        instrucciones.setWrapText(true);instrucciones.setMaxWidth(430);
        Hyperlink abrirGoogle=new Hyperlink("Crear contraseña de aplicación en Google");
        abrirGoogle.setOnAction(e->{
            try {
                if(!java.awt.Desktop.isDesktopSupported())throw new IllegalStateException();
                java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://myaccount.google.com/apppasswords"));
            } catch(Exception ex){Ui.info("Abre en tu navegador: https://myaccount.google.com/apppasswords");}
        });
        TextField visible=new TextField();visible.textProperty().bindBidirectional(clave.textProperty());
        CheckBox mostrar=new CheckBox("Mostrar contraseña");
        visible.setVisible(false);visible.setManaged(false);
        mostrar.selectedProperty().addListener((o,a,b)->{visible.setVisible(b);visible.setManaged(b);clave.setVisible(!b);clave.setManaged(!b);});
        Label error=new Label();error.setWrapText(true);error.getStyleClass().add("error");error.setMaxWidth(430);
        dialogo.getDialogPane().setContent(new VBox(10,instrucciones,abrirGoogle,new Label("Gmail que enviará los comprobantes"),usuario,clave,visible,mostrar,error));
        ButtonType guardar=new ButtonType("Guardar configuración",ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(guardar,ButtonType.CANCEL);
        dialogo.getDialogPane().lookupButton(guardar).addEventFilter(javafx.event.ActionEvent.ACTION,e->{
            try {CorreoService.configurar(usuario.getText(),clave.getText());}
            catch(Exception ex){error.setText(ex.getMessage());e.consume();}
        });
        dialogo.setResultConverter(b->b==guardar);
        boolean guardado=dialogo.showAndWait().orElse(false);clave.clear();return guardado;
    }
}
