package com.jjm.ecommerce.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Abstracción de la pasarela de pagos. Soporta tarjeta de crédito/débito,
 * transferencia, OXXO, Seven, vales de despensa y billeteras digitales.
 *
 * Mientras JJM no proporcione las credenciales de un proveedor real (p.ej.
 * Stripe, Conekta, Mercado Pago o similar, todos con soporte para pagos en
 * efectivo tipo OXXO/Seven en México), el servicio corre en modo MOCK: simula
 * una autorización exitosa para permitir que TODO el flujo de compra,
 * facturación interna y dispersión de fondos funcione de principio a fin.
 *
 * Para producción sólo debe sustituirse el cuerpo de {@link #cobrar} por la
 * llamada real al SDK/API del proveedor elegido; el resto del sistema
 * (pedidos, dispersiones, reportes) no requiere cambios.
 */
@Service
public class PasarelaPagoService {

    @Value("${integraciones.pasarela-pagos.proveedor}")
    private String proveedor;

    public ResultadoPago cobrar(String metodoPago, java.math.BigDecimal monto) {
        if ("MOCK".equalsIgnoreCase(proveedor)) {
            String referencia = "MOCK-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            return new ResultadoPago(true, referencia, "APROBADO");
        }
        // TODO: integrar proveedor real de pagos (tarjeta, OXXO, Seven, transferencia, vales, wallets).
        return new ResultadoPago(false, null, "RECHAZADO");
    }

    public record ResultadoPago(boolean exitoso, String referencia, String estatus) {}
}
