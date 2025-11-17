package com.transporte.ms_solicitudes.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TarifaDTO {
    // REQ 64: "valor del litro"
    private BigDecimal precioLitro;
    // Otros cargos que ms-rutas necesite informar (ej. estadía)
    private BigDecimal costoEstadiaDiario;
}