package com.umg.api.service;

import jakarta.activation.DataHandler;
import jakarta.mail.Authenticator;
import jakarta.mail.internet.AddressException;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Envía correos con Gmail directamente desde el programa (SMTP), sin pasar por la API.
 *
 * Gmail no acepta la contraseña normal de la cuenta: hay que crear una "contraseña de aplicación"
 * (ver docs/CORREO_GMAIL.md). Se guardan fuera del ZIP, en APPDATA/SVB-GUA en Windows.
 * Se admite el archivo antiguo junto al proyecto y las variables GMAIL_USUARIO/GMAIL_CLAVE:
 *
 *     usuario=micuenta@gmail.com
 *     clave=abcdefghijklmnop
 */
public class CorreoService {

    private static final String ARCHIVO = "correo.properties";

    private final String usuario;
    private final String clave;
    private final String host;
    private final int puerto;

    /** Lee la configuración (correo.properties o variables de entorno). */
    public CorreoService() throws Exception {
        Properties p = leerConfiguracion();
        this.usuario = valor(p, "usuario", "GMAIL_USUARIO");
        this.clave = quitarEspacios(valor(p, "clave", "GMAIL_CLAVE"));
        this.host = p.getProperty("host", "smtp.gmail.com").trim();
        this.puerto = Integer.parseInt(p.getProperty("puerto", "587").trim());
        if (usuario == null || clave.isEmpty()) {
            throw new Exception("Configura la cuenta remitente en Comprobantes y QR > Configurar Gmail.");
        }
        com.umg.api.validacion.Validaciones.correo(usuario);
        if(!clave.matches("[a-zA-Z]{16}"))throw new Exception("Gmail requiere su contraseña de aplicación de 16 letras. Corrígela en Configurar Gmail.");
    }

    /** Para pruebas o para usar otro servidor SMTP con los mismos datos. */
    public CorreoService(String usuario, String clave, String host, int puerto) {
        this.usuario = usuario;
        this.clave = quitarEspacios(clave);
        this.host = host;
        this.puerto = puerto;
    }

    /** true si ya existe la configuración del correo (para no intentar enviar si no se ha configurado). */
    public static boolean configurado() {
        try {new CorreoService();return true;}catch(Exception e){return false;}
    }

    // ------------------------------------------------------------------ envío

    /**
     * Envía un correo de texto con un archivo adjunto (puede ser null).
     *
     * @param para           correo del destinatario
     * @param adjunto        archivo a adjuntar (por ejemplo, el PDF del comprobante)
     * @param nombreAdjunto  nombre con el que le llega al cliente, por ejemplo "comprobante-venta-5.pdf"
     */
    public void enviar(String para, String asunto, String texto, Path adjunto, String nombreAdjunto) throws Exception {
        Session sesion=crearSesion();

        try {
            MimeMessage mensaje = new MimeMessage(sesion);
            mensaje.setFrom(new InternetAddress(usuario, "Boletería SVB-GUA", "UTF-8"));
            mensaje.setRecipient(Message.RecipientType.TO, new InternetAddress(para, true));
            mensaje.setSubject(asunto, "UTF-8");

            MimeMultipart partes = new MimeMultipart();

            MimeBodyPart cuerpo = new MimeBodyPart();
            cuerpo.setText(texto, "UTF-8");
            partes.addBodyPart(cuerpo);

            if (adjunto != null) {
                MimeBodyPart archivo = new MimeBodyPart();
                archivo.setDataHandler(new DataHandler(new ByteArrayDataSource(Files.readAllBytes(adjunto), "application/pdf")));
                archivo.setFileName(nombreAdjunto);
                partes.addBodyPart(archivo);
            }

            mensaje.setContent(partes);
            Transport.send(mensaje);

        } catch (AuthenticationFailedException e) {
            throw new Exception("Gmail rechazó el usuario o la contraseña de aplicación. Revisa " + ARCHIVO
                    + " (debe ser una contraseña de aplicación, no la contraseña normal de la cuenta).");
        } catch (AddressException e) {
            throw new Exception("El correo del destinatario no es válido: " + para);
        } catch (SendFailedException e) {
            throw new Exception("No se pudo enviar el correo a " + para + ": " + e.getMessage());
        } catch (MessagingException e) {
            throw new Exception("No se pudo enviar el correo. Revisa tu conexión a internet. (" + e.getMessage() + ")");
        } catch (IOException e) {
            throw new Exception("No se pudo leer el archivo a adjuntar: " + e.getMessage());
        }
    }

