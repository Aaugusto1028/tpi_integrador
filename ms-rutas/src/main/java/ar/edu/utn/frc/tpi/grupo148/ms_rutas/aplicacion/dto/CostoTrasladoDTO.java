package ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO que encapsula el desglose de costos reales de un traslado.
 * Usado para que ms-solicitudes pueda finalizar solicitudes con costo real.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CostoTrasladoDTO {
    // Desglose del costo real de un traslado
    private BigDecimal costoKm;                // Costo por km del camión asignado
    private BigDecimal costoCombustible;      // Costo combustible (consumo * precio litro)
    private BigDecimal costoEstadia;          // Costo estadía en depósitos
    private BigDecimal costoTotal;            // Suma de todos los costos
}
