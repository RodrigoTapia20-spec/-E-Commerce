package com.jjm.ecommerce.controller;

import com.jjm.ecommerce.dto.CuentaCobroRequest;
import com.jjm.ecommerce.dto.Vistas.CuentaCobroView;
import com.jjm.ecommerce.dto.Vistas.PagoRecibidoView;
import com.jjm.ecommerce.security.CurrentUserProvider;
import com.jjm.ecommerce.service.AliadoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Datos del vendedor autenticado: perfil, reporte y pagos/cobros. */
@RestController
@RequestMapping("/aliados")
public class AliadoController {

    private final AliadoService aliadoService;
    private final CurrentUserProvider currentUser;

    public AliadoController(AliadoService aliadoService, CurrentUserProvider currentUser) {
        this.aliadoService = aliadoService;
        this.currentUser = currentUser;
    }

    @GetMapping("/mi-perfil")
    public Map<String, Object> perfil() {
        return aliadoService.perfil(currentUser.obtenerId());
    }

    @GetMapping("/mi-reporte")
    public Map<String, Object> reporte() {
        return aliadoService.reporte(currentUser.obtenerId());
    }

    /** Punto 10: registrar/actualizar dónde recibe sus pagos el vendedor. */
    @PostMapping("/mi-cuenta-cobro")
    public CuentaCobroView guardarCuentaCobro(@Valid @RequestBody CuentaCobroRequest req) {
        return aliadoService.guardarCuentaCobro(currentUser.obtenerId(), req);
    }

    @GetMapping("/mi-cuenta-cobro")
    public CuentaCobroView miCuentaCobro() {
        return aliadoService.miCuentaCobro(currentUser.obtenerId());
    }

    @GetMapping("/mis-pagos")
    public List<PagoRecibidoView> misPagos() {
        return aliadoService.misPagos(currentUser.obtenerId());
    }
}
