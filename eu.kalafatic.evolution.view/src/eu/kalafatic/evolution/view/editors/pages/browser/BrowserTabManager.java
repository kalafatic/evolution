package eu.kalafatic.evolution.view.editors.pages.browser;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Display;

import eu.kalafatic.evolution.controller.orchestration.ServerManager;
import eu.kalafatic.evolution.controller.orchestration.WebApplicationDescriptor;

/**
 * Single authoritative tab manager for the EVO Browser.
 * Enforces deduplication rules:
 * 1. Opening an already registered website activates its existing tab.
 * 2. Repeated server registration events do not create duplicate tabs.
 * 3. Browser initialization and repeated refreshes do not duplicate registered websites.
 * 4. Navigating within an existing website updates the existing tab without creating a new one.
 * 5. Reopening a website from another EVO component reuses the existing tab.
 * 6. Concurrent requests to open the same website are safely synchronized and deduplicated.
 * 7. Tab creation, activation, and removal are idempotent.
 */
public class BrowserTabManager {

    public static class ClosedTabState {
        private final String url;
        private final String title;
        private final String appId;
        private final boolean pinned;

        public ClosedTabState(String url, String title, String appId, boolean pinned) {
            this.url = url;
            this.title = title;
            this.appId = appId;
            this.pinned = pinned;
        }

        public String getUrl() { return url; }
        public String getTitle() { return title; }
        public String getAppId() { return appId; }
        public boolean isPinned() { return pinned; }
    }

    private final CTabFolder tabFolder;
    private final Consumer<BrowserTabModel> onActiveTabChanged;
    private final Consumer<String> onLocationChanged;
    private final Consumer<Boolean> onLoadingStateChanged;

    // Track active tabs by Tab ID and CTabItem
    private final Map<CTabItem, BrowserTabModel> tabByItem = new ConcurrentHashMap<>();
    private final Map<String, BrowserTabModel> tabById = new ConcurrentHashMap<>();

    // Dedup lookup caches: appId -> BrowserTabModel, normalizedUrl -> BrowserTabModel
    private final Map<String, BrowserTabModel> tabByAppId = new ConcurrentHashMap<>();

    // Pending in-flight openings to prevent race condition duplicates before Browser widget initializes
    private final Set<String> pendingKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Recently closed tabs stack for Ctrl+Shift+T
    private final Deque<ClosedTabState> closedTabsStack = new ArrayDeque<>();
    private static final int MAX_CLOSED_TABS = 30;

    public BrowserTabManager(CTabFolder tabFolder,
                             Consumer<BrowserTabModel> onActiveTabChanged,
                             Consumer<String> onLocationChanged,
                             Consumer<Boolean> onLoadingStateChanged) {
        this.tabFolder = tabFolder;
        this.onActiveTabChanged = onActiveTabChanged;
        this.onLocationChanged = onLocationChanged;
        this.onLoadingStateChanged = onLoadingStateChanged;
    }

    /**
     * Authoritative tab resolution and activation.
     * Guaranteed to return an existing tab if the URL or WebApplication is already open.
     */
    public synchronized BrowserTabModel openOrActivate(String rawUrl, String preferredTitle, WebApplicationDescriptor explicitApp) {
        if ((rawUrl == null || rawUrl.trim().isEmpty()) && explicitApp == null) {
            return null;
        }

        WebApplicationDescriptor matchedApp = explicitApp;
        if (matchedApp == null && rawUrl != null) {
            List<WebApplicationDescriptor> allApps = ServerManager.getInstance().getWebApplications();
            matchedApp = BrowserUrlUtils.findMatchingApp(rawUrl, allApps);
        }

        String targetUrl = rawUrl;
        if (targetUrl == null || targetUrl.trim().isEmpty()) {
            if (matchedApp != null) {
                targetUrl = matchedApp.getFullUrl();
            } else {
                return null;
            }
        }

        // 1. Check existing tab by registered WebApp ID
        if (matchedApp != null) {
            BrowserTabModel existingAppTab = tabByAppId.get(matchedApp.getId());
            if (existingAppTab != null && isValid(existingAppTab)) {
                activateTab(existingAppTab);
                // If caller requested a specific sub-path or page, navigate the existing tab to it
                if (rawUrl != null && !BrowserUrlUtils.isSameUrl(rawUrl, existingAppTab.getCurrentUrl())) {
                    existingAppTab.getBrowser().setUrl(rawUrl);
                }
                return existingAppTab;
            }
        }

        // 2. Check existing tab by normalized URL
        String normalizedTarget = BrowserUrlUtils.normalizeUrl(targetUrl);
        for (BrowserTabModel model : tabByItem.values()) {
            if (isValid(model) && BrowserUrlUtils.isSameUrl(targetUrl, model.getCurrentUrl())) {
                activateTab(model);
                return model;
            }
        }

        // 3. Deduplicate against in-flight pending creations
        String deduplicationKey = (matchedApp != null) ? ("APP:" + matchedApp.getId()) : ("URL:" + normalizedTarget);
        if (!pendingKeys.add(deduplicationKey)) {
            // Already being opened by an asynchronous request
            BrowserTabModel pendingTab = findTabByDedupKey(matchedApp, normalizedTarget);
            if (pendingTab != null) {
                activateTab(pendingTab);
                return pendingTab;
            }
        }

        try {
            // 4. Create new tab
            return createNewTabInternal(targetUrl, preferredTitle, matchedApp);
        } finally {
            pendingKeys.remove(deduplicationKey);
        }
    }

