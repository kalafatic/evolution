package eu.kalafatic.evolution.controller.workflow;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;

public class WorkflowGraphManager implements RuntimeEventListener {
	private final Map<String, SessionGraphData> sessionGraphs = new ConcurrentHashMap<>();

	public WorkflowGraphManager(RuntimeEventBus bus) {
		bus.subscribe(this);
	}

	private SessionGraphData getSessionGraph(String sessionId) {
		if (sessionId == null)
			sessionId = "Default";
		return sessionGraphs.computeIfAbsent(sessionId, id -> new SessionGraphData(id));
	}

	public void removeInstance(String sessionId) {
		if (sessionId != null) {
			sessionGraphs.remove(sessionId);
		}
	}
	
	

	@Override
	public void onEvent(RuntimeEvent event) {
		String sid = event.getSessionId();
		if (sid == null)
			sid = "Default";
		getSessionGraph(sid).onEvent(event);
	}

	public JSONObject getGraphJson(String sessionId) {
		return getSessionGraph(sessionId).getGraphJson();
	}

	public GraphEntity getEntity(String sessionId, String entityId) {
		return getSessionGraph(sessionId).getEntity(entityId);
	}

	// Helper classes to maintain state per session
	private static class SessionGraphData {
		private final String sessionId;
		private final Map<String, GraphEntity> entities = new ConcurrentHashMap<>();
		private final List<JSONObject> links = new CopyOnWriteArrayList<>();

		public SessionGraphData(String sessionId) {
			this.sessionId = sessionId;
			addEntity("user", EntityType.USER);
		}

		public void addEntity(String id, EntityType type) {
			entities.put(id, new GraphEntity(id, type));
		}

		public GraphEntity getEntity(String id) {
			return entities.get(id);
		}

		public void onEvent(RuntimeEvent event) {
			if (!sessionId.equals(event.getSessionId()))
				return;

			switch (event.getType()) {
			case MODE_CHANGED:
				handleModeChanged(event);
				break;
			case TASK_STARTED:
				handleTaskStarted(event);
				break;
			case TASK_COMPLETED:
				handleTaskCompleted(event);
				break;
			case TASK_FAILED:
				handleTaskFailed(event);
				break;
			case SUPERVISOR_STATUS_CHANGED:
				handleSupervisorStatusChanged(event);
				break;
			case ITERATION_STARTED:
				handleIterationStarted(event);
				break;
			case EXPORT_READY:
				handleExportReady(event);
				break;
			case DEPLOYMENT_STATUS_CHANGED:
				handleDeploymentStatusChanged(event);
				break;
			case MUTATING:
				handleMutating(event);
				break;
			case STEP_WAITING:
				handleStepWaiting(event);
				break;
			case MUTATION_REVIEW:
				handleMutationReview(event);
				break;
			case STEP_RESUMED:
				handleStepResumed(event);
				break;
			case FORGE_SESSION_CREATED:
			case FORGE_SESSION_SWITCHED:
				setupForgeTemplate();
				break;
			case FORGE_MODEL_CHANGED:
				handleForgeModelChanged(event);
				break;
			case FORGE_DATASET_IMPORTED:
				handleForgeDatasetImported(event);
				break;
			case FORGE_TRAINING_STARTED:
				handleForgeTrainingStarted(event);
				break;
			case FORGE_TRAINING_STOPPED:
				handleForgeTrainingStopped(event);
				break;
			case FORGE_TRAINING_FAILED:
				handleForgeTrainingFailed(event);
				break;
			case FORGE_SNAPSHOT_CREATED:
				handleForgeSnapshotCreated(event);
				break;
			case EVOLUTION_PROGRESS:
				handleForgeProgress(event);
				break;
			}
		}

		private void handleForgeModelChanged(RuntimeEvent event) {
			setupForgeTemplateIfNeeded();
			GraphEntity scanner = entities.get("scanner");
			if (scanner != null) {
				scanner.setStatus("DONE");
				if (event.getMetadata().containsKey("filesScanned")) {
					scanner.setRuntimeState(event.getMetadata().get("filesScanned") + " files found");
					scanner.getMetadata().put("Files Scanned", event.getMetadata().get("filesScanned"));
				} else {
					scanner.setRuntimeState("Scanning completed");
				}
			}
			GraphEntity enhancer = entities.get("enhancer");
			if (enhancer != null) {
				enhancer.setStatus("RUNNING");
				enhancer.setRuntimeState("Analyzing & Enhancing...");
			}
		}

