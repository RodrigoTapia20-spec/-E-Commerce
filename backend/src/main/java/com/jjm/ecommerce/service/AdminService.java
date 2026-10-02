package com.jjm.ecommerce.service;

import com.jjm.ecommerce.dto.AdminCrearUsuarioRequest;
import com.jjm.ecommerce.dto.Vistas.AliadoAdminView;
import com.jjm.ecommerce.dto.Vistas.UsuarioView;
import com.jjm.ecommerce.model.Aliado;
import com.jjm.ecommerce.model.Producto;
import com.jjm.ecommerce.model.Rol;
import com.jjm.ecommerce.model.Usuario;
import com.jjm.ecommerce.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Gestión que solo el Administrador (dueño) puede hacer: cuentas internas, aprobación de vendedores, borrado de cuentas. */
@Service
public class AdminService {

    private static final Set<String> ROLES_INTERNOS = Set.of("DISTRIBUIDOR", "ADMIN");

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final AliadoRepository aliadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final ProductoRepository productoRepository;
    private final PromocionRepository promocionRepository;
    private final CuentaCobroAliadoRepository cuentaCobroAliadoRepository;
    private final CalificacionVendedorRepository calificacionVendedorRepository;
    private final ResenaRepository resenaRepository;
    private final QuejaSugerenciaRepository quejaSugerenciaRepository;
    private final ChatbotConversacionRepository chatbotConversacionRepository;

