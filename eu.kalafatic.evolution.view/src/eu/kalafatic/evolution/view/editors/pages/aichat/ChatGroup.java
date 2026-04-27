package eu.kalafatic.evolution.view.editors.pages.aichat;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
import eu.kalafatic.evolution.model.orchestration.ChatThread;
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
    private ChatThread currentThread;
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
            toolbar.setLayout(new GridLayout(4, false));

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
                refreshBrowser();
            }
        });

        setupBrowser(browser);
    }
    
    private void setupBrowser(Browser browser) {
        setupJavaScriptBridges();
        
        String html = loadHtmlTemplate("/chat.html");
        browser.setText(html);
    }

    private void refreshGitStatus() {
        if (orchestrator == null || page == null) return;
        File projectRoot = page.getProjectRoot();
        if (projectRoot == null) return;

        new Thread(() -> {
            try {
                eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider gitProvider = new eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider();
                List<String> changedFiles = gitProvider.getChangedFiles(projectRoot, "HEAD");
                JSONArray array = new JSONArray(changedFiles);
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
                eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider gitProvider = new eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider();
                String diff = gitProvider.getFileDiff(projectRoot, "HEAD", filePath);
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
        new BrowserFunction(browser, "JavaHandler") {
            @Override
            public Object function(Object[] args) {
                if (args.length < 3) return null;
                try {
                    String action = (String) args[0];
                    int index = Integer.parseInt((String) args[1]);
                    String text = (String) args[2];

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
            try (var reader = new BufferedReader(new InputStreamReader(is))) {
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
        if (currentThread != null && index >= 0 && index < currentThread.getMessages().size()) {
            ChatMessage msg = currentThread.getMessages().get(index);
            String agentType = msg.getAgentType();
            if (agentType == null) agentType = "ai";

            // If it was waiting, change it back to what it likely was
            if ("waiting".equals(agentType)) {
                agentType = "final-response";
            } else if (agentType.startsWith("waiting ")) {
                 agentType = agentType.replace("waiting ", "");
            }

            if (!agentType.contains("approved")) {
                msg.setAgentType(agentType + " approved");
            }
            refreshBrowser();
        }
    }

    public void markLastWaitingAsApproved() {
        if (currentThread != null) {
            List<ChatMessage> messages = currentThread.getMessages();
            for (int i = messages.size() - 1; i >= 0; i--) {
                ChatMessage msg = messages.get(i);
                if (msg.getAgentType() != null && msg.getAgentType().contains("waiting")) {
                    handleApprove(i);
                    return;
                }
            }
        }
    }

    public void setEditCallback(EditMessageCallback callback) {
        this.editCallback = callback;
    }

    public void appendText(String text, org.eclipse.swt.graphics.Color color, int style) {
        if (currentThread == null || text == null || text.trim().isEmpty()) return;
    	if (color == null) {
    		color = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
		}
    	
        String trimmedText = text.trim();
        String sender = "Evo";
        String content = trimmedText;
        String agentType = "ai";
        if (trimmedText.startsWith("You: ")) {
            sender = "You";
            content = trimmedText.substring(5);
            agentType = "user";
        } else if (trimmedText.startsWith("User [SELF-DEV]: ")) {
            sender = "User [SELF-DEV]";
            content = trimmedText.substring(17);
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
            else if (senderLower.contains("-response")) agentType = "response";

            if (content.toLowerCase().startsWith("[darwin]")) {
                agentType = "darwin";
            }

            if (content.contains("[DARWIN_BRANCHES]")) {
		agentType = "darwin-branches";
		content = content.replace("[DARWIN_BRANCHES]", "").trim();
            }

            if (content.toLowerCase().contains("waiting for user") ||
                content.toLowerCase().contains("guidance?") ||
                content.toLowerCase().contains("clarify") ||
                content.toLowerCase().contains("clarification") ||
                content.contains("[PROPOSAL:") ||
                content.toLowerCase().contains("ambiguous")) {
                agentType = "waiting";
            }
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

        try {
			String hexColor = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
			boolean isBold = (style & SWT.BOLD) != 0;
			boolean isItalic = (style & SWT.ITALIC) != 0;
			String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

            ChatMessage msg = OrchestrationFactory.eINSTANCE.createChatMessage();
            msg.setIndex(currentThread.getMessages().size());
            msg.setSender(sender);
            msg.setText(content);
            msg.setColor(hexColor);
            msg.setIsBold(isBold);
            msg.setIsItalic(isItalic);
            msg.setAgentType(agentType);
            msg.setTimestamp(timestamp);
			currentThread.getMessages().add(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
        refreshBrowser();
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
        if (currentThread != null && index >= 0 && index < currentThread.getMessages().size()) {
            currentThread.getMessages().get(index).setText(newText);
            refreshBrowser();
        }
    }

    public void setThinking(boolean show) {
        if (!isLoaded || browser.isDisposed()) return;
        browser.execute("showThinking(" + show + ");");
    }

    public void focusWaitingMessage() {
        if (!isLoaded || browser.isDisposed()) return;
        browser.execute("scrollToLastWaiting();");
    }

    private void refreshBrowser() {
        if (!isLoaded || browser.isDisposed()) return;
        refreshGitStatus();
        JSONArray array = new JSONArray();
        if (currentThread != null) {
            for (ChatMessage m : currentThread.getMessages()) {
                array.put(toJsonObject(m));
            }
        }
        String json = array.toString();
        // Pass the JSON object directly to the JS function
        browser.execute("updateMessages(" + json + ");");
    }

    private JSONObject toJsonObject(ChatMessage m) {
        JSONObject obj = new JSONObject();
        obj.put("index", m.getIndex());
        obj.put("sender", m.getSender());
        obj.put("text", m.getText());
        obj.put("color", m.getColor());
        obj.put("isBold", m.isIsBold());
        obj.put("isItalic", m.isIsItalic());
        obj.put("agentType", m.getAgentType() != null ? m.getAgentType() : "ai");
        obj.put("timestamp", m.getTimestamp() != null ? m.getTimestamp() : "");
        return obj;
    }

    public void clear() {
        if (currentThread != null) {
            currentThread.getMessages().clear();
        }
        refreshBrowser();
    }

    public String getText() {
        if (currentThread == null) return "";
        StringBuilder sb = new StringBuilder();
        for (ChatMessage m : currentThread.getMessages()) {
            sb.append(m.getSender()).append(": ").append(m.getText()).append("\n\n");
        }
        return sb.toString();
    }

    public void setThread(ChatThread thread) {
        this.currentThread = thread;
        refreshBrowser();
    }

    public void setText(String text) {
        if (currentThread == null) return;
        currentThread.getMessages().clear();
        if (text == null || text.isEmpty()) {
            refreshBrowser();
            return;
        }
        String[] lines = text.split("\n\n");
        for (String line : lines) {
            String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            ChatMessage msg = OrchestrationFactory.eINSTANCE.createChatMessage();
            msg.setIndex(currentThread.getMessages().size());
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
            currentThread.getMessages().add(msg);
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