		private void handleForgeDatasetImported(RuntimeEvent event) {
			setupForgeTemplateIfNeeded();
			GraphEntity enhancer = entities.get("enhancer");
			if (enhancer != null) {
				enhancer.setStatus("DONE");
				if (event.getMetadata().containsKey("instructionsGenerated")) {
					enhancer.setRuntimeState(event.getMetadata().get("instructionsGenerated") + " samples generated");
					enhancer.getMetadata().put("Samples", event.getMetadata().get("instructionsGenerated"));
				} else {
					enhancer.setRuntimeState("Dataset Synthesized");
				}
			}
			GraphEntity trainer = entities.get("trainer");
			if (trainer != null) {
				trainer.setStatus("RUNNING");
				trainer.setRuntimeState("Configuring Trainer...");
			}
		}

		private void handleForgeTrainingStarted(RuntimeEvent event) {
			setupForgeTemplateIfNeeded();
			GraphEntity enhancer = entities.get("enhancer");
			if (enhancer != null && !"DONE".equals(enhancer.getStatus())) {
				enhancer.setStatus("DONE");
			}
			GraphEntity trainer = entities.get("trainer");
			if (trainer != null) {
				trainer.setStatus("RUNNING");
				trainer.setRuntimeState("Epoch 1/1: Training...");
				if (event.getMetadata().containsKey("currentLoss")) {
					trainer.getMetadata().put("Loss", event.getMetadata().get("currentLoss"));
				}
			}
		}

		private void handleForgeTrainingStopped(RuntimeEvent event) {
			setupForgeTemplateIfNeeded();
			GraphEntity trainer = entities.get("trainer");
			if (trainer != null) {
				trainer.setStatus("IDLE");
				trainer.setRuntimeState("Training stopped");
			}
		}

		private void handleForgeTrainingFailed(RuntimeEvent event) {
			setupForgeTemplateIfNeeded();
			GraphEntity trainer = entities.get("trainer");
			if (trainer != null) {
				trainer.setStatus("FAILED");
				trainer.setRuntimeState("Failed: " + event.getPayload().toString());
			}
		}

		private void handleForgeSnapshotCreated(RuntimeEvent event) {
			setupForgeTemplateIfNeeded();
			GraphEntity registration = entities.get("registration");
			if (registration != null) {
				registration.setStatus("DONE");
				registration.setRuntimeState("Snapshot created");
			}
		}

		private void handleForgeProgress(RuntimeEvent event) {
			setupForgeTemplateIfNeeded();
			Map<String, Object> meta = event.getMetadata();
			String status = (String) meta.getOrDefault("status", "RUNNING");

			if ("SCANNING".equals(status)) {
				GraphEntity scanner = entities.get("scanner");
				if (scanner != null) {
					scanner.setStatus("RUNNING");
					scanner.setRuntimeState("Scanning codebase...");
					if (meta.containsKey("filesScanned")) {
						scanner.setRuntimeState("Scanned " + meta.get("filesScanned") + " files");
						scanner.getMetadata().put("Files Scanned", meta.get("filesScanned"));
					}
				}
			} else if ("ENHANCING".equals(status)) {
				GraphEntity scanner = entities.get("scanner");
				if (scanner != null) scanner.setStatus("DONE");

				GraphEntity enhancer = entities.get("enhancer");
				if (enhancer != null) {
					enhancer.setStatus("RUNNING");
					enhancer.setRuntimeState("Synthesizing dataset...");
				}
			} else if ("TRAINING".equals(status)) {
				GraphEntity scanner = entities.get("scanner");
				if (scanner != null) scanner.setStatus("DONE");
				GraphEntity enhancer = entities.get("enhancer");
				if (enhancer != null) enhancer.setStatus("DONE");

				GraphEntity trainer = entities.get("trainer");
				if (trainer != null) {
					trainer.setStatus("RUNNING");
					Object lossObj = meta.getOrDefault("currentLoss", 0.0);
					double loss = (lossObj instanceof Number) ? ((Number) lossObj).doubleValue() : 0.0;
					String epoch = String.valueOf(meta.getOrDefault("currentEpoch", "1/1"));
					trainer.setRuntimeState("Loss: " + String.format("%.4f", loss) + " (Epoch: " + epoch + ")");
					trainer.getMetadata().put("Loss", loss);
					trainer.getMetadata().put("Epoch", epoch);
				}
			} else if ("EXPORTING".equals(status)) {
				GraphEntity trainer = entities.get("trainer");
				if (trainer != null) trainer.setStatus("DONE");

				GraphEntity exporter = entities.get("exporter");
				if (exporter != null) {
					exporter.setStatus("RUNNING");
					exporter.setRuntimeState("Compiling GGUF...");
					if (meta.containsKey("outputFolder")) {
						exporter.getMetadata().put("Output Folder", meta.get("outputFolder"));
					}
				}
			} else if ("EXPORT_GGUF".equals(status)) {
				GraphEntity exporter = entities.get("exporter");
				if (exporter != null) exporter.setStatus("DONE");

				GraphEntity registration = entities.get("registration");
				if (registration != null) {
					registration.setStatus("RUNNING");
					registration.setRuntimeState("Registering with Ollama...");
				}
			} else if ("COMPLETE".equals(status)) {
				for (GraphEntity entity : entities.values()) {
					if (!"user".equals(entity.getId()) && !"forge_engine".equals(entity.getId())) {
						entity.setStatus("DONE");
					}
				}
				GraphEntity forge = entities.get("forge_engine");
				if (forge != null) forge.setStatus("DONE");
			}
		}

