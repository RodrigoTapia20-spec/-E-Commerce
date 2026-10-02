package com.jjm.ecommerce.controller;

import com.jjm.ecommerce.dto.MensajeChatbotRequest;
import com.jjm.ecommerce.dto.Vistas.ConversacionView;
import com.jjm.ecommerce.security.CurrentUserProvider;
import com.jjm.ecommerce.service.ChatbotIaService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chatbot")
public class ChatbotController {

    private final ChatbotIaService chatbotIaService;
    private final CurrentUserProvider currentUser;

    public ChatbotController(ChatbotIaService chatbotIaService, CurrentUserProvider currentUser) {
        this.chatbotIaService = chatbotIaService;
        this.currentUser = currentUser;
    }

    /** Disponible para visitantes y para usuarios con sesión. */
    @PostMapping("/mensaje")
    public ConversacionView enviar(@RequestBody MensajeChatbotRequest req) {
        Integer idUsuario = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            try {
                idUsuario = currentUser.obtenerId();
            } catch (Exception ignored) {
                // se atiende como conversación anónima
            }
        }
        return chatbotIaService.responder(idUsuario, req);
    }
}
