# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/application/

## Domain: general

## Components
* `HeadlessApplication.java`: package eu.kalafatic.evolution.controller.application; import org.eclipse.equinox.app.IApplication; import org.eclipse.equinox.app.IApplicationContext; import eu.kalafatic.evolution.controller.orchestration.ServerManager; public class HeadlessApplication implements IApplication { @Override public Object start(IApplicationContext context) throws Exception { String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS); int port = 48080; for (int i = 0; i < args.length; i++) { if ("--port".equals(args[i]) && i + 1 < args.length) { port = Integer.parseInt(args[++i]); } } System.out.println("Evolution Headless Server starting on port " + port + "..."); ServerManager.getInstance().start(port); System.out.println("Evolution Headless Server started. Press Ctrl+C to stop."); synchronized (this) { while (true) { try {
