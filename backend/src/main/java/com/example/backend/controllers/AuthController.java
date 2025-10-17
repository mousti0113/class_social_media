package com.example.backend.controllers;

import com.example.backend.dtos.request.LoginRequestDTO;
import com.example.backend.dtos.request.RegistrazioneRequestDTO;
import com.example.backend.dtos.request.RefreshTokenRequestDTO;
import com.example.backend.dtos.response.LoginResponseDTO;
import com.example.backend.dtos.response.RefreshTokenResponseDTO;
import com.example.backend.models.User;
import com.example.backend.repositories.UserRepository;
import com.example.backend.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller per la gestione dell'autenticazione.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    /**
     * POST /api/auth/registrazione
     * Registra un nuovo utente
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponseDTO> register(@Valid @RequestBody RegistrazioneRequestDTO request) {
        return ResponseEntity.ok(authService.registrazione(request));
    }

    /**
     * POST /api/auth/login
     * Effettua il login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * POST /api/auth/refresh-token
     * Rinnova l'access token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    /**
     * POST /api/auth/logout
     * Effettua il logout invalidando i token
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal UserDetails userDetails) {
        // Recupera l'utente dal database
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        authService.logout(user.getId());
        return ResponseEntity.ok("Logout effettuato con successo");
    }
}
