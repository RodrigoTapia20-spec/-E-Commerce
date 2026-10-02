package com.jjm.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/** Empresas de mensajería disponibles (DHL, FedEx, Estafeta, etc.) con su tarifa estimada. */
@Entity
@Table(name = "paqueterias")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Paqueteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paqueteria")
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(name = "tiempo_entrega_dias_min", nullable = false)
    private Integer tiempoEntregaDiasMin;

    @Column(name = "tiempo_entrega_dias_max", nullable = false)
    private Integer tiempoEntregaDiasMax;

    @Column(name = "costo_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoBase;

    @Column(name = "costo_por_kg", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal costoPorKg = BigDecimal.ZERO;

    @Builder.Default
    private Boolean activa = true;
}
