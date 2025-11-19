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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime; // <-- IMPORTADO
import java.util.List;
import java.util.Optional; // <-- IMPORTADO
import java.util.stream.Collectors;

@Service
public class SolicitudService {

    private static final Logger logger = LoggerFactory.getLogger(SolicitudService.class);

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
     * Auto-crea el cliente si no existe.
     */
    @Transactional
    public SolicitudResponseDTO crearSolicitud(SolicitudRequestDTO dto) {
        // 1. Buscar o crear cliente
        Cliente cliente = clienteService.buscarOcreaerCliente(dto.getClienteDni());

        // 2. Crear contenedor (lógica existente)
        Contenedor contenedor = new Contenedor();
        contenedor.setPeso(BigDecimal.valueOf(dto.getPesoContenedor()));
        contenedor.setVolumen(BigDecimal.valueOf(dto.getVolumenContenedor()));
        contenedor.setClienteAsociado(cliente);

        // 3. Crear solicitud (lógica existente)
        Solicitud solicitud = new Solicitud();
        solicitud.setCliente(cliente);
        solicitud.setContenedor(contenedor);
        solicitud.setOrigenLatitud(BigDecimal.valueOf(dto.getOrigenLatitud()));
        solicitud.setOrigenLongitud(BigDecimal.valueOf(dto.getOrigenLongitud()));
        solicitud.setDestinoLatitud(BigDecimal.valueOf(dto.getDestinoLatitud()));
        solicitud.setDestinoLongitud(BigDecimal.valueOf(dto.getDestinoLongitud()));
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
            logger.error("Error al calcular costo/tiempo estimado: ", e);
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

 
private CostoTiempoDTO calcularCostoTiempoEstimado(
            Double lat1, Double lon1, Double lat2, Double lon2,
            Double peso, Double volumen) {

        try {
            logger.info("Iniciando cálculo ROBUSTO de costo/tiempo estimado...");
            
            // 1. Obtener distancia base de Google (A -> B directo)
            DistanciaDTO distancia = rutasWebClient.obtenerDistancia(lat1, lon1, lat2, lon2);
            BigDecimal distanciaDirectaKm = distancia.getDistanciaMetros().divide(new BigDecimal(1000));
            
            // MEJORA 1: Factor de Desvío (Ruta Real vs Directa)
            // La ruta real pasa por depósitos, nunca es línea recta. Agregamos un 25% de margen.
            BigDecimal factorDesvio = new BigDecimal("1.25");
            BigDecimal distanciaEstimadaTotal = distanciaDirectaKm.multiply(factorDesvio);
            
            logger.info("Distancia directa: {} km. Distancia estimada con desvíos: {} km", 
                    distanciaDirectaKm, distanciaEstimadaTotal);

            // 2. Obtener promedios y tarifas
            PromediosDTO promedios = camionesWebClient.obtenerPromedios(peso, volumen);
            TarifaDTO tarifas = rutasWebClient.obtenerTarifas();

            // MEJORA 2: Margen de Seguridad en Costos de Camión
            // No uses el promedio puro, usa un margen (1.15) por si toca un camión más caro.
            BigDecimal margenSeguridad = new BigDecimal("1.15");
            
            BigDecimal costoKmSeguro = promedios.getCostoPromedioPorKm().multiply(margenSeguridad);
            BigDecimal consumoKmSeguro = promedios.getConsumoPromedioPorKm().multiply(margenSeguridad);

            // Cálculo de costos de transporte
            BigDecimal costoTramo = distanciaEstimadaTotal.multiply(costoKmSeguro);
            BigDecimal costoCombustible = distanciaEstimadaTotal
                    .multiply(consumoKmSeguro)
                    .multiply(tarifas.getPrecioLitro());

            // MEJORA 3: Estimación de Estadías (Tramos)
            // Si el viaje es largo, habrá paradas intermedias (tramos). 
            // Estimamos 1 tramo inicial + 1 parada extra cada 600km.
            // Ejemplo: 100km -> 1 tramo. 700km -> 2 tramos.
            int tramosEstimados = 1 + distanciaEstimadaTotal.divide(new BigDecimal(600), 0, RoundingMode.UP).intValue();
            
            BigDecimal costoEstadiaDiario = tarifas.getCostoEstadiaDiario() != null 
                    ? tarifas.getCostoEstadiaDiario() 
                    : BigDecimal.ZERO;
            
            // Cobramos una estadía por cada tramo estimado
            BigDecimal costoEstadiaTotal = costoEstadiaDiario.multiply(new BigDecimal(tramosEstimados));

            BigDecimal costoTotal = costoTramo.add(costoCombustible).add(costoEstadiaTotal);
            
            // Redondear a 2 decimales
            costoTotal = costoTotal.setScale(2, RoundingMode.HALF_UP);

            // Calcular tiempo (distancia aumentada / 70 km/h promedio + 4 horas por tramo de gestión)
            BigDecimal tiempoViaje = distanciaEstimadaTotal.divide(new BigDecimal(70), 2, RoundingMode.HALF_UP);
            BigDecimal tiempoGestion = new BigDecimal(tramosEstimados * 4); // 4 horas por parada
            BigDecimal tiempoTotalHoras = tiempoViaje.add(tiempoGestion);

            logger.info("Estimación Final: Costo=${} (Tramos est: {}), Tiempo={}hs", 
                    costoTotal, tramosEstimados, tiempoTotalHoras);
            
            return new CostoTiempoDTO(costoTotal, tiempoTotalHoras);
            
        } catch (Exception e) {
            logger.error("Error en calcularCostoTiempoEstimado: ", e);
            // En caso de error, devolver ceros para no romper la creación, pero loguear el fallo
            return new CostoTiempoDTO(BigDecimal.ZERO, BigDecimal.ZERO);
        
    }
    }


    @Transactional
    public SolicitudResponseDTO finalizarSolicitud(Long id, FinalizarSolicitudDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("DTO de finalización no puede ser nulo");
        }
        
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        solicitud.setEstado("ENTREGADA");
        
        // Determinar el costo final: SIEMPRE intentar obtener el costo real de ms-rutas primero
        BigDecimal costoFinal = BigDecimal.ZERO;
        
        try {
            // Intentar obtener el costo real de ms-rutas (basado en tramos completados)
            CostoTrasladoDTO costoTraslado = rutasWebClient.obtenerCostoTrasladoRealPorSolicitud(id);
            if (costoTraslado != null && costoTraslado.getCostoTotal() != null && costoTraslado.getCostoTotal().signum() > 0) {
                costoFinal = costoTraslado.getCostoTotal();
                logger.info("Costo final obtenido de ms-rutas para solicitud {}: {}", id, costoFinal);
            }
        } catch (Exception e) {
            logger.warn("No se pudo obtener costo real de ms-rutas para idSolicitud={}: {}. Usando costoEstimado.", id, e.getMessage());
            // Si falla, usar el costo estimado
            if (solicitud.getCostoEstimado() != null && solicitud.getCostoEstimado().signum() > 0) {
                costoFinal = solicitud.getCostoEstimado();
                logger.info("Usando costoEstimado como costoFinal para solicitud {}: {}", id, costoFinal);
            }
        }
        
        solicitud.setCostoFinal(costoFinal);
        
        // Obtener el tiempo real desde ms-rutas (en HORAS, basado en fechas de tramos)
        BigDecimal tiempoReal = BigDecimal.ZERO;
        try {
            Double tiempoRealHoras = rutasWebClient.obtenerTiempoRealPorSolicitud(id);
            if (tiempoRealHoras != null && tiempoRealHoras > 0) {
                tiempoReal = java.math.BigDecimal.valueOf(tiempoRealHoras);
                logger.info("Tiempo real obtenido de ms-rutas para solicitud {} (en horas): {}", id, tiempoReal);
            }
        } catch (Exception e) {
            logger.warn("No se pudo obtener tiempo real de ms-rutas para idSolicitud={}. Usando valor del DTO o 0.", id);
            // Si falla, usar el valor del DTO si está disponible
            if (dto.getTiempoReal() != null && dto.getTiempoReal().signum() > 0) {
                tiempoReal = dto.getTiempoReal();
                logger.info("Usando tiempoReal del DTO para solicitud {}: {}", id, tiempoReal);
            }
        }
        
        solicitud.setTiempoReal(tiempoReal);

   
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
     * Utiliza el historial del contenedor (EstadoContenedor) como fuente principal de verdad.
     * El estado se sincroniza automáticamente via actualizarEstadoContenedor() 
     * que se debe llamar desde ms-rutas cuando cambian los estados de tramos.

     */
    public SeguimientoDTO obtenerEstado(Long id) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        Contenedor contenedor = solicitud.getContenedor();
        String estadoDeducido = "PENDIENTE";
        String descripcion = "Estado: PENDIENTE";

        EstadoContenedor estadoActual = contenedor.getEstadoActual();

        if (estadoActual != null && estadoActual.getNombre() != null && !estadoActual.getNombre().isBlank()) {
            estadoDeducido = estadoActual.getNombre();
            descripcion = "Ubicación: " + estadoActual.getNombre() + " (actualizado a: " + estadoActual.getFecha() + ")";
            logger.info("Estado de solicitud {} obtenido desde historial del contenedor: {}", id, estadoDeducido);
        } else if (solicitud.getEstado() != null && !solicitud.getEstado().isBlank()) {
            // Fallback al campo estado de la solicitud si no hay historial
            estadoDeducido = solicitud.getEstado();
            descripcion = "Estado: " + estadoDeducido;
            logger.info("Estado de solicitud {} obtenido desde campo estado (sin historial): {}", id, estadoDeducido);
        }

        return new SeguimientoDTO(estadoDeducido, descripcion);
    }

    /**
     * Actualiza el estado de un contenedor.
     * (LÓGICA ACTUALIZADA - YA NO ESTÁ VACÍA)
     */
    @Transactional
    public void actualizarEstadoContenedor(Long idContenedor, String nuevoEstado) {
        Contenedor contenedor = contenedorRepository.findById(idContenedor)
                .orElseThrow(() -> new EntityNotFoundException("Contenedor no encontrado"));


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
    
    }


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


    private static class CostoTiempoDTO {
        BigDecimal costo;
        BigDecimal tiempo;

        CostoTiempoDTO(BigDecimal costo, BigDecimal tiempo) {
            this.costo = costo;
            this.tiempo = tiempo;
        }
    }
}