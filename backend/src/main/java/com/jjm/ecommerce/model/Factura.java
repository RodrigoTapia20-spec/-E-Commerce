package com.jjm.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Solicitud de facturación (CFDI) de un pedido. El timbrado fiscal real ante
 * el SAT requiere contratar un PAC; este registro deja lista toda la
 * información fiscal para conectarlo sin tocar el resto del sistema.
 */
@Entity
@Table(name = "facturas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_factura")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "rfc_receptor", nullable = false, length = 20)
    private String rfcReceptor;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(name = "uso_cfdi", length = 10)
    @Builder.Default
    private String usoCfdi = "G03";

    @Column(name = "regimen_fiscal", length = 10)
    private String regimenFiscal;

    @Column(name = "cp_fiscal", length = 10)
    private String cpFiscal;

    @Column(name = "correo_envio", length = 150)
    private String correoEnvio;

    @Column(name = "folio_interno", nullable = false, length = 30)
    private String folioInterno;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal iva;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(length = 25)
    @Builder.Default
    private String estatus = "PENDIENTE_TIMBRADO"; // PENDIENTE_TIMBRADO / TIMBRADA / CANCELADA

    @Column(name = "uuid_fiscal", length = 50)
    private String uuidFiscal;

    @Column(name = "fecha_emision")
    private LocalDateTime fechaEmision;

    @PrePersist
    void prePersist() {
        if (fechaEmision == null) fechaEmision = LocalDateTime.now();
    }
}
