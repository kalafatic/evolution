package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import eu.kalafatic.evolution.model.orchestration.Compiler;

/**
 * Tool for executing C/C++ compilation and build commands.
 */
public class CppTool implements ITool {
    @Override
    public String getName() {
        return "CppTool";
    }

    @Override
    public String execute(String command, File workingDir, TaskContext context) throws Exception {
        context.log("CppTool: Executing " + command);

        Compiler compiler = context.getOrchestrator().getCompiler();
        String gccPath = (compiler != null && compiler.getCPath() != null && !compiler.getCPath().isEmpty()) ? compiler.getCPath() : "gcc";
        String gppPath = (compiler != null && compiler.getCppPath() != null && !compiler.getCppPath().isEmpty()) ? compiler.getCppPath() : "g++";
        String makePath = (compiler != null && compiler.getMakePath() != null && !compiler.getMakePath().isEmpty()) ? compiler.getMakePath() : "make";
        String cmakePath = (compiler != null && compiler.getCmakePath() != null && !compiler.getCmakePath().isEmpty()) ? compiler.getCmakePath() : "cmake";

        String fullCommand = "";
        String[] parts = command.split("\\s+", 2);
        String action = parts[0].toUpperCase();
        String args = parts.length > 1 ? parts[1] : "";

        switch (action) {
            case "COMPILE_C":
                fullCommand = gccPath + " " + args;
                break;
            case "COMPILE_CPP":
                fullCommand = gppPath + " " + args;
                break;
            case "MAKE":
                fullCommand = makePath + " " + args;
                break;
            case "CMAKE":
                fullCommand = cmakePath + " " + args;
                break;
            case "TEST_CONNECTION":
                StringBuilder sb = new StringBuilder();
                ShellTool shell = new ShellTool();
                try { sb.append("GCC: ").append(shell.execute(gccPath + " --version", workingDir, context).split("\n")[0]).append("\n"); } catch (Exception e) { sb.append("GCC: Not found\n"); }
                try { sb.append("G++: ").append(shell.execute(gppPath + " --version", workingDir, context).split("\n")[0]).append("\n"); } catch (Exception e) { sb.append("G++: Not found\n"); }
                try { sb.append("Make: ").append(shell.execute(makePath + " --version", workingDir, context).split("\n")[0]).append("\n"); } catch (Exception e) { sb.append("Make: Not found\n"); }
                try { sb.append("CMake: ").append(shell.execute(cmakePath + " --version", workingDir, context).split("\n")[0]).append("\n"); } catch (Exception e) { sb.append("CMake: Not found\n"); }
                return sb.toString();
            default:
                // Fallback to direct shell execution if it's whitelisted
                fullCommand = command;
        }

        ShellTool shell = new ShellTool();
        return shell.execute(fullCommand, workingDir, context);
    }
}
