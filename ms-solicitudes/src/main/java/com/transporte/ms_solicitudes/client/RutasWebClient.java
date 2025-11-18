package com.transporte.ms_solicitudes.client;

import com.transporte.ms_solicitudes.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class RutasWebClient {

    @Autowired
    private WebClient webClient;

    private static final String MS_RUTAS_URL = "http://ms-rutas:8082/rutas";

    public DistanciaDTO obtenerDistancia(double lat1, double lon1, double lat2, double lon2) {
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
                .bodyToMono(DistanciaDTO.class)
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

    /**
     * Obtiene el costo real desglosado de un traslado por idSolicitud.
     * @param idSolicitud ID de la solicitud
     * @return CostoTrasladoDTO con desglose (costo por km, combustible, estadía, total)
     */
    public CostoTrasladoDTO obtenerCostoTrasladoRealPorSolicitud(Long idSolicitud) {
        return webClient.get()
                .uri(MS_RUTAS_URL + "/solicitud/{idSolicitud}/costo-real", idSolicitud)
                .retrieve()
                .bodyToMono(CostoTrasladoDTO.class)
                .block();
    }

    /**
     * Obtiene el costo real desglosado de un traslado por idRuta.
     * @param idRuta ID de la ruta
     * @return CostoTrasladoDTO con desglose de costos
     */
    public CostoTrasladoDTO obtenerCostoTrasladoRealPorRuta(Long idRuta) {
        return webClient.get()
                .uri(MS_RUTAS_URL + "/ruta/{idRuta}/costo-real", idRuta)
                .retrieve()
                .bodyToMono(CostoTrasladoDTO.class)
                .block();
    }
}