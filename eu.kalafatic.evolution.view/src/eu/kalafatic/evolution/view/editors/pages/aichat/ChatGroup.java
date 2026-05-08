package eu.kalafatic.evolution.view.editors.pages.aichat;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import java.net.URL;
import java.util.Collections;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.model.orchestration.ChatMessage;
import eu.kalafatic.evolution.model.orchestration.ChatSession;
import eu.kalafatic.evolution.model.orchestration.FeedbackLevel;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.AiChatPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

/**
 * @evo.lastModified: 13:A
 * @evo.origin: user
 */
public class ChatGroup extends AEvoGroup {
    private Browser browser;
    private AiChatPage page;
    private boolean isLoaded = false;
    private boolean isJsReady = false;
    private ChatSession currentSession;
    private EditMessageCallback editCallback;
    private int logCount = 0;

    public interface EditMessageCallback {
        void onEditMessage(int index, String oldText);
    }

    public ChatGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, Font chatFont, AiChatPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent, chatFont);
    }

    @Override
    public void refreshUI() {
        refreshBrowser();
    }

    // @evo:13:A reason=fill-chat-group
    private void createControl(FormToolkit toolkit, Composite parent, Font chatFont) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Chat", 1, true, true);

        if (group.getParent() instanceof Section) {
            Section section = (Section) group.getParent();
            Composite toolbar = toolkit.createComposite(section);
            toolbar.setLayout(new GridLayout(5, false));

            Button selectAllBtn = toolkit.createButton(toolbar, "Select All", SWT.PUSH);
            selectAllBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    browser.execute("selectAll();");
                }
            });

            Button toggleAllBtn = toolkit.createButton(toolbar, "\u2195 Expand/Collapse All", SWT.PUSH);
            toggleAllBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    browser.execute("toggleAll();");
                }
            });

            Button quoteSelectionBtn = toolkit.createButton(toolbar, "Quote Selection", SWT.PUSH);
            quoteSelectionBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    browser.execute("quoteSelection();");
                }
            });

            Button copySelectionBtn = toolkit.createButton(toolbar, "Copy Selection", SWT.PUSH);
            copySelectionBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    browser.execute("var sel = window.getSelection().toString(); if(sel) { JavaHandler('copy', '0', sel); }");
                }
            });

            SWTFactory.createMaximizeButton(toolbar, section, false);
            section.setTextClient(toolbar);
        }

        browser = new Browser(group, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 264;
        browser.setLayoutData(gd);

        browser.addProgressListener(new org.eclipse.swt.browser.ProgressAdapter() {
            @Override
            public void completed(org.eclipse.swt.browser.ProgressEvent event) {
                isLoaded = true;

                // 🔥 CRITICAL: re-register JS bridge AFTER page load
                setupJavaScriptBridges();

                // give JS a moment, then refresh
                Display.getDefault().timerExec(200, () -> {
                    refreshBrowser();
                });
            }
        });

        setupBrowser(browser);
    }
    
    private void setupBrowser(Browser browser) {
        setupJavaScriptBridges();
        
        try {
            Bundle bundle = Platform.getBundle("eu.kalafatic.evolution.view");
            if (bundle == null) {
                bundle = FrameworkUtil.getBundle(getClass());
            }

            if (bundle != null) {
                // We use setUrl because it's the most reliable way for SWT Browser to handle ES modules
                // and resolve relative paths (./js/...) correctly.
                // We MUST use FileLocator.toFileURL on the BUNDLE ROOT to ensure all JS/CSS files are extracted.
                URL bundleRoot = FileLocator.toFileURL(bundle.getEntry("/"));
                URL chatUrl = new URL(bundleRoot, "chat.html");
                //browser.setUrl(chatUrl.toString());
                
                String html = loadHtmlTemplate("/chat.html");

             // critical: inject base path so relative JS works
             //URL bundleRoot = FileLocator.toFileURL(bundle.getEntry("/"));
             String base = bundleRoot.toString();

             html = html.replace(
                 "<head>",
                 "<head><base href=\"" + base + "\">"
             );

             browser.setText(html, true); // trusted = allow scripts
            } else {
                throw new Exception("Bundle not found");
            }
        } catch (Exception e) {
            System.err.println("Failed to load chat.html via setUrl: " + e.getMessage());
            String html = loadHtmlTemplate("/chat.html");
            browser.setText(html);
        }
    }

    public void refreshGitStatus() {
        if (orchestrator == null || page == null) return;
        File projectRoot = page.getProjectRoot();
        if (projectRoot == null) return;

        new Thread(() -> {
            try {
                eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider gitProvider = new eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider();
                List<String> changedFiles = gitProvider.getChangedFiles(projectRoot, "HEAD");

                JSONArray array = new JSONArray();
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (String fileInfo : changedFiles) {
                    JSONObject obj = new JSONObject();
                    obj.put("info", fileInfo);

                    String path = fileInfo;
                    if (path.length() > 2 && path.charAt(1) == ' ') {
                        path = path.substring(2);
                    }
                    File file = new File(projectRoot, path);
                    if (file.exists()) {
                        obj.put("date", sdf.format(new java.util.Date(file.lastModified())));
                    } else {
                        obj.put("date", "");
                    }
                    array.put(obj);
                }

                Display.getDefault().asyncExec(() -> {
                    if (browser != null && !browser.isDisposed()) {
                        browser.execute("updateChanges(" + array.toString() + ");");
                    }
                });
            } catch (Exception e) {
                // Ignore git errors in background refresh
            }
        }).start();
    }

    private void revertGitFile(String filePath) {
        if (page == null) return;
        File projectRoot = page.getProjectRoot();
        if (projectRoot == null) return;

        new Thread(() -> {
            try {
                eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider gitProvider = new eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider();
                gitProvider.revertFile(projectRoot, filePath);
                refreshGitStatus();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void openInComparePage(String filePath) {
        if (page == null) return;

        Display.getDefault().asyncExec(() -> {
            try {
                org.eclipse.core.resources.IProject project = null;
                if (editor.getEditorInput() instanceof org.eclipse.ui.IFileEditorInput) {
                    project = ((org.eclipse.ui.IFileEditorInput) editor.getEditorInput()).getFile().getProject();
                }
                if (project != null) {
                    org.eclipse.core.resources.IFile file = project.getFile(new org.eclipse.core.runtime.Path(filePath));
                    if (file.exists()) {
                        editor.showComparePage(file);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void downloadChangesAsZip() {
        if (page == null) return;
        File projectRoot = page.getProjectRoot();
        if (projectRoot == null) return;

        Display.getDefault().asyncExec(() -> {
            org.eclipse.swt.widgets.FileDialog dialog = new org.eclipse.swt.widgets.FileDialog(page.getShell(), SWT.SAVE);
            dialog.setFilterExtensions(new String[]{"*.zip"});
            dialog.setFileName("changes_" + System.currentTimeMillis() + ".zip");
            String zipPath = dialog.open();
            if (zipPath != null) {
                new Thread(() -> {
                    try {
                        eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider gitProvider = new eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider();
                        List<String> files = gitProvider.getChangedFiles(projectRoot, "HEAD");
                        if (files.isEmpty()) {
                            Display.getDefault().asyncExec(() -> {
                                org.eclipse.swt.widgets.MessageBox mb = new org.eclipse.swt.widgets.MessageBox(page.getShell(), SWT.ICON_WARNING | SWT.OK);
                                mb.setText("No Changes");
                                mb.setMessage("No changed files to include in ZIP.");
                                mb.open();
                            });
                            return;
                        }

                        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(zipPath))) {
                            for (String filePath : files) {
                                // Strip status prefix if present
                                String path = filePath;
                                if (path.length() > 2 && (path.startsWith("M ") || path.startsWith("A ") || path.startsWith("D "))) {
                                    path = path.substring(2);
                                }

                                File file = new File(projectRoot, path);
                                if (file.exists() && file.isFile()) {
                                    zos.putNextEntry(new java.util.zip.ZipEntry(path));
                                    java.nio.file.Files.copy(file.toPath(), zos);
                                    zos.closeEntry();
                                }
                            }
                        }

                        Display.getDefault().asyncExec(() -> {
                            org.eclipse.swt.widgets.MessageBox mb = new org.eclipse.swt.widgets.MessageBox(page.getShell(), SWT.ICON_INFORMATION | SWT.OK);
                            mb.setText("ZIP Created");
                            mb.setMessage("Successfully created ZIP with " + files.size() + " files at:\n" + zipPath);
                            mb.open();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        });
    }

    private void commitGitChanges(String message) {
        if (page == null) return;
        File projectRoot = page.getProjectRoot();
        if (projectRoot == null) return;

        new Thread(() -> {
            try {
                eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider gitProvider = new eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider();
                gitProvider.commitChanges(projectRoot, message);
                refreshGitStatus();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void fetchDiff(String filePath) {
        if (page == null) return;
        File projectRoot = page.getProjectRoot();
        if (projectRoot == null) return;

        new Thread(() -> {
            try {
                // Strip status prefix if present
                String path = filePath;
                if (path.length() > 2 && (path.startsWith("M ") || path.startsWith("A ") || path.startsWith("D "))) {
                    path = path.substring(2);
                }

                eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider gitProvider = new eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider();
                String diff = gitProvider.getFileDiff(projectRoot, "HEAD", path);
                Display.getDefault().asyncExec(() -> {
                    if (browser != null && !browser.isDisposed()) {
                        JSONObject json = new JSONObject();
                        json.put("path", filePath);
                        json.put("diff", diff);
                        browser.execute("showDiff(" + json.toString() + ");");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void setupJavaScriptBridges() {
        // Logging bridge
        new BrowserFunction(browser, "JavaLog") {
            @Override
            public Object function(Object[] args) {
                if (args.length > 0) {
                    System.out.println("[Chat Browser] " + args[0]);
                }
                return null;
            }
        };

        new BrowserFunction(browser, "JavaHandler") {
            @Override
            public Object function(Object[] args) {
                if (args.length < 1) return null;
                try {
                    String action = (String) args[0];

                    if ("ready".equals(action)) {
                        isLoaded = true;
                        isJsReady = true;
                        refreshBrowser();
                        return null;
                    }

                    if (args.length < 2) return null;
                    String indexStr = (String) args[1];
                    int index = -1;
                    try {
                        index = Integer.parseInt(indexStr);
                    } catch (NumberFormatException e) {
                        // Safe fallback for non-numeric indices (like "undefined" or null)
                    }

                    String text = args.length >= 3 ? (String) args[2] : "";

                    switch (action) {
                        case "edit":
                            handleEdit(index, text);
                            break;
                        case "copy":
                            handleCopy(text);
                            break;
                        case "quote":
                            page.handleQuote(text);
                            break;
                        case "approve":
                            handleApprove(index);
                            page.provideApproval(true);
                            break;
                        case "approveDarwinVariant":
                            handleApproveDarwinVariant(index, text);
                            page.handleExecuteProposal("Approve variant " + text);
                            break;
                        case "create":
                            page.provideApproval(true);
                            break;
                        case "clarify":
                            page.handleClarify();
                            break;
                        case "helloworld":
                            page.handleSimpleSolution();
                            break;
                        case "executeProposal":
                            page.handleExecuteProposal(text);
                            break;
                        case "openDiff":
                            page.handleOpenDiff(text);
                            break;
                        case "getDiff":
                            fetchDiff(text);
                            break;
                        case "refreshGit":
                            refreshGitStatus();
                            break;
                        case "commitGit":
                            commitGitChanges(text);
                            break;
                        case "revertFile":
                            // Strip status prefix if present
                            String revertPath = text;
                            if (revertPath.length() > 2 && (revertPath.startsWith("M ") || revertPath.startsWith("A ") || revertPath.startsWith("D "))) {
                                revertPath = revertPath.substring(2);
                            }
                            ChatGroup.this.revertGitFile(revertPath);
                            break;
                        case "downloadZip":
                            ChatGroup.this.downloadChangesAsZip();
                            break;
                        case "openInWorkspace":
                            page.handleOpenDiff(text);
                            break;
                        case "openInReviewEditor":
                            // Strip status prefix if present
                            String reviewPath = text;
                            if (reviewPath.length() > 2 && (reviewPath.startsWith("M ") || reviewPath.startsWith("A ") || reviewPath.startsWith("D "))) {
                                reviewPath = reviewPath.substring(2);
                            }
                            ChatGroup.this.openInComparePage(reviewPath);
                            break;
                        case "openPeerReview":
                            editor.showPeerReviewPage();
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
    }
    
    private String loadHtmlTemplate(String path) {
        try (var is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                return "<html><body>Error: Template not found at " + path + "</body></html>";
            }
            try (var reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            return "<html><body>Exception: " + e.getMessage() + "</body></html>";
        }
    }
    
    private void handleEdit(int index, String oldText) {
        if (editCallback != null) {
            Display.getDefault().asyncExec(() -> {
                editCallback.onEditMessage(index, oldText);
            });
        }
    }

    private void handleCopy(String text) {
        org.eclipse.swt.dnd.Clipboard cb = new org.eclipse.swt.dnd.Clipboard(browser.getDisplay());
        cb.setContents(new Object[]{text}, new org.eclipse.swt.dnd.Transfer[]{org.eclipse.swt.dnd.TextTransfer.getInstance()});
        cb.dispose();
    }

    public void handleApprove(int index) {
        if (currentSession != null && index >= 0 && index < currentSession.getMessages().size()) {
            ChatMessage msg = currentSession.getMessages().get(index);
            String agentType = msg.getAgentType();
            if (agentType == null) agentType = "ai";

            // Clean up waiting status
            agentType = agentType.replace("waiting", "").trim();
            if (agentType.isEmpty()) agentType = "final-response";

            if (!agentType.contains("approved")) {
                msg.setAgentType(agentType + " approved");
            }
            refreshBrowser();
        }
    }

    public void handleApproveDarwinVariant(int index, String variantId) {
        if (currentSession != null && index >= 0 && index < currentSession.getMessages().size()) {
            ChatMessage msg = currentSession.getMessages().get(index);
            String agentType = msg.getAgentType();
            if (agentType == null) agentType = "darwin";

            agentType = agentType.replace("waiting", "").trim();
            if (agentType.isEmpty()) agentType = "darwin";

            if (!agentType.contains("approved")) {
                msg.setAgentType(agentType + " approved:" + variantId);
            }
            refreshBrowser();
        }
    }

    public void markLastWaitingAsApproved() {
        if (currentSession != null) {
            List<ChatMessage> messages = currentSession.getMessages();
            for (int i = messages.size() - 1; i >= 0; i--) {
                ChatMessage msg = messages.get(i);
                if (msg.getAgentType() != null && msg.getAgentType().contains("waiting")) {
                    handleApprove(i);
                    return;
                }
            }
        }
    }

    public void markLastAiMessageAsWaiting() {
        if (currentSession != null) {
            List<ChatMessage> messages = currentSession.getMessages();
            for (int i = messages.size() - 1; i >= 0; i--) {
                ChatMessage msg = messages.get(i);
                String agentType = msg.getAgentType();
                if (agentType != null && !agentType.contains("user") && !agentType.contains("approved")) {
                    if (!agentType.contains("waiting")) {
                        msg.setAgentType(agentType + " waiting");
                    }
                    refreshBrowser();
                    return;
                }
            }
        }
    }

    public void setEditCallback(EditMessageCallback callback) {
        this.editCallback = callback;
    }

    public void appendText(String text, org.eclipse.swt.graphics.Color color, int style) {
        appendTextToSession(currentSession, text, color, style);
    }

    public void appendTextToSession(ChatSession thread, String text, org.eclipse.swt.graphics.Color color, int style) {
        if (thread == null || text == null || text.trim().isEmpty()) return;
    	if (color == null) {
    		color = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
		}
    	
        String trimmedText = text.trim();
        String sender = "Evo";
        String content = trimmedText;
        String agentType = "ai";
        String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

        // Match patterns like:
        // USER [DARWIN] [12:14:11]: create java class...
        // EVO [12:14:11]: Initializing DARWIN loop...
        // TOOL [SHELLTOOL] [12:14:11]: Running git...
        // LLMROUTER-LOCAL [12:14:14]: Using Ollama...
        // EVO-DARWINENGINE-THINKING [12:14:14]: Role: DarwinEngine...
        java.util.regex.Pattern logPattern = java.util.regex.Pattern.compile("^([A-Z][A-Z0-9-]*)(?:\\s+\\[([^\\]]*)\\])?(?:\\s+\\[(\\d{2}:\\d{2}:\\d{2})\\])?:\\s*([\\s\\S]*)$", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = logPattern.matcher(trimmedText);

        if (matcher.find()) {
            sender = matcher.group(1);
            String extra = matcher.group(2);
            String foundTime = matcher.group(3);
            content = matcher.group(4);

            if (extra != null && extra.matches("\\d{2}:\\d{2}:\\d{2}") && foundTime == null) {
                foundTime = extra;
                extra = null;
            }

            if (foundTime != null) timestamp = foundTime;

            String senderUpper = sender.toUpperCase();
            if (senderUpper.startsWith("USER")) agentType = "user";
            else if (senderUpper.startsWith("EVO")) agentType = "ai";
            else if (senderUpper.startsWith("TOOL")) agentType = "tool";
            else if (senderUpper.startsWith("LLMROUTER")) agentType = "orchestrator";

            String agentSource = (sender + (extra != null ? "-" + extra : "")).toLowerCase();
            if (agentSource.contains("planner")) agentType = "planner";
            else if (agentSource.contains("architect")) agentType = "architect";
            else if (agentSource.contains("javadev")) agentType = "javadev";
            else if (agentSource.contains("tester")) agentType = "tester";
            else if (agentSource.contains("reviewer")) agentType = "reviewer";
            else if (agentSource.contains("analytic") || agentSource.contains("analysis")) agentType = "analytic";
            else if (agentSource.contains("general")) agentType = "general";
            else if (agentSource.contains("terminal")) agentType = "terminal";
            else if (agentSource.contains("file")) agentType = "file";
            else if (agentSource.contains("maven")) agentType = "maven";
            else if (agentSource.contains("git")) agentType = "git";
            else if (agentSource.contains("structure")) agentType = "structure";
            else if (agentSource.contains("websearch")) agentType = "websearch";
            else if (agentSource.contains("quality")) agentType = "quality";
            else if (agentSource.contains("observability")) agentType = "observability";
            else if (agentSource.contains("orchestrator")) agentType = "orchestrator";
            else if (agentSource.contains("darwinengine")) agentType = "darwin";

            if (agentSource.contains("thinking")) agentType = "thinking";
            else if (agentSource.contains("response") && !agentType.equals("darwin")) agentType = "response";
        } else if (trimmedText.startsWith("You: ")) {
            sender = "You";
            content = trimmedText.substring(5);
            agentType = "user";
        } else if (trimmedText.startsWith("User [") && trimmedText.contains("]: ")) {
            int closeBracket = trimmedText.indexOf("]: ");
            sender = trimmedText.substring(0, closeBracket + 1);
            content = trimmedText.substring(closeBracket + 3);
            agentType = "user";
        } else if (trimmedText.startsWith("Final Response: ")) {
            sender = "Final Response";
            content = trimmedText.substring(16);
            agentType = "final-response";
            if (content.contains("CLARIFY:")) {
                agentType = "waiting";
            }
        } else if (trimmedText.startsWith("Error: ")) {
            sender = "Error";
            content = trimmedText.substring(7);
            agentType = "error";
        } else if (trimmedText.startsWith("Result Summary: ")) {
            sender = "Result Summary";
            content = trimmedText.substring(16);
            agentType = "result-summary";
        } else if (trimmedText.startsWith("Evo-") && trimmedText.contains(": ")) {
            int colon = trimmedText.indexOf(": ");
            sender = trimmedText.substring(0, colon);
            content = trimmedText.substring(colon + 2).trim();

            String senderLower = sender.toLowerCase();
            if (senderLower.contains("-planner-")) agentType = "planner";
            else if (senderLower.contains("-architect-")) agentType = "architect";
            else if (senderLower.contains("-javadev-")) agentType = "javadev";
            else if (senderLower.contains("-tester-")) agentType = "tester";
            else if (senderLower.contains("-reviewer-")) agentType = "reviewer";
            else if (senderLower.contains("-analytic-") || senderLower.contains("-analysis-")) agentType = "analytic";
            else if (senderLower.contains("-general-")) agentType = "general";
            else if (senderLower.contains("-terminal-")) agentType = "terminal";
            else if (senderLower.contains("-file-")) agentType = "file";
            else if (senderLower.contains("-maven-")) agentType = "maven";
            else if (senderLower.contains("-git-")) agentType = "git";
            else if (senderLower.contains("-structure-")) agentType = "structure";
            else if (senderLower.contains("-websearch-")) agentType = "websearch";
            else if (senderLower.contains("-quality-")) agentType = "quality";
            else if (senderLower.contains("-observability-")) agentType = "observability";
            else if (senderLower.contains("-orchestrator-")) agentType = "orchestrator";
            else if (senderLower.contains("-darwinengine-")) agentType = "darwin";

            if (senderLower.contains("-thinking")) agentType = "thinking";
            else if (senderLower.contains("-response") && !agentType.equals("darwin")) agentType = "response";
        } else if (trimmedText.startsWith("Evolution: ")) {
            sender = "Evo";
            content = trimmedText.substring(11);
            agentType = "ai";
        } else if (trimmedText.startsWith("Evo: ")) {
            sender = "Evo";
            content = trimmedText.substring(5);
            agentType = "ai";
        } else if (trimmedText.startsWith("User Interaction: ")) {
            sender = "User Interaction";
            content = trimmedText.substring(18);
            agentType = "user";
        } else if (trimmedText.startsWith("LlmRouter: ") || trimmedText.startsWith("LlmRouter-")) {
            int colon = trimmedText.indexOf(": ");
            sender = trimmedText.substring(0, colon);
            content = trimmedText.substring(colon + 2);
            agentType = "orchestrator";
        } else if (trimmedText.contains("Agent [")) {
            int start = trimmedText.indexOf("Agent [");
            int end = trimmedText.indexOf("]", start);
            if (end != -1) {
                sender = trimmedText.substring(start, end + 1);
                content = trimmedText.substring(end + 1).trim();
                if (content.startsWith(":")) content = content.substring(1).trim();
                String agentName = sender.toLowerCase();
                if (agentName.contains("planner")) agentType = "planner";
                else if (agentName.contains("architect")) agentType = "architect";
                else if (agentName.contains("javadev")) agentType = "javadev";
                else if (agentName.contains("tester")) agentType = "tester";
                else if (agentName.contains("reviewer")) agentType = "reviewer";
            }
        } else if (trimmedText.contains("Tool [")) {
            int start = trimmedText.indexOf("Tool [");
            int end = trimmedText.indexOf("]", start);
            if (end != -1) {
                sender = trimmedText.substring(start, end + 1);
                content = trimmedText.substring(end + 1).trim();
                if (content.startsWith(":")) content = content.substring(1).trim();
                agentType = "tool";
            }
        }

        if (content.contains("[DARWIN_BRANCHES]")) {
            agentType = "darwin-branches";
            content = content.replace("[DARWIN_BRANCHES]", "").trim();
        } else if (sender.toLowerCase().contains("-darwinengine-") || sender.toLowerCase().equals("evo-darwinengine")) {
            if (!agentType.equals("thinking")) {
                agentType = "darwin";
            }
        }

        boolean needsApproval = content.toLowerCase().contains("waiting for user") ||
            content.toLowerCase().contains("guidance?") ||
            content.toLowerCase().contains("clarify") ||
            content.toLowerCase().contains("clarification") ||
            content.contains("[PROPOSAL:") ||
            content.toLowerCase().contains("ambiguous") ||
            content.toLowerCase().contains("approve") ||
            content.toLowerCase().contains("approval") ||
            content.toLowerCase().contains("proceed?");

        if (agentType.equals("darwin-branches")) {
            needsApproval = true;
        }

        if (needsApproval && !agentType.contains("waiting") && !agentType.contains("user")) {
            agentType += " waiting";
        }

        // Downgrade previous waiting messages to avoid multiple pulsing bubbles
        for (ChatMessage existing : thread.getMessages()) {
            if (existing.getAgentType() != null && existing.getAgentType().contains("waiting")) {
                existing.setAgentType("response");
            }
        }

        // Clean up technical markers for human-readability
        content = content.replaceAll("\\[KERNEL\\]", "")
                        .replaceAll("\\[STRATEGY\\]", "")
                        .replaceAll("\\[ANALYSIS\\]", "")
                        .replaceAll("\\[DIAGNOSIS\\]", "")
                        .replaceAll("\\[SUPERVISOR\\]", "")
                        .replaceAll("\\[EVO\\]", "")
                        .replaceAll("\\[DARWIN\\]", "")
                        .replaceAll("\\[DARWINENGINE\\]", "")
                        .replaceAll("\\[THINKING\\]", "")
                        .replaceAll("\\[ORCHESTRATOR\\]", "")
                        .trim();

        String finalContent = content;

        try {
            final String fSender = sender;
            final String fContent = finalContent;
            final String fAgentType = agentType;
            final String fHexColor = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
            final boolean isBold = (style & SWT.BOLD) != 0;
            final boolean isItalic = (style & SWT.ITALIC) != 0;
            final String fTimestamp = timestamp;
            Display.getDefault().asyncExec(() -> {
                try {
                    ChatMessage msg = OrchestrationFactory.eINSTANCE.createChatMessage();
                    msg.setIndex(thread.getMessages().size());
                    msg.setSender(fSender);
                    msg.setText(fContent);
                    msg.setColor(fHexColor);
                    msg.setIsBold(isBold);
                    msg.setIsItalic(isItalic);
                    msg.setAgentType(fAgentType);
                    msg.setTimestamp(fTimestamp);
                    thread.getMessages().add(msg);

                    if (thread == currentSession) {
                        refreshBrowser();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getIconForAgentType(String agentType) {
	if (agentType == null) return "🤖";
	switch (agentType) {
		case "user": return "👤";
		case "ai": return "🤖";
		case "planner": return "📋";
		case "architect": return "📐";
		case "javadev": return "💻";
		case "tester": return "🧪";
		case "reviewer": return "⚖️";
		case "tool": return "⚙️";
		case "analytic": return "🔍";
		case "general": return "🧠";
		case "terminal": return "📟";
		case "file": return "📂";
		case "maven": return "📦";
		case "git": return "🌿";
		case "structure": return "🌳";
		case "websearch": return "🌐";
		case "quality": return "✨";
		case "observability": return "📊";
		case "orchestrator": return "🎼";
		case "darwin": return "🧬";
		case "darwin-branches": return "🧬";
		case "final-response": return "✅";
		case "result-summary": return "ℹ️";
		case "waiting": return "❓";
		case "error": return "❌";
		case "thinking": return "💭";
		case "response": return "💬";
		default: return "🤖";
	}
    }

	public void updateMessage(int index, String newText) {
        if (currentSession != null && index >= 0 && index < currentSession.getMessages().size()) {
            currentSession.getMessages().get(index).setText(newText);
            refreshBrowser();
        }
    }

    public void setThinking(boolean show) {
        if (!isLoaded || browser.isDisposed()) return;
        browser.execute("showThinking(" + show + ");");
    }

    public void setFeedbackLevel(FeedbackLevel level) {
        if (!isLoaded || browser.isDisposed() || level == null) return;
        browser.execute("setFeedbackLevel('" + level.getName().toLowerCase() + "');");
    }

    public void focusWaitingMessage() {
        if (!isLoaded || browser.isDisposed()) return;
        browser.execute("scrollToLastWaiting();");
    }

    public void selectFile(String path) {
        if (!isLoaded || browser.isDisposed()) return;
        browser.execute("if(window.ChatApp && window.ChatApp.Panel) { window.ChatApp.Panel.selectFile('" + path + "'); }");
    }

    private void refreshBrowser() {
    	if (browser.isDisposed()) return;
        refreshGitStatus();
        if (orchestrator != null && !orchestrator.getTasks().isEmpty()) {
            setFeedbackLevel(orchestrator.getTasks().get(0).getFeedbackLevel());
        }
        JSONArray array = new JSONArray();
        if (currentSession != null) {
            for (ChatMessage m : currentSession.getMessages()) {
                JSONObject json = toJsonObject(m);
                if (json != null) array.put(json);
            }
        }
        String json = array.toString();
        // Pass the JSON object directly to the JS function as suggested
        //browser.execute("if(window.updateMessages) { window.updateMessages(" + json + "); }");
  
        browser.execute(
                "if(window.updateMessages) {" +
                "  window.updateMessages(" + json + ");" +
                "} else {" +
                "  console.log('updateMessages not ready');" +
                "}"
            );
    
    }

    private JSONObject toJsonObject(ChatMessage m) {
        if (m == null) return null;
        JSONObject obj = new JSONObject();
        obj.put("index", m.getIndex());
        obj.put("sender", m.getSender() != null ? m.getSender() : "System");
        obj.put("text", m.getText() != null ? m.getText() : "");
        obj.put("color", m.getColor() != null ? m.getColor() : "#000000");
        obj.put("isBold", m.isIsBold());
        obj.put("isItalic", m.isIsItalic());
        obj.put("agentType", m.getAgentType() != null ? m.getAgentType() : "ai");
        obj.put("timestamp", m.getTimestamp() != null ? m.getTimestamp() : "");
        return obj;
    }

    public void clear() {
        if (currentSession != null) {
            currentSession.getMessages().clear();
        }
        refreshBrowser();
    }

    public String getText() {
        if (currentSession == null) return "";
        StringBuilder sb = new StringBuilder();
        for (ChatMessage m : currentSession.getMessages()) {
            sb.append(m.getSender()).append(": ").append(m.getText()).append("\n\n");
        }
        return sb.toString();
    }

    public void setSession(ChatSession thread) {
        this.currentSession = thread;
        refreshBrowser();
    }

    public void setText(String text) {
        if (currentSession == null) return;
        currentSession.getMessages().clear();
        if (text == null || text.isEmpty()) {
            refreshBrowser();
            return;
        }
        String[] lines = text.split("\n\n");
        for (String line : lines) {
            String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            ChatMessage msg = OrchestrationFactory.eINSTANCE.createChatMessage();
            msg.setIndex(currentSession.getMessages().size());
            if (line.contains(": ")) {
                int colon = line.indexOf(": ");
                String sender = line.substring(0, colon);
                String content = line.substring(colon + 2);
                String agentType = sender.toLowerCase().contains("you") ? "user" : "ai";
                msg.setSender(sender);
                msg.setText(content);
                msg.setColor("#000000");
                msg.setIsBold(false);
                msg.setIsItalic(false);
                msg.setAgentType(agentType);
                msg.setTimestamp(timestamp);
            } else {
                msg.setSender("System");
                msg.setText(line);
                msg.setColor("#666666");
                msg.setIsBold(false);
                msg.setIsItalic(true);
                msg.setAgentType("ai");
                msg.setTimestamp(timestamp);
            }
            currentSession.getMessages().add(msg);
        }
        refreshBrowser();
    }

    public StyleRange[] getStyleRanges() { return new StyleRange[0]; }
    public void setStyleRanges(StyleRange[] ranges) { }
    public void setSelection(int offset) { }
    public boolean isDisposed() { return browser.isDisposed(); }

    public Composite getControl() { return group; }

    public int getLogCount() { return logCount; }
    public void incrementLogCount() { logCount++; }
    public void resetLogCount() { logCount = 0; }
}
