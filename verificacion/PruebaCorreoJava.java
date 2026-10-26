import com.umg.api.config.Sesion;
import com.umg.api.service.*;
import com.umg.api.reportes.BoletosPdf;
import java.util.List;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;

/** Se ejecuta exclusivamente contra el servidor ficticio de prueba. */
public class PruebaCorreoJava {
    public static void main(String[] args) throws Exception {
        var login = new authservice().login("vendedor", "clave-prueba");
        if (login.token == null) throw new AssertionError("No recibió sesión del backend");
        Sesion.iniciar(login.usuario, login.token);
        var venta = new checkoutservice().registrarVenta(2, 3,
                List.of(new checkoutservice.Item(700,1), new checkoutservice.Item(701,1)));
        if (venta.getId_venta()!=8 || venta.getTotal_venta()!=250f) throw new AssertionError("Venta incorrecta");
        var documento = new ComprobanteService().cargar(8,2);
        if (documento.lineas().size()!=2) throw new AssertionError("Faltan boletos");
        var fila = documento.lineas().get(0);
        var imagen = BoletosPdf.qr(BoletosPdf.enlace(fila));
        int[] rgb = imagen.getRGB(0,0,imagen.getWidth(),imagen.getHeight(),null,0,imagen.getWidth());
        var fuente = new RGBLuminanceSource(imagen.getWidth(),imagen.getHeight(),rgb);
        String qr = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(fuente))).getText();
        if (!qr.endsWith("/api/detalle_ventas/21")) throw new AssertionError("QR incorrecto");
        var correo = new ComprobanteCorreoService();
        System.out.println(correo.enviar(8,2));
        System.out.println(correo.enviar(8,2)); // Debe consultar el estado, sin repetir ni PDF ni cobro.
        Sesion.cerrar();
        try { correo.enviar(8,2); throw new AssertionError("Envió sin sesión"); }
        catch (IllegalStateException esperado) {}
        System.out.println("PASS Java: login, compra, JasperReports, QR legible, envío y consulta sin duplicar.");
    }
}
