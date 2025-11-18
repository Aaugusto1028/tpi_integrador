package ar.edu.utnfrc.backend.mscamiones.controllers;

import ar.edu.utnfrc.backend.mscamiones.dtos.TramoDTO;
import ar.edu.utnfrc.backend.mscamiones.dtos.PromediosDTO;
import ar.edu.utnfrc.backend.mscamiones.dtos.CamionDetalleDTO;
import ar.edu.utnfrc.backend.mscamiones.models.Camion;
import ar.edu.utnfrc.backend.mscamiones.services.CamionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/camiones")
@Tag(name = "Camiones", description = "Gestión de camiones y asignación a transportistas")
public class CamionController {

    @Autowired
    private CamionService camionService; // Inyección del Servicio

    // Endpoint: GET /camiones (Roles: Operador)
    @GetMapping
    @PreAuthorize("hasAuthority('OPERADOR')")
    @Operation(summary = "Listar camiones", description = "Obtiene un listado de todos los camiones, opcionalmente filtrados por disponibilidad")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Listado de camiones obtenido exitosamente"),
        @ApiResponse(responseCode = "403", description = "No autorizado (Requiere rol OPERADOR)")
    })
    public ResponseEntity<List<Camion>> getAllCamiones(
            @RequestParam(required = false) Optional<Boolean> disponibilidad) {
        List<Camion> camiones = camionService.findAll(disponibilidad);
        return ResponseEntity.ok(camiones);
    }

    // Endpoint: POST /camiones (Roles: Operador)
    @PostMapping
    @PreAuthorize("hasAuthority('OPERADOR')")
    @Operation(summary = "Crear un nuevo camión", description = "Crea un nuevo camión en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Camión creado exitosamente", 
                    content = @Content(schema = @Schema(implementation = Camion.class))),
        @ApiResponse(responseCode = "403", description = "No autorizado (Requiere rol OPERADOR)"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos en la solicitud")
    })
    public ResponseEntity<Camion> createCamion(@RequestBody Camion camion) {
        Camion nuevoCamion = camionService.save(camion);
        return new ResponseEntity<>(nuevoCamion, HttpStatus.CREATED);
    }

    // --- Endpoint: GET /camiones/buscar-apto (Public - for inter-service communication) ---
    @GetMapping("/buscar-apto")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Buscar camiones aptos", description = "Busca camiones que cumplan con los requisitos de peso y volumen especificados (acceso público para comunicación entre servicios)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Listado de camiones aptos obtenido exitosamente"),
        @ApiResponse(responseCode = "400", description = "Parámetros de búsqueda inválidos")
    })
    public ResponseEntity<List<Camion>> getCamionesAptos(
            @RequestParam Double peso,
            @RequestParam Double volumen) {
        
        List<Camion> camionesAptos = camionService.findAptos(peso, volumen);
        return ResponseEntity.ok(camionesAptos);
    }

    // --- Endpoint: GET /camiones/transportistas/me/tramos (Roles: Transportista) ---
    @GetMapping("/transportistas/me/tramos")
    @PreAuthorize("hasAuthority('TRANSPORTISTA')")
    @Operation(summary = "Obtener tramos del transportista", description = "Obtiene todos los tramos asignados al transportista autenticado")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tramos obtenidos exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No autorizado (Requiere rol TRANSPORTISTA)"),
        @ApiResponse(responseCode = "400", description = "No se pudo determinar el camión del transportista")
    })
    public ResponseEntity<List<TramoDTO>> getTramosTransportista(Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String jwtToken = null;
        if (authentication instanceof JwtAuthenticationToken) {
            Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
            jwtToken = jwt.getTokenValue();
        }
        
        if (jwtToken == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); 
        }

        String patenteCamionAsignado = authentication.getName();
        if (patenteCamionAsignado == null || patenteCamionAsignado.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        List<TramoDTO> tramos = camionService.getTramosPorTransportista(patenteCamionAsignado, jwtToken);

        return ResponseEntity.ok(tramos);
    }

    // Endpoint: GET /camiones/{patente} (Public - for inter-service communication)
    @GetMapping("/{patente}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Obtener camión por patente", description = "Obtiene los detalles de un camión específico identificado por su patente (acceso público para comunicación entre servicios)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Camión obtenido exitosamente", 
                    content = @Content(schema = @Schema(implementation = Camion.class))),
        @ApiResponse(responseCode = "404", description = "Camión no encontrado")
    })
    public ResponseEntity<Camion> getCamionById(@PathVariable String patente) {
        Optional<Camion> camion = camionService.findById(patente);
        return camion.map(ResponseEntity::ok)
                     .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Endpoint: GET /camiones/promedios?peso=...&volumen=...
    @GetMapping("/promedios")
    @PreAuthorize("hasAuthority('OPERADOR')")
    @Operation(summary = "Obtener promedios de camiones", description = "Calcula y obtiene promedios de características de camiones aptos para peso y volumen especificados")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Promedios calculados exitosamente", 
                    content = @Content(schema = @Schema(implementation = PromediosDTO.class))),
        @ApiResponse(responseCode = "403", description = "No autorizado (Requiere rol OPERADOR)"),
        @ApiResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    public ResponseEntity<PromediosDTO> getPromedios(
            @RequestParam("peso") Double peso,
            @RequestParam("volumen") Double volumen) {

        PromediosDTO promedios = camionService.obtenerPromedios(peso, volumen);
        return ResponseEntity.ok(promedios);
    }

    /**
     * Endpoint: GET /camiones/detalle/{patente}
     * Expone datos específicos de un camión para que ms-rutas calcule costos reales.
     * No requiere autenticación para que ms-rutas pueda acceder sin token.
     */
    @GetMapping("/detalle/{patente}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Obtener detalles del camión", description = "Obtiene información detallada de un camión para cálculo de costos (acceso público)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Detalles obtenidos exitosamente", 
                    content = @Content(schema = @Schema(implementation = CamionDetalleDTO.class))),
        @ApiResponse(responseCode = "404", description = "Camión no encontrado")
    })
    public ResponseEntity<CamionDetalleDTO> getDetalleCamion(@PathVariable String patente) {
        CamionDetalleDTO detalle = camionService.obtenerDetalleCamion(patente);
        if (detalle == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detalle);
    }
}