    private BrowserTabModel findTabByDedupKey(WebApplicationDescriptor app, String normalizedUrl) {
        if (app != null) {
            BrowserTabModel m = tabByAppId.get(app.getId());
            if (m != null && isValid(m)) return m;
        }
        for (BrowserTabModel m : tabByItem.values()) {
            if (isValid(m) && BrowserUrlUtils.isSameUrl(normalizedUrl, m.getCurrentUrl())) {
                return m;
            }
        }
        return null;
    }

    private BrowserTabModel createNewTabInternal(String url, String preferredTitle, WebApplicationDescriptor webApp) {
        String tabId = UUID.randomUUID().toString();
        String initialTitle = preferredTitle;
        if (initialTitle == null || initialTitle.trim().isEmpty()) {
            initialTitle = (webApp != null) ? webApp.getName() : "Loading...";
        }

        CTabItem item = new CTabItem(tabFolder, SWT.NONE);
        item.setShowClose(true);

        Browser browser = new Browser(tabFolder, SWT.NONE);
        item.setControl(browser);

        BrowserTabModel model = new BrowserTabModel(tabId, item, browser, url, initialTitle);
        if (webApp != null) {
            model.setWebApp(webApp);
            tabByAppId.put(webApp.getId(), model);
        }

        tabByItem.put(item, model);
        tabById.put(tabId, model);

        configureBrowserHooks(model);

        model.updateTabItemText();
        browser.setUrl(url);

        activateTab(model);
        return model;
    }

    private void configureBrowserHooks(BrowserTabModel model) {
        Browser browser = model.getBrowser();
        CTabItem item = model.getTabItem();

        browser.addLocationListener(new LocationAdapter() {
            @Override
            public void changing(LocationEvent event) {
                model.setLoading(true);
                if (tabFolder.getSelection() == item && onLoadingStateChanged != null) {
                    onLoadingStateChanged.accept(true);
                }
            }

            @Override
            public void changed(LocationEvent event) {
                model.setCurrentUrl(event.location);
                model.setLoading(false);

                if (tabFolder.getSelection() == item) {
                    if (onLocationChanged != null) {
                        onLocationChanged.accept(event.location);
                    }
                    if (onLoadingStateChanged != null) {
                        onLoadingStateChanged.accept(false);
                    }
                }
                model.updateTabItemText();
            }
        });

        browser.addProgressListener(new ProgressAdapter() {
            @Override
            public void completed(ProgressEvent event) {
                model.setLoading(false);
                try {
                    String title = (String) browser.evaluate("return document.title;");
                    if (title != null && !title.trim().isEmpty() && !title.equalsIgnoreCase("about:blank")) {
                        model.setTitle(title.trim());
                    } else if (model.getWebApp() != null) {
                        model.setTitle(model.getWebApp().getName());
                    }
                } catch (Exception ignored) {
                }

                if (tabFolder.getSelection() == item && onLoadingStateChanged != null) {
                    onLoadingStateChanged.accept(false);
                }
            }
        });

        // Register default JS bridges
        try {
            new BrowserFunction(browser, "JavaLog") {
                @Override
                public Object function(Object[] args) {
                    if (args.length > 0) {
                        System.out.println("[EVO Browser] " + args[0]);
                    }
                    return null;
                }
            };
        } catch (Throwable ignored) {}
    }

    public synchronized void activateTab(BrowserTabModel model) {
        if (model == null || !isValid(model)) {
            return;
        }

        tabFolder.setSelection(model.getTabItem());
        if (onActiveTabChanged != null) {
            onActiveTabChanged.accept(model);
        }
    }

