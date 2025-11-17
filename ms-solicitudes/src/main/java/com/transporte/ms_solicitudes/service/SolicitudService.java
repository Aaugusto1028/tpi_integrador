package com.transporte.ms_solicitudes.service;

import com.transporte.ms_solicitudes.client.CamionesWebClient;
import com.transporte.ms_solicitudes.client.RutasWebClient;
import com.transporte.ms_solicitudes.dto.*;
import com.transporte.ms_solicitudes.model.Cliente;
import com.transporte.ms_solicitudes.model.Contenedor;
import com.transporte.ms_solicitudes.model.EstadoContenedor;
import com.transporte.ms_solicitudes.model.Solicitud;
import com.transporte.ms_solicitudes.repository.ClienteRepository;
import com.transporte.ms_solicitudes.repository.SolicitudRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    
    // Inyectamos los WebClients (asegúrate que estén en WebClientConfig)
    @Autowired
    private RutasWebClient rutasWebClient;
    @Autowired
    private CamionesWebClient camionesWebClient;

    /**
     * REQ 1: Crear Solicitud (Corregido)
     */
    @Transactional
    public SolicitudResponseDTO crearSolicitud(SolicitudRequestDTO request) {
        
        // 1. Buscar Cliente
        Cliente cliente = clienteRepository.findByDni(request.getClienteDni())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con DNI: " + request.getClienteDni()));

        // 2. Crear Contenedor
        Contenedor contenedor = new Contenedor();
        contenedor.setPeso(request.getPesoContenedor());
        contenedor.setVolumen(request.getVolumenContenedor());
        contenedor.setClienteAsociado(cliente); 

        // 3. Crear Estado Inicial
        // Usamos el constructor corregido (sin nombres de parámetros)
        EstadoContenedor estadoActual = new EstadoContenedor("Borrador", LocalDateTime.now(), contenedor);
        contenedor.setHistorialEstados(List.of(estadoActual));

        // 4. Crear Solicitud
        Solicitud solicitud = new Solicitud();
        solicitud.setCliente(cliente);
        solicitud.setContenedor(contenedor);
        solicitud.setEstado("Borrador");
        
        // Guardamos las coordenadas
        solicitud.setOrigenLatitud(request.getOrigenLatitud());
        solicitud.setOrigenLongitud(request.getOrigenLongitud());
        solicitud.setDestinoLatitud(request.getDestinoLatitud());
        solicitud.setDestinoLongitud(request.getDestinoLongitud());

        // El costo estimado se pone en null. Se calcula en el Flujo 2.
        solicitud.setCostoEstimado(null); 

        Solicitud solicitudGuardada = solicitudRepository.save(solicitud);

        // 5. Mapear respuesta
        return mapToSolicitudResponseDTO(solicitudGuardada);
    }

    /**
     * REQ 3 y 8: Calcular Costo Estimado (¡Esto es lo que sigue!)
     */
    @Transactional
    public SolicitudResponseDTO calcularCostoTiempoEstimado(Long idSolicitud) {
        System.out.println("Calculando costo para solicitud: " + idSolicitud);
        // 1. Obtener Solicitud
        Solicitud solicitud = findSolicitudById(idSolicitud);
        Contenedor contenedor = solicitud.getContenedor();

        // 2. LLAMAR A MS-CAMIONES (WebClient)
        // REQ 11: Validar capacidad
        List<CamionDTO> camionesAptos = camionesWebClient.obtenerCamionesAptos(
                contenedor.getPeso(),
                contenedor.getVolumen()
        ).block(); // .block() hace la llamada síncrona

        if (camionesAptos == null || camionesAptos.isEmpty()) {
            throw new RuntimeException("No se encontraron camiones aptos.");
        }
        System.out.println("Camiones aptos encontrados: " + camionesAptos.size());

        // 3. LLAMAR A MS-CAMIONES (WebClient)
        // REQ 105: "valores promedio entre los camiones elegibles"
        List<String> dominios = camionesAptos.stream().map(CamionDTO::getDominio).collect(Collectors.toList());
        PromediosDTO promedios = camionesWebClient.obtenerPromediosCostos(dominios).block();
        
        // 4. LLAMAR A MS-RUTAS (WebClient)
        // REQ 8.1: Distancia y REQ 106: Tiempo
        CoordenadasRequest coords = new CoordenadasRequest(
                solicitud.getOrigenLatitud(), solicitud.getOrigenLongitud(),
                solicitud.getDestinoLatitud(), solicitud.getDestinoLongitud()
        );
        DistanciaResponse distancia = rutasWebClient.obtenerDistanciaEstimada(coords).block();
        
        // REQ 64: Precio combustible
        TarifaDTO tarifa = rutasWebClient.obtenerTarifaVigente().block();

        // 5. LÓGICA DE NEGOCIO (LOCAL)
        if (promedios == null || distancia == null || tarifa == null) {
            throw new RuntimeException("No se pudo obtener la información completa de costos y rutas.");
        }

        BigDecimal distanciaEnKm = distancia.getDistanciaMetros().divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);
        BigDecimal tiempoEnHoras = distancia.getDuracionSegundos().divide(new BigDecimal("3600"), 2, RoundingMode.HALF_UP);

        // Fórmula (Regla 103, 105): Costo = (Costo Base Km * Dist) + (Consumo Prom * Dist * Precio Comb)
        BigDecimal costoBase = promedios.getCostoPromedioPorKm().multiply(distanciaEnKm);
        BigDecimal costoCombustible = promedios.getConsumoPromedioPorKm()
                                            .multiply(distanciaEnKm)
                                            .multiply(tarifa.getPrecioLitro());
        
        BigDecimal costoEstimadoTotal = costoBase.add(costoCombustible).setScale(2, RoundingMode.HALF_UP);
        System.out.println("Costo estimado calculado: " + costoEstimadoTotal);

        // 6. Actualizar Entidades
        solicitud.setCostoEstimado(costoEstimadoTotal);
        solicitud.setTiempoEstimado(tiempoEnHoras);
        solicitud.setEstado("Programada");

        EstadoContenedor estadoProgramado = new EstadoContenedor("Programada", LocalDateTime.now(), contenedor);
        contenedor.getHistorialEstados().add(estadoProgramado);
        
        Solicitud solicitudActualizada = solicitudRepository.save(solicitud);
        return mapToSolicitudResponseDTO(solicitudActualizada);
    }
    
    /**
     * REQ 9: Finalizar Solicitud (¡Esto es lo que sigue!)
     */
    @Transactional
    public SolicitudResponseDTO finalizarSolicitud(Long idSolicitud, FinalizarSolicitudDTO dto) {
        Solicitud solicitud = findSolicitudById(idSolicitud);

        // Lógica simple: El operador nos pasa el costo y tiempo final (según entrega inicial)
        solicitud.setCostoFinal(dto.getCostoFinal());
        solicitud.setTiempoReal(dto.getTiempoReal());
        solicitud.setEstado("Entregada");

        // (Lógica Opcional Avanzada: podrías llamar a ms-rutas para que calcule el costo final
        // basado en los tramos reales, estadías, etc. y no confiar en el DTO de entrada)

        EstadoContenedor estadoEntregado = new EstadoContenedor("Entregada", LocalDateTime.now(), solicitud.getContenedor());
        solicitud.getContenedor().getHistorialEstados().add(estadoEntregado);

        Solicitud solicitudActualizada = solicitudRepository.save(solicitud);
        return mapToSolicitudResponseDTO(solicitudActualizada);
    }

    /**
     * REQ 2: Consultar Estado/Seguimiento (Corregido)
     */
    @Transactional(readOnly = true)
    public SeguimientoDTO consultarEstadoSolicitud(Long idSolicitud) {
        Solicitud solicitud = findSolicitudById(idSolicitud);
        EstadoContenedor estadoActualDB = solicitud.getContenedor().getEstadoActual();
        
        if (estadoActualDB == null) {
            throw new RuntimeException("La solicitud no tiene ningún estado registrado.");
        }

        String estadoNombre = estadoActualDB.getNombre();

        // Flujo 3: Si está "en viaje", preguntamos a ms-rutas
        if (estadoNombre.equals("En Transito") || estadoNombre.equals("En Deposito")) {
            try {
                // LLAMAR A MS-RUTAS (WebClient)
                SeguimientoDTO seguimientoRuta = rutasWebClient.obtenerUbicacionActual(idSolicitud).block();
                if (seguimientoRuta != null) {
                    return seguimientoRuta; // Ej: {"estado": "En Deposito", "ubicacion": "Depósito Central"}
                }
            } catch (Exception e) {
                 return new SeguimientoDTO(estadoNombre, "Ubicación no disponible (error de red)");
            }
        }
        
        // Si no está "en viaje", respondemos con el estado local
        String ubicacionLocal = "Origen (pendiente de retiro)";
        if (estadoNombre.equals("Borrador")) ubicacionLocal = "Origen (solicitud no programada)";
        if (estadoNombre.equals("Programada")) ubicacionLocal = "Origen (programada para retiro)";
        if (estadoNombre.equals("Entregada")) ubicacionLocal = "Destino (entrega finalizada)";

        return new SeguimientoDTO(estadoNombre, ubicacionLocal);
    }

    /**
     * GET /solicitudes (Corregido)
     */
    @Transactional(readOnly = true)
    public List<SolicitudResponseDTO> obtenerTodasLasSolicitudes(String estado) {
        // 1. Buscamos todas las solicitudes
        List<Solicitud> solicitudes;
        if (estado != null && !estado.isBlank()) {
            solicitudes = solicitudRepository.findByEstado(estado);
        } else {
            solicitudes = solicitudRepository.findAll();
        }
        
        // 2. Las convertimos a DTOs
        return solicitudes.stream()
                .map(this::mapToSolicitudResponseDTO)
                .toList();
    }
    
    /**
     * GET /solicitudes/{id} (¡Faltaba este!)
     */
    @Transactional(readOnly = true)
    public SolicitudResponseDTO obtenerSolicitudPorId(Long idSolicitud) {
        Solicitud solicitud = findSolicitudById(idSolicitud);
        return mapToSolicitudResponseDTO(solicitud);
    }

    // --- MÉTODOS PRIVADOS HELPER ---

    private Solicitud findSolicitudById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        return solicitudRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada: " + id));
    }

    private SolicitudResponseDTO mapToSolicitudResponseDTO(Solicitud solicitud) {
        SolicitudResponseDTO dto = new SolicitudResponseDTO();
        dto.setIdSolicitud(solicitud.getId());
        
        Cliente cliente = solicitud.getCliente();
        dto.setNombreCliente(cliente.getNombre() + " " + cliente.getApellido());
        
        // Usamos el método helper que agregamos en Contenedor.java
        EstadoContenedor estadoActual = solicitud.getContenedor().getEstadoActual();
        dto.setEstadoActual(estadoActual != null ? estadoActual.getNombre() : "SIN_ESTADO");

        dto.setCostoEstimado(solicitud.getCostoEstimado());
        dto.setCostoFinal(solicitud.getCostoFinal());
        dto.setTiempoEstimado(solicitud.getTiempoEstimado());
        dto.setTiempoReal(solicitud.getTiempoReal());
        
        return dto;
    }
}