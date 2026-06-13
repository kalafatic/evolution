# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.controller.tests/src/eu/kalafatic/evolution/controller/orchestration/isolation/

## Domain: general

## Components
* `SessionIsolationStressTest.java`: package eu.kalafatic.evolution.controller.orchestration.isolation; import static org.junit.Assert.*; import org.junit.Test; import java.util.ArrayList; import java.util.List; import java.util.concurrent.*; import eu.kalafatic.evolution.controller.orchestration.*; import eu.kalafatic.evolution.controller.workflow.*; public class SessionIsolationStressTest { @Test public void testParallelSessionIsolation() throws Exception { int sessionCount = 10; ExecutorService executor = Executors.newFixedThreadPool(sessionCount); List<Future<Integer>> results = new ArrayList<>(); final List<RuntimeEvent> allReceivedEvents = new CopyOnWriteArrayList<>(); for (int i = 0; i < sessionCount; i++) { final String sessionId = "session-" + i; results.add(executor.submit(() -> { SessionContainer container = SessionManager.getInstance().getOrCreateSession(sessionId); final List<RuntimeEvent> sessionEvents = new ArrayList<>();