		private void setupForgeTemplateIfNeeded() {
			if (!entities.containsKey("scanner")) {
				setupForgeTemplate();
			}
		}

		private void handleStepWaiting(RuntimeEvent event) {
			String stepId = event.getPayload().toString();
			SessionContainer session = SessionManager.getInstance().getSession(sessionId);
			if (session == null) return;
			WorkflowStep step = session.getWorkflowRegistry().getStep(stepId);
			if (step != null) {
				GraphEntity entity = entities.get(step.getEntityId());
				if (entity == null) {
					// Try to map generic mediated/evo entities if they don't exist yet
					if ("mediated_flow".equals(step.getEntityId())) {
						setupMediatedTemplate();
						entity = entities.get("mediated_flow");
					} else if ("evolution_loop".equals(step.getEntityId())) {
						setupSelfDevTemplate();
						entity = entities.get("evolution_loop");
					}
				}
				if (entity != null) {
					entity.setStatus("WAITING_USER");
					entity.setRuntimeState("STEP: " + step.getStepType());
					entity.getMetadata().put("currentStepId", stepId);
					entity.getMetadata().put("stepDescription", step.getDescription());
					entity.getActions().clear();
					entity.getActions().add("CONTINUE");
					entity.getActions().add("RETRY");
					entity.getActions().add("SKIP");
					entity.getActions().add("INSPECT");
				}
			}
		}

		private void handleStepResumed(RuntimeEvent event) {
			String stepId = event.getPayload().toString();
			SessionContainer session = SessionManager.getInstance().getSession(sessionId);
			if (session == null) return;
			WorkflowStep step = session.getWorkflowRegistry().getStep(stepId);
			if (step != null) {
				GraphEntity entity = entities.get(step.getEntityId());
				if (entity != null) {
					entity.setStatus("RUNNING");
					entity.getActions().clear();
					// Optionally restore original actions based on entity type
					if (EntityType.SUPERVISOR.equals(entity.getType())) {
						entity.getActions().add("STOP_SUPERVISOR");
					}
				}
			}
		}

		public void addLink(String from, String to, String type) {
			JSONObject link = new JSONObject();
			link.put("from", from);
			link.put("to", to);
			link.put("type", type);
			links.add(link);
		}

		private void handleTaskFailed(RuntimeEvent event) {
			String taskId = event.getPayload().toString();
			GraphEntity entity = entities.get(taskId);
			if (entity != null)
				entity.setStatus("FAILED");
		}

		private void handleSupervisorStatusChanged(RuntimeEvent event) {
			GraphEntity supervisor = entities.get("supervisor");
			if (supervisor == null) {
				addEntity("supervisor", EntityType.SUPERVISOR);
				supervisor = entities.get("supervisor");
			}
			supervisor.setStatus(event.getPayload().toString());
			if ("RUNNING".equals(supervisor.getStatus())) {
				supervisor.getActions().clear();
				supervisor.getActions().add("STOP_SUPERVISOR");
			} else {
				supervisor.getActions().clear();
				supervisor.getActions().add("START_SUPERVISOR");
			}
		}

