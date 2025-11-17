package com.transporte.ms_solicitudes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeguimientoDTO {
    // Ejemplo: "En Transito", "En Deposito"
    private String estado; 
    // Ejemplo: "Ruta 5, Km 300", "Depósito Central"
    private String ubicacion; 
}