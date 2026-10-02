package com.jjm.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Campaña de marketing lanzada por el propio vendedor/proveedor: temporada
 * (Navidad, Día de Muertos, Fiestas Patrias...), % de descuento (20/30/40 u
 * otro) y, opcionalmente, dirigida a una asociación civil / fundación sin
 * fines de lucro que recibe un % de donación de cada venta.
 */
@Entity
@Table(name = "promociones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Promocion {

    public enum TipoTemporada {
        GENERAL, NAVIDAD, DIA_DE_MUERTOS, FIESTAS_PATRIAS, BUEN_FIN,
        DIA_DE_LAS_MADRES, SAN_VALENTIN, OTRA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_promocion")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_aliado", nullable = false)
    private Aliado aliado;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "porcentaje_descuento", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeDescuento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_temporada", length = 25)
    @Builder.Default
    private TipoTemporada tipoTemporada = TipoTemporada.GENERAL;

    @Column(name = "es_causa_social")
    @Builder.Default
    private Boolean esCausaSocial = false;

    @Column(name = "fundacion_beneficiaria", length = 150)
    private String fundacionBeneficiaria;

    /** % del importe de cada venta que se dona a la fundación (sale de la parte del vendedor). */
    @Column(name = "porcentaje_donacion", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal porcentajeDonacion = BigDecimal.ZERO;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(length = 20)
    @Builder.Default
    private String estatus = "ACTIVA"; // ACTIVA / PAUSADA / CANCELADA

    @ManyToMany
    @JoinTable(
        name = "promocion_productos",
        joinColumns = @JoinColumn(name = "id_promocion"),
        inverseJoinColumns = @JoinColumn(name = "id_producto")
    )
    @Builder.Default
    private List<Producto> productos = new ArrayList<>();

    public boolean vigenteHoy() {
        LocalDate hoy = LocalDate.now();
        return "ACTIVA".equals(estatus) && !hoy.isBefore(fechaInicio) && !hoy.isAfter(fechaFin);
    }
}
