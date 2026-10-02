package com.jjm.ecommerce.controller;

import com.jjm.ecommerce.dto.FacturaRequest;
import com.jjm.ecommerce.dto.Vistas.FacturaView;
import com.jjm.ecommerce.security.CurrentUserProvider;
import com.jjm.ecommerce.service.FacturaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/facturas")
public class FacturaController {

    private final FacturaService facturaService;
    private final CurrentUserProvider currentUser;

    public FacturaController(FacturaService facturaService, CurrentUserProvider currentUser) {
        this.facturaService = facturaService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public FacturaView emitir(@Valid @RequestBody FacturaRequest req) {
        return facturaService.emitir(currentUser.obtener(), req);
    }

    @GetMapping("/mias")
    public List<FacturaView> mias() {
        return facturaService.mias(currentUser.obtenerId());
    }

    @GetMapping("/pedido/{idPedido}")
    public FacturaView porPedido(@PathVariable Integer idPedido) {
        return facturaService.porPedido(currentUser.obtener(), idPedido);
    }
}
