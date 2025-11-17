package ar.edu.utnfrc.backend.mscamiones.dtos;

// DTO que representa un Tramo devuelto por el Microservicio de Rutas.
// Usamos Lombok para reducir el boilerplate (getters/setters/constructores)
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TramoDTO {
    private Long id;
    private String origen;
    private String destino;
    private String estado;
    private String patenteCamionAsignado; // Podría no ser necesario si ya lo sabes
}
