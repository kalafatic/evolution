package eu.kalafatic.evolution.view.editors.pages;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
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
import eu.kalafatic.evolution.controller.resource.PortRegistry;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.browser.BrowserTabManager;
import eu.kalafatic.evolution.view.editors.pages.browser.BrowserTabModel;
import eu.kalafatic.evolution.view.editors.pages.browser.BrowserUrlUtils;

/**
 * Integrated internal EVO Browser page.
 * Provides a professional desktop browser experience embedded within Eclipse RCP:
 * - Single authoritative tab identity and deduplication via BrowserTabManager
 * - Automatic discovery and sync of all registered web applications for configured EVO ports (e.g. 28080, 28280)
 * - Navigation toolbar (Back, Forward, Reload, Stop, Home, URL Bar, Go, New Tab, Bookmarks, History)
 * - Rich Tab bar with favicons, pinning, availability badges, and comprehensive context menus
 * - Full standard keyboard shortcuts (Ctrl+L, Ctrl+T, Ctrl+W, Ctrl+R, Alt+Left/Right, Ctrl+D, Ctrl+Shift+T, Ctrl+Tab)
 */
public class BrowserPage extends Composite implements ServerLifecycleListener {

    private final MultiPageEditor editor;
    private Orchestrator orchestrator;

    private Button backBtn;
    private Button forwardBtn;
    private Button refreshBtn;
    private Button stopBtn;
    private Button homeBtn;
    private Text urlText;
    private Label statusIndicator;
    private Button goBtn;
    private Button newTabBtn;
    private Button bookmarkBtn;
    private Button bookmarksMenuBtn;
    private Button historyMenuBtn;
    private Button serverMenuBtn;

    private CTabFolder tabFolder;
    private BrowserTabManager tabManager;

    private final List<BookmarkEntry> bookmarks = new ArrayList<>();
    private final List<HistoryEntry> history = new ArrayList<>();

    private Listener globalKeyListener;

    public static class BookmarkEntry {
        private String title;
        private String url;

        public BookmarkEntry(String title, String url) {
            this.title = title != null && !title.isEmpty() ? title : url;
            this.url = url;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public static class HistoryEntry {
        private final String url;
        private final long timestamp;

        public HistoryEntry(String url, long timestamp) {
            this.url = url;
            this.timestamp = timestamp;
        }

        public String getUrl() { return url; }
        public long getTimestamp() { return timestamp; }
    }

    public BrowserPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, SWT.NONE);
        this.editor = editor;
        this.orchestrator = orchestrator;

        loadBookmarks();
        createControl();

        ServerManager.getInstance().addListener(this);
    }

    private void createControl() {
        GridLayout rootLayout = new GridLayout(1, false);
        rootLayout.marginWidth = 2;
        rootLayout.marginHeight = 2;
        rootLayout.verticalSpacing = 2;
        setLayout(rootLayout);

        // 1. Navigation Toolbar
        createNavigationToolbar();

        // 2. Tab Folder
        createTabFolder();

        // 3. Tab Context Menu
        createTabContextMenu();

        // 4. Install Global Keyboard Shortcut Filter
        installKeyboardShortcuts();

        // 5. Initialize tabs: Discover all registered web apps and create default tabs
        initializeTabs();
    }

