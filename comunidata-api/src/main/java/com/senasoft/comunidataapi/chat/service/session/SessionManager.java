package com.senasoft.comunidataapi.chat.service.session;

import com.senasoft.comunidataapi.chat.enums.SessionState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SessionManager {
    private static final int MAX_FAILED_ATTEMPTS = 2;
    private static final int BLOCK_DURATION_HOURS = 1;

    // Almacenar sesiones por identificador (número de teléfono o CallSid)
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @Data
    @Builder
    @AllArgsConstructor
    public static class Session {
        private String identifier;
        private SessionState state;
        private String email;
        private String token; // JWT token
        private Integer failedAttempts;
        private LocalDateTime createdAt;
        private LocalDateTime authenticatedAt;
        private LocalDateTime blockedUntil;

        public boolean isBlocked() {
            return state == SessionState.BLOCKED
                    && blockedUntil != null
                    && LocalDateTime.now().isBefore(blockedUntil);
        }

        public boolean isAuthenticated() {
            return state == SessionState.AUTHENTICATED && token != null;
        }
    }

    /**
     * Obtiene o crea una nueva sesión para el identificador dado
     */
    public Session getOrCreateSession(String identifier) {
        return sessions.computeIfAbsent(
                identifier,
                key -> {
                    log.info("Creating new session for identifier: {}", identifier);
                    return Session.builder()
                            .identifier(identifier)
                            .state(SessionState.AWAITING_CREDENTIALS)
                            .failedAttempts(0)
                            .createdAt(LocalDateTime.now())
                            .build();
                });
    }

    /**
     * Verifica si una sesión está autenticada
     */
    public boolean isAuthenticated(String identifier) {
        Session session = sessions.get(identifier);
        if (session == null) {
            return false;
        }

        // Verificar si está bloqueado
        if (session.isBlocked()) {
            log.warn("Session {} is blocked until {}", identifier, session.blockedUntil);
            return false;
        }

        return session.isAuthenticated();
    }

    /**
     * Marca una sesión como autenticada
     */
    public void authenticateSession(String identifier, String email, String token) {
        Session session = getOrCreateSession(identifier);
        session.setState(SessionState.AUTHENTICATED);
        session.setEmail(email);
        session.setToken(token);
        session.setFailedAttempts(0);
        session.setAuthenticatedAt(LocalDateTime.now());
        session.setBlockedUntil(null);

        log.info("Session {} authenticated successfully for user: {}", identifier, email);
    }

    /**
     * Registra un intento fallido de autenticación
     */
    public boolean recordFailedAttempt(String identifier) {
        Session session = getOrCreateSession(identifier);
        int newFailedAttempts = session.failedAttempts + 1;
        session.setFailedAttempts(newFailedAttempts);

        // Si alcanza el máximo de intentos, bloquear la sesión
        if (newFailedAttempts >= MAX_FAILED_ATTEMPTS) {
            session.setState(SessionState.BLOCKED);
            session.setBlockedUntil(LocalDateTime.now().plusHours(BLOCK_DURATION_HOURS));
            log.warn(
                    "Session {} blocked until {} after {} failed attempts",
                    identifier,
                    session.blockedUntil,
                    newFailedAttempts);
            return true;
        }

        log.info(
                "Failed attempt recorded for session {}. Total attempts: {}/{}",
                identifier,
                newFailedAttempts,
                MAX_FAILED_ATTEMPTS);
        return false;
    }

    /**
     * Obtiene el número de intentos fallidos restantes
     */
    public int getRemainingAttempts(String identifier) {
        Session session = getOrCreateSession(identifier);
        return Math.max(0, MAX_FAILED_ATTEMPTS - session.failedAttempts);
    }

    /**
     * Limpia una sesión específica
     */
    public void cleanupSession(String identifier) {
        sessions.remove(identifier);
        log.info("Session {} removed from memory", identifier);
    }

    /**
     * Obtiene la sesión completa
     */
    public Session getSession(String identifier) {
        return sessions.get(identifier);
    }

    /**
     * Tarea programada para limpiar sesiones expiradas
     */
    @Scheduled(fixedRate = 3600000) // Cada hora
    public void cleanupExpiredSessions() {
        log.info("Starting cleanup of expired sessions");
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);

        sessions.entrySet()
                .removeIf(
                        entry -> {
                            Session session = entry.getValue();

                            // Remover sesiones no autenticadas después de 24 horas
                            if (session.state != SessionState.AUTHENTICATED
                                    && session.createdAt.isBefore(cutoffTime)) {
                                log.info(
                                        "Removing expired non-authenticated session: {}",
                                        entry.getKey());
                                return true;
                            }

                            // Remover sesiones bloqueadas cuyo bloqueo ha expirado hace más de 24
                            // horas
                            if (session.isBlocked() && session.blockedUntil.isBefore(cutoffTime)) {
                                log.info("Removing expired blocked session: {}", entry.getKey());
                                return true;
                            }

                            return false;
                        });

        log.info("Cleanup completed. Active sessions: {}", sessions.size());
    }
}
