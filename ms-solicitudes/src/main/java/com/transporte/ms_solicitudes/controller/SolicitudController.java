package com.transporte.ms_solicitudes.controller;

import com.transporte.ms_solicitudes.dto.SeguimientoDTO;
import com.transporte.ms_solicitudes.dto.SolicitudRequestDTO;
import com.transporte.ms_solicitudes.dto.SolicitudResponseDTO;
import com.transporte.ms_solicitudes.dto.EstadoDTO; 
import com.transporte.ms_solicitudes.dto.FinalizarSolicitudDTO; // <-- IMPORTADO
import com.transporte.ms_solicitudes.service.SolicitudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/solicitudes")
public class SolicitudController {

    @Autowired
    private SolicitudService solicitudService;

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENTE')")
    public ResponseEntity<SolicitudResponseDTO> crearSolicitud(@RequestBody SolicitudRequestDTO solicitudRequest) {
        SolicitudResponseDTO solicitud = solicitudService.crearSolicitud(solicitudRequest);
        return ResponseEntity.ok(solicitud);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('OPERADOR')")
    public ResponseEntity<List<SolicitudResponseDTO>> obtenerSolicitudes(
            // CAMBIO 1: Aceptamos un parámetro de filtro opcional
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(solicitudService.listarSolicitudes(estado)); // CAMBIO 2: Pasamos el filtro
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CLIENTE', 'OPERADOR')")
    public ResponseEntity<SolicitudResponseDTO> obtenerSolicitud(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.obtenerSolicitud(id));
    }

    @GetMapping("/{id}/estado")
    @PreAuthorize("hasAnyAuthority('CLIENTE', 'OPERADOR')")
    public ResponseEntity<SeguimientoDTO> obtenerEstado(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.obtenerEstado(id));
    }

    /**
     * Endpoint que dispara el cálculo final y la finalización de la solicitud.
     * Es llamado por un Operador cuando el transporte ha concluido.
     */
    @PutMapping("/{id}/finalizar")
    @PreAuthorize("hasAuthority('OPERADOR')")
    public ResponseEntity<SolicitudResponseDTO> finalizarSolicitud(
            @PathVariable Long id,
            // CAMBIO 3: Aceptamos los datos reales en el Body
            @RequestBody FinalizarSolicitudDTO dto) {
        SolicitudResponseDTO solicitud = solicitudService.finalizarSolicitud(id, dto); // CAMBIO 4: Pasamos el DTO
        return ResponseEntity.ok(solicitud);
    }

    // --- CÓDIGO AÑADIDO --- (Este ya lo tenías, se queda igual)
    /**
     * Endpoint interno para que otros servicios (como ms-rutas)
     * actualicen el estado del contenedor.
     * @param idContenedor El ID del contenedor a actualizar.
     * @param estadoDTO El DTO que contiene el nuevo nombre del estado (ej. "EN TRANSITO").
     * @return ResponseEntity vacía.
     */
    @PutMapping("/contenedores/{idContenedor}/estado")
    @PreAuthorize("hasAnyAuthority('OPERADOR', 'TRANSPORTISTA')")
    public ResponseEntity<Void> actualizarEstadoContenedor(
            @PathVariable Long idContenedor,
            @RequestBody EstadoDTO estadoDTO) {

        solicitudService.actualizarEstadoContenedor(idContenedor, estadoDTO.getEstado());
        return ResponseEntity.ok().build();
    }
    // --- FIN DEL CÓDIGO AÑADIDO ---
}