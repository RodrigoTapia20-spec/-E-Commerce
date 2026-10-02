package com.jjm.ecommerce.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envío de correos (código 2FA, confirmación de pedidos) usando la cuenta
 * SMTP configurada en application.properties (spring.mail.*).
 *
 * IMPORTANTE — para que el correo realmente llegue necesitas:
 * 1) Una cuenta con verificación en dos pasos activada (ej. Gmail) y una
 *    "Contraseña de aplicación" generada específicamente para esto (una
 *    contraseña normal de Gmail YA NO funciona para SMTP desde 2022).
 * 2) spring.mail.username = tu correo completo (ej. tucorreo@gmail.com)
 * 3) spring.mail.password = la contraseña de aplicación de 16 caracteres
 *    (sin espacios), NO tu contraseña normal de la cuenta.
 * Si estos datos son incorrectos o son los de ejemplo (changeme), el envío
 * fallará y el sistema seguirá funcionando en "modo desarrollo": el código
 * se imprime en la consola del backend en vez de llegar al correo.
 */
@Service
public class CorreoService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remitente;

    @Value("${app.webapp.url:http://localhost:5500}")
    private String webappUrl;

    public CorreoService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarEnlaceRecuperacion(String destinatario, String token) {
        String enlace = webappUrl + "/restablecer-password.html?token=" + token;
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject("JJM Tecnologías Innovadoras - Recupera tu contraseña");
        mensaje.setText("Recibimos una solicitud para restablecer tu contraseña.\n\n" +
                "Abre este enlace para crear una nueva contraseña (válido por 30 minutos):\n" + enlace +
                "\n\nSi tú no lo solicitaste, ignora este mensaje; tu contraseña actual sigue siendo válida.");
        try {
            mailSender.send(mensaje);
            System.out.println("[CORREO] Enlace de recuperación enviado a " + destinatario);
        } catch (MailException e) {
            System.out.println("[ERROR AL ENVIAR CORREO] " + e.getMessage());
            System.out.println("[MODO DEV] Enlace de recuperación para " + destinatario + ": " + enlace);
        }
    }

    /** Punto 5: código de 6 dígitos para confirmar la cuenta recién registrada. */
    public void enviarCodigoVerificacionCuenta(String destinatario, String codigo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject("JJM Tecnologías Innovadoras - Confirma tu cuenta");
        mensaje.setText("¡Gracias por registrarte en JJM con Causa!\n\n" +
                "Tu código de confirmación es: " + codigo +
                "\nIngrésalo en la página para activar tu cuenta. Este código expira en 15 minutos." +
                "\n\nSi tú no creaste esta cuenta, ignora este mensaje.");
        try {
            mailSender.send(mensaje);
            System.out.println("[CORREO] Código de confirmación de cuenta enviado a " + destinatario);
        } catch (MailException e) {
            System.out.println("[ERROR AL ENVIAR CORREO] " + e.getMessage());
            System.out.println("[MODO DEV] Código de confirmación de cuenta para " + destinatario + ": " + codigo);
        }
    }

    public void enviarCodigo2fa(String destinatario, String codigo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject("JJM Tecnologías Innovadoras - Código de verificación");
        mensaje.setText("Tu código de verificación en dos pasos es: " + codigo +
                "\nEste código expira en unos minutos. Si no solicitaste este código, ignora este mensaje.");
        try {
            mailSender.send(mensaje);
            System.out.println("[CORREO] Código 2FA enviado correctamente a " + destinatario);
        } catch (MailException e) {
            // Se imprime la causa real del fallo (credenciales, host, puerto, etc.)
            // para poder diagnosticarlo, y de todas formas se muestra el código en
            // consola para no bloquear las pruebas del flujo funcional.
            System.out.println("[ERROR AL ENVIAR CORREO] " + e.getMessage());
            System.out.println("[MODO DEV] Código 2FA para " + destinatario + ": " + codigo);
        }
    }

    public void notificarPedidoConfirmado(String destinatario, Integer idPedido) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject("Confirmación de tu pedido #" + idPedido);
        mensaje.setText("¡Gracias por tu compra con causa! Tu pedido #" + idPedido +
                " fue confirmado y una parte de tu pago está apoyando directamente a nuestros aliados.");
        try {
            mailSender.send(mensaje);
        } catch (MailException e) {
            System.out.println("[ERROR AL ENVIAR CORREO] " + e.getMessage());
            System.out.println("[MODO DEV] Notificación de pedido " + idPedido + " para " + destinatario);
        }
    }
}
