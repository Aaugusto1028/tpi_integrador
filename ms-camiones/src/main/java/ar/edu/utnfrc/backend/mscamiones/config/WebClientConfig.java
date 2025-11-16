package ar.edu.utnfrc.backend.mscamiones.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    // URL base del microservicio de Rutas y Tramos
    // Usamos el nombre del servicio de Docker Compose (ms-rutas) y su puerto interno (8082)
    private static final String MS_RUTAS_BASE_URL = "http://ms-rutas:8082"; 

    @Bean
    public WebClient webClientRutas() {
        return WebClient.builder()
                .baseUrl(MS_RUTAS_BASE_URL)
                .build();
    }
}
