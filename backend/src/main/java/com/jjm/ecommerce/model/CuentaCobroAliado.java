package com.jjm.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Datos de cobro del vendedor (dónde recibe el dinero de sus ventas).
 *
 * SEGURIDAD: por norma de la industria (PCI-DSS), nunca se debe guardar el
 * número completo de una tarjeta ni la CLABE completa en una base de datos
 * propia sin cifrado certificado. Aquí sólo se guarda el dato ENMASCARADO
 * (últimos 4 dígitos) para mostrarlo de referencia al vendedor; el número
 * completo llega desde el formulario pero NUNCA se persiste.
 */
@Entity
@Table(name = "cuentas_cobro_aliado")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CuentaCobroAliado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuenta")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_aliado", nullable = false, unique = true)
    private Aliado aliado;

    @Column(nullable = false, length = 150)
    private String titular;

    @Column(nullable = false, length = 100)
    private String banco;

    @Column(name = "tipo_cuenta", nullable = false, length = 20)
    private String tipoCuenta; // CLABE / TARJETA

    @Column(name = "numero_enmascarado", nullable = false, length = 30)
    private String numeroEnmascarado; // ej. "**** **** **** 4242"

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist @PreUpdate
    void marcarFecha() {
        fechaActualizacion = LocalDateTime.now();
    }
}
