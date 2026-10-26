package com.umg.api.fx.view;

import com.umg.api.config.Sesion;
import com.umg.api.fx.FxApp;
import com.umg.api.model.usuario;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** Ventana principal: barra superior + pestañas según el rol. */
public class MainView extends BorderPane {

    public MainView(usuario u) {
        Label titulo = new Label("🎟  Boletería");
        titulo.getStyleClass().add("titulo-app");
        String nombre = u.getNombre_completo() == null || u.getNombre_completo().isBlank()
                ? u.getNombre_usuario() : u.getNombre_completo();
        Label quien = new Label(nombre + "  ·  " + (u.getRol() == null ? "vendedor" : u.getRol()));
        Button salir = new Button("Cerrar sesión");
        salir.setOnAction(e -> {
            Sesion.cerrar();
            FxApp.mostrarLogin(null);
        });
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox barra = new HBox(14, titulo, espacio, quien, salir);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("barra");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        agregar(tabs, "Punto de venta", new PuntoVentaView());
        agregar(tabs, "Clientes", new ClientesView());
        agregar(tabs, "Comprobantes", new ComprobantesView());
        if (Sesion.esAdmin()) {                         // el rol decide qué ve cada quien
            agregar(tabs, "Artistas", new ArtistasView());
            agregar(tabs, "Conciertos", new ConciertosView());
            agregar(tabs, "Localidades e inventario", new InventarioView());
            agregar(tabs, "Usuarios", new UsuariosView());
            agregar(tabs, "Reportes", new ReportesView());
        }

        tabs.getSelectionModel().selectedItemProperty().addListener((o, a, t) -> recargar(t));
        recargar(tabs.getSelectionModel().getSelectedItem());

        setTop(barra);
        setCenter(tabs);
    }

    private static void agregar(TabPane tabs, String titulo, Node vista) {
        Tab t = new Tab(titulo, vista);
        t.setUserData(vista);
        tabs.getTabs().add(t);
    }

    private static void recargar(Tab t) {
        if (t != null && t.getUserData() instanceof Recargable r) r.recargar();
    }
}
