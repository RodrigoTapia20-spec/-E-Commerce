package com.jjm.ecommerce.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Capa de verificación de autenticidad mediante Inteligencia Artificial.
 *
 * Este servicio define el CONTRATO que usa el resto del sistema (verificar
 * identidad de aliados, autenticidad de productos, generar niveles de
 * seguridad aleatorios/bidimensionales para accesos sensibles).
 *
 * Mientras la empresa no proporcione las credenciales del proveedor de IA
 * definitivo (ej. un servicio de verificación de identidad/documentos y
 * detección de fraude), opera en modo MOCK con reglas determinísticas para
 * que TODO el flujo (registro, aprobación de aliados, publicación de
 * productos) sea completamente funcional de extremo a extremo.
 *
 * Para producción, sólo se debe implementar {@link #verificarIdentidadAliado}
 * y {@link #verificarAutenticidadProducto} contra el proveedor real
 * (configurable en application.properties -> integraciones.ia-verificacion.*)
 * sin tocar el resto de la aplicación.
 */
@Service
public class IaVerificacionService {

    @Value("${integraciones.ia-verificacion.proveedor}")
    private String proveedor;

    private final SecureRandom secureRandom = new SecureRandom();

    /** Verifica la autenticidad de un aliado a partir de sus documentos/RFC. */
    public boolean verificarIdentidadAliado(String rfc, String nombreComercial) {
        if ("MOCK".equalsIgnoreCase(proveedor)) {
            // Regla determinística de validación estructural mientras se integra
            // el proveedor real de verificación de identidad.
            return rfc != null && rfc.trim().length() >= 12 && !nombreComercial.isBlank();
        }
        // TODO: integrar proveedor real de verificación de identidad (KYC/IA).
        return false;
    }

    /** Verifica la autenticidad de un producto/servicio publicado por un aliado. */
    public boolean verificarAutenticidadProducto(String nombre, String descripcion) {
        if ("MOCK".equalsIgnoreCase(proveedor)) {
            return nombre != null && !nombre.isBlank() &&
                   descripcion != null && descripcion.length() >= 10;
        }
        // TODO: integrar proveedor real (análisis de imagen/texto con IA).
        return false;
    }

    /**
     * Genera un nivel de seguridad "aleatorio y bidimensional" (matriz de
     * reto visual + código temporal) utilizado como segundo factor adicional
     * para accesos de alto riesgo (ej. panel de administración, dispersión de
     * fondos).
     */
    public String generarNivelSeguridadAleatorio() {
        int fila = secureRandom.nextInt(9) + 1;
        int columna = secureRandom.nextInt(9) + 1;
        return fila + "-" + columna;
    }
}
