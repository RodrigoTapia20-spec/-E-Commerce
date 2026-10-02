package com.jjm.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_metodo", nullable = false)
    private MetodoPago metodo;

    @Column(name = "referencia_pasarela", length = 120)
    private String referenciaPasarela;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(length = 20)
    @Builder.Default
    private String estatus = "PROCESANDO"; // PROCESANDO/APROBADO/RECHAZADO/REEMBOLSADO

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @PrePersist
    void prePersist() {
        if (fechaPago == null) fechaPago = LocalDateTime.now();
    }
}
