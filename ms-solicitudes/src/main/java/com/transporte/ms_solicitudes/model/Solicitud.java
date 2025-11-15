package com.transporte.mssolicitudes.model; // Asegurate que el package sea el correcto

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "solicitudes")
@Data
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Long id;

    @Column(name = "costo_estimado", precision = 12, scale = 2)
    private BigDecimal costoEstimado;

    @Column(name = "costo_final", precision = 12, scale = 2)
    private BigDecimal costoFinal;

    @Column(name = "tiempo_real", length = 50)
    private String tiempoReal;

    // --- RELACIONES OBLIGATORIAS (LADO DUEÑO DE LA FK) ---

    // Esta es la relación para la columna 'id_cliente'
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    // Esta es la relación para la columna 'id_contenedor'
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "id_contenedor", referencedColumnName = "id_contenedor", nullable = false, unique = true)
    private Contenedor contenedor;
}