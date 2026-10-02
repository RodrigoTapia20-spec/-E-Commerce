package com.jjm.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Calificación (1-5) que un cliente da a un aliado/vendedor tras un pedido. */
@Entity
@Table(name = "calificaciones_vendedor", uniqueConstraints = {
        @UniqueConstraint(name = "unico_por_pedido", columnNames = {"id_aliado", "id_usuario", "id_pedido"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CalificacionVendedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_calificacion")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_aliado", nullable = false)
    private Aliado aliado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido")
    private Pedido pedido;

    @Column(nullable = false)
    private Integer calificacion; // 1-5

    @Column(columnDefinition = "TEXT")
    private String comentario;

    private LocalDateTime fecha;

    @PrePersist
    void prePersist() {
        if (fecha == null) fecha = LocalDateTime.now();
    }
}
