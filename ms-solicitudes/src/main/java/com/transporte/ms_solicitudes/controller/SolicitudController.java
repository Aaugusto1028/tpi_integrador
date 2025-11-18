package com.transporte.ms_solicitudes.controller;

import com.transporte.ms_solicitudes.dto.SeguimientoDTO;
import com.transporte.ms_solicitudes.dto.SolicitudRequestDTO;
import com.transporte.ms_solicitudes.dto.SolicitudResponseDTO;
import com.transporte.ms_solicitudes.dto.EstadoDTO; 
import com.transporte.ms_solicitudes.dto.FinalizarSolicitudDTO;
import com.transporte.ms_solicitudes.service.SolicitudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/solicitudes")
@Tag(name = "Solicitudes", description = "Gestión de solicitudes de transporte")
public class SolicitudController {

    @Autowired
    private SolicitudService solicitudService;

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENTE')")
    @Operation(summary = "Crear una nueva solicitud", description = "Crea una solicitud de transporte para un cliente existente o nuevo")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Solicitud creada exitosamente", 
                    content = @Content(schema = @Schema(implementation = SolicitudResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "No autorizado (Requiere rol CLIENTE)"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos en la solicitud")
    })
    public ResponseEntity<SolicitudResponseDTO> crearSolicitud(@RequestBody SolicitudRequestDTO solicitudRequest) {
        SolicitudResponseDTO solicitud = solicitudService.crearSolicitud(solicitudRequest);
        return ResponseEntity.ok(solicitud);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('OPERADOR')")
    @Operation(summary = "Listar solicitudes", description = "Obtiene un listado de todas las solicitudes, opcionalmente filtradas por estado")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Listado de solicitudes obtenido exitosamente"),
        @ApiResponse(responseCode = "403", description = "No autorizado (Requiere rol OPERADOR)")
    })
    public ResponseEntity<List<SolicitudResponseDTO>> obtenerSolicitudes(
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(solicitudService.listarSolicitudes(estado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CLIENTE', 'OPERADOR')")
    @Operation(summary = "Obtener detalles de una solicitud", description = "Obtiene los detalles completos de una solicitud específica por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Detalles de la solicitud obtenidos exitosamente", 
                    content = @Content(schema = @Schema(implementation = SolicitudResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "No autorizado"),
        @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    })
    public ResponseEntity<SolicitudResponseDTO> obtenerSolicitud(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.obtenerSolicitud(id));
    }

    @GetMapping("/{id}/estado")
    @PreAuthorize("hasAnyAuthority('CLIENTE', 'OPERADOR')")
    @Operation(summary = "Obtener estado y seguimiento de una solicitud", description = "Obtiene el estado actual y detalles de seguimiento de una solicitud específica")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estado obtenido exitosamente", 
                    content = @Content(schema = @Schema(implementation = SeguimientoDTO.class))),
        @ApiResponse(responseCode = "403", description = "No autorizado"),
        @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    })
    public ResponseEntity<SeguimientoDTO> obtenerEstado(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.obtenerEstado(id));
    }

    /**
     * Endpoint que dispara el cálculo final y la finalización de la solicitud.
     * Es llamado por un Operador cuando el transporte ha concluido.
     */
    @PutMapping("/{id}/finalizar")
    @PreAuthorize("hasAuthority('OPERADOR')")
    @Operation(summary = "Finalizar una solicitud", description = "Finaliza una solicitud de transporte y calcula los costos finales")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Solicitud finalizada exitosamente", 
                    content = @Content(schema = @Schema(implementation = SolicitudResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "No autorizado (Requiere rol OPERADOR)"),
        @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
        @ApiResponse(responseCode = "400", description = "No se puede finalizar una solicitud en este estado")
    })
    public ResponseEntity<SolicitudResponseDTO> finalizarSolicitud(
            @PathVariable Long id,
            @RequestBody FinalizarSolicitudDTO dto) {
        SolicitudResponseDTO solicitud = solicitudService.finalizarSolicitud(id, dto);
        return ResponseEntity.ok(solicitud);
    }

    /**
     * Endpoint interno para que otros servicios (como ms-rutas)
     * actualicen el estado del contenedor.
     * @param idContenedor El ID del contenedor a actualizar.
     * @param estadoDTO El DTO que contiene el nuevo nombre del estado (ej. "EN TRANSITO").
     * @return ResponseEntity vacía.
     */
    @PutMapping("/contenedores/{idContenedor}/estado")
    @PreAuthorize("hasAnyAuthority('OPERADOR', 'TRANSPORTISTA')")
    @Operation(summary = "Actualizar estado de contenedor (interno)", description = "Endpoint interno para que otros servicios actualicen el estado de un contenedor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estado del contenedor actualizado exitosamente"),
        @ApiResponse(responseCode = "403", description = "No autorizado"),
        @ApiResponse(responseCode = "404", description = "Contenedor no encontrado")
    })
    public ResponseEntity<Void> actualizarEstadoContenedor(
            @PathVariable Long idContenedor,
            @RequestBody EstadoDTO estadoDTO) {

        solicitudService.actualizarEstadoContenedor(idContenedor, estadoDTO.getEstado());
        return ResponseEntity.ok().build();
    }
}