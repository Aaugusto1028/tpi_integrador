package ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// --- IMPORTACIONES AÑADIDAS ---
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
// --- FIN DE IMPORTACIONES ---

@Entity
@Table(name = "TRAMOS")
@Data
// --- ANOTACIÓN AÑADIDA (Arregla el error de Hibernate Proxy) ---
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Tramo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tramo")
    private Long id;

    // Relación: Muchos Tramos pertenecen a una Ruta
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ruta") // Esta es la columna FK en la tabla TRAMOS
    @JsonIgnore // <-- ANOTACIÓN AÑADIDA (Arregla el bucle infinito)
    private Ruta ruta;

    // Relación: Un Tramo tiene un Depósito de origen
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_deposito_origen")
    private Deposito depositoOrigen;

    // Relación: Un Tramo tiene un Depósito de destino
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_deposito_destino")
    private Deposito depositoDestino;

    // Relación: Un Tramo tiene un Tipo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_tramo")
    private TipoTramo tipoTramo;

    // Relación: Un Tramo tiene un Estado
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_tramo")
    private EstadoTramo estadoTramo;

    @Column(name = "patente_camion_asignado")
    private String patenteCamionAsignado; // FK de Camión (otro microservicio)

    //Agregamos luego de la mejora de RutaService Impl
    @Column(name = "distancia_km")
    private Double distanciaKm;

    @Column(name = "costo_aproximado", precision = 12, scale = 2)
    private BigDecimal costoAproximado;

    @Column(name = "costo_real", precision = 12, scale = 2) // Ojo, tu DER dice (12, 21), asumí (12, 2)
    private BigDecimal costoReal;

    @Column(name = "fecha_hora_inicio")
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin")
    private LocalDateTime fechaHoraFin;
}