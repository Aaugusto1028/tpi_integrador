package com.transporte.ms_solicitudes.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class SolicitudRequestDTO {
    private String clienteDni; // El cliente ya debe existir (por simplicidad)
    private BigDecimal pesoContenedor;
    private BigDecimal volumenContenedor;
    // Agregá aquí otros datos que necesites del cliente para la solicitud
}