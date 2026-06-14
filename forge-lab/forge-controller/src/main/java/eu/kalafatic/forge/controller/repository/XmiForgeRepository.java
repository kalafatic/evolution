package eu.kalafatic.forge.controller.repository;

import eu.kalafatic.forge.model.ForgeSession;
import java.util.List;
import java.util.ArrayList;

public class XmiForgeRepository implements ForgeRepository {
    @Override
    public void save(ForgeSession session) {}
    @Override
    public ForgeSession load(String id) { return null; }
    @Override
    public List<ForgeSession> getSessions() { return new ArrayList<>(); }
}
