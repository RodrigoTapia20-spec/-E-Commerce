package com.jjm.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistroRequest {
    @NotBlank private String nombre;
    @NotBlank private String apellidos;
    @Email @NotBlank private String correo;
    private String telefono;
    @NotBlank @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;
    @NotBlank private String rol; // CLIENTE o ALIADO

    // Datos adicionales si rol = ALIADO
    private String nombreComercial;
    private String causaSocial;
    private String rfc;
    private java.math.BigDecimal porcentajeConvenio;
}
