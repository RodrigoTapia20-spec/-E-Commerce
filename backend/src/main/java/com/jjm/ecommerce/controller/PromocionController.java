package com.jjm.ecommerce.controller;

import com.jjm.ecommerce.dto.PromocionRequest;
import com.jjm.ecommerce.dto.Vistas.PromocionView;
import com.jjm.ecommerce.security.CurrentUserProvider;
import com.jjm.ecommerce.service.PromocionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PromocionController {

    private final PromocionService promocionService;
    private final CurrentUserProvider currentUser;

    public PromocionController(PromocionService promocionService, CurrentUserProvider currentUser) {
        this.promocionService = promocionService;
        this.currentUser = currentUser;
    }

    @GetMapping("/promociones/activas")
    public List<PromocionView> activas() {
        return promocionService.vigentes();
    }

    @PostMapping("/aliados/promociones")
    public PromocionView crear(@Valid @RequestBody PromocionRequest req) {
        return promocionService.crear(currentUser.obtenerId(), req);
    }

    @GetMapping("/aliados/promociones")
    public List<PromocionView> mias() {
        return promocionService.mias(currentUser.obtenerId());
    }

    @PatchMapping("/aliados/promociones/{id}/estatus")
    public PromocionView cambiarEstatus(@PathVariable Integer id, @RequestParam String estatus) {
        return promocionService.cambiarEstatus(currentUser.obtenerId(), id, estatus);
    }
}
