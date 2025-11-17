package com.transporte.ms_solicitudes.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RutaFinalizadaDTO {
    // DTO alternativo si decides que ms-rutas calcule el costo final
    private boolean completada;
    private BigDecimal costoTotalReal; // REQ 103
    private BigDecimal tiempoTotalReal;
}