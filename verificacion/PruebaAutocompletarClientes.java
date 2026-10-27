package com.umg.api.fx.view;
import com.umg.api.config.Sesion;
import com.umg.api.model.cliente;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.event.Event;
import javafx.stage.Stage;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

public class PruebaAutocompletarClientes {
    static cliente cliente(int id,String nombre,String nit,String telefono){return new cliente(id,nombre,"Ejemplo","Zona 1","cliente@example.invalid",telefono,nit,true);}
    static void texto(Object vista,String campo,String valor)throws Exception{((TextField)PruebaInterfazIntegrada.campo(vista,campo)).setText(valor);}
    static void entrar(javafx.scene.Node nodo){Event.fireEvent(nodo,new KeyEvent(KeyEvent.KEY_PRESSED,"","",KeyCode.ENTER,false,false,false,false));}
    public static void main(String[] args)throws Exception {
        CompletableFuture<Void> fin=new CompletableFuture<>();
        Platform.startup(()->{try{
            Platform.setImplicitExit(false);Sesion.iniciar(PruebaInterfazIntegrada.usuario("administrador"),null);
            var p=PruebaInterfazIntegrada.venta();
            var clientes=(ObservableList<cliente>)PruebaInterfazIntegrada.campo(p,"clientesTodos");
            var sofia=cliente(1,"Ana María","12345678","12345678");var otra=cliente(2,"María","12345999","12345678");
            clientes.setAll(sofia,otra);
            Stage ventana=new Stage();Scene escena=PruebaInterfazIntegrada.escena(p,1180,720);ventana.setScene(escena);ventana.show();
            var buscar=(TextField)PruebaInterfazIntegrada.campo(p,"buscarCliente");
            var sugerencias=(ListView<cliente>)PruebaInterfazIntegrada.campo(p,"sugerenciasClientes");
            var combo=(ComboBox<cliente>)PruebaInterfazIntegrada.campo(p,"cbCliente");
            buscar.requestFocus();buscar.setText("12345");
            PruebaInterfazIntegrada.verificar(sugerencias.isVisible() && sugerencias.getItems().size()==2,"No desplegó coincidencias parciales");
            PruebaInterfazIntegrada.foto(escena,Path.of(args[0],"busqueda-autocompletar.png"));
            sugerencias.getSelectionModel().select(otra);entrar(sugerencias);
            PruebaInterfazIntegrada.verificar(combo.getValue()==otra && !sugerencias.isVisible(),"Enter no seleccionó el cliente");
            buscar.setText("12345678");PruebaInterfazIntegrada.verificar(sugerencias.isVisible() && sugerencias.getItems().size()==1,"No encontró NIT exacto");
            entrar(buscar);PruebaInterfazIntegrada.verificar(combo.getValue()==sofia,"Enter no completó el NIT exacto");
            // Al faltar Gmail, cancelar su configuración debe conservar el carrito y no iniciar la compra.
            var configAbierta=new java.util.concurrent.atomic.AtomicBoolean();
            Platform.runLater(()->{
                for(var w:java.util.List.copyOf(javafx.stage.Window.getWindows())) {
                    if(w instanceof Stage st && "Configurar Gmail".equals(st.getTitle())) {configAbierta.set(true);st.close();}
                }
            });
            Button cobrar=(Button)p.lookupAll(".button").stream().filter(n->n instanceof Button b && b.getText().equals("Cobrar")).findFirst().orElseThrow();
            cobrar.fire();
            PruebaInterfazIntegrada.verificar(configAbierta.get(),"No pidió configurar Gmail antes de vender");
            PruebaInterfazIntegrada.verificar(((java.util.List<?>)PruebaInterfazIntegrada.campo(p,"carrito")).size()==1,"Alteró el carrito al cancelar Gmail");
            buscar.setText("ana maria");PruebaInterfazIntegrada.verificar(sugerencias.getItems().contains(sofia),"No encontró nombre sin tilde");
            buscar.setText("1234-5678");PruebaInterfazIntegrada.verificar(sugerencias.getItems().contains(sofia),"No toleró guion");
            buscar.setText("sin coincidencias");PruebaInterfazIntegrada.verificar(!sugerencias.isVisible() && sugerencias.getItems().isEmpty(),"No vació las sugerencias");
            var formulario=new ClientesView();ventana.setScene(PruebaInterfazIntegrada.escena(formulario,1180,720));
            texto(formulario,"txtNombre","Ana María");texto(formulario,"txtApellido","Ejemplo");texto(formulario,"txtNit","12345678");
            texto(formulario,"txtTelefono","1234567");texto(formulario,"txtCorreo","no-es-correo");
            var guardar=(Button)PruebaInterfazIntegrada.campo(formulario,"guardar");
            var etiquetas=(java.util.Map<String,Label>)PruebaInterfazIntegrada.campo(formulario,"erroresCampo");
            PruebaInterfazIntegrada.verificar(!etiquetas.get("nombre").isVisible() && !etiquetas.get("apellido").isVisible(),"Mostró mensajes rojos en nombres");
            texto(formulario,"txtNit","");
            ((TextField)PruebaInterfazIntegrada.campo(formulario,"txtNit")).requestFocus();
            PruebaInterfazIntegrada.verificar(etiquetas.get("nit").getText().equals("Obligatorio"),"El NIT no mostró el mensaje corto");
            texto(formulario,"txtNit","12345678");
            PruebaInterfazIntegrada.verificar(guardar.isDisabled(),"Permitió guardar contacto inválido");
            var errores=(java.util.Map<String,Label>)PruebaInterfazIntegrada.campo(formulario,"erroresCampo");
            PruebaInterfazIntegrada.verificar(errores.get("telefono").isVisible() && errores.get("correo").isVisible(),"No mostró errores en Clientes");
            PruebaInterfazIntegrada.foto(ventana.getScene(),Path.of(args[0],"validacion-clientes.png"));
            texto(formulario,"txtTelefono","1234-5678");texto(formulario,"txtCorreo","cliente@example.invalid");
            PruebaInterfazIntegrada.verificar(!guardar.isDisabled(),"No habilitó Guardar al corregir los datos");
            var espera=new javafx.animation.PauseTransition(javafx.util.Duration.millis(150));
            espera.setOnFinished(evento->{try {
            var campoTelefono=(TextField)PruebaInterfazIntegrada.campo(formulario,"txtTelefono");campoTelefono.requestFocus();
            campoTelefono.replaceText(0,campoTelefono.getLength(),"1234 5678");
            PruebaInterfazIntegrada.verificar(campoTelefono.getText().equals("1234 5678"),"Rechazó teléfono con espacios");
            campoTelefono.appendText("9");PruebaInterfazIntegrada.verificar(campoTelefono.getText().equals("1234 5678"),"Aceptó noveno dígito");
            campoTelefono.appendText("A");PruebaInterfazIntegrada.verificar(!campoTelefono.getText().contains("A"),"Aceptó letra en teléfono");
            var nombre=(TextField)PruebaInterfazIntegrada.campo(formulario,"txtNombre");nombre.requestFocus();nombre.appendText("2");
            PruebaInterfazIntegrada.verificar(!nombre.getText().contains("2"),"Aceptó dígito en nombre");
            var nit=(TextField)PruebaInterfazIntegrada.campo(formulario,"txtNit");nit.requestFocus();nit.appendText("K");
            PruebaInterfazIntegrada.verificar(!nit.getText().contains("K"),"Aceptó letra en NIT");
            ventana.close();fin.complete(null);
            }catch(Throwable e){fin.completeExceptionally(e);}});espera.play();
        }catch(Throwable e){fin.completeExceptionally(e);}});
        try{fin.get(30,java.util.concurrent.TimeUnit.SECONDS);System.out.println("PASS interfaz: sugerencias parciales/exactas, Enter, nombre con tilde, NIT con guion y validación en Clientes antes de guardar.");}
        finally{Platform.exit();}
    }
}
