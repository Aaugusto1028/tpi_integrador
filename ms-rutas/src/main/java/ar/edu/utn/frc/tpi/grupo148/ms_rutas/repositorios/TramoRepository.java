package ar.edu.utn.frc.tpi.grupo148.ms_rutas.repositorios;

import ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio.Tramo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TramoRepository extends JpaRepository<Tramo, Long> {
    // Aquí podés agregar métodos de búsqueda personalizados si los necesitás
    org.springframework.data.domain.Page<Tramo> findByPatenteCamionAsignadoAndEstadoTramoId(String patente, Long estadoId, org.springframework.data.domain.Pageable pageable);
}