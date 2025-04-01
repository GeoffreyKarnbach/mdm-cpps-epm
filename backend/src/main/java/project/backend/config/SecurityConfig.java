package project.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Apply custom CORS configuration
            .authorizeRequests(authorizeRequests ->
                authorizeRequests
                    .requestMatchers("api/v1/project/{projectId}/export").permitAll() // Permit access to /api/v1/project/**/export
                    .requestMatchers("api/v1/project/{projectId}/export/anonymized").permitAll() // Permit access to /api/v1/project/**/export/anonymized
                    .requestMatchers("/api/v1/auth/**").permitAll() // Permit access to /api/v1/auth/**
                    .requestMatchers("v3/api-docs/**").permitAll() // Permit access to /v3/api-docs/swagger-config
                    .requestMatchers("swagger-ui/**").permitAll() // Permit access to /swagger-ui/**
                    .anyRequest().authenticated() // Requires authentication for all other requests
            )
            .addFilterBefore(new JWTAuthorizationFilter(authenticationManager), UsernamePasswordAuthenticationFilter.class); // Add JWTAuthorizationFilter before UsernamePasswordAuthenticationFilter

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        //configuration.addAllowedOrigin("http://localhost:4200");
        configuration.addAllowedOrigin("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}