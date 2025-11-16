package ar.edu.utnfrc.backend.mscamiones.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
// REMOVER ESTA IMPORTACIÓN: import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitar CSRF para APIs sin estado (Stateless)
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configurar la autorización de las peticiones HTTP
            .authorizeHttpRequests(authorize -> authorize
                // Endpoints públicos (ej: Swagger)
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                
                // Endpoints específicos para Transportistas
                .requestMatchers("/camiones/transportistas/me/tramos").hasRole("Transportista") //
                
                // Endpoints para Operadores
                .requestMatchers("/camiones/**").hasRole("Operador")
                
                // Cualquier otra petición debe ser autenticada
                .anyRequest().authenticated()
            )
            
            // Configurar el Resource Server (OAuth2 JWT)
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwtConfigurer -> jwtConfigurer
                    .jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    // Configurador para extraer los roles de Keycloak (claim: realm_access.roles)
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        
        // Define cómo extraer las autoridades (roles) del JWT
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Collection<String>> realmAccess = jwt.getClaim("realm_access");
            Collection<String> roles = realmAccess.get("roles");
            
            // Mapea los roles de Keycloak a las autoridades de Spring Security (ej: ROLE_Operador)
            return roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                        .collect(Collectors.toList());
        });
        return jwtConverter;
    }
}