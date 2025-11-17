package com.transporte.ms_solicitudes.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SolicitudResponseDTO {
    // Datos del ejemplo de entrega inicial
    private Long idSolicitud;
    private String estadoActual;
    private BigDecimal costoEstimado;
    
    // Datos útiles adicionales
    private String nombreCliente;
    private BigDecimal costoFinal;
    private BigDecimal tiempoEstimado;
    private BigDecimal tiempoReal;
}