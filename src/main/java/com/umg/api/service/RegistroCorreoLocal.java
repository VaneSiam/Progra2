package com.umg.api.service;
import com.umg.api.config.ApiConfig;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Evita reenvíos accidentales desde esta instalación. El modo API lleva registro global. */
public final class RegistroCorreoLocal {
    private RegistroCorreoLocal() {}
    @FunctionalInterface public interface Envio {void ejecutar() throws Exception;}
    static Path ruta(int id) throws Exception {
        if(id<1)throw new IllegalArgumentException("Venta inválida.");
        String clave=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(ApiConfig.url("ventas/"+id).getBytes(StandardCharsets.UTF_8)));
        String personalizada=System.getProperty("svb.correo.registro");
        Path dir=personalizada!=null && !personalizada.isBlank()?Path.of(personalizada)
            :CorreoService.archivoConfiguracion().getParent().resolve("envios");
        Files.createDirectories(dir);
        Path destino=dir.resolve(clave+".estado");
        Path anterior=Path.of(".svb-correo",clave+".estado");
        if(!Files.exists(destino) && Files.isRegularFile(anterior)) {
            try{Files.copy(anterior,destino);}catch(FileAlreadyExistsException ignorado){}
        }
        return destino;
    }
    public static String ejecutar(int id,Envio envio) throws Exception {
        Path p=ruta(id);
        try(var canal=FileChannel.open(p.resolveSibling(p.getFileName()+".lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
            var lock=canal.tryLock()) {
            if(lock==null)throw new IllegalStateException("El envío de esta venta está en proceso.");
            String estado=Files.exists(p)?Files.readString(p):"pendiente";
            if(estado.equals("aceptado"))return "Este equipo ya registró el envío del comprobante. No se duplicó.";
            if(!estado.equals("pendiente"))throw new IllegalStateException("Envío en proceso o incierto. Revisa Gmail Enviados antes de volver a enviar.");
            Files.writeString(p,"enviando");
            try {envio.ejecutar();Files.writeString(p,"aceptado");return "Gmail aceptó el comprobante para su envío. Revisa la bandeja de entrada y spam.";}
            catch(Exception e) {
                Files.writeString(p,e.getMessage()!=null && e.getMessage().startsWith("Gmail rechazó")?"pendiente":"incierto");
                throw e;
            }
        }catch(java.nio.channels.OverlappingFileLockException e){throw new IllegalStateException("El envío de esta venta está en proceso.");}
    }
}
