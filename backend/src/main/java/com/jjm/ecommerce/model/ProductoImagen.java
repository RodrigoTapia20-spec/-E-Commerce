package com.jjm.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Un elemento de la galería de un producto: puede ser una imagen o un video
 * corto (10-15 s aprox., con o sin audio). Cada producto debe tener entre
 * 5 y 7 imágenes (regla validada en ProductoService al publicar); los videos
 * son adicionales y opcionales.
 */
@Entity
@Table(name = "producto_imagenes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductoImagen {

    public enum Tipo { IMAGEN, VIDEO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imagen")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Tipo tipo = Tipo.IMAGEN;

    @Column(name = "url_imagen", nullable = false)
    private String urlImagen;

    @Column(name = "duracion_segundos")
    private Integer duracionSegundos; // solo aplica si tipo = VIDEO

    @Column(name = "con_audio")
    private Boolean conAudio; // solo aplica si tipo = VIDEO

    @Builder.Default
    private Integer orden = 0;
}
