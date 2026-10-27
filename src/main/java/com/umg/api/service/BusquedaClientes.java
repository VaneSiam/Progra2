package com.umg.api.service;
import com.umg.api.model.cliente;
import java.text.Normalizer;
import java.util.Locale;
public final class BusquedaClientes {
    private BusquedaClientes() {}
    private static String normal(String s) {return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).trim();}
    public static boolean coincide(cliente c,String busqueda) {
        String q=normal(busqueda).replace('\u00a0',' ');
        if(q.isEmpty())return true;
        String nombre=normal((c.getNombre()==null?"":c.getNombre())+" "+(c.getApellido()==null?"":c.getApellido()));
        String nit=normal(c.getNit()).replaceAll("[^a-z0-9]","");
        String consultaNit=q.replaceAll("[^a-z0-9]","");
        return nombre.contains(q) || (!consultaNit.isEmpty() && nit.contains(consultaNit));
    }
    public static String etiqueta(cliente c) {
        String nit=c.getNit()==null || c.getNit().isBlank()?"Sin NIT":"NIT "+c.getNit().trim();
        return nit+" · "+((c.getNombre()==null?"":c.getNombre())+" "+(c.getApellido()==null?"":c.getApellido())).trim();
    }
    public static String avisoNit(cliente c) {
        if(c==null)return "";
        return c.getNit()==null || c.getNit().isBlank() || "CF".equalsIgnoreCase(c.getNit().trim())
                ? "El cliente no contiene NIT registrado (consumidor final)." : "NIT: "+c.getNit();
    }
}
