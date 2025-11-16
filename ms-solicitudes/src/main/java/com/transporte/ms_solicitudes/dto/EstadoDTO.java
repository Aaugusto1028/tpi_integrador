package com.transporte.ms_solicitudes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class EstadoDTO {
    private String estado;
    private LocalDateTime fecha;
}