package ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "TARIFAS")
@Data
public class Tarifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tarifa")
    private Long id;
    
    // Podrías agregar un nombre o clave (ej. "PRECIO_LITRO_COMBUSTIBLE")
    // para identificar qué tarifa es esta.

    @Column(name = "precio_litro", precision = 12, scale = 2)
    private BigDecimal precioLitro;
    
    // Aquí podrías agregar otros valores de tarifa mencionados en el enunciado
    // ej: costo_gestion_tramo, costo_km_base, etc.
}
