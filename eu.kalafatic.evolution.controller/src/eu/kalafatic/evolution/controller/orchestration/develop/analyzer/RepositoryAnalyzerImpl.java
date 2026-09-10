package eu.kalafatic.evolution.controller.orchestration.develop.analyzer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of RepositoryAnalyzer detecting Maven/Tycho, Gradle, NPM, PyTest, CMake, and Make projects.
 */
public class RepositoryAnalyzerImpl implements RepositoryAnalyzer {

    @Override
    public ProjectAnalysis analyze(File repositoryDir) {
        ProjectAnalysis analysis = new ProjectAnalysis();
        List<String> languages = new ArrayList<>();
        List<String> buildFiles = new ArrayList<>();

        if (repositoryDir == null || !repositoryDir.exists()) {
            analysis.setSummary("Repository directory does not exist.");
            return analysis;
        }

        // Maven / Tycho
        File pom = new File(repositoryDir, "pom.xml");
        if (pom.exists()) {
            buildFiles.add("pom.xml");
            languages.add("Java");
            boolean isTycho = new File(repositoryDir, "category.xml").exists()
                    || new File(repositoryDir, "META-INF/MANIFEST.MF").exists()
                    || containsText(pom, "tycho");
            if (isTycho) {
                analysis.setProjectType("Eclipse RCP / OSGi / Tycho");
                analysis.setBuildSystem("Maven Tycho");
            } else {
                analysis.setProjectType("Java Maven");
                analysis.setBuildSystem("Maven");
            }
        }

        // Gradle
        File gradle = new File(repositoryDir, "build.gradle");
        File gradleKts = new File(repositoryDir, "build.gradle.kts");
        if (gradle.exists() || gradleKts.exists()) {
            if (gradle.exists()) buildFiles.add("build.gradle");
            if (gradleKts.exists()) buildFiles.add("build.gradle.kts");
            languages.add("Java/Kotlin");
            analysis.setProjectType("Gradle Project");
            analysis.setBuildSystem("Gradle");
        }

        // Node.js
        File packageJson = new File(repositoryDir, "package.json");
        if (packageJson.exists()) {
            buildFiles.add("package.json");
            languages.add("JavaScript/TypeScript");
            analysis.setProjectType("Node.js");
            analysis.setBuildSystem("NPM/Yarn/PNPM");
        }

        // Python
        File requirements = new File(repositoryDir, "requirements.txt");
        File setupPy = new File(repositoryDir, "setup.py");
        File pyproject = new File(repositoryDir, "pyproject.toml");
        if (requirements.exists() || setupPy.exists() || pyproject.exists()) {
            if (requirements.exists()) buildFiles.add("requirements.txt");
            if (setupPy.exists()) buildFiles.add("setup.py");
            if (pyproject.exists()) buildFiles.add("pyproject.toml");
            languages.add("Python");
            analysis.setProjectType("Python");
            analysis.setBuildSystem("PyTest / Setuptools");
        }

        // C / C++ CMake
        File cmake = new File(repositoryDir, "CMakeLists.txt");
        if (cmake.exists()) {
            buildFiles.add("CMakeLists.txt");
            languages.add("C/C++");
            analysis.setProjectType("C/C++ CMake");
            analysis.setBuildSystem("CMake");
        }

        boolean hasTests = new File(repositoryDir, "src/test").exists()
                || new File(repositoryDir, "tests").exists()
                || new File(repositoryDir, "test").exists();
        analysis.setHasTests(hasTests);
        analysis.setLanguages(languages);
        analysis.setBuildFiles(buildFiles);

        String summary = String.format("Analyzed repository: Type=%s, BuildSystem=%s, Languages=%s, BuildFiles=%s, HasTests=%b",
                analysis.getProjectType(), analysis.getBuildSystem(), languages, buildFiles, hasTests);
        analysis.setSummary(summary);

        return analysis;
    }

    private boolean containsText(File file, String needle) {
        try {
            String content = java.nio.file.Files.readString(file.toPath());
            return content.toLowerCase().contains(needle.toLowerCase());
        } catch (Exception e) {
            return false;
        }
    }
}
