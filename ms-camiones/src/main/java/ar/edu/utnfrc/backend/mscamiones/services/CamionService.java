package com.utn.backend.mscamiones.services;

import com.utn.backend.mscamiones.models.Camion;
import com.utn.backend.mscamiones.repositories.CamionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service // Indica a Spring que esta clase contiene lógica de negocio y debe ser administrada.
public class CamionService {

    // 1. Inyección del Repositorio
    // Spring inyectará automáticamente una instancia de CamionRepository aquí.
    @Autowired 
    private CamionRepository repository;

    // --- Métodos CRUD Básicos ---

    // 2. Crear/Actualizar
    public Camion save(Camion camion) {
        // En un caso real, aquí iría la validación de capacidad (Regla de negocio 11: [cite: 1568])
        return repository.save(camion); 
    }

    // 3. Leer todos (con filtro de disponibilidad)
    public List<Camion> findAll(Optional<Boolean> disponibilidad) {
        if (disponibilidad.isPresent()) {
            // Usa el método que definimos en la interfaz del repositorio
            return repository.findByDisponibilidad(disponibilidad.get());
        }
        // Si no hay filtro, trae todos
        return repository.findAll();
    }
    
    // 4. Leer por Patente (PK)
    public Optional<Camion> findById(String patente) {
        return repository.findById(patente);
    }
    
    // 5. Eliminar por Patente
    public void deleteById(String patente) {
        repository.deleteById(patente);
    }

    // --- Lógica de Negocio Específica (Regla: Camiones aptos) ---
    /*
    * Método clave que el microservicio de Solicitudes/Rutas necesitará consumir.
    * Regla: Un camión no puede transportar contenedores que superen su peso o volumen máximo. [cite: 1611]
    */
    public List<Camion> findAptos(Double pesoRequerido, Double volumenRequerido) {
        // En una implementación avanzada, esto usaría un QueryMethod o @Query más complejo, 
        // pero por simplicidad, simularemos un filtro en memoria. 
        // Idealmente, se consultaría a la BD usando un método como findByCapacidadPesoGreaterThanAndCapacidadVolumenGreaterThan...
        
        List<Camion> disponibles = repository.findByDisponibilidad(true);
        
        return disponibles.stream()
                .filter(c -> c.getCapacidadPeso() >= pesoRequerido)
                .filter(c -> c.getCapacidadVolumen() >= volumenRequerido)
                .toList();
    }
}
