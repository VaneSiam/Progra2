import com.umg.api.model.cliente;
import com.umg.api.service.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
public class PruebaNuevosModulos {
    static void ok(boolean valor){if(!valor)throw new AssertionError();}
    public static void main(String[] args)throws Exception {
        cliente ana=new cliente();ana.setNombre("María José");ana.setApellido("Pérez");ana.setNit("1234567-K");
        ok(BusquedaClientes.coincide(ana,"maria jose"));ok(BusquedaClientes.coincide(ana,"1234567K"));ok(!BusquedaClientes.coincide(ana,"Luis"));
        ana.setNit("CF");ok(BusquedaClientes.avisoNit(ana).contains("no contiene NIT"));
        ana.setNit(null);ok(BusquedaClientes.avisoNit(ana).contains("no contiene NIT"));
        // Ejecutar solo en directorio temporal: son credenciales ficticias.
        Path archivo=CorreoService.archivoConfiguracion();if(Files.exists(archivo))throw new AssertionError("La prueba requiere directorio vacío");
        ok(!CorreoService.configurado());
        var u=new com.umg.api.model.usuario();u.setId_usuario(2);u.setRol("vendedor");
        com.umg.api.config.Sesion.iniciar(u,null);
        try {new ComprobanteCorreoService().enviar(1,2);throw new AssertionError();}
        catch(IllegalStateException esperado){ok(esperado.getMessage().contains("Configurar Gmail"));}
        com.umg.api.config.Sesion.cerrar();
        try {CorreoService.configurar("no-es-correo","aaaaaaaaaaaaaaaa");throw new AssertionError();}catch(IllegalArgumentException esperado){}
        CorreoService.configurar("sender@example.invalid","aaaa bbbb cccc dddd");ok(CorreoService.configurado());
        new CorreoService();Files.delete(archivo);ok(!CorreoService.configurado());
        AtomicInteger enviados=new AtomicInteger();
        RegistroCorreoLocal.ejecutar(1,()->enviados.incrementAndGet());RegistroCorreoLocal.ejecutar(1,()->enviados.incrementAndGet());ok(enviados.get()==1);
        try {RegistroCorreoLocal.ejecutar(2,()->{throw new Exception("timeout simulado");});throw new AssertionError();}catch(Exception esperado){}
        try {RegistroCorreoLocal.ejecutar(2,()->enviados.incrementAndGet());throw new AssertionError();}catch(IllegalStateException esperado){}
        ok(enviados.get()==1);
        System.out.println("PASS búsqueda nombre/NIT, avisos, configuración Gmail sin secretos reales, persistencia local y bloqueo de envío incierto.");
    }
}
