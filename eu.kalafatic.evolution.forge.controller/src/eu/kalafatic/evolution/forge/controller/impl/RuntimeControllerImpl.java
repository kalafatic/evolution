package eu.kalafatic.evolution.forge.controller.impl;

import eu.kalafatic.evolution.forge.controller.api.RuntimeController;
import eu.kalafatic.evolution.forge.controller.service.OllamaService;
import java.lang.reflect.Method;

public class RuntimeControllerImpl implements RuntimeController {
    private final OllamaService ollamaService;

    public RuntimeControllerImpl(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @Override
    public void deployModel(String sessionId, String modelName) {
        publishEvent(sessionId, "DEPLOYMENT_STARTED", modelName);
        if (ollamaService != null) {
            try {
                ollamaService.pullModel(modelName, progress -> {
                    // Progress tracking handled via UI events
                });
                ollamaService.setModel(modelName);
            } catch (Exception e) {}
        }
    }

    @Override
    public String chat(String sessionId, String message) {
        if (ollamaService != null) {
            try {
                return ollamaService.chat(message, "runtime-session");
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }
        return "Service Unavailable";
    }

    private void publishEvent(String sessionId, String typeName, Object payload) {
        try {
            Class<?> sessionManagerClass = Class.forName("eu.kalafatic.evolution.controller.orchestration.SessionManager");
            Method getInstance = sessionManagerClass.getMethod("getInstance");
            Object sm = getInstance.invoke(null);

            Method getSession = sm.getClass().getMethod("getSession", String.class);
            Object session = getSession.invoke(sm, sessionId);

            if (session != null) {
                Method getEventBus = session.getClass().getMethod("getEventBus");
                Object bus = getEventBus.invoke(session);

                if (bus != null) {
                    Class<?> eventTypeClass = Class.forName("eu.kalafatic.evolution.controller.workflow.RuntimeEventType");
                    Object type = Enum.valueOf((Class<Enum>)eventTypeClass, typeName);

                    Class<?> eventClass = Class.forName("eu.kalafatic.evolution.controller.workflow.RuntimeEvent");
                    Object event = eventClass.getConstructor(eventTypeClass, String.class, String.class, Object.class)
                                             .newInstance(type, sessionId, "RuntimeController", payload);

                    Method publish = bus.getClass().getMethod("publish", eventClass);
                    publish.invoke(bus, event);
                }
            }
        } catch (Exception e) {
            // Decoupled: fail silently if controller bundle is not present
        }
    }
}
