package ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto;

import lombok.Data;
import java.util.List;

@Data
public class CrearRutaRequest {

    // El ID de la solicitud a la que pertenece esta ruta
    private Long idSolicitud;

    // La lista de tramos que componen la ruta
    private List<TramoDTO> tramos;

    @Data
    public static class TramoDTO {
        private Long idDepositoOrigen;
        private Long idDepositoDestino;
        private Long idTipoTramo;
    }

}
