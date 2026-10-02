package com.jjm.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Publicación de un producto o servicio. La galería (media) debe traer de 5 a 7
 * elementos en total, entre imágenes y videos cortos (máx. 15 s, con o sin audio).
 */
@Data
public class ProductoRequest {
    @NotNull private Integer idCategoria;
    @NotBlank private String tipo;          // PRODUCTO / SERVICIO
    @NotBlank private String nombre;
    private String descripcion;
    @NotNull private BigDecimal precio;     // costo normal
    private BigDecimal pesoKg;              // para calcular el envío
    private Integer existencia;
    private List<MediaProductoRequest> media;
    private List<Integer> idsPaqueterias;   // esquemas de mensajería habilitados (vacío = todos)
}
