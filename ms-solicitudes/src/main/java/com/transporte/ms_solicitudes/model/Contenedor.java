package com.transporte.ms_solicitudes.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import lombok.NoArgsConstructor;
// ... (imports)


@Entity
@Table(name = "contenedores")
@Data
@NoArgsConstructor
public class Contenedor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contenedor")
    private Long id;

    @Column(name = "peso", precision = 12, scale = 2)
    private BigDecimal peso;

    @Column(name = "volumen", precision = 12, scale = 2)
    private BigDecimal volumen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente_asociado")
    private Cliente clienteAsociado;

    @OneToMany(mappedBy = "contenedor", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("fecha DESC") // MUY IMPORTANTE: Ordena los estados por fecha
    private List<EstadoContenedor> historialEstados;

    // --- MÉTODO NUEVO ---
    /**
     * Método helper para obtener el estado más reciente basado en la fecha.
     * Devuelve null si el historial está vacío.
     */
    @Transient // Indica a JPA que esto NO es una columna de la BD
    public EstadoContenedor getEstadoActual() {
        if (historialEstados == null || historialEstados.isEmpty()) {
            return null;
        }
        // Como la lista está ordenada por fecha descendente, el primero es el actual
        return historialEstados.get(0); 
    }
}