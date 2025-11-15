package ar.edu.utn.frc.tpi.grupo148.ms_rutas.infraestructura.controladores;

import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.RutaService;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.AsignarCamionRequest;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio.Tramo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tramos")
public class TramoController {

    @Autowired
    private RutaService rutaService;

    /**
     * Endpoint para asignar un camión a un tramo específico.
     * Solo accesible por el rol 'OPERADOR'.
     *
     */
    @PutMapping("/{id}/asignar-camion")
    @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<Tramo> asignarCamion(@PathVariable Long id, @RequestBody AsignarCamionRequest request) {
        Tramo tramo = rutaService.asignarCamionATramo(id, request);
        return ResponseEntity.ok(tramo);
    }

    /**
     * Endpoint para que un transportista inicie un tramo.
     * Solo accesible por el rol 'TRANSPORTISTA'.
     *
     */
    @PostMapping("/{id}/iniciar")
    @PreAuthorize("hasRole('TRANSPORTISTA')")
    public ResponseEntity<Tramo> iniciarTramo(@PathVariable Long id) {
        Tramo tramo = rutaService.iniciarTramo(id);
        return ResponseEntity.ok(tramo);
    }

    /**
     * Endpoint para que un transportista finalice un tramo.
     * Solo accesible por el rol 'TRANSPORTISTA'.
     *
     */
    @PostMapping("/{id}/finalizar")
    @PreAuthorize("hasRole('TRANSPORTISTA')")
    public ResponseEntity<Tramo> finalizarTramo(@PathVariable Long id) {
        Tramo tramo = rutaService.finalizarTramo(id);
        return ResponseEntity.ok(tramo);
    }
}