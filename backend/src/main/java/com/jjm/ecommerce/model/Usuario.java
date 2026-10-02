package com.jjm.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Column(length = 20)
    private String telefono;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @Column(name = "token_2fa", length = 10)
    private String token2fa;

    @Column(name = "reset_password_token", length = 100)
    private String resetPasswordToken;

    @Column(name = "reset_password_expira")
    private LocalDateTime resetPasswordExpira;

    @Column(name = "verificado_ia")
    @Builder.Default
    private Boolean verificadoIa = false;

    @Column(name = "biometria_hash")
    private String biometriaHash;

    @Column(name = "nivel_seguridad", length = 20)
    @Builder.Default
    private String nivelSeguridad = "ESTANDAR";

    @Column(length = 20)
    @Builder.Default
    private String estatus = "ACTIVO";

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @PrePersist
    void prePersist() {
        if (fechaRegistro == null) fechaRegistro = LocalDateTime.now();
    }
}
