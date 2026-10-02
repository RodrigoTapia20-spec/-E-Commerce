package com.jjm.ecommerce.service;

import com.jjm.ecommerce.dto.*;
import com.jjm.ecommerce.model.*;
import com.jjm.ecommerce.repository.AliadoRepository;
import com.jjm.ecommerce.repository.RolRepository;
import com.jjm.ecommerce.repository.UsuarioRepository;
import com.jjm.ecommerce.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquesta el registro (con confirmación de correo por código de 6 dígitos),
 * la autenticación en dos pasos (2FA) y la emisión de JWT. Es el único punto
 * de entrada de autenticación tanto para la webapp como para la app móvil.
 *
 * Estatus de una cuenta a lo largo de su vida:
 *   PENDIENTE_VERIFICACION → recién registrada, aún no confirma su correo.
 *   ACTIVO                 → correo confirmado, puede iniciar sesión.
 *   SUSPENDIDO              → un Administrador la suspendió.
 * Para un Vendedor (ALIADO), además, Aliado.estatusVerificacion debe llegar a
 * "APROBADO" (lo hace el Administrador) antes de que pueda publicar productos;
 * esto es independiente de la confirmación de correo.
 */
@Service
public class AuthService {

    private static final String PENDIENTE_VERIFICACION = "PENDIENTE_VERIFICACION";

    private final UsuarioRepository usuarioRepository;
    private final AliadoRepository aliadoRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CorreoService correoService;
    private final WhatsAppService whatsAppService;
    private final IaVerificacionService iaVerificacionService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${security.twofactor.enabled}")
    private boolean twoFactorEnabled;

    public AuthService(UsuarioRepository usuarioRepository, AliadoRepository aliadoRepository,
                        RolRepository rolRepository, PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager, JwtService jwtService,
                        CorreoService correoService, WhatsAppService whatsAppService,
                        IaVerificacionService iaVerificacionService) {
        this.usuarioRepository = usuarioRepository;
        this.aliadoRepository = aliadoRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.correoService = correoService;
        this.whatsAppService = whatsAppService;
        this.iaVerificacionService = iaVerificacionService;
    }

    private static final java.util.Set<String> ROLES_REGISTRO_PUBLICO = java.util.Set.of("CLIENTE", "ALIADO");

    /**
     * Paso 1 del registro: crea la cuenta en estatus PENDIENTE_VERIFICACION y
     * envía un código de 6 dígitos al correo. La cuenta NO puede iniciar
     * sesión hasta confirmar ese código (ver {@link #verificarCuenta}).
     */
    @Transactional
    public AuthResponse registrar(RegistroRequest req) {
        if (usuarioRepository.existsByCorreo(req.getCorreo())) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con ese correo.");
        }

        // Seguridad: el registro público (este endpoint, sin autenticación) SOLO
        // puede crear cuentas de Cliente o Vendedor (Aliado). Las cuentas de
        // Distribuidor y Administrador las crea el Administrador desde su panel.
        String rolSolicitado = req.getRol() == null ? "" : req.getRol().toUpperCase();
        if (!ROLES_REGISTRO_PUBLICO.contains(rolSolicitado)) {
            throw new IllegalArgumentException("Sólo puedes registrarte como Cliente o Vendedor.");
        }

        Rol rol = rolRepository.findByNombre(rolSolicitado)
                .orElseThrow(() -> new IllegalArgumentException("Rol inválido: " + req.getRol()));

        if ("ALIADO".equals(rolSolicitado)
                && (req.getNombreComercial() == null || req.getNombreComercial().isBlank())) {
            throw new IllegalArgumentException("Escribe el nombre comercial de tu negocio para registrarte como Vendedor.");
        }

        Usuario usuario = Usuario.builder()
                .nombre(req.getNombre())
                .apellidos(req.getApellidos())
                .correo(req.getCorreo())
                .telefono(req.getTelefono())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .rol(rol)
                .nivelSeguridad(iaVerificacionService.generarNivelSeguridadAleatorio())
                .estatus(PENDIENTE_VERIFICACION)
                .build();
        usuario = usuarioRepository.save(usuario);
        // El código de confirmación NO se manda aquí: el usuario elige primero si lo quiere
        // por correo o por WhatsApp (ver enviarCodigoRegistroCorreo / enviarCodigoRegistroWhatsapp).

