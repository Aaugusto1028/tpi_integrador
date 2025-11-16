package com.transporte.ms_solicitudes.controller;

import com.transporte.ms_solicitudes.dto.EstadoDTO;
import com.transporte.ms_solicitudes.dto.SolicitudRequestDTO;
import com.transporte.ms_solicitudes.dto.SolicitudResponseDTO;
import com.transporte.ms_solicitudes.service.SolicitudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // <-- ¡NUEVO IMPORT!
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/solicitudes") 
public class SolicitudController {

    @Autowired
    private SolicitudService solicitudService;

    // Endpoint: POST /solicitudes
    // Rol: Cliente
    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')") // <-- ¡NUEVO! Solo el rol CLIENTE puede crear
    public ResponseEntity<SolicitudResponseDTO> crearSolicitud(@RequestBody SolicitudRequestDTO request) {
        SolicitudResponseDTO response = solicitudService.crearSolicitud(request);
        return ResponseEntity.ok(response);
    }

    // Endpoint: GET /solicitudes/{id}/estado
    // Rol: Cliente
    @GetMapping("/{id}/estado")
    @PreAuthorize("hasRole('CLIENTE')") // <-- ¡NUEVO! Solo el rol CLIENTE puede ver su estado
    public ResponseEntity<EstadoDTO> consultarEstado(@PathVariable Long id) {
        EstadoDTO estado = solicitudService.consultarEstadoSolicitud(id);
        return ResponseEntity.ok(estado);
    }
}