package ar.edu.utn.frc.tpi.grupo148.ms_rutas.infraestructura.controladores;

import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.RutaService;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.CrearRutaRequest;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.TarifaDTO;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.CostoTrasladoDTO;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.TramoDTO;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio.Ruta;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.repositorios.RutaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// --- IMPORTACIONES AÑADIDAS ---
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.GoogleMapsClient;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.CoordenadasRequest;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.DistanciaDTO;
import java.math.BigDecimal;
import reactor.core.publisher.Mono;
// --- FIN DE IMPORTACIONES AÑADIDAS ---

@RestController
@RequestMapping("/rutas")
public class RutaController {

    @Autowired
    private RutaService rutaService;

    @Autowired
    private RutaRepository rutaRepository;

    @Autowired
    private org.springframework.web.reactive.function.client.WebClient.Builder webClientBuilder;

    // --- CAMPO AÑADIDO ---
    // Inyecta el GoogleMapsClient que ya existe en tu proyecto
    @Autowired
    private GoogleMapsClient googleMapsClient;
    // --- FIN DE CAMPO AÑADIDO ---


    /**
     * Endpoint para crear una nueva ruta tentativa con todos sus tramos.
     * ... (resto de los comentarios)
     */
    @PostMapping
    @PreAuthorize("hasAuthority('OPERADOR')")
    public ResponseEntity<Ruta> crearRuta(@RequestBody CrearRutaRequest request) {
        Ruta nuevaRuta = rutaService.crearRutaTentativa(request);
        // Devuelve 201 Created
        return ResponseEntity.status(201).body(nuevaRuta);
    }

    /**
     * Endpoint para listar rutas (paginado).
     * ... (resto de los comentarios)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('OPERADOR')")
    public ResponseEntity<Page<Ruta>> listarRutas(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Ruta> resultado = rutaRepository.findAll(pageable);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Endpoint para obtener rutas asociadas a una solicitud.
     * ... (resto de los comentarios)
     */
    @GetMapping("/solicitud/{idSolicitud}")
    @PreAuthorize("hasAuthority('OPERADOR')")
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
    @PreAuthorize("hasAuthority('OPERADOR')")
    public ResponseEntity<Ruta> obtenerRutaPorId(@PathVariable Long id) {
        return rutaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Marcar una ruta como asignada (OPERADOR).
     */
    @PostMapping("/{id}/asignar")
    @PreAuthorize("hasAuthority('OPERADOR')")
    public ResponseEntity<Ruta> asignarRuta(@PathVariable Long id) {
        Ruta rutaAsignada = rutaService.asignarRuta(id);
        return ResponseEntity.ok(rutaAsignada);
    }

    /**
     * Proxy simple para listar contenedores pendientes desde ms-solicitudes.
     */
    @GetMapping("/contenedores-pendientes")
    @PreAuthorize("hasAuthority('OPERADOR')")
    public ResponseEntity<Object> contenedoresPendientes() {
        try {
            String url = "http://ms-solicitudes:8081/contenedores/pendientes";
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

    /**
     * Endpoint público para que ms-solicitudes obtenga el costo real desglosado de un traslado.
     * ... (resto de los comentarios)
     */
    @GetMapping("/solicitud/{idSolicitud}/costo-real")
    public ResponseEntity<CostoTrasladoDTO> obtenerCostoTrasladoRealPorSolicitud(@PathVariable Long idSolicitud) {
        try {
            CostoTrasladoDTO costo = rutaService.obtenerCostoTrasladoRealPorSolicitud(idSolicitud);
            return ResponseEntity.ok(costo);
        } catch (Exception e) {
            return ResponseEntity.status(502).body(null);
        }
    }

    /**
     * Endpoint público para que ms-solicitudes obtenga la tarifa vigente.
     * ... (resto de los comentarios)
     */
    @GetMapping("/tarifas")
    public ResponseEntity<TarifaDTO> obtenerTarifasPublicas() {
        try {
            TarifaDTO dto = rutaService.obtenerTarifas();
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(502).body(null);
        }
    }

    /**
     * Endpoint público para que ms-camiones u otros servicios obtengan los tramos
     * ... (resto de los comentarios)
     */
    @GetMapping("/patente/{patenteCamion}/tramos")
    public ResponseEntity<List<TramoDTO>> obtenerTramosAsignadosPorPatente(
            @PathVariable String patenteCamion) {
        try {
            List<TramoDTO> tramos = rutaService.obtenerTramosAsignadosPorPatente(patenteCamion);
            if (tramos == null || tramos.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(tramos);
        } catch (Exception e) {
            return ResponseEntity.status(502).body(null);
        }
    }

    /**
     * Endpoint público para que ms-solicitudes obtenga el costo real desglosado de un traslado.
     * ... (resto de los comentarios)
     */
    @GetMapping("/ruta/{idRuta}/costo-real")
    public ResponseEntity<CostoTrasladoDTO> obtenerCostoTrasladoReal(@PathVariable Long idRuta) {
        try {
            CostoTrasladoDTO costo = rutaService.obtenerCostoTrasladoReal(idRuta);
            return ResponseEntity.ok(costo);
        } catch (Exception e) {
            return ResponseEntity.status(502).body(null);
        }
    }


    // --- ENDPOINT AÑADIDO ---
    /**
     * Endpoint para que ms-solicitudes calcule la distancia de una ruta.
     * Este era el endpoint que faltaba y causaba el 503.
     */
    @PostMapping("/distancia")
    public Mono<DistanciaDTO> obtenerDistancia(@RequestBody CoordenadasRequest request) {
        String origen = request.getOrigenLatitud() + "," + request.getOrigenLongitud();
        String destino = request.getDestinoLatitud() + "," + request.getDestinoLongitud();

        // Llama al cliente de Google Maps que ya tenías
        return googleMapsClient.getDistanciaEnMetros(origen, destino)
                .map(distanciaEnMetros -> {
                    // El DTO espera BigDecimal, y Google solo nos da distancia (no duración)
                    return new DistanciaDTO(
                            new BigDecimal(distanciaEnMetros),
                            BigDecimal.ZERO // Devolvemos 0 para la duración por ahora
                    );
                });
    }
    // --- FIN DE ENDPOINT AÑADIDO ---
}