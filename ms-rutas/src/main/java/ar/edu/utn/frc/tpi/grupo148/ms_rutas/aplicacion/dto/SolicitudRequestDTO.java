package ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto;


import lombok.Data;

@Data
public class SolicitudRequestDTO {
    // Datos del Cliente (REQ 1.2)
    private String clienteDni;

    // Datos del Contenedor (REQ 1.1)
    private Double pesoContenedor;
    private Double volumenContenedor;
    
    // Coordenadas (REQ 32, 33)
    private Double origenLatitud;
    private Double origenLongitud;
    private Double destinoLatitud;
    private Double destinoLongitud;
}