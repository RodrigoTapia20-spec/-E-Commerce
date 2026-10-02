package com.jjm.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Objetos de respuesta de la API. Se usan en lugar de devolver las entidades JPA
 * directamente, para no exponer datos internos (hash de contraseña, tokens...) ni
 * romper la serialización por relaciones perezosas o circulares.
 */
public final class Vistas {
    private Vistas() {}

    public record AliadoView(Integer id, String nombreComercial, String causaSocial,
                             Double calificacionPromedio, long totalCalificaciones, String nivel) {}

    public record MediaView(String tipo, String url, Integer duracionSegundos, Boolean conAudio) {}

    public record PaqueteriaView(Integer id, String nombre, Integer diasMin, Integer diasMax,
                                 BigDecimal costoBase, BigDecimal costoPorKg) {}

    public record PromocionResumen(Integer id, String titulo, BigDecimal porcentajeDescuento, String tipoTemporada,
                                   boolean esCausaSocial, String fundacionBeneficiaria, BigDecimal porcentajeDonacion,
                                   LocalDate fechaFin, BigDecimal precioPromocional) {}

    public record ProductoView(Integer id, String tipo, String nombre, String descripcion, BigDecimal precio,
                               BigDecimal pesoKg, Integer existencia, boolean verificadoIa, String estatus,
                               Integer idCategoria, String categoria, AliadoView aliado, List<MediaView> media,
                               List<PaqueteriaView> paqueterias, PromocionResumen promocion) {}

    public record PromocionView(Integer id, String titulo, String descripcion, BigDecimal porcentajeDescuento,
                                String tipoTemporada, boolean esCausaSocial, String fundacionBeneficiaria,
                                BigDecimal porcentajeDonacion, LocalDate fechaInicio, LocalDate fechaFin,
                                String estatus, String vendedor, List<Integer> idsProductos) {}

    public record EnvioView(Integer id, String paqueteria, String numeroGuia, BigDecimal costo,
                            Integer tiempoEntregaDias, LocalDate fechaEstimada, String estatus) {}

    public record DetalleView(Integer idDetalle, Integer idProducto, String nombre, String imagen, int cantidad,
                              String modalidad, BigDecimal precioLista, BigDecimal precioCobrado,
                              BigDecimal porcentajeDescuento, String fundacionBeneficiaria, BigDecimal montoCausa,
                              Integer idAliado, String vendedor, BigDecimal importe,
                              boolean puedeCalificar, boolean calificadoVendedor, boolean calificadoProducto,
                              EnvioView envio) {}

    public record ResenaView(Integer id, Integer idProducto, String cliente, int calificacion,
                             String comentario, LocalDateTime fecha) {}

    public record PedidoView(Integer id, LocalDateTime fecha, String estatus, BigDecimal subtotal,
                             BigDecimal costoEnvio, BigDecimal total, BigDecimal montoCausa,
                             String direccionEnvio, String cpEnvio, boolean facturado, List<DetalleView> detalles) {}

    public record EnvioDistView(Integer id, Integer idPedido, String cliente, String direccion, String producto,
                                int cantidad, String vendedor, String paqueteria, String numeroGuia,
                                LocalDate fechaEstimada, String estatus) {}

    public record FacturaView(Integer id, Integer idPedido, String folio, String rfcReceptor, String razonSocial,
                              String regimenFiscal, String usoCfdi, String cpFiscal, String correoEnvio,
                              BigDecimal subtotal, BigDecimal iva, BigDecimal total, String estatus,
                              LocalDateTime fechaEmision) {}

    public record CalificacionView(Integer id, String cliente, int calificacion, String comentario,
                                   LocalDateTime fecha) {}

    public record QuejaView(Integer id, String tipo, String asunto, String descripcion, String estatus,
                            String respuesta, String usuario, LocalDateTime fechaCreacion) {}

    public record MensajeView(String emisor, String mensaje, LocalDateTime fecha) {}

    public record ConversacionView(Long id, List<MensajeView> mensajes) {}

    public record UsuarioView(Integer id, String nombre, String apellidos, String correo, String rol,
                              String estatus, LocalDateTime fechaRegistro) {}

    public record AliadoAdminView(Integer id, String nombreComercial, String correo, String rfc,
                                  String estatusVerificacion, BigDecimal porcentajeConvenio) {}

    public record CuentaCobroView(String titular, String banco, String tipoCuenta,
                                  String numeroEnmascarado, LocalDateTime fechaActualizacion) {}

    public record PagoRecibidoView(Integer idDispersion, Integer idPedido, LocalDateTime fecha,
                                   BigDecimal montoRecibido, BigDecimal montoDonado, String estatus) {}
}
