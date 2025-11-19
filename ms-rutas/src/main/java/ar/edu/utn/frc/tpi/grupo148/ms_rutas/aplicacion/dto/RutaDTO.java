package ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para representar una Ruta con todos sus detalles incluyendo tramos.
 * Utilizado para devolver una ruta consultada por el Operador con:
 * - Todos los tramos asociados
 * - Costo estimado de cada tramo
 * - Tiempo estimado derivado de distancia/velocidad
 * - Estado de cada tramo
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RutaDTO {
    private Long id;
    private Long idSolicitud;
    private Integer cantidadTramos;
    private Integer cantidadDepositos;
    private Boolean asignada;
    
    // Costo total estimado (suma de todos los tramos)
    private BigDecimal costoEstimadoTotal;
    
    // Tiempo estimado total (suma de todos los tiempos de tramos)
    private Double tiempoEstimadoTotal;
    
    // Lista de tramos con todos sus detalles
    private List<TramoDetalleDTO> tramos;
    
    /**
     * DTO interno para detalles de un tramo
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TramoDetalleDTO {
        private Long id;
        private String depositoOrigen;
        private String depositoDestino;
        private String tipoTramo;
        private String estado;
        private Double distanciaKm;
        private BigDecimal costoAproximado;
        private Double tiempoEstimadoHoras; // distancia / velocidad promedio
        private String patenteCamionAsignado;
        private BigDecimal costoReal;
        private String fechaHoraInicio;
        private String fechaHoraFin;
    }
}
