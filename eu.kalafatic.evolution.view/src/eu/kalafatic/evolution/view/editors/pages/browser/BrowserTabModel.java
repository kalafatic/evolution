package eu.kalafatic.evolution.view.editors.pages.browser;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.CTabItem;

import eu.kalafatic.evolution.controller.orchestration.WebApplicationDescriptor;

/**
 * Model representing an active or pinned browser tab in the EVO Browser.
 * Holds stable identity, associated registered web application, navigation status,
 * pinned state, and widget references.
 */
public class BrowserTabModel {

    private final String id;
    private final CTabItem tabItem;
    private final Browser browser;

    private String appId; // associated WebApplicationDescriptor id if registered
    private WebApplicationDescriptor webApp;
    private String currentUrl;
    private String initialUrl;
    private String title;
    private boolean pinned;
    private boolean loading;
    private boolean available = true;

    public BrowserTabModel(String id, CTabItem tabItem, Browser browser, String initialUrl, String title) {
        this.id = id;
        this.tabItem = tabItem;
        this.browser = browser;
        this.initialUrl = initialUrl;
        this.currentUrl = initialUrl;
        this.title = title != null ? title : "New Tab";
        this.loading = true;
    }

    public String getId() {
        return id;
    }

    public CTabItem getTabItem() {
        return tabItem;
    }

    public Browser getBrowser() {
        return browser;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public WebApplicationDescriptor getWebApp() {
        return webApp;
    }

    public void setWebApp(WebApplicationDescriptor webApp) {
        this.webApp = webApp;
        if (webApp != null) {
            this.appId = webApp.getId();
        }
    }

    public boolean isRegisteredApp() {
        return appId != null && !appId.isEmpty();
    }

    public String getCurrentUrl() {
        return currentUrl;
    }

    public void setCurrentUrl(String currentUrl) {
        this.currentUrl = currentUrl;
    }

    public String getInitialUrl() {
        return initialUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        updateTabItemText();
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
        if (tabItem != null && !tabItem.isDisposed()) {
            tabItem.setShowClose(!pinned);
        }
        updateTabItemText();
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
        updateTabItemText();
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
        updateTabItemText();
    }

    public void updateTabItemText() {
        if (tabItem == null || tabItem.isDisposed()) {
            return;
        }

        StringBuilder label = new StringBuilder();
        if (pinned) {
            label.append("📌 ");
        }
        if (loading) {
            label.append("⏳ ");
        }
        if (title != null && !title.isEmpty()) {
            label.append(title);
        } else if (webApp != null) {
            label.append(webApp.getName());
        } else {
            label.append("Loading...");
        }

        if (!available) {
            label.append(" (Unavailable)");
        }

        tabItem.setText(label.toString());
        if (currentUrl != null && !currentUrl.isEmpty()) {
            tabItem.setToolTipText(currentUrl + (available ? "" : " [Service Unavailable]"));
        }
    }

    @Override
    public String toString() {
        return "BrowserTabModel{" +
                "id='" + id + '\'' +
                ", appId='" + appId + '\'' +
                ", title='" + title + '\'' +
                ", url='" + currentUrl + '\'' +
                ", pinned=" + pinned +
                ", available=" + available +
                '}';
    }
}
