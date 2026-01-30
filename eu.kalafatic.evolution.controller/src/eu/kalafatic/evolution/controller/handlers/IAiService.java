package eu.kalafatic.evolution.controller.handlers;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public interface IAiService {
    String sendRequest(Orchestrator orchestrator, String prompt, String proxyUrl) throws Exception;
}
