package com.umg.api.fx.view;

import com.umg.api.config.Sesion;
import com.umg.api.fx.util.Async;
import com.umg.api.fx.util.Ui;
import com.umg.api.fx.util.Filtros;
import com.umg.api.validacion.Validaciones;
import com.umg.api.model.artista;
import com.umg.api.model.cliente;
import com.umg.api.model.concierto;
import com.umg.api.model.inventarios_boletos;
import com.umg.api.model.localidad;
import com.umg.api.model.ventas;
import com.umg.api.service.artistaservice;
import com.umg.api.service.checkoutservice;
import com.umg.api.service.clienteservice;
import com.umg.api.service.conciertoservice;
import com.umg.api.service.inventarios_boletosservice;
import com.umg.api.service.localidadservice;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Pantalla del vendedor:  [1] elegir concierto (filtros por artista/fecha)
 *                         [2] elegir localidad + cantidad  ->  [3] carrito  ->  Cobrar.
 * Una venta puede mezclar varias localidades y hasta varios conciertos.
 */
public class PuntoVentaView extends HBox implements Recargable {

    /** Estados de concierto que permiten vender. */
    private static final Set<String> VENDIBLES = Set.of("programado", "activo");

    /** Todo lo que necesita la pantalla, pedido junto en una sola tarea de fondo. */
    private record Datos(List<artista> artistas, List<concierto> conciertos, List<localidad> localidades,
                         List<inventarios_boletos> inventarios, List<cliente> clientes) {}

    private final Map<Integer, String> nombreArtista = new HashMap<>();
    private final Map<Integer, String> nombreLocalidad = new HashMap<>();
    private final Map<Integer, String> asientoLocalidad = new HashMap<>();
    private final Set<Integer> asientosLibres = new java.util.HashSet<>();
    private List<inventarios_boletos> todoInventario = List.of();

    // [1] Conciertos
    private final ObservableList<concierto> todos = FXCollections.observableArrayList();
    private final FilteredList<concierto> filtrados = new FilteredList<>(todos);
    private final ComboBox<artista> cbArtista = new ComboBox<>();
    private final DatePicker dpFecha = new DatePicker();
    private final TableView<concierto> tblConciertos = new TableView<>();

    // [2] Estadio con los asientos del concierto seleccionado
    private final EstadioMapa mapa = new EstadioMapa();
    private final Label lblEvento = new Label("Selecciona un concierto");
    private concierto conciertoActual;

    // [3] Carrito
    record Linea(concierto concierto, inventarios_boletos inv, String localidad, BigDecimal precio, int cantidad) {
        BigDecimal subtotal() { return precio.multiply(BigDecimal.valueOf(cantidad)); }
        BigDecimal base() { return Ui.dec(inv.getPrecio()).setScale(2,java.math.RoundingMode.HALF_UP); }
        BigDecimal cargo() { return precio.subtract(base()); }
    }
    private final ObservableList<Linea> carrito = FXCollections.observableArrayList();
    private final TableView<Linea> tblCarrito = new TableView<>();
    private final ObservableList<cliente> clientesTodos = FXCollections.observableArrayList();
    private final FilteredList<cliente> clientesFiltrados = new FilteredList<>(clientesTodos);
    private final ComboBox<cliente> cbCliente = new ComboBox<>();
    private final TextField buscarCliente = new TextField();
    private final javafx.scene.control.ListView<cliente> sugerenciasClientes = new javafx.scene.control.ListView<>();
    private final SimpleBooleanProperty clienteValido = new SimpleBooleanProperty(false);
    private final Label avisoCliente = new Label();
    private final Label lblTotal = new Label();
    private final Label lblDesglose = new Label();
    private final Label detalleBoleto = new Label("Selecciona una entrada del carrito para ver su desglose.");
    private final SimpleBooleanProperty cobrando = new SimpleBooleanProperty(false);

    public PuntoVentaView() {
        setSpacing(14);
        setPadding(new Insets(14));

        VBox izq = panelConciertos();
        VBox centro = panelLocalidades();
        VBox der = panelCarrito();
        izq.setPrefWidth(360);
        izq.setMinWidth(280);
        centro.setPrefWidth(290);
        centro.setMinWidth(245);
        der.setPrefWidth(340);
        der.setMinWidth(300);
        for (VBox p : List.of(izq, centro, der)) HBox.setHgrow(p, Priority.ALWAYS);
        getChildren().addAll(izq, centro, der);

        actualizarTotal();
    }

    // ------------------------------------------------------------------ paneles

