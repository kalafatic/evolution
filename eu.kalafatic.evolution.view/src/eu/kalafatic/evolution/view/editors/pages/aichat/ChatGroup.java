package eu.kalafatic.evolution.view.editors.pages.aichat;

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

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.AiChatPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class ChatGroup extends AEvoGroup {
    private Browser browser;
    private AiChatPage page;
    private boolean isLoaded = false;
    private List<ChatMessage> messages = new ArrayList<>();
    private EditMessageCallback editCallback;

    public interface EditMessageCallback {
        void onEditMessage(int index, String oldText);
    }

    private static class ChatMessage {
        int index;
        String sender;
        String text;
        String color;
        boolean isBold;
        boolean isItalic;
        String agentType;
        String timestamp;

        ChatMessage(int index, String sender, String text, String color, boolean isBold, boolean isItalic, String agentType, String timestamp) {
            this.index = index;
            this.sender = sender;
            this.text = text;
            this.color = color;
            this.isBold = isBold;
            this.isItalic = isItalic;
            this.agentType = agentType;
            this.timestamp = timestamp;
        }

        JSONObject toJsonObject() {
            JSONObject obj = new JSONObject();
            obj.put("index", index);
            obj.put("sender", sender);
            obj.put("text", text);
            obj.put("color", color);
            obj.put("isBold", isBold);
            obj.put("isItalic", isItalic);
            obj.put("agentType", agentType != null ? agentType : "ai");
            obj.put("timestamp", timestamp != null ? timestamp : "");
            return obj;
        }
    }

    public ChatGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, Font chatFont, AiChatPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent, chatFont);
    }

    @Override
    protected void refreshUI() {
        // Managed by AiChatPage
    }

    private void createControl(FormToolkit toolkit, Composite parent, Font chatFont) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Chat", 1, true);

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
                        case "approve":
                            page.provideApproval(true);
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

    public void setEditCallback(EditMessageCallback callback) {
        this.editCallback = callback;
    }

    public void appendText(String text, org.eclipse.swt.graphics.Color color, int style) {    	
    	if (color == null) {
    		color = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
		}
    	
        String sender = "Evo";
        String content = text;
        String agentType = "ai";
        if (text.startsWith("You: ")) {
            sender = "You";
            content = text.substring(5);
            agentType = "user";
        } else if (text.startsWith("User [SELF-DEV]: ")) {
            sender = "User [SELF-DEV]";
            content = text.substring(17);
            agentType = "user";
        } else if (text.startsWith("Final Response: ")) {
            sender = "Final Response";
            content = text.substring(16);
            agentType = "final-response";
        } else if (text.startsWith("Evo-") && text.contains(": ")) {
            int colon = text.indexOf(": ");
            sender = text.substring(0, colon);
            content = text.substring(colon + 2).trim();

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
        } else if (text.startsWith("Evolution: ")) {
            sender = "Evo";
            content = text.substring(11);
            agentType = "ai";
        } else if (text.startsWith("Evo: ")) {
            sender = "Evo";
            content = text.substring(5);
            agentType = "ai";
        } else if (text.contains("Agent [")) {
            int start = text.indexOf("Agent [");
            int end = text.indexOf("]", start);
            if (end != -1) {
                sender = text.substring(start, end + 1);
                content = text.substring(end + 1).trim();
                if (content.startsWith(":")) content = content.substring(1).trim();
                String agentName = sender.toLowerCase();
                if (agentName.contains("planner")) agentType = "planner";
                else if (agentName.contains("architect")) agentType = "architect";
                else if (agentName.contains("javadev")) agentType = "javadev";
                else if (agentName.contains("tester")) agentType = "tester";
                else if (agentName.contains("reviewer")) agentType = "reviewer";
            }
        } else if (text.contains("Tool [")) {
            int start = text.indexOf("Tool [");
            int end = text.indexOf("]", start);
            if (end != -1) {
                sender = text.substring(start, end + 1);
                content = text.substring(end + 1).trim();
                if (content.startsWith(":")) content = content.substring(1).trim();
                agentType = "tool";
            }
        }

        try {
			String hexColor = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
			boolean isBold = (style & SWT.BOLD) != 0;
			boolean isItalic = (style & SWT.ITALIC) != 0;
			String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

			messages.add(new ChatMessage(messages.size(), sender, content, hexColor, isBold, isItalic, agentType, timestamp));
		} catch (Exception e) {
			e.printStackTrace();
		}
        refreshBrowser();
    }

	public void updateMessage(int index, String newText) {
        if (index >= 0 && index < messages.size()) {
            messages.get(index).text = newText;
            refreshBrowser();
        }
    }

    public void setThinking(boolean show) {
        if (!isLoaded || browser.isDisposed()) return;
        browser.execute("showThinking(" + show + ");");
    }

    private void refreshBrowser() {
        if (!isLoaded || browser.isDisposed()) return;
        JSONArray array = new JSONArray();
        for (ChatMessage m : messages) {
            array.put(m.toJsonObject());
        }
        String json = array.toString();
        // Pass the JSON object directly to the JS function
        browser.execute("updateMessages(" + json + ");");
    }

    public void clear() {
        messages.clear();
        refreshBrowser();
    }

    public String getText() {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage m : messages) {
            sb.append(m.sender).append(": ").append(m.text).append("\n\n");
        }
        return sb.toString();
    }

    public void setText(String text) {
        clear();
        if (text == null || text.isEmpty()) return;
        String[] lines = text.split("\n\n");
        for (String line : lines) {
            String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            if (line.contains(": ")) {
                int colon = line.indexOf(": ");
                String sender = line.substring(0, colon);
                String content = line.substring(colon + 2);
                String agentType = sender.toLowerCase().contains("you") ? "user" : "ai";
                messages.add(new ChatMessage(messages.size(), sender, content, "#000000", false, false, agentType, timestamp));
            } else {
                messages.add(new ChatMessage(messages.size(), "System", line, "#666666", false, true, "ai", timestamp));
            }
        }
        refreshBrowser();
    }

    public StyleRange[] getStyleRanges() { return new StyleRange[0]; }
    public void setStyleRanges(StyleRange[] ranges) { }
    public void setSelection(int offset) { }
    public boolean isDisposed() { return browser.isDisposed(); }
}
