package com.jjm.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispersiones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Dispersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dispersion")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pago", nullable = false)
    private Pago pago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_aliado", nullable = false)
    private Aliado aliado;

    @Column(name = "monto_aliado", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoAliado;

    @Column(name = "monto_empresa", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoEmpresa;

    @Column(name = "monto_causa", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal montoCausa = BigDecimal.ZERO;

    @Column(length = 20)
    @Builder.Default
    private String estatus = "PENDIENTE"; // PENDIENTE/LIBERADO

    @Column(name = "fecha_dispersion")
    private LocalDateTime fechaDispersion;
}
