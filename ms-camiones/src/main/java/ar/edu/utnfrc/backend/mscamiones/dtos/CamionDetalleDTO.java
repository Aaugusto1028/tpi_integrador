package ar.edu.utnfrc.backend.mscamiones.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO que expone datos específicos de un camión para que otros servicios
 * (como ms-rutas) puedan calcular costos reales de traslados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CamionDetalleDTO {
    private String patente;
    private BigDecimal costoPorKm;
    private Double consumoCombustibleKm;
    private Double capacidadPeso;
    private Double capacidadVolumen;
    private Boolean disponibilidad;
}
