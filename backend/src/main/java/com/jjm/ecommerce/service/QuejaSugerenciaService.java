package com.jjm.ecommerce.service;

import com.jjm.ecommerce.dto.QuejaRequest;
import com.jjm.ecommerce.dto.Vistas.QuejaView;
import com.jjm.ecommerce.model.QuejaSugerencia;
import com.jjm.ecommerce.model.Usuario;
import com.jjm.ecommerce.repository.QuejaSugerenciaRepository;
import com.jjm.ecommerce.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class QuejaSugerenciaService {

    private final QuejaSugerenciaRepository quejaRepository;
    private final UsuarioRepository usuarioRepository;

    public QuejaSugerenciaService(QuejaSugerenciaRepository quejaRepository, UsuarioRepository usuarioRepository) {
        this.quejaRepository = quejaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public QuejaView crear(Integer idUsuario, QuejaRequest req) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        QuejaSugerencia.Tipo tipo;
        try {
            tipo = QuejaSugerencia.Tipo.valueOf(req.getTipo().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El tipo debe ser QUEJA o SUGERENCIA.");
        }
        return toView(quejaRepository.save(QuejaSugerencia.builder()
                .usuario(usuario).tipo(tipo).asunto(req.getAsunto()).descripcion(req.getDescripcion()).build()));
    }

    @Transactional(readOnly = true)
    public List<QuejaView> abiertas() {
        return quejaRepository.findByEstatus("ABIERTA").stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<QuejaView> mias(Integer idUsuario) {
        return quejaRepository.findByUsuario_Id(idUsuario).stream().map(this::toView).toList();
    }

    @Transactional
    public QuejaView responder(Integer idQueja, String respuesta, String nuevoEstatus) {
        QuejaSugerencia q = quejaRepository.findById(idQueja)
                .orElseThrow(() -> new IllegalArgumentException("Queja/sugerencia no encontrada."));
        q.setRespuesta(respuesta);
        q.setEstatus(nuevoEstatus.toUpperCase());
        if ("RESUELTA".equalsIgnoreCase(nuevoEstatus) || "CERRADA".equalsIgnoreCase(nuevoEstatus)) {
            q.setFechaResolucion(LocalDateTime.now());
        }
        return toView(quejaRepository.save(q));
    }

    private QuejaView toView(QuejaSugerencia q) {
        return new QuejaView(q.getId(), q.getTipo().name(), q.getAsunto(), q.getDescripcion(), q.getEstatus(),
                q.getRespuesta(), q.getUsuario().getNombre() + " " + q.getUsuario().getApellidos(),
                q.getFechaCreacion());
    }
}
