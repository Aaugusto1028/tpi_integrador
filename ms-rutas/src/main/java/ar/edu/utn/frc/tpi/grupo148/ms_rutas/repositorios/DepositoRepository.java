package ar.edu.utn.frc.tpi.grupo148.ms_rutas.repositorios;

import ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio.Deposito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepositoRepository extends JpaRepository<Deposito, Long> {
    // Spring ya te da findById(Long id) y findAll()
}