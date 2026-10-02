package com.jjm.ecommerce.dto;

import lombok.Data;

/** Un elemento de galería que el proveedor sube al publicar: imagen o video corto. */
@Data
public class MediaProductoRequest {
    private String tipo;             // IMAGEN / VIDEO
    private String url;
    private Integer duracionSegundos; // solo si tipo = VIDEO (recomendado 10-15 s)
    private Boolean conAudio;         // solo si tipo = VIDEO
}
