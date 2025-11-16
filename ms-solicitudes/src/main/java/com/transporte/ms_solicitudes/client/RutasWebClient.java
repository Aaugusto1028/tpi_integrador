package com.transporte.ms_solicitudes.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class RutasWebClient {

    private final WebClient webClient;

    @Value("${rutas.service.base-url:http://localhost:8081}")
    private String rutasBaseUrl;

    public RutasWebClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public <T> Mono<T> getRutaById(String id, Class<T> responseType) {
        return webClient
                .get()
                .uri(rutasBaseUrl + "/rutas/{id}", id)
                .retrieve()
                .bodyToMono(responseType);
    }
}
