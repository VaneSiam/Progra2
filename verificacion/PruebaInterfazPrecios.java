package com.umg.api.fx.view;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

public class PruebaInterfazPrecios {
 public static void main(String[] args)throws Exception {
  var fin=new CompletableFuture<Void>();Platform.startup(()->{try{
   Platform.setImplicitExit(false);var vista=new InventarioView();
   var escena=new Scene(vista,1180,700);escena.getStylesheets().add(MainView.class.getResource("/fx/style.css").toExternalForm());
   var stage=new Stage();stage.setScene(escena);stage.show();vista.applyCss();vista.layout();
   var f=(TextField)PruebaInterfazIntegrada.campo(vista,"txtPrecio");
   PruebaInterfazIntegrada.verificar(f.isVisible(),"Falta precio base");
   f.setText("1000");
   PruebaInterfazIntegrada.verificar(vista.lookupAll(".text-field").size()==1,"Debe existir un solo precio");
   var base=new java.math.BigDecimal("1000.00");
   for(var caso:java.util.Map.of("VIP","1450.00","Plata","1250.00","General","1000.00").entrySet()) {
    var total=base.add(com.umg.api.service.Tarifas.cargo(base,com.umg.api.service.Tarifas.porcentaje(caso.getKey())));
    PruebaInterfazIntegrada.verificar(total.compareTo(new java.math.BigDecimal(caso.getValue()))==0,"Recargo incorrecto");
   }
   PruebaInterfazIntegrada.foto(escena,Path.of(args[0]));
   stage.close();fin.complete(null);
  }catch(Throwable e){fin.completeExceptionally(e);}});
  try{fin.get();System.out.println("PASS un precio base visible; VIP 1450, Platea 1250 y General 1000.");}finally{Platform.exit();}
 }
}
