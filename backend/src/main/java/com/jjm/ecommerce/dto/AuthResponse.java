package com.jjm.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String rol;
    private Integer idUsuario;
    private String nombre;
    private boolean requiere2fa;
    /** true justo después de registrarse: la cuenta existe pero falta confirmar el código enviado al correo. */
    private boolean requiereVerificacionCorreo;
    /** Mensaje informativo para mostrar al usuario (ej. "revisa tu correo", "tu cuenta está en revisión"). */
    private String mensaje;
}
