package ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion;

import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.AsignarCamionRequest;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.CrearRutaRequest;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio.*;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.repositorios.*;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.TarifaDTO;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.CostoTrasladoDTO;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.TramoDTO;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.RutaDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RutaServiceImpl implements RutaService {

    // --- Constantes para IDs de Estados (¡No más "Magic Numbers"!) ---
    private static final class EstadosTramo {
        static final Long ESTIMADO = 1L;
        static final Long ASIGNADO = 2L;
        static final Long INICIADO = 3L;
        static final Long FINALIZADO = 4L;
    }

    // --- Repositorios Inyectados ---
    @Autowired
    private RutaRepository rutaRepository;
    @Autowired
    private TramoRepository tramoRepository;
    @Autowired
    private DepositoRepository depositoRepository;
    @Autowired
    private TarifaRepository tarifaRepository;
    @Autowired
    private TipoTramoRepository tipoTramoRepository;
    @Autowired
    private EstadoTramoRepository estadoTramoRepository;

    // --- Servicios Externos Inyectados ---
    @Autowired
    private GoogleMapsClient googleMapsClient;
    @Autowired
    private WebClient.Builder webClientBuilder;

    /**
     * DTO interno para mapear la respuesta del ms-camiones
     * (Asumimos que el ms-camiones nos devuelve esto)
     */
    @Data
    private static class CamionDTO {
        private String patente;
        private Double consumoCombustibleKm;
        private BigDecimal costoPorKm;
        private Double capacidadPeso;
        private Double capacidadVolumen;
        private Boolean disponibilidad;
    }

    // --- MÉTODOS PÚBLICOS (Casos de Uso) ---

    @Override
    @Transactional
    public Ruta crearRutaTentativa(CrearRutaRequest request) {
        Ruta ruta = new Ruta();
        ruta.setIdSolicitud(request.getIdSolicitud());
        ruta.setCantidadTramos(request.getTramos().size());
        Ruta rutaGuardada = rutaRepository.save(ruta);

        EstadoTramo estadoEstimado = buscarEstadoTramo(EstadosTramo.ESTIMADO);
        Tarifa tarifaBase = tarifaRepository.findById(1L) // Asumimos 1L = tarifa base
                .orElseThrow(() -> new EntityNotFoundException("Tarifa base no encontrada"));

        List<Tramo> tramosCreados = new ArrayList<>();
        for (CrearRutaRequest.TramoDTO tramoDto : request.getTramos()) {
            // Extraemos la lógica de creación a un método privado
            tramosCreados.add(crearTramo(tramoDto, rutaGuardada, estadoEstimado, tarifaBase));
        }

        rutaGuardada.setTramos(tramosCreados);
        return rutaGuardada;
    }

    @Override
    @Transactional
    public Tramo asignarCamionATramo(Long idTramo, AsignarCamionRequest request) {
        Tramo tramo = buscarTramo(idTramo);

        // Extraemos la validación a un método privado
        CamionDTO camionValidado = validarCamion(request.getPatenteCamion(), request.getPesoContenedor(),
                request.getVolumenContenedor());

        tramo.setPatenteCamionAsignado(camionValidado.getPatente());
        tramo.setEstadoTramo(buscarEstadoTramo(EstadosTramo.ASIGNADO));

        Tramo tramoGuardado = tramoRepository.save(tramo);

        // Notificar a ms-solicitudes que el camión ha sido asignado
        try {
            notificarEstadoContenedor(tramo.getRuta().getIdSolicitud(), "ASIGNADO");
        } catch (Exception e) {
            System.err.println("Error notificando asignación de camión a ms-solicitudes: " + e.getMessage());
        }

        return tramoGuardado;
    }

    @Override
    @Transactional
    public Tramo iniciarTramo(Long idTramo) {
        Tramo tramo = buscarTramo(idTramo);

        // Validar que el tramo esté 'ASIGNADO' antes de iniciarlo
        if (!tramo.getEstadoTramo().getId().equals(EstadosTramo.ASIGNADO)) {
            throw new IllegalStateException("El tramo no está asignado y no puede ser iniciado.");
        }

        tramo.setFechaHoraInicio(LocalDateTime.now());
        tramo.setEstadoTramo(buscarEstadoTramo(EstadosTramo.INICIADO));

        Tramo tramoGuardado = tramoRepository.save(tramo);

        // Notificar a ms-solicitudes que el tramo ha sido iniciado
        try {
            notificarEstadoContenedor(tramo.getRuta().getIdSolicitud(), "INICIADO");
        } catch (Exception e) {
            System.err.println("Error notificando inicio de tramo a ms-solicitudes: " + e.getMessage());
        }

        return tramoGuardado;
    }

    @Override
    @Transactional
    public Tramo finalizarTramo(Long idTramo) {
        Tramo tramo = buscarTramo(idTramo);

        // Validar que el tramo esté 'INICIADO'
        if (!tramo.getEstadoTramo().getId().equals(EstadosTramo.INICIADO)) {
            throw new IllegalStateException("El tramo no está iniciado y no puede ser finalizado.");
        }

        tramo.setFechaHoraFin(LocalDateTime.now());
        tramo.setEstadoTramo(buscarEstadoTramo(EstadosTramo.FINALIZADO));

        // Extraemos el cálculo de costo real a un método privado
        BigDecimal costoReal = calcularCostoRealTramo(tramo);
        tramo.setCostoReal(costoReal);

        Tramo tramoGuardado = tramoRepository.save(tramo);

        // Notificar a ms-solicitudes que el contenedor ha sido finalizado
        try {
            notificarEstadoContenedor(tramo.getRuta().getIdSolicitud(), "FINALIZADO");
        } catch (Exception e) {
            System.err.println("Error notificando finalización de tramo a ms-solicitudes: " + e.getMessage());
        }

        return tramoGuardado;
    }

    // --- MÉTODOS PRIVADOS (Lógica interna) ---

    /**
     * Crea y guarda un único tramo, llamando a Google Maps y calculando costos.
     */
    private Tramo crearTramo(CrearRutaRequest.TramoDTO dto, Ruta ruta, EstadoTramo estado, Tarifa tarifaBase) {
        Deposito origen = buscarDeposito(dto.getIdDepositoOrigen());
        Deposito destino = buscarDeposito(dto.getIdDepositoDestino());
        TipoTramo tipoTramo = tipoTramoRepository.findById(dto.getIdTipoTramo())
                .orElseThrow(() -> new EntityNotFoundException("Tipo de tramo no encontrado: " + dto.getIdTipoTramo()));

        String origenCoords = origen.getLatitud() + "," + origen.getLongitud();
        String destinoCoords = destino.getLatitud() + "," + destino.getLongitud();

        // Llamada a Google Maps
        Long distanciaMetros = googleMapsClient.getDistanciaEnMetros(origenCoords, destinoCoords)
                .blockOptional(Duration.ofSeconds(5)) // .block() con timeout de 5s
                .orElse(0L);

        double distanciaKm = distanciaMetros / 1000.0;

        // Lógica de costo estimado (ej: $150 por km)
        BigDecimal costoEstimado = tarifaBase.getPrecioLitro().multiply(BigDecimal.valueOf(distanciaKm));

        Tramo tramo = new Tramo();
        tramo.setRuta(ruta);
        tramo.setDepositoOrigen(origen);
        tramo.setDepositoDestino(destino);
        tramo.setTipoTramo(tipoTramo);
        tramo.setEstadoTramo(estado);
        tramo.setCostoAproximado(costoEstimado);
        tramo.setDistanciaKm(distanciaKm); // ¡Guardamos la distancia!

        return tramoRepository.save(tramo);
    }

    /**
     * Llama al ms-camiones para validar y obtener datos de un camión.
     * Primero busca camiones disponibles que cumplan con los requisitos de peso/volumen,
     * luego valida que la patente especificada sea una de ellas y que esté disponible.
     */
    private CamionDTO validarCamion(String patente, Double peso, Double volumen) {
        // 1. Llamar a buscar-apto para encontrar camiones disponibles
        String urlBuscarApto = String.format("http://ms-camiones:8083/camiones/buscar-apto?peso=%s&volumen=%s", peso, volumen);
        
        List<CamionDTO> camionesApts;
        try {
            camionesApts = webClientBuilder.build().get()
                    .uri(urlBuscarApto)
                    .retrieve()
                    .bodyToFlux(CamionDTO.class)
                    .collectList()
                    .block(Duration.ofSeconds(5)); // Timeout de 5s
            
            if (camionesApts == null) {
                camionesApts = new ArrayList<>();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al buscar camiones disponibles en ms-camiones: " + e.getMessage());
        }
        
        // 2. Buscar la patente especificada en los camiones aptos
        CamionDTO camionSolicitado = camionesApts.stream()
                .filter(c -> c.getPatente().equalsIgnoreCase(patente))
                .findFirst()
                .orElse(null);
        
        // Si no está en los aptos, obtener directamente por patente (fallback)
        if (camionSolicitado == null) {
            String urlDirecta = String.format("http://ms-camiones:8083/camiones/%s", patente);
            try {
                camionSolicitado = webClientBuilder.build().get()
                        .uri(urlDirecta)
                        .retrieve()
                        .bodyToMono(CamionDTO.class)
                        .block(Duration.ofSeconds(5)); // Timeout de 5s
            } catch (Exception e) {
                throw new RuntimeException("Error al obtener camión " + patente + " de ms-camiones: " + e.getMessage());
            }
        }
        
        // 3. Validaciones finales
        if (camionSolicitado == null) {
            throw new EntityNotFoundException("Camión no encontrado: " + patente);
        }
        
        if (camionSolicitado.getDisponibilidad() == null || !camionSolicitado.getDisponibilidad()) {
            throw new IllegalStateException("El camión " + patente + " no está disponible para asignación.");
        }
        
        if (camionSolicitado.getCapacidadPeso() == null || camionSolicitado.getCapacidadVolumen() == null) {
            throw new IllegalStateException("El camión " + patente + " no tiene datos de capacidad definidos.");
        }
        
        if (camionSolicitado.getCapacidadPeso() < peso) {
            throw new IllegalStateException("El camión " + patente + " no soporta el peso (" + peso + "kg) del contenedor. Capacidad máxima: " + camionSolicitado.getCapacidadPeso() + "kg");
        }
        
        if (camionSolicitado.getCapacidadVolumen() < volumen) {
            throw new IllegalStateException("El camión " + patente + " no soporta el volumen (" + volumen + "m³) del contenedor. Capacidad máxima: " + camionSolicitado.getCapacidadVolumen() + "m³");
        }
        
        return camionSolicitado;
    }

    /**
     * Llama al ms-camiones para obtener los datos de un camión (sin validar).
     * Usa el endpoint /camiones/detalle/{patente} que devuelve CamionDTO con todos los datos.
     */
    private CamionDTO buscarCamion(String patente) {
        String url = String.format("http://ms-camiones:8083/camiones/detalle/%s", patente);
        try {
            CamionDTO camion = webClientBuilder.build().get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(CamionDTO.class)
                    .block(Duration.ofSeconds(5));
            if (camion == null)
                throw new EntityNotFoundException("Camión no encontrado: " + patente);
            return camion;
        } catch (Exception e) {
            throw new RuntimeException("Error al conectar con ms-camiones: " + e.getMessage());
        }
    }

    /**
     * Calcula el costo real de un tramo finalizado.
     */
    /**
     * Calcula el costo real de un tramo finalizado.
     */
    private BigDecimal calcularCostoRealTramo(Tramo tramo) {
        // 1. Obtener datos del camión
        CamionDTO camion = buscarCamion(tramo.getPatenteCamionAsignado());

        // 2. Obtener tarifa de combustible
        Tarifa tarifaBase = tarifaRepository.findById(1L) // Asumimos 1L = tarifa base
                .orElseThrow(() -> new EntityNotFoundException("Tarifa base no encontrada"));

        // 3. Calcular costo por km (Manejo seguro de nulos)
        BigDecimal costoKmBase = camion.getCostoPorKm() != null ? camion.getCostoPorKm() : BigDecimal.ZERO;
        BigDecimal costoKm = costoKmBase.multiply(BigDecimal.valueOf(tramo.getDistanciaKm()));

        // 4. Calcular costo de combustible (CORRECCIÓN AQUÍ)
        Double consumo = camion.getConsumoCombustibleKm();
        if (consumo == null) {
            consumo = 0.0; // Valor por defecto para evitar NPE
        }
        
        double consumoTotalLitros = consumo * tramo.getDistanciaKm();
        
        BigDecimal precioLitro = tarifaBase.getPrecioLitro() != null ? tarifaBase.getPrecioLitro() : BigDecimal.ZERO;
        BigDecimal costoCombustible = precioLitro.multiply(BigDecimal.valueOf(consumoTotalLitros));

        // 5. Calcular costo de estadía (si aplica)
        BigDecimal costoEstadia = BigDecimal.ZERO;

        // 6. Sumar todo
        return costoKm.add(costoCombustible).add(costoEstadia);
    }

    // --- Métodos "helper" para evitar código repetido ---

    private Tramo buscarTramo(Long id) {
        return tramoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tramo no encontrado: " + id));
    }

    private Deposito buscarDeposito(Long id) {
        return depositoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Depósito no encontrado: " + id));
    }

    private EstadoTramo buscarEstadoTramo(Long id) {
        return estadoTramoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Estado de tramo no encontrado: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Tramo> obtenerTramosPorPatenteYEstado(String patente, Long estadoId, org.springframework.data.domain.Pageable pageable) {
        return tramoRepository.findByPatenteCamionAsignadoAndEstadoTramoId(patente, estadoId, pageable);
    }

    @Override
    @Transactional
    public Ruta asignarRuta(Long idRuta) {
        Ruta ruta = rutaRepository.findById(idRuta)
                .orElseThrow(() -> new EntityNotFoundException("Ruta no encontrada: " + idRuta));

        ruta.setAsignada(true);
        Ruta guardada = rutaRepository.save(ruta);

        // Intentamos notificar a ms-solicitudes para confirmar la ruta (best-effort)
        try {
            String url = String.format("http://ms-solicitudes:8081/solicitudes/%d/confirmar-ruta", ruta.getIdSolicitud());
            webClientBuilder.build().post().uri(url).retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(5));
        } catch (Exception ignored) {
            // best-effort: si falla la notificación no interrumpimos el flujo
        }

        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public TarifaDTO obtenerTarifas() {
        java.util.List<Tarifa> tarifas = tarifaRepository.findAll();
        TarifaDTO dto = new TarifaDTO();
        if (tarifas == null || tarifas.isEmpty()) {
            dto.setPrecioLitro(java.math.BigDecimal.ZERO);
            dto.setCostoEstadiaDiario(java.math.BigDecimal.ZERO);
            return dto;
        }
        Tarifa primera = tarifas.get(0);
        dto.setPrecioLitro(primera.getPrecioLitro() != null ? primera.getPrecioLitro() : java.math.BigDecimal.ZERO);
        dto.setCostoEstadiaDiario(java.math.BigDecimal.ZERO);
        return dto;
    }

    /**
     * Obtiene el costo real desglosado de un traslado (ruta con tramos) por idSolicitud.
     * Suma los costos de todos los tramos asignados y finalizados.
     * @param idSolicitud ID de la solicitud (para buscar su ruta asociada)
     * @return CostoTrasladoDTO con desglose de costos
     */
    @Override
    @Transactional(readOnly = true)
    public CostoTrasladoDTO obtenerCostoTrasladoRealPorSolicitud(Long idSolicitud) {
        // Buscar la ruta asociada a esta solicitud
        java.util.List<Ruta> rutas = rutaRepository.findByIdSolicitud(idSolicitud);
        if (rutas == null || rutas.isEmpty()) {
            // Si no hay ruta, devolver costos cero
            return new CostoTrasladoDTO(
                    java.math.BigDecimal.ZERO,
                    java.math.BigDecimal.ZERO,
                    java.math.BigDecimal.ZERO,
                    java.math.BigDecimal.ZERO
            );
        }

        // Usar la primera ruta (normalmente habrá una sola)
        Ruta ruta = rutas.get(0);

        java.util.List<Tramo> tramos = ruta.getTramos();
        if (tramos == null || tramos.isEmpty()) {
            return new CostoTrasladoDTO(
                    java.math.BigDecimal.ZERO,
                    java.math.BigDecimal.ZERO,
                    java.math.BigDecimal.ZERO,
                    java.math.BigDecimal.ZERO
            );
        }

        BigDecimal costoKmTotal = java.math.BigDecimal.ZERO;
        BigDecimal costoCombustibleTotal = java.math.BigDecimal.ZERO;
        BigDecimal costoEstadiaTotal = java.math.BigDecimal.ZERO;

        Tarifa tarifaBase = tarifaRepository.findById(1L)
                .orElseThrow(() -> new EntityNotFoundException("Tarifa base no encontrada"));

        for (Tramo tramo : tramos) {
            if (tramo.getPatenteCamionAsignado() == null || tramo.getDistanciaKm() == null) {
                continue; // Saltar tramos sin camión asignado o distancia
            }

            try {
                // Obtener datos del camión desde ms-camiones
                CamionDTO camion = buscarCamion(tramo.getPatenteCamionAsignado());

                // Calcular costos para este tramo
                BigDecimal costoKmTramo = camion.getCostoPorKm()
                        .multiply(java.math.BigDecimal.valueOf(tramo.getDistanciaKm()));
                costoKmTotal = costoKmTotal.add(costoKmTramo);

                double consumoTotalLitros = camion.getConsumoCombustibleKm() * tramo.getDistanciaKm();
                BigDecimal costoCombustibleTramo = tarifaBase.getPrecioLitro()
                        .multiply(java.math.BigDecimal.valueOf(consumoTotalLitros));
                costoCombustibleTotal = costoCombustibleTotal.add(costoCombustibleTramo);

                // Costo de estadía (simplificado: precio del depósito destino)
                if (tramo.getDepositoDestino() != null && tramo.getDepositoDestino().getPrecioEstadia() != null) {
                    costoEstadiaTotal = costoEstadiaTotal.add(tramo.getDepositoDestino().getPrecioEstadia());
                }
            } catch (Exception e) {
                // Si falla el cálculo para un tramo, lo registramos pero continuamos
                System.err.println("Error calculando costo para tramo " + tramo.getId() + ": " + e.getMessage());
            }
        }

        BigDecimal costoTotal = costoKmTotal.add(costoCombustibleTotal).add(costoEstadiaTotal);

        return new CostoTrasladoDTO(
                costoKmTotal,
                costoCombustibleTotal,
                costoEstadiaTotal,
                costoTotal
        );
    }

    /**
     * Obtiene el costo real desglosado de un traslado por idRuta.
     * Suma los costos de todos los tramos asignados y finalizados.
     * @param idRuta ID de la ruta
     * @return CostoTrasladoDTO con desglose de costos
     */
    @Override
    @Transactional(readOnly = true)
    public CostoTrasladoDTO obtenerCostoTrasladoReal(Long idRuta) {
        Ruta ruta = rutaRepository.findById(idRuta)
                .orElseThrow(() -> new EntityNotFoundException("Ruta no encontrada: " + idRuta));

        java.util.List<Tramo> tramos = ruta.getTramos();
        if (tramos == null || tramos.isEmpty()) {
            return new CostoTrasladoDTO(
                    java.math.BigDecimal.ZERO,
                    java.math.BigDecimal.ZERO,
                    java.math.BigDecimal.ZERO,
                    java.math.BigDecimal.ZERO
            );
        }

        BigDecimal costoKmTotal = java.math.BigDecimal.ZERO;
        BigDecimal costoCombustibleTotal = java.math.BigDecimal.ZERO;
        BigDecimal costoEstadiaTotal = java.math.BigDecimal.ZERO;

        Tarifa tarifaBase = tarifaRepository.findById(1L)
                .orElseThrow(() -> new EntityNotFoundException("Tarifa base no encontrada"));

        for (Tramo tramo : tramos) {
            if (tramo.getPatenteCamionAsignado() == null || tramo.getDistanciaKm() == null) {
                continue;
            }

            try {
                CamionDTO camion = buscarCamion(tramo.getPatenteCamionAsignado());

                BigDecimal costoKmTramo = camion.getCostoPorKm()
                        .multiply(java.math.BigDecimal.valueOf(tramo.getDistanciaKm()));
                costoKmTotal = costoKmTotal.add(costoKmTramo);

                double consumoTotalLitros = camion.getConsumoCombustibleKm() * tramo.getDistanciaKm();
                BigDecimal costoCombustibleTramo = tarifaBase.getPrecioLitro()
                        .multiply(java.math.BigDecimal.valueOf(consumoTotalLitros));
                costoCombustibleTotal = costoCombustibleTotal.add(costoCombustibleTramo);

                if (tramo.getDepositoDestino() != null && tramo.getDepositoDestino().getPrecioEstadia() != null) {
                    costoEstadiaTotal = costoEstadiaTotal.add(tramo.getDepositoDestino().getPrecioEstadia());
                }
            } catch (Exception e) {
                System.err.println("Error calculando costo para tramo " + tramo.getId() + ": " + e.getMessage());
            }
        }

        BigDecimal costoTotal = costoKmTotal.add(costoCombustibleTotal).add(costoEstadiaTotal);

        return new CostoTrasladoDTO(
                costoKmTotal,
                costoCombustibleTotal,
                costoEstadiaTotal,
                costoTotal
        );
    }

    /**
     * Obtiene todos los tramos asignados a una patente específica
     * y los convierte a TramoDTO.
     * @param patenteCamion Patente del camión
     * @return Lista de TramoDTO con todos los datos de los tramos asignados
     */
    @Override
    @Transactional(readOnly = true)
    public List<TramoDTO> obtenerTramosAsignadosPorPatente(String patenteCamion) {
        List<Tramo> tramos = tramoRepository.findByPatenteCamionAsignado(patenteCamion);
        List<TramoDTO> tramosDTO = new ArrayList<>();

        if (tramos == null || tramos.isEmpty()) {
            return tramosDTO;
        }

        for (Tramo tramo : tramos) {
            TramoDTO dto = new TramoDTO();
            dto.setId(tramo.getId());
            dto.setOrigen(tramo.getDepositoOrigen() != null ? tramo.getDepositoOrigen().getNombre() : null);
            dto.setDestino(tramo.getDepositoDestino() != null ? tramo.getDepositoDestino().getNombre() : null);
            dto.setEstado(tramo.getEstadoTramo() != null ? tramo.getEstadoTramo().getNombre() : null);
            dto.setPatenteCamionAsignado(tramo.getPatenteCamionAsignado());
            dto.setDistanciaKm(tramo.getDistanciaKm());
            dto.setCostoAproximado(tramo.getCostoAproximado());
            dto.setCostoReal(tramo.getCostoReal());
            dto.setFechaHoraInicio(tramo.getFechaHoraInicio());
            dto.setFechaHoraFin(tramo.getFechaHoraFin());
            
            // Precios de estadía de depósitos
            if (tramo.getDepositoOrigen() != null) {
                dto.setPrecioEstadiaOrigen(tramo.getDepositoOrigen().getPrecioEstadia());
            }
            if (tramo.getDepositoDestino() != null) {
                dto.setPrecioEstadiaDestino(tramo.getDepositoDestino().getPrecioEstadia());
            }

            tramosDTO.add(dto);
        }

        return tramosDTO;
    }

    /**
     * Notifica a ms-solicitudes que el estado del contenedor ha cambiado.
     * Envia un JSON con la estructura requerida por el endpoint actualizar estado.
     */
    private void notificarEstadoContenedor(Long idContenedor, String estado) {
        try {
            String url = "http://ms-solicitudes:8081/solicitudes/contenedores/" + idContenedor + "/estado";
            
            // Enviar JSON con formato esperado por ms-solicitudes
            String payload = "{\"estado\": \"" + estado + "\"}";
            
            webClientBuilder.build()
                    .put()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block(Duration.ofSeconds(5));
            
            System.out.println("Estado actualizado en ms-solicitudes: contenedor=" + idContenedor + ", estado=" + estado);
        } catch (Exception e) {
            System.err.println("Error al notificar estado a ms-solicitudes: " + e.getMessage());
        }
    }

    /**
     * Obtiene una ruta completa con todos sus tramos y detalles.
     * Convierte la entidad Ruta a RutaDTO incluyendo información detallada de cada tramo.
     */
    @Override
    @Transactional(readOnly = true)
    public RutaDTO obtenerRutaConDetalles(Long idRuta) {
        Ruta ruta = rutaRepository.findById(idRuta)
                .orElseThrow(() -> new EntityNotFoundException("Ruta no encontrada: " + idRuta));

        return convertirRutaADTO(ruta);
    }

    /**
     * Obtiene todas las rutas de una solicitud con sus tramos y detalles completos.
     * Útil para consultar las rutas alternativas de una solicitud.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RutaDTO> obtenerRutasConDetallesPorSolicitud(Long idSolicitud) {
        List<Ruta> rutas = rutaRepository.findByIdSolicitud(idSolicitud);
        List<RutaDTO> rutasDTO = new ArrayList<>();

        if (rutas != null && !rutas.isEmpty()) {
            for (Ruta ruta : rutas) {
                rutasDTO.add(convertirRutaADTO(ruta));
            }
        }

        return rutasDTO;
    }

    /**
     * Método privado que convierte una entidad Ruta a RutaDTO.
     * Incluye todos los detalles de los tramos, cálculo de tiempos y costos totales.
     */
    private RutaDTO convertirRutaADTO(Ruta ruta) {
        RutaDTO rutaDTO = new RutaDTO();
        rutaDTO.setId(ruta.getId());
        rutaDTO.setIdSolicitud(ruta.getIdSolicitud());
        rutaDTO.setCantidadTramos(ruta.getCantidadTramos());
        rutaDTO.setCantidadDepositos(ruta.getCantidadDepositos());
        rutaDTO.setAsignada(ruta.getAsignada());

        BigDecimal costoTotalEstimado = BigDecimal.ZERO;
        Double tiempoTotalEstimado = 0.0;
        List<RutaDTO.TramoDetalleDTO> tramosDTO = new ArrayList<>();

        if (ruta.getTramos() != null && !ruta.getTramos().isEmpty()) {
            for (Tramo tramo : ruta.getTramos()) {
                RutaDTO.TramoDetalleDTO tramoDTO = new RutaDTO.TramoDetalleDTO();
                
                tramoDTO.setId(tramo.getId());
                tramoDTO.setDepositoOrigen(tramo.getDepositoOrigen() != null ? tramo.getDepositoOrigen().getNombre() : "N/A");
                tramoDTO.setDepositoDestino(tramo.getDepositoDestino() != null ? tramo.getDepositoDestino().getNombre() : "N/A");
                tramoDTO.setTipoTramo(tramo.getTipoTramo() != null ? tramo.getTipoTramo().getDescripcion() : "N/A");
                tramoDTO.setEstado(tramo.getEstadoTramo() != null ? tramo.getEstadoTramo().getNombre() : "N/A");
                tramoDTO.setDistanciaKm(tramo.getDistanciaKm());
                tramoDTO.setCostoAproximado(tramo.getCostoAproximado());
                tramoDTO.setPatenteCamionAsignado(tramo.getPatenteCamionAsignado());
                tramoDTO.setCostoReal(tramo.getCostoReal());
                
                // Convertir fechas a String
                if (tramo.getFechaHoraInicio() != null) {
                    tramoDTO.setFechaHoraInicio(tramo.getFechaHoraInicio().toString());
                }
                if (tramo.getFechaHoraFin() != null) {
                    tramoDTO.setFechaHoraFin(tramo.getFechaHoraFin().toString());
                }

                // Calcular tiempo estimado para este tramo (distancia / velocidad promedio)
                // Asumimos velocidad promedio de 80 km/h
                Double tiempoHoras = 0.0;
                if (tramo.getDistanciaKm() != null && tramo.getDistanciaKm() > 0) {
                    tiempoHoras = tramo.getDistanciaKm() / 80.0; // 80 km/h velocidad promedio
                }
                tramoDTO.setTiempoEstimadoHoras(tiempoHoras);

                // Acumular costos y tiempos
                if (tramo.getCostoAproximado() != null) {
                    costoTotalEstimado = costoTotalEstimado.add(tramo.getCostoAproximado());
                }
                tiempoTotalEstimado += tiempoHoras;

                tramosDTO.add(tramoDTO);
            }
        }

        rutaDTO.setTramos(tramosDTO);
        rutaDTO.setCostoEstimadoTotal(costoTotalEstimado);
        rutaDTO.setTiempoEstimadoTotal(tiempoTotalEstimado);

        return rutaDTO;
    }
}