    private void createNavigationToolbar() {
        Composite navBar = new Composite(this, SWT.NONE);
        GridLayout navLayout = new GridLayout(13, false);
        navLayout.marginWidth = 4;
        navLayout.marginHeight = 3;
        navLayout.horizontalSpacing = 4;
        navBar.setLayout(navLayout);
        navBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        backBtn = createToolbarButton(navBar, "◀", "Back (Alt+Left)");
        forwardBtn = createToolbarButton(navBar, "▶", "Forward (Alt+Right)");
        refreshBtn = createToolbarButton(navBar, "⟳", "Reload (Ctrl+R)");
        stopBtn = createToolbarButton(navBar, "✕", "Stop (Esc)");
        homeBtn = createToolbarButton(navBar, "⌂", "Home");

        // Address Bar
        urlText = new Text(navBar, SWT.BORDER | SWT.SINGLE);
        GridData urlGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        urlGd.heightHint = 22;
        urlText.setLayoutData(urlGd);
        urlText.setMessage("Enter URL or select a registered web application...");

        // Auto select-all on focus
        urlText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                Display.getDefault().asyncExec(() -> {
                    if (!urlText.isDisposed()) {
                        urlText.selectAll();
                    }
                });
            }
        });

        // Loading indicator dot
        statusIndicator = new Label(navBar, SWT.NONE);
        statusIndicator.setText("  ");
        GridData statusGd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        statusGd.widthHint = 16;
        statusIndicator.setLayoutData(statusGd);

        goBtn = createToolbarButton(navBar, "Go", "Navigate to URL (Enter)");
        newTabBtn = createToolbarButton(navBar, "+", "New Tab (Ctrl+T)");
        bookmarkBtn = createToolbarButton(navBar, "★", "Bookmark current page (Ctrl+D)");
        bookmarksMenuBtn = createToolbarButton(navBar, "Bookmarks ▾", "View and manage bookmarks");
        historyMenuBtn = createToolbarButton(navBar, "History ▾", "Browsing history");
        serverMenuBtn = createToolbarButton(navBar, "Servers ▾", "EVO Web Applications & Servers");

        // Toolbar Action Listeners
        backBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { navigateBack(); }
        });

        forwardBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { navigateForward(); }
        });

        refreshBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { reloadActiveTab(); }
        });

        stopBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { stopActiveTab(); }
        });

        homeBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { navigateHome(); }
        });

        goBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { navigateTo(urlText.getText()); }
        });

        urlText.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetDefaultSelected(SelectionEvent e) { navigateTo(urlText.getText()); }
        });

        newTabBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { createNewEmptyTab(); }
        });

        bookmarkBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { toggleBookmarkCurrentPage(); }
        });

        bookmarksMenuBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { showBookmarksMenu(); }
        });

        historyMenuBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { showHistoryMenu(); }
        });

        serverMenuBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { showServerMenu(); }
        });
    }

    private void createTabFolder() {
        tabFolder = new CTabFolder(this, SWT.BORDER | SWT.CLOSE | SWT.FLAT);
        tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        tabFolder.setSimple(false);
        tabFolder.setUnselectedCloseVisible(true);
        tabFolder.setSelectionBackground(Display.getDefault().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));

        // Tab Manager
        tabManager = new BrowserTabManager(
                tabFolder,
                this::onActiveTabChanged,
                this::onActiveLocationChanged,
                this::onLoadingStateChanged
        );

        tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
            @Override
            public void close(CTabFolderEvent event) {
                CTabItem item = (CTabItem) event.item;
                tabManager.closeTab(item);
                event.doit = false; // tabManager handles disposal safely
            }
        });

        tabFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                BrowserTabModel active = tabManager.getActiveTabModel();
                onActiveTabChanged(active);
            }
        });

        // Double click on tab strip to create a new tab
        tabFolder.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                CTabItem item = tabFolder.getItem(new Point(e.x, e.y));
                if (item == null) {
                    createNewEmptyTab();
                }
            }
        });
    }

    private void initializeTabs() {
        // Discover registered web applications associated with configured ports
        List<WebApplicationDescriptor> allApps = ServerManager.getInstance().getWebApplications();
        boolean hasRunningAppTab = false;

        for (WebApplicationDescriptor app : allApps) {
            if (ServerManager.getInstance().isPortRunning(app.getServerPort())) {
                tabManager.openOrActivate(app.getFullUrl(), app.getName(), app);
                hasRunningAppTab = true;
            }
        }

        // If no running web app tabs were opened, open standard default home tab
        if (!hasRunningAppTab && tabFolder.getItemCount() == 0) {
            tabManager.openOrActivate("https://ollama.com", "Ollama", null);
        }
    }

    private void onActiveTabChanged(BrowserTabModel model) {
        if (model != null && !isDisposed()) {
            Browser b = model.getBrowser();
            String currentUrl = model.getCurrentUrl();
            if (currentUrl != null && !urlText.isDisposed()) {
                urlText.setText(currentUrl);
            }
            updateNavigationButtons(b);
            onLoadingStateChanged(model.isLoading());
        } else if (!urlText.isDisposed()) {
            urlText.setText("");
            updateNavigationButtons(null);
            onLoadingStateChanged(false);
        }
    }

    private void onActiveLocationChanged(String location) {
        if (urlText != null && !urlText.isDisposed()) {
            urlText.setText(location != null ? location : "");
        }
        BrowserTabModel active = tabManager.getActiveTabModel();
        if (active != null) {
            updateNavigationButtons(active.getBrowser());
        }
        addToHistory(location);
    }

    private void onLoadingStateChanged(boolean loading) {
        if (statusIndicator != null && !statusIndicator.isDisposed()) {
            statusIndicator.setText(loading ? "⏳" : "  ");
        }
        if (stopBtn != null && !stopBtn.isDisposed()) {
            stopBtn.setEnabled(loading);
        }
    }

    private void updateNavigationButtons(Browser browser) {
        if (browser != null && !browser.isDisposed()) {
            backBtn.setEnabled(browser.isBackEnabled());
            forwardBtn.setEnabled(browser.isForwardEnabled());
            refreshBtn.setEnabled(true);
        } else {
            backBtn.setEnabled(false);
            forwardBtn.setEnabled(false);
            refreshBtn.setEnabled(false);
            stopBtn.setEnabled(false);
        }
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

        BrowserTabModel model = tabManager.getTabModel(activeItem);
        if (model == null) return;

        Browser browser = model.getBrowser();
        String currentUrl = model.getCurrentUrl();

        // 1. Reload
        Action reloadAction = new Action("Reload\tCtrl+R") {
            @Override
            public void run() {
                if (browser != null && !browser.isDisposed()) {
                    browser.refresh();
                }
            }
        };
        manager.add(reloadAction);

        // 2. Duplicate Tab
        Action duplicateAction = new Action("Duplicate Tab") {
            @Override
            public void run() {
                tabManager.duplicateTab(activeItem);
            }
        };
        manager.add(duplicateAction);

        // 3. Pin / Unpin Tab
        String pinText = model.isPinned() ? "Unpin Tab" : "Pin Tab";
        Action pinAction = new Action(pinText) {
            @Override
            public void run() {
                tabManager.togglePin(activeItem);
            }
        };
        manager.add(pinAction);

        manager.add(new Separator());

        // 4. Close Tab
        Action closeAction = new Action("Close Tab\tCtrl+W") {
            @Override
            public void run() {
                tabManager.closeTab(activeItem);
            }
        };
        closeAction.setEnabled(!model.isPinned());
        manager.add(closeAction);

        // 5. Close Other Tabs
        Action closeOthersAction = new Action("Close Other Tabs") {
            @Override
            public void run() {
                tabManager.closeOtherTabs(activeItem);
            }
        };
        closeOthersAction.setEnabled(tabFolder.getItemCount() > 1);
        manager.add(closeOthersAction);

        // 6. Close Tabs to the Right
        Action closeRightAction = new Action("Close Tabs to the Right") {
            @Override
            public void run() {
                tabManager.closeTabsToRight(activeItem);
            }
        };
        manager.add(closeRightAction);

        // 7. Reopen Closed Tab
        Action reopenAction = new Action("Reopen Closed Tab\tCtrl+Shift+T") {
            @Override
            public void run() {
                tabManager.reopenClosedTab();
            }
        };
        reopenAction.setEnabled(tabManager.canReopenClosedTab());
        manager.add(reopenAction);

        manager.add(new Separator());

        // 8. Copy URL
        Action copyUrlAction = new Action("Copy URL") {
            @Override
            public void run() {
                if (currentUrl != null && !currentUrl.isEmpty()) {
                    Clipboard cb = new Clipboard(getDisplay());
                    cb.setContents(new Object[]{currentUrl}, new Transfer[]{TextTransfer.getInstance()});
                    cb.dispose();
                }
            }
        };
        manager.add(copyUrlAction);

        // 9. Open in External Browser
        Action openExternalAction = new Action("Open in External Browser") {
            @Override
            public void run() {
                if (currentUrl != null && !currentUrl.isEmpty()) {
                    Program.launch(currentUrl);
                }
            }
        };
        manager.add(openExternalAction);

        // Server control actions if associated with EVO server
        ServerEntry associatedServer = findServerForUrl(currentUrl);
        if (associatedServer != null) {
            manager.add(new Separator());
            int port = associatedServer.getPort();

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

            Action restartServerAction = new Action("Restart Server (" + associatedServer.getName() + ")") {
                @Override
                public void run() {
                    executeServerJob("Restarting " + associatedServer.getName(), () -> {
                        try {
                            ServerManager.getInstance().restart(port);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    });
                }
            };
            restartServerAction.setEnabled(associatedServer.canRestart());
            manager.add(restartServerAction);
        }

        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    private void installKeyboardShortcuts() {
        globalKeyListener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (isDisposed() || !isVisible()) {
                    return;
                }

                // Check if key event belongs to this control hierarchy
                if (event.widget instanceof org.eclipse.swt.widgets.Widget) {
                    org.eclipse.swt.widgets.Widget w = event.widget;
                    boolean belongs = false;
                    if (w instanceof org.eclipse.swt.widgets.Control) {
                        org.eclipse.swt.widgets.Control c = (org.eclipse.swt.widgets.Control) w;
                        while (c != null) {
                            if (c == BrowserPage.this) {
                                belongs = true;
                                break;
                            }
                            c = c.getParent();
                        }
                    }
                    if (!belongs) {
                        return;
                    }
                }

                int state = event.stateMask;
                int key = event.keyCode;

                // Ctrl+L: Focus address bar
                if ((state & SWT.MOD1) != 0 && (key == 'l' || key == 'L')) {
                    if (urlText != null && !urlText.isDisposed()) {
                        urlText.setFocus();
                        urlText.selectAll();
                        event.doit = false;
                    }
                }
                // Ctrl+T: New tab
                else if ((state & SWT.MOD1) != 0 && (key == 't' || key == 'T') && (state & SWT.SHIFT) == 0) {
                    createNewEmptyTab();
                    event.doit = false;
                }
                // Ctrl+Shift+T: Reopen closed tab
                else if ((state & SWT.MOD1) != 0 && (state & SWT.SHIFT) != 0 && (key == 't' || key == 'T')) {
                    tabManager.reopenClosedTab();
                    event.doit = false;
                }
                // Ctrl+W: Close active tab
                else if ((state & SWT.MOD1) != 0 && (key == 'w' || key == 'W')) {
                    CTabItem active = tabFolder.getSelection();
                    if (active != null) {
                        tabManager.closeTab(active);
                    }
                    event.doit = false;
                }
                // Ctrl+R or F5: Reload active tab
                else if (((state & SWT.MOD1) != 0 && (key == 'r' || key == 'R')) || key == SWT.F5) {
                    reloadActiveTab();
                    event.doit = false;
                }
                // Alt+Left: Back
                else if ((state & SWT.ALT) != 0 && key == SWT.ARROW_LEFT) {
                    navigateBack();
                    event.doit = false;
                }
                // Alt+Right: Forward
                else if ((state & SWT.ALT) != 0 && key == SWT.ARROW_RIGHT) {
                    navigateForward();
                    event.doit = false;
                }
                // Ctrl+D: Bookmark current page
                else if ((state & SWT.MOD1) != 0 && (key == 'd' || key == 'D')) {
                    toggleBookmarkCurrentPage();
                    event.doit = false;
                }
                // Escape: Stop loading
                else if (key == SWT.ESC) {
                    stopActiveTab();
                    event.doit = false;
                }
                // Ctrl+Tab: Switch to next tab
                else if ((state & SWT.MOD1) != 0 && key == SWT.TAB && (state & SWT.SHIFT) == 0) {
                    int count = tabFolder.getItemCount();
                    if (count > 1) {
                        int current = tabFolder.getSelectionIndex();
                        int next = (current + 1) % count;
                        tabFolder.setSelection(next);
                        onActiveTabChanged(tabManager.getActiveTabModel());
                    }
                    event.doit = false;
                }
                // Ctrl+Shift+Tab: Switch to previous tab
                else if ((state & SWT.MOD1) != 0 && key == SWT.TAB && (state & SWT.SHIFT) != 0) {
                    int count = tabFolder.getItemCount();
                    if (count > 1) {
                        int current = tabFolder.getSelectionIndex();
                        int prev = (current - 1 + count) % count;
                        tabFolder.setSelection(prev);
                        onActiveTabChanged(tabManager.getActiveTabModel());
                    }
                    event.doit = false;
                }
            }
        };

        Display.getDefault().addFilter(SWT.KeyDown, globalKeyListener);
    }

    public void openTab(String url) {
        openTab(url, null);
    }

    public void openTab(String url, String preferredTitle) {
        if (url == null || url.trim().isEmpty()) return;
        tabManager.openOrActivate(url, preferredTitle, null);
    }

    public void openTabs(List<String> urls) {
        if (urls == null) return;
        for (String url : urls) {
            openTab(url);
        }
    }

    private void createNewEmptyTab() {
        tabManager.openOrActivate("https://ollama.com", "New Tab", null);
        if (urlText != null && !urlText.isDisposed()) {
            urlText.setFocus();
            urlText.selectAll();
        }
    }

    private void navigateTo(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) return;
        String normalized = BrowserUrlUtils.normalizeUrl(rawUrl);

        BrowserTabModel active = tabManager.getActiveTabModel();
        if (active != null) {
            // Check if user is navigating active tab to a known web app
            WebApplicationDescriptor app = BrowserUrlUtils.findMatchingApp(normalized, ServerManager.getInstance().getWebApplications());
            if (app != null) {
                // If this app is already open in another tab, switch to it rather than duplicating
                BrowserTabModel existing = tabManager.openOrActivate(normalized, app.getName(), app);
                if (existing != null && existing != active) {
                    return;
                }
            }
            active.setCurrentUrl(normalized);
            active.getBrowser().setUrl(normalized);
        } else {
            tabManager.openOrActivate(normalized, null, null);
        }
    }

    private void navigateBack() {
        BrowserTabModel active = tabManager.getActiveTabModel();
        if (active != null && active.getBrowser() != null && !active.getBrowser().isDisposed()) {
            active.getBrowser().back();
        }
    }

    private void navigateForward() {
        BrowserTabModel active = tabManager.getActiveTabModel();
        if (active != null && active.getBrowser() != null && !active.getBrowser().isDisposed()) {
            active.getBrowser().forward();
        }
    }

    private void reloadActiveTab() {
        BrowserTabModel active = tabManager.getActiveTabModel();
        if (active != null && active.getBrowser() != null && !active.getBrowser().isDisposed()) {
            active.getBrowser().refresh();
        }
    }

    private void stopActiveTab() {
        BrowserTabModel active = tabManager.getActiveTabModel();
        if (active != null && active.getBrowser() != null && !active.getBrowser().isDisposed()) {
            active.getBrowser().stop();
            onLoadingStateChanged(false);
        }
    }

    private void navigateHome() {
        navigateTo("https://ollama.com");
    }

    private void toggleBookmarkCurrentPage() {
        String currentUrl = urlText.getText();
        if (currentUrl == null || currentUrl.trim().isEmpty()) return;

        BookmarkEntry existing = findBookmark(currentUrl);
        if (existing != null) {
            bookmarks.remove(existing);
        } else {
            BrowserTabModel active = tabManager.getActiveTabModel();
            String title = (active != null && active.getTitle() != null) ? active.getTitle() : currentUrl;
            bookmarks.add(new BookmarkEntry(title, currentUrl));
        }
        saveBookmarks();
    }

    private BookmarkEntry findBookmark(String url) {
        for (BookmarkEntry b : bookmarks) {
            if (BrowserUrlUtils.isSameUrl(b.getUrl(), url)) {
                return b;
            }
        }
        return null;
    }

    private void showBookmarksMenu() {
        Menu menu = new Menu(getShell(), SWT.POP_UP);

        // Add Bookmark Action
        MenuItem addBookmarkItem = new MenuItem(menu, SWT.PUSH);
        addBookmarkItem.setText("Bookmark Current Page\tCtrl+D");
        addBookmarkItem.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { toggleBookmarkCurrentPage(); }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        if (bookmarks.isEmpty()) {
            MenuItem emptyItem = new MenuItem(menu, SWT.PUSH);
            emptyItem.setText("No bookmarks saved");
            emptyItem.setEnabled(false);
        } else {
            for (BookmarkEntry b : bookmarks) {
                MenuItem item = new MenuItem(menu, SWT.CASCADE);
                item.setText(b.getTitle() + " (" + b.getUrl() + ")");

                Menu subMenu = new Menu(menu);
                item.setMenu(subMenu);

                MenuItem openItem = new MenuItem(subMenu, SWT.PUSH);
                openItem.setText("Open");
                openItem.addSelectionListener(new SelectionAdapter() {
                    @Override public void widgetSelected(SelectionEvent e) { navigateTo(b.getUrl()); }
                });

                MenuItem openNewTabItem = new MenuItem(subMenu, SWT.PUSH);
                openNewTabItem.setText("Open in New Tab");
                openNewTabItem.addSelectionListener(new SelectionAdapter() {
                    @Override public void widgetSelected(SelectionEvent e) {
                        tabManager.openOrActivate(b.getUrl(), b.getTitle(), null);
                    }
                });

                MenuItem editItem = new MenuItem(subMenu, SWT.PUSH);
                editItem.setText("Edit Title...");
                editItem.addSelectionListener(new SelectionAdapter() {
                    @Override public void widgetSelected(SelectionEvent e) {
                        InputDialog dlg = new InputDialog(getShell(), "Edit Bookmark", "Bookmark Title:", b.getTitle(), null);
                        if (dlg.open() == Window.OK) {
                            b.setTitle(dlg.getValue());
                            saveBookmarks();
                        }
                    }
                });

                MenuItem removeItem = new MenuItem(subMenu, SWT.PUSH);
                removeItem.setText("Delete Bookmark");
                removeItem.addSelectionListener(new SelectionAdapter() {
                    @Override public void widgetSelected(SelectionEvent e) {
                        bookmarks.remove(b);
                        saveBookmarks();
                    }
                });
            }
        }

        Point loc = bookmarksMenuBtn.toDisplay(0, bookmarksMenuBtn.getSize().y);
        menu.setLocation(loc);
        menu.setVisible(true);
    }

    private void showHistoryMenu() {
        Menu menu = new Menu(getShell(), SWT.POP_UP);

        MenuItem clearItem = new MenuItem(menu, SWT.PUSH);
        clearItem.setText("Clear Browsing History");
        clearItem.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                history.clear();
            }
        });
        clearItem.setEnabled(!history.isEmpty());

        new MenuItem(menu, SWT.SEPARATOR);

        if (history.isEmpty()) {
            MenuItem emptyItem = new MenuItem(menu, SWT.PUSH);
            emptyItem.setText("No history");
            emptyItem.setEnabled(false);
        } else {
            int count = 0;
            for (HistoryEntry h : history) {
                if (count++ > 25) break;
                MenuItem item = new MenuItem(menu, SWT.PUSH);
                item.setText(h.getUrl());
                item.addSelectionListener(new SelectionAdapter() {
                    @Override public void widgetSelected(SelectionEvent e) { navigateTo(h.getUrl()); }
                });
            }
        }

        Point loc = historyMenuBtn.toDisplay(0, historyMenuBtn.getSize().y);
        menu.setLocation(loc);
        menu.setVisible(true);
    }

    private void showServerMenu() {
        Menu menu = new Menu(getShell(), SWT.POP_UP);

        List<WebApplicationDescriptor> webApps = ServerManager.getInstance().getWebApplications();
        for (WebApplicationDescriptor app : webApps) {
            boolean running = ServerManager.getInstance().isPortRunning(app.getServerPort());
            MenuItem item = new MenuItem(menu, SWT.PUSH);
            item.setText(app.getName() + " (Port " + app.getServerPort() + ")" + (running ? " [Active]" : " [Stopped]"));
            item.addSelectionListener(new SelectionAdapter() {
                @Override public void widgetSelected(SelectionEvent e) {
                    tabManager.openOrActivate(app.getFullUrl(), app.getName(), app);
                }
            });
        }

        new MenuItem(menu, SWT.SEPARATOR);

        MenuItem startAllItem = new MenuItem(menu, SWT.PUSH);
        startAllItem.setText("Start All EVO Servers");
        startAllItem.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                ServerManager.getInstance().autoStartAll();
            }
        });

        Point loc = serverMenuBtn.toDisplay(0, serverMenuBtn.getSize().y);
        menu.setLocation(loc);
        menu.setVisible(true);
    }

    private void addToHistory(String url) {
        if (url == null || url.trim().isEmpty() || url.equalsIgnoreCase("about:blank")) return;
        history.removeIf(h -> BrowserUrlUtils.isSameUrl(h.getUrl(), url));
        history.add(0, new HistoryEntry(url, System.currentTimeMillis()));
        if (history.size() > 100) {
            history.remove(history.size() - 1);
        }
    }

    private ServerEntry findServerForUrl(String url) {
        int port = BrowserUrlUtils.extractPort(url);
        if (port > 0) {
            return ServerManager.getInstance().getServerEntry(port);
        }
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

    private Button createToolbarButton(Composite parent, String text, String tooltip) {
        Button btn = new Button(parent, SWT.PUSH | SWT.FLAT);
        btn.setText(text);
        btn.setToolTipText(tooltip);
        return btn;
    }

    private void loadBookmarks() {
        bookmarks.clear();
        if (orchestrator != null && orchestrator.getSharedMemory() != null) {
            try {
                JSONObject json = new JSONObject(orchestrator.getSharedMemory());
                JSONArray array = json.optJSONArray("browser_bookmarks");
                if (array != null) {
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject item = array.optJSONObject(i);
                        if (item != null) {
                            bookmarks.add(new BookmarkEntry(item.optString("title"), item.optString("url")));
                        } else {
                            String url = array.getString(i);
                            bookmarks.add(new BookmarkEntry(url, url));
                        }
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
            JSONArray array = new JSONArray();
            for (BookmarkEntry b : bookmarks) {
                JSONObject item = new JSONObject();
                item.put("title", b.getTitle());
                item.put("url", b.getUrl());
                array.put(item);
            }
            json.put("browser_bookmarks", array);
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
            boolean isRunning = (entry.getState() == ServerState.RUNNING);
            for (WebApplicationDescriptor webApp : ServerManager.getInstance().getWebApplications()) {
                if (webApp.getServerPort() == entry.getPort()) {
                    if (isRunning) {
                        tabManager.updateAppAvailability(webApp.getId(), true);
                        tabManager.openOrActivate(webApp.getFullUrl(), webApp.getName(), webApp);
                    } else if (entry.getState() == ServerState.STOPPED || entry.getState() == ServerState.FAILED) {
                        tabManager.updateAppAvailability(webApp.getId(), false);
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
                    tabManager.updateAppAvailability(webApp.getId(), true);
                    tabManager.openOrActivate(webApp.getFullUrl(), webApp.getName(), webApp);
                }
            }
        });
    }

    @Override
    public void onWebApplicationReady(WebApplicationDescriptor webApp) {
        if (isDisposed()) return;
        Display.getDefault().asyncExec(() -> {
            if (isDisposed()) return;
            tabManager.updateAppAvailability(webApp.getId(), true);
            tabManager.openOrActivate(webApp.getFullUrl(), webApp.getName(), webApp);
        });
    }

    @Override
    public void onWebApplicationUnavailable(WebApplicationDescriptor webApp) {
        if (isDisposed()) return;
        Display.getDefault().asyncExec(() -> {
            if (isDisposed()) return;
            tabManager.updateAppAvailability(webApp.getId(), false);
        });
    }

    @Override
    public void dispose() {
        if (globalKeyListener != null) {
            Display.getDefault().removeFilter(SWT.KeyDown, globalKeyListener);
            globalKeyListener = null;
        }
        ServerManager.getInstance().removeListener(this);
        super.dispose();
    }
}
