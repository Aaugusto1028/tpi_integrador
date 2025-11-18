package ar.edu.utnfrc.backend.mscamiones.controllers;

import ar.edu.utnfrc.backend.mscamiones.dtos.TramoDTO; // Import para el DTO
import ar.edu.utnfrc.backend.mscamiones.dtos.PromediosDTO;
import ar.edu.utnfrc.backend.mscamiones.dtos.CamionDetalleDTO;
import ar.edu.utnfrc.backend.mscamiones.models.Camion;
import ar.edu.utnfrc.backend.mscamiones.services.CamionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Import de Spring Security
import org.springframework.security.oauth2.jwt.Jwt; // Import para el objeto JWT
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken; // Import para el tipo de token
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/camiones")
public class CamionController {

    @Autowired
    private CamionService camionService; // Inyección del Servicio

    // Endpoint: GET /camiones (Roles: Operador)
    @GetMapping
    @PreAuthorize("hasAuthority('OPERADOR')")
    public ResponseEntity<List<Camion>> getAllCamiones(
            @RequestParam(required = false) Optional<Boolean> disponibilidad) {
        List<Camion> camiones = camionService.findAll(disponibilidad);
        return ResponseEntity.ok(camiones);
    }

    // Endpoint: POST /camiones (Roles: Operador)
    @PostMapping
    @PreAuthorize("hasAuthority('OPERADOR')")
    public ResponseEntity<Camion> createCamion(@RequestBody Camion camion) {
        Camion nuevoCamion = camionService.save(camion);
        return new ResponseEntity<>(nuevoCamion, HttpStatus.CREATED);
    }

    // --- Endpoint: GET /camiones/buscar-apto (Roles: Operador) ---
    @GetMapping("/buscar-apto")
    @PreAuthorize("hasAuthority('OPERADOR')")
    public ResponseEntity<List<Camion>> getCamionesAptos(
            @RequestParam Double peso,
            @RequestParam Double volumen) {
        
        List<Camion> camionesAptos = camionService.findAptos(peso, volumen);
        return ResponseEntity.ok(camionesAptos);
    }

    // --- Endpoint: GET /camiones/transportistas/me/tramos (Roles: Transportista) ---
    @GetMapping("/transportistas/me/tramos")
    @PreAuthorize("hasAuthority('TRANSPORTISTA')")
    public ResponseEntity<List<TramoDTO>> getTramosTransportista(Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // 1. Extraer el Token JWT (el token completo es necesario para llamar a ms-rutas)
        String jwtToken = null;
        if (authentication instanceof JwtAuthenticationToken) {
            Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
            jwtToken = jwt.getTokenValue();
        }
        
        if (jwtToken == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); 
        }

        // 2. Lógica para obtener la patente del camión del transportista autenticado
        // NOTA: Esta es una simplificación. En la realidad, usarías authentication.getName() 
        // para buscar en la BD (o un DTO de Keycloak) la patente asociada al usuario.
        String patenteCamionAsignado = authentication.getName();
        if (patenteCamionAsignado == null || patenteCamionAsignado.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // 3. Llamada al servicio (que usa el token para llamar a ms-rutas)
        List<TramoDTO> tramos = camionService.getTramosPorTransportista(patenteCamionAsignado, jwtToken);

        return ResponseEntity.ok(tramos);
    }

    // Endpoint: GET /camiones/{patente} (Public - for inter-service communication)
    @GetMapping("/{patente}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Camion> getCamionById(@PathVariable String patente) {
        Optional<Camion> camion = camionService.findById(patente);
        return camion.map(ResponseEntity::ok)
                     .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Endpoint: GET /camiones/promedios?peso=...&volumen=...
    @GetMapping("/promedios")
    @PreAuthorize("hasAuthority('OPERADOR')")
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
    public ResponseEntity<CamionDetalleDTO> getDetalleCamion(@PathVariable String patente) {
        CamionDetalleDTO detalle = camionService.obtenerDetalleCamion(patente);
        if (detalle == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detalle);
    }
}