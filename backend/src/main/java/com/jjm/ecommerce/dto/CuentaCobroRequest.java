package com.jjm.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * numeroCompleto llega desde el formulario del vendedor sólo para extraer los
 * últimos 4 dígitos; el backend NUNCA lo guarda completo (ver CuentaCobroAliado).
 */
@Data
public class CuentaCobroRequest {
    @NotBlank private String titular;
    @NotBlank private String banco;
    @NotBlank private String tipoCuenta; // CLABE / TARJETA
    @NotBlank private String numeroCompleto;
}