    public synchronized void closeTab(CTabItem item) {
        if (item == null || item.isDisposed()) {
            return;
        }

        BrowserTabModel model = tabByItem.remove(item);
        if (model != null) {
            if (model.isPinned()) {
                // Do not close pinned tabs via standard close request
                return;
            }

            tabById.remove(model.getId());
            if (model.getAppId() != null) {
                tabByAppId.remove(model.getAppId());
            }

            // Save to closed tabs stack
            if (model.getCurrentUrl() != null && !model.getCurrentUrl().isEmpty()) {
                pushClosedTab(new ClosedTabState(model.getCurrentUrl(), model.getTitle(), model.getAppId(), model.isPinned()));
            }

            if (model.getBrowser() != null && !model.getBrowser().isDisposed()) {
                model.getBrowser().dispose();
            }
        }

        if (!item.isDisposed()) {
            item.dispose();
        }

        // Notify active tab change
        CTabItem newActive = tabFolder.getSelection();
        if (newActive != null && !newActive.isDisposed()) {
            BrowserTabModel activeModel = tabByItem.get(newActive);
            if (activeModel != null && onActiveTabChanged != null) {
                onActiveTabChanged.accept(activeModel);
            }
        } else if (tabFolder.getItemCount() > 0) {
            CTabItem fallback = tabFolder.getItem(tabFolder.getItemCount() - 1);
            tabFolder.setSelection(fallback);
            BrowserTabModel activeModel = tabByItem.get(fallback);
            if (activeModel != null && onActiveTabChanged != null) {
                onActiveTabChanged.accept(activeModel);
            }
        } else {
            if (onActiveTabChanged != null) {
                onActiveTabChanged.accept(null);
            }
        }
    }

    public synchronized void closeOtherTabs(CTabItem keepItem) {
        if (keepItem == null || keepItem.isDisposed()) return;
        List<CTabItem> toClose = new ArrayList<>();
        for (CTabItem item : tabFolder.getItems()) {
            if (item != keepItem) {
                BrowserTabModel m = tabByItem.get(item);
                if (m != null && m.isPinned()) {
                    continue; // Preserve pinned tabs
                }
                toClose.add(item);
            }
        }
        for (CTabItem item : toClose) {
            closeTab(item);
        }
    }

    public synchronized void closeTabsToRight(CTabItem baseItem) {
        if (baseItem == null || baseItem.isDisposed()) return;
        CTabItem[] all = tabFolder.getItems();
        boolean found = false;
        List<CTabItem> toClose = new ArrayList<>();
        for (CTabItem item : all) {
            if (found) {
                BrowserTabModel m = tabByItem.get(item);
                if (m == null || !m.isPinned()) {
                    toClose.add(item);
                }
            } else if (item == baseItem) {
                found = true;
            }
        }
        for (CTabItem item : toClose) {
            closeTab(item);
        }
    }

    public synchronized BrowserTabModel duplicateTab(CTabItem item) {
        if (item == null || item.isDisposed()) return null;
        BrowserTabModel source = tabByItem.get(item);
        if (source == null) return null;

        String url = source.getCurrentUrl();
        String title = (source.getTitle() != null) ? source.getTitle() : "Duplicate Tab";
        return createNewTabInternal(url, title, null);
    }

    public synchronized void togglePin(CTabItem item) {
        if (item == null || item.isDisposed()) return;
        BrowserTabModel model = tabByItem.get(item);
        if (model != null) {
            model.setPinned(!model.isPinned());
        }
    }

    public synchronized BrowserTabModel reopenClosedTab() {
        if (closedTabsStack.isEmpty()) {
            return null;
        }
        ClosedTabState state = closedTabsStack.pop();
        if (state != null && state.getUrl() != null) {
            WebApplicationDescriptor app = null;
            if (state.getAppId() != null) {
                for (WebApplicationDescriptor desc : ServerManager.getInstance().getWebApplications()) {
                    if (desc.getId().equalsIgnoreCase(state.getAppId())) {
                        app = desc;
                        break;
                    }
                }
            }
            BrowserTabModel model = openOrActivate(state.getUrl(), state.getTitle(), app);
            if (model != null && state.isPinned()) {
                model.setPinned(true);
            }
            return model;
        }
        return null;
    }

    public synchronized void updateAppAvailability(String appId, boolean available) {
        BrowserTabModel model = tabByAppId.get(appId);
        if (model != null && isValid(model)) {
            model.setAvailable(available);
        }
    }

    public BrowserTabModel getTabModel(CTabItem item) {
        if (item == null) return null;
        return tabByItem.get(item);
    }

    public BrowserTabModel getActiveTabModel() {
        CTabItem item = tabFolder.getSelection();
        if (item == null) return null;
        return tabByItem.get(item);
    }

    public List<BrowserTabModel> getAllTabs() {
        return new ArrayList<>(tabByItem.values());
    }

    public boolean canReopenClosedTab() {
        return !closedTabsStack.isEmpty();
    }

    private void pushClosedTab(ClosedTabState state) {
        if (closedTabsStack.size() >= MAX_CLOSED_TABS) {
            closedTabsStack.removeLast();
        }
        closedTabsStack.push(state);
    }

    private boolean isValid(BrowserTabModel model) {
        return model != null &&
               model.getTabItem() != null && !model.getTabItem().isDisposed() &&
               model.getBrowser() != null && !model.getBrowser().isDisposed();
    }
}
