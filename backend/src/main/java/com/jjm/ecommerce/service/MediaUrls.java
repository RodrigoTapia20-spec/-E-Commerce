package com.jjm.ecommerce.service;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Los archivos subidos se guardan como "/uploads/xxx.jpg". Al responder, se
 * convierten a URL absoluta usando el mismo host con el que el cliente llamó a la API
 * (así funciona igual desde el navegador, el emulador Android y un celular físico).
 */
public final class MediaUrls {
    private MediaUrls() {}

    public static String absoluta(String url) {
        if (url != null && url.startsWith("/uploads/")) {
            try {
                return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() + url;
            } catch (IllegalStateException e) {
                return url; // sin petición HTTP activa
            }
        }
        return url;
    }
}
