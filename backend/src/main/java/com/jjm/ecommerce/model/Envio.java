package com.jjm.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Guía de envío de UN producto del pedido: paquetería elegida por el cliente
 * para ese producto, costo (ya sumado al total), número de guía y tiempo de
 * entrega estimado. El Distribuidor actualiza su estatus.
 */
@Entity
@Table(name = "envios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Envio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_envio")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_detalle", nullable = false)
    private DetallePedido detalle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paqueteria", nullable = false)
    private Paqueteria paqueteria;

    @Column(name = "numero_guia", nullable = false, length = 60)
    private String numeroGuia;

    @Column(name = "costo_envio", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoEnvio;

    @Column(name = "tiempo_entrega_estimado_dias", nullable = false)
    private Integer tiempoEntregaEstimadoDias;

    @Column(name = "fecha_estimada_entrega")
    private LocalDate fechaEstimadaEntrega;

    @Column(length = 20)
    @Builder.Default
    private String estatus = "PENDIENTE"; // PENDIENTE / EN_TRANSITO / ENTREGADO

    @Column(name = "fecha_generacion")
    private LocalDateTime fechaGeneracion;

    @PrePersist
    void prePersist() {
        if (fechaGeneracion == null) fechaGeneracion = LocalDateTime.now();
    }
}
