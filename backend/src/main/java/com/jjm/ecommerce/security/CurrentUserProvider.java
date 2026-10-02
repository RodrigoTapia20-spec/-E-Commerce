package com.jjm.ecommerce.security;

import com.jjm.ecommerce.model.Usuario;
import com.jjm.ecommerce.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resuelve el {@link Usuario} autenticado en la petición actual (idéntico
 * para peticiones que vienen de la webapp o de la app móvil, ya que ambas
 * viajan con el mismo JWT en el header Authorization).
 */
@Component
public class CurrentUserProvider {

    private final UsuarioRepository usuarioRepository;

    public CurrentUserProvider(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario obtener() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No hay una sesión autenticada.");
        }
        String correo = auth.getName();
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado."));
    }

    public Integer obtenerId() {
        return obtener().getId();
    }
}
