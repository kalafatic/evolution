# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/behavior/

## Domain: general

## Components
* `BehaviorResolver.java`: package eu.kalafatic.evolution.controller.orchestration.behavior; import eu.kalafatic.evolution.controller.orchestration.PlatformMode; import eu.kalafatic.evolution.controller.orchestration.PlatformType;
* `MediatedInstructionModule.java`: package eu.kalafatic.evolution.controller.orchestration.behavior; import java.util.stream.Collectors; public class MediatedInstructionModule implements InstructionModule {
* `BehaviorProfile.java`: package eu.kalafatic.evolution.controller.orchestration.behavior; import java.util.HashSet; import java.util.Set;
* `PolicyResolver.java`: package eu.kalafatic.evolution.controller.orchestration.behavior; import eu.kalafatic.evolution.controller.orchestration.behavior.ExecutionPolicy.*; public class PolicyResolver {
* `ConservativeReasoningModule.java`: package eu.kalafatic.evolution.controller.orchestration.behavior; import java.util.stream.Collectors; public class ConservativeReasoningModule implements InstructionModule {
* `ExploratoryReasoningModule.java`: package eu.kalafatic.evolution.controller.orchestration.behavior; import java.util.stream.Collectors; public class ExploratoryReasoningModule implements InstructionModule {
* `ExecutionPolicy.java`: package eu.kalafatic.evolution.controller.orchestration.behavior; import java.util.ArrayList; import java.util.List;
* `RuleRegistry.java`: package eu.kalafatic.evolution.controller.orchestration.behavior; import java.util.ArrayList; import java.util.List;
* `PromptComposer.java`: package eu.kalafatic.evolution.controller.orchestration.behavior; import java.util.List; import java.util.stream.Collectors;
* `BehaviorTrait.java`: package eu.kalafatic.evolution.controller.orchestration.behavior; public enum BehaviorTrait { SUPERVISION_MEDIATED,
* `PolicyRule.java`: package eu.kalafatic.evolution.controller.orchestration.behavior; @FunctionalInterface public interface PolicyRule {
* `BitState.java`: package eu.kalafatic.evolution.controller.orchestration.behavior; public class BitState { private static final int MODE_SHIFT = 0;
* `PACKAGE_CONTEXT.md`: 
* `InstructionModule.java`: package eu.kalafatic.evolution.controller.orchestration.behavior; public interface InstructionModule { String getInstructions(ExecutionPolicy policy);
* `SelfDevInstructionModule.java`: package eu.kalafatic.evolution.controller.orchestration.behavior; public class SelfDevInstructionModule implements InstructionModule { @Override
* `StepModeInstructionModule.java`: package eu.kalafatic.evolution.controller.orchestration.behavior; import java.util.stream.Collectors; public class StepModeInstructionModule implements InstructionModule {
* `DarwinIterativeInstructionModule.java`: package eu.kalafatic.evolution.controller.orchestration.behavior; import java.util.stream.Collectors; public class DarwinIterativeInstructionModule implements InstructionModule {