    private VBox panelConciertos() {
        Label titulo = new Label("1. Concierto");
        titulo.getStyleClass().add("seccion");

        cbArtista.setPromptText("Todos los artistas");
        cbArtista.setConverter(Ui.convertidor(artista::getNombre_artistico));
        cbArtista.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cbArtista, Priority.ALWAYS);
        dpFecha.setPromptText("Fecha");
        Button limpiar = new Button("✕");
        limpiar.setOnAction(e -> { cbArtista.setValue(null); dpFecha.setValue(null); });
        cbArtista.valueProperty().addListener((o, a, b) -> aplicarFiltros());
        dpFecha.valueProperty().addListener((o, a, b) -> aplicarFiltros());
        HBox filtros = new HBox(8, cbArtista, dpFecha, limpiar);

        Ui.columnas(tblConciertos,
                Ui.col("Evento", concierto::getTitulo_evento, 150),
                Ui.col("Artista", c -> nombreArtista.get(c.getId_artista()), 110),
                Ui.col("Fecha", c -> Ui.fecha(c.getFecha_concierto())
                        + (c.getHorario() == null || c.getHorario().isBlank() ? "" : " " + c.getHorario()), 110),
                Ui.col("Recinto", concierto::getRecinto, 100));
        SortedList<concierto> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tblConciertos.comparatorProperty());
        tblConciertos.setItems(ordenados);
        tblConciertos.getSelectionModel().selectedItemProperty().addListener((o, a, c) -> mostrarInventario(c));
        VBox.setVgrow(tblConciertos, Priority.ALWAYS);

        VBox p = new VBox(10, titulo, filtros, tblConciertos);
        p.getStyleClass().add("panel");
        return p;
    }

    private VBox panelLocalidades() {
        Label titulo = new Label("2. Asientos");
        titulo.getStyleClass().add("seccion");
        lblEvento.setWrapText(true);
        lblEvento.setMinHeight(Region.USE_PREF_SIZE);   // que se vean sus dos líneas aunque la ventana sea baja

        mapa.setAlHacerClic(this::alternarAsiento);

        Label tarifas=new Label("Precios base + recargo: VIP 45% · Platea 25% · General 0%");
        tarifas.setWrapText(true);tarifas.setMinHeight(Region.USE_PREF_SIZE);
        VBox p = new VBox(10, titulo, tarifas, lblEvento, mapa);
        VBox.setVgrow(mapa, Priority.ALWAYS);
        p.getStyleClass().add("panel");
        return p;
    }

    private VBox panelCarrito() {
        Label titulo = new Label("3. Carrito");
        titulo.getStyleClass().add("seccion");

        Ui.columnas(tblCarrito,
                Ui.col("Evento", l -> l.concierto().getTitulo_evento(), 120),
                Ui.col("Localidad", Linea::localidad, 90),
                Ui.col("Cant.", l -> String.valueOf(l.cantidad()), 50),
                Ui.col("Subtotal", l -> Ui.dinero(l.subtotal()), 90));
        tblCarrito.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblCarrito.getColumns().get(0).setMinWidth(64);
        tblCarrito.getColumns().get(1).setMinWidth(64);
        tblCarrito.getColumns().get(2).setMinWidth(46);
        tblCarrito.getColumns().get(3).setMinWidth(92);
        tblCarrito.setItems(carrito);
        detalleBoleto.setWrapText(true);detalleBoleto.setMinHeight(Region.USE_PREF_SIZE);
        tblCarrito.getSelectionModel().selectedItemProperty().addListener((o,a,l) -> detalleBoleto.setText(l==null
            ? "Selecciona una entrada para ver su desglose."
            : l.localidad()+": base "+Ui.dinero(l.base())+" + recargo "+Ui.dinero(l.cargo())+" = "+Ui.dinero(l.precio())));
        VBox.setVgrow(tblCarrito, Priority.ALWAYS);
        carrito.addListener((ListChangeListener<Linea>) c -> {actualizarTotal(); dibujarMapa();if(tblCarrito.getSelectionModel().getSelectedItem()==null && !carrito.isEmpty())tblCarrito.getSelectionModel().selectFirst();});

        Button quitar = new Button("Quitar");
        quitar.disableProperty().bind(tblCarrito.getSelectionModel().selectedItemProperty().isNull());
        quitar.setOnAction(e -> carrito.remove(tblCarrito.getSelectionModel().getSelectedItem()));
        Button vaciar = new Button("Vaciar");
        vaciar.disableProperty().bind(Bindings.isEmpty(carrito));
        vaciar.setOnAction(e -> carrito.clear());
        HBox botonesCarrito = new HBox(8, quitar, vaciar);

        cbCliente.setItems(clientesFiltrados);
        buscarCliente.setPromptText("Buscar cliente por nombre o NIT");
        sugerenciasClientes.setItems(clientesFiltrados);
        sugerenciasClientes.setFixedCellSize(32);
        sugerenciasClientes.setCellFactory(lista -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(cliente c,boolean vacia) {
                super.updateItem(c,vacia);setText(vacia || c==null?null:com.umg.api.service.BusquedaClientes.etiqueta(c));
            }
        });
        sugerenciasClientes.setVisible(false);sugerenciasClientes.setManaged(false);
        sugerenciasClientes.setOnMouseClicked(e -> elegirSugerencia());
        sugerenciasClientes.setOnKeyPressed(e -> {
            if(e.getCode()==javafx.scene.input.KeyCode.ENTER){elegirSugerencia();e.consume();}
            if(e.getCode()==javafx.scene.input.KeyCode.ESCAPE){ocultarSugerencias();buscarCliente.requestFocus();e.consume();}
        });
        buscarCliente.textProperty().addListener((o,a,q) -> {
            cbCliente.setValue(null);
            clientesFiltrados.setPredicate(c -> com.umg.api.service.BusquedaClientes.coincide(c,q));
            actualizarAvisoCliente();mostrarSugerencias();
        });
        buscarCliente.setOnKeyPressed(e -> {
            if(e.getCode()==javafx.scene.input.KeyCode.DOWN && sugerenciasClientes.isVisible()) {
                sugerenciasClientes.requestFocus();sugerenciasClientes.getSelectionModel().selectFirst();e.consume();
            } else if(e.getCode()==javafx.scene.input.KeyCode.ENTER && sugerenciasClientes.isVisible()) {
                if(sugerenciasClientes.getSelectionModel().getSelectedItem()==null)sugerenciasClientes.getSelectionModel().selectFirst();
                elegirSugerencia();e.consume();
            } else if(e.getCode()==javafx.scene.input.KeyCode.ESCAPE){ocultarSugerencias();e.consume();}
        });
        cbCliente.valueProperty().addListener((o,a,c) -> {actualizarAvisoCliente();if(c!=null)ocultarSugerencias();});
        avisoCliente.setWrapText(true);avisoCliente.setMinHeight(Region.USE_PREF_SIZE);
        cbCliente.setPromptText("Selecciona un cliente");
        cbCliente.setConverter(Ui.convertidor(com.umg.api.service.BusquedaClientes::etiqueta));
        cbCliente.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cbCliente, Priority.ALWAYS);
        Button sinCliente = new Button("✕");
        sinCliente.setOnAction(e -> {buscarCliente.clear();cbCliente.setValue(null);ocultarSugerencias();});
        Label etiquetaCliente = new Label("Cliente:");
        etiquetaCliente.setMinWidth(Region.USE_PREF_SIZE);
        cbCliente.setMinWidth(0);
        HBox filaCliente = new HBox(8, etiquetaCliente, cbCliente, sinCliente);
        filaCliente.setAlignment(Pos.CENTER_LEFT);

        lblTotal.getStyleClass().add("total");
        Region esp = new Region();
        HBox.setHgrow(esp, Priority.ALWAYS);
        Button cobrar = new Button("Cobrar");
        cobrar.getStyleClass().add("boton-primario");
        cobrar.disableProperty().bind(Bindings.isEmpty(carrito).or(cobrando).or(clienteValido.not()));
        cobrar.setOnAction(e -> cobrar());
        HBox pie = new HBox(10, new Label("Total:"), lblTotal, esp, cobrar);
        pie.setAlignment(Pos.CENTER_LEFT);

        lblDesglose.setWrapText(true);
        VBox p = new VBox(10, titulo, tblCarrito, detalleBoleto, botonesCarrito, buscarCliente, sugerenciasClientes, filaCliente, avisoCliente, lblDesglose, pie);
        p.getStyleClass().add("panel");
        return p;
    }

    // ------------------------------------------------------------------ datos

    @Override
    public void recargar() {
        Integer idSeleccionado = conciertoActual == null ? null : conciertoActual.getId_concierto();
        Integer idArtistaFiltro = cbArtista.getValue() == null ? null : cbArtista.getValue().getId_artista();
        Integer idCliente = cbCliente.getValue() == null ? null : cbCliente.getValue().getId();

        Async.run(() -> new Datos(
                new artistaservice().getArtista(),
                new conciertoservice().getConcierto(),
                new localidadservice().getLocalidad(),
                new inventarios_boletosservice().getInventarios_boletos(),
                new clienteservice().getCliente()), d -> {

            nombreArtista.clear();
            d.artistas().forEach(a -> nombreArtista.put(a.getId_artista(), a.getNombre_artistico()));
            nombreLocalidad.clear();
            d.localidades().forEach(l -> nombreLocalidad.put(l.getId_localidad(), l.getNombre()));
            asientoLocalidad.clear();
            asientosLibres.clear();
            for (localidad l : d.localidades()) {
                asientoLocalidad.put(l.getId_localidad(), l.getAsiento());
                if (l.isEstado()) asientosLibres.add(l.getId_localidad());   // estado true = asiento libre
            }
            todoInventario = d.inventarios();

            cbArtista.getItems().setAll(d.artistas());
            cbArtista.getItems().stream().filter(a -> Objects.equals(a.getId_artista(), idArtistaFiltro))
                    .findFirst().ifPresent(cbArtista::setValue);
            clientesTodos.setAll(d.clientes());
            actualizarAvisoCliente();mostrarSugerencias();
            cbCliente.getItems().stream().filter(c -> Objects.equals(c.getId(), idCliente))
                    .findFirst().ifPresent(cbCliente::setValue);

            todos.setAll(d.conciertos().stream()
                    .filter(c -> c.getEstado() != null && VENDIBLES.contains(c.getEstado().trim().toLowerCase()))
                    .toList());
            tblConciertos.refresh();
            dibujarMapa();

            concierto reseleccion = todos.stream()
                    .filter(c -> idSeleccionado != null && c.getId_concierto() == idSeleccionado)
                    .findFirst().orElse(null);
            if (reseleccion != null) tblConciertos.getSelectionModel().select(reseleccion);
            else mostrarInventario(tblConciertos.getSelectionModel().getSelectedItem());
        });
    }

    private void aplicarFiltros() {
        artista a = cbArtista.getValue();
        LocalDate f = dpFecha.getValue();
        filtrados.setPredicate(c -> {
            if (a != null && c.getId_artista() != a.getId_artista()) return false;
            if (f != null && !f.equals(Ui.parseFecha(c.getFecha_concierto()))) return false;
            return true;
        });
    }

    private void mostrarInventario(concierto c) {
        conciertoActual = c;
        if (c == null) {
            lblEvento.setText("Selecciona un concierto");
        } else {
            lblEvento.setText(c.getTitulo_evento() + "\n" + Ui.fecha(c.getFecha_concierto())
                    + (c.getHorario() == null ? "" : " " + c.getHorario()) + " · " + c.getRecinto());
        }
        dibujarMapa();
    }

    // Pasa al mapa los asientos del concierto seleccionado: cada fila de inventario es un asiento
    private void dibujarMapa() {
        if (conciertoActual == null) {
            mapa.mostrar(List.of());
            return;
        }
        mapa.mostrar(todoInventario.stream()
                .filter(i -> i.getId_concierto() == conciertoActual.getId_concierto())
                .map(i -> new EstadioMapa.Asiento(
                        i.getId_inventario(),
                        asientoLocalidad.get(i.getId_localidad()),
                        nombreLocalidad.get(i.getId_localidad()),
                        i.getCantidad_disponible() > 0 && asientosLibres.contains(i.getId_localidad()),
                        enCarrito(i.getId_inventario()) > 0,
                        Ui.dinero(precioFinal(i))))
                .toList());
    }

    // ------------------------------------------------------------------ carrito

    private int enCarrito(int idInventario) {
        return carrito.stream().filter(l -> l.inv().getId_inventario() == idInventario)
                .mapToInt(Linea::cantidad).sum();
    }

    // La API cobra un recargo según la localidad: VIP +45 % y "Plata" +25 %
    private BigDecimal precioFinal(inventarios_boletos inv) {
        BigDecimal precio = Ui.dec(inv.getPrecio()).setScale(2,java.math.RoundingMode.HALF_UP);
        return precio.add(com.umg.api.service.Tarifas.cargo(precio,
                com.umg.api.service.Tarifas.porcentaje(nombreLocalidad.get(inv.getId_localidad()))));
    }

    // Clic en un asiento del mapa: si ya está en el carrito se quita, y si no, se agrega
    private void alternarAsiento(int idInventario) {
        if (conciertoActual == null) return;

        for (Linea l : carrito) {
            if (l.inv().getId_inventario() == idInventario) {
                carrito.remove(l);
                return;
            }
        }

        inventarios_boletos inv = todoInventario.stream()
                .filter(i -> i.getId_inventario() == idInventario).findFirst().orElse(null);
        if (inv == null) return;
        String loc = nombreLocalidad.getOrDefault(inv.getId_localidad(), "Localidad " + inv.getId_localidad())
                + " " + asientoLocalidad.getOrDefault(inv.getId_localidad(), "");
        // cada asiento se vende una sola vez
        carrito.add(new Linea(conciertoActual, inv, loc.trim(), precioFinal(inv), 1));
    }

    private BigDecimal total() {
        return carrito.stream().map(Linea::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void ocultarSugerencias(){sugerenciasClientes.setVisible(false);sugerenciasClientes.setManaged(false);}
    private void mostrarSugerencias() {
        boolean mostrar=!buscarCliente.getText().isBlank() && !clientesFiltrados.isEmpty() && cbCliente.getValue()==null;
        sugerenciasClientes.setPrefHeight(Math.min(4,clientesFiltrados.size())*32+2);
        sugerenciasClientes.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        sugerenciasClientes.setVisible(mostrar);sugerenciasClientes.setManaged(mostrar);
    }
    private void elegirSugerencia() {
        cliente elegida=sugerenciasClientes.getSelectionModel().getSelectedItem();
        if(elegida==null)return;
        cbCliente.setValue(elegida);ocultarSugerencias();buscarCliente.requestFocus();
    }
    private void actualizarAvisoCliente() {
        cliente c=cbCliente.getValue();
        clienteValido.set(c!=null && com.umg.api.validacion.ValidacionCliente.errores(c).isEmpty());
        avisoCliente.setText(clientesFiltrados.isEmpty() ? "No se encontró cliente con ese nombre o NIT."
                : c!=null && !clienteValido.get() ? "Este cliente tiene datos pendientes de corregir en Clientes."
                : com.umg.api.service.BusquedaClientes.avisoNit(c));
    }

    private void actualizarTotal() {
        BigDecimal bases=carrito.stream().map(l->l.base().multiply(BigDecimal.valueOf(l.cantidad()))).reduce(BigDecimal.ZERO,BigDecimal::add);
        lblDesglose.setText("Precio base: "+Ui.dinero(bases)+" · Recargos: "+Ui.dinero(total().subtract(bases)));
        lblTotal.setText(Ui.dinero(total()));
    }

    record VentaConCorreo(ventas venta, String correo) {}

    private void cobrar() {
        if (carrito.isEmpty()) return;
        if (cbCliente.getValue() == null) {
            Ui.error("Selecciona el cliente de la venta.");
            return;
        }
        if(!clienteValido.get())return; // La corrección y sus mensajes por campo están en Clientes.
        if(Sesion.token()==null && !com.umg.api.service.CorreoService.configurado()) {
            if(!com.umg.api.fx.util.ConfigurarGmail.mostrar(getScene().getWindow()))return;
        }
        int boletos = carrito.stream().mapToInt(Linea::cantidad).sum();
        if (!Ui.confirmar(boletos + " boleto(s) por " + Ui.dinero(total()) + "\n¿Confirmar la venta?")) return;

        int idVendedor = Sesion.actual().getId_usuario();
        Integer idCliente = cbCliente.getValue() == null ? null : cbCliente.getValue().getId();
        List<checkoutservice.Item> items = carrito.stream()
                .map(l -> new checkoutservice.Item(l.inv().getId_inventario(), l.cantidad())).toList();

        cobrando.set(true);   // evita doble clic = doble venta
        Integer vendedorPermitido = Sesion.esAdmin() ? null : idVendedor;
        Async.run(() -> {
                    ventas venta = new checkoutservice().registrarVenta(idVendedor, idCliente, items);
                    String correo;
                    try {
                        correo = new com.umg.api.service.ComprobanteCorreoService().enviar(venta.getId_venta(), vendedorPermitido);
                    } catch (Exception ex) {
                        correo = "Correo sin confirmar: " + ex.getMessage()
                                + "\nRevisa Comprobantes y boletos. No repitas la compra.";
                    }
                    return new VentaConCorreo(venta, correo);
                },
                (VentaConCorreo resultado) -> {
                    ventas v = resultado.venta();
                    com.umg.api.fx.util.UltimaVenta.recordar(v.getId_venta());
                    cobrando.set(false);
                    carrito.clear();
                    Ui.info("Venta #" + v.getId_venta() + " registrada.\nTotal cobrado: " + Ui.dinero(v.getTotal_venta()) + "\n" + resultado.correo());
                    recargar();
                },
                msg -> {
                    cobrando.set(false);
                    Ui.error(msg);
                    recargar();   // refresca disponibilidad por si otro vendedor agotó boletos
                });
    }
}
