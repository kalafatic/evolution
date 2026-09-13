package eu.kalafatic.evolution.controller.ui;

/**
 * Authoritative interface for the Unified EVO HTML Style Architecture.
 * Provides central theme design tokens, base workstation typography,
 * common UI component styles, and module-specific style extensions.
 */
public interface IEvoStyleManager {

    /**
     * Gets the central theme design tokens (:root CSS variables).
     */
    String getThemeCss();

    /**
     * Gets the base workstation HTML/body typography, reset, and scrollbar styles.
     */
    String getBaseCss();

    /**
     * Gets the common workstation UI component CSS (.btn, input, select, .evo-panel, tables, badges, etc.).
     */
    String getComponentCss();

    /**
     * Gets the complete aggregated EVO master stylesheet string (Theme + Base + Components).
     */
    String getEvoStyleCss();

    /**
     * Gets the Architecture visualization graph CSS extension.
     */
    String getArchitectureCss();

    /**
     * Gets the AI Chat messaging and cognitive state CSS extension.
     */
    String getChatCss();

    /**
     * Gets the Forge model canvas and visualization CSS extension.
     */
    String getForgeCss();

    /**
     * Gets the Self-Dev / Autonomous Coding Agent CSS extension.
     */
    String getDevelopCss();

    /**
     * Gets page-specific CSS extension for the requested page key.
     */
    String getPageCss(String page);

    /**
     * Injects the centralized EVO style system into an HTML template string.
     */
    String injectEvoStyle(String htmlTemplate);
}
