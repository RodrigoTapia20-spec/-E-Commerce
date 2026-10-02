package com.jjm.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FacturaRequest {
    @NotNull private Integer idPedido;
    @NotBlank private String rfcReceptor;
    @NotBlank private String razonSocial;
    private String usoCfdi;        // ej. G03 = Gastos en general
    private String regimenFiscal;  // ej. 601, 612, 626
    private String cpFiscal;
    private String correoEnvio;
}
