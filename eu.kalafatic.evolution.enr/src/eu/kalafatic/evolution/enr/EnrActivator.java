package eu.kalafatic.evolution.enr;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
/**
 * ENR bundle activator (Phase 6).
 *
 * <p>Registers the ENR runtime as an OSGi service ({@link EnrInferenceEngine}), bridges it
 * into the existing Darwin LLM router ({@link LlmRouter}) as the selectable ENR provider,
 * and starts the ENR HTTP server (family convention port 28780) unless explicitly disabled
 * via {@code evo.enr.server.enabled=false}. All wiring is additive: when the ENR bundle is
 * absent the router falls back to its direct Ollama/native providers unchanged.</p>
 */
public class EnrActivator implements BundleActivator {

    public static final String SYS_SERVER_ENABLED = "evo.enr.server.enabled";
    public static final String SYS_WIRE_ROUTER = "evo.enr.router.enabled";

    private ServiceRegistration<EnrInferenceEngine> serviceRegistration;
    private EnrHttpServer server;

    @Override
    public void start(BundleContext context) {
        EnrRuntime runtime = EnrRuntime.instance();
        serviceRegistration = context.registerService(EnrInferenceEngine.class, runtime, null);

        if (!"false".equalsIgnoreCase(System.getProperty(SYS_WIRE_ROUTER))) {
            try {
                LlmRouter.getInstance().setEnrProvider(new EnrLlmProvider(runtime));
            } catch (Throwable t) {
                System.err.println("[ENR] LlmRouter wiring failed: " + t.getMessage());
            }
        }

        if (!"false".equalsIgnoreCase(System.getProperty(SYS_SERVER_ENABLED))) {
            try {
                server = new EnrHttpServer(runtime);
                server.start(60_000, false);
                System.out.println("[ENR] HTTP API listening on port "
                        + server.getListeningPort() + " (/enr/v1)");
            } catch (Throwable t) {
                System.err.println("[ENR] HTTP server start failed: " + t.getMessage());
                server = null;
            }
        }
    }

    @Override
    public void stop(BundleContext context) {
        if (server != null) {
            server.stop();
            server = null;
        }
        try {
            LlmRouter.getInstance().setEnrProvider(null);
        } catch (Throwable ignored) {
        }
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }
}
