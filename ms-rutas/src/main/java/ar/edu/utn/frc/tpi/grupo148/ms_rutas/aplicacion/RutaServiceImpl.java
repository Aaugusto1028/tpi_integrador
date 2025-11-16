package ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion;

import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.AsignarCamionRequest;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.CrearRutaRequest;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio.*;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.repositorios.*;
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

        return tramoRepository.save(tramo);
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

        return tramoRepository.save(tramo);
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

        return tramoRepository.save(tramo);
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
     */
    private CamionDTO validarCamion(String patente, Double peso, Double volumen) {
        // Asumimos que "ms-camiones" es el nombre del servicio en Docker/Kubernetes
        String url = String.format("http://ms-camiones:8083/camiones/%s", patente);

        CamionDTO camion;
        try {
            camion = webClientBuilder.build().get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(CamionDTO.class)
                    .block(Duration.ofSeconds(5)); // Timeout de 5s
        } catch (Exception e) {
            throw new RuntimeException("Error al conectar con ms-camiones: " + e.getMessage());
        }

        if (camion == null) {
            throw new EntityNotFoundException("Camión no encontrado: " + patente);
        }
        if (!camion.getDisponibilidad()) {
            throw new IllegalStateException("El camión " + patente + " no está disponible.");
        }
        if (camion.getCapacidadPeso() < peso || camion.getCapacidadVolumen() < volumen) {
            throw new IllegalStateException("El camión " + patente + " no soporta el peso/volumen del contenedor.");
        }

        return camion;
    }

    /**
     * Llama al ms-camiones para obtener los datos de un camión (sin validar).
     */
    private CamionDTO buscarCamion(String patente) {
        String url = String.format("http://ms-camiones:8083/camiones/%s", patente);
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
    private BigDecimal calcularCostoRealTramo(Tramo tramo) {
        // 1. Obtener datos del camión
        CamionDTO camion = buscarCamion(tramo.getPatenteCamionAsignado());

        // 2. Obtener tarifa de combustible
        Tarifa tarifaBase = tarifaRepository.findById(1L) // Asumimos 1L = tarifa base
                .orElseThrow(() -> new EntityNotFoundException("Tarifa base no encontrada"));

        // 3. Calcular costo por km
        BigDecimal costoKm = camion.getCostoPorKm().multiply(BigDecimal.valueOf(tramo.getDistanciaKm()));

        // 4. Calcular costo de combustible
        double consumoTotalLitros = camion.getConsumoCombustibleKm() * tramo.getDistanciaKm();
        BigDecimal costoCombustible = tarifaBase.getPrecioLitro().multiply(BigDecimal.valueOf(consumoTotalLitros));

        // 5. Calcular costo de estadía (si aplica)
        // (Esta lógica es más compleja, requiere saber cuánto tiempo estuvo en el
        // depósito)
        // Por ahora, lo dejamos en 0.
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
            String url = String.format("http://ms-solicitudes:8082/solicitudes/%d/confirmar-ruta", ruta.getIdSolicitud());
            webClientBuilder.build().post().uri(url).retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(5));
        } catch (Exception ignored) {
            // best-effort: si falla la notificación no interrumpimos el flujo
        }

        return guardada;
    }
}