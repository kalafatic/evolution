package eu.kalafatic.evolution.controller.tools;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import eu.kalafatic.evolution.controller.orchestration.FileChangeTracker;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Tool for file operations within the orchestration system.
 *
 * @evo.lastModified: 12:A
 * @evo.origin: self
 */
public class FileTool implements ITool {
    @Override
    public String getName() {
        return "FileTool";
    }

    @Override
    // @evo:12:A reason=traceability-support
    public String execute(String command, File workingDir, TaskContext context) throws Exception {
        // command format expected: "WRITE path/to/file\n[CONTENT]"
        if (command.startsWith("WRITE")) {
            int newlineIndex = command.indexOf("\n");
            if (newlineIndex == -1) {
                throw new Exception("Malformed WRITE command for FileTool: No content separator.");
            }
            String pathPart = command.substring(5, newlineIndex).trim();
            String contentPart = command.substring(newlineIndex + 1);

            context.log("Tool [FileTool]: Writing to " + pathPart);

            if (pathPart.contains("..") || pathPart.startsWith("/") || pathPart.contains(":")) {
                throw new Exception("Security Violation: Path traversal attempt or absolute path detected: " + pathPart);
            }

            try {
                IFile iFile = getIFile(workingDir, pathPart);
                if (iFile != null) {
                    prepareContainer(iFile.getParent());
                    ByteArrayInputStream source = new ByteArrayInputStream(contentPart.getBytes());
                    if (iFile.exists()) {
                        iFile.setContents(source, IResource.FORCE, null);
                        context.getFileChangeTracker().recordChange(pathPart, FileChangeTracker.ChangeType.EDITED);
                    } else {
                        iFile.create(source, IResource.FORCE, null);
                        context.getFileChangeTracker().recordChange(pathPart, FileChangeTracker.ChangeType.NEW);
                    }
                    context.log("Tool [FileTool]: Successfully wrote " + contentPart.length() + " bytes to " + pathPart + " via IFile API");
                } else {
                    // Fallback to java.io.File if not in workspace (should rarely happen in Evo)
                    File file = new File(workingDir, pathPart);
                    file.getParentFile().mkdirs();
                    try (FileWriter writer = new FileWriter(file)) {
                        writer.write(contentPart);
                    }
                    context.log("Tool [FileTool]: Wrote " + contentPart.length() + " bytes to " + pathPart + " via java.io.File");
                }
            } catch (CoreException e) {
                throw new Exception("Failed to write file via IFile API: " + pathPart + " - " + e.getMessage(), e);
            }

            context.log("Tool [FileTool]: Wrote file " + pathPart);
            return "SUCCESS: Wrote file " + pathPart;
        } else if (command.startsWith("READ")) {
            String pathPart = command.substring(4).trim();
            context.log("Tool [FileTool]: Reading from " + pathPart);
            if (pathPart.contains("..") || pathPart.startsWith("/") || pathPart.contains(":")) {
                throw new Exception("Security Violation: Path traversal attempt or absolute path detected: " + pathPart);
            }
            File file = new File(workingDir, pathPart);
            if (!file.exists()) {
                throw new Exception("File not found: " + pathPart);
            }
            // Simple read logic
            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            String content = new String(bytes);
            context.log("Tool [FileTool]: Read " + content.length() + " bytes from " + pathPart);
            return content;
        } else if (command.startsWith("DELETE")) {
            String pathPart = command.substring(6).trim();
            context.log("Tool [FileTool]: Deleting " + pathPart);
            if (pathPart.contains("..") || pathPart.startsWith("/") || pathPart.contains(":")) {
                throw new Exception("Security Violation: Path traversal attempt or absolute path detected: " + pathPart);
            }

            try {
                IFile iFile = getIFile(workingDir, pathPart);
                if (iFile != null && iFile.exists()) {
                    iFile.delete(IResource.FORCE, null);
                    context.getFileChangeTracker().recordChange(pathPart, FileChangeTracker.ChangeType.REMOVED);
                    context.log("Tool [FileTool]: Deleted " + pathPart + " via IFile API");
                    return "SUCCESS: Deleted " + pathPart;
                }
            } catch (CoreException e) {
                // Ignore and try java.io.File
            }

            File file = new File(workingDir, pathPart);
            if (file.exists()) {
                if (deleteRecursively(file)) {
                    context.log("Tool [FileTool]: Deleted " + pathPart + " via java.io.File");
                    return "SUCCESS: Deleted " + pathPart;
                } else {
                    throw new Exception("Failed to delete " + pathPart);
                }
            }
            return "SUCCESS: File did not exist " + pathPart;
        } else if (command.startsWith("MKDIR")) {
            String pathPart = command.substring(5).trim();
            context.log("Tool [FileTool]: Creating directory " + pathPart);
            if (pathPart.contains("..") || pathPart.startsWith("/") || pathPart.contains(":")) {
                throw new Exception("Security Violation: Path traversal attempt or absolute path detected: " + pathPart);
            }

            try {
                IFolder iFolder = getIFolder(workingDir, pathPart);
                if (iFolder != null) {
                    if (iFolder.exists()) {
                        return "SUCCESS: Directory already exists " + pathPart;
                    }
                    prepareContainer(iFolder);
                    context.log("Tool [FileTool]: Created directory " + pathPart + " via IFolder API");
                    return "SUCCESS: Created directory " + pathPart;
                }
            } catch (CoreException e) {
                // Fallback to java.io.File
            }

            File dir = new File(workingDir, pathPart);
            if (dir.exists()) {
                return "SUCCESS: Directory already exists " + pathPart;
            }
            if (dir.mkdirs()) {
                context.log("Tool [FileTool]: Created directory " + pathPart + " via java.io.File");
                return "SUCCESS: Created directory " + pathPart;
            } else {
                throw new Exception("Failed to create directory " + pathPart);
            }
        }
        throw new Exception("Unsupported command for FileTool: " + command);
    }

    private IFile getIFile(File workingDir, String path) {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        Path p = new Path(new File(workingDir, path).getAbsolutePath());
        return root.getFileForLocation(p);
    }

    private IFolder getIFolder(File workingDir, String path) {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        Path p = new Path(new File(workingDir, path).getAbsolutePath());
        return root.getContainerForLocation(p) instanceof IFolder ? (IFolder) root.getContainerForLocation(p) : null;
    }

    private void prepareContainer(IResource resource) throws CoreException {
        if (resource instanceof IFolder && !resource.exists()) {
            prepareContainer(resource.getParent());
            ((IFolder) resource).create(true, true, null);
        }
    }

    private boolean deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteRecursively(f);
                }
            }
        }
        return file.delete();
    }
}
