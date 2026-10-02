package com.jjm.ecommerce.service;

import com.jjm.ecommerce.dto.MensajeChatbotRequest;
import com.jjm.ecommerce.dto.Vistas.ConversacionView;
import com.jjm.ecommerce.dto.Vistas.MensajeView;
import com.jjm.ecommerce.model.ChatbotConversacion;
import com.jjm.ecommerce.model.ChatbotMensaje;
import com.jjm.ecommerce.model.Usuario;
import com.jjm.ecommerce.repository.ChatbotConversacionRepository;
import com.jjm.ecommerce.repository.ProductoRepository;
import com.jjm.ecommerce.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Asistente virtual siempre presente en la web y en la app: acompaña al CLIENTE (buscar, comprar,
 * promociones, envíos, factura, calificar) y al VENDEDOR (publicar con 5-7 archivos, campañas,
 * paqueterías, ranking). Responde según el contexto que manda cada pantalla.
 *
 * En modo MOCK responde con reglas conversacionales y datos reales del catálogo. Para conectar un
 * proveedor de IA generativa basta sustituir el cuerpo de generarRespuesta() por la llamada a su API
 * (integraciones.chatbot.* en application.properties).
 */
@Service
public class ChatbotIaService {

    @Value("${integraciones.chatbot.proveedor}")
    private String proveedor;

    private final ChatbotConversacionRepository conversacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;

