# Threading & Concurrency Architecture

## Threading Model Overview

EVO enforces a strict boundary between UI rendering, asynchronous job scheduling, long-running background engine executions, and external process execution.

```
+-----------------------------------------------------------------------------------+
|                                SWT UI Thread                                      |
|   (User Interactions, Table Updates, Progress Bar Canvas, Context Menus)          |
+-----------------------------------------------------------------------------------+
       |                                                                      ^
       | asyncExec / syncExec                                                 | asyncExec
       v                                                                      |
+-----------------------------------------------------------------------------------+
|                        Eclipse Background Jobs / Worker Threads                   |
|   (Git Discovery Job, Model Training Job, Dataset Download Job, Darwin Engine)     |
+-----------------------------------------------------------------------------------+
       |                                                                      ^
       | ProcessBuilder / Subprocess Execution                                | Stream Readers
       v                                                                      |
+-----------------------------------------------------------------------------------+
|                        OS Subprocesses / External Executables                     |
|   (Maven / Tycho Build, Ollama LLM CLI, Java Process Supervisor)                   |
+-----------------------------------------------------------------------------------+
```

---

## Threading Rules & Safeguards

### 1. SWT UI Thread Access Rules
* All UI modifications (widget updates, table row refreshes, canvas repaints, SWT Browser JavaScript execution) MUST occur on the SWT UI thread.
* **SWT Threading Safeguard**: Methods invoking `Display.getDefault().asyncExec(...)` or `timerExec(...)` MUST guard widget access with explicit `!control.isDisposed()` checks to prevent `org.eclipse.swt.SWTException: Invalid thread access` or `Disposed widget` exceptions.

```java
// Thread-Safe Pattern in TestsPage / DevelopmentPage
Display.getDefault().asyncExec(() -> {
    if (browser != null && !browser.isDisposed()) {
        browser.execute("updateDiagram('" + statusJson + "');");
    }
});
```

### 2. Eclipse Job Asynchronous Offloading
* Heavy operations (Git repository initialization, dataset acquisition, neural network model training, architecture scanning) execute asynchronously inside `org.eclipse.core.runtime.jobs.Job` instances with `IProgressMonitor` support.
* **Non-Blocking Workbench Startup**: `GitRepositoryStartup.earlyStartup()` offloads heavy repository status checks and cloning to a background Job named "Git Repository Discovery", keeping workbench window opening instant and responsive.

### 3. Long-Running Subprocess & Stream Handling
* **I/O Buffer Deadlock Prevention**: `MavenBuildExecutor` and `ProcessRunner` drain subprocess standard output and standard error streams concurrently in dedicated background threads (`[MAVEN][STDOUT]` and `[MAVEN][STDERR]`).
* **Stdin Closure**: Process standard input (`process.getOutputStream().close()`) is closed immediately after launch to prevent child processes waiting indefinitely on interactive prompts in non-interactive batch environments.
* **Timeout Enforcement**: Process execution executes `process.waitFor(timeoutMinutes, TimeUnit.MINUTES)`. If a timeout expires, descendant processes (`process.descendants()`) and the root process are forcibly terminated (`process.destroyForcibly()`).
