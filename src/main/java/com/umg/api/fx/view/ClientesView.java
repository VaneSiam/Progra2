package com.umg.api.fx.view;

import com.umg.api.fx.util.Async;
import com.umg.api.fx.util.Ui;
import com.umg.api.fx.util.Filtros;
import com.umg.api.validacion.Validaciones;
import com.umg.api.model.cliente;
import com.umg.api.service.clienteservice;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** CRUD de clientes (NIT, contacto). Lo usan vendedores y administradores. */
public class ClientesView extends HBox implements Recargable {
    private final TableView<cliente> tabla = new TableView<>();
    private final TextField txtNombre = new TextField();
    private final TextField txtApellido = new TextField();
    private final TextField txtNit = new TextField();
    private final TextField txtTelefono = new TextField();
    private final TextField txtCorreo = new TextField();
    private final TextField txtDireccion = new TextField();
    private final CheckBox chkConsumidorFinal = new CheckBox("Consumidor final (sin NIT)");
    private final CheckBox chkActivo = new CheckBox("Activo");
    private cliente seleccionado;
    private final java.util.Map<String,Label> erroresCampo=new java.util.LinkedHashMap<>();
    private final javafx.beans.property.SimpleBooleanProperty formularioValido=new javafx.beans.property.SimpleBooleanProperty(false);
    private final javafx.beans.property.SimpleBooleanProperty guardando=new javafx.beans.property.SimpleBooleanProperty(false);
    private boolean mostrarTodosLosErrores;
    private final Button guardar = new Button("Guardar");

