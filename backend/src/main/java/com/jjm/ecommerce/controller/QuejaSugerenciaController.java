package com.jjm.ecommerce.controller;

import com.jjm.ecommerce.dto.QuejaRequest;
import com.jjm.ecommerce.dto.Vistas.QuejaView;
import com.jjm.ecommerce.security.CurrentUserProvider;
import com.jjm.ecommerce.service.QuejaSugerenciaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quejas")
public class QuejaSugerenciaController {

    private final QuejaSugerenciaService quejaService;
    private final CurrentUserProvider currentUser;

    public QuejaSugerenciaController(QuejaSugerenciaService quejaService, CurrentUserProvider currentUser) {
        this.quejaService = quejaService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public QuejaView crear(@Valid @RequestBody QuejaRequest req) {
        return quejaService.crear(currentUser.obtenerId(), req);
    }

    @GetMapping("/mias")
    public List<QuejaView> mias() {
        return quejaService.mias(currentUser.obtenerId());
    }

    @GetMapping("/admin/abiertas")
    public List<QuejaView> abiertas() {
        return quejaService.abiertas();
    }

    @PatchMapping("/admin/{id}/responder")
    public QuejaView responder(@PathVariable Integer id, @RequestParam String respuesta,
                               @RequestParam String estatus) {
        return quejaService.responder(id, respuesta, estatus);
    }
}
