package com.transporte.ms_solicitudes.dto;

import lombok.Data;
import java.util.List;

@Data
public class DetallesRutaDTO {
    private List<TramoDetalleDTO> tramos;
    private List<EstadiaDetalleDTO> estadias;
}