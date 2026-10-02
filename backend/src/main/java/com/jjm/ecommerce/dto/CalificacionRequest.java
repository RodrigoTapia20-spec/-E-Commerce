package com.jjm.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CalificacionRequest {
    @NotNull private Integer idPedido;
    @NotNull private Integer idAliado;
    @NotNull private Integer calificacion; // 1 a 5
    private String comentario;
}
