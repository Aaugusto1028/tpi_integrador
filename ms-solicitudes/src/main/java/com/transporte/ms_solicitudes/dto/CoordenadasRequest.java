package com.transporte.ms_solicitudes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoordenadasRequest {
    private BigDecimal origenLatitud;
    private BigDecimal origenLongitud;
    private BigDecimal destinoLatitud;
    private BigDecimal destinoLongitud;
}