package eu.kalafatic.evolution.forge.controller.impl;

import eu.kalafatic.evolution.forge.controller.api.SessionController;
import eu.kalafatic.evolution.forge.controller.api.SessionInfo;
import eu.kalafatic.evolution.forge.controller.service.SessionService;
import java.util.List;

public class SessionControllerImpl implements SessionController {
    private final SessionService sessionService;

    public SessionControllerImpl(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public String createSession(String name) {
        return sessionService.createSession(name);
    }

    @Override
    public void deleteSession(String sessionId) {
        sessionService.deleteSession(sessionId);
    }

    @Override
    public void selectSession(String sessionId) {
        sessionService.selectSession(sessionId);
    }

    @Override
    public SessionInfo getCurrentSession() {
        return sessionService.getCurrentSession();
    }

    @Override
    public List<SessionInfo> getSessions() {
        return sessionService.getSessions();
    }

    @Override
    public void saveSession(String sessionId) {
        sessionService.saveSession(sessionId);
    }
}
