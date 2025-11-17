package com.transporte.ms_solicitudes.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SolicitudRequestDTO {
    // Datos del Cliente (REQ 1.2)
    private String clienteDni;

    // Datos del Contenedor (REQ 1.1)
    private BigDecimal pesoContenedor;
    private BigDecimal volumenContenedor;
    
    // Coordenadas (REQ 32, 33)
    private BigDecimal origenLatitud;
    private BigDecimal origenLongitud;
    private BigDecimal destinoLatitud;
    private BigDecimal destinoLongitud;
}