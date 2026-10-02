package com.jjm.ecommerce.service;

import com.jjm.ecommerce.dto.CarritoItemRequest;
import com.jjm.ecommerce.dto.CrearPedidoRequest;
import com.jjm.ecommerce.dto.Vistas.DetalleView;
import com.jjm.ecommerce.dto.Vistas.EnvioView;
import com.jjm.ecommerce.dto.Vistas.PedidoView;
import com.jjm.ecommerce.model.*;
import com.jjm.ecommerce.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Núcleo transaccional: arma el pedido con la modalidad elegida en cada producto (costo normal o
 * promoción/causa), suma el envío de la paquetería elegida, cobra, genera las guías y reparte los
 * fondos (vendedor / JJM por convenio / donación a la fundación cuando la campaña es de causa social).
 */
@Service
public class PedidoService {

    private final ProductoRepository productoRepository;
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final PagoRepository pagoRepository;
    private final DispersionRepository dispersionRepository;
    private final EnvioRepository envioRepository;
    private final FacturaRepository facturaRepository;
    private final CalificacionVendedorRepository calificacionRepository;
    private final ResenaRepository resenaRepository;
    private final PasarelaPagoService pasarelaPagoService;
    private final CorreoService correoService;
    private final EnvioService envioService;
    private final ProductoService productoService;

    public PedidoService(ProductoRepository productoRepository, PedidoRepository pedidoRepository,
                         UsuarioRepository usuarioRepository, MetodoPagoRepository metodoPagoRepository,
                         PagoRepository pagoRepository, DispersionRepository dispersionRepository,
                         EnvioRepository envioRepository, FacturaRepository facturaRepository,
                         CalificacionVendedorRepository calificacionRepository, ResenaRepository resenaRepository,
                         PasarelaPagoService pasarelaPagoService, CorreoService correoService,
                         EnvioService envioService, ProductoService productoService) {
        this.productoRepository = productoRepository;
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.metodoPagoRepository = metodoPagoRepository;
        this.pagoRepository = pagoRepository;
        this.dispersionRepository = dispersionRepository;
        this.envioRepository = envioRepository;
        this.facturaRepository = facturaRepository;
        this.calificacionRepository = calificacionRepository;
        this.resenaRepository = resenaRepository;
        this.pasarelaPagoService = pasarelaPagoService;
        this.correoService = correoService;
        this.envioService = envioService;
        this.productoService = productoService;
    }

