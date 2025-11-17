package com.transporte.ms_solicitudes.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TramoDetalleDTO {
    private double distanciaKm;
    private String patenteCamionAsignado;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
}