package com.jjm.ecommerce.service;

import com.jjm.ecommerce.model.DetallePedido;
import com.jjm.ecommerce.model.Pedido;
import com.jjm.ecommerce.repository.AliadoRepository;
import com.jjm.ecommerce.repository.DetallePedidoRepository;
import com.jjm.ecommerce.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reportes de ventas: general (dueño) y por vendedor. */
@Service
public class ReporteService {

    private static final Set<String> VENDIDOS = Set.of("PAGADO", "ENVIADO", "ENTREGADO");

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final AliadoRepository aliadoRepository;
    private final RankingService rankingService;

    public ReporteService(PedidoRepository pedidoRepository, DetallePedidoRepository detallePedidoRepository,
                          AliadoRepository aliadoRepository, RankingService rankingService) {
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.aliadoRepository = aliadoRepository;
        this.rankingService = rankingService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> reporteGeneralVentas() {
        List<Pedido> pedidos = pedidoRepository.findAll().stream()
                .filter(p -> VENDIDOS.contains(p.getEstatus())).toList();
        BigDecimal ventas = pedidos.stream().map(Pedido::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal envios = pedidos.stream().map(Pedido::getCostoEnvio).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal donado = pedidos.stream().map(Pedido::getMontoCausa).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal comision = pedidos.stream().flatMap(p -> p.getDetalles().stream())
                .map(DetallePedido::getMontoEmpresa).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("totalPedidosPagados", pedidos.size());
        r.put("totalVentas", ventas);
        r.put("totalEnvios", envios);
        r.put("totalDonadoACausas", donado);
        r.put("totalComisionJJM", comision);
        return r;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> reportePorAliado(Integer idAliado) {
        List<DetallePedido> detalles = detallePedidoRepository.findByAliado_Id(idAliado).stream()
                .filter(d -> VENDIDOS.contains(d.getPedido().getEstatus())).toList();
        BigDecimal ventas = detalles.stream()
                .map(d -> d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal recibido = detalles.stream().map(DetallePedido::getMontoAliado).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal donado = detalles.stream().map(DetallePedido::getMontoCausa).reduce(BigDecimal.ZERO, BigDecimal::add);
        long unidades = detalles.stream().mapToLong(DetallePedido::getCantidad).sum();

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("unidadesVendidas", unidades);
        r.put("ventasTotales", ventas);
        r.put("montoRecibido", recibido);
        r.put("montoDonado", donado);
        aliadoRepository.findById(idAliado).ifPresent(a -> r.put("ranking", rankingService.aliadoView(a)));
        return r;
    }
}
