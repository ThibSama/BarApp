package com.lebarapp.config;

import com.lebarapp.security.ActiveUserJwtAuthenticationConverter;
import com.lebarapp.security.JsonAccessDeniedHandler;
import com.lebarapp.security.JsonAuthenticationEntryPoint;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Stateless Spring Security configuration: stateless JWT resource server,
 * BCrypt password encoder, CORS with exact configured origins, and JSON error
 * handlers for authentication and authorization failures. Public client APIs
 * (menu, orders) remain unauthenticated; all {@code /api/bar/**} routes require
 * {@code ROLE_BARMAKER}.
 *
 * <p>The {@link ActiveUserJwtAuthenticationConverter} is wired into the OAuth2
 * resource server so that every authenticated request reloads the user from
 * PostgreSQL and verifies {@code active = true}. A disabled or deleted user's
 * token is immediately rejected, even before token expiration.</p>
 */
@Configuration
@ConfigurationPropertiesScan
public class SecurityConfig {

    private final SecurityProperties properties;

    public SecurityConfig(SecurityProperties properties) {
        this.properties = properties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JsonAuthenticationEntryPoint authEntryPoint,
                                                   JsonAccessDeniedHandler accessDeniedHandler,
                                                   JwtDecoder jwtDecoder,
                                                   ActiveUserJwtAuthenticationConverter jwtAuthConverter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // --- Public routes (anonymous clients) ---
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/menu").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/orders").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/orders/{orderId}").permitAll()
                        // --- Authenticated routes ---
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        // --- Protected barmaker namespace ---
                        .requestMatchers("/api/bar/**").hasRole("BARMAKER")
                        // --- Secure default ---
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthConverter))
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * CORS: exact configured origins only (no wildcard), {@code allowCredentials=false},
     * limited methods and headers, {@code Location} exposed.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.corsAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Location"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
