package com.transporte.ms_solicitudes.client;

import com.transporte.ms_solicitudes.dto.CamionDTO;
import com.transporte.ms_solicitudes.dto.PromediosDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class CamionesWebClient {

    private static final Logger logger = LoggerFactory.getLogger(CamionesWebClient.class);

    @Autowired
    private WebClient webClient;

    private static final String MS_CAMIONES_URL = "http://ms-camiones:8083/camiones";

    public PromediosDTO obtenerPromedios(Double peso, Double volumen) {
        try {
            logger.info("Llamando a ms-camiones/promedios con peso={}, volumen={}", peso, volumen);
            return webClient.get()
                    .uri(MS_CAMIONES_URL + "/promedios?peso={peso}&volumen={volumen}",
                            peso, volumen)
                    .retrieve()
                    .bodyToMono(PromediosDTO.class)
                    .block();
        } catch (Exception e) {
            logger.error("Error al obtener promedios desde ms-camiones: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudieron obtener los promedios de camiones", e);
        }
    }

    public CamionDTO getCamion(String patente) {
        try {
            logger.info("Llamando a ms-camiones/{}", patente);
            return webClient.get()
                    .uri(MS_CAMIONES_URL + "/{patente}", patente)
                    .retrieve()
                    .bodyToMono(CamionDTO.class)
                    .block();
        } catch (Exception e) {
            logger.error("Error al obtener camión {} desde ms-camiones: {}", patente, e.getMessage(), e);
            throw new RuntimeException("No se pudo obtener el camión", e);
        }
    }
}