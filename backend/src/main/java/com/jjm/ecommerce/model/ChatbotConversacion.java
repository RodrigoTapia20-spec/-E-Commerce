package com.jjm.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chatbot_conversaciones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatbotConversacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conversacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(length = 20)
    @Builder.Default
    private String canal = "WEB"; // WEB / APP

    @Column(length = 20)
    @Builder.Default
    private String contexto = "CLIENTE"; // CLIENTE / VENDEDOR / DISTRIBUIDOR / ADMIN

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @OneToMany(mappedBy = "conversacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<ChatbotMensaje> mensajes = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (fechaInicio == null) fechaInicio = LocalDateTime.now();
    }
}
