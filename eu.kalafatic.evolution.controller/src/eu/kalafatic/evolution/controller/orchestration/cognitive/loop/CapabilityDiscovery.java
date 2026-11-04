package eu.kalafatic.evolution.controller.orchestration.cognitive.loop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.tools.ITool;
import eu.kalafatic.evolution.controller.tools.ToolFactory;

/**
 * Dynamic discovery service that inspects registered tools and session capabilities
 * and produces typed metadata schema for runtime intelligence (LLM).
 */
public class CapabilityDiscovery {

    public static class CapabilityDescriptor {
        private final String name;
        private final String description;
        private final String source;
        private final boolean requiresAuthority;

        public CapabilityDescriptor(String name, String description, String source, boolean requiresAuthority) {
            this.name = name;
            this.description = description;
            this.source = source;
            this.requiresAuthority = requiresAuthority;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getSource() {
            return source;
        }

        public boolean isRequiresAuthority() {
            return requiresAuthority;
        }

        @Override
        public String toString() {
            return name + " (" + source + "): " + description;
        }
    }

    public List<CapabilityDescriptor> discoverCapabilities(SessionContainer session) {
        List<CapabilityDescriptor> list = new ArrayList<>();

        // 1. Discover dynamically registered tools in ToolFactory
        java.util.Set<String> toolNames = new java.util.TreeSet<>(ToolFactory.getRegisteredTools());
        for (String toolName : toolNames) {
            ITool tool = ToolFactory.getTool(toolName);
            String desc = (tool != null) ? "System tool capability: " + tool.getName() : "Environment tool: " + toolName;
            boolean reqAuth = "shell".equalsIgnoreCase(toolName) || "git".equalsIgnoreCase(toolName);
            list.add(new CapabilityDescriptor(toolName, desc, "ToolFactory", reqAuth));
        }

        // 2. Discover session capabilities from CapabilityRegistry
        if (session != null && session.getCapabilityRegistry() != null) {
            session.getCapabilityRegistry().getAllCapabilities().forEach(cap -> {
                list.add(new CapabilityDescriptor(cap.getCapabilityId(), "Session capability: " + cap.getCapabilityId(), "CapabilityRegistry", true));
            });
        }

        // 3. Discover isolated agents available in session
        if (session != null) {
            try {
                var agents = eu.kalafatic.evolution.controller.agents.AgentFactory.createIsolatedAgents(session);
                for (var agent : agents) {
                    if (agent != null && agent.getType() != null) {
                        list.add(new CapabilityDescriptor("agent:" + agent.getType().toLowerCase(), "Agent capability: " + agent.getType(), "AgentFactory", false));
                    }
                }
            } catch (Exception ignored) {}
        }

        return Collections.unmodifiableList(list);
    }
}
