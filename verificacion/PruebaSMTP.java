import com.umg.api.service.CorreoService;
import java.nio.file.Path;
/** Solo SMTP local con STARTTLS y credenciales ficticias; nunca Gmail real. */
public class PruebaSMTP {
    public static void main(String[] args)throws Exception {
        int puerto=Integer.parseInt(args[0]);
        CorreoService smtp=new CorreoService("sender@example.invalid","aaaabbbbccccdddd","localhost",puerto);
        smtp.comprobarConexion(); // Autenticación correcta, cero mensajes enviados.
        smtp.enviar("cliente@example.invalid","Prueba local con PDF","Comprobante de prueba.",Path.of(args[1]),"boletos.pdf");
        try {new CorreoService("sender@example.invalid","xxxxxxxxxxxxxxxx","localhost",puerto).comprobarConexion();throw new AssertionError("Aceptó una clave incorrecta");}
        catch(Exception e){if(!e.getMessage().contains("Gmail rechazó"))throw e;}
        System.out.println("PASS SMTP local: STARTTLS, autenticación, conexión sin envío, PDF adjunto y rechazo de clave incorrecta.");
    }
}
