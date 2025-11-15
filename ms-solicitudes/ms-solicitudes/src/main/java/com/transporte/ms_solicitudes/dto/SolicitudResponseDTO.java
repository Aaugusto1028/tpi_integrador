package com.transporte.ms_solicitudes.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class SolicitudResponseDTO {
    private Long idSolicitud;
    private String nombreCliente;
    private String estadoActual;
    private BigDecimal costoEstimado;
    // Agregá aquí otros datos que quieras mostrar en la respuesta
}