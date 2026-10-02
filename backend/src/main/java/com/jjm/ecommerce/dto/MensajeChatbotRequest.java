package com.jjm.ecommerce.dto;

import lombok.Data;

@Data
public class MensajeChatbotRequest {
    private Long idConversacion; // null para iniciar una nueva conversación
    private String mensaje;
    private String canal;        // WEB / APP
    private String contexto;     // CLIENTE / VENDEDOR / DISTRIBUIDOR / ADMIN (lo define la página o pantalla)
    private String pagina;       // dónde está el usuario: catalogo, carrito, publicar-producto, ...
}
