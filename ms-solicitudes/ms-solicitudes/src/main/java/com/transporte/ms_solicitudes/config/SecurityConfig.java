package com.transporte.ms_solicitudes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // <-- Esto habilita la seguridad por método (ej. @PreAuthorize)
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated() // Requiere autenticación para TODOS los endpoints
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {}) // Configura el servicio para aceptar tokens JWT de Keycloak
            );
        return http.build();
    }
}