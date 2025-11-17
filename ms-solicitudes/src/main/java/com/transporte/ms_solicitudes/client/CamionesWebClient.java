package com.transporte.ms_solicitudes.client;

import com.transporte.ms_solicitudes.dto.CamionDTO;
import com.transporte.ms_solicitudes.dto.PromediosDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.math.BigDecimal;

@Component
public class CamionesWebClient {

    @Autowired
    private WebClient webClient;

    private static final String MS_CAMIONES_URL = "http://ms-camiones:8082/camiones";

    public PromediosDTO obtenerPromedios(BigDecimal peso, BigDecimal volumen) {
        return webClient.get()
                .uri(MS_CAMIONES_URL + "/promedios?peso={peso}&volumen={volumen}",
                        peso.doubleValue(), volumen.doubleValue())
                .retrieve()
                .bodyToMono(PromediosDTO.class)
                .block();
    }

    public CamionDTO getCamion(String patente) {
        return webClient.get()
                .uri(MS_CAMIONES_URL + "/{patente}", patente)
                .retrieve()
                .bodyToMono(CamionDTO.class)
                .block();
    }
}