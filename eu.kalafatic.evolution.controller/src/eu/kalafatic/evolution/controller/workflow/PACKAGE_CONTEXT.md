# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/workflow/

## Domain: general

## Components
* `DeploymentManager.java`: package eu.kalafatic.evolution.controller.workflow; import eu.kalafatic.evolution.controller.orchestration.TaskContext; public class DeploymentManager {
* `WorkflowGraphManager.java`: package eu.kalafatic.evolution.controller.workflow; import java.util.Map; import java.util.concurrent.ConcurrentHashMap;
* `WorkflowStep.java`: package eu.kalafatic.evolution.controller.workflow; import java.util.ArrayList; import java.util.List;
* `RuntimeEventType.java`: package eu.kalafatic.evolution.controller.workflow; import eu.kalafatic.evolution.controller.trajectory.EventCategory; public enum RuntimeEventType {
* `GraphActionExecutor.java`: package eu.kalafatic.evolution.controller.workflow; import eu.kalafatic.evolution.controller.orchestration.KernelFacade; import eu.kalafatic.evolution.controller.orchestration.TaskContext;
* `RuntimeEvent.java`: package eu.kalafatic.evolution.controller.workflow; import java.util.Map; import java.util.HashMap;
* `WorkflowStepRegistry.java`: package eu.kalafatic.evolution.controller.workflow; import java.util.Map; import java.util.concurrent.ConcurrentHashMap;
* `SupervisorManager.java`: package eu.kalafatic.evolution.controller.workflow; import eu.kalafatic.evolution.controller.orchestration.TaskContext; import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevBootstrapController;
* `WorkflowStatus.java`: package eu.kalafatic.evolution.controller.workflow; public enum WorkflowStatus { PENDING,
* `StepModeController.java`: package eu.kalafatic.evolution.controller.workflow; import java.util.Map; import java.util.concurrent.ConcurrentHashMap;
* `GraphEntity.java`: package eu.kalafatic.evolution.controller.workflow; import java.util.ArrayList; import java.util.List;
* `EntityType.java`: package eu.kalafatic.evolution.controller.workflow; public enum EntityType { USER,
* `RuntimeEventListener.java`: package eu.kalafatic.evolution.controller.workflow; public interface RuntimeEventListener { void onEvent(RuntimeEvent event);
* `PACKAGE_CONTEXT.md`: 
* `RuntimeEventBus.java`: package eu.kalafatic.evolution.controller.workflow; import java.util.ArrayList; import java.util.List;
* `MediatedExportManager.java`: package eu.kalafatic.evolution.controller.workflow; import java.io.*; import java.nio.charset.StandardCharsets;
