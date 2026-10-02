package com.jjm.ecommerce.service;

import com.jjm.ecommerce.dto.Vistas.EnvioDistView;
import com.jjm.ecommerce.dto.Vistas.PaqueteriaView;
import com.jjm.ecommerce.model.DetallePedido;
import com.jjm.ecommerce.model.Envio;
import com.jjm.ecommerce.model.Paqueteria;
import com.jjm.ecommerce.model.Pedido;
import com.jjm.ecommerce.repository.EnvioRepository;
import com.jjm.ecommerce.repository.PaqueteriaRepository;
import com.jjm.ecommerce.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;

/**
 * Esquemas de mensajería (DHL, FedEx, Estafeta...). Cada producto habilita los suyos y el
 * cliente elige uno al comprar; el costo se suma al total y se genera la guía con su
 * tiempo de entrega. Costo = costo base + costo por kg × peso del producto × cantidad.
 *
 * Mientras JJM no contrate las APIs oficiales de cada paquetería, el número de guía se
 * genera localmente y el Distribuidor actualiza el estatus desde su panel. Para producción
 * sólo hay que sustituir generarNumeroGuia() por la llamada real a la paquetería.
 */
@Service
public class EnvioService {

    private final EnvioRepository envioRepository;
    private final PaqueteriaRepository paqueteriaRepository;
    private final PedidoRepository pedidoRepository;
    private final SecureRandom random = new SecureRandom();

    public EnvioService(EnvioRepository envioRepository, PaqueteriaRepository paqueteriaRepository,
                        PedidoRepository pedidoRepository) {
        this.envioRepository = envioRepository;
        this.paqueteriaRepository = paqueteriaRepository;
        this.pedidoRepository = pedidoRepository;
    }

    public List<PaqueteriaView> listarActivas() {
        return paqueteriaRepository.findByActivaTrue().stream()
                .map(q -> new PaqueteriaView(q.getId(), q.getNombre(), q.getTiempoEntregaDiasMin(),
                        q.getTiempoEntregaDiasMax(), q.getCostoBase(), q.getCostoPorKg()))
                .toList();
    }

    public static BigDecimal calcularCosto(Paqueteria p, BigDecimal pesoKg, int cantidad) {
        BigDecimal peso = pesoKg == null ? new BigDecimal("0.50") : pesoKg;
        return p.getCostoBase()
                .add(p.getCostoPorKg().multiply(peso).multiply(BigDecimal.valueOf(cantidad)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Envio generarEnvio(DetallePedido detalle, Paqueteria paqueteria, BigDecimal costo) {
        int dias = paqueteria.getTiempoEntregaDiasMax();
        return envioRepository.save(Envio.builder()
                .detalle(detalle)
                .paqueteria(paqueteria)
                .numeroGuia(generarNumeroGuia(paqueteria.getNombre()))
                .costoEnvio(costo)
                .tiempoEntregaEstimadoDias(dias)
                .fechaEstimadaEntrega(LocalDate.now().plusDays(dias))
                .estatus("PENDIENTE")
                .build());
    }

    // ------------------------------------------------------------ panel del Distribuidor
    @Transactional(readOnly = true)
    public List<EnvioDistView> listarParaDistribuidor(String estatus) {
        List<Envio> envios = (estatus == null || estatus.isBlank())
                ? envioRepository.findAllByOrderByFechaGeneracionDesc()
                : envioRepository.findByEstatusOrderByFechaGeneracionDesc(estatus.toUpperCase());
        return envios.stream().map(this::toDistView).toList();
    }

    @Transactional
    public EnvioDistView cambiarEstatus(Integer idEnvio, String nuevo) {
        Envio envio = envioRepository.findById(idEnvio)
                .orElseThrow(() -> new IllegalArgumentException("Envío no encontrado."));
        String destino = nuevo == null ? "" : nuevo.toUpperCase();
        boolean valido = ("PENDIENTE".equals(envio.getEstatus()) && "EN_TRANSITO".equals(destino))
                || ("EN_TRANSITO".equals(envio.getEstatus()) && "ENTREGADO".equals(destino));
        if (!valido) {
            throw new IllegalStateException("Transición no permitida: " + envio.getEstatus() + " → " + destino
                    + ". El orden es PENDIENTE → EN_TRANSITO → ENTREGADO.");
        }
        envio.setEstatus(destino);
        envioRepository.save(envio);

        // Sincroniza el estatus general del pedido con el de todas sus guías
        Pedido pedido = envio.getDetalle().getPedido();
        List<Envio> delPedido = envioRepository.findByDetalle_Pedido_Id(pedido.getId());
        boolean todosEntregados = delPedido.stream().allMatch(e -> "ENTREGADO".equals(e.getEstatus()));
        boolean algunoEnCamino = delPedido.stream().anyMatch(e -> !"PENDIENTE".equals(e.getEstatus()));
        if (todosEntregados) {
            pedido.setEstatus("ENTREGADO");
        } else if (algunoEnCamino) {
            pedido.setEstatus("ENVIADO");
        }
        pedidoRepository.save(pedido);
        return toDistView(envio);
    }

    private EnvioDistView toDistView(Envio e) {
        DetallePedido d = e.getDetalle();
        Pedido p = d.getPedido();
        return new EnvioDistView(e.getId(), p.getId(),
                p.getUsuario().getNombre() + " " + p.getUsuario().getApellidos(),
                (p.getDireccionEnvio() == null ? "" : p.getDireccionEnvio())
                        + (p.getCpEnvio() == null ? "" : " C.P. " + p.getCpEnvio()),
                d.getProducto().getNombre(), d.getCantidad(), d.getAliado().getNombreComercial(),
                e.getPaqueteria().getNombre(), e.getNumeroGuia(), e.getFechaEstimadaEntrega(), e.getEstatus());
    }

    private String generarNumeroGuia(String nombrePaqueteria) {
        String letras = nombrePaqueteria.replaceAll("[^A-Za-z]", "").toUpperCase();
        String prefijo = letras.length() >= 3 ? letras.substring(0, 3) : letras;
        StringBuilder sufijo = new StringBuilder();
        for (int i = 0; i < 10; i++) sufijo.append(random.nextInt(10));
        return prefijo + sufijo;
    }
}
