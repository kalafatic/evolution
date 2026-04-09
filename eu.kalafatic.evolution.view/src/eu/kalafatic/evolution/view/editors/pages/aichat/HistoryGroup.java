package eu.kalafatic.evolution.view.editors.pages.aichat;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.view.factories.SWTFactory;

public class HistoryGroup {
    private Composite group;
    private Browser browser;
    private boolean isLoaded = false;
    private List<ChatMessage> messages = new ArrayList<>();
    private EditMessageCallback editCallback;

    public interface EditMessageCallback {
        void onEditMessage(int index, String oldText);
    }

    private static class ChatMessage {
        String sender;
        String text;
        String color;
        boolean isBold;
        boolean isItalic;
        String agentType;
        String timestamp;

        ChatMessage(String sender, String text, String color, boolean isBold, boolean isItalic, String agentType, String timestamp) {
            this.sender = sender;
            this.text = text;
            this.color = color;
            this.isBold = isBold;
            this.isItalic = isItalic;
            this.agentType = agentType;
            this.timestamp = timestamp;
        }
    }

    public HistoryGroup(FormToolkit toolkit, Composite parent, Font chatFont) {
        createControl(toolkit, parent, chatFont);
    }

    private void createControl(FormToolkit toolkit, Composite parent, Font chatFont) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Conversation History", 1, true);
        browser = new Browser(group, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 400;
        browser.setLayoutData(gd);

        browser.addProgressListener(new org.eclipse.swt.browser.ProgressAdapter() {
            @Override
            public void completed(org.eclipse.swt.browser.ProgressEvent event) {
                isLoaded = true;
                refreshBrowser();
            }
        });

        new BrowserFunction(browser, "callEditMessage") {
            @Override
            public Object function(Object[] arguments) {
                if (arguments.length >= 2 && editCallback != null) {
                    int index = ((Double) arguments[0]).intValue();
                    String oldText = (String) arguments[1];
                    editCallback.onEditMessage(index, oldText);
                }
                return null;
            }
        };

        new BrowserFunction(browser, "copyToClipboard") {
            @Override
            public Object function(Object[] arguments) {
                if (arguments.length >= 1) {
                    String text = (String) arguments[0];
                    org.eclipse.swt.dnd.Clipboard cb = new org.eclipse.swt.dnd.Clipboard(browser.getDisplay());
                    cb.setContents(new Object[] { text }, new org.eclipse.swt.dnd.Transfer[] { org.eclipse.swt.dnd.TextTransfer.getInstance() });
                    cb.dispose();
                }
                return null;
            }
        };

        browser.setText(getHtmlTemplate());
    }

    public void setEditCallback(EditMessageCallback callback) {
        this.editCallback = callback;
    }

