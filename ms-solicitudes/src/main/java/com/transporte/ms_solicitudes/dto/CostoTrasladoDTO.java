package com.transporte.ms_solicitudes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO que encapsula el desglose de costos reales de un traslado.
 * Recibido desde ms-rutas para finalizar solicitudes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CostoTrasladoDTO {
    private BigDecimal costoKm;                // Costo por km del camión asignado
    private BigDecimal costoCombustible;      // Costo combustible (consumo * precio litro)
    private BigDecimal costoEstadia;          // Costo estadía en depósitos
    private BigDecimal costoTotal;            // Suma de todos los costos
}
