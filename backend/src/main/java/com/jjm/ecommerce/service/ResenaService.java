package com.jjm.ecommerce.service;

import com.jjm.ecommerce.dto.ResenaRequest;
import com.jjm.ecommerce.dto.Vistas.ResenaView;
import com.jjm.ecommerce.model.DetallePedido;
import com.jjm.ecommerce.model.Envio;
import com.jjm.ecommerce.model.Producto;
import com.jjm.ecommerce.model.Resena;
import com.jjm.ecommerce.model.Usuario;
import com.jjm.ecommerce.repository.DetallePedidoRepository;
import com.jjm.ecommerce.repository.EnvioRepository;
import com.jjm.ecommerce.repository.ProductoRepository;
import com.jjm.ecommerce.repository.ResenaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Calificación del PRODUCTO que el cliente compró (independiente de la
 * calificación al vendedor, que maneja {@link RankingService}). Sólo puede
 * calificar quien: (a) compró ese producto, y (b) ya lo recibió — si es un
 * producto físico, su envío debe estar en estatus ENTREGADO; si es un
 * servicio (sin envío), basta con que el pedido esté pagado.
 */
@Service
public class ResenaService {

    private final ResenaRepository resenaRepository;
    private final ProductoRepository productoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final EnvioRepository envioRepository;

    public ResenaService(ResenaRepository resenaRepository, ProductoRepository productoRepository,
                         DetallePedidoRepository detallePedidoRepository, EnvioRepository envioRepository) {
        this.resenaRepository = resenaRepository;
        this.productoRepository = productoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.envioRepository = envioRepository;
    }

    @Transactional
    public ResenaView crear(Usuario usuario, ResenaRequest req) {
        if (req.getCalificacion() == null || req.getCalificacion() < 1 || req.getCalificacion() > 5) {
            throw new IllegalArgumentException("La calificación debe ser de 1 a 5 estrellas.");
        }
        if (resenaRepository.existsByProducto_IdAndUsuario_Id(req.getIdProducto(), usuario.getId())) {
            throw new IllegalStateException("Ya calificaste este producto.");
        }
        Producto producto = productoRepository.findById(req.getIdProducto())
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));

        List<DetallePedido> compras = detallePedidoRepository
                .findByProducto_IdAndPedido_Usuario_Id(req.getIdProducto(), usuario.getId());
        boolean elegible = compras.stream().anyMatch(d -> esElegible(d));
        if (!elegible) {
            throw new IllegalStateException("Sólo puedes calificar productos que hayas comprado y recibido.");
        }

        Resena resena = resenaRepository.save(Resena.builder()
                .producto(producto).usuario(usuario)
                .calificacion(req.getCalificacion())
                .comentario(req.getComentario() == null || req.getComentario().isBlank() ? null : req.getComentario().trim())
                .build());
        return toView(resena);
    }

    /** ¿Este renglón de compra ya se puede calificar? (recibido si es físico, pagado si es servicio). */
    public boolean esElegible(DetallePedido d) {
        if (d.getProducto().getTipo() == Producto.Tipo.SERVICIO) {
            return List.of("PAGADO", "ENVIADO", "ENTREGADO").contains(d.getPedido().getEstatus());
        }
        return envioRepository.findByDetalle_Id(d.getId())
                .map(Envio::getEstatus).map("ENTREGADO"::equals).orElse(false);
    }

    @Transactional(readOnly = true)
    public List<ResenaView> deProducto(Integer idProducto) {
        return resenaRepository.findByProducto_IdOrderByFechaDesc(idProducto).stream().map(this::toView).toList();
    }

    private ResenaView toView(Resena r) {
        return new ResenaView(r.getId(), r.getProducto().getId(), r.getUsuario().getNombre(),
                r.getCalificacion(), r.getComentario(), r.getFecha());
    }
}
