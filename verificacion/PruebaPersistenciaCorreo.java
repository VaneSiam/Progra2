import com.umg.api.service.*;
public class PruebaPersistenciaCorreo {
    public static void main(String[] args)throws Exception {
        if(args[0].equals("guardar")) {
            CorreoService.configurar("sender@example.invalid","aaaabbbbccccdddd");
            RegistroCorreoLocal.ejecutar(991,()->{});
        } else {
            if(!CorreoService.configurado())throw new AssertionError("Perdió la configuración al cambiar carpeta");
            String r=RegistroCorreoLocal.ejecutar(991,()->{throw new AssertionError("Reenvió desde otra carpeta");});
            if(!r.contains("No se duplicó"))throw new AssertionError(r);
        }
        System.out.println("PASS persistencia de configuración y registro desde "+args[0]);
    }
}
