package com.example.backend.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entità per tracciare tutte le azioni amministrative.
 * Utilizzato per audit e sicurezza.
 */
@Entity
@Table(name = "admin_audit_logs",
        indexes = {
                @Index(name = "idx_admin_id", columnList = "admin_id"),
                @Index(name = "idx_azione", columnList = "azione"),
                @Index(name = "idx_created_at", columnList = "created_at")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = {"admin", "targetUser"})
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AzioneAdmin azione;

    @Column(columnDefinition = "TEXT")
    private String descrizione;

    // ID della risorsa coinvolta (post_id, user_id, etc.)
    @Column(name = "target_type", length = 50)
    private String targetType; // "POST", "USER", "COMMENT", etc.

    @Column(name = "target_id")
    private Long targetId;

    // Riferimento diretto all'utente target (se applicabile)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
