package com.transporte.ms_solicitudes.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CamionDTO {
    private String dominio;
    private BigDecimal capacidadPeso;
    private BigDecimal capacidadVolumen;
    private BigDecimal costoPorKm;
    private BigDecimal consumoCombustibleKm;
}