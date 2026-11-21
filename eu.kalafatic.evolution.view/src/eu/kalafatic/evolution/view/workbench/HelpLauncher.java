package eu.kalafatic.evolution.view.workbench;

/**
 * Help launcher (Family UX Standard v1.0): opens the EVO help page
 * {@code /help.html} served by the primary EvolutionServer at a topic anchor.
 * Used by the Help menu and the F1 context-help key. The port is always taken
 * from the live server state via {@link EvoDesktopLauncher#resolvePrimaryPort()},
 * never hardcoded.
 */
public final class HelpLauncher {

    /** Relative path of the served help page. */
    public static final String HELP_PATH = "/help.html";

    /** Default anchor of the help page (overview). */
    public static final String TOPIC_TOP = "top";

    /** Getting-started section. */
    public static final String TOPIC_GETTING_STARTED = "getting-started";

    /** Context-help section (views / editor pages). */
    public static final String TOPIC_CONTEXT = "context";

    /** Keyboard shortcuts section. */
    public static final String TOPIC_SHORTCUTS = "shortcuts";

    /** Headless CLI commands section. */
    public static final String TOPIC_CLI = "cli";

    /** Diagnostics section. */
    public static final String TOPIC_DIAGNOSTICS = "diagnostics";

    private HelpLauncher() {
    }

    /** Opens the help page at the given anchor (e.g. {@code getting-started}). */
    public static void openHelpTopic(String anchor) {
        String topic = (anchor == null || anchor.isBlank()) ? TOPIC_TOP : anchor;
        EvoDesktopLauncher.launchBrowser(helpUrl(topic));
    }

    /** Opens the help overview ("Help Contents"). */
    public static void openHelpContents() {
        openHelpTopic(TOPIC_TOP);
    }

    /** Opens the context-help section for the given part (view/editor) id. */
    public static void contextHelpFor(String partId) {
        openHelpTopic(anchorFor(partId));
    }

    /**
     * Maps a workbench part id to its help anchor (unknown parts fall back to
     * the general context section).
     */
    static String anchorFor(String partId) {
        if (partId == null || partId.isBlank()) {
            return TOPIC_CONTEXT;
        }
        String id = partId.toLowerCase();
        if (id.contains("navigator") || id.contains("explorer")) {
            return "navigator";
        }
        if (id.contains("multipageeditor") || id.endsWith("tools") || id.contains("toolspage")) {
            return "editor";
        }
        if (id.contains("properties")) {
            return "properties";
        }
        return TOPIC_CONTEXT;
    }

    private static String helpUrl(String anchor) {
        return "http://127.0.0.1:" + EvoDesktopLauncher.resolvePrimaryPort()
                + HELP_PATH + "#" + anchor;
    }
}
