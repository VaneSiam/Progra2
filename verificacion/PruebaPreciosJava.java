import com.umg.api.service.localidadservice;
import java.math.BigDecimal;
public class PruebaPreciosJava {
 public static void main(String[] args)throws Exception {
  var s=new localidadservice();var vip=new BigDecimal("1000.00");var platea=new BigDecimal("500.00");var general=new BigDecimal("150.00");
  s.generarAsientos(1,vip,platea,general);
  if(s.actualizarPrecios(1,vip,platea,general)!=99)throw new AssertionError();
  try{s.generarAsientos(1,vip,platea,general);throw new AssertionError("Generó contra API sin soporte");}
  catch(Exception e){if(!e.getMessage().contains("No se crearon"))throw e;}
  System.out.println("PASS Java precios separados, actualización y bloqueo de API antigua sin escrituras.");
 }
}
