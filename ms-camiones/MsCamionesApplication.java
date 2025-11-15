package ar.edu.utnfrc.backend.mscamiones; // ¡Importante que sea el paquete raíz!

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient // Para que se registre en Eureka
public class MsCamionesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsCamionesApplication.class, args);
    }
}