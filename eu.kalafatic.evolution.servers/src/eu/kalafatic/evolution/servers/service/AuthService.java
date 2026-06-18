package eu.kalafatic.evolution.servers.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import eu.kalafatic.evolution.servers.model.Session;
import eu.kalafatic.evolution.servers.model.User;
import eu.kalafatic.evolution.servers.repository.SessionRepository;
import eu.kalafatic.evolution.servers.repository.UserRepository;
import eu.kalafatic.evolution.servers.security.BCryptUtils;

public class AuthService {
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private static final int SESSION_TIMEOUT_MINUTES = 5;

    public AuthService(UserRepository userRepository, SessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    public Optional<String> login(String username, String password, String clientIp) throws SQLException {
        return login(username, password, clientIp, "GENERAL");
    }

    public Optional<String> login(String username, String password, String clientIp, String workflowType) throws SQLException {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.isEnabled() && BCryptUtils.checkPassword(password, user.getPasswordHash())) {
                String sessionId = UUID.randomUUID().toString();
                Session session = new Session();
                session.setSessionId(sessionId);
                session.setUserId(user.getId());
                session.setCreatedAt(LocalDateTime.now());
                session.setLastAccess(LocalDateTime.now());
                session.setClientIp(clientIp);
                session.setWorkflowType(workflowType);
                sessionRepository.save(session);
                return Optional.of(sessionId);
            }
        }
        return Optional.empty();
    }

    public void logout(String sessionId) throws SQLException {
        sessionRepository.deleteBySessionId(sessionId);
    }

    public Optional<User> validateSession(String sessionId) throws SQLException {
        Optional<Session> sessionOpt = sessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            if (session.getLastAccess().isAfter(LocalDateTime.now().minusMinutes(SESSION_TIMEOUT_MINUTES))) {
                sessionRepository.updateLastAccess(sessionId, LocalDateTime.now());
                return userRepository.findById(session.getUserId());
            } else {
                sessionRepository.deleteBySessionId(sessionId);
            }
        }
        return Optional.empty();
    }

    public Optional<Session> getSession(String sessionId) throws SQLException {
        return sessionRepository.findBySessionId(sessionId);
    }

    public void updateWorkflowType(String sessionId, String workflowType) throws SQLException {
        sessionRepository.updateWorkflowType(sessionId, workflowType);
    }

    public void cleanupSessions() throws SQLException {
        sessionRepository.deleteExpired(LocalDateTime.now().minusMinutes(SESSION_TIMEOUT_MINUTES));
    }
}
