package com.umg.api.validacion;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/** Reglas compartidas por formularios y punto de venta, sin dependencias gráficas. */
public final class Validaciones {
    private Validaciones() {}
    public static String texto(String valor, String campo, int max, boolean obligatorio) {
        String s=valor==null?"":valor.trim();
        if ((obligatorio && s.isEmpty()) || s.length()>max || s.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException(campo+": "+(obligatorio?"es obligatorio y debe tener":"debe tener")+" hasta "+max+" caracteres válidos.");
        return s;
    }
    public static String nombre(String valor,String campo,int max,boolean obligatorio) {
        String s=texto(valor,campo,max,obligatorio);
        if(!s.isEmpty() && (!s.matches("[\\p{L}\\p{M} .’'&/\\-]+") || !Pattern.compile("\\p{L}").matcher(s).find()))
            throw new IllegalArgumentException(campo+": utiliza letras, sin números; se permiten tildes, espacios y puntuación del nombre.");
        return s;
    }
    public static String correo(String valor) {
        String s=texto(valor,"Correo electrónico",254,true);
        if(s.length()>254 || !s.matches("[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+")
                || s.substring(0,s.indexOf('@')).length()>64 || s.startsWith(".") || s.contains("..") || s.contains(".@"))
            throw new IllegalArgumentException("Correo electrónico obligatorio: escribe una dirección válida, por ejemplo nombre@dominio.com.");
        return s;
    }
    public static String telefono(String valor) {
        String s=texto(valor,"Teléfono",15,true);
        if(!s.matches("[0-9]{8,15}")) throw new IllegalArgumentException("Teléfono: escribe de 8 a 15 dígitos, sin espacios, letras ni signos.");
        return s;
    }
    public static String nit(String valor) {
        String s=texto(valor,"NIT",20,true).toUpperCase(java.util.Locale.ROOT);
        if(!s.matches("CF|[0-9]+-?[0-9K]")) throw new IllegalArgumentException("NIT: escribe CF o un número válido, con guion opcional y dígito final o K.");
        return s;
    }
    public static BigDecimal precio(String valor) {
        String s=texto(valor,"Precio",13,true).replace(',','.');
        if(!s.matches("[0-9]{1,8}(\\.[0-9]{1,2})?")) throw new IllegalArgumentException("Precio: utiliza hasta 8 enteros y 2 decimales, sin letras ni signos.");
        BigDecimal n=new BigDecimal(s);
        if(n.signum()<=0) throw new IllegalArgumentException("El precio debe ser mayor que cero.");
        return n;
    }
}
