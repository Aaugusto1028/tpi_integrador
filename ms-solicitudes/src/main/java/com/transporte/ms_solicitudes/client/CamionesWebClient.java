package com.transporte.ms_solicitudes.client;

import com.transporte.ms_solicitudes.dto.CamionDTO;
import com.transporte.ms_solicitudes.dto.PromediosDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CamionesWebClient {

    @Autowired
    private WebClient webClient; // Este bean se crea en WebClientConfig

    /**
     * Llama a ms-camiones/camiones/aptos
     * (Basado en ms-camiones/controllers/CamionController.java)
     */
    public Mono<List<CamionDTO>> obtenerCamionesAptos(BigDecimal peso, BigDecimal volumen) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("http://ms-camiones/camiones/aptos") // Usa el nombre del servicio Docker
                        .queryParam("peso", peso)
                        .queryParam("volumen", volumen)
                        .build())
                .retrieve()
                .bodyToFlux(CamionDTO.class) // Esperamos una lista, así que usamos Flux
                .collectList(); // Y la convertimos a Mono<List<...>>
    }

    /**
     * Llama a ms-camiones/camiones/promedios
     * (Basado en ms-camiones/controllers/CamionController.java)
     */
    public Mono<PromediosDTO> obtenerPromediosCostos(List<String> dominios) {
        // Enviar una lista como query param puede ser complejo.
        // Asegúrate que tu CamionController en ms-camiones pueda recibir "dominios"
        // como una lista de strings (ej. ?dominios=AA123BB,CC456DD)
        // Spring lo maneja automáticamente si el @RequestParam es List<String>.
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("http://ms-camiones/camiones/promedios")
                        .queryParam("dominios", String.join(",", dominios)) // Enviamos como "dominio1,dominio2"
                        .build())
                .retrieve()
                .bodyToMono(PromediosDTO.class); // Esperamos un solo objeto DTO
    }
}