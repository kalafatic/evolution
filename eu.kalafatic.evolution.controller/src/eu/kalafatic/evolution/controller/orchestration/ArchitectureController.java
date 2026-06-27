package eu.kalafatic.evolution.controller.orchestration;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.design.ComponentRecord;
import eu.kalafatic.evolution.controller.orchestration.design.DesignModel;
import eu.kalafatic.evolution.controller.orchestration.design.DesignRenderer;
import eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.controller.agents.GenomeUpdateAgent;

/**
 * Headless controller for repository architecture discovery and rendering.
 */
public class ArchitectureController {

    private final DesignRenderer renderer = new DesignRenderer();

    public enum ViewMode {
        USE_CASES, SUBSYSTEMS, COMPONENTS, KNOWLEDGE_GRAPH
    }

    public String renderArchitecture(Orchestrator orchestrator, String targetPath, String modeStr) {
        if ("UPDATE_GENOME".equals(modeStr)) {
            handleUpdateGenome(orchestrator, targetPath);
            return renderArchitecture(orchestrator, targetPath, "COMPONENTS");
        }

        ViewMode mode = ViewMode.COMPONENTS;
        try {
            if (modeStr != null) mode = ViewMode.valueOf(modeStr.toUpperCase());
        } catch (Exception e) {}

        DesignModel model = extractModel(orchestrator, targetPath, mode);
        return renderer.render(model, mode.name(), targetPath, new ArrayList<>());
    }

    public DesignModel extractModel(Orchestrator orchestrator, String targetPath, ViewMode mode) {
        if (targetPath == null || targetPath.isEmpty()) {
            return createDefaultModel(orchestrator);
        }

        File root = new File(targetPath);
        if (!root.exists()) {
             return createDefaultModel(orchestrator);
        }

        // 1. Try to load from Cache first
        DesignModel cached = loadModelFromCache(orchestrator, targetPath, mode);
        if (cached != null) {
            return filterModel(cached, mode);
        }

        // 2. Initial view: physical scan for metadata
        DesignModel model = discoverArchitectureNodes(root);

        // Integrate Reality Discovery Model if present in orchestrator
        if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
            String sid = orchestrator.getSelfDevSession().getId();
            SessionContainer session = SessionManager.getInstance().getSession(sid);
            if (session instanceof SessionContext) {
                TaskContext ctx = ((SessionContext)session).getTaskContext();
                if (ctx != null) {
                    Object trm = ctx.getOrchestrationState().getMetadata().get("targetRealityModel");
                    if (trm instanceof eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel) {
                        convertRealityToModel((eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel) trm, model);
                    }
                }
            }
        }

        model.setName(root.getName() + " Architecture");

        if (!model.getComponents().isEmpty()) {
            publishDiscoveryEvent(orchestrator);
        }

        if (model.getComponents().isEmpty()) {
            return createDefaultModel(orchestrator);
        }

