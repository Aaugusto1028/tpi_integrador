package ar.edu.utn.frc.tpi.grupo148.ms_rutas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient // <-- ¡Agregá esta línea!
public class MsRutasApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsRutasApplication.class, args);
	}

}