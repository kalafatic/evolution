package eu.kalafatic.forge.controller.repository;

import eu.kalafatic.forge.model.ForgeSession;
import java.util.List;

public interface ForgeRepository {
    void save(ForgeSession session);
    ForgeSession load(String id);
    List<ForgeSession> getSessions();
}
