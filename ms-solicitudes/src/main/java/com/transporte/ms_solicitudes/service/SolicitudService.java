package com.transporte.ms_solicitudes.service;

import com.transporte.ms_solicitudes.dto.EstadoDTO;
import com.transporte.ms_solicitudes.dto.SolicitudRequestDTO;
import com.transporte.ms_solicitudes.dto.SolicitudResponseDTO;
import com.transporte.ms_solicitudes.model.Cliente;
import com.transporte.ms_solicitudes.model.Contenedor;
import com.transporte.ms_solicitudes.model.EstadoContenedor;
import com.transporte.ms_solicitudes.model.Solicitud;
import com.transporte.ms_solicitudes.repository.ClienteRepository;
import com.transporte.ms_solicitudes.repository.SolicitudRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SolicitudService {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    // (Más adelante) @Autowired private RutasFeignClient rutasFeignClient;
    // (Más adelante) @Autowired private CamionesFeignClient camionesFeignClient;

    @Transactional //transaccional significa que si algo falla, se deshacen todos los cambios hechos en la BD
    public SolicitudResponseDTO crearSolicitud(SolicitudRequestDTO request) { // <-- CORRECCIÓN 1: Faltaba el nombre 'request'
        
        // CORRECCIÓN 2: Usamos 'clienteRepository' (variable) y 'findByDni' devuelve un Optional
        Cliente cliente = clienteRepository.findByDni(request.getClienteDni()) // <-- CORRECCIÓN 3: Usamos getters del DTO
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con DNI: " + request.getClienteDni()));

        Contenedor contenedor = new Contenedor(); // <-- CORRECCIÓN 4: 'Contendor' -> 'Contenedor'
        contenedor.setPeso(request.getPesoContenedor()); // <-- CORRECCIÓN 3
        contenedor.setVolumen(request.getVolumenContenedor()); // <-- CORRECCIÓN 3 y 4: 'serVolumen' -> 'setVolumen'
        
        // CORRECCIÓN 5: Sintaxis de constructor de Java (sin nombres de parámetros)
        EstadoContenedor estadoActual = new EstadoContenedor("Borrador", LocalDateTime.now(), contenedor);
        // Asignamos el historial al contenedor
        contenedor.setHistorialEstados(List.of(estadoActual));

        Solicitud solicitud = new Solicitud() ;
        solicitud.setCliente(cliente);
        solicitud.setContenedor(contenedor);
        
        // Lógica de Costo Estimado (valor temporal)
        solicitud.setCostoEstimado(new BigDecimal("9999.99")); 

        Solicitud solicitudGuardada = solicitudRepository.save(solicitud);

        // CORRECCIÓN 6: El tipo de variable debe ser SolicitudResponseDTO
        SolicitudResponseDTO solicitudResponseDTO = new SolicitudResponseDTO();
        solicitudResponseDTO.setIdSolicitud(solicitudGuardada.getId());
        solicitudResponseDTO.setNombreCliente(cliente.getNombre() + " " + cliente.getApellido()); // Concatenamos
        solicitudResponseDTO.setEstadoActual(estadoActual.getNombre());
        solicitudResponseDTO.setCostoEstimado(solicitudGuardada.getCostoEstimado());

        return solicitudResponseDTO;
    }

    @Transactional(readOnly = true)
    public EstadoDTO consultarEstadoSolicitud(Long idSolicitud) {
        Solicitud solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + idSolicitud));

        EstadoContenedor estadoActual = solicitud.getContenedor().getEstadoActual();
        
        if (estadoActual == null) {
            throw new RuntimeException("La solicitud no tiene ningún estado registrado.");
        }
        return new EstadoDTO(estadoActual.getNombre(), estadoActual.getFecha());
    }


    @Transactional(readOnly = true)
    public List<SolicitudResponseDTO> obtenerTodasLasSolicitudes() {
        // 1. Buscamos todas las solicitudes
        List<Solicitud> solicitudes = solicitudRepository.findAll();
        
        // 2. Las convertimos a DTOs (esto se puede optimizar)
        return solicitudes.stream().map(solicitud -> {
            SolicitudResponseDTO dto = new SolicitudResponseDTO();
            dto.setIdSolicitud(solicitud.getId());
            dto.setNombreCliente(solicitud.getCliente().getNombre() + " " + solicitud.getCliente().getApellido());
            dto.setEstadoActual(solicitud.getContenedor().getEstadoActual() != null ? 
                                solicitud.getContenedor().getEstadoActual().getNombre() : "SIN_ESTADO");
            dto.setCostoEstimado(solicitud.getCostoEstimado());
            return dto;
        }).toList();
    }
}