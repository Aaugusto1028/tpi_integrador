package ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "DEPOSITOS")
@Data
public class Deposito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_deposito")
    private Long id;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "calle")
    private String calle;

    @Column(name = "id_ciudad")
    private Long idCiudad; // Asumiendo que Ciudad es gestionado por otro servicio o es un ID simple

    @Column(name = "latitud", precision = 10, scale = 6)
    private BigDecimal latitud;

    @Column(name = "longitud", precision = 10, scale = 6)
    private BigDecimal longitud;

    @Column(name = "precio_estadia", precision = 12, scale = 2)
    private BigDecimal precioEstadia;
}
