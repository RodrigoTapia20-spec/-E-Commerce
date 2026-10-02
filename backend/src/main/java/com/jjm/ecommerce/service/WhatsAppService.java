package com.jjm.ecommerce.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;

/**
 * Envío de mensajes de WhatsApp (recuperación de contraseña) usando la API de
 * WhatsApp de Twilio.
 *
 * IMPORTANTE — para que el mensaje realmente llegue necesitas una cuenta de
 * Twilio (tiene un nivel gratuito de prueba) con WhatsApp habilitado:
 * 1) Crea una cuenta en https://www.twilio.com/try-twilio
 * 2) Activa el "WhatsApp Sandbox" (Messaging → Try it out → Send a WhatsApp message)
 *    y sigue las instrucciones para unir tu propio número de WhatsApp al sandbox
 *    (mientras estés en modo de prueba, SÓLO pueden recibir mensajes los números
 *    que se hayan unido al sandbox de esa forma).
 * 3) Copia tu Account SID y Auth Token del dashboard de Twilio.
 * 4) Ponlos como variables de entorno WHATSAPP_ACCOUNT_SID y WHATSAPP_AUTH_TOKEN
 *    (o directamente en application.properties).
 * Mientras estos valores sigan siendo los de ejemplo ("changeme"), el sistema
 * sigue funcionando en "modo desarrollo": el mensaje se imprime en la consola
 * del backend en vez de llegar por WhatsApp de verdad.
 */
@Service
public class WhatsAppService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${integraciones.whatsapp.account-sid}")
    private String accountSid;
    @Value("${integraciones.whatsapp.auth-token}")
    private String authToken;
    @Value("${integraciones.whatsapp.numero-remitente}")
    private String numeroRemitente; // formato Twilio: whatsapp:+14155238886
    @Value("${integraciones.whatsapp.lada-defecto:+52}")
    private String ladaDefecto;
    @Value("${app.webapp.url:http://localhost:5500}")
    private String webappUrl;

    public String construirEnlaceRecuperacion(String token) {
        return webappUrl + "/restablecer-password.html?token=" + token;
    }

    private boolean configurado() {
        return accountSid != null && !accountSid.isBlank() && !accountSid.equalsIgnoreCase("changeme")
                && authToken != null && !authToken.isBlank() && !authToken.equalsIgnoreCase("changeme");
    }

    /** Normaliza el teléfono guardado en la cuenta a formato internacional "whatsapp:+52...". */
    private String numeroWhatsApp(String telefono) {
        String limpio = telefono.replaceAll("[^0-9+]", "");
        if (!limpio.startsWith("+")) limpio = ladaDefecto + limpio;
        return "whatsapp:" + limpio;
    }

    public void enviarMensaje(String telefonoDestino, String texto) {
        if (!configurado()) {
            System.out.println("[WHATSAPP NO CONFIGURADO] Ver comentario en WhatsAppService.java para activarlo.");
            System.out.println("[MODO DEV] Mensaje que se hubiera enviado a " + telefonoDestino + ":\n" + texto);
            return;
        }
        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(accountSid, authToken);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("To", numeroWhatsApp(telefonoDestino));
            body.add("From", numeroRemitente);
            body.add("Body", texto);

            var respuesta = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            if (respuesta.getStatusCode() == HttpStatus.CREATED || respuesta.getStatusCode() == HttpStatus.OK) {
                System.out.println("[WHATSAPP] Mensaje enviado a " + telefonoDestino);
            } else {
                System.out.println("[WHATSAPP] Respuesta inesperada de Twilio: " + respuesta.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("[ERROR AL ENVIAR WHATSAPP] " + e.getMessage());
            System.out.println("[MODO DEV] Mensaje que se hubiera enviado a " + telefonoDestino + ":\n" + texto);
        }
    }
}
