package com.jjm.ecommerce.dto;

import lombok.Data;

@Data
public class CarritoItemRequest {
    private Integer idProducto;
    private Integer cantidad;
    private String modalidad;       // NORMAL (costo normal) o PROMOCION (campaña / causa)
    private Integer idPaqueteria;   // esquema de mensajería elegido para este producto
}
