package com.transporte.msgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.core.convert.converter.Converter;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Collections; // Asegúrate de importar Collections

@Configuration
@EnableWebFluxSecurity 
public class GatewayConfig {

    // --- 1. Configuración de Ruteo (RouteLocator) ---
    // (Esta parte queda igual, está perfecta)
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            
            .route("ms-solicitudes-api", r -> r.path("/api/solicitudes/**", "/api/clientes/**")
                .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/${segment}"))
                .uri("lb://ms-solicitudes:8081")) 
            
            .route("ms-rutas-api", r -> r.path("/api/rutas/**", "/api/tramos/**", "/api/depositos/**")
                .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/${segment}"))
                .uri("lb://ms-rutas:8082")) 
            
            .route("ms-camiones-api", r -> r.path("/api/camiones/**")
                .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/${segment}"))
                .uri("lb://ms-camiones:8083")) 

            .build();
    }
    
    // --- 2. Configuración de Seguridad (SecurityWebFilterChain) ---
    // (Esta parte queda igual)
    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwtConfigurer -> jwtConfigurer
                    .jwtAuthenticationConverter(new JwtReactiveAuthenticationConverter())
                )
            );
        return http.build();
    }
    
    // --- 3. Extractor de Roles de Keycloak para WebFlux ---

    private static class JwtReactiveAuthenticationConverter implements Converter<Jwt, Mono<? extends org.springframework.security.authentication.AbstractAuthenticationToken>> {
        
        @Override
        public Mono<? extends org.springframework.security.authentication.AbstractAuthenticationToken> convert(Jwt jwt) {
            Map<String, Collection<String>> realmAccess = jwt.getClaim("realm_access");
            
            // ARREGLO: Chequeo de nulos
            if (realmAccess == null) {
                return Mono.just(new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt, Collections.emptyList()));
            }
            Collection<String> roles = realmAccess.get("roles");
            if (roles == null) {
                return Mono.just(new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt, Collections.emptyList()));
            }
            
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            for (String role : roles) {
                // ARREGLO CLAVE: Convertir a mayúsculas
                authorities.add(new SimpleGrantedAuthority(role.toUpperCase()));
            }
            
            var token = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt, authorities);
            return Mono.just(token);
        }
    }
}