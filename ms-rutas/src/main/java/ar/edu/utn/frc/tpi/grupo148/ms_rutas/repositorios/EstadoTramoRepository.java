package ar.edu.utn.frc.tpi.grupo148.ms_rutas.repositorios;

import ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio.EstadoTramo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadoTramoRepository extends JpaRepository<EstadoTramo, Long> {
    // Ejemplo: buscar un estado por su nombre
    // Optional<EstadoTramo> findByNombre(String nombre);
}