		private void handleIterationStarted(RuntimeEvent event) {
			String iterId = event.getPayload().toString();
			addEntity(iterId, EntityType.EVOLUTION_LOOP);
			entities.get(iterId).setStatus("RUNNING");
			addLink("supervisor", iterId, "iteration");
		}

		private void handleExportReady(RuntimeEvent event) {
			addEntity("export", EntityType.ZIP_EXPORT);
			GraphEntity export = entities.get("export");
			export.setStatus("READY");
			export.getMetadata().put("path", event.getPayload().toString());
			export.getActions().add("OPEN_ZIP");
		}

		private void handleDeploymentStatusChanged(RuntimeEvent event) {
			String target = event.getMetadata().getOrDefault("target", "target").toString();
			addEntity(target, EntityType.DEPLOYMENT_TARGET);
			entities.get(target).setStatus(event.getPayload().toString());
		}

		private void handleModeChanged(RuntimeEvent event) {
			String mode = event.getPayload().toString();
			if ("SELF_DEV".equals(mode) || "SELF_DEV_MODE".equals(mode)) {
				setupSelfDevTemplate();
			} else if ("DARWIN_MODE".equals(mode)) {
				setupDarwinTemplate();
			} else if ("MEDIATED".equals(mode) || "HYBRID_MANUAL_EXPORT".equals(mode)) {
				setupMediatedTemplate();
			} else if ("ASSISTED_CODING".equals(mode)) {
				setupAssistedCodingTemplate();
			} else if ("FORGE".equals(mode)) {
				setupForgeTemplate();
			} else {
				setupDefaultTemplate();
			}
		}

		private void handleTaskStarted(RuntimeEvent event) {
			String taskId = event.getPayload().toString();
			GraphEntity entity = entities.get(taskId);
			if (entity == null) {
				entity = new GraphEntity(taskId, EntityType.SELF_DEV_TASK);
				entities.put(taskId, entity);

				if (entities.containsKey("mutation_loop"))
					addLink("mutation_loop", taskId, "task");
				else if (entities.containsKey("evolution_loop"))
					addLink("evolution_loop", taskId, "task");
				else if (entities.containsKey("assisted_coding"))
					addLink("assisted_coding", taskId, "task");
				else if (entities.containsKey("supervisor"))
					addLink("supervisor", taskId, "task");
				else if (entities.containsKey("orchestrator"))
					addLink("orchestrator", taskId, "task");
				else if (entities.containsKey("mediated_flow"))
					addLink("mediated_flow", taskId, "task");
				else
					addLink("user", taskId, "task");
			}
			entity.setStatus("RUNNING");
		}

		private void handleTaskCompleted(RuntimeEvent event) {
			String taskId = event.getPayload().toString();
			GraphEntity entity = entities.get(taskId);
			if (entity != null)
				entity.setStatus("DONE");
		}

		private void setupSelfDevTemplate() {
			entities.clear();
			links.clear();
			addEntity("user", EntityType.USER);
			addEntity("supervisor", EntityType.SUPERVISOR);
			addEntity("evolution_loop", EntityType.EVOLUTION_LOOP);
			addLink("user", "supervisor", "trigger");
			addLink("supervisor", "evolution_loop", "manages");
		}

		private void setupMediatedTemplate() {
			entities.clear();
			links.clear();
			addEntity("user", EntityType.USER);
			addEntity("mediated_flow", EntityType.MEDIATED_FLOW);
			addEntity("zip_export", EntityType.ZIP_EXPORT);
			addEntity("workspace", EntityType.SELF_DEV_TASK);

			addLink("user", "mediated_flow", "trigger");
			addLink("mediated_flow", "workspace", "updates");
			addLink("mediated_flow", "zip_export", "produces");
		}

		private void handleMutating(RuntimeEvent event) {
			GraphEntity loop = entities.get("evolution_loop");
			if (loop != null)
				loop.setStatus("MUTATING");
		}

		private void handleMutationReview(RuntimeEvent event) {
			if (event.getPayload() instanceof JSONArray) {
				JSONArray variants = (JSONArray) event.getPayload();
				String parentId = (String) event.getMetadata().getOrDefault("parentId", "evolution_loop");

				for (int i = 0; i < variants.length(); i++) {
					JSONObject v = variants.getJSONObject(i);
					String vId = v.optString("id", "variant-" + i);
					addEntity(vId, EntityType.DARWIN_VARIANT);
					GraphEntity entity = entities.get(vId);
					entity.setStatus("PENDING");
					entity.setRuntimeState(v.optString("strategy", ""));
					addLink(parentId, vId, "mutation");
				}
			}
		}

