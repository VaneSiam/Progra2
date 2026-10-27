package com.umg.api.service;
import com.umg.api.model.cliente;
import java.text.Normalizer;
import java.util.Locale;
public final class BusquedaClientes {
    private BusquedaClientes() {}
    private static String normal(String s) {return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).trim();}
    public static boolean coincide(cliente c,String busqueda) {
        String q=normal(busqueda);
        return q.isEmpty() || normal(c.getNombre()+" "+(c.getApellido()==null?"":c.getApellido())).contains(q)
                || (!q.replace("-","").isEmpty() && normal(c.getNit()).replace("-","").contains(q.replace("-","")));
    }
    public static String avisoNit(cliente c) {
        if(c==null)return "";
        return c.getNit()==null || c.getNit().isBlank() || "CF".equalsIgnoreCase(c.getNit().trim())
                ? "El cliente no contiene NIT registrado (consumidor final)." : "NIT: "+c.getNit();
    }
}
