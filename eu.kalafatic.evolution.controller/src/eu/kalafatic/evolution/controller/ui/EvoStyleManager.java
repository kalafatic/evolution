package eu.kalafatic.evolution.controller.ui;

import eu.kalafatic.evolution.controller.tools.FileTool;

/**
 * Single source of truth for the Unified EVO HTML Style System.
 * Authoritative provider for visual appearance, theme tokens, typography,
 * common UI components, and page-specific CSS extensions across all EVO HTML interfaces.
 */
public class EvoStyleManager implements IEvoStyleManager {

    private static final EvoStyleManager INSTANCE = new EvoStyleManager();

    public static EvoStyleManager getInstance() {
        return INSTANCE;
    }

    private EvoStyleManager() {
    }

    @Override
    public String getThemeCss() {
        return """
            /* EVO Unified Theme Tokens - Rich Engineering Workstation */
            :root {
                /* Working Surface & Canvas */
                --evo-bg: #e8ecef;
                --evo-surface: #ffffff;
                --evo-surface-alt: #f1f3f6;
                --evo-surface-hover: #e2e7ec;
                --evo-surface-selected: #dbe4f0;

                /* Borders & Separators */
                --evo-border: #b8c0c8;
                --evo-border-dark: #98a2ac;
                --evo-border-focus: #1d5296;

                /* Typography & Text */
                --evo-text: #1a202c;
                --evo-text-secondary: #4a5568;
                --evo-text-muted: #718096;
                --evo-text-inverse: #ffffff;
                --evo-font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, Helvetica, Arial, sans-serif;
                --evo-font-mono: 'Consolas', 'Fira Code', 'JetBrains Mono', 'Courier New', monospace;

                /* Accent & Branding */
                --evo-primary: #1d5296;
                --evo-primary-hover: #153e70;
                --evo-primary-light: #e8f0fe;
                --evo-primary-border: #153e70;

                /* Status & Semantic Colors */
                --evo-success: #1e7e34;
                --evo-success-hover: #145827;
                --evo-success-light: #e6f4ea;
                --evo-warning: #b45309;
                --evo-warning-hover: #853704;
                --evo-warning-light: #fef3c7;
                --evo-danger: #b91c1c;
                --evo-danger-hover: #881313;
                --evo-danger-light: #fee2e2;
                --evo-info: #0284c7;
                --evo-info-light: #e0f2fe;

                /* Geometry & Components */
                --evo-radius: 4px;
                --evo-shadow-sm: 0 1px 2px rgba(0,0,0,0.06);
                --evo-shadow-md: 0 2px 5px rgba(0,0,0,0.1);
            }
            """;
    }

    @Override
    public String getBaseCss() {
        return """
            /* Base Workstation Typography & Reset */
            *, *::before, *::after {
                box-sizing: border-box;
            }

            html, body {
                margin: 0;
                padding: 0;
                height: 100%;
                width: 100%;
                background-color: var(--evo-bg);
                color: var(--evo-text);
                font-family: var(--evo-font-family);
                font-size: 12px;
                line-height: 1.4;
                overflow: hidden;
                -webkit-font-smoothing: antialiased;
            }

            /* Precise Workstation Scrollbars */
            ::-webkit-scrollbar {
                width: 7px;
                height: 7px;
            }
            ::-webkit-scrollbar-track {
                background: var(--evo-bg);
            }
            ::-webkit-scrollbar-thumb {
                background: var(--evo-border-dark);
                border-radius: var(--evo-radius);
            }
            ::-webkit-scrollbar-thumb:hover {
                background: var(--evo-text-muted);
            }

            a {
                color: var(--evo-primary);
                text-decoration: none;
            }
            a:hover {
                text-decoration: underline;
            }
            """;
    }

