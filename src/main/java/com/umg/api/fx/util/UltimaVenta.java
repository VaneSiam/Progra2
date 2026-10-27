package com.umg.api.fx.util;

import com.umg.api.config.ApiConfig;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import java.util.prefs.Preferences;

/** Recordatorio local por usuario y servidor; nunca predice el siguiente folio. */
public final class UltimaVenta {
    private static final ReadOnlyIntegerWrapper numero = new ReadOnlyIntegerWrapper();
    private static String clave;
    private UltimaVenta() {}
    public static ReadOnlyIntegerProperty numeroProperty() { return numero.getReadOnlyProperty(); }
    public static void cargar(int usuario) {
        clave = Integer.toHexString(ApiConfig.url("").hashCode()) + "-" + usuario;
        int guardado = 0;
        try { guardado = Preferences.userNodeForPackage(UltimaVenta.class).getInt(clave, 0); }
        catch (SecurityException ignored) { }
        numero.set(guardado);
    }
    public static void recordar(int id) {
        if (id <= 0) return;
        numero.set(id);
        try { if (clave != null) Preferences.userNodeForPackage(UltimaVenta.class).putInt(clave, id); }
        catch (SecurityException ignored) { }
    }
}
