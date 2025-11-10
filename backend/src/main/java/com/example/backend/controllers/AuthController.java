package com.example.backend.controllers;

import com.example.backend.config.CurrentUser;
import com.example.backend.dtos.request.LoginRequestDTO;
import com.example.backend.dtos.request.PasswordResetConfirmDTO;
import com.example.backend.dtos.request.PasswordResetRequestDTO;
import com.example.backend.dtos.request.RegistrazioneRequestDTO;
import com.example.backend.dtos.request.RefreshTokenRequestDTO;
import com.example.backend.dtos.response.LoginResponseDTO;
import com.example.backend.dtos.response.RefreshTokenResponseDTO;
import com.example.backend.models.User;
import com.example.backend.services.AuthService;
import com.example.backend.services.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller per la gestione dell'autenticazione.
 * Tutti i metodi delegano la logica al AuthService e gestiscono solo HTTP.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    /**
     * POST /api/auth/registrazione
     * Registra un nuovo utente
     *
     * @param request DTO con username, email, password e nome completo
     * @return LoginResponseDTO con access token, refresh token e dati utente
     * @throws ResourceAlreadyExistsException se username o email già esistono
     * @throws LimitExceededException se si supera il limite di 17 studenti
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponseDTO> registrazione(@Valid @RequestBody RegistrazioneRequestDTO request) {
        log.debug("POST /api/auth/register - Username: {}", request.getUsername());

        LoginResponseDTO response = authService.registrazione(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * POST /api/auth/login
     * Effettua il login
     *
     * @param request DTO con username e password
     * @return LoginResponseDTO con access token, refresh token e dati utente
     * @throws InvalidCredentialsException se username o password non sono corretti
     * @throws ResourceNotFoundException se l'utente non esiste
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        log.debug("POST /api/auth/login - Username: {}", request.getUsername());

        LoginResponseDTO response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/refresh-token
     * Rinnova l'access token usando il refresh token
     *
     * @param request DTO con refresh token
     * @return RefreshTokenResponseDTO con nuovo access token e refresh token
     * @throws InvalidTokenException se il refresh token non è valido o è scaduto
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request) {
        log.debug("POST /api/auth/refresh-token");

        RefreshTokenResponseDTO response = authService.refreshToken(request);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/logout
     * Effettua il logout invalidando tutti i refresh token dell'utente
     *
     *  Richiede autenticazione (access token nell'header Authorization)
     *
     * @param user Utente autenticato (iniettato automaticamente)
     * @return Messaggio di conferma
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@CurrentUser User user) {
        log.debug("POST /api/auth/logout - Username: {}", user.getUsername());

        // Effettua il logout
        authService.logout(user.getId());

        return ResponseEntity.ok("Logout effettuato con successo");
    }

    /**
     * POST /api/auth/reset-password
     * Richiede il reset della password.
     * <p>
     * Endpoint pubblico (non richiede autenticazione).
     * L'utente fornisce la propria email e riceve un link per resettare la password.
     * <p>
     * Per sicurezza, restituisce sempre un messaggio di successo anche se
     * l'email non esiste nel sistema (previene user enumeration).
     *
     * @param request DTO contenente l'email dell'utente
     * @return Messaggio di conferma
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDTO request) {
        log.debug("POST /api/auth/reset-password - Email: {}", request.getEmail());

        passwordResetService.requestPasswordReset(request.getEmail());

        // Messaggio generico per sicurezza (non rivela se l'email esiste)
        return ResponseEntity.ok(
                "Se l'email fornita è registrata, riceverai un link per resettare la password.");
    }

    /**
     * POST /api/auth/confirm-reset-password
     * Conferma il reset della password con token e nuova password.
     * <p>
     * Endpoint pubblico (non richiede autenticazione).
     * L'utente fornisce il token ricevuto via email e la nuova password.
     * <p>
     * Il token deve essere:
     * - Valido (esistente nel database)
     * - Non scaduto (max 1 ora dalla generazione)
     * - Non ancora utilizzato
     *
     * @param request DTO contenente token e nuova password
     * @return Messaggio di conferma
     * @throws InvalidInputException se il token è invalido o la password non valida
     */
    @PostMapping("/confirm-reset-password")
    public ResponseEntity<String> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmDTO request) {
        log.debug("POST /api/auth/confirm-reset-password - Token presente");

        passwordResetService.confirmPasswordReset(request.getToken(), request.getNewPassword());

        return ResponseEntity.ok("Password resettata con successo. Ora puoi effettuare il login.");
    }

    /**
     * GET /api/auth/validate-reset-token?token={token}
     * Verifica se un token di reset password è valido.
     * <p>
     * Endpoint pubblico (non richiede autenticazione).
     * Utilizzato dal frontend per verificare se il token nell'URL
     * è ancora valido prima di mostrare il form di reset.
     * <p>
     * Se il token è scaduto o invalido, il frontend può mostrare
     * un messaggio appropriato senza far compilare il form all'utente.
     *
     * @param token Il token da verificare
     * @return Oggetto JSON con campo "valid": true/false
     */
    @GetMapping("/validate-reset-token")
    public ResponseEntity<java.util.Map<String, Boolean>> validateResetToken(@RequestParam String token) {
        log.debug("GET /api/auth/validate-reset-token");

        boolean isValid = passwordResetService.isTokenValid(token);

        return ResponseEntity.ok(java.util.Map.of("valid", isValid));
    }
}