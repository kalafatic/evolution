package eu.kalafatic.evolution.view.editors.pages;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class BrowserPage extends Composite {
    private Text urlText;
    private CTabFolder tabFolder;
    private Orchestrator orchestrator;

    private Button backBtn, forwardBtn, refreshBtn, stopBtn, homeBtn, addBookmarkBtn, bookmarksBtn, historyBtn, newTabBtn;

    private List<String> bookmarks = new ArrayList<>();
    private List<String> history = new ArrayList<>();

    public BrowserPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, SWT.NONE);
        this.orchestrator = orchestrator;
        loadBookmarks();
        createControl();
    }

    private void createControl() {
        this.setLayout(new GridLayout(1, false));

        // Toolbar
        Composite toolbar = new Composite(this, SWT.NONE);
        toolbar.setLayout(new GridLayout(11, false));
        toolbar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        backBtn = createButton(toolbar, "\u2B05", "Back");
        forwardBtn = createButton(toolbar, "\u27A1", "Forward");
        refreshBtn = createButton(toolbar, "\uD83D\uDD04", "Refresh");
        stopBtn = createButton(toolbar, "\u23F9", "Stop");
        homeBtn = createButton(toolbar, "\uD83C\uDFE0", "Home");

        urlText = new Text(toolbar, SWT.BORDER);
        urlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        urlText.setText("https://ollama.com");

        Button goButton = new Button(toolbar, SWT.PUSH);
        goButton.setText("Go");

        addBookmarkBtn = createButton(toolbar, "\u2B50", "Add Bookmark");
        bookmarksBtn = createButton(toolbar, "\uD83D\uDCD6", "Bookmarks");
        historyBtn = createButton(toolbar, "\uD83D\uDCC3", "History");
        newTabBtn = createButton(toolbar, "\u2795", "New Tab");

        // Tab Folder
        tabFolder = new CTabFolder(this, SWT.BORDER | SWT.CLOSE);
        tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
        tabFolder.setSimple(false);
        tabFolder.setUnselectedImageVisible(true);
        tabFolder.setUnselectedCloseVisible(true);

        tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
            @Override
            public void close(CTabFolderEvent event) {
                CTabItem item = (CTabItem) event.item;
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

        // Initial tab
        createNewTab("https://ollama.com");
        createNewTab(orchestrator.getOllama().getUrl());
        if (orchestrator.getServerSettings()!=null) {
			createNewTab("localhost:" + orchestrator.getServerSettings().getPort());			
		} else {
			createNewTab("localhost:48080");
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
            @Override public void widgetSelected(SelectionEvent e) { createNewTab("https://ollama.com"); }
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

    private Button createButton(Composite parent, String text, String tooltip) {
        Button btn = new Button(parent, SWT.PUSH | SWT.FLAT);
        btn.setText(text);
        btn.setToolTipText(tooltip);
        return btn;
    }

    private void createNewTab(String url) {
        CTabItem item = new CTabItem(tabFolder, SWT.NONE);
        item.setText("Loading...");

        Browser browser = new Browser(tabFolder, SWT.NONE);
        item.setControl(browser);

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
                String title = (String) browser.evaluate("return document.title;");
                if (title == null || title.isEmpty()) title = browser.getUrl();
                item.setText(title);
            }
        });

        browser.setUrl(url);
        tabFolder.setSelection(item);
        updateUrlFromActiveTab();
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
            createNewTab(url);
        }
    }

    private void updateUrlFromActiveTab() {
        Browser browser = getActiveBrowser();
        if (browser != null) {
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
            } catch (Exception e) {}
        }
    }

    private void saveBookmarks() {
        if (orchestrator == null) return;
        try {
            String sharedMem = orchestrator.getSharedMemory();
            JSONObject json = (sharedMem == null || sharedMem.isEmpty()) ? new JSONObject() : new JSONObject(sharedMem);
            json.put("browser_bookmarks", new JSONArray(bookmarks));
            orchestrator.setSharedMemory(json.toString());
        } catch (Exception e) {}
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        loadBookmarks();
    }
}
