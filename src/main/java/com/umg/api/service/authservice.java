package com.umg.api.service;

import com.umg.api.model.usuario;
import com.umg.api.config.ApiConfig;
import java.util.Map;
import java.util.List;

/**
 * Usa la sesión validada por el backend de comprobantes. Conserva compatibilidad
 * con la API anterior mientras se publica el nuevo endpoint.
 */
public class authservice {

    public static class LoginRespuesta {
        public String token;          // la API no maneja token
        public usuario usuario;
    }

    public LoginRespuesta login(String nombreUsuario, String contrasena) throws Exception {
        try {
            return HttpJson.post(ApiConfig.url("comprobantes/login"),
                    Map.of("nombre_usuario", nombreUsuario, "contrasena", contrasena), LoginRespuesta.class);
        } catch (HttpJson.ErrorHttp e) {
            // Durante el despliegue gradual, el servidor anterior aún permite entrar.
            // Sin token, el envío queda pendiente y explica que falta activar el backend.
            if (e.codigo != 404 && e.codigo != 503) throw e;
        }
        List<usuario> lista = new usuarioservice().getUsuario();
        for (usuario u : lista) {
            if (nombreUsuario.equals(u.getNombre_usuario()) && contrasena.equals(u.getContrasena_hash())) {
                LoginRespuesta r = new LoginRespuesta();
                r.usuario = u;
                return r;
            }
        }
        throw new Exception("Usuario o contraseña incorrectos.");
    }
}
