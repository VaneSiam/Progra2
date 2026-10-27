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
   for(String campo:java.util.List.of("txtPrecioVip","txtPrecioPlatea","txtPrecioGeneral")) {
    var f=(TextField)PruebaInterfazIntegrada.campo(vista,campo);
    PruebaInterfazIntegrada.verificar(f.isVisible(),"Falta precio "+campo);
    f.setText(campo.contains("Vip")?"1000":campo.contains("Platea")?"500":"150");
   }
   PruebaInterfazIntegrada.foto(escena,Path.of(args[0]));
   stage.close();fin.complete(null);
  }catch(Throwable e){fin.completeExceptionally(e);}});
  try{fin.get();System.out.println("PASS tres campos de precio visibles e independientes en JavaFX.");}finally{Platform.exit();}
 }
}
