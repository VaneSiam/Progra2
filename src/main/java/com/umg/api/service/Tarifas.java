package com.umg.api.service;
import java.math.BigDecimal;
import java.math.RoundingMode;
/** Estimación de taquilla. El comprobante utiliza el desglose guardado por la API. */
public final class Tarifas {
    private Tarifas() {}
    public static int porcentaje(String tipo) {return "VIP".equals(tipo==null?"":tipo.trim())?45:"Plata".equals(tipo==null?"":tipo.trim())?25:0;}
    public static BigDecimal cargo(BigDecimal base,int porcentaje) {return base.multiply(BigDecimal.valueOf(porcentaje)).movePointLeft(2).setScale(2,RoundingMode.HALF_UP);}
    public static String desglose(com.fasterxml.jackson.databind.JsonNode detalle) {
        if(!detalle.hasNonNull("precio_base") || !detalle.hasNonNull("recargo_porcentaje") || !detalle.hasNonNull("recargo_monto"))
            return "Desglose histórico no registrado. Precio final cobrado: Q"+detalle.path("precio_unitario").asText();
        BigDecimal base=new BigDecimal(detalle.get("precio_base").asText()),
                porcentaje=new BigDecimal(detalle.get("recargo_porcentaje").asText()),
                cargo=new BigDecimal(detalle.get("recargo_monto").asText()),
                finalCobrado=new BigDecimal(detalle.get("precio_unitario").asText());
        if(base.signum()<0 || porcentaje.signum()<0 || cargo.signum()<0 || base.add(cargo).compareTo(finalCobrado)!=0
                || base.multiply(porcentaje).movePointLeft(2).setScale(2,RoundingMode.HALF_UP).compareTo(cargo)!=0)
            throw new IllegalArgumentException("El desglose de cargos no coincide con el precio cobrado.");
        return "Base Q"+base.setScale(2)+" + recargo "+porcentaje.stripTrailingZeros().toPlainString()+"% Q"+cargo.setScale(2)+" = Q"+finalCobrado.setScale(2);
    }
}
