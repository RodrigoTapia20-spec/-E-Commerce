package com.jjm.ecommerce.controller;

import com.jjm.ecommerce.dto.ResenaRequest;
import com.jjm.ecommerce.dto.Vistas.ResenaView;
import com.jjm.ecommerce.security.CurrentUserProvider;
import com.jjm.ecommerce.service.ResenaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Calificación del producto (distinta de la calificación al vendedor, ver CalificacionController). */
@RestController
@RequestMapping("/resenas")
public class ResenaController {

    private final ResenaService resenaService;
    private final CurrentUserProvider currentUser;

    public ResenaController(ResenaService resenaService, CurrentUserProvider currentUser) {
        this.resenaService = resenaService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public Map<String, String> crear(@Valid @RequestBody ResenaRequest req) {
        resenaService.crear(currentUser.obtener(), req);
        return Map.of("mensaje", "¡Gracias por calificar el producto!");
    }

    @GetMapping("/producto/{idProducto}")
    public List<ResenaView> deProducto(@PathVariable Integer idProducto) {
        return resenaService.deProducto(idProducto);
    }
}
