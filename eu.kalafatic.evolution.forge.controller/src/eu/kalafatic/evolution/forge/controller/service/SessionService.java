package eu.kalafatic.evolution.forge.controller.service;

import java.util.List;

import eu.kalafatic.evolution.forge.controller.api.SessionInfo;

public interface SessionService {
    String createSession(String name);
    void deleteSession(String sessionId);
    void selectSession(String sessionId);
    SessionInfo getCurrentSession();
    List<SessionInfo> getSessions();
    void saveSession(String sessionId);
}
