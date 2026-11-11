package eu.kalafatic.evolution.controller.workflow;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionContext;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;

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
		private volatile String currentMode = "DEFAULT";

		public SessionGraphData(String sessionId) {
			this.sessionId = sessionId;
			addEntity("user", "User", EntityType.USER, "Active developer / operator triggering actions");
		}

		public void addEntity(String id, EntityType type) {
			addEntity(id, id, type, null);
		}

		public void addEntity(String id, String label, EntityType type, String description) {
			GraphEntity existing = entities.get(id);
			if (existing != null) {
				if (label != null && !label.trim().isEmpty()) {
					existing.setLabel(label);
				}
				if (description != null) {
					existing.setDescription(description);
				}
			} else {
				GraphEntity entity = new GraphEntity(id, label, type);
				if (description != null) {
					entity.setDescription(description);
				}
				entities.put(id, entity);
			}
		}

		public GraphEntity getEntity(String id) {
			return entities.get(id);
		}

		public synchronized void addLink(String from, String to, String type) {
			if (from == null || to == null) return;
			from = from.trim();
			to = to.trim();
			if (from.isEmpty() || to.isEmpty() || from.equals(to)) return;

			for (JSONObject existing : links) {
				if (from.equals(existing.optString("from")) &&
				    to.equals(existing.optString("to")) &&
				    (type == null || type.equals(existing.optString("type")))) {
					return; // Deduplicate
				}
			}

			JSONObject link = new JSONObject();
			link.put("from", from);
			link.put("to", to);
			link.put("type", type != null ? type : "dependency");
			links.add(link);
		}

		public void onEvent(RuntimeEvent event) {
			if (!sessionId.equals(event.getSessionId()))
				return;

			switch (event.getType()) {
			case FLOW_STARTED:
				handleFlowStarted(event);
				break;
			case FLOW_COMPLETED:
				handleFlowCompleted(event);
				break;
			case FLOW_PAUSED:
				handleFlowPaused(event);
				break;
			case COGNITIVE_STATE_CHANGED:
				handleCognitiveStateChanged(event);
				break;
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
			case REASONING_STEP:
			case HYPOTHESIS_GENERATED:
			case PLAN_SELECTED:
			case BRANCH_CREATED:
			case DECISION_UPDATED:
				handleReasoningEvent(event);
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
			GraphEntity validator = entities.get("validator");
			if (validator != null) {
				validator.setStatus("RUNNING");
				validator.setRuntimeState("Validating dataset & configuration...");
			}
		}

		private void handleForgeTrainingStarted(RuntimeEvent event) {
			setupForgeTemplateIfNeeded();
			GraphEntity scanner = entities.get("scanner");
			if (scanner != null && !"DONE".equals(scanner.getStatus())) {
				scanner.setStatus("DONE");
			}
			GraphEntity enhancer = entities.get("enhancer");
			if (enhancer != null && !"DONE".equals(enhancer.getStatus())) {
				enhancer.setStatus("DONE");
			}
			GraphEntity validator = entities.get("validator");
			if (validator != null && !"DONE".equals(validator.getStatus())) {
				validator.setStatus("DONE");
			}
			GraphEntity trainer = entities.get("trainer");
			if (trainer != null) {
				trainer.setStatus("RUNNING");
				trainer.setRuntimeState("Epoch 1: Training...");
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
				trainer.setRuntimeState("Failed: " + (event.getPayload() != null ? event.getPayload().toString() : "unknown"));
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
			} else if ("PREFLIGHT".equals(status) || "VALIDATING_INPUT".equals(status)) {
				GraphEntity scanner = entities.get("scanner");
				if (scanner != null) scanner.setStatus("DONE");
				GraphEntity enhancer = entities.get("enhancer");
				if (enhancer != null) enhancer.setStatus("DONE");

				GraphEntity validator = entities.get("validator");
				if (validator != null) {
					validator.setStatus("RUNNING");
					validator.setRuntimeState("Checking preflight rules...");
				}
			} else if ("TRAINING".equals(status)) {
				GraphEntity scanner = entities.get("scanner");
				if (scanner != null) scanner.setStatus("DONE");
				GraphEntity enhancer = entities.get("enhancer");
				if (enhancer != null) enhancer.setStatus("DONE");
				GraphEntity validator = entities.get("validator");
				if (validator != null) validator.setStatus("DONE");

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
			} else if ("EVALUATING".equals(status)) {
				GraphEntity trainer = entities.get("trainer");
				if (trainer != null) trainer.setStatus("DONE");

				GraphEntity evaluator = entities.get("evaluator");
				if (evaluator != null) {
					evaluator.setStatus("RUNNING");
					evaluator.setRuntimeState("Evaluating loss & quality...");
				}
			} else if ("EXPORTING".equals(status)) {
				GraphEntity trainer = entities.get("trainer");
				if (trainer != null) trainer.setStatus("DONE");
				GraphEntity evaluator = entities.get("evaluator");
				if (evaluator != null) evaluator.setStatus("DONE");

				GraphEntity exporter = entities.get("exporter");
				if (exporter != null) {
					exporter.setStatus("RUNNING");
					exporter.setRuntimeState("Compiling GGUF...");
					if (meta.containsKey("outputFolder")) {
						exporter.getMetadata().put("Output Folder", meta.get("outputFolder"));
					}
				}
			} else if ("EXPORT_GGUF".equals(status) || "REGISTERING".equals(status)) {
				GraphEntity exporter = entities.get("exporter");
				if (exporter != null) exporter.setStatus("DONE");

				GraphEntity registration = entities.get("registration");
				if (registration != null) {
					registration.setStatus("RUNNING");
					registration.setRuntimeState("Registering with Ollama...");
				}
			} else if ("COMPLETE".equals(status)) {
				for (GraphEntity entity : entities.values()) {
					if (!"user".equals(entity.getId())) {
						entity.setStatus("DONE");
					}
				}
			} else if ("ERROR".equals(status)) {
				GraphEntity active = entities.get("trainer");
				if (active == null || !"RUNNING".equals(active.getStatus())) {
					active = entities.get("validator");
				}
				if (active != null) {
					active.setStatus("FAILED");
					active.setRuntimeState("Error during execution");
				}
			}
		}

		private void handleFlowStarted(RuntimeEvent event) {
			String resolvedMode = resolveActiveMode();
			boolean isForgeRequested = "FORGE".equalsIgnoreCase(resolvedMode) || "FORGE".equalsIgnoreCase(currentMode);

			if (isForgeRequested) {
				if (!"FORGE".equals(currentMode) || !entities.containsKey("scanner")) {
					setupForgeTemplate();
				}
			} else {
				// If we currently hold stale FORGE nodes, clean them out for non-FORGE workflows
				if (entities.containsKey("forge_engine")) {
					if ("SELF_DEV".equals(resolvedMode) || "SELF_DEV_MODE".equals(resolvedMode)) {
						setupSelfDevTemplate();
					} else if ("DARWIN_MODE".equals(resolvedMode)) {
						setupDarwinTemplate();
					} else if ("MEDIATED".equals(resolvedMode) || "HYBRID_MANUAL_EXPORT".equals(resolvedMode)) {
						setupMediatedTemplate();
					} else if ("ASSISTED_CODING".equals(resolvedMode)) {
						setupAssistedCodingTemplate();
					} else {
						setupDefaultTemplate();
					}
				} else if (entities.size() <= 1) {
					if ("SELF_DEV".equals(resolvedMode) || "SELF_DEV_MODE".equals(resolvedMode)) {
						setupSelfDevTemplate();
					} else {
						setupDefaultTemplate();
					}
				}
			}

			String payloadText = event.getPayload() != null ? event.getPayload().toString() : "";
			for (GraphEntity entity : entities.values()) {
				if (EntityType.SUPERVISOR.equals(entity.getType()) || EntityType.MEDIATED_FLOW.equals(entity.getType())) {
					entity.setStatus("RUNNING");
					if (!payloadText.isEmpty()) {
						entity.setRuntimeState(payloadText.length() > 30 ? payloadText.substring(0, 27) + "..." : payloadText);
					}
				}
			}
		}

		private String resolveActiveMode() {
			SessionContainer session = SessionManager.getInstance().getSession(sessionId);
			if (session instanceof SessionContext ctx && ctx.getTaskContext() != null) {
				Orchestrator orch = ctx.getTaskContext().getOrchestrator();
				if (orch != null && orch.getAiMode() != null) {
					AiMode aiMode = orch.getAiMode();
					if (AiMode.FORGE.equals(aiMode)) return "FORGE";
					if (AiMode.MEDIATED.equals(aiMode)) return "MEDIATED";
					if (AiMode.INTENT.equals(aiMode)) return "INTENT";
					if (AiMode.DREAM.equals(aiMode)) return "DREAM";
					if (AiMode.DEVELOPER.equals(aiMode)) return "DEVELOPER";
				}
			}
			return currentMode;
		}

		private void handleFlowCompleted(RuntimeEvent event) {
			for (GraphEntity entity : entities.values()) {
				if ("RUNNING".equals(entity.getStatus())) {
					entity.setStatus("DONE");
				}
			}
		}

		private void handleFlowPaused(RuntimeEvent event) {
			for (GraphEntity entity : entities.values()) {
				if ("RUNNING".equals(entity.getStatus())) {
					entity.setStatus("WAITING_USER");
					entity.setRuntimeState("PAUSED");
				}
			}
		}

		private void handleCognitiveStateChanged(RuntimeEvent event) {
			String stateStr = event.getPayload() != null ? event.getPayload().toString() : "";
			GraphEntity targetNode = entities.get("orchestrator");
			if (targetNode == null) targetNode = entities.get("darwin_engine");
			if (targetNode == null) targetNode = entities.get("supervisor");
			if (targetNode == null) targetNode = entities.get("forge_engine");
			if (targetNode == null) targetNode = entities.get("mediated_flow");
			if (targetNode != null && !stateStr.isEmpty()) {
				targetNode.setRuntimeState(stateStr.length() > 35 ? stateStr.substring(0, 32) + "..." : stateStr);
			}
		}

		private void handleReasoningEvent(RuntimeEvent event) {
			String msg = event.getPayload() != null ? event.getPayload().toString() : event.getType().name();
			GraphEntity llmNode = entities.get("local_llm");
			if (llmNode == null) llmNode = entities.get("remote_llm");
			if (llmNode == null) llmNode = entities.get("llm");
			if (llmNode != null) {
				llmNode.setStatus("RUNNING");
				llmNode.setRuntimeState(msg.length() > 30 ? msg.substring(0, 27) + "..." : msg);
			}
		}

		private void setupForgeTemplateIfNeeded() {
			if (!"FORGE".equals(currentMode) || !entities.containsKey("scanner")) {
				setupForgeTemplate();
			}
		}

		private void handleStepWaiting(RuntimeEvent event) {
			if (event.getPayload() == null) return;
			String stepId = event.getPayload().toString();
			SessionContainer session = SessionManager.getInstance().getSession(sessionId);
			if (session == null) return;
			WorkflowStep step = session.getWorkflowRegistry().getStep(stepId);
			if (step != null) {
				GraphEntity entity = entities.get(step.getEntityId());
				if (entity == null) {
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
			if (event.getPayload() == null) return;
			String stepId = event.getPayload().toString();
			SessionContainer session = SessionManager.getInstance().getSession(sessionId);
			if (session == null) return;
			WorkflowStep step = session.getWorkflowRegistry().getStep(stepId);
			if (step != null) {
				GraphEntity entity = entities.get(step.getEntityId());
				if (entity != null) {
					entity.setStatus("RUNNING");
					entity.getActions().clear();
					if (EntityType.SUPERVISOR.equals(entity.getType())) {
						entity.getActions().add("STOP_SUPERVISOR");
					}
				}
			}
		}

		private void handleTaskFailed(RuntimeEvent event) {
			if (event.getPayload() == null) return;
			String taskId = event.getPayload().toString();
			GraphEntity entity = entities.get(taskId);
			if (entity != null)
				entity.setStatus("FAILED");
		}

		private void handleSupervisorStatusChanged(RuntimeEvent event) {
			if (event.getPayload() == null) return;
			// If we are currently in FORGE mode, do not inject an arbitrary supervisor node into Forge pipeline
			if ("FORGE".equals(currentMode) && entities.containsKey("forge_engine")) {
				return;
			}

			GraphEntity supervisor = entities.get("supervisor");
			if (supervisor == null) {
				addEntity("supervisor", "Supervisor", EntityType.SUPERVISOR, "Autonomous self-development supervisor");
				supervisor = entities.get("supervisor");
				addLink("user", "supervisor", "trigger");
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
			if (event.getPayload() == null) return;
			String iterId = event.getPayload().toString();
			addEntity(iterId, "Iteration " + iterId.replace("iteration-", "#"), EntityType.EVOLUTION_LOOP, "Genetic evolution iteration loop");
			entities.get(iterId).setStatus("RUNNING");
			if (entities.containsKey("supervisor")) {
				addLink("supervisor", iterId, "iteration");
			} else if (entities.containsKey("darwin_engine")) {
				addLink("darwin_engine", iterId, "iteration");
			} else {
				addLink("user", iterId, "iteration");
			}
		}

		private void handleExportReady(RuntimeEvent event) {
			if (event.getPayload() == null) return;
			addEntity("export", "Export Bundle", EntityType.ZIP_EXPORT, "Ready deployment archive package");
			GraphEntity export = entities.get("export");
			export.setStatus("READY");
			export.getMetadata().put("path", event.getPayload().toString());
			export.getActions().add("OPEN_ZIP");
		}

		private void handleDeploymentStatusChanged(RuntimeEvent event) {
			if (event.getPayload() == null) return;
			String target = event.getMetadata().getOrDefault("target", "target").toString();
			addEntity(target, "Deploy: " + target, EntityType.DEPLOYMENT_TARGET, "Application runtime deployment target");
			entities.get(target).setStatus(event.getPayload().toString());
		}

		private void handleModeChanged(RuntimeEvent event) {
			if (event.getPayload() == null) return;
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
			if (event.getPayload() == null) return;
			String taskId = event.getPayload().toString();

			// If in FORGE mode, check if taskId matches an existing Forge stage
			if ("FORGE".equals(currentMode) && entities.containsKey("forge_engine")) {
				GraphEntity forgeStage = entities.get(taskId);
				if (forgeStage != null) {
					forgeStage.setStatus("RUNNING");
					return;
				}
				// Do not allow self-dev tasks to pollute an active Forge workflow
				return;
			}

			GraphEntity entity = entities.get(taskId);
			if (entity == null) {
				String label = resolveTaskLabel(taskId, event);
				entity = new GraphEntity(taskId, label, EntityType.SELF_DEV_TASK);
				if (event.getMetadata().containsKey("description")) {
					entity.setDescription(String.valueOf(event.getMetadata().get("description")));
				}
				if (event.getMetadata().containsKey("taskType")) {
					entity.setCategory(String.valueOf(event.getMetadata().get("taskType")));
				}
				entities.put(taskId, entity);

				// Connect to the appropriate parent execution entity
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

		private String resolveTaskLabel(String taskId, RuntimeEvent event) {
			if (event != null && event.getMetadata().containsKey("name")) {
				String name = String.valueOf(event.getMetadata().get("name"));
				if (name != null && !name.trim().isEmpty()) {
					return name;
				}
			}

			// Try resolving from session container
			try {
				SessionContainer session = SessionManager.getInstance().getSession(sessionId);
				if (session instanceof SessionContext ctx && ctx.getTaskContext() != null) {
					Orchestrator orch = ctx.getTaskContext().getOrchestrator();
					if (orch != null && orch.getSelfDevSession() != null) {
						for (Iteration it : orch.getSelfDevSession().getIterations()) {
							for (Task t : it.getTasks()) {
								if (taskId.equals(t.getId()) && t.getName() != null && !t.getName().trim().isEmpty()) {
									return t.getName();
								}
							}
						}
					}
				}
			} catch (Exception ignored) {}

			// Clean fallback for sd-task-...
			if (taskId.startsWith("sd-task-")) {
				int lastDash = taskId.lastIndexOf('-');
				if (lastDash > 0 && lastDash < taskId.length() - 1) {
					String idxStr = taskId.substring(lastDash + 1);
					try {
						int idx = Integer.parseInt(idxStr);
						return "Task #" + (idx + 1);
					} catch (NumberFormatException ignored) {}
				}
			}
			return taskId;
		}

		private void handleTaskCompleted(RuntimeEvent event) {
			if (event.getPayload() == null) return;
			String taskId = event.getPayload().toString();
			GraphEntity entity = entities.get(taskId);
			if (entity != null)
				entity.setStatus("DONE");
		}

		private void setupSelfDevTemplate() {
			currentMode = "SELF_DEV";
			entities.clear();
			links.clear();
			addEntity("user", "User", EntityType.USER, "Developer / Operator");
			addEntity("supervisor", "Supervisor", EntityType.SUPERVISOR, "Coordinates the autonomous self-development loop");
			addEntity("evolution_loop", "Evolution Loop", EntityType.EVOLUTION_LOOP, "Manages iterations, mutations, and verification");
			addLink("user", "supervisor", "trigger");
			addLink("supervisor", "evolution_loop", "manages");
		}

		private void setupMediatedTemplate() {
			currentMode = "MEDIATED";
			entities.clear();
			links.clear();
			addEntity("user", "User", EntityType.USER, "Developer / Operator");
			addEntity("mediated_flow", "Mediated Flow", EntityType.MEDIATED_FLOW, "Human-in-the-loop review and analysis workflow");
			addEntity("zip_export", "Export Bundle", EntityType.ZIP_EXPORT, "Unified export archive container");
			addEntity("workspace", "Workspace", EntityType.SELF_DEV_TASK, "Target source code and configuration files");

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
					String strat = v.optString("strategy", "Variant " + (i + 1));
					addEntity(vId, strat, EntityType.DARWIN_VARIANT, "Mutant code proposal: " + strat);
					GraphEntity entity = entities.get(vId);
					entity.setStatus("PENDING");
					entity.setRuntimeState(v.optString("strategy", ""));
					addLink(parentId, vId, "mutation");
				}
			}
		}

		private void setupDarwinTemplate() {
			currentMode = "DARWIN_MODE";
			entities.clear();
			links.clear();
			addEntity("user", "User", EntityType.USER, "Developer / Operator");
			addEntity("darwin_engine", "Darwin Engine", EntityType.SUPERVISOR, "Coordinates genetic search and mutation evaluation");
			addEntity("mutation_loop", "Mutation Loop", EntityType.EVOLUTION_LOOP, "Generates and tests code variants");
			addLink("user", "darwin_engine", "trigger");
			addLink("darwin_engine", "mutation_loop", "manages");
		}

		private void setupDefaultTemplate() {
			currentMode = "DEFAULT";
			entities.clear();
			links.clear();
			addEntity("user", "User", EntityType.USER, "Developer / Operator");
			addEntity("orchestrator", "Orchestrator", EntityType.SUPERVISOR, "Main reasoning and task dispatch orchestrator");
			addEntity("local_llm", "Local LLM", EntityType.LOCAL_LLM, "Offline private local model in Ollama");
			addEntity("remote_llm", "Remote LLM", EntityType.REMOTE_LLM, "Frontier API LLM model provider");

			addLink("user", "orchestrator", "trigger");
			addLink("orchestrator", "local_llm", "query");
			addLink("orchestrator", "remote_llm", "query");

			SessionContainer session = SessionManager.getInstance().getSession(sessionId);
			if (session instanceof SessionContext ctx && ctx.getTaskContext() != null && ctx.getTaskContext().getOrchestrator() != null) {
				Orchestrator orch = ctx.getTaskContext().getOrchestrator();
				if (AiMode.REMOTE.equals(orch.getAiMode())) {
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
		}

		private void setupAssistedCodingTemplate() {
			currentMode = "ASSISTED_CODING";
			entities.clear();
			links.clear();
			addEntity("user", "User", EntityType.USER, "Developer / Operator");
			addEntity("assisted_coding", "Coding Agent", EntityType.SUPERVISOR, "Assisted coding assistant and code generator");
			addEntity("llm", "Local LLM", EntityType.LOCAL_LLM, "Local inference model");
			addEntity("workspace", "Workspace", EntityType.SELF_DEV_TASK, "Target code repositories");
			addEntity("maven", "Maven Builder", EntityType.SELF_DEV_TASK, "Build and unit test executor");

			addLink("user", "assisted_coding", "trigger");
			addLink("assisted_coding", "llm", "query");
			addLink("assisted_coding", "workspace", "code");
			addLink("workspace", "maven", "build");
		}

		private void setupForgeTemplate() {
			currentMode = "FORGE";
			entities.clear();
			links.clear();

			addEntity("user", "User", EntityType.USER, "Developer / Operator");
			addEntity("forge_engine", "Forge Engine", EntityType.SUPERVISOR, "Authoritative LLM Forging Supervisor");
			addEntity("scanner", "Source Scanner", EntityType.SELF_DEV_TASK, "Codebase scanning and corpus discovery");
			addEntity("enhancer", "Dataset Enhancer", EntityType.SELF_DEV_TASK, "Dataset composition, tokenization, and vector synthesis");
			addEntity("validator", "Preflight Check", EntityType.SELF_DEV_TASK, "Hardware, dataset, and configuration preflight validation");
			addEntity("trainer", "LLM Trainer", EntityType.EVOLUTION_LOOP, "Causal Transformer backpropagation and loss optimization");
			addEntity("evaluator", "Model Evaluator", EntityType.SELF_DEV_TASK, "Convergence, quality metrics, and overfitting evaluation");
			addEntity("exporter", "GGUF Exporter", EntityType.ZIP_EXPORT, "Native .evo container serialization, quantization, and GGUF export");
			addEntity("registration", "Model Registration", EntityType.DEPLOYMENT_TARGET, "Ollama model registration and native inference smoke test");

			addLink("user", "forge_engine", "trigger");
			addLink("forge_engine", "scanner", "manages");
			addLink("scanner", "enhancer", "pipeline");
			addLink("enhancer", "validator", "pipeline");
			addLink("validator", "trainer", "pipeline");
			addLink("trainer", "evaluator", "pipeline");
			addLink("evaluator", "exporter", "pipeline");
			addLink("exporter", "registration", "pipeline");

			entities.get("forge_engine").setStatus("RUNNING");
			entities.get("scanner").setStatus("PENDING");
			entities.get("enhancer").setStatus("PENDING");
			entities.get("validator").setStatus("PENDING");
			entities.get("trainer").setStatus("PENDING");
			entities.get("evaluator").setStatus("PENDING");
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
