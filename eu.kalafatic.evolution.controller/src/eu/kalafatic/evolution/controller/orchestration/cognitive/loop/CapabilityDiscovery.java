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

        // 1. Discover registered tools in ToolFactory
        List<String> defaultTools = List.of("file", "maven", "git", "shell", "eclipse", "cpp", "database", "dataset_acquisition");
        for (String toolName : defaultTools) {
            ITool tool = ToolFactory.getTool(toolName);
            if (tool != null) {
                list.add(new CapabilityDescriptor(tool.getName(), "System tool capability for " + tool.getName(), "ToolFactory", false));
            } else {
                list.add(new CapabilityDescriptor(toolName, "Generic environment capability: " + toolName, "BuiltIn", false));
            }
        }

        // 2. Discover session capabilities
        if (session != null && session.getCapabilityRegistry() != null) {
            session.getCapabilityRegistry().getAllCapabilities().forEach(cap -> {
                list.add(new CapabilityDescriptor(cap.getCapabilityId(), "Session capability " + cap.getCapabilityId(), "CapabilityRegistry", true));
            });
        }

        return Collections.unmodifiableList(list);
    }
}
