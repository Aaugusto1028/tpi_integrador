package ar.edu.utn.frc.tpi.grupo148.ms_rutas.infraestructura.controladores;

import ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio.Deposito;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.repositorios.DepositoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/depositos")
public class DepositoController {

    // Para un CRUD simple, podemos inyectar el repositorio directamente
    @Autowired
    private DepositoRepository depositoRepository;

    /**
     * Endpoint para obtener todos los depósitos.
     * Solo accesible por el rol 'OPERADOR'.
     *
     */
    @GetMapping
    @PreAuthorize("hasAuthority('OPERADOR')")
    public ResponseEntity<List<Deposito>> obtenerTodos() {
        return ResponseEntity.ok(depositoRepository.findAll());
    }

    /**
     * Endpoint para crear un nuevo depósito.
     * Solo accesible por el rol 'OPERADOR'.
     *
     */
    @PostMapping
    @PreAuthorize("hasAuthority('OPERADOR')")
    public ResponseEntity<Deposito> crear(@RequestBody Deposito deposito) {
        return ResponseEntity.status(201).body(depositoRepository.save(deposito));
    }

    /**
     * Endpoint para actualizar un depósito existente.
     * Solo accesible por el rol 'OPERADOR'.
     *
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('OPERADOR')")
    public ResponseEntity<Deposito> actualizarDeposito(@PathVariable Long id,
            @RequestBody Deposito depositoActualizado) {
        Deposito deposito = depositoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Depósito no encontrado: " + id));

        // Actualizamos los campos modificables
        deposito.setNombre(depositoActualizado.getNombre());
        deposito.setCalle(depositoActualizado.getCalle());
        deposito.setLatitud(depositoActualizado.getLatitud());
        deposito.setLongitud(depositoActualizado.getLongitud());
        deposito.setPrecioEstadia(depositoActualizado.getPrecioEstadia());

        return ResponseEntity.ok(depositoRepository.save(deposito));
    }
}