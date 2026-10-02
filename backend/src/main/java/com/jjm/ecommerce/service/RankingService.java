package com.jjm.ecommerce.service;

import com.jjm.ecommerce.dto.CalificacionRequest;
import com.jjm.ecommerce.dto.Vistas.AliadoView;
import com.jjm.ecommerce.dto.Vistas.CalificacionView;
import com.jjm.ecommerce.model.Aliado;
import com.jjm.ecommerce.model.CalificacionVendedor;
import com.jjm.ecommerce.model.DetallePedido;
import com.jjm.ecommerce.model.Envio;
import com.jjm.ecommerce.model.Producto;
import com.jjm.ecommerce.model.Pedido;
import com.jjm.ecommerce.model.Usuario;
import com.jjm.ecommerce.repository.AliadoRepository;
import com.jjm.ecommerce.repository.CalificacionVendedorRepository;
import com.jjm.ecommerce.repository.EnvioRepository;
import com.jjm.ecommerce.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Ranking de vendedores: los clientes califican (1-5) al vendedor después de comprar.
 * Niveles: NUEVO (menos de 3 calificaciones), BRONCE, PLATA, ORO y PLATINO.
 */
@Service
public class RankingService {

    private static final Set<String> ESTATUS_CALIFICABLES = Set.of("PAGADO", "ENVIADO", "ENTREGADO");

    private final CalificacionVendedorRepository calificacionRepository;
    private final AliadoRepository aliadoRepository;
    private final PedidoRepository pedidoRepository;
    private final EnvioRepository envioRepository;

    public RankingService(CalificacionVendedorRepository calificacionRepository,
                          AliadoRepository aliadoRepository, PedidoRepository pedidoRepository,
                          EnvioRepository envioRepository) {
        this.calificacionRepository = calificacionRepository;
        this.aliadoRepository = aliadoRepository;
        this.pedidoRepository = pedidoRepository;
        this.envioRepository = envioRepository;
    }

    public AliadoView aliadoView(Aliado a) {
        List<Object[]> resumen = calificacionRepository.resumenDeAliado(a.getId());
        Double promedio = null;
        long total = 0;
        if (!resumen.isEmpty() && resumen.get(0)[0] != null) {
            promedio = BigDecimal.valueOf(((Number) resumen.get(0)[0]).doubleValue())
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();
            total = ((Number) resumen.get(0)[1]).longValue();
        }
        return new AliadoView(a.getId(), a.getNombreComercial(), a.getCausaSocial(), promedio, total,
                nivel(promedio, total));
    }

    static String nivel(Double promedio, long total) {
        if (promedio == null || total < 3) return "NUEVO";
        if (promedio >= 4.5) return "PLATINO";
        if (promedio >= 4.0) return "ORO";
        if (promedio >= 3.0) return "PLATA";
        return "BRONCE";
    }

    @Transactional(readOnly = true)
    public List<AliadoView> ranking() {
        return aliadoRepository.findByEstatusVerificacion("APROBADO").stream()
                .map(this::aliadoView)
                .sorted(Comparator
                        .comparing((AliadoView v) -> v.calificacionPromedio() == null ? 0.0 : v.calificacionPromedio())
                        .thenComparingLong(AliadoView::totalCalificaciones).reversed())
                .limit(50)
                .toList();
    }

    @Transactional
    public void calificar(Usuario usuario, CalificacionRequest req) {
        if (req.getCalificacion() < 1 || req.getCalificacion() > 5) {
            throw new IllegalArgumentException("La calificación debe ser de 1 a 5 estrellas.");
        }
        Pedido pedido = pedidoRepository.findById(req.getIdPedido())
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));
        if (!pedido.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalStateException("Solo puedes calificar vendedores de tus propios pedidos.");
        }
        if (!ESTATUS_CALIFICABLES.contains(pedido.getEstatus())) {
            throw new IllegalStateException("Podrás calificar cuando el pedido esté pagado.");
        }
        DetallePedido detalle = pedido.getDetalles().stream()
                .filter(d -> d.getAliado().getId().equals(req.getIdAliado())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ese vendedor no participó en este pedido."));
        Aliado aliado = detalle.getAliado();
        // Punto 8: sólo se puede calificar al vendedor una vez que el pedido llegó a su domicilio
        // (producto físico entregado) o, si es un servicio, una vez pagado (no aplica envío).
        boolean recibido = detalle.getProducto().getTipo() == Producto.Tipo.SERVICIO
                || envioRepository.findByDetalle_Id(detalle.getId())
                        .map(Envio::getEstatus).map("ENTREGADO"::equals).orElse(false);
        if (!recibido) {
            throw new IllegalStateException("Podrás calificar a este vendedor cuando recibas tu pedido.");
        }
        if (calificacionRepository.existsByAliado_IdAndUsuario_IdAndPedido_Id(
                aliado.getId(), usuario.getId(), pedido.getId())) {
            throw new IllegalStateException("Ya calificaste a este vendedor por este pedido.");
        }
        calificacionRepository.save(CalificacionVendedor.builder()
                .aliado(aliado).usuario(usuario).pedido(pedido)
                .calificacion(req.getCalificacion())
                .comentario(req.getComentario() == null ? null : req.getComentario().trim())
                .build());
    }

    @Transactional(readOnly = true)
    public List<CalificacionView> calificacionesDe(Integer idAliado) {
        return calificacionRepository.findByAliado_IdOrderByFechaDesc(idAliado).stream()
                .map(c -> new CalificacionView(c.getId(), c.getUsuario().getNombre(), c.getCalificacion(),
                        c.getComentario(), c.getFecha()))
                .toList();
    }
}
