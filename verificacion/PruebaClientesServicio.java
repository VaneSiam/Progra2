import com.umg.api.model.cliente;
import com.umg.api.service.clienteservice;
public class PruebaClientesServicio {
    public static void main(String[] args)throws Exception {
        cliente c=new cliente(1,"Ana","Ejemplo","Zona 1","cliente@example.invalid","1234567","1234567-K",true);
        var servicio=new clienteservice();
        try{servicio.createCliente(c);throw new AssertionError();}catch(IllegalArgumentException esperado){}
        try{servicio.updateCliente(1,c);throw new AssertionError();}catch(IllegalArgumentException esperado){}
        c.setTelefono("12345678");c.setCorreo("no-es-correo");
        try{servicio.createCliente(c);throw new AssertionError();}catch(IllegalArgumentException esperado){}
        c.setCorreo("cliente@example.invalid");
        servicio.createCliente(c);servicio.updateCliente(1,c);
        System.out.println("PASS servicio de clientes rechaza teléfono/correo inválidos antes de HTTP y permite datos válidos.");
    }
}