		private void setupDarwinTemplate() {
			entities.clear();
			links.clear();
			addEntity("user", EntityType.USER);
			addEntity("darwin_engine", EntityType.SUPERVISOR);
			addEntity("mutation_loop", EntityType.EVOLUTION_LOOP);
			addLink("user", "darwin_engine", "trigger");
			addLink("darwin_engine", "mutation_loop", "manages");
		}

		private void setupDefaultTemplate() {
			entities.clear();
			links.clear();
			addEntity("user", EntityType.USER);
			addEntity("orchestrator", EntityType.SUPERVISOR);
			addEntity("local_llm", EntityType.LOCAL_LLM);
			addEntity("remote_llm", EntityType.REMOTE_LLM);

			addLink("user", "orchestrator", "trigger");
			addLink("orchestrator", "local_llm", "query");
			addLink("orchestrator", "remote_llm", "query");

			SessionContainer session = SessionManager.getInstance().getSession(sessionId);
			if (session instanceof eu.kalafatic.evolution.controller.orchestration.SessionContext) {
				eu.kalafatic.evolution.controller.orchestration.SessionContext ctx = (eu.kalafatic.evolution.controller.orchestration.SessionContext) session;
				if (ctx.getTaskContext() != null && ctx.getTaskContext().getOrchestrator() != null) {
					eu.kalafatic.evolution.model.orchestration.Orchestrator orch = ctx.getTaskContext().getOrchestrator();
					if (eu.kalafatic.evolution.model.orchestration.AiMode.REMOTE.equals(orch.getAiMode())) {
						entities.get("remote_llm").setStatus("RUNNING");
						entities.get("local_llm").setStatus("IDLE");
					} else {
						entities.get("local_llm").setStatus("RUNNING");
						entities.get("remote_llm").setStatus("IDLE");
					}
				} else {
					entities.get("local_llm").setStatus("RUNNING");
					entities.get("remote_llm").setStatus("IDLE");
				}
			} else {
				entities.get("local_llm").setStatus("RUNNING");
				entities.get("remote_llm").setStatus("IDLE");
			}
		}

		private void setupAssistedCodingTemplate() {
			entities.clear();
			links.clear();
			addEntity("user", EntityType.USER);
			addEntity("assisted_coding", EntityType.SUPERVISOR);
			addEntity("llm", EntityType.LOCAL_LLM);
			addEntity("workspace", EntityType.SELF_DEV_TASK);
			addEntity("maven", EntityType.SELF_DEV_TASK);

			addLink("user", "assisted_coding", "trigger");
			addLink("assisted_coding", "llm", "query");
			addLink("assisted_coding", "workspace", "code");
			addLink("workspace", "maven", "build");
		}

		private void setupForgeTemplate() {
			entities.clear();
			links.clear();

			addEntity("user", EntityType.USER);
			addEntity("forge_engine", EntityType.SUPERVISOR);
			addEntity("scanner", EntityType.SELF_DEV_TASK);
			addEntity("enhancer", EntityType.SELF_DEV_TASK);
			addEntity("trainer", EntityType.EVOLUTION_LOOP);
			addEntity("exporter", EntityType.ZIP_EXPORT);
			addEntity("registration", EntityType.DEPLOYMENT_TARGET);

			addLink("user", "forge_engine", "trigger");
			addLink("forge_engine", "scanner", "manages");
			addLink("scanner", "enhancer", "pipeline");
			addLink("enhancer", "trainer", "pipeline");
			addLink("trainer", "exporter", "pipeline");
			addLink("exporter", "registration", "pipeline");

			entities.get("forge_engine").setStatus("RUNNING");
			entities.get("scanner").setStatus("PENDING");
			entities.get("enhancer").setStatus("PENDING");
			entities.get("trainer").setStatus("PENDING");
			entities.get("exporter").setStatus("PENDING");
			entities.get("registration").setStatus("PENDING");
		}

		public JSONObject getGraphJson() {
			JSONObject json = new JSONObject();
			JSONArray nodesArr = new JSONArray();
			for (GraphEntity entity : entities.values())
				nodesArr.put(entity.toJson());
			json.put("nodes", nodesArr);
			json.put("links", new JSONArray(links));
			return json;
		}
	}
}
