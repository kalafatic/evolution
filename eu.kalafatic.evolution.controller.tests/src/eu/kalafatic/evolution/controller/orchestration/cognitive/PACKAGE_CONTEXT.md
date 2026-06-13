# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.controller.tests/src/eu/kalafatic/evolution/controller/orchestration/cognitive/

## Domain: general

## Components
* `CognitiveStateEngineTest.java`: package eu.kalafatic.evolution.controller.orchestration.cognitive; import static org.junit.Assert.*; import org.junit.Before; import org.junit.Test; import eu.kalafatic.evolution.controller.orchestration.ContextAssistResult; import eu.kalafatic.evolution.controller.orchestration.PlatformType; import eu.kalafatic.evolution.controller.orchestration.PlatformMode; import eu.kalafatic.evolution.controller.orchestration.AutonomyLevel; public class CognitiveStateEngineTest { private CognitiveStateEngine engine; private SessionCognitiveState state; @Before public void setUp() { engine = new CognitiveStateEngine(); state = new SessionCognitiveState(); } @Test public void testChatToCodeTransition() { assertEquals(CapabilityType.CHAT, state.getCurrentCapability()); engine.processInteraction("create a new java class", state, null);
