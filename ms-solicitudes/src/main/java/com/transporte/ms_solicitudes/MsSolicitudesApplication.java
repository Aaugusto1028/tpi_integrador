package com.transporte.ms_solicitudes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
// @EnableDiscoveryClient  // <-- CAMBIO: Comentado porque no usamos Eureka
public class MsSolicitudesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsSolicitudesApplication.class, args);//Esto hace que se inicie la aplicacion,  es decir, que se levante el servidor
    }
}