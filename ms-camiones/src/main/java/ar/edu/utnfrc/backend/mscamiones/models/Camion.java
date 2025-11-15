// Clase Camion.java
package com.utn.backend.mscamiones.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data; 

// @Data de Lombok agrega getters, setters, toString, etc.
@Data 
// @Entity marca la clase como persistente para Spring Data JPA 
@Entity 
public class Camion {

    // La PK según el DER es la patente/dominio [cite: 1258, 1259]
    @Id 
    private String patente; 

    // Mapeo del resto de los atributos [cite: 1261-1272]
    private String nombreTransportista;
    private String telefonoTransportista;
    private Double capacidadPeso;        // DECIMAL(10,2) [cite: 1265]
    private Double capacidadVolumen;     // DECIMAL(10,2) [cite: 1267]
    private Double consumoCombustibleKm; // DECIMAL(10,2) [cite: 1269]
    private Double costoPorKm;           // DECIMAL(12,2) [cite: 1270]
    private Boolean disponibilidad;       // BOOLEAN [cite: 1272]
    
}
