package ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO que representa un Tramo con todos los datos necesarios
 * para que ms-camiones o ms-solicitudes puedan calcular costos y tiempos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TramoDTO {
    private Long id;
    private String origen;                      // Nombre depósito origen
    private String destino;                     // Nombre depósito destino
    private String estado;                      // Estado del tramo (ESTIMADO, ASIGNADO, etc)
    private String patenteCamionAsignado;       // Patente del camión asignado
    private Double distanciaKm;                 // Distancia en km del tramo
    private BigDecimal costoAproximado;         // Costo estimado del tramo
    private BigDecimal costoReal;               // Costo real (si ya fue finalizado)
    private LocalDateTime fechaHoraInicio;      // Inicio del viaje en este tramo
    private LocalDateTime fechaHoraFin;         // Fin del viaje en este tramo
    private BigDecimal precioEstadiaOrigen;     // Precio de estadía en depósito origen
    private BigDecimal precioEstadiaDestino;    // Precio de estadía en depósito destino
}
