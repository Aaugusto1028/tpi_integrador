package ar.edu.utnfrc.backend.mscamiones.controllers;
import ar.edu.utnfrc.backend.mscamiones.models.Camion;
import ar.edu.utnfrc.backend.mscamiones.services.CamionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

// @RestController: Combina @Controller y @ResponseBody. Indica que devuelve datos JSON/XML directamente.
@RestController 
// @RequestMapping: Define la URI base para todos los métodos del controlador.
@RequestMapping("/camiones") 
public class CamionController {

    @Autowired
    private CamionService camionService; // Inyección de la dependencia del Servicio

    // Endpoint: GET /camiones
    // Roles permitidos: Operador
    @GetMapping
    public ResponseEntity<List<Camion>> getAllCamiones(
        @RequestParam Optional<Boolean> disponibilidad) { // Filtro opcional

        // Usar la lógica del servicio para obtener los camiones.
        List<Camion> camiones = camionService.findAll(disponibilidad);

        // Retorna la lista con código 200 OK
        return ResponseEntity.ok(camiones);
    }

    // Endpoint: POST /camiones
    // Roles permitidos: Operador
    @PostMapping
    public ResponseEntity<Camion> createCamion(@RequestBody Camion camion) {
        // En un proyecto real, la validación de capacidad (Regla 11) iría en el Service.
        Camion nuevoCamion = camionService.save(camion);
        
       // Retorna el recurso creado con código 201 Created [cite: 3480]
        return new ResponseEntity<>(nuevoCamion, HttpStatus.CREATED);
    }
    
    // Endpoint: GET /camiones/{dominio}
 // Roles permitidos: Operador [cite: 2852]
    @GetMapping("/{patente}")
    public ResponseEntity<Camion> getCamionById(@PathVariable String patente) {
        
        Optional<Camion> camion = camionService.findById(patente);
        
        // Si el camión existe, retorna 200 OK. [cite_start]Si no, retorna 404 Not Found [cite: 3480]
        return camion.map(ResponseEntity::ok)
                     .orElseGet(() -> ResponseEntity.notFound().build()); 
    }

    // Pendiente: Endpoint de Lógica de negocio para encontrar camiones aptos/libres (consumido por ms-rutas/solicitudes)
    // Pendiente: Endpoint /transportistas/me/tramos (GET) (requiere seguridad/Keycloak)
}
