package com.transporte.ms_solicitudes.controller;

import com.transporte.ms_solicitudes.dto.SeguimientoDTO;
import com.transporte.ms_solicitudes.dto.SolicitudRequestDTO;
import com.transporte.ms_solicitudes.dto.SolicitudResponseDTO;
import com.transporte.ms_solicitudes.dto.EstadoDTO; 
import com.transporte.ms_solicitudes.dto.FinalizarSolicitudDTO;
import com.transporte.ms_solicitudes.service.SolicitudService;
import com.transporte.ms_solicitudes.model.Solicitud;
import com.transporte.ms_solicitudes.repository.SolicitudRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
// Importa todo de web.bind.annotation, incluyendo RequestBody de Spring
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*; 

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
// ❌ BORRA ESTA LÍNEA: import io.swagger.v3.oas.annotations.parameters.RequestBody;

import java.util.List;
import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/solicitudes")
@Tag(name = "Solicitudes", description = "Gestión de solicitudes de transporte")
public class SolicitudController {

    @Autowired
    private SolicitudService solicitudService;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENTE')")
    @Operation(summary = "Crear una nueva solicitud", description = "Crea una solicitud de transporte para un cliente existente o nuevo")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Solicitud creada exitosamente", 
                    content = @Content(schema = @Schema(implementation = SolicitudResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "No autorizado (Requiere rol CLIENTE)"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos en la solicitud")
    })
    public ResponseEntity<?> crearSolicitud(@RequestBody SolicitudRequestDTO solicitudRequest) {
        try {
            // Validaciones específicas
            if (solicitudRequest.getClienteDni() == null || solicitudRequest.getClienteDni().isBlank()) {
                return ResponseEntity.badRequest().body("DNI del cliente es obligatorio");
            }
            if (solicitudRequest.getPesoContenedor() == null) {
                return ResponseEntity.badRequest().body("Peso del contenedor es obligatorio");
            }
            if (solicitudRequest.getPesoContenedor() <= 0) {
                return ResponseEntity.badRequest().body("Peso del contenedor debe ser mayor a 0");
            }
            if (solicitudRequest.getVolumenContenedor() == null) {
                return ResponseEntity.badRequest().body("Volumen del contenedor es obligatorio");
            }
            if (solicitudRequest.getVolumenContenedor() <= 0) {
                return ResponseEntity.badRequest().body("Volumen del contenedor debe ser mayor a 0");
            }
            if (solicitudRequest.getOrigenLatitud() == null) {
                return ResponseEntity.badRequest().body("Latitud de origen es obligatoria");
            }
            if (solicitudRequest.getOrigenLongitud() == null) {
                return ResponseEntity.badRequest().body("Longitud de origen es obligatoria");
            }
            if (solicitudRequest.getDestinoLatitud() == null) {
                return ResponseEntity.badRequest().body("Latitud de destino es obligatoria");
            }
            if (solicitudRequest.getDestinoLongitud() == null) {
                return ResponseEntity.badRequest().body("Longitud de destino es obligatoria");
            }
            
            SolicitudResponseDTO solicitud = solicitudService.crearSolicitud(solicitudRequest);
            return ResponseEntity.ok(solicitud);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al crear solicitud: " + e.getMessage());
        }
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
    @PreAuthorize("permitAll()")
    @Operation(summary = "Obtener detalles de una solicitud", description = "Obtiene los detalles completos de una solicitud específica por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Detalles de la solicitud obtenidos exitosamente", 
                    content = @Content(schema = @Schema(implementation = SolicitudResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "No autorizado"),
        @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    })
    public ResponseEntity<?> obtenerSolicitud(@PathVariable Long id) {
        try {
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().body("ID de solicitud inválido");
            }
            return ResponseEntity.ok(solicitudService.obtenerSolicitud(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Solicitud no encontrada: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al obtener solicitud: " + e.getMessage());
        }
    }


    @GetMapping("/{id}/coordenadas")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Obtener detalles de una solicitud", description = "Obtiene los detalles completos de una solicitud específica por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Coordenadas de la solicitud obtenidos exitosamente", 
                    content = @Content(schema = @Schema(implementation = SolicitudResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "No autorizado"),
        @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    })
    public ResponseEntity<?> obtenerCoordenadas(@PathVariable Long id) {
        try {
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().body("ID de solicitud inválido");
            }
            return ResponseEntity.ok(solicitudService.obtenerCordenadasSolicitud(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Solicitud no encontrada: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al obtener coordenadas: " + e.getMessage());
        }
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
    public ResponseEntity<?> obtenerEstado(@PathVariable Long id) {
        try {
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().body("ID de solicitud inválido");
            }
            return ResponseEntity.ok(solicitudService.obtenerEstado(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Solicitud no encontrada: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al obtener estado: " + e.getMessage());
        }
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
    public ResponseEntity<?> finalizarSolicitud(
            @PathVariable Long id,
            @RequestBody FinalizarSolicitudDTO dto) {
        try {
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().body("ID de solicitud inválido");
            }
            if (dto == null) {
                return ResponseEntity.badRequest().body("Cuerpo de la solicitud no puede estar vacío");
            }
            // Nota: costoFinal y tiempoReal ahora se calculan automáticamente desde ms-rutas
            // Estos campos en el DTO son opcionales y se usan como fallback si falla la llamada a ms-rutas
            if (dto.getCostoFinal() != null && dto.getCostoFinal().signum() < 0) {
                return ResponseEntity.badRequest().body("Costo final no puede ser negativo");
            }
            if (dto.getTiempoReal() != null && dto.getTiempoReal().signum() < 0) {
                return ResponseEntity.badRequest().body("Tiempo real no puede ser negativo");
            }
            
            SolicitudResponseDTO solicitud = solicitudService.finalizarSolicitud(id, dto);
            return ResponseEntity.ok(solicitud);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Solicitud no encontrada: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al finalizar solicitud: " + e.getMessage());
        }
    }

    /**
     * Endpoint interno para que otros servicios (como ms-rutas)
     * actualicen el estado del contenedor.
     * Acepta tanto idContenedor como idSolicitud para mayor flexibilidad.
     * @param idContenedor El ID del contenedor a actualizar (si se proporciona).
     * @param idSolicitud El ID de la solicitud (si se proporciona, se busca su contenedor).
     * @param estadoDTO El DTO que contiene el nuevo nombre del estado (ej. "EN TRANSITO").
     * @return ResponseEntity vacía.
     */
    @PutMapping("/contenedores/{idContenedor}/estado")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Actualizar estado de contenedor (inter-servicio)", description = "Endpoint para que otros microservicios (como ms-rutas) actualicen el estado de un contenedor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estado del contenedor actualizado exitosamente"),
        @ApiResponse(responseCode = "403", description = "No autorizado"),
        @ApiResponse(responseCode = "404", description = "Contenedor no encontrado")
    })
    public ResponseEntity<?> actualizarEstadoContenedor(
            @PathVariable(required = false) Long idContenedor,
            @RequestParam(required = false) Long idSolicitud,
            @RequestBody EstadoDTO estadoDTO) {
        try {
            // Validar que al menos uno de los IDs sea válido ANTES de intentar hacer conversiones
            if ((idContenedor == null || idContenedor <= 0) && (idSolicitud == null || idSolicitud <= 0)) {
                return ResponseEntity.badRequest().body("ID de contenedor o ID de solicitud es obligatorio");
            }
            
            // Si se proporciona idSolicitud (y es válido), buscar el contenedor asociado
            if (idSolicitud != null && idSolicitud > 0) {
                Solicitud solicitud = solicitudRepository.findById(idSolicitud)
                        .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada: " + idSolicitud));
                idContenedor = solicitud.getContenedor().getId();
            }
            
            // Ahora idContenedor debe ser válido
            if (idContenedor == null || idContenedor <= 0) {
                return ResponseEntity.badRequest().body("ID de contenedor inválido");
            }
            if (estadoDTO == null || estadoDTO.getEstado() == null || estadoDTO.getEstado().isBlank()) {
                return ResponseEntity.badRequest().body("Estado del contenedor es obligatorio");
            }
            
            solicitudService.actualizarEstadoContenedor(idContenedor, estadoDTO.getEstado());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Contenedor no encontrado: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al actualizar estado: " + e.getMessage());
        }
    }
}