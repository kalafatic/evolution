package eu.kalafatic.evolution.controller.orchestration.develop;

/**
 * Granular permissions granted to the Develop agent for a task.
 */
public class DevelopPermissions {
    private boolean read = true;
    private boolean write = true;
    private boolean execute = true;
    private boolean push = false;

    public DevelopPermissions() {
    }

    public DevelopPermissions(boolean read, boolean write, boolean execute, boolean push) {
        this.read = read;
        this.write = write;
        this.execute = execute;
        this.push = push;
    }

    public boolean canRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean canWrite() {
        return write;
    }

    public void setWrite(boolean write) {
        this.write = write;
    }

    public boolean canExecute() {
        return execute;
    }

    public void setExecute(boolean execute) {
        this.execute = execute;
    }

    public boolean canPush() {
        return push;
    }

    public void setPush(boolean push) {
        this.push = push;
    }
}
