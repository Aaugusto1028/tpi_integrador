package ar.edu.utn.frc.tpi.grupo148.ms_rutas.repositorios;

import ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio.Tarifa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TarifaRepository extends JpaRepository<Tarifa, Long> {
    // Podrías buscar una tarifa por su "clave" si le agregás ese campo
}