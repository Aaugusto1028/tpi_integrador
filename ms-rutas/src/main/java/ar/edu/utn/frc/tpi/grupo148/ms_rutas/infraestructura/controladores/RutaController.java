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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
// --- FIN DE IMPORTACIONES AÑADIDAS ---

@RestController
@RequestMapping("/rutas")
@Tag(name = "Rutas", description = "Gestión de rutas de transporte")

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
     */
    @PostMapping
    @PreAuthorize("hasAuthority('OPERADOR')")
    @Operation(summary = "Crear una nueva ruta", description = "Crea una ruta tentativa con todos sus tramos asociados")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Ruta creada exitosamente", 
                    content = @Content(schema = @Schema(implementation = Ruta.class))),
        @ApiResponse(responseCode = "403", description = "No autorizado (Requiere rol OPERADOR)"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos en la solicitud")
    })
    public ResponseEntity<Ruta> crearRuta(@RequestBody CrearRutaRequest request) {
        Ruta nuevaRuta = rutaService.crearRutaTentativa(request);
        return ResponseEntity.status(201).body(nuevaRuta);
    }

    /**
     * Endpoint para listar rutas (paginado).
     */
    @GetMapping
    @PreAuthorize("hasAuthority('OPERADOR')")
    @Operation(summary = "Listar rutas", description = "Obtiene un listado paginado de todas las rutas")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Listado de rutas obtenido exitosamente"),
        @ApiResponse(responseCode = "403", description = "No autorizado (Requiere rol OPERADOR)")
    })
    public ResponseEntity<Page<Ruta>> listarRutas(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Ruta> resultado = rutaRepository.findAll(pageable);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Endpoint para obtener rutas asociadas a una solicitud.
     */
    @GetMapping("/solicitud/{idSolicitud}")
    @PreAuthorize("hasAuthority('OPERADOR')")
    @Operation(summary = "Obtener rutas por solicitud", description = "Obtiene todas las rutas asociadas a una solicitud específica")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rutas obtenidas exitosamente"),
        @ApiResponse(responseCode = "403", description = "No autorizado (Requiere rol OPERADOR)"),
        @ApiResponse(responseCode = "204", description = "No hay rutas para esta solicitud")
    })
    public ResponseEntity<java.util.List<Ruta>> obtenerRutasPorSolicitud(@PathVariable Long idSolicitud) {
        java.util.List<Ruta> rutas = rutaRepository.findByIdSolicitud(idSolicitud);
        if (rutas == null || rutas.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(rutas);
    }

    /**
     * Obtener detalle de una ruta por id (requiere OPERADOR)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('OPERADOR')")
    @Operation(summary = "Obtener detalles de una ruta", description = "Obtiene los detalles completos de una ruta específica por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Detalles de la ruta obtenidos exitosamente", 
                    content = @Content(schema = @Schema(implementation = Ruta.class))),
        @ApiResponse(responseCode = "403", description = "No autorizado (Requiere rol OPERADOR)"),
        @ApiResponse(responseCode = "404", description = "Ruta no encontrada")
    })
    public ResponseEntity<Ruta> obtenerRutaPorId(@PathVariable Long id) {
        return rutaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Obtener detalle de una ruta por id (versión pública, sin autenticación)
     */
    @GetMapping("/publico/{id}")
    @Operation(summary = "Obtener ruta pública", description = "Obtiene los detalles de una ruta sin requerir autenticación")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Detalles de la ruta obtenidos exitosamente", 
                    content = @Content(schema = @Schema(implementation = Ruta.class))),
        @ApiResponse(responseCode = "404", description = "Ruta no encontrada")
    })
    public ResponseEntity<Ruta> obtenerRutaPorIdPublico(@PathVariable Long id) {
        return rutaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Marcar una ruta como asignada (OPERADOR).
     */
    @PostMapping("/{id}/asignar")
    @PreAuthorize("hasAuthority('OPERADOR')")
    @Operation(summary = "Asignar una ruta", description = "Marca una ruta como asignada")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ruta asignada exitosamente", 
                    content = @Content(schema = @Schema(implementation = Ruta.class))),
        @ApiResponse(responseCode = "403", description = "No autorizado (Requiere rol OPERADOR)"),
        @ApiResponse(responseCode = "404", description = "Ruta no encontrada")
    })
    public ResponseEntity<Ruta> asignarRuta(@PathVariable Long id) {
        Ruta rutaAsignada = rutaService.asignarRuta(id);
        return ResponseEntity.ok(rutaAsignada);
    }

    /**
     * Proxy simple para listar contenedores pendientes desde ms-solicitudes.
     */
    @GetMapping("/contenedores-pendientes")
    @PreAuthorize("hasAuthority('OPERADOR')")
    @Operation(summary = "Obtener contenedores pendientes", description = "Obtiene la lista de contenedores pendientes de traslado")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Listado de contenedores pendientes obtenido exitosamente"),
        @ApiResponse(responseCode = "403", description = "No autorizado (Requiere rol OPERADOR)"),
        @ApiResponse(responseCode = "502", description = "Error al obtener contenedores pendientes")
    })
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
     */
    @GetMapping("/solicitud/{idSolicitud}/costo-real")
    @Operation(summary = "Obtener costo real de un traslado", description = "Obtiene el costo desglosado y detallado de un traslado asociado a una solicitud")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Costo obtenido exitosamente", 
                    content = @Content(schema = @Schema(implementation = CostoTrasladoDTO.class))),
        @ApiResponse(responseCode = "502", description = "Error al calcular el costo")
    })
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
     */
    @GetMapping("/tarifas")
    @Operation(summary = "Obtener tarifas vigentes", description = "Obtiene las tarifas de transporte vigentes")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tarifas obtenidas exitosamente", 
                    content = @Content(schema = @Schema(implementation = TarifaDTO.class))),
        @ApiResponse(responseCode = "502", description = "Error al obtener tarifas")
    })
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
     */
    @GetMapping("/patente/{patenteCamion}/tramos")
    @Operation(summary = "Obtener tramos por patente", description = "Obtiene todos los tramos asignados a un camión identificado por su patente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tramos obtenidos exitosamente"),
        @ApiResponse(responseCode = "204", description = "No hay tramos para esta patente"),
        @ApiResponse(responseCode = "502", description = "Error al obtener tramos")
    })
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
     */
    @GetMapping("/ruta/{idRuta}/costo-real")
    @Operation(summary = "Obtener costo real de una ruta", description = "Obtiene el costo desglosado y detallado de una ruta específica")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Costo obtenido exitosamente", 
                    content = @Content(schema = @Schema(implementation = CostoTrasladoDTO.class))),
        @ApiResponse(responseCode = "502", description = "Error al calcular el costo")
    })
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