    public ChatbotIaService(ChatbotConversacionRepository conversacionRepository,
                            UsuarioRepository usuarioRepository, ProductoRepository productoRepository) {
        this.conversacionRepository = conversacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional
    public ConversacionView responder(Integer idUsuario, MensajeChatbotRequest req) {
        if (req.getMensaje() == null || req.getMensaje().isBlank()) {
            throw new IllegalArgumentException("Escribe un mensaje.");
        }
        String contexto = normalizarContexto(req.getContexto());
        ChatbotConversacion conversacion;
        if (req.getIdConversacion() != null) {
            conversacion = conversacionRepository.findById(req.getIdConversacion())
                    .orElseThrow(() -> new IllegalArgumentException("Conversación no encontrada."));
        } else {
            Usuario usuario = idUsuario != null ? usuarioRepository.findById(idUsuario).orElse(null) : null;
            conversacion = conversacionRepository.save(ChatbotConversacion.builder()
                    .usuario(usuario)
                    .canal(req.getCanal() == null ? "WEB" : req.getCanal())
                    .contexto(contexto)
                    .build());
        }
        conversacion.getMensajes().add(ChatbotMensaje.builder()
                .conversacion(conversacion).emisor(ChatbotMensaje.Emisor.USUARIO)
                .mensaje(req.getMensaje().trim()).build());
        conversacion.getMensajes().add(ChatbotMensaje.builder()
                .conversacion(conversacion).emisor(ChatbotMensaje.Emisor.BOT)
                .mensaje(generarRespuesta(req.getMensaje(), contexto, req.getPagina())).build());
        conversacion = conversacionRepository.save(conversacion);

        List<MensajeView> mensajes = conversacion.getMensajes().stream()
                .map(m -> new MensajeView(m.getEmisor().name(), m.getMensaje(), m.getFecha())).toList();
        return new ConversacionView(conversacion.getId(), mensajes);
    }

    private String normalizarContexto(String c) {
        if (c == null) return "CLIENTE";
        String u = c.trim().toUpperCase();
        return switch (u) {
            case "VENDEDOR", "ALIADO" -> "VENDEDOR";
            case "DISTRIBUIDOR", "ADMIN" -> u;
            default -> "CLIENTE";
        };
    }

    private String generarRespuesta(String mensaje, String contexto, String pagina) {
        if (!"MOCK".equalsIgnoreCase(proveedor)) {
            return "El asistente virtual no está disponible en este momento.";   // TODO: proveedor de IA real
        }
        String t = sinAcentos(mensaje);

        if (tiene(t, "hola", "buenas", "buenos dias", "buenas tardes", "buenas noches", "que tal")) {
            return "VENDEDOR".equals(contexto)
                    ? "¡Hola! Soy tu asistente 🤝. Te acompaño a publicar tus productos, lanzar campañas de temporada, elegir paqueterías y mejorar tu ranking. ¿Con qué empezamos?"
                    : "¡Hola! Soy tu asistente 🤝. Te ayudo a encontrar productos con causa, elegir entre costo normal o promoción, escoger paquetería, facturar y dar seguimiento a tu pedido. ¿Qué necesitas?";
        }
        if (tiene(t, "gracias", "te lo agradezco")) return "¡Con mucho gusto! Aquí sigo por si necesitas algo más. 😊";
        if (tiene(t, "adios", "hasta luego", "bye", "nos vemos")) return "¡Hasta pronto! Gracias por apoyar a nuestros aliados con causa. 🤝";

        // --- Cuenta y acceso
        if (tiene(t, "contrasena", "password", "olvide", "recuperar", "restablecer", "no puedo entrar", "no puedo iniciar")) {
            return "Si olvidaste tu contraseña, en la pantalla de inicio de sesión toca \"¿Olvidaste tu contraseña?\", escribe tu correo y te enviaremos un enlace para crear una nueva (vale 30 minutos).";
        }
        if (tiene(t, "que rol", "distribuidor", "administrador", "tipo de cuenta", "tipos de cuenta")) {
            return "Hay 4 tipos de cuenta: Cliente (compra), Vendedor (publica y vende), Distribuidor (gestiona guías y entregas) y Administrador (dueño de la plataforma). Al registrarte eliges Cliente o Vendedor; las cuentas de Distribuidor las crea el Administrador.";
        }

        // --- Vendedor
        if (tiene(t, "publicar", "subir", "agregar producto", "imagen", "foto", "video", "galeria")) {
            return "Para publicar un producto necesitas de 5 a 7 archivos en su galería: imágenes y, si quieres, videos de máximo 15 segundos (con o sin audio). Un buen consejo: foto principal con fondo limpio, detalles, uso real y un video corto mostrando el producto. 📸";
        }
        if (tiene(t, "campana", "temporada", "navidad", "dia de muertos", "fiestas patrias", "buen fin", "descuento", "promocion", "oferta")) {
            return "VENDEDOR".equals(contexto)
                    ? "Puedes lanzar campañas desde tu panel: elige productos, temporada (Navidad, Día de Muertos, Fiestas Patrias, Buen Fin…), el descuento (20%, 30%, 40%…) y las fechas. Si la campaña apoya a una asociación civil o fundación, indica su nombre y el % de donación."
                    : "Cuando un producto tiene promoción vigente verás dos opciones al comprar: costo normal o precio con promoción. Si la campaña es de causa social, parte de tu compra se dona a la asociación o fundación indicada. 🎁";
        }
        if (tiene(t, "ranking", "nivel", "calificacion", "calificar", "estrellas", "reputacion")) {
            return "VENDEDOR".equals(contexto)
                    ? "Tu nivel sube con las calificaciones de tus clientes: NUEVO → BRONCE → PLATA → ORO → PLATINO. Entregar a tiempo, fotos claras y buena atención mejoran tu promedio."
                    : "Después de tu compra puedes calificar al vendedor de 1 a 5 estrellas desde \"Mis pedidos\". Así ayudas a otros clientes y reconoces a los mejores vendedores.";
        }

        // --- Compra
        if (tiene(t, "envio", "paqueteria", "dhl", "fedex", "estafeta", "guia", "rastrear", "entrega", "cuanto tarda")) {
            return "Cada producto trae sus paqueterías disponibles (DHL, FedEx, Estafeta, etc.). Al elegirla verás el costo y los días de entrega; el envío se suma al total y, al pagar, se genera tu número de guía. Lo consultas en \"Mis pedidos\".";
        }
        if (tiene(t, "factura", "facturar", "rfc", "cfdi", "iva")) {
            return "Puedes solicitar tu factura desde \"Mis pedidos\" en cualquier pedido pagado: necesitas tu RFC, razón social, régimen fiscal y código postal fiscal. Los precios ya incluyen IVA.";
        }
        if (tiene(t, "pago", "pagar", "tarjeta", "oxxo", "seven", "transferencia", "vale", "billetera")) {
            return "Aceptamos tarjeta de crédito y débito, transferencia, pago en OXXO o Seven, vales de despensa y billeteras digitales. Eliges el método al finalizar tu compra en el carrito.";
        }
        if (tiene(t, "pedido", "mis compras", "donde esta mi")) {
            return "Puedes ver el estatus de tus compras, guías y tiempos de entrega en \"Mis pedidos\". También ahí solicitas tu factura y calificas al vendedor.";
        }
        if (tiene(t, "queja", "sugerencia", "problema", "reclamo", "no llego", "no funciona")) {
            return "Lamento el inconveniente. Puedes registrar tu queja o sugerencia en \"Quejas y Sugerencias\" y el equipo de JJM le dará seguimiento. Si quieres, cuéntame qué pasó y te oriento.";
        }
        if (tiene(t, "causa", "fundacion", "asociacion", "donar", "donacion", "aliado")) {
            return "En JJM cada compra apoya a nuestros aliados; además, las campañas de causa social destinan un porcentaje a una asociación civil o fundación sin fines de lucro, y verás su nombre en el producto y en tu pedido.";
        }
        if (tiene(t, "producto", "servicio", "buscar", "catalogo", "recomienda", "comprar")) {
            List<String> sugerencias = productoRepository.findByEstatus("ACTIVO").stream()
                    .limit(3).map(p -> p.getNombre()).toList();
            return sugerencias.isEmpty()
                    ? "Todavía no hay productos publicados, pero en cuanto un vendedor suba los suyos los verás en el catálogo."
                    : "Algunos productos que podrían interesarte: " + String.join(", ", sugerencias)
                    + ". Usa el buscador o las categorías para ver más. 🛍️";
        }

        return "VENDEDOR".equals(contexto)
                ? "Puedo ayudarte con: publicar productos (5-7 archivos), campañas y descuentos, paqueterías, tu ranking y tus reportes. ¿Sobre qué quieres saber?"
                : "Puedo ayudarte con: buscar productos, promociones, paqueterías y tiempos de entrega, métodos de pago, factura, calificar vendedores o levantar una queja. ¿Sobre qué quieres saber?";
    }

    private static boolean tiene(String texto, String... claves) {
        for (String c : claves) if (texto.contains(c)) return true;
        return false;
    }

    private static String sinAcentos(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT).trim();
    }
}
