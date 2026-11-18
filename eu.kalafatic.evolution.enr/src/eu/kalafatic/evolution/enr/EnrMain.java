package eu.kalafatic.evolution.enr;

/**
 * Standalone ENR entry point (Phase 6). Deploys the ENR runtime and its HTTP API outside an
 * Eclipse RCP product, e.g.:
 *
 * <pre>java -cp &lt;enr-bundles-classpath&gt; eu.kalafatic.evolution.enr.EnrMain</pre>
 *
 * The process runs until terminated (SIGINT/SIGTERM triggers a clean shutdown).
 */
public class EnrMain {

    public static void main(String[] args) throws Exception {
        EnrHttpServer server = new EnrHttpServer(EnrRuntime.instance());
        server.start(60_000, false);
        System.out.println("[ENR] Neural Runtime HTTP API on port " + server.getListeningPort()
                + " (/enr/v1). Press Ctrl+C to stop.");

        final EnrHttpServer current = server;
        Runtime.getRuntime().addShutdownHook(new Thread(current::stop, "enr-shutdown"));

        Thread.currentThread().join();
    }
}
