package eu.kalafatic.evolution.controller.resource;

import java.util.Objects;

/**
 * Encapsulates target platform configuration (OS, WS, Arch, packaging, profile).
 */
public class TargetPlatform {
    private final String os;
    private final String ws;
    private final String arch;
    private final String packaging;
    private final String profile;

    public TargetPlatform(String os, String ws, String arch, String packaging, String profile) {
        this.os = os != null ? os : "win32";
        this.ws = ws != null ? ws : (this.os.contains("win") ? "win32" : "gtk");
        this.arch = arch != null ? arch : "x86_64";
        this.packaging = packaging != null ? packaging : (this.os.contains("win") ? "zip" : "tar.gz");
        this.profile = profile != null ? profile : (this.os.contains("win") ? "-Pwindows" : "-Plinux");
    }

    public String getOs() { return os; }
    public String getWs() { return ws; }
    public String getArch() { return arch; }
    public String getPackaging() { return packaging; }
    public String getProfile() { return profile; }

    public boolean isWindows() { return "win32".equalsIgnoreCase(os) || os.toLowerCase().contains("win"); }
    public boolean isLinux() { return "linux".equalsIgnoreCase(os) || os.toLowerCase().contains("linux"); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TargetPlatform that = (TargetPlatform) o;
        return Objects.equals(os, that.os) && Objects.equals(ws, that.ws) && Objects.equals(arch, that.arch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(os, ws, arch);
    }

    @Override
    public String toString() {
        return os + "." + ws + "." + arch + " (" + packaging + ")";
    }
}
