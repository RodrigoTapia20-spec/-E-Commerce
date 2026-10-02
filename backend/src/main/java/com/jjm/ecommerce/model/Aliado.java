package com.jjm.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "aliados")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Aliado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aliado")
    private Integer id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "nombre_comercial", nullable = false, length = 150)
    private String nombreComercial;

    @Column(name = "causa_social", nullable = true)
    private String causaSocial; // Opcional: el aliado puede registrarse sin apoyar una causa específica

    @Column(length = 20)
    private String rfc;

    @Column(name = "porcentaje_convenio", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeConvenio;

    @Column(name = "documentos_validados")
    @Builder.Default
    private Boolean documentosValidados = false;

    @Column(name = "estatus_verificacion", length = 20)
    @Builder.Default
    private String estatusVerificacion = "PENDIENTE";

    @Column(name = "fecha_alta")
    private LocalDateTime fechaAlta;

    @PrePersist
    void prePersist() {
        if (fechaAlta == null) fechaAlta = LocalDateTime.now();
    }
}
