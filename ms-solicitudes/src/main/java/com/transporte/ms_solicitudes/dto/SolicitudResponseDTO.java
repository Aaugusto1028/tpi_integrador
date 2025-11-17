package com.transporte.ms_solicitudes.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SolicitudResponseDTO {
    private Long id;
    private String estado;
    private BigDecimal costoEstimado;
    private BigDecimal costoFinal;
    private BigDecimal tiempoEstimado;
    private BigDecimal tiempoReal;
}