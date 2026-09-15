package eu.kalafatic.evolution.controller.orchestration.design;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deep architecture scanner for Git repositories.
 * Extracts a multi-level hierarchical codebase graph:
 * Level 1: Repository Root
 * Level 2: Modules / OSGi Bundles / Maven Projects
 * Level 3: Java Packages
 * Level 4: Java Classes / Interfaces / Enums / Records
 * Level 5: Methods / Fields / Members
 *
 * Captures explicit relationship types:
 * CONTAINS, IMPORT, TYPE_REFERENCE, METHOD_CALL, FIELD_REFERENCE, EXTENDS, IMPLEMENTS,
 * DEPENDENCY, OSGI_REQUIRE_BUNDLE, OSGI_IMPORT_PACKAGE, OSGI_EXPORT_PACKAGE, MAVEN_DEPENDENCY,
 * EXTENSION, EXTENSION_POINT, RESOURCE_REFERENCE.
 *
 * @evo:20:A reason=repository-codebase-graph-scanner
 */
public class RepoArchitectureScanner {

    private static final Set<String> IGNORE_DIRS = Set.of(".git", "target", "bin", "node_modules", ".settings", ".metadata", ".idea", "dist", "build");

    // Regex patterns for parsing Java code
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;", Pattern.MULTILINE);
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([a-zA-Z0-9_.*]+)\\s*;", Pattern.MULTILINE);
    private static final Pattern TYPE_HEADER_PATTERN = Pattern.compile("(?:public|protected|private|static|abstract|final|sealed|non-sealed|strictfp)*\\s*(class|interface|enum|record)\\s+([a-zA-Z0-9_]+)(?:<[^>]+>)?(?:\\s+extends\\s+([a-zA-Z0-9_.<>\\s]+))?(?:\\s+implements\\s+([a-zA-Z0-9_.,<>\\s]+))?");
    private static final Pattern METHOD_PATTERN = Pattern.compile("(?:public|protected|private|static|final|synchronized|abstract|default|native)*\\s*(?:<[^>]+>\\s+)?([a-zA-Z0-9_<>?ShS\\[\\]]+)\\s+([a-zA-Z0-9_]+)\\s*\\(([^)]*)\\)\\s*(?:throws\\s+[a-zA-Z0-9_.,\\s]+)?\\s*\\{");
    private static final Pattern FIELD_PATTERN = Pattern.compile("(?:private|protected|public)\\s+(?:static\\s+)?(?:final\\s+)?([a-zA-Z0-9_<>?S\\[\\]]+)\\s+([a-zA-Z0-9_]+)\\s*(?:=\\s*[^;]+)?;");
    private static final Pattern METHOD_CALL_PATTERN = Pattern.compile("([a-zA-Z0-9_]+)\\.([a-zA-Z0-9_]+)\\s*\\(");

