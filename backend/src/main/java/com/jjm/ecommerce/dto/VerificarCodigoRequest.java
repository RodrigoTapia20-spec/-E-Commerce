package com.jjm.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerificarCodigoRequest {
    @NotBlank private String correo;
    @NotBlank private String codigo;
}
