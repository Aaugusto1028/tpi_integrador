package com.transporte.ms_solicitudes.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class FinalizarSolicitudDTO {
    // DTO basado en el ejemplo de la entrega inicial
    private BigDecimal tiempoReal;
    private BigDecimal costoFinal;
    // Nota: Si se desea calcular el costo real desde ms-rutas, 
    // no se necesita pasar idRuta aquí, ya que la solicitud ya tiene un ID.
    // El servicio usará el ID de la solicitud para buscar la ruta asociada.
}