    public AdminService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                        AliadoRepository aliadoRepository, PasswordEncoder passwordEncoder,
                        PedidoRepository pedidoRepository, DetallePedidoRepository detallePedidoRepository,
                        ProductoRepository productoRepository, PromocionRepository promocionRepository,
                        CuentaCobroAliadoRepository cuentaCobroAliadoRepository,
                        CalificacionVendedorRepository calificacionVendedorRepository,
                        ResenaRepository resenaRepository, QuejaSugerenciaRepository quejaSugerenciaRepository,
                        ChatbotConversacionRepository chatbotConversacionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.aliadoRepository = aliadoRepository;
        this.passwordEncoder = passwordEncoder;
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.productoRepository = productoRepository;
        this.promocionRepository = promocionRepository;
        this.cuentaCobroAliadoRepository = cuentaCobroAliadoRepository;
        this.calificacionVendedorRepository = calificacionVendedorRepository;
        this.resenaRepository = resenaRepository;
        this.quejaSugerenciaRepository = quejaSugerenciaRepository;
        this.chatbotConversacionRepository = chatbotConversacionRepository;
    }

    @Transactional
    public UsuarioView crearUsuarioInterno(AdminCrearUsuarioRequest req) {
        if (usuarioRepository.existsByCorreo(req.getCorreo())) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con ese correo.");
        }
        String rolSolicitado = req.getRol() == null ? "" : req.getRol().toUpperCase();
        if (!ROLES_INTERNOS.contains(rolSolicitado)) {
            throw new IllegalArgumentException("Aquí solo se crean cuentas de Distribuidor o Administrador.");
        }
        Rol rol = rolRepository.findByNombre(rolSolicitado)
                .orElseThrow(() -> new IllegalArgumentException("Rol inválido: " + req.getRol()));
        // Igual que cualquier otra cuenta nueva: queda pendiente hasta que la propia persona
        // confirme el código de 6 dígitos (por correo o WhatsApp) la primera vez que intente
        // entrar — el admin sólo le comparte su correo y contraseña inicial.
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombre(req.getNombre()).apellidos(req.getApellidos())
                .correo(req.getCorreo()).telefono(req.getTelefono())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .rol(rol).verificadoIa(true).estatus("PENDIENTE_VERIFICACION").build());
        return toView(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioView> listarUsuarios() {
        return usuarioRepository.findAllByOrderByFechaRegistroDesc().stream().map(this::toView).toList();
    }

    @Transactional
    public UsuarioView cambiarEstatus(Integer idUsuario, Integer idAdminActual, String estatus) {
        Usuario u = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if (u.getId().equals(idAdminActual)) {
            throw new IllegalStateException("No puedes suspender tu propia cuenta.");
        }
        String nuevo = estatus == null ? "" : estatus.toUpperCase();
        if (!nuevo.equals("ACTIVO") && !nuevo.equals("SUSPENDIDO")) {
            throw new IllegalArgumentException("El estatus debe ser ACTIVO o SUSPENDIDO.");
        }
        u.setEstatus(nuevo);
        return toView(usuarioRepository.save(u));
    }

    @Transactional(readOnly = true)
    public List<AliadoAdminView> vendedoresPorVerificar() {
        return aliadoRepository.findByEstatusVerificacion("PENDIENTE").stream().map(a ->
                new AliadoAdminView(a.getId(), a.getNombreComercial(), a.getUsuario().getCorreo(), a.getRfc(),
                        a.getEstatusVerificacion(), a.getPorcentajeConvenio())).toList();
    }

    @Transactional
    public void verificarVendedor(Integer idAliado, boolean aprobar) {
        Aliado a = aliadoRepository.findById(idAliado)
                .orElseThrow(() -> new IllegalArgumentException("Vendedor no encontrado."));
        a.setEstatusVerificacion(aprobar ? "APROBADO" : "RECHAZADO");
        a.setDocumentosValidados(aprobar);
        aliadoRepository.save(a);
    }

    /**
     * Botón "Eliminar Cuenta" del Administrador.
     *
     * Si la cuenta nunca compró ni vendió nada, se borra por completo de la base de datos
     * (usuario, aliado si aplica, productos, promociones, etc. — todo lo suyo, de verdad).
     *
     * Si la cuenta ya tiene historial de compras o ventas, borrarla de verdad rompería los
     * pedidos, facturas y envíos de OTRAS personas que compraron con ella (su registro de
     * compra quedaría apuntando a un vendedor o comprador que ya no existe). En ese caso se
     * anonimiza: se borran sus datos personales (nombre, correo, teléfono, contraseña —
     * nadie, ni siquiera con la contraseña anterior, puede volver a entrar) y sus productos
     * se ocultan del catálogo, pero el historial de pedidos de otras personas sigue intacto.
     */
    @Transactional
    public void eliminarCuenta(Integer idUsuario, Integer idAdminActual) {
        if (idUsuario.equals(idAdminActual)) {
            throw new IllegalStateException("No puedes eliminar tu propia cuenta.");
        }
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        Aliado aliado = aliadoRepository.findByUsuario_Id(idUsuario).orElse(null);
        boolean vendioAlgo = aliado != null && detallePedidoRepository.existsByAliado_Id(aliado.getId());
        boolean comproAlgo = pedidoRepository.existsByUsuario_Id(idUsuario);

        // Datos propios que siempre se pueden borrar de verdad, tenga o no historial de compra/venta.
        chatbotConversacionRepository.findByUsuario_Id(idUsuario).forEach(c -> c.setUsuario(null));
        quejaSugerenciaRepository.findByUsuario_Id(idUsuario).forEach(quejaSugerenciaRepository::delete);

        if (!vendioAlgo && !comproAlgo) {
            // --- Sin historial: borrado completo y real ---
            resenaRepository.findByUsuario_Id(idUsuario).forEach(resenaRepository::delete);
            calificacionVendedorRepository.findByUsuario_Id(idUsuario).forEach(calificacionVendedorRepository::delete);
            if (aliado != null) {
                cuentaCobroAliadoRepository.findByAliado_Id(aliado.getId()).ifPresent(cuentaCobroAliadoRepository::delete);
                promocionRepository.findByAliado_IdOrderByFechaFinDesc(aliado.getId()).forEach(promocionRepository::delete);
                productoRepository.findByAliado_Id(aliado.getId()).forEach(productoRepository::delete);
                aliadoRepository.delete(aliado);
            }
            usuarioRepository.delete(usuario);
        } else {
            // --- Con historial: anonimizar y desactivar para siempre ---
            usuario.setNombre("Cuenta eliminada");
            usuario.setApellidos("");
            usuario.setCorreo("eliminada-" + idUsuario + "@jjm.local");
            usuario.setTelefono(null);
            usuario.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString())); // nadie puede volver a entrar
            usuario.setEstatus("ELIMINADA");
            usuario.setResetPasswordToken(null);
            usuario.setResetPasswordExpira(null);
            usuario.setToken2fa(null);
            usuarioRepository.save(usuario);

            if (aliado != null) {
                aliado.setNombreComercial("Vendedor eliminado");
                aliado.setCausaSocial(null);
                aliado.setRfc(null);
                aliado.setEstatusVerificacion("RECHAZADO");
                aliadoRepository.save(aliado);
                cuentaCobroAliadoRepository.findByAliado_Id(aliado.getId()).ifPresent(cuentaCobroAliadoRepository::delete);
                // Sus productos se ocultan del catálogo, pero no se borran: ya fueron comprados por otras personas.
                for (Producto p : productoRepository.findByAliado_Id(aliado.getId())) {
                    p.setEstatus("PAUSADO");
                    productoRepository.save(p);
                }
                promocionRepository.findByAliado_IdOrderByFechaFinDesc(aliado.getId())
                        .forEach(promo -> { promo.setEstatus("CANCELADA"); promocionRepository.save(promo); });
            }
        }
    }

    private UsuarioView toView(Usuario u) {
        return new UsuarioView(u.getId(), u.getNombre(), u.getApellidos(), u.getCorreo(),
                u.getRol().getNombre(), u.getEstatus(), u.getFechaRegistro());
    }
}
