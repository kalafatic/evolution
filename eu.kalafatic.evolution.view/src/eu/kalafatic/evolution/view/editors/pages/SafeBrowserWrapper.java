package eu.kalafatic.evolution.view.editors.pages;


import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.graphics.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Safe SWT Browser wrapper:
 * - prevents blank screen issues
 * - queues JS calls until ready
 * - handles lifecycle timing in Eclipse MultiPageEditor
 */
public class SafeBrowserWrapper {

    private final Composite parent;
    private Browser browser;

    private final ConcurrentLinkedQueue<Runnable> jsQueue = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean pageLoaded = new AtomicBoolean(false);
    private final AtomicBoolean jsReady = new AtomicBoolean(false);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private String pendingHtml;
    private String pendingUrl;

    public SafeBrowserWrapper(Composite parent) {
        this.parent = parent;
        create();
    }

    private void create() {
        parent.setLayout(new GridLayout(1, false));

        browser = new Browser(parent, SWT.NONE);
        browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        attachLifecycleHooks();

        // Prevent blank screen: delay initial render until SWT stabilizes
        parent.getDisplay().asyncExec(() -> {
            parent.getDisplay().timerExec(100, this::safeInitRender);
        });

        initialized.set(true);
    }

    private void attachLifecycleHooks() {

        browser.addProgressListener(new ProgressAdapter() {
            @Override
            public void completed(ProgressEvent event) {
                pageLoaded.set(true);

                // JS bridge hook point
                installJSReadyBridge();

                flushQueue();
            }
        });

        browser.addDisposeListener(e -> {
            jsQueue.clear();
        });
    }

    // ---------------------------
    // PUBLIC API
    // ---------------------------

    public void setHtml(String html) {
        runOnUI(() -> {
            if (browser.isDisposed()) return;

            pendingHtml = html;

            if (!pageLoaded.get()) {
                // defer until safe render
                return;
            }

            browser.setText(html);
        });
    }

    public void setUrl(String url) {
        runOnUI(() -> {
            if (browser.isDisposed()) return;

            pendingUrl = url;

            if (!pageLoaded.get()) return;

            browser.setUrl(url);
        });
    }

    public void executeJS(String js) {
        jsQueue.add(() -> {
            if (!browser.isDisposed() && jsReady.get()) {
                browser.execute(js);
            }
        });

        flushQueue();
    }

    public void refreshHtml(String html) {
        executeJS("if(window.render) window.render(" + escape(html) + ");");
    }

    public Browser getBrowser() {
        return browser;
    }

    // ---------------------------
    // LIFECYCLE SAFETY
    // ---------------------------

    private void safeInitRender() {
        if (browser.isDisposed()) return;

        // Force a stable first paint (prevents blank screen)
        if (pendingUrl != null) {
            browser.setUrl(pendingUrl);
        } else if (pendingHtml != null) {
            browser.setText(pendingHtml);
        } else {
            browser.setText("<html><body></body></html>");
        }
    }

    private void installJSReadyBridge() {
        if (browser.isDisposed()) return;

        new BrowserFunction(browser, "JavaBridgeReady") {
            @Override
            public Object function(Object[] arguments) {
                jsReady.set(true);
                flushQueue();
                return null;
            }
        };
    }

    private void flushQueue() {
        if (!jsReady.get()) return;

        Runnable r;
        while ((r = jsQueue.poll()) != null) {
            runOnUI(r);
        }
    }

    // ---------------------------
    // SWT SAFETY
    // ---------------------------

    private void runOnUI(Runnable r) {
        if (browser == null || browser.isDisposed()) return;

        Display display = browser.getDisplay();
        if (display.isDisposed()) return;

        display.asyncExec(() -> {
            if (!browser.isDisposed()) {
                r.run();
            }
        });
    }

    // ---------------------------
    // UTIL
    // ---------------------------

    private String escape(String json) {
        return json
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}