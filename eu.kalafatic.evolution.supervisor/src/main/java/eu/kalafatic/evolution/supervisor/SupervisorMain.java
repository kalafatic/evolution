package eu.kalafatic.evolution.supervisor;

import java.io.File;

public class SupervisorMain {
    public static void main(String[] args) {
        System.out.println("=== EVO AI SUPERVISOR STARTING ===");

        String path = (args.length > 0) ? args[0] : ".";
        File baseDir = new File(path);

        System.out.println("[CONFIG] Base Directory: " + baseDir.getAbsolutePath());

        Supervisor supervisor = new Supervisor(baseDir);
        supervisor.run();

        System.out.println("=== EVO AI SUPERVISOR FINISHED ===");
    }
}
