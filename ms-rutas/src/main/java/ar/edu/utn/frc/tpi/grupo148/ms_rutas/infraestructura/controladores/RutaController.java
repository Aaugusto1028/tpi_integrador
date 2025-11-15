package ar.edu.utn.frc.tpi.grupo148.ms_rutas.infraestructura.controladores;

import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.RutaService;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.CrearRutaRequest;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio.Ruta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rutas")
public class RutaController {

    @Autowired
    private RutaService rutaService;

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

    // Aquí podrías agregar endpoints GET para consultar rutas
    // @GetMapping
    // @PreAuthorize("hasRole('OPERADOR')")
    // public ResponseEntity<List<Ruta>> obtenerRutas() { ... }
}