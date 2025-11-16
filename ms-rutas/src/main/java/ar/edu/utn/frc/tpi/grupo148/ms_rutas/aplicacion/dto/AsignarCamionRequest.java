package ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto;

import lombok.Data;

@Data
public class AsignarCamionRequest {

    // Patente del camión a asignar
    private String patenteCamion;

    // Necesitamos que nos manden el peso y volumen del contenedor
    // para poder validarlo contra el camión.
    private Double pesoContenedor;
    private Double volumenContenedor;

}
