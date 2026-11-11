package eu.kalafatic.evolution.view.editors.pages;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.ServerEntry;
import eu.kalafatic.evolution.controller.orchestration.ServerLifecycleListener;
import eu.kalafatic.evolution.controller.orchestration.ServerManager;
import eu.kalafatic.evolution.controller.orchestration.ServerState;
import eu.kalafatic.evolution.controller.orchestration.WebApplicationDescriptor;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

/**
 * Integrated internal EVO Browser page.
 * Listens to ServerLifecycleListener to automatically open tabs for available EVO web apps,
 * handle reload on server restart, show unavailable badges on stop, and provide context menu actions.
 */
public class BrowserPage extends Composite implements ServerLifecycleListener {

    private final MultiPageEditor editor;
    private Orchestrator orchestrator;

    private Text urlText;
    private Button backBtn;
    private Button forwardBtn;
    private Button refreshBtn;
    private Button stopBtn;
    private Button homeBtn;
    private Button newTabBtn;
    private Button addBookmarkBtn;
    private Button bookmarksBtn;
    private Button historyBtn;
    private CTabFolder tabFolder;

    private final List<String> bookmarks = new ArrayList<>();
    private final List<String> history = new ArrayList<>();

    // Tracks open tabs by app ID / URL key
    private final Map<String, CTabItem> openTabsByKey = new ConcurrentHashMap<>();

