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

@Configuration
@EnableWebFluxSecurity // Necesario para seguridad en Spring WebFlux (usado por Gateway)
public class GatewayConfig {

    // --- 1. Configuración de Ruteo (RouteLocator) ---

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            
            // Rutas para ms-solicitudes (Puerto 8081)
            .route("ms-solicitudes-api", r -> r.path("/api/solicitudes/**", "/api/clientes/**")
                .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/${segment}"))
                .uri("lb://ms-solicitudes:8081")) // Usamos lb:// o la URL directa: http://ms-solicitudes:8081
            
            // Rutas para ms-rutas (Puerto 8082)
            .route("ms-rutas-api", r -> r.path("/api/rutas/**", "/api/tramos/**", "/api/depositos/**")
                .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/${segment}"))
                .uri("lb://ms-rutas:8082")) // Usamos lb:// o la URL directa: http://ms-rutas:8082
            
            // Rutas para ms-camiones (Puerto 8083)
            .route("ms-camiones-api", r -> r.path("/api/camiones/**")
                .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/${segment}"))
                .uri("lb://ms-camiones:8083")) // Usamos lb:// o la URL directa: http://ms-camiones:8083

            .build();
    }
    
    // --- 2. Configuración de Seguridad (SecurityWebFilterChain) ---

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                // Endpoints públicos (Swagger, API Docs, Health)
                .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Cualquier otra petición debe estar autenticada
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
            Collection<String> roles = realmAccess != null ? realmAccess.get("roles") : new ArrayList<>();
            
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if (roles != null) {
                for (String role : roles) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                }
            }
            
            var token = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt, authorities);
            return Mono.just(token);
        }
    }
}
