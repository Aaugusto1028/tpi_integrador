package com.transporte.ms_solicitudes.model;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "CLIENTES")
@Data
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, unique = true, length = 20)
    private String dni;

    @Column(length = 25)
    private String telefono;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    // --- NO HAY RELACIONES @OneToMany ---
    // (No hay List<Solicitud> ni List<Contenedor>)
}