    @Override
    public String getComponentCss() {
        return """
            /* Common Workstation UI Components */

            /* Buttons */
            .btn {
                display: inline-flex;
                align-items: center;
                justify-content: center;
                gap: 5px;
                background: linear-gradient(to bottom, #ffffff 0%, var(--evo-surface-alt) 100%);
                color: var(--evo-text);
                border: 1px solid var(--evo-border);
                padding: 5px 12px;
                border-radius: var(--evo-radius);
                font-family: var(--evo-font-family);
                font-size: 11px;
                font-weight: 600;
                cursor: pointer;
                user-select: none;
                transition: all 0.12s ease;
                text-decoration: none;
                box-shadow: var(--evo-shadow-sm);
            }
            .btn:hover:not(:disabled) {
                background: var(--evo-surface-hover);
                border-color: var(--evo-border-dark);
                color: var(--evo-primary);
            }
            .btn:active:not(:disabled) {
                background: var(--evo-border);
                box-shadow: inset 0 1px 2px rgba(0,0,0,0.1);
            }
            .btn:disabled {
                opacity: 0.5;
                cursor: not-allowed;
            }

            .btn-primary {
                background: linear-gradient(to bottom, #2563eb 0%, var(--evo-primary) 100%);
                color: var(--evo-text-inverse);
                border-color: var(--evo-primary-border);
            }
            .btn-primary:hover:not(:disabled) {
                background: linear-gradient(to bottom, #1d4ed8 0%, var(--evo-primary-hover) 100%);
                border-color: var(--evo-primary-hover);
                color: var(--evo-text-inverse);
            }

            .btn-secondary {
                background: linear-gradient(to bottom, #ffffff 0%, var(--evo-surface-alt) 100%);
                color: var(--evo-text);
                border-color: var(--evo-border);
            }

            .btn-danger {
                background: linear-gradient(to bottom, #dc2626 0%, var(--evo-danger) 100%);
                color: var(--evo-text-inverse);
                border-color: var(--evo-danger);
            }
            .btn-danger:hover:not(:disabled) {
                background: var(--evo-danger-hover);
            }

            .btn-success {
                background: linear-gradient(to bottom, #16a34a 0%, var(--evo-success) 100%);
                color: var(--evo-text-inverse);
                border-color: var(--evo-success);
            }
            .btn-success:hover:not(:disabled) {
                background: var(--evo-success-hover);
            }

            .btn-sm {
                padding: 3px 8px;
                font-size: 10px;
            }

            /* Inputs & Selects */
            input[type="text"],
            input[type="number"],
            input[type="search"],
            select,
            textarea {
                background-color: var(--evo-surface);
                border: 1px solid var(--evo-border);
                color: var(--evo-text);
                padding: 5px 8px;
                border-radius: var(--evo-radius);
                font-family: var(--evo-font-family);
                font-size: 11px;
                outline: none;
                box-shadow: inset 0 1px 2px rgba(0,0,0,0.04);
                transition: border-color 0.15s, box-shadow 0.15s;
            }
            input[type="text"]:focus,
            input[type="number"]:focus,
            input[type="search"]:focus,
            select:focus,
            textarea:focus {
                border-color: var(--evo-border-focus);
                box-shadow: 0 0 0 1px var(--evo-border-focus);
            }
            select option {
                background-color: var(--evo-surface);
                color: var(--evo-text);
            }

            /* Workstation Tool Panels & Cards */
            .evo-panel, .panel {
                background-color: var(--evo-surface);
                border: 1px solid var(--evo-border);
                border-radius: var(--evo-radius);
                display: flex;
                flex-direction: column;
                box-shadow: var(--evo-shadow-sm);
                margin-bottom: 8px;
            }
            .evo-panel-header, .panel-header, .panel h3 {
                padding: 8px 12px;
                background: var(--evo-surface-alt);
                border-bottom: 1px solid var(--evo-border);
                border-left: 3px solid var(--evo-primary);
                font-weight: 700;
                font-size: 11px;
                text-transform: uppercase;
                letter-spacing: 0.5px;
                color: var(--evo-primary);
                display: flex;
                align-items: center;
                justify-content: space-between;
                margin: 0;
            }
            .evo-panel-body, .panel-body {
                padding: 12px;
                flex: 1;
                overflow: auto;
            }

            /* Toolbars */
            .evo-toolbar {
                display: flex;
                align-items: center;
                gap: 8px;
                padding: 6px 12px;
                background-color: var(--evo-surface-alt);
                border-bottom: 1px solid var(--evo-border);
            }

            /* Badges & Status */
            .evo-badge {
                display: inline-block;
                padding: 2px 7px;
                border-radius: var(--evo-radius);
                font-size: 10px;
                font-weight: 700;
                text-transform: uppercase;
                letter-spacing: 0.3px;
                background-color: var(--evo-surface-alt);
                color: var(--evo-text-secondary);
                border: 1px solid var(--evo-border);
            }
            .evo-status-success {
                background-color: var(--evo-success-light);
                color: var(--evo-success);
                border-color: var(--evo-success);
            }
            .evo-status-warning {
                background-color: var(--evo-warning-light);
                color: var(--evo-warning);
                border-color: var(--evo-warning);
            }
            .evo-status-error {
                background-color: var(--evo-danger-light);
                color: var(--evo-danger);
                border-color: var(--evo-danger);
            }

            /* Tables */
            .evo-table, .data-table {
                width: 100%;
                border-collapse: collapse;
                font-size: 11px;
                background-color: var(--evo-surface);
            }
            .evo-table th, .data-table th,
            .evo-table td, .data-table td {
                border: 1px solid var(--evo-border);
                padding: 6px 10px;
                text-align: left;
                vertical-align: top;
            }
            .evo-table th, .data-table th {
                background-color: var(--evo-surface-alt);
                color: var(--evo-primary);
                font-weight: 700;
                position: sticky;
                top: 0;
                z-index: 1;
            }
            .evo-table tr:nth-child(even) {
                background-color: var(--evo-surface-alt);
            }
            .evo-table tr:hover, .data-table tr:hover {
                background-color: var(--evo-surface-selected);
            }

            /* Empty & Loading States */
            .evo-empty {
                text-align: center;
                color: var(--evo-text-muted);
                padding: 20px;
                font-style: italic;
            }
            .evo-loading {
                display: flex;
                align-items: center;
                justify-content: center;
                gap: 8px;
                padding: 15px;
                color: var(--evo-text-secondary);
            }
            """;
    }

