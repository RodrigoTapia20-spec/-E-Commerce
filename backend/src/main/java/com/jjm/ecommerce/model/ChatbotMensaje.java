package com.jjm.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chatbot_mensajes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatbotMensaje {

    public enum Emisor { USUARIO, BOT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_conversacion", nullable = false)
    private ChatbotConversacion conversacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Emisor emisor;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column
    private LocalDateTime fecha;

    @PrePersist
    void prePersist() {
        if (fecha == null) fecha = LocalDateTime.now();
    }
}
