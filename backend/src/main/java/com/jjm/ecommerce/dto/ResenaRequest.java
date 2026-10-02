package com.jjm.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Calificación del PRODUCTO (distinta de la calificación al vendedor). */
@Data
public class ResenaRequest {
    @NotNull private Integer idProducto;
    @NotNull private Integer calificacion; // 1 a 5
    private String comentario; // opcional
}