    public BrowserPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, SWT.NONE);
        this.editor = editor;
        this.orchestrator = orchestrator;

        loadBookmarks();
        createControl();

        ServerManager.getInstance().addListener(this);
    }

    private void createControl() {
        setLayout(new GridLayout(1, false));

        // Navigation Bar
        Composite navBar = new Composite(this, SWT.NONE);
        navBar.setLayout(new GridLayout(10, false));
        navBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        backBtn = createButton(navBar, "◀", "Back");
        forwardBtn = createButton(navBar, "▶", "Forward");
        refreshBtn = createButton(navBar, "⟳", "Refresh");
        stopBtn = createButton(navBar, "✕", "Stop");
        homeBtn = createButton(navBar, "⌂", "Home");

        urlText = new Text(navBar, SWT.BORDER);
        urlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Button goButton = createButton(navBar, "Go", "Navigate to URL");
        newTabBtn = createButton(navBar, "+", "New Tab");
        addBookmarkBtn = createButton(navBar, "★", "Add Bookmark");
        bookmarksBtn = createButton(navBar, "Bookmarks", "View Bookmarks");
        historyBtn = createButton(navBar, "History", "View History");

        // Tab Folder
        tabFolder = new CTabFolder(this, SWT.BORDER | SWT.CLOSE);
        tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        tabFolder.setSimple(false);
        tabFolder.setUnselectedCloseVisible(true);

        tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
            @Override
            public void close(CTabFolderEvent event) {
                CTabItem item = (CTabItem) event.item;
                removeTrackedTab(item);
                if (item.getControl() != null) {
                    item.getControl().dispose();
                }
            }
        });

        tabFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateUrlFromActiveTab();
            }
        });

        // Tab context menu
        createTabContextMenu();

        // Standard default tab
        createNewTab("https://ollama.com", "Ollama");

        // Open tabs for any web applications already running
        for (WebApplicationDescriptor webApp : ServerManager.getInstance().getWebApplications()) {
            if (ServerManager.getInstance().isPortRunning(webApp.getServerPort())) {
                openOrReloadAppTab(webApp);
            }
        }

        // Listeners
        goButton.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { navigateTo(urlText.getText()); }
        });

        urlText.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetDefaultSelected(SelectionEvent e) { navigateTo(urlText.getText()); }
        });

        backBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                Browser b = getActiveBrowser();
                if (b != null) b.back();
            }
        });

        forwardBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                Browser b = getActiveBrowser();
                if (b != null) b.forward();
            }
        });

        refreshBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                Browser b = getActiveBrowser();
                if (b != null) b.refresh();
            }
        });

        stopBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                Browser b = getActiveBrowser();
                if (b != null) b.stop();
            }
        });

        homeBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { navigateTo("https://ollama.com"); }
        });

        newTabBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { createNewTab("https://ollama.com", "New Tab"); }
        });

        addBookmarkBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { addBookmark(); }
        });

        bookmarksBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { showBookmarksMenu(); }
        });

        historyBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { showHistoryMenu(); }
        });
    }

    private void createTabContextMenu() {
        MenuManager menuMgr = new MenuManager("#BrowserTabPopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                fillTabContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(tabFolder);
        tabFolder.setMenu(menu);
    }

    private void fillTabContextMenu(IMenuManager manager) {
        CTabItem activeItem = tabFolder.getSelection();
        if (activeItem == null || activeItem.isDisposed()) {
            return;
        }

        Browser browser = (Browser) activeItem.getControl();
        String currentUrl = (browser != null && !browser.isDisposed()) ? browser.getUrl() : "";

        // 1. Reload
        Action reloadAction = new Action("Reload") {
            @Override
            public void run() {
                if (browser != null && !browser.isDisposed()) {
                    browser.refresh();
                }
            }
        };
        manager.add(reloadAction);

        // Associate with server if internal EVO endpoint
        ServerEntry associatedServer = findServerForUrl(currentUrl);
        if (associatedServer != null) {
            int port = associatedServer.getPort();
            ServerState state = associatedServer.getState();

            // 2. Stop Server
            Action stopServerAction = new Action("Stop Server (" + associatedServer.getName() + ")") {
                @Override
                public void run() {
                    executeServerJob("Stopping " + associatedServer.getName(), () -> {
                        ServerManager.getInstance().stop(port);
                    });
                }
            };
            stopServerAction.setEnabled(associatedServer.canStop());
            manager.add(stopServerAction);

            // 3. Restart Server
            Action restartServerAction = new Action("Restart Server (" + associatedServer.getName() + ")") {
                @Override
                public void run() {
                    executeServerJob("Restarting " + associatedServer.getName(), () -> {
                        try {
                            ServerManager.getInstance().restart(port);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
            };
            restartServerAction.setEnabled(associatedServer.canRestart());
            manager.add(restartServerAction);
        }

        manager.add(new Separator());

        // 4. Open URL
        Action openUrlAction = new Action("Copy URL") {
            @Override
            public void run() {
                if (currentUrl != null && !currentUrl.isEmpty()) {
                    org.eclipse.swt.dnd.Clipboard cb = new org.eclipse.swt.dnd.Clipboard(getDisplay());
                    cb.setContents(new Object[] { currentUrl }, new org.eclipse.swt.dnd.Transfer[] { org.eclipse.swt.dnd.TextTransfer.getInstance() });
                    cb.dispose();
                }
            }
        };
        manager.add(openUrlAction);

        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    private ServerEntry findServerForUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            URI uri = URI.create(url);
            int port = uri.getPort();
            if (port > 0) {
                return ServerManager.getInstance().getServerEntry(port);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void executeServerJob(String taskName, Runnable task) {
        Job job = new Job(taskName) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                task.run();
                return Status.OK_STATUS;
            }
        };
        job.setUser(true);
        job.schedule();
    }

    private void removeTrackedTab(CTabItem item) {
        for (Map.Entry<String, CTabItem> entry : openTabsByKey.entrySet()) {
            if (entry.getValue() == item) {
                openTabsByKey.remove(entry.getKey());
                break;
            }
        }
    }

    private Button createButton(Composite parent, String text, String tooltip) {
        Button btn = new Button(parent, SWT.PUSH | SWT.FLAT);
        btn.setText(text);
        btn.setToolTipText(tooltip);
        return btn;
    }

    public void openTab(String url) {
        openTab(url, null);
    }

    public void openTab(String url, String preferredTitle) {
        if (url == null || url.trim().isEmpty()) return;

        // Normalize URL key to prevent duplicates
        String key = normalizeUrlKey(url);
        CTabItem existingItem = openTabsByKey.get(key);
        if (existingItem != null && !existingItem.isDisposed()) {
            tabFolder.setSelection(existingItem);
            updateUrlFromActiveTab();
            return;
        }

        for (CTabItem item : tabFolder.getItems()) {
            Browser b = (Browser) item.getControl();
            if (b != null && !b.isDisposed() && isSameUrl(url, b.getUrl())) {
                openTabsByKey.put(key, item);
                tabFolder.setSelection(item);
                updateUrlFromActiveTab();
                return;
            }
        }
        createNewTab(url, preferredTitle != null ? preferredTitle : "Loading...");
    }

    public void openTabs(List<String> urls) {
        if (urls == null) return;
        for (String url : urls) {
            openTab(url);
        }
    }

    private void openOrReloadAppTab(WebApplicationDescriptor webApp) {
        String url = webApp.getFullUrl();
        String key = webApp.getId();

        CTabItem existingItem = openTabsByKey.get(key);
        if (existingItem != null && !existingItem.isDisposed()) {
            // Already open -> reload existing tab
            existingItem.setText(webApp.getName());
            Browser browser = (Browser) existingItem.getControl();
            if (browser != null && !browser.isDisposed()) {
                browser.setUrl(url);
            }
            return;
        }

        // Check if any tab matches URL
        for (CTabItem item : tabFolder.getItems()) {
            Browser b = (Browser) item.getControl();
            if (b != null && !b.isDisposed() && isSameUrl(url, b.getUrl())) {
                openTabsByKey.put(key, item);
                item.setText(webApp.getName());
                b.setUrl(url);
                return;
            }
        }

        createNewTabForApp(webApp);
    }

    private void createNewTabForApp(WebApplicationDescriptor webApp) {
        CTabItem item = new CTabItem(tabFolder, SWT.NONE);
        item.setText(webApp.getName());
        openTabsByKey.put(webApp.getId(), item);

        Browser browser = new Browser(tabFolder, SWT.NONE);
        item.setControl(browser);

        configureBrowser(browser, item, webApp.getName());
        browser.setUrl(webApp.getFullUrl());
        tabFolder.setSelection(item);
        updateUrlFromActiveTab();
    }

    private void createNewTab(String url, String title) {
        CTabItem item = new CTabItem(tabFolder, SWT.NONE);
        item.setText(title != null ? title : "Loading...");
        String key = normalizeUrlKey(url);
        openTabsByKey.put(key, item);

        Browser browser = new Browser(tabFolder, SWT.NONE);
        item.setControl(browser);

        configureBrowser(browser, item, title);
        browser.setUrl(url);
        tabFolder.setSelection(item);
        updateUrlFromActiveTab();
    }

    private void configureBrowser(Browser browser, CTabItem item, String fallbackTitle) {
        browser.addLocationListener(new LocationAdapter() {
            @Override
            public void changed(LocationEvent event) {
                if (tabFolder.getSelection() == item) {
                    urlText.setText(event.location);
                }
                updateNavButtons(browser);
                addToHistory(event.location);
            }
        });

        browser.addProgressListener(new ProgressAdapter() {
            @Override
            public void completed(ProgressEvent event) {
                try {
                    String title = (String) browser.evaluate("return document.title;");
                    if (title != null && !title.trim().isEmpty()) {
                        item.setText(title);
                    } else if (fallbackTitle != null) {
                        item.setText(fallbackTitle);
                    }
                } catch (Exception ignored) {}
            }
        });

        new BrowserFunction(browser, "JavaLog") {
            @Override
            public Object function(Object[] args) {
                if (args.length > 0) {
                    System.out.println("[Internal Browser] " + args[0]);
                }
                return null;
            }
        };

        new BrowserFunction(browser, "JavaHandler") {
            @Override
            public Object function(Object[] args) {
                return null;
            }
        };
    }

    private String normalizeUrlKey(String url) {
        if (url == null) return "";
        return url.trim().toLowerCase().replaceAll("\\?runtime=SWT", "");
    }

    private boolean isSameUrl(String u1, String u2) {
        if (u1 == null || u2 == null) return false;
        String n1 = normalizeUrlKey(u1);
        String n2 = normalizeUrlKey(u2);
        return n1.equalsIgnoreCase(n2);
    }

    private Browser getActiveBrowser() {
        CTabItem selection = tabFolder.getSelection();
        if (selection != null) {
            return (Browser) selection.getControl();
        }
        return null;
    }

    private void navigateTo(String url) {
        Browser browser = getActiveBrowser();
        if (browser != null) {
            browser.setUrl(url);
        } else {
            createNewTab(url, null);
        }
    }

    private void updateUrlFromActiveTab() {
        Browser browser = getActiveBrowser();
        if (browser != null && !browser.isDisposed()) {
            urlText.setText(browser.getUrl());
            updateNavButtons(browser);
        }
    }

    private void updateNavButtons(Browser browser) {
        if (browser != null && !browser.isDisposed()) {
            backBtn.setEnabled(browser.isBackEnabled());
            forwardBtn.setEnabled(browser.isForwardEnabled());
        }
    }

    private void addToHistory(String url) {
        if (!history.contains(url)) {
            history.add(0, url);
            if (history.size() > 50) history.remove(history.size() - 1);
        }
    }

    private void addBookmark() {
        String url = urlText.getText();
        if (url != null && !url.isEmpty() && !bookmarks.contains(url)) {
            bookmarks.add(url);
            saveBookmarks();
        }
    }

    private void showBookmarksMenu() {
        Menu menu = new Menu(getShell(), SWT.POP_UP);
        for (String url : bookmarks) {
            MenuItem item = new MenuItem(menu, SWT.PUSH);
            item.setText(url);
            item.addSelectionListener(new SelectionAdapter() {
                @Override public void widgetSelected(SelectionEvent e) { navigateTo(url); }
            });
        }
        if (bookmarks.isEmpty()) {
            MenuItem item = new MenuItem(menu, SWT.PUSH);
            item.setText("No bookmarks");
            item.setEnabled(false);
        }
        menu.setLocation(bookmarksBtn.toDisplay(0, bookmarksBtn.getSize().y));
        menu.setVisible(true);
    }

    private void showHistoryMenu() {
        Menu menu = new Menu(getShell(), SWT.POP_UP);
        for (String url : history) {
            MenuItem item = new MenuItem(menu, SWT.PUSH);
            item.setText(url);
            item.addSelectionListener(new SelectionAdapter() {
                @Override public void widgetSelected(SelectionEvent e) { navigateTo(url); }
            });
        }
        if (history.isEmpty()) {
            MenuItem item = new MenuItem(menu, SWT.PUSH);
            item.setText("No history");
            item.setEnabled(false);
        }
        menu.setLocation(historyBtn.toDisplay(0, historyBtn.getSize().y));
        menu.setVisible(true);
    }

    private void loadBookmarks() {
        bookmarks.clear();
        if (orchestrator != null && orchestrator.getSharedMemory() != null) {
            try {
                JSONObject json = new JSONObject(orchestrator.getSharedMemory());
                JSONArray array = json.optJSONArray("browser_bookmarks");
                if (array != null) {
                    for (int i = 0; i < array.length(); i++) {
                        bookmarks.add(array.getString(i));
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveBookmarks() {
        if (orchestrator == null) return;
        try {
            String sharedMem = orchestrator.getSharedMemory();
            JSONObject json = (sharedMem == null || sharedMem.isEmpty()) ? new JSONObject() : new JSONObject(sharedMem);
            json.put("browser_bookmarks", new JSONArray(bookmarks));
            orchestrator.setSharedMemory(json.toString());
        } catch (Exception ignored) {}
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        loadBookmarks();
    }

    @Override
    public void onServerStateChanged(ServerEntry entry) {
        if (isDisposed()) return;
        Display.getDefault().asyncExec(() -> {
            if (isDisposed()) return;
            if (entry.getState() == ServerState.RUNNING) {
                // If this server just came up, reload or open its web apps
                for (WebApplicationDescriptor webApp : ServerManager.getInstance().getWebApplications()) {
                    if (webApp.getServerPort() == entry.getPort()) {
                        openOrReloadAppTab(webApp);
                    }
                }
            } else if (entry.getState() == ServerState.STOPPED || entry.getState() == ServerState.FAILED) {
                // Clearly indicate service unavailable in tab title without closing unrelated tabs
                for (WebApplicationDescriptor webApp : ServerManager.getInstance().getWebApplications()) {
                    if (webApp.getServerPort() == entry.getPort()) {
                        CTabItem item = openTabsByKey.get(webApp.getId());
                        if (item != null && !item.isDisposed()) {
                            item.setText(webApp.getName() + " (Unavailable)");
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onAllServersStarted(List<ServerEntry> servers) {
        if (isDisposed()) return;
        Display.getDefault().asyncExec(() -> {
            if (isDisposed()) return;
            for (WebApplicationDescriptor webApp : ServerManager.getInstance().getWebApplications()) {
                if (ServerManager.getInstance().isPortRunning(webApp.getServerPort())) {
                    openOrReloadAppTab(webApp);
                }
            }
        });
    }

    @Override
    public void onWebApplicationReady(WebApplicationDescriptor webApp) {
        if (isDisposed()) return;
        Display.getDefault().asyncExec(() -> {
            if (isDisposed()) return;
            openOrReloadAppTab(webApp);
        });
    }

    @Override
    public void onWebApplicationUnavailable(WebApplicationDescriptor webApp) {
        if (isDisposed()) return;
        Display.getDefault().asyncExec(() -> {
            if (isDisposed()) return;
            CTabItem item = openTabsByKey.get(webApp.getId());
            if (item != null && !item.isDisposed()) {
                item.setText(webApp.getName() + " (Unavailable)");
            }
        });
    }

    @Override
    public void dispose() {
        ServerManager.getInstance().removeListener(this);
        super.dispose();
    }
}
