package com.transporte.mssolicitudes.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor; // Es buena práctica agregar un constructor vacío para JPA
import java.time.LocalDateTime;

@Entity
@Table(name = "estado_contenedor")
@Data
@NoArgsConstructor // Lombok genera el constructor vacío
public class EstadoContenedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_contenedor")
    private Long id;

    // Los atributos de tu diagrama
    @Column(nullable = false, length = 50)
    private String nombre; // "En Tránsito", "En Depósito", "Entregado"

    @Column(nullable = false)
    private LocalDateTime fecha;

    // --- Relación para el Historial ---
    // Esta es la FK que une este registro de historial al contenedor.
    // Muchos registros de estado pertenecen a UN contenedor.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contenedor", nullable = false)
    private Contenedor contenedor;
    
    // Constructor útil para agregar nuevos estados fácilmente
    public EstadoContenedor(String nombre, LocalDateTime fecha, Contenedor contenedor) {
        this.nombre = nombre;
        this.fecha = fecha;
        this.contenedor = contenedor;
    }
}