package ar.edu.utn.frc.tpi.grupo148.ms_rutas.repositorios;

import ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio.Ruta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RutaRepository extends JpaRepository<Ruta, Long> {
    // Aquí podés agregar métodos de búsqueda personalizados si los necesitás
    // Por ejemplo: List<Ruta> findByIdSolicitud(Long idSolicitud);
}