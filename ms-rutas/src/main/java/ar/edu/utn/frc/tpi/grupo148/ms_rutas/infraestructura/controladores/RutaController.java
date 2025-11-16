package ar.edu.utn.frc.tpi.grupo148.ms_rutas.infraestructura.controladores;

import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.RutaService;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.CrearRutaRequest;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio.Ruta;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.repositorios.RutaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rutas")
public class RutaController {

    @Autowired
    private RutaService rutaService;

    @Autowired
    private RutaRepository rutaRepository;

    @Autowired
    private org.springframework.web.reactive.function.client.WebClient.Builder webClientBuilder;

    /**
     * Endpoint para crear una nueva ruta tentativa con todos sus tramos.
     * Calcula distancias y costos estimados.
     * Solo accesible por el rol 'OPERADOR'.
     *
     */
    @PostMapping
    @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<Ruta> crearRuta(@RequestBody CrearRutaRequest request) {
        Ruta nuevaRuta = rutaService.crearRutaTentativa(request);
        // Devuelve 201 Created
        return ResponseEntity.status(201).body(nuevaRuta);
    }

    /**
     * Endpoint para listar rutas (paginado).
     * Solo accesible por el rol 'OPERADOR'.
     */
    @GetMapping
    @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<Page<Ruta>> listarRutas(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Ruta> resultado = rutaRepository.findAll(pageable);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Endpoint para obtener rutas asociadas a una solicitud.
     * Solo accesible por el rol 'OPERADOR'.
     */
    @GetMapping("/solicitud/{idSolicitud}")
    @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<java.util.List<Ruta>> obtenerRutasPorSolicitud(@PathVariable Long idSolicitud) {
        java.util.List<Ruta> rutas = rutaRepository.findByIdSolicitud(idSolicitud);
        if (rutas == null || rutas.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(rutas);
    }

    /**
     * Obtener detalle de una ruta por id
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<Ruta> obtenerRutaPorId(@PathVariable Long id) {
        return rutaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Marcar una ruta como asignada (OPERADOR).
     */
    @PostMapping("/{id}/asignar")
    @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<Ruta> asignarRuta(@PathVariable Long id) {
        Ruta rutaAsignada = rutaService.asignarRuta(id);
        return ResponseEntity.ok(rutaAsignada);
    }

    /**
     * Proxy simple para listar contenedores pendientes desde ms-solicitudes.
     */
    @GetMapping("/contenedores-pendientes")
    @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<Object> contenedoresPendientes() {
        try {
            String url = "http://ms-solicitudes:8082/contenedores/pendientes";
            Object cuerpo = webClientBuilder.build().get().uri(url).retrieve().bodyToMono(Object.class).block(java.time.Duration.ofSeconds(5));
            return ResponseEntity.ok(cuerpo);
        } catch (Exception e) {
            return ResponseEntity.status(502).body("No se pudo obtener contenedores pendientes: " + e.getMessage());
        }
    }

    // Aquí podrías agregar endpoints GET para consultar rutas
    // @GetMapping
    // @PreAuthorize("hasRole('OPERADOR')")
    // public ResponseEntity<List<Ruta>> obtenerRutas() { ... }
}