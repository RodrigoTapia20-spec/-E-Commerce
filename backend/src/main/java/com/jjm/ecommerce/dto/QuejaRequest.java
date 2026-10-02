package com.jjm.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QuejaRequest {
    @NotBlank private String tipo; // QUEJA / SUGERENCIA
    @NotBlank private String asunto;
    @NotBlank private String descripcion;
}