    public DesignModel scanRepository(File repoRoot) {
        if (repoRoot == null || !repoRoot.exists() || !repoRoot.isDirectory()) {
            DesignModel empty = new DesignModel();
            empty.setName("Empty Repository");
            return empty;
        }

        DesignModel model = new DesignModel();
        model.setName(repoRoot.getName() + " Repository Architecture");

        Map<String, ComponentRecord> nodeMap = new HashMap<>();
        List<RelationshipRecord> relationships = new ArrayList<>();

        // Level 1: Repository Root Node
        ComponentRecord repoNode = new ComponentRecord();
        repoNode.setId("repo");
        repoNode.setName(repoRoot.getName());
        repoNode.setQualifiedName(repoRoot.getName());
        repoNode.setType("REPOSITORY");
        repoNode.setLevel(1);
        repoNode.setPath(".");
        repoNode.setDescription("Root Git Repository: " + repoRoot.getName());
        repoNode.setImportanceScore(1.0);
        nodeMap.put("repo", repoNode);
        model.getComponents().add(repoNode);

        // Map class simple names and qualified names for cross-linking
        Map<String, String> simpleToQualifiedMap = new HashMap<>();
        Map<String, Set<String>> packageClassesMap = new HashMap<>();

        // Phase 1 & 2: Discover Modules & Scan Source Trees
        List<File> moduleDirs = findModules(repoRoot, repoRoot);
        if (moduleDirs.isEmpty()) {
            moduleDirs.add(repoRoot); // Root as fallback module
        }

        for (File moduleDir : moduleDirs) {
            String relPath = getRelativePath(repoRoot, moduleDir);
            String moduleName = moduleDir.getName();
            String moduleId = "module:" + (relPath.isEmpty() ? moduleName : relPath);

            ComponentRecord moduleNode = new ComponentRecord();
            moduleNode.setId(moduleId);
            moduleNode.setName(moduleName);
            moduleNode.setQualifiedName(moduleName);
            moduleNode.setType(hasManifest(moduleDir) ? "BUNDLE" : (hasPom(moduleDir) ? "MAVEN_MODULE" : "MODULE"));
            moduleNode.setLevel(2);
            moduleNode.setParentId("repo");
            moduleNode.setPath(relPath);
            moduleNode.setDescription(moduleNode.getType() + ": " + moduleName);
            moduleNode.setImportanceScore(0.85);

            nodeMap.put(moduleId, moduleNode);
            model.getComponents().add(moduleNode);

            // Level 1 -> Level 2 Link
            addRelationship(relationships, "repo", moduleId, "CONTAINS", 1, "Module contained in repo");

            // Parse OSGi Manifest
            parseManifest(moduleDir, moduleId, relationships, moduleNode);

            // Parse pom.xml
            parsePom(moduleDir, moduleId, relationships, moduleNode);

            // Parse plugin.xml
            parsePluginXml(moduleDir, moduleId, relationships, moduleNode);

            // Discover Packages & Classes in this module
            scanModuleSources(moduleDir, repoRoot, moduleId, nodeMap, model, relationships, simpleToQualifiedMap, packageClassesMap);
        }

        // Phase 3: Post-process & Link References Across Classes & Packages
        resolveReferencesAndAggregations(nodeMap, relationships, simpleToQualifiedMap, packageClassesMap);

        // Compute incoming & outgoing reference counts
        computeReferenceCounts(nodeMap, relationships);

        model.setRelationships(relationships);
        return model;
    }

    private List<File> findModules(File current, File root) {
        List<File> modules = new ArrayList<>();
        if (!current.isDirectory()) return modules;

        if (!current.equals(root) && IGNORE_DIRS.contains(current.getName())) {
            return modules;
        }

        if (hasPom(current) || hasManifest(current) || hasPluginXml(current)) {
            modules.add(current);
        }

        File[] files = current.listFiles();
        if (files != null) {
            for (File child : files) {
                if (child.isDirectory()) {
                    modules.addAll(findModules(child, root));
                }
            }
        }
        return modules;
    }

    private boolean hasManifest(File dir) {
        return new File(dir, "META-INF/MANIFEST.MF").exists();
    }

    private boolean hasPom(File dir) {
        return new File(dir, "pom.xml").exists();
    }

    private boolean hasPluginXml(File dir) {
        return new File(dir, "plugin.xml").exists();
    }

