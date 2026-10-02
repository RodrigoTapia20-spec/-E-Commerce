package com.jjm.ecommerce.controller;

import com.jjm.ecommerce.dto.Vistas.EnvioDistView;
import com.jjm.ecommerce.dto.Vistas.PaqueteriaView;
import com.jjm.ecommerce.service.EnvioService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class EnvioController {

    private final EnvioService envioService;

    public EnvioController(EnvioService envioService) {
        this.envioService = envioService;
    }

    /** Catálogo público de paqueterías (para que el vendedor elija las de cada producto). */
    @GetMapping("/envios/paqueterias")
    public List<PaqueteriaView> paqueterias() {
        return envioService.listarActivas();
    }

    // ---------- Panel del Distribuidor
    @GetMapping("/distribuidor/envios")
    public List<EnvioDistView> envios(@RequestParam(required = false) String estatus) {
        return envioService.listarParaDistribuidor(estatus);
    }

    @PatchMapping("/distribuidor/envios/{id}/estatus")
    public EnvioDistView cambiarEstatus(@PathVariable Integer id, @RequestParam String estatus) {
        return envioService.cambiarEstatus(id, estatus);
    }
}
