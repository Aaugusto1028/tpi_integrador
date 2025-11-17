package com.transporte.ms_solicitudes.service;

import com.transporte.ms_solicitudes.client.CamionesWebClient;
import com.transporte.ms_solicitudes.client.RutasWebClient;
import com.transporte.ms_solicitudes.dto.*; // Asegúrate de crear los DTOs que faltan (ver Paso 2)
import com.transporte.ms_solicitudes.model.Cliente;
import com.transporte.ms_solicitudes.model.Contenedor;
import com.transporte.ms_solicitudes.model.EstadoContenedor;
import com.transporte.ms_solicitudes.model.Solicitud;
import com.transporte.ms_solicitudes.repository.ClienteRepository;
import com.transporte.ms_solicitudes.repository.ContenedorRepository;
import com.transporte.ms_solicitudes.repository.SolicitudRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SolicitudService {

    @Autowired
    private SolicitudRepository solicitudRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ContenedorRepository contenedorRepository; // Necesario para guardar el contenedor

    // Clientes para la comunicación inter-servicio
    @Autowired
    private RutasWebClient rutasWebClient;
    @Autowired
    private CamionesWebClient camionesWebClient;

    /**
     * REQ 1: Registrar una nueva solicitud.
     * (Versión mejorada de tu método)
     */
    @Transactional
    public SolicitudResponseDTO crearSolicitud(SolicitudRequestDTO request) {
        
        // 1. Buscar Cliente (REQ 1.2)
        Cliente cliente = clienteRepository.findByDni(request.getClienteDni())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con DNI: " + request.getClienteDni()));

        // 2. Crear Contenedor (REQ 1.1)
        Contenedor contenedor = new Contenedor();
        contenedor.setPeso(request.getPesoContenedor());
        contenedor.setVolumen(request.getVolumenContenedor());
        contenedor.setClienteAsociado(cliente); // Asignamos el cliente al contenedor (según DER)

        // 3. Crear Estado Inicial (REQ 1.3)
        EstadoContenedor estadoActual = new EstadoContenedor("Borrador", LocalDateTime.now(), contenedor);
        contenedor.setHistorialEstados(List.of(estadoActual)); // Asignamos el historial

        // 4. Crear Solicitud
        Solicitud solicitud = new Solicitud();
        solicitud.setCliente(cliente);
        solicitud.setContenedor(contenedor); // El @OneToOne(cascade = CascadeType.ALL) guardará el contenedor y su estado.
        
        // Guardamos las coordenadas para futuros cálculos
        solicitud.setOrigenLatitud(request.getOrigenLatitud());
        solicitud.setOrigenLongitud(request.getOrigenLongitud());
        solicitud.setDestinoLatitud(request.getDestinoLatitud());
        solicitud.setDestinoLongitud(request.getDestinoLongitud());

        // El costo estimado se calculará después (POST /solicitudes/{id}/calcular-costo)
        solicitud.setCostoEstimado(null); 
        solicitud.setEstado("Borrador"); // Estado inicial de la solicitud

        Solicitud solicitudGuardada = solicitudRepository.save(solicitud);

        // 5. Mapear Respuesta
        return mapToSolicitudResponseDTO(solicitudGuardada);
    }

    /**
     * REQ 3 y 8: Calcular costo y tiempo estimados (Endpoint: POST /solicitudes/{id}/calcular-costo)
     * Orquesta llamadas a ms-camiones y ms-rutas.
     */
    @Transactional
    public SolicitudResponseDTO calcularCostoTiempoEstimado(Long idSolicitud) {
        // 1. Obtener la Solicitud y el Contenedor
        Solicitud solicitud = findSolicitudById(idSolicitud);
        Contenedor contenedor = solicitud.getContenedor();

        // 2. Llamada a ms-camiones: Obtener camiones aptos (REQ 11)
        List<CamionDTO> camionesAptos = camionesWebClient.obtenerCamionesAptos(
                contenedor.getPeso(),
                contenedor.getVolumen()
        ).block(); // .block() convierte la llamada asíncrona en síncrona

        if (camionesAptos == null || camionesAptos.isEmpty()) {
            throw new RuntimeException("No se encontraron camiones aptos para este contenedor.");
        }

        // 3. Llamada a ms-camiones: Obtener promedios de costos (Regla 105)
        List<String> dominios = camionesAptos.stream().map(CamionDTO::getDominio).collect(Collectors.toList());
        PromediosDTO promedios = camionesWebClient.obtenerPromediosCostos(dominios).block();
        
        // 4. Llamada a ms-rutas: Obtener distancia y tarifa de combustible (REQ 8.1 y Regla 64)
        CoordenadasRequest coords = new CoordenadasRequest(
                solicitud.getOrigenLatitud(), solicitud.getOrigenLongitud(),
                solicitud.getDestinoLatitud(), solicitud.getDestinoLongitud()
        );
        DistanciaResponse distancia = rutasWebClient.obtenerDistanciaEstimada(coords).block();
        TarifaDTO tarifa = rutasWebClient.obtenerTarifaVigente().block();

        // 5. Aplicar Lógica de Negocio (Fórmula de Tarifa Aproximada - Reglas 63, 65, 106)
        if (promedios == null || distancia == null || tarifa == null) {
            throw new RuntimeException("No se pudo obtener la información de costos y rutas.");
        }

        BigDecimal distanciaEnKm = distancia.getDistanciaMetros().divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);
        BigDecimal tiempoEnHoras = distancia.getDuracionSegundos().divide(new BigDecimal("3600"), 2, RoundingMode.HALF_UP);

        // Costo = (Costo Base por Km * Distancia) + (Consumo Promedio * Distancia * Precio Combustible)
        BigDecimal costoBase = promedios.getCostoPromedioPorKm().multiply(distanciaEnKm);
        BigDecimal costoCombustible = promedios.getConsumoPromedioPorKm()
                                            .multiply(distanciaEnKm)
                                            .multiply(tarifa.getPrecioLitro()); // Asumiendo que TarifaDTO tiene .getPrecioLitro()
        
        BigDecimal costoEstimadoTotal = costoBase.add(costoCombustible).setScale(2, RoundingMode.HALF_UP);

        // 6. Actualizar y Guardar Solicitud
        solicitud.setCostoEstimado(costoEstimadoTotal);
        solicitud.setTiempoEstimado(tiempoEnHoras);
        solicitud.setEstado("Programada");

        // Añadimos el nuevo estado al historial del contenedor
        EstadoContenedor estadoProgramado = new EstadoContenedor("Programada", LocalDateTime.now(), contenedor);
        contenedor.getHistorialEstados().add(estadoProgramado);
        
        Solicitud solicitudActualizada = solicitudRepository.save(solicitud);
        return mapToSolicitudResponseDTO(solicitudActualizada);
    }

    /**
     * REQ 9: Finalizar la solicitud (Endpoint: PUT /solicitudes/{id}/finalizar)
     */
    @Transactional
    public SolicitudResponseDTO finalizarSolicitud(Long idSolicitud, FinalizarSolicitudDTO dto) {
        Solicitud solicitud = findSolicitudById(idSolicitud);
        Contenedor contenedor = solicitud.getContenedor();

        // (Lógica Opcional: Pedir el costo real a ms-rutas en lugar de recibirlo)
        // RutaFinalizadaDTO rutaFinalizada = rutasWebClient.obtenerRutaFinalizada(idSolicitud).block();
        // solicitud.setCostoFinal(rutaFinalizada.getCostoTotalReal());
        // solicitud.setTiempoReal(rutaFinalizada.getTiempoTotalReal());
        
        // Siguiendo el DTO de la entrega inicial, recibimos los datos del operador:
        solicitud.setCostoFinal(dto.getCostoFinal());
        solicitud.setTiempoReal(dto.getTiempoReal());
        solicitud.setEstado("Entregada");

        // Añadimos el estado final al historial
        EstadoContenedor estadoEntregado = new EstadoContenedor("Entregada", LocalDateTime.now(), contenedor);
        contenedor.getHistorialEstados().add(estadoEntregado);

        Solicitud solicitudActualizada = solicitudRepository.save(solicitud);
        return mapToSolicitudResponseDTO(solicitudActualizada);
    }


    /**
     * REQ 2: Consultar el estado del transporte (Endpoint: GET /solicitudes/{id}/estado)
     * (Tu método 'consultarEstadoSolicitud' pero adaptado para seguimiento)
     */
    @Transactional(readOnly = true)
    public SeguimientoDTO obtenerSeguimientoSolicitud(Long idSolicitud) {
        Solicitud solicitud = findSolicitudById(idSolicitud);
        EstadoContenedor estadoActualDB = solicitud.getContenedor().getEstadoActual();

        if (estadoActualDB == null) {
            throw new RuntimeException("La solicitud no tiene ningún estado registrado.");
        }

        // Si el estado es "Borrador" o "Programada", la ubicación es el origen.
        if (estadoActualDB.getNombre().equals("Borrador") || estadoActualDB.getNombre().equals("Programada")) {
            return new SeguimientoDTO(estadoActualDB.getNombre(), "Origen (pendiente de retiro)");
        }
        
        // Si ya está "Entregada", la ubicación es el destino.
        if (estadoActualDB.getNombre().equals("Entregada")) {
             return new SeguimientoDTO(estadoActualDB.getNombre(), "Destino (entrega finalizada)");
        }

        // Para "En Transito" o "En Deposito", DEBEMOS consultar a ms-rutas
        try {
            SeguimientoDTO seguimientoRuta = rutasWebClient.obtenerUbicacionActual(idSolicitud).block();
            if (seguimientoRuta != null) {
                return seguimientoRuta; // Devuelve la info de ms-rutas (ej: "En Deposito", "Deposito X")
            }
        } catch (Exception e) {
            // Si ms-rutas falla, devolvemos el último estado local.
             return new SeguimientoDTO(estadoActualDB.getNombre(), "Ubicación desconocida (error de comunicación)");
        }
        
        return new SeguimientoDTO(estadoActualDB.getNombre(), "Ubicación no disponible");
    }

    /**
     * Endpoint: GET /solicitudes/{id}
     */
    @Transactional(readOnly = true)
    public SolicitudResponseDTO obtenerSolicitudPorId(Long idSolicitud) {
        Solicitud solicitud = findSolicitudById(idSolicitud);
        return mapToSolicitudResponseDTO(solicitud);
    }


    /**
     * Endpoint: GET /solicitudes
     * (Tu método 'obtenerTodasLasSolicitudes' filtrado por estado)
     */
    @Transactional(readOnly = true)
    public List<SolicitudResponseDTO> obtenerSolicitudes(String estado) {
        List<Solicitud> solicitudes;
        if (estado != null && !estado.isEmpty()) {
            // Filtra por el estado de la *Solicitud* (Borrador, Programada, Entregada)
            solicitudes = solicitudRepository.findByEstado(estado);
        } else {
            solicitudes = solicitudRepository.findAll();
        }
        
        return solicitudes.stream()
                .map(this::mapToSolicitudResponseDTO)
                .toList();
    }

    // --- Métodos Utilitarios ---

    /**
     * Busca una solicitud o lanza una excepción estándar.
     */
    private Solicitud findSolicitudById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID de la solicitud no puede ser nulo");
        }
        return solicitudRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada: " + id));
    }

    /**
     * Mapeador centralizado para convertir Entidad a ResponseDTO.
     */
    private SolicitudResponseDTO mapToSolicitudResponseDTO(Solicitud solicitud) {
        SolicitudResponseDTO dto = new SolicitudResponseDTO();
        dto.setIdSolicitud(solicitud.getId());
        dto.setNombreCliente(solicitud.getCliente().getNombre() + " " + solicitud.getCliente().getApellido());
        
        EstadoContenedor estadoActual = solicitud.getContenedor().getEstadoActual();
        dto.setEstadoActual(estadoActual != null ? estadoActual.getNombre() : "SIN_ESTADO");
        
        dto.setCostoEstimado(solicitud.getCostoEstimado());
        dto.setCostoFinal(solicitud.getCostoFinal());
        dto.setTiempoEstimado(solicitud.getTiempoEstimado());
        dto.setTiempoReal(solicitud.getTiempoReal());
        
        return dto;
    }
}