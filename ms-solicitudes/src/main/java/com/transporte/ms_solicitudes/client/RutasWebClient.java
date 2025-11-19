package com.transporte.ms_solicitudes.client;

import com.transporte.ms_solicitudes.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class RutasWebClient {

    private static final Logger logger = LoggerFactory.getLogger(RutasWebClient.class);

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
        try {
            logger.info("Llamando a ms-rutas/distancia con coordenadas: ({}, {}) -> ({}, {})", lat1, lon1, lat2, lon2);
            return webClient.post()
                    .uri(MS_RUTAS_URL + "/distancia")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(DistanciaDTO.class)
                    .block();
        } catch (Exception e) {
            logger.error("Error al obtener distancia desde ms-rutas: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo obtener la distancia", e);
        }
    }

    public TarifaDTO obtenerTarifas() {
        try {
            logger.info("Llamando a ms-rutas/tarifas");
            return webClient.get()
                    .uri(MS_RUTAS_URL + "/tarifas")
                    .retrieve()
                    .bodyToMono(TarifaDTO.class)
                    .block();
        } catch (Exception e) {
            logger.error("Error al obtener tarifas desde ms-rutas: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo obtener las tarifas", e);
        }
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

    /**
     * Obtiene el tiempo real (en HORAS) que tomó completar una solicitud.
     * Se calcula basándose en las fechas de inicio y fin de los tramos.
     * @param idSolicitud ID de la solicitud
     * @return Double con el tiempo real en horas
     */
    public Double obtenerTiempoRealPorSolicitud(Long idSolicitud) {
        try {
            logger.info("Llamando a ms-rutas/tiempo-real/{} para obtener tiempo real", idSolicitud);
            java.util.Map<String, Double> respuesta = webClient.get()
                    .uri(MS_RUTAS_URL + "/tiempo-real/{idSolicitud}", idSolicitud)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<java.util.Map<String, Double>>() {})
                    .block();
            
            if (respuesta != null && respuesta.containsKey("tiempoRealHoras")) {
                Double tiempoReal = respuesta.get("tiempoRealHoras");
                logger.info("Tiempo real obtenido para solicitud {}: {} horas", idSolicitud, tiempoReal);
                return tiempoReal;
            }
            logger.warn("No se obtuvo tiempoRealHoras en la respuesta de ms-rutas para solicitud {}", idSolicitud);
            return 0.0;
        } catch (Exception e) {
            logger.error("Error al obtener tiempo real desde ms-rutas para solicitud {}: {}", idSolicitud, e.getMessage(), e);
            return 0.0;
        }
    }
}