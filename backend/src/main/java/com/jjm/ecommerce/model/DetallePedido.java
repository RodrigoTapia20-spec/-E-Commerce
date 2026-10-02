package com.jjm.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "detalle_pedido")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_aliado", nullable = false)
    private Aliado aliado;

    @Column(nullable = false)
    private Integer cantidad;

    /** NORMAL (costo normal) o PROMOCION (campaña de temporada / causa social). */
    @Column(length = 15, nullable = false)
    @Builder.Default
    private String modalidad = "NORMAL";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_promocion")
    private Promocion promocion;

    @Column(name = "precio_lista", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioLista;

    /** Precio unitario realmente cobrado (con descuento si la modalidad fue PROMOCION). */
    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "porcentaje_descuento", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal porcentajeDescuento = BigDecimal.ZERO;

    @Column(name = "porcentaje_aplicado", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeAplicado;

    @Column(name = "monto_aliado", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoAliado;

    @Column(name = "monto_empresa", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoEmpresa;

    /** Parte del importe que se dona a la asociación/fundación de la campaña. */
    @Column(name = "monto_causa", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal montoCausa = BigDecimal.ZERO;
}
