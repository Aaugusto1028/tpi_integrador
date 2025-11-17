package com.transporte.ms_solicitudes.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PromediosDTO {
    // REQ 105: "valores promedio entre los camiones elegibles"
    private BigDecimal costoPromedioPorKm;
    private BigDecimal consumoPromedioPorKm;
}