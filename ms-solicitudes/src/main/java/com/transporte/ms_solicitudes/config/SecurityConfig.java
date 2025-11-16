package com.transporte.ms_solicitudes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// IMPORTANTE: NO importes EnableMethodSecurity por ahora
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
// @EnableMethodSecurity // <-- IMPORTANTE: Comentá o borrá esta línea (así ignora los @PreAuthorize)
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll() // <-- CAMBIO 1: Permite TODAS las peticiones
            )
            .csrf(csrf -> csrf.disable()); // <-- CAMBIO 2: Deshabilita CSRF (necesario para POST/PUT)
        
        // NO configuramos .oauth2ResourceServer(), así Spring no busca Keycloak
        
        return http.build();
    }
}