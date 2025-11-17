package com.transporte.ms_solicitudes.service;

import com.transporte.ms_solicitudes.client.CamionesWebClient;
import com.transporte.ms_solicitudes.client.RutasWebClient;
import com.transporte.ms_solicitudes.dto.*;
import com.transporte.ms_solicitudes.model.Cliente;
import com.transporte.ms_solicitudes.model.Contenedor;
import com.transporte.ms_solicitudes.model.EstadoContenedor; // <-- IMPORTADO
import com.transporte.ms_solicitudes.model.Solicitud;
import com.transporte.ms_solicitudes.repository.ContenedorRepository;
import com.transporte.ms_solicitudes.repository.EstadoContenedorRepository; // <-- IMPORTADO
import com.transporte.ms_solicitudes.repository.SolicitudRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime; // <-- IMPORTADO
import java.util.List;
import java.util.Optional; // <-- IMPORTADO
import java.util.stream.Collectors;

@Service
public class SolicitudService {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private ContenedorRepository contenedorRepository;

    // --- AÑADIDO ---
    @Autowired
    private EstadoContenedorRepository estadoContenedorRepository;
    // --- FIN AÑADIDO ---

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
        // 1. Buscar cliente (lógica existente)
        Cliente cliente = clienteService.buscarClientePorDni(dto.getClienteDni());

        // 2. Crear contenedor (lógica existente)
        Contenedor contenedor = new Contenedor();
        contenedor.setPeso(dto.getPesoContenedor());
        contenedor.setVolumen(dto.getVolumenContenedor());
        contenedor.setClienteAsociado(cliente);

        // 3. Crear solicitud (lógica existente)
        Solicitud solicitud = new Solicitud();
        solicitud.setCliente(cliente);
        solicitud.setContenedor(contenedor);
        solicitud.setOrigenLatitud(dto.getOrigenLatitud());
        solicitud.setOrigenLongitud(dto.getOrigenLongitud());
        solicitud.setDestinoLatitud(dto.getDestinoLatitud());
        solicitud.setDestinoLongitud(dto.getDestinoLongitud());
        solicitud.setEstado("PENDIENTE"); // Estado inicial de la solicitud

        // 4. Calcular estimados (lógica existente, usa WebClient)
        try {
            CostoTiempoDTO estimado = calcularCostoTiempoEstimado(
                    dto.getOrigenLatitud(), dto.getOrigenLongitud(),
                    dto.getDestinoLatitud(), dto.getDestinoLongitud(),
                    dto.getPesoContenedor(), dto.getVolumenContenedor()
            );
            solicitud.setCostoEstimado(estimado.costo);
            solicitud.setTiempoEstimado(estimado.tiempo);
        } catch (Exception e) {
            // Manejo de error si los otros servicios fallan
            solicitud.setCostoEstimado(BigDecimal.ZERO);
            solicitud.setTiempoEstimado(BigDecimal.ZERO);
        }

        // 5. Guardar entidades
        contenedorRepository.save(contenedor); // Guardamos contenedor primero
        solicitudRepository.save(solicitud); // Guardamos solicitud
        
        // --- LÓGICA AÑADIDA ---
        // 6. Creamos el PRIMER estado en el historial del contenedor
        EstadoContenedor estadoInicial = new EstadoContenedor(
                "PENDIENTE",
                LocalDateTime.now(),
                contenedor
        );
        estadoContenedorRepository.save(estadoInicial);
        // --- FIN LÓGICA AÑADIDA ---

