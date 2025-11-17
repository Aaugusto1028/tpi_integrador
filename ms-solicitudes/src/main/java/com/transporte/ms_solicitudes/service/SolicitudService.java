package com.transporte.ms_solicitudes.service;

import com.transporte.ms_solicitudes.client.CamionesWebClient;
import com.transporte.ms_solicitudes.client.RutasWebClient;
import com.transporte.ms_solicitudes.dto.*;
import com.transporte.ms_solicitudes.model.Cliente;
import com.transporte.ms_solicitudes.model.Contenedor;
import com.transporte.ms_solicitudes.model.Solicitud;
import com.transporte.ms_solicitudes.repository.ContenedorRepository;
import com.transporte.ms_solicitudes.repository.SolicitudRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SolicitudService {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private ContenedorRepository contenedorRepository;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private CamionesWebClient camionesWebClient;

    @Autowired
    private RutasWebClient rutasWebClient;

    /**
     * Crea una nueva solicitud con costo y tiempo estimado.
     */
    @Transactional
    public SolicitudResponseDTO crearSolicitud(SolicitudRequestDTO dto) {
        // Buscar cliente por DNI
        Cliente cliente = clienteService.buscarClientePorDni(dto.getClienteDni());

        // Crear contenedor
        Contenedor contenedor = new Contenedor();
        contenedor.setPeso(dto.getPesoContenedor());
        contenedor.setVolumen(dto.getVolumenContenedor());
        contenedor.setClienteAsociado(cliente);

        // Crear solicitud
        Solicitud solicitud = new Solicitud();
        solicitud.setCliente(cliente);
        solicitud.setContenedor(contenedor);
        solicitud.setOrigenLatitud(dto.getOrigenLatitud());
        solicitud.setOrigenLongitud(dto.getOrigenLongitud());
        solicitud.setDestinoLatitud(dto.getDestinoLatitud());
        solicitud.setDestinoLongitud(dto.getDestinoLongitud());
        solicitud.setEstado("PENDIENTE");

        // Calcular costo y tiempo estimado
        try {
            CostoTiempoDTO estimado = calcularCostoTiempoEstimado(
                    dto.getOrigenLatitud(), dto.getOrigenLongitud(),
                    dto.getDestinoLatitud(), dto.getDestinoLongitud(),
                    dto.getPesoContenedor(), dto.getVolumenContenedor()
            );
            solicitud.setCostoEstimado(estimado.costo);
            solicitud.setTiempoEstimado(estimado.tiempo);
        } catch (Exception e) {
            solicitud.setCostoEstimado(BigDecimal.ZERO);
            solicitud.setTiempoEstimado(BigDecimal.ZERO);
        }

        contenedorRepository.save(contenedor);
        solicitudRepository.save(solicitud);

        return mapToResponseDTO(solicitud);
    }

    /**
     * Calcula costo y tiempo estimado.
     */
    private CostoTiempoDTO calcularCostoTiempoEstimado(
            BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2,
            BigDecimal peso, BigDecimal volumen) {

        // Obtener distancia de ms-rutas
        DistanciaResponse distancia = rutasWebClient.obtenerDistancia(
                lat1.doubleValue(), lon1.doubleValue(),
                lat2.doubleValue(), lon2.doubleValue()
        );
        BigDecimal distanciaKm = distancia.getDistanciaMetros().divide(new BigDecimal(1000));

        // Obtener promedios de ms-camiones
        PromediosDTO promedios = camionesWebClient.obtenerPromedios(peso, volumen);

        // Obtener tarifas de ms-rutas
        TarifaDTO tarifas = rutasWebClient.obtenerTarifas();

        // Calcular costo
        BigDecimal costoTramo = distanciaKm.multiply(promedios.getCostoPromedioPorKm());
        BigDecimal costoCombustible = distanciaKm
                .multiply(promedios.getConsumoPromedioPorKm())
                .multiply(tarifas.getPrecioLitro());
        BigDecimal costoTotal = costoTramo.add(costoCombustible).add(tarifas.getCostoEstadiaDiario());

        // Calcular tiempo (distancia / 80 km/h + 24h)
        BigDecimal tiempoHoras = distanciaKm.divide(new BigDecimal(80)).add(new BigDecimal(24));

        return new CostoTiempoDTO(costoTotal, tiempoHoras);
    }

    /**
     * Finaliza una solicitud.
     */
    @Transactional
    public SolicitudResponseDTO finalizarSolicitud(Long id) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        solicitud.setEstado("ENTREGADA");
        solicitud.setCostoFinal(solicitud.getCostoEstimado());
        solicitud.setTiempoReal(solicitud.getTiempoEstimado());

        solicitudRepository.save(solicitud);
        return mapToResponseDTO(solicitud);
    }

    /**
     * Lista todas las solicitudes.
     */
    public List<SolicitudResponseDTO> listarSolicitudes() {
        return solicitudRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una solicitud por ID.
     */
    public SolicitudResponseDTO obtenerSolicitud(Long id) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));
        return mapToResponseDTO(solicitud);
    }

    /**
     * Obtiene el seguimiento de una solicitud.
     */
    public SeguimientoDTO obtenerEstado(Long id) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        return new SeguimientoDTO(
                solicitud.getEstado(),
                "Estado: " + solicitud.getEstado()
        );
    }

    /**
     * Actualiza el estado de un contenedor.
     */
    @Transactional
    public void actualizarEstadoContenedor(Long idContenedor, String nuevoEstado) {
        Contenedor contenedor = contenedorRepository.findById(idContenedor)
                .orElseThrow(() -> new EntityNotFoundException("Contenedor no encontrado"));
        // Aquí actualizarías el estado si existe una entidad EstadoContenedor
        // Por ahora, se deja vacío
    }

    /**
     * Mapea Solicitud a SolicitudResponseDTO.
     */
    private SolicitudResponseDTO mapToResponseDTO(Solicitud solicitud) {
        SolicitudResponseDTO dto = new SolicitudResponseDTO();
        dto.setId(solicitud.getId());
        dto.setEstado(solicitud.getEstado());
        dto.setCostoEstimado(solicitud.getCostoEstimado());
        dto.setCostoFinal(solicitud.getCostoFinal());
        dto.setTiempoEstimado(solicitud.getTiempoEstimado());
        dto.setTiempoReal(solicitud.getTiempoReal());
        return dto;
    }

    /**
     * DTO interno para costo y tiempo.
     */
    private static class CostoTiempoDTO {
        BigDecimal costo;
        BigDecimal tiempo;

        CostoTiempoDTO(BigDecimal costo, BigDecimal tiempo) {
            this.costo = costo;
            this.tiempo = tiempo;
        }
    }
}