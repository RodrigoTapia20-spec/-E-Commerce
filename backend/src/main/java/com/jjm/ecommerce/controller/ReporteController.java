package com.jjm.ecommerce.controller;

import com.jjm.ecommerce.service.ReporteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Reportes generales — solo Administrador (ver SecurityConfig). */
@RestController
@RequestMapping("/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/ventas-general")
    public Map<String, Object> ventasGeneral() {
        return reporteService.reporteGeneralVentas();
    }
}
