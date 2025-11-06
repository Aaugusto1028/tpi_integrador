package com.transporte.ms_solicitudes.repository;

import com.transporte.ms_solicitudes.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    
    // Spring Data JPA crea la consulta automáticamente basándose en el nombre
    Optional<Cliente> findByDni(String dni);
    Optional<Cliente> findByEmail(String email);
}