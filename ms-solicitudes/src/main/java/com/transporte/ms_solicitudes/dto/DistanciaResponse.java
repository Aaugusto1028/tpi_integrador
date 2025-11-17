package com.transporte.ms_solicitudes.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DistanciaResponse {
    // REQ 8.1: Recorrido total
    private BigDecimal distanciaMetros;
    // REQ 106: Tiempo estimado
    private BigDecimal duracionSegundos; 
}