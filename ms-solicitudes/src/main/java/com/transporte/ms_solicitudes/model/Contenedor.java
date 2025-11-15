package com.transporte.ms_solicitudes.model;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "contenedores")
@Data
public class Contenedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contenedor")
    private Long id;

    @Column(precision = 10, scale = 2)
    private BigDecimal peso;

    @Column(precision = 10, scale = 2)
    private BigDecimal volumen;

    @OneToMany(mappedBy = "contenedor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("fecha DESC") 
    private List<EstadoContenedor> historialEstados;
    
    @Transient// No se persiste en la BD
    public EstadoContenedor getEstadoActual() {
        if (historialEstados == null || historialEstados.isEmpty()) {
            return null;
        }
        // Como la lista está ordenada por fecha DESC, el estado más nuevo es el primero
        return historialEstados.get(0); // <-- CORRECCIÓN: Faltaba el punto y coma (;)
    }
}