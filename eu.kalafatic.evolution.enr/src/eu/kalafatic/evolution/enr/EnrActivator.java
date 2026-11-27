package eu.kalafatic.evolution.enr;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.controller.orchestration.llm.ProviderLifecycleRegistry;
/**
 * ENR bundle activator (Phase 6).
 *
 * <p>Registers the ENR runtime as an OSGi service ({@link EnrInferenceEngine}), bridges it
 * into the existing Darwin LLM router ({@link LlmRouter}) as the selectable ENR provider,
 * registers the {@link EnrProviderLifecycle} availability/startup lifecycle for the AI
 * Commander, and starts the ENR HTTP server (family convention port 28780) unless explicitly
 * disabled via {@code evo.enr.server.enabled=false}. All wiring is additive: when the ENR bundle is
 * absent the router falls back to its direct Ollama/native providers unchanged.</p>
 */
public class EnrActivator implements BundleActivator {

    public static final String SYS_SERVER_ENABLED = "evo.enr.server.enabled";
    public static final String SYS_WIRE_ROUTER = "evo.enr.router.enabled";

    private ServiceRegistration<EnrInferenceEngine> serviceRegistration;
    private EnrHttpServer server;

    @Override
    public void start(BundleContext context) {
        EnrRuntime runtime;
        try {
            runtime = EnrRuntime.instance();
        } catch (Throwable t) {
            System.err.println("[ENR] EnrRuntime initialization failed: " + t.getMessage()
                    + ". ENR provider will not be registered.");
            return;
        }

        try {
            serviceRegistration = context.registerService(EnrInferenceEngine.class, runtime, null);
        } catch (Throwable t) {
            System.err.println("[ENR] EnrInferenceEngine service registration failed: "
                    + t.getMessage());
        }

        if (!"false".equalsIgnoreCase(System.getProperty(SYS_WIRE_ROUTER))) {
            try {
                LlmRouter.getInstance().setEnrProvider(new EnrLlmProvider(runtime));
                System.out.println("[ENR] ENR provider registered in LlmRouter (evo.enr.router.enabled="
                        + System.getProperty(SYS_WIRE_ROUTER, "true") + ")");
            } catch (Throwable t) {
                System.err.println("[ENR] LlmRouter wiring failed: " + t.getMessage());
            }
        }

        // Register the availability/startup lifecycle so AI Commander can offer to start
        // ENR on demand. Reuses the same shared server started below.
        try {
            ProviderLifecycleRegistry.getInstance().register(new EnrProviderLifecycle());
        } catch (Throwable t) {
            System.err.println("[ENR] ProviderLifecycle registration failed: " + t.getMessage());
        }

        if (!"false".equalsIgnoreCase(System.getProperty(SYS_SERVER_ENABLED))) {
            try {
                server = EnrProviderLifecycle.startServer();
                System.out.println("[ENR] HTTP API listening on port "
                        + server.getListeningPort() + " (/enr/v1)");
            } catch (Throwable t) {
                System.err.println("[ENR] HTTP server start failed: " + t.getMessage());
                server = null;
            }

            // ENR Web (independent listener, authoritative 28880). The Web Control Center
            // is model/Ollama-independent and must be available whenever the ENR API is
            // started through EVO. It is started even when the API already existed, so a
            // stopped Web can be recovered without a second API instance.
            if (!"false".equalsIgnoreCase(System.getProperty(EnrWebServer.SYS_ENR_WEB_ENABLED))) {
                startWeb(server);
            }
        }
    }

    /**
     * Starts (or verifies) the independent ENR Web listener. Reuses a live Web listener when
     * present; otherwise starts only the Web listener against the already-running API. A Web
     * startup failure is reported with its actual cause and never marks ENR as fully
     * operational implicitly.
     */
    private static void startWeb(EnrHttpServer apiServer) {
        try {
            String apiHost = "127.0.0.1";
            int apiPort = apiServer != null ? apiServer.getListeningPort()
                    : EnrHttpServer.resolvePort();
            EnrWebServer web = EnrProviderLifecycle.startWebServer(apiHost, apiPort);
            System.out.println("[ENR] Web Control Center listening on port "
                    + web.getListeningPort() + " (api_base_url=" + web.apiBaseUrl() + ")");
        } catch (Throwable t) {
            System.err.println("[ENR] Web server start failed: " + t.getMessage());
        }
    }

    @Override
    public void stop(BundleContext context) {
        EnrProviderLifecycle.stopWebServer();
        EnrProviderLifecycle.stopServer();
        server = null;
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
