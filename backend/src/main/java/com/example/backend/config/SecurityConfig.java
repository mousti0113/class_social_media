package com.example.backend.config;

import com.example.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configurazione di sicurezza per l'applicazione.
 * Definisce le regole di autenticazione e autorizzazione.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Abilita @PreAuthorize, @PostAuthorize, etc.
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    /**
     * Configura la security filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disabilita CSRF (non necessario per API REST stateless)
                .csrf(AbstractHttpConfigurer::disable)

                // Configura CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Configura le regole di autorizzazione
                .authorizeHttpRequests(auth -> auth
                        // Endpoint pubblici (nessuna autenticazione richiesta)
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/registrazione",
                                "/api/auth/refresh-token",
                                "/api/auth/reset-password",
                                "/api/auth/conferma-reset-password"
                        ).permitAll()

                        // WebSocket (pubblico per connessione, poi validato)
                        .requestMatchers("/ws/**").permitAll()

                        // Endpoint admin (solo utenti con ruolo ADMIN)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Tutti gli altri endpoint richiedono autenticazione
                        .anyRequest().authenticated()
                )

                // Gestione sessioni: STATELESS (nessuna sessione server-side)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Configura l'authentication provider
                .authenticationProvider(authenticationProvider())

                // Aggiunge il filtro JWT prima del filtro di autenticazione standard
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configura l'authentication provider con UserDetailsService e PasswordEncoder
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Bean per l'AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Password encoder con BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configurazione CORS per consentire richieste dal frontend
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Origini permesse (modifica con l'URL del tuo frontend Angular)
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));

        // Metodi HTTP permessi
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Headers permessi
        configuration.setAllowedHeaders(List.of("*"));

        // Permetti credenziali (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Tempo di cache per preflight requests
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}