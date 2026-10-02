package com.jjm.ecommerce.service;

import com.jjm.ecommerce.dto.FacturaRequest;
import com.jjm.ecommerce.dto.Vistas.FacturaView;
import com.jjm.ecommerce.model.Factura;
import com.jjm.ecommerce.model.Pedido;
import com.jjm.ecommerce.model.Usuario;
import com.jjm.ecommerce.repository.FacturaRepository;
import com.jjm.ecommerce.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;

/**
 * Solicitud de factura de un pedido pagado.
 *
 * IMPORTANTE: una factura con validez fiscal (CFDI 4.0 timbrado ante el SAT) requiere contratar un
 * PAC (Facturama, SW Sapien, FacturAPI, etc.) con los certificados (CSD) de la empresa emisora.
 * Este servicio guarda toda la solicitud con folio interno y desglose de IVA en estatus
 * PENDIENTE_TIMBRADO; para producción sólo hay que sustituir el cuerpo de emitir() por la llamada
 * al PAC y guardar el UUID fiscal devuelto (estatus TIMBRADA).
 */
@Service
public class FacturaService {

    private static final Set<String> ESTATUS_FACTURABLES = Set.of("PAGADO", "ENVIADO", "ENTREGADO");
    private static final String REGEX_RFC = "^[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3}$";

    private final FacturaRepository facturaRepository;
    private final PedidoRepository pedidoRepository;
    private final SecureRandom random = new SecureRandom();

    public FacturaService(FacturaRepository facturaRepository, PedidoRepository pedidoRepository) {
        this.facturaRepository = facturaRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional
    public FacturaView emitir(Usuario usuario, FacturaRequest req) {
        Pedido pedido = pedidoRepository.findById(req.getIdPedido())
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));
        if (!pedido.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalStateException("Solo puedes facturar tus propios pedidos.");
        }
        if (!ESTATUS_FACTURABLES.contains(pedido.getEstatus())) {
            throw new IllegalStateException("Solo se pueden facturar pedidos ya pagados.");
        }
        if (facturaRepository.findByPedido_Id(pedido.getId()).isPresent()) {
            throw new IllegalStateException("Este pedido ya tiene una factura solicitada.");
        }
        String rfc = req.getRfcReceptor().trim().toUpperCase();
        if (!rfc.matches(REGEX_RFC)) {
            throw new IllegalArgumentException("El RFC no tiene un formato válido (ej. XAXX010101000).");
        }
        String cp = req.getCpFiscal() == null ? "" : req.getCpFiscal().trim();
        if (!cp.matches("\\d{5}")) {
            throw new IllegalArgumentException("El código postal fiscal debe tener 5 dígitos.");
        }
        if (req.getRegimenFiscal() == null || req.getRegimenFiscal().isBlank()) {
            throw new IllegalArgumentException("Indica tu régimen fiscal (ej. 601, 612, 626).");
        }

        // Los precios ya incluyen IVA (16%): se desglosa a partir del total cobrado.
        BigDecimal total = pedido.getTotal();
        BigDecimal subtotal = total.divide(new BigDecimal("1.16"), 2, RoundingMode.HALF_UP);
        BigDecimal iva = total.subtract(subtotal);

        Factura factura = Factura.builder()
                .pedido(pedido).usuario(usuario)
                .rfcReceptor(rfc).razonSocial(req.getRazonSocial().trim())
                .usoCfdi(req.getUsoCfdi() == null || req.getUsoCfdi().isBlank() ? "G03" : req.getUsoCfdi().trim())
                .regimenFiscal(req.getRegimenFiscal().trim()).cpFiscal(cp)
                .correoEnvio(req.getCorreoEnvio() == null || req.getCorreoEnvio().isBlank()
                        ? usuario.getCorreo() : req.getCorreoEnvio().trim())
                .folioInterno("JJM-" + pedido.getId() + "-" + (100000 + random.nextInt(900000)))
                .subtotal(subtotal).iva(iva).total(total)
                .estatus("PENDIENTE_TIMBRADO")
                .build();
        // TODO PAC: timbrar aquí y guardar uuidFiscal + estatus TIMBRADA.
        return toView(facturaRepository.save(factura));
    }

    @Transactional(readOnly = true)
    public List<FacturaView> mias(Integer idUsuario) {
        return facturaRepository.findByUsuario_Id(idUsuario).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public FacturaView porPedido(Usuario usuario, Integer idPedido) {
        Factura f = facturaRepository.findByPedido_Id(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Este pedido aún no tiene factura solicitada."));
        if (!f.getUsuario().getId().equals(usuario.getId()) && !"ADMIN".equals(usuario.getRol().getNombre())) {
            throw new IllegalStateException("No tienes acceso a esta factura.");
        }
        return toView(f);
    }

    private FacturaView toView(Factura f) {
        return new FacturaView(f.getId(), f.getPedido().getId(), f.getFolioInterno(), f.getRfcReceptor(),
                f.getRazonSocial(), f.getRegimenFiscal(), f.getUsoCfdi(), f.getCpFiscal(), f.getCorreoEnvio(),
                f.getSubtotal(), f.getIva(), f.getTotal(), f.getEstatus(), f.getFechaEmision());
    }
}
