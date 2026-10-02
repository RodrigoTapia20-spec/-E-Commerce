package com.jjm.ecommerce.controller;

import com.jjm.ecommerce.dto.CrearPedidoRequest;
import com.jjm.ecommerce.dto.Vistas.PedidoView;
import com.jjm.ecommerce.security.CurrentUserProvider;
import com.jjm.ecommerce.service.PedidoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Compra, historial y seguimiento (guías y tiempos de entrega) — común para web y app. */
@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final CurrentUserProvider currentUser;

    public PedidoController(PedidoService pedidoService, CurrentUserProvider currentUser) {
        this.pedidoService = pedidoService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public PedidoView crear(@RequestBody CrearPedidoRequest req) {
        return pedidoService.crearYPagarPedido(currentUser.obtenerId(), req);
    }

    @GetMapping
    public List<PedidoView> historial() {
        return pedidoService.historial(currentUser.obtenerId());
    }

    @GetMapping("/{id}")
    public PedidoView detalle(@PathVariable Integer id) {
        return pedidoService.detalle(currentUser.obtener(), id);
    }
}
