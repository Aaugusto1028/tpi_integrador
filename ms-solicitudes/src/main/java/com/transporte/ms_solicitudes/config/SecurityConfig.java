package com.transporte.ms_solicitudes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer; // <-- IMPORTADO
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // <-- IMPORTADO
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // <-- CAMBIO 1: ¡¡HABILITADO!! Para que @PreAuthorize funcione
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                //.anyRequest().permitAll() // <-- CAMBIO 2: Eliminado
                .anyRequest().authenticated() // <-- CAMBIO 3: Requerimos autenticación
            )
            // CAMBIO 4: Habilita la validación de tokens JWT de Keycloak
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults())) 
            .csrf(csrf -> csrf.disable()); // <-- Necesario para POST/PUT
        
        return http.build();
    }
}