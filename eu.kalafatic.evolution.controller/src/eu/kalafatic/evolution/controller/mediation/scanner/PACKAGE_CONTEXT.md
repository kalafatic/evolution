# PACKAGE CONTEXT

## Directory: git/evolution-240526-ok/eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/mediation/scanner/

## Domain: general

## Components
* `TargetScanner.java`: package eu.kalafatic.evolution.controller.mediation.scanner; import java.io.File; import java.util.Set; import java.util.HashSet; import eu.kalafatic.evolution.controller.mediation.model.FileDescriptor; import eu.kalafatic.evolution.controller.mediation.model.SemanticNode; import eu.kalafatic.evolution.controller.mediation.model.TargetDescriptor; import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot; import java.util.ArrayList; public class TargetScanner { private static final Set<String> IGNORE_DIRS = Set.of(".git", "node_modules", "target", "build", ".settings", ".metadata"); public TargetDescriptor scan(File root) { TargetDescriptor target = new TargetDescriptor(root.getAbsolutePath()); scanRecursive(root, root, target); detectTechnologies(target); return target; } public TargetSnapshot scanToSnapshot(File root, TargetSnapshot.TargetType type) { String id = "snapshot-" + System.currentTimeMillis(); TargetSnapshot snapshot = new TargetSnapshot(id, "1.0", type, root.getAbsolutePath());
