package com.transporte.ms_solicitudes.repository;

import com.transporte.ms_solicitudes.model.EstadoContenedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadoContenedorRepository extends JpaRepository<EstadoContenedor, Long> {
    // Por ahora, los métodos básicos de JpaRepository (save, findById) son suficientes.
}