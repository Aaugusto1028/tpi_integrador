package ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "TIPO_TRAMO")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TipoTramo {

    @Id
    @Column(name = "id_tipo_tramo") // Usualmente no es auto-generado si es catálogo
    private Long id;

    @Column(name = "descripcion")
    private String descripcion; // Ej: "deposito-destino", "origen-deposito"
}