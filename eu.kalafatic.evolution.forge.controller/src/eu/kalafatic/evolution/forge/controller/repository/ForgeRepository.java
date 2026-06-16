package eu.kalafatic.evolution.forge.controller.repository;

import java.util.List;

import eu.kalafatic.evolution.forge.model.ForgeSession;

public interface ForgeRepository {
    void save(ForgeSession session);
    ForgeSession load(String id);
    List<ForgeSession> getSessions();
}
