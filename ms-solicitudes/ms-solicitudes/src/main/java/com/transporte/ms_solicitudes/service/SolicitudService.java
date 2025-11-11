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
    public SolicitudResponseDTO crearSolicitud(SolicitudRequestDTO request) { // <-- 1. Faltaba el nombre 'request'
        
        // --- QUE ES LO QUE TIENE request? ---
        // clienteDni, pesoContenedor, volumenContenedor

        // --- QUE ES LO QUE TIENE response?? ---
        // idSolicitud, nombreCliente, estadoActual, costoEstimado
        
        // 2. Usamos la variable 'clienteRepository' (en minúscula)
        // 3. 'findByDni' devuelve un Optional, hay que "abrirlo"
        Cliente cliente = clienteRepository.findByDni(request.getClienteDni()) // 4. Usamos los getters del DTO
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con DNI: " + request.getClienteDni()));

        Contenedor contenedor = new Contenedor(); // 5. Corregido: Contendor -> Contenedor
        contenedor.setPeso(request.getPesoContenedor()); // 4. Usamos los getters del DTO
        contenedor.setVolumen(request.getVolumenContenedor()); // 4. Usamos getters y 5. Corregido: serVolumen -> setVolumen
        
        // 6. Sintaxis de constructor de Java (sin nombres de parámetros)
        EstadoContenedor estadoActual = new EstadoContenedor("Borrador", LocalDateTime.now(), contenedor);
        // Asignamos el historial al contenedor
        contenedor.setHistorialEstados(List.of(estadoActual));

        Solicitud solicitud = new Solicitud() ;
        solicitud.setCliente(cliente);
        solicitud.setContenedor(contenedor);
        
        // Lógica de Costo Estimado (valor temporal)
        solicitud.setCostoEstimado(new BigDecimal("9999.99")); 

        Solicitud solicitudGuardada = solicitudRepository.save(solicitud);

        // 7. El tipo de variable debe ser SolicitudResponseDTO
        SolicitudResponseDTO solicitudResponseDTO = new SolicitudResponseDTO();
        solicitudResponseDTO.setIdSolicitud(solicitudGuardada.getId());
        solicitudResponseDTO.setNombreCliente(cliente.getNombre() + " " + cliente.getApellido()); // Concatenamos
        solicitudResponseDTO.setEstadoActual(estadoActual.getNombre());
        solicitudResponseDTO.setCostoEstimado(solicitudGuardada.getCostoEstimado());

        return solicitudResponseDTO;
    }
    @Transactional(readOnly = true)
    public EstadoDTO consultarEstadoSolicitud(Long idSolicitud) {
        // 1. Buscamos la solicitud
        Solicitud solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + idSolicitud));

        // 2. Obtenemos el estado actual usando el método @Transient
        EstadoContenedor estadoActual = solicitud.getContenedor().getEstadoActual();
        
        if (estadoActual == null) {
            throw new RuntimeException("La solicitud no tiene ningún estado registrado.");
        }

        // 3. Devolvemos el DTO de estado
        return new EstadoDTO(estadoActual.getNombre(), estadoActual.getFecha());
    }

}