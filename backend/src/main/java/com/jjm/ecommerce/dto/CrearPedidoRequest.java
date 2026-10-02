package com.jjm.ecommerce.dto;

import lombok.Data;

import java.util.List;

@Data
public class CrearPedidoRequest {
    private List<CarritoItemRequest> items;
    private String metodoPago;      // TARJETA_CREDITO, OXXO, SEVEN, TRANSFERENCIA, VALE_DESPENSA, BILLETERA_DIGITAL
    private String direccionEnvio;  // obligatorio si hay productos físicos
    private String cpEnvio;

    // Punto 9: sólo para métodos TARJETA_CREDITO / TARJETA_DEBITO. El número completo y el CVV
    // NUNCA se envían al servidor (se validan y se descartan en el navegador); sólo se manda la
    // marca de la tarjeta y sus últimos 4 dígitos, únicamente para mostrarlos de referencia en el
    // historial de pagos (ej. "Visa terminación 4242").
    private String marcaTarjeta;
    private String ultimos4Tarjeta;
}