    private void parseManifest(File moduleDir, String moduleId, List<RelationshipRecord> relationships, ComponentRecord moduleNode) {
        File manifestFile = new File(moduleDir, "META-INF/MANIFEST.MF");
        if (!manifestFile.exists()) return;

        try {
            List<String> lines = Files.readAllLines(manifestFile.toPath());
            StringBuilder content = new StringBuilder();
            for (String line : lines) {
                if (line.startsWith(" ")) {
                    content.append(line.trim());
                } else {
                    content.append("\n").append(line);
                }
            }

            String full = content.toString();
            for (String block : full.split("\n")) {
                if (block.startsWith("Bundle-SymbolicName:")) {
                    String name = block.substring("Bundle-SymbolicName:".length()).trim().split(";")[0];
                    if (!name.isEmpty()) {
                        moduleNode.setName(name);
                        moduleNode.setQualifiedName(name);
                    }
                } else if (block.startsWith("Require-Bundle:")) {
                    String deps = block.substring("Require-Bundle:".length()).trim();
                    for (String dep : deps.split(",")) {
                        String depName = dep.trim().split(";")[0].trim();
                        if (!depName.isEmpty()) {
                            addRelationship(relationships, moduleId, "module:" + depName, "OSGI_REQUIRE_BUNDLE", 1, "Require-Bundle: " + depName);
                            moduleNode.getDependencies().add("bundle:" + depName);
                        }
                    }
                } else if (block.startsWith("Import-Package:")) {
                    String imports = block.substring("Import-Package:".length()).trim();
                    for (String imp : imports.split(",")) {
                        String pkgName = imp.trim().split(";")[0].trim();
                        if (!pkgName.isEmpty()) {
                            addRelationship(relationships, moduleId, "pkg:" + pkgName, "OSGI_IMPORT_PACKAGE", 1, "Import-Package: " + pkgName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore malformed manifests
        }
    }

    private void parsePom(File moduleDir, String moduleId, List<RelationshipRecord> relationships, ComponentRecord moduleNode) {
        File pomFile = new File(moduleDir, "pom.xml");
        if (!pomFile.exists()) return;

        try {
            String content = Files.readString(pomFile.toPath());
            Matcher matcher = Pattern.compile("<artifactId>([a-zA-Z0-9._-]+)</artifactId>").matcher(content);
            Set<String> artifacts = new HashSet<>();
            while (matcher.find()) {
                artifacts.add(matcher.group(1));
            }
            for (String artifact : artifacts) {
                if (!artifact.equals(moduleNode.getName())) {
                    addRelationship(relationships, moduleId, "module:" + artifact, "MAVEN_DEPENDENCY", 1, "Maven dependency: " + artifact);
                }
            }
        } catch (Exception e) {}
    }

    private void parsePluginXml(File moduleDir, String moduleId, List<RelationshipRecord> relationships, ComponentRecord moduleNode) {
        File pluginXml = new File(moduleDir, "plugin.xml");
        if (!pluginXml.exists()) return;

        try {
            String content = Files.readString(pluginXml.toPath());
            Matcher epMatcher = Pattern.compile("<extension-point\\s+id=\"([^\"]+)\"").matcher(content);
            while (epMatcher.find()) {
                String epId = epMatcher.group(1);
                addRelationship(relationships, moduleId, "ep:" + epId, "EXTENSION_POINT", 1, "Declares Extension Point: " + epId);
            }

            Matcher extMatcher = Pattern.compile("<extension\\s+point=\"([^\"]+)\"").matcher(content);
            while (extMatcher.find()) {
                String point = extMatcher.group(1);
                addRelationship(relationships, moduleId, "ep:" + point, "EXTENSION", 1, "Extends Point: " + point);
            }
        } catch (Exception e) {}
    }

    private void scanModuleSources(File moduleDir, File repoRoot, String moduleId,
                                  Map<String, ComponentRecord> nodeMap, DesignModel model,
                                  List<RelationshipRecord> relationships,
                                  Map<String, String> simpleToQualifiedMap,
                                  Map<String, Set<String>> packageClassesMap) {

        List<File> javaFiles = findJavaFiles(moduleDir);
        Map<String, ComponentRecord> packageNodes = new HashMap<>();

        for (File javaFile : javaFiles) {
            try {
                String code = Files.readString(javaFile.toPath());
                String relPath = getRelativePath(repoRoot, javaFile);

                // Extract Package
                Matcher pkgMatcher = PACKAGE_PATTERN.matcher(code);
                String packageName = pkgMatcher.find() ? pkgMatcher.group(1) : "default";

                // Package Node
                String pkgId = "pkg:" + packageName;
                ComponentRecord pkgNode = packageNodes.get(pkgId);
                if (pkgNode == null) {
                    pkgNode = nodeMap.get(pkgId);
                }
                if (pkgNode == null) {
                    pkgNode = new ComponentRecord();
                    pkgNode.setId(pkgId);
                    pkgNode.setName(packageName);
                    pkgNode.setQualifiedName(packageName);
                    pkgNode.setType("PACKAGE");
                    pkgNode.setLevel(3);
                    pkgNode.setParentId(moduleId);
                    pkgNode.setPath(getRelativePath(repoRoot, javaFile.getParentFile()));
                    pkgNode.setDescription("Java Package: " + packageName);
                    pkgNode.setImportanceScore(0.70);

                    packageNodes.put(pkgId, pkgNode);
                    nodeMap.put(pkgId, pkgNode);
                    model.getComponents().add(pkgNode);

                    // Module -> Package Link
                    addRelationship(relationships, moduleId, pkgId, "CONTAINS", 1, "Module contains package " + packageName);
                }

                // Extract Type Declaration (Class, Interface, Enum, Record)
                Matcher typeMatcher = TYPE_HEADER_PATTERN.matcher(code);
                if (typeMatcher.find()) {
                    String kind = typeMatcher.group(1).toUpperCase(); // CLASS, INTERFACE, ENUM, RECORD
                    String simpleName = typeMatcher.group(2);
                    String superClass = typeMatcher.group(3);
                    String interfacesStr = typeMatcher.group(4);

                    String qualifiedName = packageName + "." + simpleName;
                    String classId = "class:" + qualifiedName;

                    simpleToQualifiedMap.put(simpleName, qualifiedName);
                    packageClassesMap.computeIfAbsent(packageName, k -> new HashSet<>()).add(qualifiedName);

                    ComponentRecord classNode = new ComponentRecord();
                    classNode.setId(classId);
                    classNode.setName(simpleName);
                    classNode.setQualifiedName(qualifiedName);
                    classNode.setType(kind);
                    classNode.setLevel(4);
                    classNode.setParentId(pkgId);
                    classNode.setPath(relPath);
                    classNode.setSuperClass(superClass != null ? superClass.trim() : "");
                    classNode.setDescription(kind + " " + qualifiedName);
                    classNode.setImportanceScore(0.60);

                    if (interfacesStr != null) {
                        for (String iface : interfacesStr.split(",")) {
                            String trimmed = iface.trim().split("<")[0];
                            if (!trimmed.isEmpty()) {
                                classNode.getInterfaces().add(trimmed);
                            }
                        }
                    }

                    // Package -> Class Link
                    addRelationship(relationships, pkgId, classId, "CONTAINS", 1, "Package contains " + simpleName);

                    // Extract Imports
                    Matcher importMatcher = IMPORT_PATTERN.matcher(code);
                    while (importMatcher.find()) {
                        String imported = importMatcher.group(1);
                        addRelationship(relationships, classId, "class:" + imported, "IMPORT", 1, "Imports " + imported);
                    }

                    // Extract Fields
                    Matcher fieldMatcher = FIELD_PATTERN.matcher(code);
                    while (fieldMatcher.find()) {
                        String fType = fieldMatcher.group(1);
                        String fName = fieldMatcher.group(2);
                        classNode.getFields().add(fType + " " + fName);

                        // Level 5 Member Node for Field
                        String fieldMemberId = "member:" + qualifiedName + "#" + fName;
                        ComponentRecord fieldMember = new ComponentRecord();
                        fieldMember.setId(fieldMemberId);
                        fieldMember.setName(fName);
                        fieldMember.setQualifiedName(qualifiedName + "." + fName);
                        fieldMember.setType("FIELD");
                        fieldMember.setLevel(5);
                        fieldMember.setParentId(classId);
                        fieldMember.setPath(relPath);
                        fieldMember.setDescription("Field " + fType + " " + fName + " in " + simpleName);
                        nodeMap.put(fieldMemberId, fieldMember);
                        model.getComponents().add(fieldMember);

                        addRelationship(relationships, classId, fieldMemberId, "CONTAINS", 1, "Contains field " + fName);
                        addRelationship(relationships, fieldMemberId, "class:" + fType, "FIELD_REFERENCE", 1, "Type " + fType);
                    }

                    // Extract Methods
                    Matcher methodMatcher = METHOD_PATTERN.matcher(code);
                    while (methodMatcher.find()) {
                        String retType = methodMatcher.group(1);
                        String mName = methodMatcher.group(2);
                        String params = methodMatcher.group(3);

                        String methodSig = retType + " " + mName + "(" + params + ")";
                        classNode.getMethods().add(methodSig);

                        // Level 5 Member Node for Method
                        String methodMemberId = "member:" + qualifiedName + "#" + mName + "()";
                        ComponentRecord methodMember = new ComponentRecord();
                        methodMember.setId(methodMemberId);
                        methodMember.setName(mName + "()");
                        methodMember.setQualifiedName(qualifiedName + "." + mName + "()");
                        methodMember.setType("METHOD");
                        methodMember.setLevel(5);
                        methodMember.setParentId(classId);
                        methodMember.setPath(relPath);
                        methodMember.setDescription("Method " + methodSig + " in " + simpleName);
                        nodeMap.put(methodMemberId, methodMember);
                        model.getComponents().add(methodMember);

                        addRelationship(relationships, classId, methodMemberId, "CONTAINS", 1, "Contains method " + mName);
                    }

                    // Method Calls inside code body
                    Matcher callMatcher = METHOD_CALL_PATTERN.matcher(code);
                    Set<String> callsSeen = new HashSet<>();
                    while (callMatcher.find()) {
                        String targetObj = callMatcher.group(1);
                        String targetMethod = callMatcher.group(2);
                        String key = targetObj + "." + targetMethod;
                        if (!callsSeen.contains(key) && !targetObj.equals("this") && !targetObj.equals("super")) {
                            callsSeen.add(key);
                            addRelationship(relationships, classId, "class:" + targetObj, "METHOD_CALL", 1, "Invokes " + targetObj + "." + targetMethod + "()");
                        }
                    }

                    nodeMap.put(classId, classNode);
                    model.getComponents().add(classNode);
                }

            } catch (Exception e) {
                // Ignore unparseable files gracefully
            }
        }
    }

    private List<File> findJavaFiles(File dir) {
        List<File> javaFiles = new ArrayList<>();
        if (!dir.isDirectory()) return javaFiles;

        if (IGNORE_DIRS.contains(dir.getName())) return javaFiles;

        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    javaFiles.addAll(findJavaFiles(f));
                } else if (f.getName().endsWith(".java")) {
                    javaFiles.add(f);
                }
            }
        }
        return javaFiles;
    }

    private void resolveReferencesAndAggregations(Map<String, ComponentRecord> nodeMap,
                                                 List<RelationshipRecord> relationships,
                                                 Map<String, String> simpleToQualifiedMap,
                                                 Map<String, Set<String>> packageClassesMap) {

        for (ComponentRecord node : nodeMap.values()) {
            if (node.getLevel() == 4) { // Class level
                // Superclass link
                if (node.getSuperClass() != null && !node.getSuperClass().isEmpty()) {
                    String targetQual = simpleToQualifiedMap.getOrDefault(node.getSuperClass(), node.getSuperClass());
                    addRelationship(relationships, node.getId(), "class:" + targetQual, "EXTENDS", 1, "Extends " + targetQual);
                }

                // Interfaces link
                for (String iface : node.getInterfaces()) {
                    String targetQual = simpleToQualifiedMap.getOrDefault(iface, iface);
                    addRelationship(relationships, node.getId(), "class:" + targetQual, "IMPLEMENTS", 1, "Implements " + targetQual);
                }
            }
        }
    }

    private void computeReferenceCounts(Map<String, ComponentRecord> nodeMap, List<RelationshipRecord> relationships) {
        for (RelationshipRecord rel : relationships) {
            ComponentRecord fromNode = nodeMap.get(rel.getFrom());
            if (fromNode != null) {
                fromNode.setOutgoingCount(fromNode.getOutgoingCount() + rel.getCount());
            }

            ComponentRecord toNode = nodeMap.get(rel.getTo());
            if (toNode != null) {
                toNode.setIncomingCount(toNode.getIncomingCount() + rel.getCount());
            }
        }
    }

    private void addRelationship(List<RelationshipRecord> relationships, String from, String to, String type, int count, String detail) {
        if (from == null || to == null || from.equals(to)) return;

        for (RelationshipRecord rel : relationships) {
            if (rel.getFrom().equals(from) && rel.getTo().equals(to) && rel.getType().equals(type)) {
                rel.setCount(rel.getCount() + count);
                if (detail != null && !rel.getDetails().contains(detail)) {
                    rel.getDetails().add(detail);
                }
                return;
            }
        }

        RelationshipRecord newRel = new RelationshipRecord();
        newRel.setFrom(from);
        newRel.setTo(to);
        newRel.setType(type);
        newRel.setCount(count);
        if (detail != null) newRel.getDetails().add(detail);
        relationships.add(newRel);
    }

    private String getRelativePath(File root, File file) {
        try {
            return root.toPath().toAbsolutePath().relativize(file.toPath().toAbsolutePath()).toString().replace('\\', '/');
        } catch (Exception e) {
            return file.getName();
        }
    }
}