    @Override
    public String getEvoStyleCss() {
        return getThemeCss() + "\n" + getBaseCss() + "\n" + getComponentCss();
    }

    @Override
    public String getArchitectureCss() {
        String archFileCss = FileTool.readResource("/architecture.css");
        if (archFileCss != null && !archFileCss.isBlank()) {
            return archFileCss;
        }
        return getEvoStyleCss();
    }

    @Override
    public String getChatCss() {
        return getEvoStyleCss();
    }

    @Override
    public String getForgeCss() {
        return getEvoStyleCss();
    }

    @Override
    public String getDevelopCss() {
        return getEvoStyleCss();
    }

    @Override
    public String getPageCss(String page) {
        if (page == null) return getEvoStyleCss();
        return switch (page.toLowerCase()) {
            case "architecture", "template" -> getArchitectureCss();
            case "chat" -> getChatCss();
            case "forge" -> getForgeCss();
            case "develop", "mutation" -> getDevelopCss();
            default -> getEvoStyleCss();
        };
    }

    @Override
    public String injectEvoStyle(String htmlTemplate) {
        if (htmlTemplate == null) return "";
        String styleBlock = "<style>\n" + getEvoStyleCss() + "\n</style>";
        if (htmlTemplate.contains("{{EVO_STYLE_CSS}}")) {
            return htmlTemplate.replace("{{EVO_STYLE_CSS}}", getEvoStyleCss());
        }
        if (htmlTemplate.contains("{{SHARED_CSS}}")) {
            return htmlTemplate.replace("{{SHARED_CSS}}", getEvoStyleCss());
        }
        if (htmlTemplate.contains("</head>")) {
            return htmlTemplate.replace("</head>", styleBlock + "\n</head>");
        }
        return styleBlock + "\n" + htmlTemplate;
    }
}
