package com.example.backend.config;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.backend.models.User;
import com.example.backend.repositories.UserRepository;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository) {
        return args -> {
            // Crea admin se non esiste
            if (!userRepository.existsByUsername("admin")) {
                User admin = User.builder()
                    .username("admin")
                    .email("admin@classe.it")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .fullName("Amministratore")
                    .isAdmin(true)
                    .isActive(true)
                    .build();
                    
                userRepository.save(admin);
                log.info("Admin user created with username: admin");
            }
            
            // Opzionale: crea alcuni utenti di test
            if (userRepository.count() == 1) { // Solo admin esiste
                for (int i = 1; i <= 4; i++) {
                    User student = User.builder()
                        .username("studente" + i)
                        .email("studente" + i + "@classe.it")
                        .passwordHash(passwordEncoder.encode("password123"))
                        .fullName("Studente " + i)
                        .isAdmin(false)
                        .isActive(true)
                        .build();
                    
                    userRepository.save(student);
                }
                log.info("Test students created");
            }
        };
    }
}
 