package eu.kalafatic.evolution.forge.controller.service;

import eu.kalafatic.evolution.forge.controller.api.SessionInfo;
import java.util.List;

public interface SessionService {
    String createSession(String name);
    void deleteSession(String sessionId);
    void selectSession(String sessionId);
    SessionInfo getCurrentSession();
    List<SessionInfo> getSessions();
    void saveSession(String sessionId);
}
