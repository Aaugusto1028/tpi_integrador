package com.transporte.ms_solicitudes.controller;

import com.transporte.ms_solicitudes.dto.EstadoDTO;
import com.transporte.ms_solicitudes.dto.SolicitudRequestDTO;
import com.transporte.ms_solicitudes.dto.SolicitudResponseDTO;
import com.transporte.ms_solicitudes.service.SolicitudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/solicitudes") // Todos los endpoints aquí empiezan con /solicitudes
public class SolicitudController {

    @Autowired
    private SolicitudService solicitudService;

    // Endpoint: POST /solicitudes
    // Rol: Cliente
    @PostMapping
    public ResponseEntity<SolicitudResponseDTO> crearSolicitud(@RequestBody SolicitudRequestDTO request) {
        SolicitudResponseDTO response = solicitudService.crearSolicitud(request);
        return ResponseEntity.ok(response);
    }

    // Endpoint: GET /solicitudes/{id}/estado
    // Rol: Cliente
    @GetMapping("/{id}/estado")
    public ResponseEntity<EstadoDTO> consultarEstado(@PathVariable Long id) {
        EstadoDTO estado = solicitudService.consultarEstadoSolicitud(id);
        return ResponseEntity.ok(estado);
    }
}