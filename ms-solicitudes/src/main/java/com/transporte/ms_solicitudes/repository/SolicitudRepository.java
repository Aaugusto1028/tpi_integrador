package com.transporte.ms_solicitudes.repository;

import com.transporte.ms_solicitudes.model.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    // Método para buscar todas las solicitudes de un cliente
    List<Solicitud> findByCliente_Id(Long clienteId);

    // Buscar por estado (ej: "Borrador", "Programada", "Entregada")
    List<Solicitud> findByEstado(String estado);
}