    public ClientesView() {
        Filtros.nombre(txtNombre,100); Filtros.nombre(txtApellido,100);
        Filtros.telefono(txtTelefono); Filtros.nit(txtNit); Filtros.correo(txtCorreo);
        chkConsumidorFinal.selectedProperty().addListener((o,a,b)->{txtNit.setDisable(b);actualizarValidacion();});
        txtTelefono.setPromptText("1234-5678"); txtCorreo.setPromptText("nombre@dominio.com (obligatorio)");
        setSpacing(16);
        setPadding(new Insets(14));

        Ui.columnas(tabla,
                Ui.col("Nombre", cliente::getNombre, 120),
                Ui.col("Apellido", cliente::getApellido, 120),
                Ui.col("NIT", cliente::getNit, 90),
                Ui.col("Teléfono", cliente::getTelefono, 100),
                Ui.col("Correo", cliente::getCorreo, 170),
                Ui.col("Datos", c -> com.umg.api.validacion.ValidacionCliente.errores(c).isEmpty()?"Correctos":"Revisar en formulario",140),
                Ui.col("Estado", c -> c.isStatus() ? "Activo" : "Inactivo", 70));
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, s) -> {
            seleccionado = s;
            if (s != null) cargarForm(s);
        });
        VBox izq = new VBox(tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        izq.getStyleClass().add("panel");
        HBox.setHgrow(izq, Priority.ALWAYS);

        chkActivo.setSelected(true);
        Label titulo = new Label("Cliente");
        titulo.getStyleClass().add("seccion");
        Button nuevo = new Button("Nuevo");
        nuevo.setOnAction(e -> limpiar());
        guardar.getStyleClass().add("boton-primario");
        guardar.setOnAction(e -> guardar());
        guardar.disableProperty().bind(formularioValido.not().or(guardando));
        Button eliminar = new Button("Eliminar");
        eliminar.getStyleClass().add("boton-peligro");
        eliminar.setOnAction(e -> eliminar());
        Label reglas=new Label("Teléfono de Guatemala: 8 dígitos. Se permiten espacios o guiones.");reglas.setWrapText(true);
        var formulario=Ui.form("Nombre *", campoValidado("nombre",txtNombre), "Apellido *", campoValidado("apellido",txtApellido),
                        "NIT *", new VBox(5,campoValidado("nit",txtNit),chkConsumidorFinal), "Teléfono *", campoValidado("telefono",txtTelefono),
                        "Correo *", campoValidado("correo",txtCorreo), "Dirección", campoValidado("direccion",txtDireccion), "", chkActivo);
        var etiquetas=new javafx.scene.layout.ColumnConstraints(92);
        var campos=new javafx.scene.layout.ColumnConstraints();campos.setMinWidth(0);campos.setHgrow(Priority.ALWAYS);
        formulario.getColumnConstraints().setAll(etiquetas,campos);
        VBox der = new VBox(12,titulo,reglas,formulario,new HBox(8,nuevo,guardar,eliminar));
        der.getStyleClass().add("panel");
        der.setPrefWidth(360);
        der.setMinWidth(360);

        getChildren().addAll(izq, der);
        actualizarValidacion();
    }

    @Override
    public void recargar() {
        Async.run(() -> new clienteservice().getCliente(), tabla.getItems()::setAll);
    }

    private static String vacio(String s) { return s == null ? "" : s; }

    private void cargarForm(cliente c) {
        txtNombre.setText(vacio(c.getNombre()));
        txtApellido.setText(vacio(c.getApellido()));
        chkConsumidorFinal.setSelected("CF".equalsIgnoreCase(vacio(c.getNit()).trim()));
        txtNit.setText(chkConsumidorFinal.isSelected()?"":vacio(c.getNit()));
        txtTelefono.setText(vacio(c.getTelefono()));
        txtCorreo.setText(vacio(c.getCorreo()));
        txtDireccion.setText(vacio(c.getDireccion()));
        chkActivo.setSelected(c.isStatus());
        mostrarTodosLosErrores=true;actualizarValidacion();
    }

    private void limpiar() {
        tabla.getSelectionModel().clearSelection();
        seleccionado = null;
        mostrarTodosLosErrores=false;chkConsumidorFinal.setSelected(false);
        for (TextField t : new TextField[]{txtNombre, txtApellido, txtNit, txtTelefono, txtCorreo, txtDireccion}) t.clear();
        chkActivo.setSelected(true);actualizarValidacion();
    }

    private javafx.scene.Node campoValidado(String nombre,TextField campo) {
        Label error=new Label();error.setWrapText(true);error.getStyleClass().add("error");
        error.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        erroresCampo.put(nombre,error);
        campo.textProperty().addListener((o,a,b)->actualizarValidacion());
        campo.focusedProperty().addListener((o,a,b)->{if(!b)actualizarValidacion();});
        VBox contenedor=new VBox(3,campo,error);contenedor.setMinWidth(0);error.setMinWidth(0);return contenedor;
    }
    private cliente borrador() {
        return new cliente(seleccionado==null?0:seleccionado.getId(),txtNombre.getText().trim(),txtApellido.getText().trim(),
            txtDireccion.getText().trim(),txtCorreo.getText().trim(),txtTelefono.getText().trim(),chkConsumidorFinal.isSelected()?"CF":txtNit.getText().trim(),chkActivo.isSelected());
    }
    private void actualizarValidacion() {
        var errores=com.umg.api.validacion.ValidacionCliente.errores(borrador());
        formularioValido.set(errores.isEmpty());
        var campos=java.util.Map.of("nombre",txtNombre,"apellido",txtApellido,"nit",txtNit,"telefono",txtTelefono,"correo",txtCorreo,"direccion",txtDireccion);
        erroresCampo.forEach((nombre,label)->{
            TextField campo=campos.get(nombre);
            boolean visible=!nombre.equals("nombre") && !nombre.equals("apellido") && errores.containsKey(nombre) && (nombre.equals("nit") || mostrarTodosLosErrores || !campo.getText().isBlank() || campo.isFocused());
            label.setText(visible?(nombre.equals("nit")?"Obligatorio":errores.get(nombre)):"");label.setVisible(visible);label.setManaged(visible);
            campo.setStyle(visible?"-fx-border-color: #c62828;":"");
        });
    }

    private void guardar() {
        cliente sel = seleccionado;
        mostrarTodosLosErrores=true;actualizarValidacion();
        if(!formularioValido.get())return;
        final cliente c;
        try {
            c = new cliente(sel == null ? 0 : sel.getId(),
                Validaciones.nombre(txtNombre.getText(),"Nombre",100,true),
                Validaciones.nombre(txtApellido.getText(),"Apellido",100,true),
                Validaciones.texto(txtDireccion.getText(),"Dirección",255,false),
                Validaciones.correo(txtCorreo.getText()),Validaciones.telefono(txtTelefono.getText()),
                Validaciones.nit(chkConsumidorFinal.isSelected()?"CF":txtNit.getText()),chkActivo.isSelected());
        } catch (IllegalArgumentException ex) { Ui.error(ex.getMessage()); return; }
        guardando.set(true);
        Async.run(() -> sel == null
                ? new clienteservice().createCliente(c)
                : new clienteservice().updateCliente(sel.getId(), c),
            r -> {guardando.set(false); limpiar(); recargar();},
            msg -> {guardando.set(false);Ui.error(msg);});
    }

    private void eliminar() {
        cliente sel = seleccionado;
        if (sel == null) { Ui.error("Selecciona un cliente."); return; }
        if (!Ui.confirmar("¿Eliminar a \"" + sel.getNombre() + "\"?")) return;
        Async.run(() -> { new clienteservice().deleteCliente(sel.getId()); return true; },
                ok -> { limpiar(); recargar(); });
    }
}
