package com.jjm.ecommerce.service;

import com.jjm.ecommerce.dto.MediaProductoRequest;
import com.jjm.ecommerce.dto.ProductoRequest;
import com.jjm.ecommerce.dto.Vistas.*;
import com.jjm.ecommerce.model.*;
import com.jjm.ecommerce.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Publicación y consulta de productos/servicios.
 * Reglas: la galería lleva de 5 a 7 elementos (imágenes y/o videos de hasta 15 s,
 * con o sin audio) y cada producto habilita sus propios esquemas de mensajería.
 */
@Service
public class ProductoService {

    public static final int MIN_MEDIA = 3;
    public static final int MAX_MEDIA = 8;
    public static final int MAX_VIDEOS = 5;
    public static final int MAX_SEGUNDOS_VIDEO = 15;

    private final ProductoRepository productoRepository;
    private final AliadoRepository aliadoRepository;
    private final CategoriaRepository categoriaRepository;
    private final PaqueteriaRepository paqueteriaRepository;
    private final PromocionRepository promocionRepository;
    private final IaVerificacionService iaVerificacionService;
    private final RankingService rankingService;
    private final com.jjm.ecommerce.repository.DetallePedidoRepository detallePedidoRepository;

    public ProductoService(ProductoRepository productoRepository, AliadoRepository aliadoRepository,
                           CategoriaRepository categoriaRepository, PaqueteriaRepository paqueteriaRepository,
                           PromocionRepository promocionRepository, IaVerificacionService iaVerificacionService,
                           RankingService rankingService, com.jjm.ecommerce.repository.DetallePedidoRepository detallePedidoRepository) {
        this.productoRepository = productoRepository;
        this.aliadoRepository = aliadoRepository;
        this.categoriaRepository = categoriaRepository;
        this.paqueteriaRepository = paqueteriaRepository;
        this.promocionRepository = promocionRepository;
        this.iaVerificacionService = iaVerificacionService;
        this.rankingService = rankingService;
        this.detallePedidoRepository = detallePedidoRepository;
    }