    @Transactional
    public PedidoView crearYPagarPedido(Integer idUsuario, CrearPedidoRequest req) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("Tu carrito está vacío.");
        }
        if (req.getMetodoPago() == null || req.getMetodoPago().isBlank()) {
            throw new IllegalArgumentException("Elige un método de pago.");
        }
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        MetodoPago metodo = metodoPagoRepository.findByNombre(req.getMetodoPago().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Método de pago no soportado."));

        Pedido pedido = Pedido.builder()
                .usuario(usuario)
                .subtotal(BigDecimal.ZERO).costoEnvio(BigDecimal.ZERO)
                .montoCausa(BigDecimal.ZERO).total(BigDecimal.ZERO)
                .direccionEnvio(req.getDireccionEnvio() == null ? null : req.getDireccionEnvio().trim())
                .cpEnvio(req.getCpEnvio() == null ? null : req.getCpEnvio().trim())
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal envioTotal = BigDecimal.ZERO;
        BigDecimal causaTotal = BigDecimal.ZERO;
        boolean hayFisicos = false;
        List<Paqueteria> paqueteriasPorDetalle = new ArrayList<>();
        List<BigDecimal> costosPorDetalle = new ArrayList<>();

        for (CarritoItemRequest item : req.getItems()) {
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + item.getIdProducto()));
            int cantidad = item.getCantidad() == null ? 0 : item.getCantidad();
            if (cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad de '" + producto.getNombre() + "' debe ser mayor a cero.");
            }
            if (!"ACTIVO".equals(producto.getEstatus())) {
                throw new IllegalStateException("'" + producto.getNombre() + "' ya no está disponible.");
            }
            boolean fisico = producto.getTipo() == Producto.Tipo.PRODUCTO;
            if (fisico && (producto.getExistencia() == null || producto.getExistencia() < cantidad)) {
                throw new IllegalStateException("Existencia insuficiente para '" + producto.getNombre() + "'.");
            }

            // --- Modalidad: costo normal o promoción / causa
            String modalidad = item.getModalidad() == null ? "NORMAL" : item.getModalidad().trim().toUpperCase();
            Promocion promo = null;
            BigDecimal descuento = BigDecimal.ZERO;
            BigDecimal precioCobrado = producto.getPrecio();
            if ("PROMOCION".equals(modalidad)) {
                promo = productoService.promocionVigente(producto.getId());
                if (promo == null) {
                    throw new IllegalStateException("La promoción de '" + producto.getNombre() + "' ya no está vigente.");
                }
                descuento = promo.getPorcentajeDescuento();
                precioCobrado = ProductoService.precioConDescuento(producto.getPrecio(), descuento);
            } else if (!"NORMAL".equals(modalidad)) {
                throw new IllegalArgumentException("Modalidad inválida (NORMAL o PROMOCION).");
            }

            // --- Mensajería (solo productos físicos)
            Paqueteria paqueteria = null;
            BigDecimal costoEnvio = BigDecimal.ZERO;
            if (fisico) {
                hayFisicos = true;
                if (item.getIdPaqueteria() == null) {
                    throw new IllegalArgumentException("Elige una paquetería para '" + producto.getNombre() + "'.");
                }
                paqueteria = producto.getPaqueterias().stream()
                        .filter(q -> q.getId().equals(item.getIdPaqueteria()) && Boolean.TRUE.equals(q.getActiva()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Ese esquema de mensajería no está habilitado para '" + producto.getNombre() + "'."));
                costoEnvio = EnvioService.calcularCosto(paqueteria, producto.getPesoKg(), cantidad);
            }

            // --- Reparto de fondos
            BigDecimal importe = precioCobrado.multiply(BigDecimal.valueOf(cantidad));
            BigDecimal convenio = producto.getAliado().getPorcentajeConvenio();
            BigDecimal montoEmpresa = importe.multiply(convenio)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal montoAliado = importe.subtract(montoEmpresa);
            BigDecimal montoCausa = BigDecimal.ZERO;
            if (promo != null && Boolean.TRUE.equals(promo.getEsCausaSocial())
                    && promo.getPorcentajeDonacion().compareTo(BigDecimal.ZERO) > 0) {
                montoCausa = importe.multiply(promo.getPorcentajeDonacion())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).min(montoAliado);
                montoAliado = montoAliado.subtract(montoCausa);   // la donación sale de la parte del vendedor
            }

            pedido.getDetalles().add(DetallePedido.builder()
                    .pedido(pedido).producto(producto).aliado(producto.getAliado())
                    .cantidad(cantidad).modalidad(modalidad).promocion(promo)
                    .precioLista(producto.getPrecio()).precioUnitario(precioCobrado)
                    .porcentajeDescuento(descuento).porcentajeAplicado(convenio)
                    .montoAliado(montoAliado).montoEmpresa(montoEmpresa).montoCausa(montoCausa)
                    .build());
            paqueteriasPorDetalle.add(paqueteria);
            costosPorDetalle.add(costoEnvio);

            subtotal = subtotal.add(importe);
            envioTotal = envioTotal.add(costoEnvio);
            causaTotal = causaTotal.add(montoCausa);
            if (fisico) {
                producto.setExistencia(producto.getExistencia() - cantidad);
                productoRepository.save(producto);
            }
        }

        if (hayFisicos) {
            if (pedido.getDireccionEnvio() == null || pedido.getDireccionEnvio().length() < 10) {
                throw new IllegalArgumentException("Escribe la dirección de entrega completa (calle, número, colonia y ciudad).");
            }
            if (pedido.getCpEnvio() == null || !pedido.getCpEnvio().matches("\\d{5}")) {
                throw new IllegalArgumentException("El código postal debe tener 5 dígitos.");
            }
        }

        pedido.setSubtotal(subtotal);
        pedido.setCostoEnvio(envioTotal);
        pedido.setMontoCausa(causaTotal);
        pedido.setTotal(subtotal.add(envioTotal));
        pedido = pedidoRepository.save(pedido);

        PasarelaPagoService.ResultadoPago resultado = pasarelaPagoService.cobrar(req.getMetodoPago(), pedido.getTotal());
        // Punto 9: si se pagó con tarjeta, se anota la marca y los últimos 4 dígitos junto a la
        // referencia de la pasarela, sólo como dato de referencia (nunca se guarda el número completo).
        String referencia = resultado.referencia();
        if (req.getUltimos4Tarjeta() != null && !req.getUltimos4Tarjeta().isBlank()) {
            referencia = (referencia == null ? "" : referencia + " · ")
                    + (req.getMarcaTarjeta() == null ? "Tarjeta" : req.getMarcaTarjeta())
                    + " terminación " + req.getUltimos4Tarjeta();
        }
        Pago pago = pagoRepository.save(Pago.builder()
                .pedido(pedido).metodo(metodo)
                .referenciaPasarela(referencia)
                .monto(pedido.getTotal()).estatus(resultado.estatus())
                .build());

        if (resultado.exitoso()) {
            pedido.setEstatus("PAGADO");
            List<DetallePedido> detalles = pedido.getDetalles();
            for (int i = 0; i < detalles.size(); i++) {
                DetallePedido d = detalles.get(i);
                dispersionRepository.save(Dispersion.builder()
                        .pago(pago).aliado(d.getAliado())
                        .montoAliado(d.getMontoAliado()).montoEmpresa(d.getMontoEmpresa())
                        .estatus("LIBERADO").fechaDispersion(LocalDateTime.now())
                        .build());
                if (paqueteriasPorDetalle.get(i) != null) {
                    envioService.generarEnvio(d, paqueteriasPorDetalle.get(i), costosPorDetalle.get(i));
                }
            }
            correoService.notificarPedidoConfirmado(usuario.getCorreo(), pedido.getId());
        } else {
            pedido.setEstatus("CANCELADO");
            for (DetallePedido d : pedido.getDetalles()) {           // devuelve el inventario
                if (d.getProducto().getTipo() == Producto.Tipo.PRODUCTO) {
                    d.getProducto().setExistencia(d.getProducto().getExistencia() + d.getCantidad());
                    productoRepository.save(d.getProducto());
                }
            }
        }
        return toView(pedidoRepository.save(pedido), usuario.getId());
    }

    @Transactional(readOnly = true)
    public List<PedidoView> historial(Integer idUsuario) {
        return pedidoRepository.findByUsuario_IdOrderByFechaPedidoDesc(idUsuario).stream()
                .map(p -> toView(p, idUsuario)).toList();
    }

    @Transactional(readOnly = true)
    public PedidoView detalle(Usuario usuario, Integer idPedido) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));
        boolean esDueno = pedido.getUsuario().getId().equals(usuario.getId());
        if (!esDueno && !"ADMIN".equals(usuario.getRol().getNombre())) {
            throw new AccessDeniedException("No tienes acceso a este pedido.");
        }
        return toView(pedido, pedido.getUsuario().getId());
    }

    private PedidoView toView(Pedido p, Integer idCliente) {
        Map<Integer, Envio> enviosPorDetalle = new HashMap<>();
        for (Envio e : envioRepository.findByDetalle_Pedido_Id(p.getId())) {
            enviosPorDetalle.put(e.getDetalle().getId(), e);
        }
        List<DetalleView> detalles = p.getDetalles().stream().map(d -> {
            Envio e = enviosPorDetalle.get(d.getId());
            EnvioView envio = e == null ? null : new EnvioView(e.getId(), e.getPaqueteria().getNombre(),
                    e.getNumeroGuia(), e.getCostoEnvio(), e.getTiempoEntregaEstimadoDias(),
                    e.getFechaEstimadaEntrega(), e.getEstatus());
            String imagen = d.getProducto().getImagenes().stream()
                    .filter(i -> i.getTipo() == ProductoImagen.Tipo.IMAGEN)
                    .map(i -> MediaUrls.absoluta(i.getUrlImagen())).findFirst().orElse(null);
            String fundacion = d.getPromocion() == null ? null : d.getPromocion().getFundacionBeneficiaria();
            // Punto 8: sólo se puede calificar (vendedor y producto) cuando el cliente ya lo recibió
            // — si es un producto físico, su guía debe estar ENTREGADO; si es un servicio (sin envío),
            // basta con que el pedido esté pagado.
            boolean puedeCalificar = d.getProducto().getTipo() == Producto.Tipo.SERVICIO
                    ? List.of("PAGADO", "ENVIADO", "ENTREGADO").contains(p.getEstatus())
                    : (e != null && "ENTREGADO".equals(e.getEstatus()));
            boolean calificadoVendedor = calificacionRepository.existsByAliado_IdAndUsuario_IdAndPedido_Id(
                    d.getAliado().getId(), idCliente, p.getId());
            boolean calificadoProducto = resenaRepository.existsByProducto_IdAndUsuario_Id(
                    d.getProducto().getId(), idCliente);
            return new DetalleView(d.getId(), d.getProducto().getId(), d.getProducto().getNombre(), imagen,
                    d.getCantidad(), d.getModalidad(), d.getPrecioLista(), d.getPrecioUnitario(),
                    d.getPorcentajeDescuento(), fundacion, d.getMontoCausa(), d.getAliado().getId(),
                    d.getAliado().getNombreComercial(),
                    d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())),
                    puedeCalificar, calificadoVendedor, calificadoProducto, envio);
        }).toList();
        return new PedidoView(p.getId(), p.getFechaPedido(), p.getEstatus(), p.getSubtotal(), p.getCostoEnvio(),
                p.getTotal(), p.getMontoCausa(), p.getDireccionEnvio(), p.getCpEnvio(),
                facturaRepository.findByPedido_Id(p.getId()).isPresent(), detalles);
    }
}
