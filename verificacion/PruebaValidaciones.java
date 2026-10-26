import com.umg.api.validacion.Validaciones;
public class PruebaValidaciones {
    static void rechaza(Runnable r) {try{r.run();throw new AssertionError("Aceptó un valor inválido");}catch(IllegalArgumentException esperado){}}
    public static void main(String[] args) {
        for(String s:new String[]{"José María","Muñoz","AC/DC","R&B","D’Angelo"})Validaciones.nombre(s,"Nombre",120,true);
        for(String s:new String[]{"Artista123","123","---"," ","José\nLuis"})rechaza(()->Validaciones.nombre(s,"Nombre",120,true));
        for(String s:new String[]{"","a@b","a..b@dominio.com",".a@dominio.com","a.@dominio.com","a b@dominio.com","a@-dominio.com"})rechaza(()->Validaciones.correo(s));
        Validaciones.correo("persona+ventas@dominio.com");
        for(String s:new String[]{"1234567","1234-5678","1234567a","+50212345678","1234567890123456"})rechaza(()->Validaciones.telefono(s));
        Validaciones.telefono("50212345678");
        for(String s:new String[]{"NaN","Infinity","-1","0","1e3","12.345"})rechaza(()->Validaciones.precio(s));
        Validaciones.precio("12,50");Validaciones.nit("CF");Validaciones.nit("1234567-K");
        try {
            var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
            String desglose=com.umg.api.service.Tarifas.desglose(mapper.readTree("{\"precio_base\":1000,\"recargo_porcentaje\":45,\"recargo_monto\":450,\"precio_unitario\":1450}"));
            if(!desglose.contains("45% Q450.00"))throw new AssertionError(desglose);
            String antiguo=com.umg.api.service.Tarifas.desglose(mapper.readTree("{\"precio_unitario\":1450}"));
            if(!antiguo.contains("no registrado"))throw new AssertionError(antiguo);
            var roto=mapper.readTree("{\"precio_base\":1000,\"recargo_porcentaje\":45,\"recargo_monto\":400,\"precio_unitario\":1450}");
            rechaza(()->com.umg.api.service.Tarifas.desglose(roto));
        } catch (java.io.IOException ex) {throw new AssertionError(ex);}
        System.out.println("PASS: nombres, correo obligatorio, teléfono, precios y NIT.");
    }
}