    /** Precio con el descuento aplicado, redondeado a centavos. */
    public static BigDecimal precioConDescuento(BigDecimal precio, BigDecimal porcentaje) {
        return precio.multiply(BigDecimal.valueOf(100).subtract(porcentaje))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ publicar
    @Transactional
    public ProductoView publicar(Integer idUsuario, ProductoRequest req) {
        Aliado aliado = aliadoRepository.findByUsuario_Id(idUsuario)
                .orElseThrow(() -> new IllegalStateException("Tu cuenta no tiene un perfil de vendedor."));
        if (!"APROBADO".equals(aliado.getEstatusVerificacion())) {
            throw new IllegalStateException("Tu cuenta de vendedor aún está en verificación; el administrador debe aprobarla para publicar.");
        }
        Categoria categoria = categoriaRepository.findById(req.getIdCategoria())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada."));

        Producto.Tipo tipo;
        try {
            tipo = Producto.Tipo.valueOf(req.getTipo().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El tipo debe ser PRODUCTO o SERVICIO.");
        }
        if (req.getPrecio() == null || req.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero.");
        }
        validarGaleria(req.getMedia());

        boolean autentico = iaVerificacionService.verificarAutenticidadProducto(req.getNombre(), req.getDescripcion());

        Producto producto = Producto.builder()
                .aliado(aliado).categoria(categoria).tipo(tipo)
                .nombre(req.getNombre().trim())
                .descripcion(req.getDescripcion())
                .precio(req.getPrecio())
                .pesoKg(req.getPesoKg() != null && req.getPesoKg().compareTo(BigDecimal.ZERO) > 0
                        ? req.getPesoKg() : new BigDecimal("0.50"))
                .existencia(req.getExistencia() == null ? 0 : req.getExistencia())
                .verificadoIa(autentico)
                .estatus(autentico ? "ACTIVO" : "PAUSADO")
                .build();

        int orden = 0;
        for (MediaProductoRequest m : req.getMedia()) {
            boolean video = "VIDEO".equalsIgnoreCase(m.getTipo());
            producto.getImagenes().add(ProductoImagen.builder()
                    .producto(producto)
                    .tipo(video ? ProductoImagen.Tipo.VIDEO : ProductoImagen.Tipo.IMAGEN)
                    .urlImagen(m.getUrl().trim())
                    .duracionSegundos(video ? m.getDuracionSegundos() : null)
                    .conAudio(video ? Boolean.TRUE.equals(m.getConAudio()) : null)
                    .orden(orden++)
                    .build());
        }

        if (tipo == Producto.Tipo.PRODUCTO) {   // los servicios no llevan mensajería
            Set<Paqueteria> elegidas = new LinkedHashSet<>();
            if (req.getIdsPaqueterias() == null || req.getIdsPaqueterias().isEmpty()) {
                elegidas.addAll(paqueteriaRepository.findByActivaTrue());
            } else {
                for (Integer id : req.getIdsPaqueterias()) {
                    Paqueteria p = paqueteriaRepository.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("Paquetería no encontrada: " + id));
                    if (!Boolean.TRUE.equals(p.getActiva())) {
                        throw new IllegalArgumentException("La paquetería " + p.getNombre() + " no está disponible.");
                    }
                    elegidas.add(p);
                }
            }
            if (elegidas.isEmpty()) {
                throw new IllegalStateException("No hay paqueterías activas configuradas.");
            }
            producto.setPaqueterias(elegidas);
        }
        return toView(productoRepository.save(producto));
    }

    private void validarGaleria(List<MediaProductoRequest> media) {
        if (media == null || media.size() < MIN_MEDIA) {
            throw new IllegalArgumentException("Sube mínimo " + MIN_MEDIA + " archivos en la galería "
                    + "(pueden ser fotos, videos de hasta " + MAX_SEGUNDOS_VIDEO + " segundos, o una mezcla de ambos). Recibimos "
                    + (media == null ? 0 : media.size()) + ".");
        }
        if (media.size() > MAX_MEDIA) {
            throw new IllegalArgumentException("Máximo " + MAX_MEDIA + " archivos por producto.");
        }
        int videos = 0;
        for (MediaProductoRequest m : media) {
            if (m.getUrl() == null || m.getUrl().isBlank()) {
                throw new IllegalArgumentException("Hay un archivo de la galería sin URL.");
            }
            if ("VIDEO".equalsIgnoreCase(m.getTipo())) {
                videos++;
                if (m.getDuracionSegundos() == null || m.getDuracionSegundos() < 1
                        || m.getDuracionSegundos() > MAX_SEGUNDOS_VIDEO) {
                    throw new IllegalArgumentException("Cada video debe durar máximo " + MAX_SEGUNDOS_VIDEO + " segundos.");
                }
            } else if (!"IMAGEN".equalsIgnoreCase(m.getTipo())) {
                throw new IllegalArgumentException("Tipo de archivo inválido (usa IMAGEN o VIDEO).");
            }
        }
        if (videos > MAX_VIDEOS) {
            throw new IllegalArgumentException("Máximo " + MAX_VIDEOS + " videos por producto.");
        }
    }

    // ------------------------------------------------------------------ consulta
    @Transactional(readOnly = true)
    public List<ProductoView> catalogo(String buscar, Integer idCategoria) {
        List<Producto> lista;
        if (buscar != null && !buscar.isBlank()) {
            lista = productoRepository.buscarPorTexto(buscar.trim());
        } else if (idCategoria != null) {
            lista = productoRepository.findByCategoria_IdAndEstatus(idCategoria, "ACTIVO");
        } else {
            lista = productoRepository.findByEstatus("ACTIVO");
        }
        return lista.stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public ProductoView detalle(Integer idProducto) {
        Producto p = productoRepository.findById(idProducto)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));
        if (!"ACTIVO".equals(p.getEstatus())) {
            throw new IllegalArgumentException("Este producto no está disponible por ahora.");
        }
        return toView(p);
    }

