package com.jjm.ecommerce.controller;

import com.jjm.ecommerce.dto.*;
import com.jjm.ecommerce.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints de autenticación consumidos tanto por la webapp como por la app
 * móvil: registro (con confirmación de correo), login, 2FA y recuperación de
 * contraseña por correo o WhatsApp.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Paso 1: crea la cuenta (queda pendiente) y envía un código de 6 dígitos al correo. */
    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest req) {
        return ResponseEntity.ok(authService.registrar(req));
    }

    /** Paso 2: confirma el código de 6 dígitos y activa la cuenta. */
    @PostMapping("/registro/verificar")
    public ResponseEntity<AuthResponse> verificarCuenta(@Valid @RequestBody VerificarCuentaRequest req) {
        return ResponseEntity.ok(authService.verificarCuenta(req));
    }

    /** Manda (o reenvía) el código de 6 dígitos del registro por CORREO. */
    @PostMapping("/registro/codigo-correo")
    public ResponseEntity<Map<String, String>> codigoRegistroPorCorreo(@Valid @RequestBody ReenviarCodigoRequest req) {
        authService.enviarCodigoRegistroCorreo(req);
        return ResponseEntity.ok(Map.of("mensaje", "Te enviamos un código de 6 dígitos a tu correo."));
    }

    /** Manda (o reenvía) el código de 6 dígitos del registro por WHATSAPP. */
    @PostMapping("/registro/codigo-whatsapp")
    public ResponseEntity<Map<String, String>> codigoRegistroPorWhatsapp(@Valid @RequestBody ReenviarCodigoRequest req) {
        authService.enviarCodigoRegistroWhatsapp(req);
        return ResponseEntity.ok(Map.of("mensaje", "Te enviamos un código de 6 dígitos por WhatsApp."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/verificar-codigo")
    public ResponseEntity<AuthResponse> verificarCodigo(@Valid @RequestBody VerificarCodigoRequest req) {
        return ResponseEntity.ok(authService.verificarCodigo(req));
    }

    /** Recuperación de contraseña por CORREO (paso 1 - solicitar enlace). */
    @PostMapping("/solicitar-recuperacion")
    public ResponseEntity<Map<String, String>> solicitarRecuperacion(@Valid @RequestBody SolicitarRecuperacionRequest req) {
        authService.solicitarRecuperacionPassword(req);
        return ResponseEntity.ok(Map.of("mensaje", "Si el correo está registrado, te enviamos un enlace para restablecer tu contraseña."));
    }

    /** Recuperación de contraseña por WHATSAPP (al número registrado en la cuenta). */
    @PostMapping("/solicitar-recuperacion-whatsapp")
    public ResponseEntity<Map<String, String>> solicitarRecuperacionWhatsApp(@Valid @RequestBody SolicitarRecuperacionRequest req) {
        authService.solicitarRecuperacionWhatsApp(req);
        return ResponseEntity.ok(Map.of("mensaje", "Si el correo está registrado y tiene un teléfono asociado, te enviamos un enlace por WhatsApp."));
    }

    /** Recuperación de contraseña por correo (paso 2 - fijar nueva contraseña). */
    @PostMapping("/restablecer-password")
    public ResponseEntity<Map<String, String>> restablecerPassword(@Valid @RequestBody RestablecerPasswordRequest req) {
        authService.restablecerPassword(req);
        return ResponseEntity.ok(Map.of("mensaje", "Tu contraseña fue actualizada. Ya puedes iniciar sesión."));
    }
}
