package com.umg.api.fx;

import com.umg.api.fx.view.LoginView;
import com.umg.api.fx.view.MainView;
import com.umg.api.model.usuario;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Aplicación JavaFX: cambia entre la pantalla de login y la ventana principal. */
public class FxApp extends Application {
    private static Stage stage;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("Boletería — Venta en puntos físicos");
        mostrarLogin(null);
        stage.show();
    }

    public static void mostrarLogin(String aviso) {
        stage.setMinWidth(0);
        stage.setMinHeight(0);
        stage.setMaximized(false);
        cambiarEscena(new LoginView(FxApp::mostrarPrincipal, aviso), 460, 460);
        stage.centerOnScreen();
    }

    public static void mostrarPrincipal(usuario u) {
        cambiarEscena(new MainView(u), 1320, 780);
        stage.setMinWidth(1100);
        stage.setMinHeight(640);
        stage.centerOnScreen();
    }

    private static void cambiarEscena(Parent raiz, double ancho, double alto) {
        Scene escena = new Scene(raiz, ancho, alto);
        var css = FxApp.class.getResource("/fx/style.css");
        if (css != null) escena.getStylesheets().add(css.toExternalForm());
        stage.setScene(escena);
    }
}
