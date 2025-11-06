package com.transporte.mssolicitudes.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "solicitudes")
@Data
public class Solicitud {
    // ... campos id, costo, etc. ...

    // --- RELACIONES OBLIGATORIAS (LADO DUEÑO DE LA FK) ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "id_contenedor", referencedColumnName = "id_contenedor", nullable = false, unique = true)
    private Contenedor contenedor;
}