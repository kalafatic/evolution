# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.model/src/eu/kalafatic/evolution/model/orchestration/validation/

## Domain: general

## Components
* `FileChangeValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; import eu.kalafatic.evolution.model.orchestration.DiffHunk; import org.eclipse.emf.common.util.EList;
* `ChangeSetValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; import eu.kalafatic.evolution.model.orchestration.FileChange; import org.eclipse.emf.common.util.EList;
* `GitValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; public interface GitValidator { boolean validate();
* `CommentValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; public interface CommentValidator { boolean validate();
* `CommandValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; import eu.kalafatic.evolution.model.orchestration.CommandStatus; public interface CommandValidator {
* `AccessRuleValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; import org.eclipse.emf.common.util.EList; public interface AccessRuleValidator {
* `OllamaValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; public interface OllamaValidator { boolean validate();
* `EclipseValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; public interface EclipseValidator { boolean validate();
* `ServerSessionValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; import eu.kalafatic.evolution.model.orchestration.SessionType; public interface ServerSessionValidator {
* `MemoryRuleValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; public interface MemoryRuleValidator { boolean validate();
* `DiffHunkValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; import org.eclipse.emf.common.util.EList; public interface DiffHunkValidator {
* `TestValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; import eu.kalafatic.evolution.model.orchestration.TestStatus; public interface TestValidator {
* `TaskValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; import eu.kalafatic.evolution.model.orchestration.FeedbackLevel; import eu.kalafatic.evolution.model.orchestration.LogLevel;
* `AgentValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; import eu.kalafatic.evolution.model.orchestration.ExecutionMode; import eu.kalafatic.evolution.model.orchestration.Rule;
* `AIProviderValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; public interface AIProviderValidator { boolean validate();
* `EvoProjectValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; import eu.kalafatic.evolution.model.orchestration.Orchestrator; import org.eclipse.emf.common.util.EList;
* `NeuronAIValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; import eu.kalafatic.evolution.model.orchestration.NeuronType; public interface NeuronAIValidator {
* `SecretRuleValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; import org.eclipse.emf.common.util.EList; public interface SecretRuleValidator {
* `OrchestratorValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; import eu.kalafatic.evolution.model.orchestration.AIProvider; import eu.kalafatic.evolution.model.orchestration.Agent;
* `ServerSettingsValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; public interface ServerSettingsValidator { boolean validate();
* `LLMValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; public interface LLMValidator { boolean validate();
* `RuleValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; public interface RuleValidator { boolean validate();
* `DatabaseValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; public interface DatabaseValidator { boolean validate();
* `NetworkRuleValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; import org.eclipse.emf.common.util.EList; public interface NetworkRuleValidator {
* `CompilerValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; public interface CompilerValidator { boolean validate();
* `ChatSessionValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; import eu.kalafatic.evolution.model.orchestration.ChatMessage; import org.eclipse.emf.common.util.EList;
* `SelfDevSessionValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; import eu.kalafatic.evolution.model.orchestration.Iteration; import eu.kalafatic.evolution.model.orchestration.SelfDevStatus;
* `EvaluationResultValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; import eu.kalafatic.evolution.model.orchestration.SelfDevDecision; import org.eclipse.emf.common.util.EList;
* `MavenValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; import org.eclipse.emf.common.util.EList; public interface MavenValidator {
* `IterationValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; import eu.kalafatic.evolution.model.orchestration.EvaluationResult; import eu.kalafatic.evolution.model.orchestration.IterationStatus;
* `MonitoringDataValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; public interface MonitoringDataValidator { boolean validate();
* `PACKAGE_CONTEXT.md`: 
* `FileConfigValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; public interface FileConfigValidator { boolean validate();
* `PromptInstructionsValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; public interface PromptInstructionsValidator { boolean validate();
* `AiChatValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; import eu.kalafatic.evolution.model.orchestration.ChatSession; import eu.kalafatic.evolution.model.orchestration.PromptInstructions;
* `ReviewSessionValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; import eu.kalafatic.evolution.model.orchestration.ChangeSet; import eu.kalafatic.evolution.model.orchestration.Comment;
* `ChatMessageValidator.java`: package eu.kalafatic.evolution.model.orchestration.validation; public interface ChatMessageValidator { boolean validate();
