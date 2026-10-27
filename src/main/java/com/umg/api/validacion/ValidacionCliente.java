package com.umg.api.validacion;
import com.umg.api.model.cliente;
import java.util.LinkedHashMap;
import java.util.Map;

/** Una sola regla para el editor, los servicios y la aptitud del cliente para vender. */
public final class ValidacionCliente {
    private ValidacionCliente() {}
    public static Map<String,String> errores(cliente c) {
        Map<String,String> errores=new LinkedHashMap<>();
        comprobar(errores,"nombre",()->Validaciones.nombre(c.getNombre(),"Nombre",100,true));
        comprobar(errores,"apellido",()->Validaciones.nombre(c.getApellido(),"Apellido",100,true));
        comprobar(errores,"nit",()->Validaciones.nit(c.getNit()));
        comprobar(errores,"telefono",()->Validaciones.telefono(c.getTelefono()));
        comprobar(errores,"correo",()->Validaciones.correo(c.getCorreo()));
        comprobar(errores,"direccion",()->Validaciones.texto(c.getDireccion(),"Dirección",255,false));
        return errores;
    }
    private static void comprobar(Map<String,String> errores,String campo,Runnable regla) {
        try{regla.run();}catch(IllegalArgumentException e){errores.put(campo,e.getMessage());}
    }
    public static void validar(cliente c) {
        var errores=errores(c);
        if(!errores.isEmpty())throw new IllegalArgumentException(String.join("\n",errores.values()));
    }
}
