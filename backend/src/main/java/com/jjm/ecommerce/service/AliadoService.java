package com.jjm.ecommerce.service;

import com.jjm.ecommerce.dto.CuentaCobroRequest;
import com.jjm.ecommerce.dto.Vistas.AliadoView;
import com.jjm.ecommerce.dto.Vistas.CuentaCobroView;
import com.jjm.ecommerce.dto.Vistas.PagoRecibidoView;
import com.jjm.ecommerce.model.Aliado;
import com.jjm.ecommerce.model.CuentaCobroAliado;
import com.jjm.ecommerce.model.Dispersion;
import com.jjm.ecommerce.repository.AliadoRepository;
import com.jjm.ecommerce.repository.CuentaCobroAliadoRepository;
import com.jjm.ecommerce.repository.DispersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Datos del propio vendedor: perfil, estatus de verificación, reporte de ventas y pagos/cobros. */
@Service
public class AliadoService {

    private final AliadoRepository aliadoRepository;
    private final RankingService rankingService;
    private final ReporteService reporteService;
    private final CuentaCobroAliadoRepository cuentaCobroRepository;
    private final DispersionRepository dispersionRepository;

    public AliadoService(AliadoRepository aliadoRepository, RankingService rankingService,
                         ReporteService reporteService, CuentaCobroAliadoRepository cuentaCobroRepository,
                         DispersionRepository dispersionRepository) {
        this.aliadoRepository = aliadoRepository;
        this.rankingService = rankingService;
        this.reporteService = reporteService;
        this.cuentaCobroRepository = cuentaCobroRepository;
        this.dispersionRepository = dispersionRepository;
    }

    private Aliado delUsuario(Integer idUsuario) {
        return aliadoRepository.findByUsuario_Id(idUsuario)
                .orElseThrow(() -> new IllegalStateException("Tu cuenta no tiene un perfil de vendedor."));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> perfil(Integer idUsuario) {
        Aliado a = delUsuario(idUsuario);
        AliadoView v = rankingService.aliadoView(a);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("nombreComercial", a.getNombreComercial());
        m.put("causaSocial", a.getCausaSocial());
        m.put("estatusVerificacion", a.getEstatusVerificacion());
        m.put("porcentajeConvenio", a.getPorcentajeConvenio());
        m.put("ranking", v);
        return m;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> reporte(Integer idUsuario) {
        return reporteService.reportePorAliado(delUsuario(idUsuario).getId());
    }

    // ------------------------------------------------------------ Punto 10: Pagos y Cobros
    @Transactional
    public CuentaCobroView guardarCuentaCobro(Integer idUsuario, CuentaCobroRequest req) {
        Aliado aliado = delUsuario(idUsuario);
        String numero = req.getNumeroCompleto().replaceAll("[^0-9]", "");
        if (numero.length() < 4) {
            throw new IllegalArgumentException("El número no es válido.");
        }
        String enmascarado = "•••• •••• •••• " + numero.substring(numero.length() - 4);
        String tipo = req.getTipoCuenta().trim().toUpperCase();
        if (!tipo.equals("CLABE") && !tipo.equals("TARJETA")) {
            throw new IllegalArgumentException("El tipo de cuenta debe ser CLABE o TARJETA.");
        }

        CuentaCobroAliado cuenta = cuentaCobroRepository.findByAliado_Id(aliado.getId())
                .orElse(CuentaCobroAliado.builder().aliado(aliado).build());
        cuenta.setTitular(req.getTitular().trim());
        cuenta.setBanco(req.getBanco().trim());
        cuenta.setTipoCuenta(tipo);
        cuenta.setNumeroEnmascarado(enmascarado);
        return toView(cuentaCobroRepository.save(cuenta));
    }

    @Transactional(readOnly = true)
    public CuentaCobroView miCuentaCobro(Integer idUsuario) {
        Aliado aliado = delUsuario(idUsuario);
        return cuentaCobroRepository.findByAliado_Id(aliado.getId()).map(this::toView).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<PagoRecibidoView> misPagos(Integer idUsuario) {
        Aliado aliado = delUsuario(idUsuario);
        return dispersionRepository.findByAliado_IdOrderByFechaDispersionDesc(aliado.getId()).stream()
                .map(d -> new PagoRecibidoView(d.getId(), d.getPago().getPedido().getId(), d.getFechaDispersion(),
                        d.getMontoAliado(), d.getMontoCausa(), d.getEstatus()))
                .toList();
    }

    private CuentaCobroView toView(CuentaCobroAliado c) {
        return new CuentaCobroView(c.getTitular(), c.getBanco(), c.getTipoCuenta(),
                c.getNumeroEnmascarado(), c.getFechaActualizacion());
    }
}
