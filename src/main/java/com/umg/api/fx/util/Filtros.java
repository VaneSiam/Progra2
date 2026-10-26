package com.umg.api.fx.util;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

public final class Filtros {
    private Filtros() {}
    private static void aplicar(TextField campo,String patron,int max) {
        campo.setTextFormatter(new TextFormatter<String>(c -> {
            String nuevo=c.getControlNewText();
            // Permite cargar valores heredados para poder corregirlos; las ediciones y el pegado se validan.
            if (!campo.isFocused()) return c;
            return nuevo.length()<=max && nuevo.matches(patron) ? c : null;
        }));
    }
    public static void nombre(TextField campo,int max) { aplicar(campo,"[\\p{L}\\p{M} .’'&/\\-]*",max); }
    public static void telefono(TextField campo) { aplicar(campo,"[0-9]*",15); }
    public static void correo(TextField campo) { aplicar(campo,"[^\\s\\p{Cntrl}]*",254); }
    public static void precio(TextField campo) { aplicar(campo,"[0-9]{0,8}([.,][0-9]{0,2})?",11); }
}
