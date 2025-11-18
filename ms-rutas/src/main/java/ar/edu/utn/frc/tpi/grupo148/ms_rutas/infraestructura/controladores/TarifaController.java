package ar.edu.utn.frc.tpi.grupo148.ms_rutas.infraestructura.controladores;

import ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio.Tarifa;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.repositorios.TarifaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tarifas")
public class TarifaController {

    @Autowired
    private TarifaRepository tarifaRepository;

    /**
     * Endpoint para obtener todas las tarifas.
     * Solo accesible por el rol 'OPERADOR'.
     *
     */
    @GetMapping
    @PreAuthorize("hasAuthority('OPERADOR')")
    public ResponseEntity<List<Tarifa>> obtenerTarifas() {
        return ResponseEntity.ok(tarifaRepository.findAll());
    }

    /**
     * Endpoint para crear una nueva tarifa (ej. valor del combustible).
     * Solo accesible por el rol 'OPERADOR'.
     *
     */
    @PostMapping
    @PreAuthorize("hasAuthority('OPERADOR')")
    public ResponseEntity<Tarifa> crearTarifa(@RequestBody Tarifa tarifa) {
        return ResponseEntity.status(201).body(tarifaRepository.save(tarifa));
    }

    /**
     * Endpoint para actualizar una tarifa (ej. precio del combustible).
     * Solo accesible por el rol 'OPERADOR'.
     *
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('OPERADOR')")
    public ResponseEntity<Tarifa> actualizarTarifa(@PathVariable Long id, @RequestBody Tarifa tarifaActualizada) {
        Tarifa tarifa = tarifaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tarifa no encontrada: " + id));

        // Actualizamos los campos modificables
        tarifa.setPrecioLitro(tarifaActualizada.getPrecioLitro());
        // (agregar más campos si los tuviera)

        return ResponseEntity.ok(tarifaRepository.save(tarifa));
    }
}