        return mapToResponseDTO(solicitud);
    }

    /**
     * Calcula costo y tiempo estimado.
     * (Lógica original - sin cambios, usa WebClient)
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

        // Calcular tiempo (ejemplo: distancia / 80 km/h + 24h de margen)
        BigDecimal tiempoHoras = distanciaKm.divide(new BigDecimal(80), 2, BigDecimal.ROUND_HALF_UP).add(new BigDecimal(24));

        return new CostoTiempoDTO(costoTotal, tiempoHoras);
    }

    /**
     * Finaliza una solicitud.
     * (LÓGICA ACTUALIZADA)
     */
    @Transactional
    public SolicitudResponseDTO finalizarSolicitud(Long id, FinalizarSolicitudDTO dto) { // <-- CAMBIO 1: Recibe DTO
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        solicitud.setEstado("ENTREGADA");
        
        // CAMBIO 2: Usamos los valores reales del DTO, no los estimados
        solicitud.setCostoFinal(dto.getCostoFinal());
        solicitud.setTiempoReal(dto.getTiempoReal());

        // CAMBIO 3: Registramos el estado final en el historial del contenedor
        actualizarEstadoContenedor(solicitud.getContenedor().getId(), "ENTREGADA");

        solicitudRepository.save(solicitud);
        return mapToResponseDTO(solicitud);
    }

    /**
     * Lista todas las solicitudes, con filtro opcional por estado.
     * (LÓGICA ACTUALIZADA)
     */
    public List<SolicitudResponseDTO> listarSolicitudes(String estado) { // <-- CAMBIO 1: Recibe estado
        List<Solicitud> solicitudes;
        
        // CAMBIO 2: Lógica de filtro
        if (estado != null && !estado.isBlank()) {
            // Usa el método del repositorio que ya tenías
            solicitudes = solicitudRepository.findByEstado(estado);
        } else {
            // Si no hay filtro, devuelve todo
            solicitudes = solicitudRepository.findAll();
        }

        return solicitudes.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una solicitud por ID.
     * (Lógica original - sin cambios)
     */
    public SolicitudResponseDTO obtenerSolicitud(Long id) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));
        return mapToResponseDTO(solicitud);
    }

    /**
     * Obtiene el seguimiento de una solicitud.
     * (LÓGICA ACTUALIZADA)
     */
    public SeguimientoDTO obtenerEstado(Long id) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        // CAMBIO 1: Buscamos el estado actual real desde el historial del contenedor
        Contenedor contenedor = solicitud.getContenedor();
        EstadoContenedor estadoActual = contenedor.getEstadoActual(); // Usamos el helper de Contenedor.java

        if (estadoActual != null) {
            // El DTO de seguimiento pide "estado" y "ubicacion".
            // Por ahora, usamos el nombre del estado (ej. "EN_DEPOSITO") como ubicación.
            // En una versión futura, el 'EstadoContenedor' podría tener un campo 'ubicacion'.
            return new SeguimientoDTO(
                    estadoActual.getNombre(),
                    "Ubicación: " + estadoActual.getNombre() 
            );
        }

        // Fallback si el historial está vacío (no debería pasar)
        return new SeguimientoDTO(
                solicitud.getEstado(),
                "Estado: " + solicitud.getEstado()
        );
    }

    /**
     * Actualiza el estado de un contenedor.
     * (LÓGICA ACTUALIZADA - YA NO ESTÁ VACÍA)
     */
    @Transactional
    public void actualizarEstadoContenedor(Long idContenedor, String nuevoEstado) {
        Contenedor contenedor = contenedorRepository.findById(idContenedor)
                .orElseThrow(() -> new EntityNotFoundException("Contenedor no encontrado"));

        // --- LÓGICA IMPLEMENTADA ---
        // 1. Crear el nuevo registro de estado
        EstadoContenedor nuevoRegistroEstado = new EstadoContenedor(
                nuevoEstado,
                LocalDateTime.now(),
                contenedor
        );
        
        // 2. Guardar el nuevo estado en el historial
        estadoContenedorRepository.save(nuevoRegistroEstado);

        // 3. Sincronizar el estado de la solicitud principal
        //    (Usando el nuevo método del repositorio)
        Optional<Solicitud> solicitudOpt = solicitudRepository.findByContenedor_Id(idContenedor);
        if (solicitudOpt.isPresent()) {
             Solicitud solicitud = solicitudOpt.get();
             solicitud.setEstado(nuevoEstado);
             solicitudRepository.save(solicitud);
        }
        // --- FIN DE LA LÓGICA ---
    }

    /**
     * Mapea Solicitud a SolicitudResponseDTO.
     * (Lógica original - sin cambios)
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
     * (Lógica original - sin cambios)
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