package com.jjm.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Alta de cuentas internas (Distribuidor o Administrador) — a diferencia del
 * registro público (/auth/registro), este endpoint sólo lo puede usar un
 * Administrador ya autenticado (ver SecurityConfig: "/admin/**").
 */
@Data
public class AdminCrearUsuarioRequest {
    @NotBlank private String nombre;
    @NotBlank private String apellidos;
    @Email @NotBlank private String correo;
    private String telefono;
    @NotBlank @Size(min = 8) private String password;
    @NotBlank private String rol; // DISTRIBUIDOR o ADMIN
}
