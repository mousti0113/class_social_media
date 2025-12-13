package com.example.backend.config;

import com.example.backend.models.User;
import com.example.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            log.info("Inizializzazione dati: creazione utenti di test");

            // Crea 4 utenti normali
            for (int i = 1; i <= 4; i++) {
                User user = User.builder()
                        .username("user" + i)
                        .email("user" + i + "@example.com")
                        .passwordHash(passwordEncoder.encode("Password"+i))
                        .fullName("User " + i)
                        .bio("Bio di User " + i)
                        .isAdmin(false)
                        .isActive(true)
                        .build();
                userRepository.save(user);
                log.info("Creato utente: {}", user.getUsername());
            }

            // Crea 1 utente admin
            User admin = User.builder()
                    .username("admin")
                    .email("admin@example.com")
                    .passwordHash(passwordEncoder.encode("Admin123"))
                    .fullName("Administrator")
                    .bio("Amministratore del sistema")
                    .isAdmin(true)
                    .isActive(true)
                    .build();
            userRepository.save(admin);
            log.info("Creato utente admin: {}", admin.getUsername());

            log.info("Inizializzazione dati completata");
        } else {
            log.info("Utenti già presenti nel database, skip inizializzazione");
        }
    }
}