    private Session crearSesion() {
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(puerto));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");      // Gmail en el puerto 587 usa STARTTLS
        props.put("mail.smtp.starttls.required", "true");    // nunca mandar la clave sin cifrar
        props.put("mail.smtp.ssl.checkserveridentity", "true");
        props.put("mail.smtp.connectiontimeout", "20000");   // si no hay internet, que falle en 20 s y no se quede colgado
        props.put("mail.smtp.timeout", "30000");
        props.put("mail.smtp.writetimeout", "30000");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(usuario, clave);
            }
        });
    }
    /** Comprueba red, TLS y autenticación, sin enviar correos ni crear ventas. */
    public void comprobarConexion() throws Exception {
        try(Transport transporte=crearSesion().getTransport("smtp")) {
            transporte.connect(host,puerto,usuario,clave);
        } catch(AuthenticationFailedException e) {
            throw new Exception("Gmail rechazó la cuenta o la contraseña de aplicación. Revisa Configurar Gmail.");
        } catch(MessagingException e) {
            throw new Exception("No se pudo conectar con Gmail. Revisa internet y que tu red permita SMTP (puerto 587).");
        }
    }

    public static Path archivoConfiguracion() {
        String personalizada=System.getProperty("svb.correo.config");
        if(personalizada!=null && !personalizada.isBlank())return Path.of(personalizada).toAbsolutePath();
        String appdata=System.getenv("APPDATA");
        Path directorio=appdata!=null && !appdata.isBlank()?Path.of(appdata,"SVB-GUA")
            :Path.of(System.getProperty("user.home"),".config","SVB-GUA");
        return directorio.resolve(ARCHIVO).toAbsolutePath();
    }
    public static String remitenteConfigurado() {
        try{return valor(leerConfiguracion(),"usuario","GMAIL_USUARIO");}catch(Exception e){return null;}
    }
    public static void configurar(String remitente,String claveAplicacion) throws Exception {
        remitente=com.umg.api.validacion.Validaciones.correo(remitente);
        String clave=quitarEspacios(claveAplicacion);
        if(!clave.matches("[a-zA-Z]{16}"))throw new IllegalArgumentException("Usa la contraseña de aplicación de Gmail de 16 letras, no la contraseña normal.");
        Properties p=new Properties();p.setProperty("usuario",remitente);p.setProperty("clave",clave);
        p.setProperty("host","smtp.gmail.com");p.setProperty("puerto","587");
        Path archivo=archivoConfiguracion();Files.createDirectories(archivo.getParent());Path temporal=Files.createTempFile(archivo.getParent(),".correo-",".tmp");
        try {
            try(var out=Files.newBufferedWriter(temporal,StandardCharsets.UTF_8)){p.store(out,"Configuración local privada; no compartir ni subir a GitHub");}
            Files.move(temporal,archivo,java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(temporal);}
    }

    // ------------------------------------------------------------------ configuración

    private static Properties leerConfiguracion() throws IOException {
        Properties p = new Properties();
        Path archivo=archivoConfiguracion();
        // Compatibilidad con la configuración antigua junto al proyecto, sin buscar otras carpetas.
        String personalizada=System.getProperty("svb.correo.config");
        if(!Files.exists(archivo) && (personalizada==null || personalizada.isBlank()))archivo=Path.of(ARCHIVO);
        if (Files.exists(archivo)) {
            try (var in = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
                p.load(in);
            }
        }
        return p;
    }

    // el valor del archivo; si no está, el de la variable de entorno
    private static String valor(Properties p, String clave, String variableDeEntorno) {
        String v = p.getProperty(clave);
        if (v == null || v.isBlank()) v = System.getenv(variableDeEntorno);
        if(v==null || v.isBlank())return null;
        v=v.trim();
        if(v.equals("micuenta@gmail.com") || v.equals("abcdefghijklmnop") || v.equals("abcd efgh ijkl mnop"))return null;
        return v;
    }

    // Google muestra la contraseña de aplicación en grupos de 4 letras separados por espacios
    private static String quitarEspacios(String s) {
        return s == null ? "" : s.replace(" ", "");
    }
}
