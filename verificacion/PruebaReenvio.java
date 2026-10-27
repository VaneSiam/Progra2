import com.umg.api.service.RegistroCorreoLocal;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

public class PruebaReenvio {
    public static void main(String[] args) throws Exception {
        System.setProperty("svb.correo.registro",Files.createTempDirectory("reenvio-prueba-").toString());
        var cantidad=new AtomicInteger();
        RegistroCorreoLocal.ejecutar(9101,()->cantidad.incrementAndGet());
        RegistroCorreoLocal.ejecutar(9101,()->cantidad.incrementAndGet());
        if(cantidad.get()!=1)throw new AssertionError("Duplicó envío automático");
        RegistroCorreoLocal.reenviar(9101,()->{
            cantidad.incrementAndGet();
            try {RegistroCorreoLocal.reenviar(9101,()->cantidad.incrementAndGet());throw new AssertionError("Permite concurrencia");}
            catch(IllegalStateException esperado){}
        });
        RegistroCorreoLocal.ejecutar(9101,()->cantidad.incrementAndGet());
        if(cantidad.get()!=2)throw new AssertionError("Debe reenviar solo una copia solicitada");
        try {RegistroCorreoLocal.reenviar(9102,()->{throw new Exception("Resultado desconocido");});}
        catch(Exception esperado){}
        try {RegistroCorreoLocal.reenviar(9102,()->cantidad.incrementAndGet());throw new AssertionError("Reenvió con estado incierto");}
        catch(IllegalStateException esperado){}
        if(cantidad.get()!=2)throw new AssertionError();
        System.out.println("PASS reenvío explícito, deduplicación automática, bloqueo concurrente e incierto. Sin correos reales.");
    }
}