        if ("ALIADO".equalsIgnoreCase(req.getRol())) {
            // La verificación IA de identidad es sólo informativa (se muestra al
            // Administrador como referencia); NUNCA aprueba la cuenta por sí sola.
            // Un Vendedor SIEMPRE queda en PENDIENTE y requiere aprobación manual
            // del Administrador antes de poder publicar productos.
            boolean identidadPlausible = iaVerificacionService.verificarIdentidadAliado(
                    req.getRfc(), req.getNombreComercial());

            String causaSocial = (req.getCausaSocial() == null || req.getCausaSocial().isBlank())
                    ? null : req.getCausaSocial().trim();

            Aliado aliado = Aliado.builder()
                    .usuario(usuario)
                    .nombreComercial(req.getNombreComercial())
                    .causaSocial(causaSocial)
                    .rfc(req.getRfc())
                    .porcentajeConvenio(req.getPorcentajeConvenio() != null ? req.getPorcentajeConvenio() : java.math.BigDecimal.ZERO)
                    .documentosValidados(identidadPlausible)
                    .estatusVerificacion("PENDIENTE")
                    .build();
            aliadoRepository.save(aliado);
        }

        correoService.enviarCodigoVerificacionCuenta(usuario.getCorreo(), codigo);

        String mensaje = "ALIADO".equals(rolSolicitado)
                ? "Elige cómo quieres recibir tu código de confirmación. Después de confirmar tu cuenta, el administrador debe aprobar tu cuenta de vendedor antes de que puedas publicar."
                : "Elige cómo quieres recibir tu código de confirmación.";

