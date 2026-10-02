package com.jjm.ecommerce.service;

import com.jjm.ecommerce.dto.PromocionRequest;
import com.jjm.ecommerce.dto.Vistas.PromocionView;
import com.jjm.ecommerce.model.Aliado;
import com.jjm.ecommerce.model.Producto;
import com.jjm.ecommerce.model.Promocion;
import com.jjm.ecommerce.repository.AliadoRepository;
import com.jjm.ecommerce.repository.ProductoRepository;
import com.jjm.ecommerce.repository.PromocionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Campañas de marketing que lanza cada vendedor sobre sus propios productos:
 * temporada (Navidad, Día de Muertos, Fiestas Patrias, Buen Fin...) con 20/30/40 %
 * de descuento u otro, y opcionalmente dirigidas a una asociación civil o
 * fundación sin fines de lucro que recibe un porcentaje de donación de cada venta.
 * El cliente elige al comprar: costo normal o precio con promoción.
 */
@Service
public class PromocionService {

    private final PromocionRepository promocionRepository;
    private final ProductoRepository productoRepository;
    private final AliadoRepository aliadoRepository;

    public PromocionService(PromocionRepository promocionRepository, ProductoRepository productoRepository,
                            AliadoRepository aliadoRepository) {
        this.promocionRepository = promocionRepository;
        this.productoRepository = productoRepository;
        this.aliadoRepository = aliadoRepository;
    }

    @Transactional
    public PromocionView crear(Integer idUsuario, PromocionRequest req) {
        Aliado aliado = aliadoRepository.findByUsuario_Id(idUsuario)
                .orElseThrow(() -> new IllegalStateException("Tu cuenta no tiene un perfil de vendedor."));
        if (!"APROBADO".equals(aliado.getEstatusVerificacion())) {
            throw new IllegalStateException("Tu cuenta de vendedor aún está en verificación.");
        }
        BigDecimal pct = req.getPorcentajeDescuento();
        if (pct.compareTo(new BigDecimal("5")) < 0 || pct.compareTo(new BigDecimal("90")) > 0) {
            throw new IllegalArgumentException("El descuento debe estar entre 5% y 90%.");
        }
        if (req.getFechaFin().isBefore(req.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la de inicio.");
        }
        if (req.getIdProductos().isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos un producto para la campaña.");
        }

        boolean esCausa = Boolean.TRUE.equals(req.getEsCausaSocial());
        BigDecimal donacion = req.getPorcentajeDonacion() == null ? BigDecimal.ZERO : req.getPorcentajeDonacion();
        if (esCausa) {
            if (req.getFundacionBeneficiaria() == null || req.getFundacionBeneficiaria().isBlank()) {
                throw new IllegalArgumentException("Indica la asociación civil o fundación que recibirá el beneficio.");
            }
            if (donacion.compareTo(BigDecimal.ZERO) <= 0 || donacion.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("El porcentaje de donación debe ser mayor a 0 y máximo 100.");
            }
        } else {
            donacion = BigDecimal.ZERO;
        }

        Promocion.TipoTemporada temporada = Promocion.TipoTemporada.GENERAL;
        if (req.getTipoTemporada() != null && !req.getTipoTemporada().isBlank()) {
            try {
                temporada = Promocion.TipoTemporada.valueOf(req.getTipoTemporada().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Temporada inválida: " + req.getTipoTemporada());
            }
        }

        List<Producto> productos = new ArrayList<>();
        for (Integer idProducto : req.getIdProductos()) {
            Producto p = productoRepository.findById(idProducto)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + idProducto));
            if (!p.getAliado().getId().equals(aliado.getId())) {
                throw new IllegalStateException("Solo puedes aplicar campañas a tus propios productos.");
            }
            productos.add(p);
        }

        Promocion promocion = Promocion.builder()
                .aliado(aliado)
                .titulo(req.getTitulo().trim())
                .descripcion(req.getDescripcion())
                .porcentajeDescuento(pct)
                .tipoTemporada(temporada)
                .esCausaSocial(esCausa)
                .fundacionBeneficiaria(esCausa ? req.getFundacionBeneficiaria().trim() : null)
                .porcentajeDonacion(donacion)
                .fechaInicio(req.getFechaInicio())
                .fechaFin(req.getFechaFin())
                .productos(productos)
                .build();
        return toView(promocionRepository.save(promocion));
    }

    @Transactional(readOnly = true)
    public List<PromocionView> vigentes() {
        return promocionRepository.findVigentes(LocalDate.now()).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<PromocionView> mias(Integer idUsuario) {
        Aliado aliado = aliadoRepository.findByUsuario_Id(idUsuario)
                .orElseThrow(() -> new IllegalStateException("Tu cuenta no tiene un perfil de vendedor."));
        return promocionRepository.findByAliado_IdOrderByFechaFinDesc(aliado.getId()).stream().map(this::toView).toList();
    }

    @Transactional
    public PromocionView cambiarEstatus(Integer idUsuario, Integer idPromocion, String estatus) {
        Promocion promocion = promocionRepository.findById(idPromocion)
                .orElseThrow(() -> new IllegalArgumentException("Promoción no encontrada."));
        if (!promocion.getAliado().getUsuario().getId().equals(idUsuario)) {
            throw new IllegalStateException("Solo puedes modificar tus propias campañas.");
        }
        String nuevo = estatus == null ? "" : estatus.toUpperCase();
        if (!nuevo.equals("ACTIVA") && !nuevo.equals("PAUSADA") && !nuevo.equals("CANCELADA")) {
            throw new IllegalArgumentException("Estatus inválido (ACTIVA, PAUSADA o CANCELADA).");
        }
        promocion.setEstatus(nuevo);
        return toView(promocionRepository.save(promocion));
    }

    private PromocionView toView(Promocion p) {
        return new PromocionView(p.getId(), p.getTitulo(), p.getDescripcion(), p.getPorcentajeDescuento(),
                p.getTipoTemporada() == null ? "GENERAL" : p.getTipoTemporada().name(),
                Boolean.TRUE.equals(p.getEsCausaSocial()), p.getFundacionBeneficiaria(), p.getPorcentajeDonacion(),
                p.getFechaInicio(), p.getFechaFin(), p.getEstatus(), p.getAliado().getNombreComercial(),
                p.getProductos().stream().map(Producto::getId).toList());
    }
}
