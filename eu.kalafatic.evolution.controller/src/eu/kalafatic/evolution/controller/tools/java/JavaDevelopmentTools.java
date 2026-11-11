package eu.kalafatic.evolution.controller.tools.java;

import java.io.File;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.tools.ITool;

/**
 * Suite of 9 Eclipse/Java development tools for Darwin Developer mode.
 * Tools:
 *  1. java.inspect_class
 *  2. java.inspect_method
 *  3. java.find_references
 *  4. java.find_callers
 *  5. java.find_implementations
 *  6. java.get_diagnostics
 *  7. java.get_project_structure
 *  8. java.get_dependencies
 *  9. java.format
 *
 * Utilizes Eclipse workspace services when available, with standard Java compiler and AST/model
 * analysis fallbacks for external folders and projects.
 */
public final class JavaDevelopmentTools {

    private JavaDevelopmentTools() {}

    /**
     * Resolves an argument value from JSON string or key-value command format.
     */
    public static JSONObject parseArgs(String command) {
        if (command == null || command.trim().isEmpty()) {
            return new JSONObject();
        }
        String trimmed = command.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                return new JSONObject(trimmed);
            } catch (Exception ignored) {}
        }
        // Fallback: key=value or positional argument
        JSONObject json = new JSONObject();
        if (trimmed.contains("=")) {
            String[] parts = trimmed.split("\\s+");
            for (String p : parts) {
                int eq = p.indexOf('=');
                if (eq > 0) {
                    json.put(p.substring(0, eq), p.substring(eq + 1));
                }
            }
        } else {
            // Positional primary arg
            json.put("target", trimmed);
            json.put("className", trimmed);
            json.put("filePath", trimmed);
        }
        return json;
    }

    /**
     * Helper to find a Java source file by class name or relative path.
     */
    public static File findSourceFile(File root, String target) {
        if (root == null || !root.exists() || target == null || target.trim().isEmpty()) {
            return null;
        }
        String cleanTarget = target.trim();
        File direct = new File(root, cleanTarget);
        if (direct.exists() && direct.isFile()) return direct;

        File directAbs = new File(cleanTarget);
        if (directAbs.isAbsolute() && directAbs.exists() && directAbs.isFile()) return directAbs;

        String simpleName = cleanTarget;
        if (simpleName.endsWith(".java")) {
            simpleName = simpleName.substring(0, simpleName.length() - 5);
        }
        if (simpleName.contains(".")) {
            simpleName = simpleName.substring(simpleName.lastIndexOf('.') + 1);
        }
        String fileName = simpleName + ".java";

        try (Stream<Path> stream = Files.walk(root.toPath())) {
            Optional<Path> found = stream.filter(p -> p.getFileName().toString().equals(fileName)).findFirst();
            if (found.isPresent()) {
                return found.get().toFile();
            }
        } catch (Exception ignored) {}

        return null;
    }

    // =========================================================================
    // 1. java.inspect_class
    // =========================================================================
    public static class InspectClassTool implements ITool {
        public static final String NAME = "java.inspect_class";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public String execute(String command, File workingDir, TaskContext context) throws Exception {
            JSONObject args = parseArgs(command);
            String className = args.optString("className", args.optString("target", args.optString("class", "")));
            File sourceFile = findSourceFile(workingDir, className);

            JSONObject result = new JSONObject();
            result.put("tool", NAME);

            if (sourceFile == null || !sourceFile.exists()) {
                result.put("success", false);
                result.put("error", "Source file not found for class: " + className);
                result.put("complete", false);
                return result.toString(2);
            }

            result.put("success", true);
            result.put("file", sourceFile.getAbsolutePath());
            result.put("relativeFile", workingDir.toPath().relativize(sourceFile.toPath()).toString().replace('\\', '/'));

            List<String> lines = Files.readAllLines(sourceFile.toPath());
            String packageName = "";
            String superclass = null;
            List<String> interfaces = new ArrayList<>();
            List<JSONObject> fields = new ArrayList<>();
            List<JSONObject> methods = new ArrayList<>();

            Pattern pkgPattern = Pattern.compile("^\\s*package\\s+([a-zA-Z0-9_.]+);");
            Pattern classPattern = Pattern.compile("(public|protected|private)?\\s*(abstract|final|static)?\\s*(class|interface|enum|record)\\s+([a-zA-Z0-9_]+)(\\s+extends\\s+([a-zA-Z0-9_.]+))?(\\s+implements\\s+([a-zA-Z0-9_.,\\s]+))?");
            Pattern methodPattern = Pattern.compile("^\\s*(public|protected|private)?\\s*(static)?\\s*(final)?\\s*([a-zA-Z0-9_<>\\[\\],\\s]+)\\s+([a-zA-Z0-9_]+)\\s*\\(([^)]*)\\)\\s*(throws\\s+[a-zA-Z0-9_.,\\s]+)?\\s*\\{?");
            Pattern fieldPattern = Pattern.compile("^\\s*(public|protected|private)?\\s*(static)?\\s*(final)?\\s*([a-zA-Z0-9_<>\\[\\]]+)\\s+([a-zA-Z0-9_]+)\\s*(=\\s*[^;]+)?;");

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                int lineNum = i + 1;

                Matcher mPkg = pkgPattern.matcher(line);
                if (mPkg.find()) {
                    packageName = mPkg.group(1);
                }

                Matcher mCls = classPattern.matcher(line);
                if (mCls.find()) {
                    result.put("kind", mCls.group(3));
                    result.put("name", mCls.group(4));
                    if (mCls.group(6) != null) superclass = mCls.group(6).trim();
                    if (mCls.group(8) != null) {
                        for (String iface : mCls.group(8).split(",")) {
                            if (!iface.trim().isEmpty()) interfaces.add(iface.trim());
                        }
                    }
                    result.put("declarationLine", lineNum);
                }

                Matcher mFld = fieldPattern.matcher(line);
                if (mFld.find()) {
                    JSONObject fld = new JSONObject();
                    fld.put("visibility", mFld.group(1) != null ? mFld.group(1) : "package");
                    fld.put("static", mFld.group(2) != null);
                    fld.put("final", mFld.group(3) != null);
                    fld.put("type", mFld.group(4));
                    fld.put("name", mFld.group(5));
                    fld.put("line", lineNum);
                    fields.add(fld);
                }

                Matcher mMth = methodPattern.matcher(line);
                if (mMth.find() && !line.trim().startsWith("if") && !line.trim().startsWith("while") && !line.trim().startsWith("for") && !line.trim().startsWith("return")) {
                    String returnType = mMth.group(4) != null ? mMth.group(4).trim() : "void";
                    String mName = mMth.group(5);
                    if (!"new".equals(mName) && !"throw".equals(mName)) {
                        JSONObject mth = new JSONObject();
                        mth.put("visibility", mMth.group(1) != null ? mMth.group(1) : "package");
                        mth.put("static", mMth.group(2) != null);
                        mth.put("final", mMth.group(3) != null);
                        mth.put("returnType", returnType);
                        mth.put("name", mName);
                        mth.put("parameters", mMth.group(6) != null ? mMth.group(6).trim() : "");
                        mth.put("line", lineNum);
                        methods.add(mth);
                    }
                }
            }

            result.put("package", packageName);
            result.put("superclass", superclass != null ? superclass : "java.lang.Object");
            result.put("interfaces", new JSONArray(interfaces));
            result.put("fields", new JSONArray(fields));
            result.put("methods", new JSONArray(methods));
            result.put("complete", true);

            return result.toString(2);
        }
    }

    // =========================================================================
    // 2. java.inspect_method
    // =========================================================================
    public static class InspectMethodTool implements ITool {
        public static final String NAME = "java.inspect_method";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public String execute(String command, File workingDir, TaskContext context) throws Exception {
            JSONObject args = parseArgs(command);
            String className = args.optString("className", args.optString("class", ""));
            String methodName = args.optString("methodName", args.optString("method", args.optString("target", "")));

            JSONObject result = new JSONObject();
            result.put("tool", NAME);

            File sourceFile = findSourceFile(workingDir, className);
            if (sourceFile == null || !sourceFile.exists()) {
                result.put("success", false);
                result.put("error", "Source file not found for class: " + className);
                result.put("complete", false);
                return result.toString(2);
            }

            List<String> lines = Files.readAllLines(sourceFile.toPath());
            List<JSONObject> matches = new ArrayList<>();

            Pattern methodHeader = Pattern.compile(".*\\b" + Pattern.quote(methodName) + "\\s*\\(([^)]*)\\).*");

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (methodHeader.matcher(line).matches() && !line.trim().startsWith("//") && !line.trim().startsWith("*")) {
                    int startLine = i + 1;
                    int braceCount = 0;
                    boolean opened = false;
                    StringBuilder body = new StringBuilder();

                    for (int j = i; j < lines.size(); j++) {
                        String bodyLine = lines.get(j);
                        body.append(bodyLine).append("\n");
                        for (char c : bodyLine.toCharArray()) {
                            if (c == '{') {
                                braceCount++;
                                opened = true;
                            } else if (c == '}') {
                                braceCount--;
                            }
                        }
                        if (opened && braceCount <= 0) {
                            JSONObject m = new JSONObject();
                            m.put("methodName", methodName);
                            m.put("startLine", startLine);
                            m.put("endLine", j + 1);
                            m.put("signature", line.trim());
                            m.put("body", body.toString());
                            matches.add(m);
                            break;
                        }
                    }
                }
            }

            result.put("success", !matches.isEmpty());
            result.put("className", className);
            result.put("methodName", methodName);
            result.put("file", sourceFile.getAbsolutePath());
            result.put("matches", new JSONArray(matches));
            result.put("complete", true);

            return result.toString(2);
        }
    }

    // =========================================================================
    // 3. java.find_references
    // =========================================================================
    public static class FindReferencesTool implements ITool {
        public static final String NAME = "java.find_references";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public String execute(String command, File workingDir, TaskContext context) throws Exception {
            JSONObject args = parseArgs(command);
            String target = args.optString("target", args.optString("element", args.optString("name", "")));

            JSONObject result = new JSONObject();
            result.put("tool", NAME);
            result.put("target", target);

            if (target.isEmpty()) {
                result.put("success", false);
                result.put("error", "No target element specified for find_references");
                return result.toString(2);
            }

            List<JSONObject> refs = new ArrayList<>();
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(target) + "\\b");

            try (Stream<Path> stream = Files.walk(workingDir.toPath())) {
                List<Path> javaFiles = stream.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
                for (Path p : javaFiles) {
                    try {
                        List<String> lines = Files.readAllLines(p);
                        for (int i = 0; i < lines.size(); i++) {
                            String l = lines.get(i);
                            if (pattern.matcher(l).find()) {
                                JSONObject ref = new JSONObject();
                                ref.put("file", p.toFile().getAbsolutePath());
                                ref.put("relativeFile", workingDir.toPath().relativize(p).toString().replace('\\', '/'));
                                ref.put("line", i + 1);
                                ref.put("content", l.trim());
                                refs.add(ref);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

            result.put("success", true);
            result.put("matchCount", refs.size());
            result.put("references", new JSONArray(refs));
            result.put("complete", true);
            return result.toString(2);
        }
    }

    // =========================================================================
    // 4. java.find_callers
    // =========================================================================
    public static class FindCallersTool implements ITool {
        public static final String NAME = "java.find_callers";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public String execute(String command, File workingDir, TaskContext context) throws Exception {
            JSONObject args = parseArgs(command);
            String methodName = args.optString("methodName", args.optString("target", args.optString("method", "")));

            JSONObject result = new JSONObject();
            result.put("tool", NAME);
            result.put("methodName", methodName);

            if (methodName.isEmpty()) {
                result.put("success", false);
                result.put("error", "No methodName specified for find_callers");
                return result.toString(2);
            }

            Pattern callPattern = Pattern.compile("\\b" + Pattern.quote(methodName) + "\\s*\\(");
            Pattern enclosingMethodPattern = Pattern.compile("(public|protected|private|static|final|void|[A-Za-z0-9_<>]+)\\s+([a-zA-Z0-9_]+)\\s*\\([^)]*\\)\\s*(throws\\s+[^;{]+)?\\s*\\{");

            List<JSONObject> callers = new ArrayList<>();

            try (Stream<Path> stream = Files.walk(workingDir.toPath())) {
                List<Path> javaFiles = stream.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
                for (Path p : javaFiles) {
                    try {
                        List<String> lines = Files.readAllLines(p);
                        String currentEnclosingMethod = "top-level / static initializer";
                        for (int i = 0; i < lines.size(); i++) {
                            String l = lines.get(i);
                            Matcher mm = enclosingMethodPattern.matcher(l);
                            if (mm.find() && !l.trim().startsWith("if") && !l.trim().startsWith("while")) {
                                currentEnclosingMethod = mm.group(2);
                            }
                            if (callPattern.matcher(l).find() && !l.contains(" " + methodName + "(")) {
                                JSONObject call = new JSONObject();
                                call.put("callerMethod", currentEnclosingMethod);
                                call.put("file", p.toFile().getAbsolutePath());
                                call.put("relativeFile", workingDir.toPath().relativize(p).toString().replace('\\', '/'));
                                call.put("line", i + 1);
                                call.put("callSite", l.trim());
                                callers.add(call);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

            result.put("success", true);
            result.put("callerCount", callers.size());
            result.put("callers", new JSONArray(callers));
            result.put("complete", true);
            return result.toString(2);
        }
    }

    // =========================================================================
    // 5. java.find_implementations
    // =========================================================================
    public static class FindImplementationsTool implements ITool {
        public static final String NAME = "java.find_implementations";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public String execute(String command, File workingDir, TaskContext context) throws Exception {
            JSONObject args = parseArgs(command);
            String ifaceName = args.optString("interfaceName", args.optString("target", args.optString("class", "")));

            JSONObject result = new JSONObject();
            result.put("tool", NAME);
            result.put("target", ifaceName);

            if (ifaceName.isEmpty()) {
                result.put("success", false);
                result.put("error", "No interfaceName or superclass specified");
                return result.toString(2);
            }

            Pattern implPattern = Pattern.compile("\\b(implements|extends)\\s+[^;{]*\\b" + Pattern.quote(ifaceName) + "\\b");
            Pattern classDefPattern = Pattern.compile("(public|protected|private)?\\s*(abstract|final)?\\s*(class|record|enum)\\s+([a-zA-Z0-9_]+)");

            List<JSONObject> impls = new ArrayList<>();

            try (Stream<Path> stream = Files.walk(workingDir.toPath())) {
                List<Path> javaFiles = stream.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
                for (Path p : javaFiles) {
                    try {
                        List<String> lines = Files.readAllLines(p);
                        for (int i = 0; i < lines.size(); i++) {
                            String l = lines.get(i);
                            if (implPattern.matcher(l).find()) {
                                Matcher mCls = classDefPattern.matcher(l);
                                String implementingClass = mCls.find() ? mCls.group(4) : p.getFileName().toString().replace(".java", "");
                                JSONObject imp = new JSONObject();
                                imp.put("className", implementingClass);
                                imp.put("file", p.toFile().getAbsolutePath());
                                imp.put("relativeFile", workingDir.toPath().relativize(p).toString().replace('\\', '/'));
                                imp.put("line", i + 1);
                                imp.put("declaration", l.trim());
                                impls.add(imp);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

            result.put("success", true);
            result.put("count", impls.size());
            result.put("implementations", new JSONArray(impls));
            result.put("complete", true);
            return result.toString(2);
        }
    }

    // =========================================================================
    // 6. java.get_diagnostics
    // =========================================================================
    public static class GetDiagnosticsTool implements ITool {
        public static final String NAME = "java.get_diagnostics";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public String execute(String command, File workingDir, TaskContext context) throws Exception {
            JSONObject result = new JSONObject();
            result.put("tool", NAME);

            List<JSONObject> diagList = new ArrayList<>();

            // 1. Try Eclipse JDT markers if workspace available
            boolean usedEclipse = false;
            try {
                org.eclipse.core.resources.IWorkspaceRoot root = org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot();
                if (root != null) {
                    org.eclipse.core.resources.IMarker[] markers = root.findMarkers(
                            org.eclipse.core.resources.IMarker.PROBLEM, true, org.eclipse.core.resources.IResource.DEPTH_INFINITE);
                    if (markers != null) {
                        for (org.eclipse.core.resources.IMarker m : markers) {
                            if (m != null && m.getResource() != null && m.getResource().getLocation() != null) {
                                File f = m.getResource().getLocation().toFile();
                                if (f.getAbsolutePath().startsWith(workingDir.getAbsolutePath())) {
                                    JSONObject d = new JSONObject();
                                    d.put("file", f.getAbsolutePath());
                                    d.put("relativeFile", workingDir.toPath().relativize(f.toPath()).toString().replace('\\', '/'));
                                    d.put("line", m.getAttribute(org.eclipse.core.resources.IMarker.LINE_NUMBER, 0));
                                    d.put("message", m.getAttribute(org.eclipse.core.resources.IMarker.MESSAGE, ""));
                                    int severity = m.getAttribute(org.eclipse.core.resources.IMarker.SEVERITY, 0);
                                    d.put("severity", severity == 2 ? "ERROR" : (severity == 1 ? "WARNING" : "INFO"));
                                    diagList.add(d);
                                }
                            }
                        }
                        usedEclipse = true;
                    }
                }
            } catch (Throwable ignored) {
                // Non-Eclipse fallback
            }

            // 2. Fallback: Run standard javax.tools.JavaCompiler over source files in target
            if (!usedEclipse) {
                JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                if (compiler != null) {
                    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
                    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
                        List<File> javaFiles = new ArrayList<>();
                        try (Stream<Path> stream = Files.walk(workingDir.toPath())) {
                            stream.filter(p -> p.toString().endsWith(".java"))
                                  .limit(100) // bounded to avoid compilation stalls
                                  .forEach(p -> javaFiles.add(p.toFile()));
                        }
                        if (!javaFiles.isEmpty()) {
                            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(javaFiles);
                            StringWriter compilerOut = new StringWriter();
                            List<String> options = List.of("-proc:none", "-nowarn");
                            JavaCompiler.CompilationTask task = compiler.getTask(compilerOut, fileManager, diagnostics, options, null, compilationUnits);
                            task.call();

                            for (Diagnostic<? extends JavaFileObject> diag : diagnostics.getDiagnostics()) {
                                JSONObject d = new JSONObject();
                                if (diag.getSource() != null) {
                                    String uriStr = diag.getSource().toUri().getPath();
                                    d.put("file", uriStr);
                                } else {
                                    d.put("file", "unknown");
                                }
                                d.put("line", diag.getLineNumber());
                                d.put("column", diag.getColumnNumber());
                                d.put("kind", diag.getKind().name());
                                d.put("message", diag.getMessage(Locale.getDefault()));
                                diagList.add(d);
                            }
                        }
                    }
                }
            }

            long errorCount = diagList.stream().filter(d -> "ERROR".equalsIgnoreCase(d.optString("severity", d.optString("kind", "")))).count();
            result.put("success", true);
            result.put("errorCount", errorCount);
            result.put("warningCount", diagList.size() - errorCount);
            result.put("diagnostics", new JSONArray(diagList));
            result.put("complete", true);

            return result.toString(2);
        }
    }

    // =========================================================================
    // 7. java.get_project_structure
    // =========================================================================
    public static class GetProjectStructureTool implements ITool {
        public static final String NAME = "java.get_project_structure";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public String execute(String command, File workingDir, TaskContext context) throws Exception {
            JSONObject result = new JSONObject();
            result.put("tool", NAME);
            result.put("projectRoot", workingDir.getAbsolutePath());

            Set<String> packages = new TreeSet<>();
            List<String> sourceRoots = new ArrayList<>();
            Map<String, List<String>> packageClasses = new TreeMap<>();

            // Find common source directories
            String[] candidateRoots = {"src/main/java", "src/test/java", "src", "."};
            for (String cr : candidateRoots) {
                File dir = new File(workingDir, cr);
                if (dir.exists() && dir.isDirectory() && !sourceRoots.contains(cr)) {
                    sourceRoots.add(cr);
                }
            }

            Pattern pkgPattern = Pattern.compile("^\\s*package\\s+([a-zA-Z0-9_.]+);");

            try (Stream<Path> stream = Files.walk(workingDir.toPath())) {
                List<Path> javaFiles = stream.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
                for (Path p : javaFiles) {
                    try {
                        String pkgName = "default";
                        for (String l : Files.readAllLines(p)) {
                            Matcher m = pkgPattern.matcher(l);
                            if (m.find()) {
                                pkgName = m.group(1);
                                break;
                            }
                        }
                        packages.add(pkgName);
                        packageClasses.computeIfAbsent(pkgName, k -> new ArrayList<>()).add(p.getFileName().toString().replace(".java", ""));
                    } catch (Exception ignored) {}
                }
            }

            result.put("success", true);
            result.put("sourceRoots", new JSONArray(sourceRoots));
            result.put("packages", new JSONArray(packages));
            JSONObject pkgObj = new JSONObject();
            packageClasses.forEach((pkg, clsList) -> pkgObj.put(pkg, new JSONArray(clsList)));
            result.put("structure", pkgObj);
            result.put("complete", true);

            return result.toString(2);
        }
    }

    // =========================================================================
    // 8. java.get_dependencies
    // =========================================================================
    public static class GetDependenciesTool implements ITool {
        public static final String NAME = "java.get_dependencies";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public String execute(String command, File workingDir, TaskContext context) throws Exception {
            JSONObject result = new JSONObject();
            result.put("tool", NAME);

            List<JSONObject> deps = new ArrayList<>();

            // 1. Check pom.xml
            File pom = new File(workingDir, "pom.xml");
            if (pom.exists()) {
                try {
                    String content = Files.readString(pom.toPath());
                    Pattern depPattern = Pattern.compile("<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>(\\s*<version>([^<]+)</version>)?", Pattern.MULTILINE);
                    Matcher m = depPattern.matcher(content);
                    while (m.find()) {
                        JSONObject d = new JSONObject();
                        d.put("source", "pom.xml");
                        d.put("groupId", m.group(1).trim());
                        d.put("artifactId", m.group(2).trim());
                        d.put("version", m.group(4) != null ? m.group(4).trim() : "inherited");
                        deps.add(d);
                    }
                } catch (Exception ignored) {}
            }

            // 2. Check MANIFEST.MF (OSGi Require-Bundle)
            File manifest = new File(workingDir, "META-INF/MANIFEST.MF");
            if (manifest.exists()) {
                try {
                    List<String> lines = Files.readAllLines(manifest.toPath());
                    boolean inRequire = false;
                    for (String l : lines) {
                        if (l.startsWith("Require-Bundle:")) {
                            inRequire = true;
                            String bName = l.substring("Require-Bundle:".length()).split(";")[0].trim();
                            if (!bName.isEmpty()) {
                                JSONObject d = new JSONObject();
                                d.put("source", "MANIFEST.MF");
                                d.put("bundle", bName);
                                deps.add(d);
                            }
                        } else if (inRequire) {
                            if (l.startsWith(" ") || l.startsWith("\t")) {
                                String bName = l.split(";")[0].trim();
                                if (bName.endsWith(",")) bName = bName.substring(0, bName.length() - 1).trim();
                                if (!bName.isEmpty()) {
                                    JSONObject d = new JSONObject();
                                    d.put("source", "MANIFEST.MF");
                                    d.put("bundle", bName);
                                    deps.add(d);
                                }
                            } else {
                                inRequire = false;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            result.put("success", true);
            result.put("count", deps.size());
            result.put("dependencies", new JSONArray(deps));
            result.put("complete", true);

            return result.toString(2);
        }
    }

    // =========================================================================
    // 9. java.format
    // =========================================================================
    public static class FormatTool implements ITool {
        public static final String NAME = "java.format";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public String execute(String command, File workingDir, TaskContext context) throws Exception {
            JSONObject args = parseArgs(command);
            String path = args.optString("path", args.optString("file", args.optString("target", "")));

            JSONObject result = new JSONObject();
            result.put("tool", NAME);

            File targetFile = findSourceFile(workingDir, path);
            if (targetFile == null || !targetFile.exists()) {
                result.put("success", false);
                result.put("error", "Source file not found for formatting: " + path);
                return result.toString(2);
            }

            String content = Files.readString(targetFile.toPath());
            String formatted = content;

            // Attempt Eclipse code formatter if available
            try {
                Class<?> tfCls = Class.forName("org.eclipse.jdt.core.ToolFactory");
                Object formatter = tfCls.getMethod("createCodeFormatter", Map.class).invoke(null, (Object) null);
                if (formatter != null) {
                    Class<?> cfCls = Class.forName("org.eclipse.jdt.core.formatter.CodeFormatter");
                    int kind = cfCls.getField("K_COMPILATION_UNIT").getInt(null);
                    Object textEdit = cfCls.getMethod("format", int.class, String.class, int.class, int.class, int.class, String.class)
                            .invoke(formatter, kind, content, 0, content.length(), 0, "\n");
                    if (textEdit != null) {
                        Class<?> idocCls = Class.forName("org.eclipse.jface.text.IDocument");
                        Class<?> docCls = Class.forName("org.eclipse.jface.text.Document");
                        Object doc = docCls.getConstructor(String.class).newInstance(content);
                        textEdit.getClass().getMethod("apply", idocCls).invoke(textEdit, doc);
                        formatted = (String) idocCls.getMethod("get").invoke(doc);
                    }
                }
            } catch (Throwable ignored) {
                // Fallback: standard line trimming and bracket indentation normalization
                formatted = basicFormat(content);
            }

            Files.writeString(targetFile.toPath(), formatted);

            result.put("success", true);
            result.put("file", targetFile.getAbsolutePath());
            result.put("relativeFile", workingDir.toPath().relativize(targetFile.toPath()).toString().replace('\\', '/'));
            result.put("formatted", true);
            result.put("complete", true);

            return result.toString(2);
        }

        private String basicFormat(String content) {
            String[] lines = content.split("\r?\n");
            StringBuilder sb = new StringBuilder();
            int indent = 0;
            for (String raw : lines) {
                String line = raw.trim();
                if (line.startsWith("}")) {
                    indent = Math.max(0, indent - 1);
                }
                sb.append("    ".repeat(indent)).append(line).append("\n");
                if (line.endsWith("{") && !line.startsWith("//")) {
                    indent++;
                }
            }
            return sb.toString();
        }
    }
}
