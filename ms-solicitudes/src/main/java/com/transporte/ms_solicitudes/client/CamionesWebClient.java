package com.transporte.ms_solicitudes.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CamionesWebClient {

    private final WebClient webClient;

    @Value("${camiones.service.base-url:http://localhost:8082}")
    private String camionesBaseUrl;

    public CamionesWebClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public <T> Mono<T> getCamionById(String id, Class<T> responseType) {
        return webClient
                .get()
                .uri(camionesBaseUrl + "/camiones/{id}", id)
                .retrieve()
                .bodyToMono(responseType);
    }
}
