package com.transporte.ms_solicitudes.client;

import com.transporte.ms_solicitudes.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class RutasWebClient {

    @Autowired
    private WebClient webClient;

    private static final String MS_RUTAS_URL = "http://ms-rutas:8081/rutas";

    public DistanciaResponse obtenerDistancia(double lat1, double lon1, double lat2, double lon2) {
        CoordenadasRequest request = new CoordenadasRequest(
                new java.math.BigDecimal(lat1),
                new java.math.BigDecimal(lon1),
                new java.math.BigDecimal(lat2),
                new java.math.BigDecimal(lon2)
        );
        return webClient.post()
                .uri(MS_RUTAS_URL + "/distancia")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DistanciaResponse.class)
                .block();
    }

    public TarifaDTO obtenerTarifas() {
        return webClient.get()
                .uri(MS_RUTAS_URL + "/tarifas")
                .retrieve()
                .bodyToMono(TarifaDTO.class)
                .block();
    }

    public String obtenerUbicacionActual(Long idContenedor) {
        return webClient.get()
                .uri(MS_RUTAS_URL + "/contenedores/{id}/ubicacion", idContenedor)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}