package eu.kalafatic.evolution.supervisor;

public class PlatformInfo {
    public enum OperatingSystem {
        WINDOWS, LINUX, MACOS, UNKNOWN
    }

    public enum Architecture {
        X86_64, ARM64, UNKNOWN
    }

    public static OperatingSystem getOperatingSystem() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (os.contains("linux") || os.contains("nix") || os.contains("nux")) {
            return OperatingSystem.LINUX;
        } else if (os.contains("mac") || os.contains("darwin")) {
            return OperatingSystem.MACOS;
        }
        return OperatingSystem.UNKNOWN;
    }

    public static Architecture getArchitecture() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.contains("64")) {
            if (arch.contains("aarch64") || arch.contains("arm64")) {
                return Architecture.ARM64;
            }
            return Architecture.X86_64;
        }
        return Architecture.UNKNOWN;
    }

    public static boolean isWindows() {
        return getOperatingSystem() == OperatingSystem.WINDOWS;
    }

    public static boolean isLinux() {
        return getOperatingSystem() == OperatingSystem.LINUX;
    }

    public static boolean isValidExecutable(java.io.File executable) {
        if (executable == null || !executable.exists()) return false;
        java.io.File parentDir = executable.getParentFile();
        if (parentDir == null) return false;
        java.io.File pluginsDir = new java.io.File(parentDir, "plugins");
        return pluginsDir.exists() && pluginsDir.isDirectory();
    }
}
