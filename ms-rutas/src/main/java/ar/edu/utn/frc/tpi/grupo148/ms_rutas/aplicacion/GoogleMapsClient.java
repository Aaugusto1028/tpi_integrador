package ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class GoogleMapsClient {

    private final WebClient webClient;

    // Inyecta la API Key desde tu application.properties
    @Value("${google.maps.api-key}")
    private String apiKey;

    // Inyectamos el WebClient.Builder (lo configurarás en WebClientConfig)
    public GoogleMapsClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://maps.googleapis.com/maps/api/directions")
                .build();
    }

    /**
     * Obtiene la distancia en metros entre dos coordenadas.
     *
     * @param origenLatLong  "latitud,longitud"
     * @param destinoLatLong "latitud,longitud"
     * @return Mono<Long> con la distancia en metros
     */
    public Mono<Long> getDistanciaEnMetros(String origenLatLong, String destinoLatLong) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/json")
                        .queryParam("origin", origenLatLong)
                        .queryParam("destination", destinoLatLong)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve() // Ejecuta la llamada
                .bodyToMono(JsonNode.class) // Convierte la respuesta a JSON
                .map(response -> response
                        // Navega el JSON de Google para encontrar el valor
                        .path("routes").path(0)
                        .path("legs").path(0)
                        .path("distance").path("value")
                        .asLong())
                .onErrorResume(e -> {
                    // Manejo básico de error si la API falla
                    System.err.println("Error al llamar a Google Maps: " + e.getMessage());
                    return Mono.just(0L); // Devuelve 0 en caso de error
                });
    }
}