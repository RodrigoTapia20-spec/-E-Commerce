package com.jjm.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SolicitarRecuperacionRequest {
    @Email @NotBlank
    private String correo;
}
