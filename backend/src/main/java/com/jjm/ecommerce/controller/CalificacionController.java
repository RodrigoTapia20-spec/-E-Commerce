package com.jjm.ecommerce.controller;

import com.jjm.ecommerce.dto.CalificacionRequest;
import com.jjm.ecommerce.dto.Vistas.AliadoView;
import com.jjm.ecommerce.dto.Vistas.CalificacionView;
import com.jjm.ecommerce.security.CurrentUserProvider;
import com.jjm.ecommerce.service.RankingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Ranking de vendedores: los clientes califican de 1 a 5 según sus resultados. */
@RestController
@RequestMapping("/calificaciones")
public class CalificacionController {

    private final RankingService rankingService;
    private final CurrentUserProvider currentUser;

    public CalificacionController(RankingService rankingService, CurrentUserProvider currentUser) {
        this.rankingService = rankingService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public Map<String, String> calificar(@Valid @RequestBody CalificacionRequest req) {
        rankingService.calificar(currentUser.obtener(), req);
        return Map.of("mensaje", "¡Gracias por calificar al vendedor!");
    }

    @GetMapping("/ranking")
    public List<AliadoView> ranking() {
        return rankingService.ranking();
    }

    @GetMapping("/aliado/{idAliado}")
    public List<CalificacionView> deAliado(@PathVariable Integer idAliado) {
        return rankingService.calificacionesDe(idAliado);
    }
}
