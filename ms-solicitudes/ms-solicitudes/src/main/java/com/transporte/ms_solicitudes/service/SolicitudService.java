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

    @Transactional //transaccional significa que si algo falla, se deshacen todos los cambios hechos en la BD durante la ejecución del método
    SolicitudResponseDTO crearSolicitud(SolicitudRequestDTO SolicitudRequestDTO{
        //QUE ES LO QUE TIENE DTO request?
        //CLIENTE , PESO , VOLUMEN. fk A CLIENTE fk a Contendor.


        //pero que es lo que tiene response?? 
        /*   private Long idSolicitud;
            private String nombreCliente;
            private String estadoActual;
            private BigDecimal costoEstimado; */
        Cliente cliente = ClienteRepository.findByDni(SolicitudRequestDTO.getDni());
        Contenedor contenedor = new Contendor();
        contenedor.setPeso(request.getPeso());
        contenedor.serVolumen(request.getVolumen());
        
        EstadoContenedor estadoActual = new EstadoContenedor(nombre:"Borrador", fecha:LocalDateTime.now(), contendor);

        Solicitud solicitud = new Solicitud() ;
        solicitud.setCliente(cliente);
        solicitud.setContenedor(contenedor);
        // 5. Lógica de Costo Estimado (Próximo paso, ahora ponemos un valor fijo)
        // BigDecimal costoEstimado = calcularCostoEstimado(contenedor.getPeso(), ...);
        solicitud.setCostoEstimado(new BigDecimal("9999.99")); // Valor temporal

        Solicitud solicitudGuardada = solicitudRepository.save(solicitud);

        SolicitudRequestDTO solicitudResponseDTO = new SolicitudResponseDTO();
        solicitudResponseDTO.setIdSolicitud(solicitudGuardada.getId());
        solicitudResponseDTO.setNombreCliente(cliente.getNombre());
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