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
    @PreAuthorize("hasAuthority('OPERADOR')")
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
    @PreAuthorize("hasAuthority('TRANSPORTISTA')")
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
    @PreAuthorize("hasAuthority('TRANSPORTISTA')")
    public ResponseEntity<Tramo> finalizarTramo(@PathVariable Long id) {
        Tramo tramo = rutaService.finalizarTramo(id);
        return ResponseEntity.ok(tramo);
    }

    /**
     * Endpoint para que un transportista liste sus tramos asignados (paginado).
     * Parámetros: patenteCamion (requerido), estadoId (opcional), page, size
     */
    @GetMapping
    @PreAuthorize("hasAuthority('TRANSPORTISTA')")
    public ResponseEntity<org.springframework.data.domain.Page<Tramo>> listarTramosPorPatente(
            @RequestParam(name = "patenteCamion") String patente,
            @RequestParam(name = "estadoId", required = false) Long estadoId,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        Long estadoFiltro = estadoId != null ? estadoId : 2L; // por defecto 2 = ASIGNADO
        org.springframework.data.domain.Page<Tramo> resultado = rutaService.obtenerTramosPorPatenteYEstado(patente, estadoFiltro, pageable);
        return ResponseEntity.ok(resultado);
    }
}