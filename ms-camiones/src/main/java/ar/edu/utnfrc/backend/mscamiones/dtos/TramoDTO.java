package ar.edu.utnfrc.backend.mscamiones.dtos;

// Usamos Lombok para simplificar el código. Asegúrate de tener la dependencia en tu pom.xml
// import lombok.Data; 

/**
 * DTO que representa un Tramo devuelto por el Microservicio de Rutas.
 * Nota: Adapta los atributos para que coincidan con la respuesta real de ms-rutas.
 */
// @Data // Si usas Lombok
public class TramoDTO {
    private Long id;
    private String origen;
    private String destino;
    private String estado;
    private String patenteCamionAsignado; // Podría no ser necesario si ya lo sabes
    
    // (Añade los constructores, getters y setters si no usas Lombok)
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getPatenteCamionAsignado() { return patenteCamionAsignado; }
    public void setPatenteCamionAsignado(String patenteCamionAsignado) { this.patenteCamionAsignado = patenteCamionAsignado; }
}