        return filterModel(model, mode);
    }

    private void handleUpdateGenome(Orchestrator orchestrator, String targetPath) {
        if (targetPath == null || targetPath.isEmpty()) return;
        File root = new File(targetPath);
        if (!root.exists()) return;

        if (orchestrator != null) {
            SessionContainer session = SessionManager.getInstance().getSession(orchestrator.getId());
            if (session != null) {
                GenomeUpdateAgent agent = new GenomeUpdateAgent(session);
                agent.runUpdate(root, root.getName());

                session.getEventBus().publish(new RuntimeEvent(RuntimeEventType.FORGE_SNAPSHOT_CREATED, orchestrator.getId(), "ArchitectureController", "GENOME_UPDATED"));
            }
        } else {
            eu.kalafatic.evolution.selfdev.genome.hub.SelfDevGenomeHub hub = eu.kalafatic.evolution.selfdev.genome.hub.SelfDevGenomeHub.getInstance();
            hub.updateGenome(root, root.getName(), "v1.0.0");
        }
    }

    private void publishDiscoveryEvent(Orchestrator orchestrator) {
        if (orchestrator == null) return;
        SessionContainer session = SessionManager.getInstance().getSession(orchestrator.getId());
        if (session != null) {
            RuntimeEventBus bus = session.getEventBus();
            if (bus != null) {
                bus.publish(new RuntimeEvent(RuntimeEventType.VIEW_UPDATED, orchestrator.getId(), "ArchitectureController", "ARCH_DISCOVERED"));
            }
        }
    }

    private DesignModel loadModelFromCache(Orchestrator orchestrator, String targetPath, ViewMode mode) {
        if (orchestrator == null || orchestrator.getSharedMemory() == null) return null;
        try {
            String sid = (orchestrator.getSelfDevSession() != null) ? orchestrator.getSelfDevSession().getId() : "discovery-session";
            String json = ConversationState.load(orchestrator.getSharedMemory(), sid).getMetadata("architecture_cache");
            if (json == null || json.isEmpty()) return null;

            JSONObject obj = new JSONObject(json);

            // Path-aware validation
            String cachedPath = obj.optString("targetPath");
            String cachedMode = obj.optString("viewMode");
            if (cachedPath != null && !cachedPath.equals(targetPath)) return null;
            if (cachedMode != null && !cachedMode.equals(mode.name())) return null;

            DesignModel model = new DesignModel();
            model.setName(obj.optString("name", "Cached Architecture"));

            JSONArray comps = obj.optJSONArray("components");
            if (comps != null) {
                for (int i = 0; i < comps.length(); i++) {
                    JSONObject co = comps.getJSONObject(i);
                    ComponentRecord c = new ComponentRecord();
                    c.setId(co.optString("id"));
                    c.setName(co.optString("name"));
                    c.setType(co.optString("type"));
                    c.setDescription(co.optString("description"));
                    c.setPath(co.optString("path"));
                    c.setImportanceScore(co.optDouble("importance", 0.5));

                    JSONArray uc = co.optJSONArray("useCases");
                    if (uc != null) for (int j = 0; j < uc.length(); j++) c.getUseCases().add(uc.getString(j));

                    JSONArray kc = co.optJSONArray("keyClasses");
                    if (kc != null) for (int j = 0; j < kc.length(); j++) c.getKeyClasses().add(kc.getString(j));

                    model.getComponents().add(c);
                }
            }

            JSONArray rels = obj.optJSONArray("relationships");
            if (rels != null) {
                for (int i = 0; i < rels.length(); i++) {
                    JSONObject ro = rels.getJSONObject(i);
                    RelationshipRecord r = new RelationshipRecord();
                    r.setFrom(ro.optString("from"));
                    r.setTo(ro.optString("to"));
                    r.setType(ro.optString("type"));
                    model.getRelationships().add(r);
                }
            }
            return model;
        } catch (Exception e) {
            return null;
        }
    }

    private DesignModel discoverArchitectureNodes(File root) {
        DesignModel model = new DesignModel();
        eu.kalafatic.utils.semantic.AIContextTool tool = new eu.kalafatic.utils.semantic.AIContextTool();
        Map<String, ComponentRecord> nodes = new HashMap<>();

        scanForMetadata(root, root, tool, nodes, model);

        File archCtx = new File(root, "ARCHITECTURE_CONTEXT.md");
        if (archCtx.exists()) {
            parseArchitectureContext(archCtx, nodes, model);
        }

        if (model.getComponents().isEmpty()) {
            discoverLocalStructure(root, root, model);
        }

        return model;
    }

    private void discoverLocalStructure(File current, File root, DesignModel model) {
        File[] files = current.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                String name = f.getName();
                if (!name.startsWith(".") && !name.equals("target") && !name.equals("bin") && !name.equals("node_modules")) {
                    ComponentRecord rec = new ComponentRecord();
                    rec.setId(root.toURI().relativize(f.toURI()).getPath());
                    rec.setName(name);
                    rec.setType("MODULE");
                    rec.setDescription("Discovered module directory");
                    model.getComponents().add(rec);

                    if (current.equals(root)) {
                        discoverLocalStructure(f, root, model);
                    }
                }
            }
        }
    }

    private void scanForMetadata(File current, File root, eu.kalafatic.utils.semantic.AIContextTool tool, Map<String, ComponentRecord> nodes, DesignModel model) {
        File[] files = current.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                if (!f.getName().startsWith(".") && !f.getName().equals("target") && !f.getName().equals("bin")) {
                    scanForMetadata(f, root, tool, nodes, model);
                }
            } else if (f.getName().endsWith(".java") || f.getName().endsWith(".md") || f.getName().endsWith(".json")) {
                eu.kalafatic.utils.semantic.EvoMetadata meta = tool.loadMetadata(f);
                if (meta != null) {
                    ComponentRecord rec = new ComponentRecord();
                    rec.setId(meta.getPath() != null ? meta.getPath() : f.getName());
                    rec.setName(f.getName());
                    rec.setType(meta.getRole() != null ? meta.getRole().toUpperCase() : "COMPONENT");
                    rec.setDescription(meta.getSummary());
                    rec.setPath(meta.getPath());
                    rec.setImportanceScore(meta.getImportanceScore());

                    model.getComponents().add(rec);
                    nodes.put(rec.getId(), rec);

                    for (String dep : meta.getDependencyLinks()) {
                        RelationshipRecord rel = new RelationshipRecord();
                        rel.setFrom(rec.getId());
                        rel.setTo(dep);
                        rel.setType("DEPENDS_ON");
                        model.getRelationships().add(rel);
                    }
                }
            }
        }
    }

    private void parseArchitectureContext(File archCtx, Map<String, ComponentRecord> nodes, DesignModel model) {
        try {
            List<String> lines = Files.readAllLines(archCtx.toPath());
            String currentDomain = null;
            for (String line : lines) {
                if (line.startsWith("* **")) {
                    int end = line.indexOf("**", 4);
                    if (end > 4) {
                        String domain = line.substring(4, end);
                        ComponentRecord rec = new ComponentRecord();
                        rec.setId("domain:" + domain.toLowerCase());
                        rec.setName(domain);
                        rec.setType("SUBSYSTEM");
                        rec.setDescription(line.substring(end + 3));
                        model.getComponents().add(rec);
                        nodes.put(rec.getId(), rec);
                    }
                } else if (line.startsWith("* `")) {
                    int end = line.indexOf("`", 3);
                    if (end > 3) {
                        String path = line.substring(3, end);
                        ComponentRecord rec = nodes.get(path);
                        if (rec != null && currentDomain != null) {
                            RelationshipRecord rel = new RelationshipRecord();
                            rel.setFrom(rec.getId());
                            rel.setTo("domain:" + currentDomain.toLowerCase());
                            rel.setType("PART_OF");
                            model.getRelationships().add(rel);
                        }
                    }
                } else if (line.startsWith("## ")) {
                    String header = line.substring(3).trim();
                    if (!header.equalsIgnoreCase("Core Domains") && !header.equalsIgnoreCase("High-Importance Components")) {
                        currentDomain = header;
                    }
                }
            }
        } catch (IOException e) {}
    }

    private void convertRealityToModel(eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel reality, DesignModel model) {
        if (reality.getDomain() != null) {
            ComponentRecord d = new ComponentRecord();
            d.setId("reality:domain");
            d.setName(reality.getDomain());
            d.setType("DOMAIN");
            d.setDescription(reality.getPurpose());
            model.getComponents().add(d);
        }

        for (eu.kalafatic.evolution.controller.mediation.model.Subsystem sub : reality.getSubsystems()) {
            ComponentRecord sr = new ComponentRecord();
            sr.setId("reality:subsystem:" + sub.getId());
            sr.setName(sub.getName());
            sr.setType("SUBSYSTEM");
            sr.setDescription(sub.getPurpose());
            sr.setImportanceScore(sub.getConfidence());
            model.getComponents().add(sr);

            for (String file : sub.getCriticalFiles()) {
                RelationshipRecord rel = new RelationshipRecord();
                rel.setFrom(sr.getId());
                rel.setTo(file);
                rel.setType("CONTAINS");
                model.getRelationships().add(rel);
            }
        }

        for (eu.kalafatic.evolution.controller.mediation.model.ArchitecturalUseCase uc : reality.getUseCases()) {
            ComponentRecord ur = new ComponentRecord();
            ur.setId("reality:uc:" + uc.getId());
            ur.setName(uc.getName());
            ur.setType("USE_CASE");
            ur.setDescription(uc.getDescription());
            ur.setImportanceScore(uc.getConfidence());
            ur.getProperties().add("Rationale: " + uc.getRationale());
            model.getComponents().add(ur);

            for (String comp : uc.getSupportingComponents()) {
                RelationshipRecord rel = new RelationshipRecord();
                rel.setFrom(ur.getId());
                rel.setTo(comp);
                rel.setType("SUPPORTED_BY");
                model.getRelationships().add(rel);
            }
            for (String file : uc.getSupportingFiles()) {
                RelationshipRecord rel = new RelationshipRecord();
                rel.setFrom(ur.getId());
                rel.setTo(file);
                rel.setType("EVIDENCE");
                model.getRelationships().add(rel);
            }
        }

        for (eu.kalafatic.evolution.controller.mediation.model.Hotspot h : reality.getHotspots()) {
            ComponentRecord hr = new ComponentRecord();
            hr.setId("reality:hotspot:" + h.getId());
            hr.setName(h.getName());
            hr.setType("HOTSPOT");
            hr.setDescription(h.getDescription());
            hr.setImportanceScore(h.getSignificance());
            model.getComponents().add(hr);

            for (String art : h.getRelatedArtifacts()) {
                RelationshipRecord rel = new RelationshipRecord();
                rel.setFrom(hr.getId());
                rel.setTo(art);
                rel.setType("HIGHLIGHTS");
                model.getRelationships().add(rel);
            }
        }

        for (String obj : reality.getObjectives()) {
            ComponentRecord o = new ComponentRecord();
            o.setId("reality:objective:" + obj.hashCode());
            o.setName(obj);
            o.setType("OBJECTIVE");
            model.getComponents().add(o);
        }
    }

    private DesignModel filterModel(DesignModel model, ViewMode mode) {
        DesignModel filtered = new DesignModel();
        filtered.setName(model.getName() + " - " + mode.name());

        switch (mode) {
            case USE_CASES:
                filtered.setComponents(model.getComponents().stream()
                    .filter(c -> "USE_CASE".equals(c.getType()))
                    .collect(Collectors.toList()));
                List<String> ucIds = filtered.getComponents().stream().map(ComponentRecord::getId).collect(Collectors.toList());

                List<ComponentRecord> supporting = new ArrayList<>();
                for (RelationshipRecord rel : model.getRelationships()) {
                    if (ucIds.contains(rel.getFrom()) && ("SUPPORTED_BY".equals(rel.getType()) || "EVIDENCE".equals(rel.getType()))) {
                        model.getComponents().stream().filter(c -> c.getId().equals(rel.getTo())).findFirst().ifPresent(supporting::add);
                        filtered.getRelationships().add(rel);
                    }
                }
                supporting.forEach(c -> { if (!filtered.getComponents().contains(c)) filtered.getComponents().add(c); });
                break;

            case SUBSYSTEMS:
                filtered.setComponents(model.getComponents().stream()
                    .filter(c -> "SUBSYSTEM".equals(c.getType()) || "DOMAIN".equals(c.getType()))
                    .collect(Collectors.toList()));
                List<String> subIds = filtered.getComponents().stream().map(ComponentRecord::getId).collect(Collectors.toList());

                List<ComponentRecord> contained = new ArrayList<>();
                for (RelationshipRecord rel : model.getRelationships()) {
                    if (subIds.contains(rel.getFrom()) && "CONTAINS".equals(rel.getType())) {
                        model.getComponents().stream().filter(c -> c.getId().equals(rel.getTo())).findFirst().ifPresent(contained::add);
                        filtered.getRelationships().add(rel);
                    }
                }
                contained.forEach(c -> { if (!filtered.getComponents().contains(c)) filtered.getComponents().add(c); });
                break;

            case COMPONENTS:
                filtered.setComponents(model.getComponents().stream()
                    .filter(c -> !"USE_CASE".equals(c.getType()) && !"SUBSYSTEM".equals(c.getType()) && !"DOMAIN".equals(c.getType()))
                    .collect(Collectors.toList()));
                List<String> compIds = filtered.getComponents().stream().map(ComponentRecord::getId).collect(Collectors.toList());
                filtered.setRelationships(model.getRelationships().stream()
                    .filter(r -> compIds.contains(r.getFrom()) && compIds.contains(r.getTo()))
                    .collect(Collectors.toList()));
                break;

            case KNOWLEDGE_GRAPH:
                return model;
        }

        return filtered;
    }

    private DesignModel createDefaultModel(Orchestrator orchestrator) {
        DesignModel model = new DesignModel();
        if (orchestrator == null) {
             model.setName("Evolution Architecture");
             return model;
        }

        if (orchestrator.getSelfDevSession() != null) {
            SelfDevSession session = orchestrator.getSelfDevSession();
            model.setName("Self-Development Session: " + (session.getId() != null ? session.getId() : "Active"));

            int i = 0;
            for (Iteration iter : session.getIterations()) {
                ComponentRecord comp = new ComponentRecord();
                comp.setName(iter.getId());
                comp.setType("Step");
                comp.setX(100 + (i * 250) % 750);
                comp.setY(100 + (i / 3) * 200);
                model.getComponents().add(comp);

                if (i > 0) {
                    RelationshipRecord rel = new RelationshipRecord();
                    rel.setFrom(session.getIterations().get(i - 1).getId());
                    rel.setTo(iter.getId());
                    rel.setType("evolves");
                    model.getRelationships().add(rel);
                }
                i++;
            }
            if (model.getComponents().isEmpty()) {
                ComponentRecord comp = new ComponentRecord();
                comp.setName("Session Active");
                comp.setType("Status");
                comp.setX(100); comp.setY(100);
                model.getComponents().add(comp);
            }
            return model;
        }

        if (!orchestrator.getTasks().isEmpty()) {
            model.setName("Active Session Tasks");
            int i = 0;
            for (Task task : orchestrator.getTasks()) {
                ComponentRecord comp = new ComponentRecord();
                String name = task.getName() != null ? task.getName() : (task.getId() != null ? task.getId() : "Task" + i);
                comp.setName(name);
                comp.setType("Task");
                comp.setX(100 + (i * 220) % 660);
                comp.setY(100 + (i / 3) * 180);
                model.getComponents().add(comp);
                i++;
            }
            return model;
        }

        model.setName("Evolution Platform");
        ComponentRecord engine = new ComponentRecord();
        engine.setName("EvolutionKernel"); engine.setType("Engine"); engine.setX(100); engine.setY(100);
        model.getComponents().add(engine);

        return model;
    }
}
