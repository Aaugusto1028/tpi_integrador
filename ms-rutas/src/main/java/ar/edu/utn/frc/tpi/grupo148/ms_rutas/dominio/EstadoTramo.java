package ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "ESTADO_TRAMO")
@Data
public class EstadoTramo {

    @Id
    @Column(name = "id_estado_tramo") // Usualmente no es auto-generado
    private Long id;

    @Column(name = "nombre")
    private String nombre; // Ej: "estimado", "asignado", "iniciado", "finalizado"
}
