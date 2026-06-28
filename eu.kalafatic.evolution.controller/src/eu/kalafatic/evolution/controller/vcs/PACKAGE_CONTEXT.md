# PACKAGE CONTEXT

## Directory: git/evolution-240526-ok/eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/vcs/

## Domain: general

## Components
* `GitVersionControlProvider.java`: package eu.kalafatic.evolution.controller.vcs; import java.io.File; import java.util.ArrayList; import java.util.Arrays; import java.util.List; import eu.kalafatic.evolution.controller.tools.ShellTool; public class GitVersionControlProvider implements VersionControlProvider { private ShellTool shell = new ShellTool(); @Override public List<String> fetchCommits(File workingDir) throws Exception { String output = null; try { output = shell.execute("git log --oneline -n 20", workingDir, null); } catch (Exception e) { return new ArrayList<>(); } if (output == null || output.isEmpty()) return new ArrayList<>(); return Arrays.asList(output.split("\n")); } @Override
* `VersionControlProvider.java`: package eu.kalafatic.evolution.controller.vcs; import java.io.File; import java.util.List; public interface VersionControlProvider { List<String> fetchCommits(File workingDir) throws Exception; String getDiff(File workingDir, String commitId) throws Exception; String getFileDiff(File workingDir, String commitId, String filePath) throws Exception; List<String> getChangedFiles(File workingDir, String commitId) throws Exception; String getFileContent(File workingDir, String commitId, String filePath) throws Exception; void checkoutBranch(File workingDir, String branchName) throws Exception; void commitChanges(File workingDir, String message) throws Exception; void push(File workingDir) throws Exception; void revertFile(File workingDir, String filePath) throws Exception; }
