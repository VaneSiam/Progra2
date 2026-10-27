import com.umg.api.service.CorreoService;
import java.lang.reflect.Field;
/** Usa una clave ficticia con dígitos y espacios de portapapeles, sin conectar a Google. */
public class PruebaClavePegada {
    public static void main(String[] args)throws Exception {
        CorreoService.configurar("sender@example.invalid","a1b2\u200b c3d4\u00a0e5f6 g7h8\n");
        CorreoService correo=new CorreoService();
        Field clave=CorreoService.class.getDeclaredField("clave");clave.setAccessible(true);
        if(!"a1b2c3d4e5f6g7h8".equals(clave.get(correo)))throw new AssertionError("No normalizó el pegado");
        if(!CorreoService.configurado())throw new AssertionError("Rechazó la clave antes de consultar Gmail");
        System.out.println("PASS clave ficticia pegada: dígitos admitidos y espacios invisibles eliminados; sin conexión a Gmail.");
    }
}