        return AuthResponse.builder()
                .idUsuario(usuario.getId())
                .nombre(usuario.getNombre())
                .rol(rol.getNombre())
                .requiere2fa(false)
                .requiereVerificacionCorreo(true)
                .mensaje(mensaje)
                .build();
    }

    /** Paso 2 del registro: confirma el código de 6 dígitos y activa la cuenta. */
    @Transactional
    public AuthResponse verificarCuenta(VerificarCuentaRequest req) {
        Usuario usuario = usuarioRepository.findByCorreo(req.getCorreo())
                .orElseThrow(() -> new IllegalArgumentException("No encontramos una cuenta con ese correo."));

        if (!PENDIENTE_VERIFICACION.equals(usuario.getEstatus())) {
            throw new IllegalStateException("Esta cuenta ya fue confirmada. Intenta iniciar sesión.");
        }
        if (usuario.getToken2fa() == null || !usuario.getToken2fa().equals(req.getCodigo().trim())) {
            throw new IllegalArgumentException("El código es incorrecto.");
        }
        if (usuario.getResetPasswordExpira() == null || usuario.getResetPasswordExpira().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El código expiró. Pide que te enviemos uno nuevo.");
        }

        usuario.setEstatus("ACTIVO");
        usuario.setToken2fa(null);
        usuario.setResetPasswordExpira(null);
        usuarioRepository.save(usuario);

        return AuthResponse.builder()
                .idUsuario(usuario.getId())
                .nombre(usuario.getNombre())
                .rol(usuario.getRol().getNombre())
                .mensaje("¡Cuenta confirmada! Ya puedes iniciar sesión.")
                .build();
    }

    /** Genera un código nuevo (sea la primera vez o un reenvío) y lo manda por CORREO. */
    @Transactional
    public void enviarCodigoRegistroCorreo(ReenviarCodigoRequest req) {
        Usuario usuario = usuarioPendienteDeVerificar(req.getCorreo());
        String codigo = regenerarCodigo(usuario);
        correoService.enviarCodigoVerificacionCuenta(usuario.getCorreo(), codigo);
    }

    /** Genera un código nuevo y lo manda por WHATSAPP, al número que el usuario registró en su cuenta. */
    @Transactional
    public void enviarCodigoRegistroWhatsapp(ReenviarCodigoRequest req) {
        Usuario usuario = usuarioPendienteDeVerificar(req.getCorreo());
        if (usuario.getTelefono() == null || usuario.getTelefono().isBlank()) {
            throw new IllegalArgumentException("No registraste un número de teléfono en tu cuenta; usa la opción de correo.");
        }
        String codigo = regenerarCodigo(usuario);
        whatsAppService.enviarMensaje(usuario.getTelefono(),
                "Hola " + usuario.getNombre() + ", tu código para confirmar tu cuenta de JJM con Causa es: " + codigo +
                "\nVale por 15 minutos. Si tú no creaste esta cuenta, ignora este mensaje.");
    }

    private Usuario usuarioPendienteDeVerificar(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new IllegalArgumentException("No encontramos una cuenta con ese correo."));
        if (!PENDIENTE_VERIFICACION.equals(usuario.getEstatus())) {
            throw new IllegalStateException("Esta cuenta ya fue confirmada. Intenta iniciar sesión.");
        }
        return usuario;
    }

    private String regenerarCodigo(Usuario usuario) {
        String codigo = generarCodigo();
        usuario.setToken2fa(codigo);
        usuario.setResetPasswordExpira(LocalDateTime.now().plusMinutes(15));
        usuarioRepository.save(usuario);
        return codigo;
    }

    public AuthResponse login(LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getCorreo(), req.getPassword()));
        } catch (Exception e) {
            throw new BadCredentialsException("Correo o contraseña incorrectos.");
        }

        Usuario usuario = usuarioRepository.findByCorreo(req.getCorreo())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado."));

        if (PENDIENTE_VERIFICACION.equals(usuario.getEstatus())) {
            throw new IllegalStateException("Confirma tu cuenta con el código que te enviamos por correo antes de iniciar sesión.");
        }
        if (!"ACTIVO".equals(usuario.getEstatus())) {
            throw new IllegalStateException("La cuenta se encuentra suspendida. Contacta a soporte.");
        }

        if (twoFactorEnabled) {
            String codigo = generarCodigo();
            usuario.setToken2fa(codigo);
            usuarioRepository.save(usuario);
            correoService.enviarCodigo2fa(usuario.getCorreo(), codigo);
            return AuthResponse.builder()
                    .idUsuario(usuario.getId())
                    .nombre(usuario.getNombre())
                    .rol(usuario.getRol().getNombre())
                    .requiere2fa(true)
                    .build();
        }

        return generarRespuestaConToken(usuario);
    }

    public AuthResponse verificarCodigo(VerificarCodigoRequest req) {
        Usuario usuario = usuarioRepository.findByCorreo(req.getCorreo())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado."));

        if (usuario.getToken2fa() == null || !usuario.getToken2fa().equals(req.getCodigo())) {
            throw new IllegalArgumentException("Código de verificación inválido o expirado.");
        }

        usuario.setToken2fa(null);
        usuarioRepository.save(usuario);

        return generarRespuestaConToken(usuario);
    }

    /** Recuperación de contraseña por CORREO: envía un enlace de un solo uso, válido 30 minutos. */
    @Transactional
    public void solicitarRecuperacionPassword(SolicitarRecuperacionRequest req) {
        usuarioRepository.findByCorreo(req.getCorreo()).ifPresent(usuario -> {
            String token = generarTokenRecuperacion(usuario);
            correoService.enviarEnlaceRecuperacion(usuario.getCorreo(), token);
        });
    }

    /**
     * Recuperación de contraseña por WHATSAPP: igual que por correo, pero el
     * enlace se manda al número de WhatsApp que el usuario registró en su
     * cuenta. Por seguridad NUNCA se envía la contraseña real (las
     * contraseñas se guardan cifradas — ni el propio sistema puede leerlas de
     * vuelta); se manda un enlace de un solo uso para crear una nueva.
     */
    @Transactional
    public void solicitarRecuperacionWhatsApp(SolicitarRecuperacionRequest req) {
        usuarioRepository.findByCorreo(req.getCorreo()).ifPresent(usuario -> {
            if (usuario.getTelefono() == null || usuario.getTelefono().isBlank()) return;
            String token = generarTokenRecuperacion(usuario);
            String enlace = whatsAppService.construirEnlaceRecuperacion(token);
            whatsAppService.enviarMensaje(usuario.getTelefono(),
                    "Hola " + usuario.getNombre() + ", recibimos una solicitud para restablecer tu contraseña de JJM con Causa.\n\n" +
                    "Crea una nueva aquí (válido 30 minutos):\n" + enlace +
                    "\n\nSi tú no lo pediste, ignora este mensaje.");
        });
    }

    private String generarTokenRecuperacion(Usuario usuario) {
        String token = java.util.UUID.randomUUID().toString().replace("-", "");
        usuario.setResetPasswordToken(token);
        usuario.setResetPasswordExpira(LocalDateTime.now().plusMinutes(30));
        usuarioRepository.save(usuario);
        return token;
    }

    public void restablecerPassword(RestablecerPasswordRequest req) {
        Usuario usuario = usuarioRepository.findByResetPasswordToken(req.getToken())
                .orElseThrow(() -> new IllegalArgumentException("El enlace de recuperación no es válido."));

        if (usuario.getResetPasswordExpira() == null
                || usuario.getResetPasswordExpira().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El enlace de recuperación expiró. Solicita uno nuevo.");
        }

        usuario.setPasswordHash(passwordEncoder.encode(req.getNuevaPassword()));
        usuario.setResetPasswordToken(null);
        usuario.setResetPasswordExpira(null);
        usuarioRepository.save(usuario);
    }

    private AuthResponse generarRespuestaConToken(Usuario usuario) {
        UserDetails userDetails = new User(usuario.getCorreo(), usuario.getPasswordHash(),
                List.of(() -> "ROLE_" + usuario.getRol().getNombre()));

        Map<String, Object> claims = new HashMap<>();
        claims.put("idUsuario", usuario.getId());
        claims.put("rol", usuario.getRol().getNombre());
        claims.put("nombre", usuario.getNombre());

        String token = jwtService.generarToken(userDetails, claims);

        return AuthResponse.builder()
                .token(token)
                .idUsuario(usuario.getId())
                .nombre(usuario.getNombre())
                .rol(usuario.getRol().getNombre())
                .requiere2fa(false)
                .build();
    }

    private String generarCodigo() {
        int codigo = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(codigo);
    }
}
