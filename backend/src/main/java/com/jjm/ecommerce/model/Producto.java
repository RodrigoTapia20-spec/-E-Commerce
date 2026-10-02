package com.jjm.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Producto o servicio publicado por un vendedor (aliado). El "precio" es siempre
 * el costo normal; las promociones (temporada / causa social) se calculan en
 * vivo a partir de las campañas vigentes, así el cliente puede elegir entre
 * costo normal y precio con promoción al comprar.
 */
@Entity
@Table(name = "productos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Producto {

    public enum Tipo { PRODUCTO, SERVICIO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_aliado", nullable = false)
    private Aliado aliado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Tipo tipo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /** Costo normal (opción a): el que maneja cualquier e-commerce tradicional. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "peso_kg", precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal pesoKg = new BigDecimal("0.50");

    @Builder.Default
    private Integer existencia = 0;

    @Column(name = "verificado_ia")
    @Builder.Default
    private Boolean verificadoIa = false;

    @Column(length = 20)
    @Builder.Default
    private String estatus = "ACTIVO";

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    /** Galería: 5 a 7 imágenes + videos cortos opcionales. */
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<ProductoImagen> imagenes = new ArrayList<>();

    /** Esquemas de mensajería que el vendedor habilita para este producto. */
    @ManyToMany
    @JoinTable(name = "producto_paqueterias",
            joinColumns = @JoinColumn(name = "id_producto"),
            inverseJoinColumns = @JoinColumn(name = "id_paqueteria"))
    @Builder.Default
    private Set<Paqueteria> paqueterias = new LinkedHashSet<>();

    @PrePersist
    void prePersist() {
        if (fechaPublicacion == null) fechaPublicacion = LocalDateTime.now();
    }
}
