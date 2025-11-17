package com.transporte.ms_solicitudes.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "SOLICITUDES")
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

    @Column(name = "tiempo_real", precision = 12, scale = 2)
    private java.math.BigDecimal tiempoReal;

    @Column(name = "tiempo_estimado", precision = 12, scale = 2)
    private java.math.BigDecimal tiempoEstimado;

    @Column(name = "estado", length = 50)
    private String estado;

    @Column(name = "origen_latitud", precision = 10, scale = 6)
    private java.math.BigDecimal origenLatitud;

    @Column(name = "origen_longitud", precision = 10, scale = 6)
    private java.math.BigDecimal origenLongitud;

    @Column(name = "destino_latitud", precision = 10, scale = 6)
    private java.math.BigDecimal destinoLatitud;

    @Column(name = "destino_longitud", precision = 10, scale = 6)
    private java.math.BigDecimal destinoLongitud;

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