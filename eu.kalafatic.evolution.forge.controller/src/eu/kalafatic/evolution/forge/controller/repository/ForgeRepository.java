package eu.kalafatic.evolution.forge.controller.repository;

import eu.kalafatic.evolution.forge.model.ForgeSession;
import java.util.List;

public interface ForgeRepository {
    void save(ForgeSession session);
    ForgeSession load(String id);
    List<ForgeSession> getSessions();
}