    @Transactional(readOnly = true)
    public List<ProductoView> misProductos(Integer idUsuario) {
        Aliado aliado = aliadoRepository.findByUsuario_Id(idUsuario)
                .orElseThrow(() -> new IllegalStateException("Tu cuenta no tiene un perfil de vendedor."));
        return productoRepository.findByAliado_Id(aliado.getId()).stream()
                .filter(p -> !"ELIMINADO".equals(p.getEstatus()))
                .map(this::toView).toList();
    }

    /** Lista de todos los productos/servicios activos para el panel del Administrador. */
    @Transactional(readOnly = true)
    public List<ProductoView> todosParaAdmin() {
        return productoRepository.findByEstatusNot("ELIMINADO").stream().map(this::toView).toList();
    }

    @Transactional
    public ProductoView cambiarEstatus(Usuario usuario, Integer idProducto, String estatus) {
        Producto p = productoRepository.findById(idProducto)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));
        boolean esDueno = p.getAliado().getUsuario().getId().equals(usuario.getId());
        if (!esDueno && !"ADMIN".equals(usuario.getRol().getNombre())) {
            throw new IllegalStateException("Solo puedes modificar tus propios productos.");
        }
        String nuevo = estatus == null ? "" : estatus.toUpperCase();
        if (!nuevo.equals("ACTIVO") && !nuevo.equals("PAUSADO")) {
            throw new IllegalArgumentException("El estatus debe ser ACTIVO o PAUSADO.");
        }
        p.setEstatus(nuevo);
        return toView(productoRepository.save(p));
    }

    /**
     * Elimina un producto/servicio (botón "Eliminar" del Vendedor o del Administrador).
     * Si el producto nunca se vendió, se borra de verdad de la base de datos. Si ya tiene
     * compras registradas, no se puede borrar de verdad sin romper el historial de esos
     * pedidos (facturas, reportes, envíos ya hechos) — en ese caso se marca como ELIMINADO:
     * desaparece del catálogo, del panel del vendedor y del panel del admin, pero el pedido
     * de quien ya lo compró sigue siendo consultable con normalidad.
     */
    @Transactional
    public void eliminar(Usuario usuario, Integer idProducto) {
        Producto p = productoRepository.findById(idProducto)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));
        boolean esDueno = p.getAliado().getUsuario().getId().equals(usuario.getId());
        if (!esDueno && !"ADMIN".equals(usuario.getRol().getNombre())) {
            throw new IllegalStateException("Solo puedes eliminar tus propios productos.");
        }
        if (detallePedidoRepository.existsByProducto_Id(idProducto)) {
            p.setEstatus("ELIMINADO");
            productoRepository.save(p);
        } else {
            productoRepository.delete(p);
        }
    }

    /**
     * Modifica un producto/servicio ya publicado (botón "Modificar Datos", sólo del Vendedor).
     * Vuelve a validar todo igual que al publicarlo (categoría, precio, galería mínimo 3
     * archivos, paqueterías para productos físicos).
     */
    @Transactional
    public ProductoView actualizar(Integer idUsuario, Integer idProducto, ProductoRequest req) {
        Producto p = productoRepository.findById(idProducto)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));
        if (!p.getAliado().getUsuario().getId().equals(idUsuario)) {
            throw new IllegalStateException("Solo puedes modificar tus propios productos.");
        }
        Categoria categoria = categoriaRepository.findById(req.getIdCategoria())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada."));
        Producto.Tipo tipo;
        try {
            tipo = Producto.Tipo.valueOf(req.getTipo().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El tipo debe ser PRODUCTO o SERVICIO.");
        }
        if (req.getPrecio() == null || req.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero.");
        }
        validarGaleria(req.getMedia());

        p.setCategoria(categoria);
        p.setTipo(tipo);
        p.setNombre(req.getNombre().trim());
        p.setDescripcion(req.getDescripcion());
        p.setPrecio(req.getPrecio());
        p.setPesoKg(req.getPesoKg() != null && req.getPesoKg().compareTo(BigDecimal.ZERO) > 0
                ? req.getPesoKg() : new BigDecimal("0.50"));
        p.setExistencia(req.getExistencia() == null ? 0 : req.getExistencia());
        p.setVerificadoIa(iaVerificacionService.verificarAutenticidadProducto(req.getNombre(), req.getDescripcion()));

        p.getImagenes().clear();
        int orden = 0;
        for (MediaProductoRequest m : req.getMedia()) {
            boolean video = "VIDEO".equalsIgnoreCase(m.getTipo());
            p.getImagenes().add(ProductoImagen.builder()
                    .producto(p)
                    .tipo(video ? ProductoImagen.Tipo.VIDEO : ProductoImagen.Tipo.IMAGEN)
                    .urlImagen(m.getUrl().trim())
                    .duracionSegundos(video ? m.getDuracionSegundos() : null)
                    .conAudio(video ? Boolean.TRUE.equals(m.getConAudio()) : null)
                    .orden(orden++)
                    .build());
        }

        if (tipo == Producto.Tipo.PRODUCTO) {
            Set<Paqueteria> elegidas = new LinkedHashSet<>();
            if (req.getIdsPaqueterias() == null || req.getIdsPaqueterias().isEmpty()) {
                elegidas.addAll(paqueteriaRepository.findByActivaTrue());
            } else {
                for (Integer id : req.getIdsPaqueterias()) {
                    Paqueteria q = paqueteriaRepository.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("Paquetería no encontrada: " + id));
                    if (!Boolean.TRUE.equals(q.getActiva())) {
                        throw new IllegalArgumentException("La paquetería " + q.getNombre() + " no está disponible.");
                    }
                    elegidas.add(q);
                }
            }
            if (elegidas.isEmpty()) {
                throw new IllegalStateException("No hay paqueterías activas configuradas.");
            }
            p.setPaqueterias(elegidas);
        } else {
            p.getPaqueterias().clear();
        }

        return toView(productoRepository.save(p));
    }

    // ------------------------------------------------------------------ vistas
    /** Promoción vigente de mayor descuento para un producto (o null). */
    public Promocion promocionVigente(Integer idProducto) {
        return promocionRepository.findVigentesPorProducto(idProducto, LocalDate.now())
                .stream().findFirst().orElse(null);
    }

    public ProductoView toView(Producto p) {
        Promocion promo = promocionVigente(p.getId());
        PromocionResumen resumen = null;
        if (promo != null) {
            resumen = new PromocionResumen(promo.getId(), promo.getTitulo(), promo.getPorcentajeDescuento(),
                    promo.getTipoTemporada() == null ? "GENERAL" : promo.getTipoTemporada().name(),
                    Boolean.TRUE.equals(promo.getEsCausaSocial()), promo.getFundacionBeneficiaria(),
                    promo.getPorcentajeDonacion(), promo.getFechaFin(),
                    precioConDescuento(p.getPrecio(), promo.getPorcentajeDescuento()));
        }
        List<MediaView> media = p.getImagenes().stream()
                .map(i -> new MediaView(i.getTipo().name(), MediaUrls.absoluta(i.getUrlImagen()),
                        i.getDuracionSegundos(), i.getConAudio()))
                .toList();
        List<PaqueteriaView> paqueterias = p.getPaqueterias().stream()
                .map(q -> new PaqueteriaView(q.getId(), q.getNombre(), q.getTiempoEntregaDiasMin(),
                        q.getTiempoEntregaDiasMax(), q.getCostoBase(), q.getCostoPorKg()))
                .toList();
        return new ProductoView(p.getId(), p.getTipo().name(), p.getNombre(), p.getDescripcion(), p.getPrecio(),
                p.getPesoKg(), p.getExistencia(), Boolean.TRUE.equals(p.getVerificadoIa()), p.getEstatus(),
                p.getCategoria().getId(), p.getCategoria().getNombre(), rankingService.aliadoView(p.getAliado()),
                media, paqueterias, resumen);
    }
}
