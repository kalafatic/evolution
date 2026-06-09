package eu.kalafatic.evolution.supervisor;

import java.io.File;

public class SupervisorMain {
    public static void main(String[] args) {
        System.out.println("=== EVO AI SUPERVISOR STARTING ===");

        String path = (args.length > 0) ? args[0] : ".";
        String goal = (args.length > 1) ? args[1] : "Improve system modularity and logic extraction";
        File baseDir = new File(path);

        System.out.println("[CONFIG] Base Directory: " + baseDir.getAbsolutePath());
        System.out.println("[CONFIG] Evolution Goal: " + goal);

        SelfDevSupervisor supervisor = new SelfDevSupervisor(baseDir, goal);
        supervisor.run();

        System.out.println("=== EVO AI SUPERVISOR FINISHED ===");
    }
}
