package com.jjm.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Campaña de marketing que lanza el propio vendedor: temporada, descuento y, si aplica, causa social. */
@Data
public class PromocionRequest {
    @NotBlank private String titulo;
    private String descripcion;
    @NotNull private BigDecimal porcentajeDescuento;   // 20, 30, 40...
    private String tipoTemporada;                       // GENERAL / NAVIDAD / DIA_DE_MUERTOS / FIESTAS_PATRIAS / BUEN_FIN ...
    private Boolean esCausaSocial;
    private String fundacionBeneficiaria;               // asociación civil / fundación
    private BigDecimal porcentajeDonacion;              // % del importe que se dona (sale de la parte del vendedor)
    @NotNull private LocalDate fechaInicio;
    @NotNull private LocalDate fechaFin;
    @NotNull private List<Integer> idProductos;
}
