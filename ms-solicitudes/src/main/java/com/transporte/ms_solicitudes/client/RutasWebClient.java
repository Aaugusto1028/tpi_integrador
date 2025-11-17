package com.transporte.ms_solicitudes.client;

import com.transporte.ms_solicitudes.dto.CoordenadasRequest;
import com.transporte.ms_solicitudes.dto.DistanciaResponse;
import com.transporte.ms_solicitudes.dto.SeguimientoDTO;
import com.transporte.ms_solicitudes.dto.RutaFinalizadaDTO;
import com.transporte.ms_solicitudes.dto.TarifaDTO; // Asumimos que existe este DTO en ms-rutas
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class RutasWebClient {

    @Autowired
    private WebClient webClient;

    /**
     * Llama a ms-rutas/rutas/distancia
     * (Basado en ms-rutas/infraestructura/controladores/RutaController.java)
     */
    public Mono<DistanciaResponse> obtenerDistanciaEstimada(CoordenadasRequest request) {
        return webClient.post()
                .uri("http://ms-rutas/rutas/distancia")
                .bodyValue(request) // Enviamos el DTO en el body
                .retrieve()
                .bodyToMono(DistanciaResponse.class);
    }
    
    /**
     * Llama a ms-rutas/tarifas/vigente (Endpoint Hipotético)
     * Necesitamos el precio del combustible (Regla 64)
     */
    public Mono<TarifaDTO> obtenerTarifaVigente() {
        // Debes crear este endpoint en ms-rutas que devuelva las tarifas actuales,
        // incluyendo el precio_litro de la tabla TARIFAS.
        return webClient.get()
                .uri("http://ms-rutas/tarifas/vigente") // Endpoint de ejemplo
                .retrieve()
                .bodyToMono(TarifaDTO.class);
    }


    // Métodos que implementaremos después (basados en tu RutaController):
    
    public Mono<SeguimientoDTO> obtenerUbicacionActual(Long solicitudId) {
         return webClient.get()
                .uri("http://ms-rutas/rutas/" + solicitudId + "/ubicacion")
                .retrieve()
                .bodyToMono(SeguimientoDTO.class);
    }

    public Mono<RutaFinalizadaDTO> obtenerRutaFinalizada(Long solicitudId) {
        return webClient.get()
                .uri("http://ms-rutas/rutas/" + solicitudId + "/finalizada")
                .retrieve()
                .bodyToMono(RutaFinalizadaDTO.class);
    }
}