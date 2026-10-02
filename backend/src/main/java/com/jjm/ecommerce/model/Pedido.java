package com.jjm.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "costo_envio", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal costoEnvio = BigDecimal.ZERO;

    @Column(name = "monto_causa", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal montoCausa = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(length = 20)
    @Builder.Default
    private String estatus = "PENDIENTE"; // PENDIENTE/PAGADO/ENVIADO/ENTREGADO/CANCELADO

    @Column(name = "direccion_envio", length = 255)
    private String direccionEnvio;

    @Column(name = "cp_envio", length = 10)
    private String cpEnvio;

    @Column(name = "fecha_pedido")
    private LocalDateTime fechaPedido;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetallePedido> detalles = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (fechaPedido == null) fechaPedido = LocalDateTime.now();
    }
}
