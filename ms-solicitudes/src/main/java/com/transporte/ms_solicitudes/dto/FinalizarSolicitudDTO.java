package com.transporte.ms_solicitudes.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class FinalizarSolicitudDTO {
    // DTO basado en el ejemplo de la entrega inicial
    private BigDecimal tiempoReal;
    private BigDecimal costoFinal;
}