    public void appendText(String text, org.eclipse.swt.graphics.Color color, int style) {
    	if(color == null) {
    		Display display = Display.getCurrent(); // safer in UI thread
    		color = display.getSystemColor(SWT.COLOR_BLACK);
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
        	if (color == null) {
				color = browser.getDisplay().getSystemColor(SWT.COLOR_BLACK);
				style = SWT.NORMAL;				
			}
			String hexColor = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
			boolean isBold = (style & SWT.BOLD) != 0;
			boolean isItalic = (style & SWT.ITALIC) != 0;
			String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

			messages.add(new ChatMessage(sender, content, hexColor, isBold, isItalic, agentType, timestamp));
		} catch (Exception e) {
			// TODO Auto-generated catch block
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
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage m = messages.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"index\":").append(i).append(",");
            sb.append("\"sender\":\"").append(escapeJs(m.sender)).append("\",");
            sb.append("\"text\":\"").append(escapeJs(m.text)).append("\",");
            sb.append("\"color\":\"").append(m.color).append("\",");
            sb.append("\"isBold\":").append(m.isBold).append(",");
            sb.append("\"isItalic\":").append(m.isItalic).append(",");
            sb.append("\"agentType\":\"").append(m.agentType != null ? m.agentType : "ai").append("\",");
            sb.append("\"timestamp\":\"").append(m.timestamp != null ? m.timestamp : "").append("\"");
            sb.append("}");
        }
        sb.append("]");
        browser.execute("updateMessages(" + sb.toString() + ");");
    }

    private String escapeJs(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
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
        // Simple implementation for compatibility
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
                messages.add(new ChatMessage(sender, content, "#000000", false, false, agentType, timestamp));
            } else {
                messages.add(new ChatMessage("System", line, "#666666", false, true, "ai", timestamp));
            }
        }
        refreshBrowser();
    }

    public StyleRange[] getStyleRanges() { return new StyleRange[0]; }
    public void setStyleRanges(StyleRange[] ranges) { }
    public void setSelection(int offset) { }
    public boolean isDisposed() { return browser.isDisposed(); }

    private String getHtmlTemplate() {
        return "<!DOCTYPE html><html><head><style>"
            + "body { font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f8fafc; margin: 0; padding: 20px; overflow-y: auto; color: #1e293b; }"
            + ".chat-container { display: flex; flex-direction: column; gap: 20px; max-width: 900px; margin: 0 auto; }"
            + ".message { display: flex; flex-direction: column; max-width: 85%; position: relative; animation: slideIn 0.3s ease-out; }"
            + ".message.user { align-self: flex-end; }"
            + ".message.ai { align-self: flex-start; }"
            + ".bubble { padding: 14px 18px; border-radius: 16px; font-size: 14px; line-height: 1.6; position: relative; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px -1px rgba(0,0,0,0.06); transition: all 0.2s; background: white; border: 1px solid #e2e8f0; }"
            + ".message.user .bubble { background: linear-gradient(135deg, #2563eb, #1d4ed8); color: white; border-bottom-right-radius: 4px; border: none; }"
            + ".message.ai .bubble { border-bottom-left-radius: 4px; }"
            + ".bubble:hover { transform: translateY(-1px); box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1); }"
            + ".header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }"
            + ".user .header { flex-direction: row-reverse; }"
            + ".sender { font-size: 12px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em; }"
            + ".user .sender { color: #2563eb; }"
            + ".timestamp { font-size: 10px; color: #94a3b8; }"
            + ".icon { font-size: 16px; }"
            + ".actions { position: absolute; top: 0; right: -40px; display: flex; flex-direction: column; gap: 4px; opacity: 0; transition: opacity 0.2s; }"
            + ".message.user .actions { left: -40px; right: auto; }"
            + ".message:hover .actions { opacity: 1; }"
            + ".action-btn { background: white; border: 1px solid #e2e8f0; border-radius: 6px; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #64748b; transition: all 0.2s; }"
            + ".action-btn:hover { background: #f1f5f9; color: #2563eb; border-color: #cbd5e1; }"
            + ".collapsed .bubble-content { display: none; }"
            + ".collapsed .bubble { padding: 8px 16px; min-height: 0; opacity: 0.7; }"
            + ".collapsed .bubble::after { content: '... (message collapsed)'; font-style: italic; font-size: 12px; color: #94a3b8; }"
            + "pre { background: #0f172a; color: #f8fafc; padding: 16px; border-radius: 12px; overflow-x: auto; font-family: 'Fira Code', 'JetBrains Mono', monospace; font-size: 13px; margin: 12px 0; border: 1px solid #1e293b; position: relative; }"
            + "code { font-family: 'Fira Code', monospace; background: rgba(0,0,0,0.05); padding: 2px 4px; border-radius: 4px; font-weight: 500; }"
            + ".user code { background: rgba(255,255,255,0.2); }"
            + "h3 { margin: 16px 0 8px 0; font-size: 16px; font-weight: 700; color: #1e293b; }"
            + ".user h3 { color: white; }"
            + "ul { margin: 8px 0; padding-left: 20px; }"
            + "li { margin: 4px 0; }"
            + "@keyframes slideIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }"
            + ".thinking { display: flex; gap: 6px; padding: 12px 16px; background: white; border: 1px solid #e2e8f0; border-radius: 16px; align-self: flex-start; margin-bottom: 20px; box-shadow: 0 1px 2px rgba(0,0,0,0.05); }"
            + ".dot { width: 8px; height: 8px; background: #3b82f6; border-radius: 50%; animation: bounce 1.4s infinite ease-in-out; }"
            + ".dot:nth-child(2) { animation-delay: 0.2s; }"
            + ".dot:nth-child(3) { animation-delay: 0.4s; }"
            + "@keyframes bounce { 0%, 80%, 100% { transform: scale(0); opacity: 0.3; } 40% { transform: scale(1); opacity: 1; } }"
            + "::-webkit-scrollbar { width: 6px; }"
            + "::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 3px; }"
            + "</style></head><body>"
            + "<div class='chat-container' id='chat'></div>"
            + "<div id='thinking' style='display:none'><div class='thinking'><div class='dot'></div><div class='dot'></div><div class='dot'></div></div></div>"
            + "<script>"
            + "const icons = { user: '👤', ai: '🤖', planner: '📋', architect: '📐', javadev: '💻', tester: '🧪', reviewer: '⚖️', tool: '⚙️' };"
            + "function formatText(text) {"
            + "  text = text.replace(/```([\\s\\S]*?)```/g, function(match, code) {"
            + "    return '<pre><code>' + escapeHtml(code.trim()) + '</code><button class=\"copy-btn\" style=\"position:absolute;top:8px;right:8px;background:rgba(255,255,255,0.1);border:none;color:white;padding:4px 8px;border-radius:4px;font-size:10px;cursor:pointer;\" onclick=\"event.stopPropagation(); copyToClipboard(\\'' + escapeJs(code.trim()) + '\\')\">Copy</button></pre>';"
            + "  });"
            + "  text = text.replace(/^### (.*$)/gim, '<h3>$1</h3>');"
            + "  text = text.replace(/^\\* (.*$)/gim, '<li>$1</li>');"
            + "  text = text.replace(/^- (.*$)/gim, '<li>$1</li>');"
            + "  text = text.replace(/(<li>.*<\\/li>)/gms, '<ul>$1</ul>');"
            + "  text = text.replace(/`([^`]+)`/g, '<code>$1</code>');"
            + "  text = text.replace(/\\*\\*([^\\*]+)\\*\\*/g, '<b>$1</b>');"
            + "  text = text.replace(/\\*([^\\*]+)\\*/g, '<i>$1</i>');"
            + "  return text.replace(/\\n/g, '<br>');"
            + "}"
            + "function escapeHtml(unsafe) {"
            + "  return unsafe.replace(/[&<\\\"']/g, function(m) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '\"': '&quot;', \"'\": '&#039;' }[m]; });"
            + "}"
            + "function escapeJs(text) { return text.replace(/\\\\/g, '\\\\\\\\').replace(/'/g, \"\\\\'\").replace(/\\n/g, '\\\\n'); }"
            + "function toggleCollapse(index) {"
            + "  document.getElementById('msg-' + index).classList.toggle('collapsed');"
            + "}"
            + "function updateMessages(messages) {"
            + "  var container = document.getElementById('chat');"
            + "  var wasAtBottom = (window.innerHeight + window.scrollY) >= document.body.offsetHeight - 50;"
            + "  container.innerHTML = '';"
            + "  messages.forEach(function(m) {"
            + "    var div = document.createElement('div');"
            + "    var isUser = m.sender.toLowerCase().includes('you') || m.sender.toLowerCase().includes('user');"
            + "    div.className = 'message ' + (isUser ? 'user' : 'ai');"
            + "    div.id = 'msg-' + m.index;"
            + "    var header = document.createElement('div'); header.className = 'header';"
            + "    var icon = document.createElement('span'); icon.className = 'icon'; icon.textContent = icons[m.agentType] || (isUser ? icons.user : icons.ai);"
            + "    var sender = document.createElement('span'); sender.className = 'sender'; sender.textContent = m.sender;"
            + "    var time = document.createElement('span'); time.className = 'timestamp'; time.textContent = m.timestamp || '';"
            + "    header.appendChild(icon); header.appendChild(sender); header.appendChild(time);"
            + "    var bubble = document.createElement('div'); bubble.className = 'bubble';"
            + "    var content = document.createElement('div'); content.className = 'bubble-content';"
            + "    content.innerHTML = formatText(m.text);"
            + "    bubble.appendChild(content);"
            + "    var actions = document.createElement('div'); actions.className = 'actions';"
            + "    actions.innerHTML = '<button class=\"action-btn\" title=\"Copy Message\" onclick=\"event.stopPropagation(); copyToClipboard(\\'' + escapeJs(m.text) + '\\')\">📋</button>' +"
            + "                        '<button class=\"action-btn\" title=\"Collapse/Expand\" onclick=\"event.stopPropagation(); toggleCollapse(' + m.index + ')\">↕️</button>';"
            + "    div.appendChild(header); div.appendChild(bubble); div.appendChild(actions);"
            + "    bubble.onclick = function() { callEditMessage(m.index, m.text); };"
            + "    container.appendChild(div);"
            + "  });"
            + "  if (wasAtBottom || messages.length === 1) window.scrollTo(0, document.body.scrollHeight);"
            + "}"
            + "function showThinking(show) {"
            + "  document.getElementById('thinking').style.display = show ? 'block' : 'none';"
            + "  if (show) window.scrollTo(0, document.body.scrollHeight);"
            + "}"
            + "</script></body></html>";
    }
}
