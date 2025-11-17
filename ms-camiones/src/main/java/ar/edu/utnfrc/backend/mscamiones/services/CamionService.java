package ar.edu.utnfrc.backend.mscamiones.services;

import ar.edu.utnfrc.backend.mscamiones.dtos.TramoDTO; // Nuevo: Importar el DTO creado
import ar.edu.utnfrc.backend.mscamiones.dtos.PromediosDTO;
import ar.edu.utnfrc.backend.mscamiones.models.Camion;
import ar.edu.utnfrc.backend.mscamiones.repositories.CamionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier; // Nuevo: Para inyección
import org.springframework.core.ParameterizedTypeReference; // Nuevo: Para el WebClient
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient; // Nuevo: Para WebClient
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CamionService {

    private final CamionRepository repository;
    private final WebClient webClientRutas; // Nuevo: Atributo para el cliente HTTP

    // Constructor con Inyección de Dependencias (Recomendado sobre @Autowired en atributos)
    // Se usa @Qualifier para inyectar el WebClient configurado en WebClientConfig
    @Autowired
    public CamionService(
            CamionRepository repository, 
            @Qualifier("webClientRutas") WebClient webClientRutas) {
        this.repository = repository;
        this.webClientRutas = webClientRutas;
    }

    // --- Métodos CRUD Básicos ---

    // 2. Crear/Actualizar
    public Camion save(Camion camion) {
        // En un caso real, aquí iría la validación de capacidad (Regla de negocio 11)
        return repository.save(camion);
    }

    // 3. Leer todos (con filtro de disponibilidad)
    public List<Camion> findAll(Optional<Boolean> disponibilidad) {
        if (disponibilidad.isPresent()) {
            return repository.findByDisponibilidad(disponibilidad.get());
        }
        return repository.findAll();
    }

    // 4. Leer por Patente (PK)
    public Optional<Camion> findById(String patente) {
        return repository.findById(patente);
    }

    // 5. Eliminar por Patente
    public void deleteById(String patente) {
        repository.deleteById(patente);
    }

    // --- PENDIENTE 1: Lógica de Negocio Específica (Camiones aptos) ---
    /*
    * Método clave que el microservicio de Solicitudes/Rutas necesitará consumir.
    * Busca camiones disponibles que cumplen con la capacidad de peso y volumen requeridos.
    */
    public List<Camion> findAptos(Double pesoRequerido, Double volumenRequerido) {
        List<Camion> disponibles = repository.findByDisponibilidad(true);
        
        return disponibles.stream()
                // Asegura que la capacidad de peso y volumen sea suficiente
                .filter(c -> c.getCapacidadPeso() != null && c.getCapacidadPeso() >= pesoRequerido)
                .filter(c -> c.getCapacidadVolumen() != null && c.getCapacidadVolumen() >= volumenRequerido)
                // Usamos toList() para compatibilidad con Java 16+, si usas Java 8, usa .collect(Collectors.toList());
                .collect(Collectors.toList());
    }
    
    // --- PENDIENTE 2: Lógica de Negocio para Transportista (Consumo de ms-rutas) ---
    /**
     * Obtiene los tramos asignados al transportista desde el Microservicio de Rutas.
     * @param patenteCamionAsignado La patente del camión asociado al transportista.
     * @param jwtToken El token de seguridad JWT para pasar al ms-rutas.
     * @return Una lista de DTOs que representan los tramos.
     */
    public List<TramoDTO> getTramosPorTransportista(String patenteCamionAsignado, String jwtToken) {
        
        // El método hace la llamada HTTP con WebClient al ms-rutas
        // Se espera que ms-rutas tenga el endpoint: GET /tramos?patenteCamion=...
        return webClientRutas.get()
                .uri(uriBuilder -> uriBuilder.path("/tramos")
                        .queryParam("patenteCamion", patenteCamionAsignado)
                        .build())
                // Añade el token de seguridad para la autenticación en ms-rutas
                .header("Authorization", "Bearer " + jwtToken) 
                
                // Realiza la petición y deserializa la respuesta a una lista de TramoDTO
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<TramoDTO>>() {})
                .block(); // Bloquea la ejecución hasta obtener el resultado (API síncrona)
    }

            /**
             * Calcula y devuelve los promedios (costo y consumo por km) entre los camiones
             * disponibles que cumplen con la capacidad mínima requerida.
             * @param pesoRequerido peso mínimo requerido
             * @param volumenRequerido volumen mínimo requerido
             * @return PromediosDTO con los valores (BigDecimal). Si no hay camiones aptos, devuelve ceros.
             */
            public PromediosDTO obtenerPromedios(Double pesoRequerido, Double volumenRequerido) {
            List<Camion> disponibles = repository.findByDisponibilidad(true);

            List<Camion> aptos = disponibles.stream()
                .filter(c -> c.getCapacidadPeso() != null && c.getCapacidadPeso() >= pesoRequerido)
                .filter(c -> c.getCapacidadVolumen() != null && c.getCapacidadVolumen() >= volumenRequerido)
                .collect(Collectors.toList());

            if (aptos.isEmpty()) {
                return new PromediosDTO(java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO);
            }

            java.math.BigDecimal sumaCosto = aptos.stream()
                .map(Camion::getCostoPorKm)
                .filter(java.util.Objects::nonNull)
                .map(d -> java.math.BigDecimal.valueOf(d))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            java.math.BigDecimal sumaConsumo = aptos.stream()
                .map(Camion::getConsumoCombustibleKm)
                .filter(java.util.Objects::nonNull)
                .map(d -> java.math.BigDecimal.valueOf(d))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            java.math.BigDecimal count = java.math.BigDecimal.valueOf(aptos.size());

            java.math.BigDecimal promedioCosto = sumaCosto.divide(count, 6, java.math.RoundingMode.HALF_UP);
            java.math.BigDecimal promedioConsumo = sumaConsumo.divide(count, 6, java.math.RoundingMode.HALF_UP);

            return new PromediosDTO(promedioCosto, promedioConsumo);
            }
}