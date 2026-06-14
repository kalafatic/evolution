package eu.kalafatic.forge.controller.api;

import java.util.List;

public interface SessionController {

    String createSession(String name);

    void deleteSession(String sessionId);

    void selectSession(String sessionId);

    SessionInfo getCurrentSession();

    List<SessionInfo> getSessions();

    void saveSession(String sessionId);

}
