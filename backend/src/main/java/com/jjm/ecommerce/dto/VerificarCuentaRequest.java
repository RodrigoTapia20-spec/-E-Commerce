package com.jjm.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Paso 2 del registro: confirmar el código de 6 dígitos enviado por correo. */
@Data
public class VerificarCuentaRequest {
    @Email @NotBlank private String correo;
    @NotBlank private String codigo;
}
