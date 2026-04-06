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

        ChatMessage(String sender, String text, String color, boolean isBold, boolean isItalic) {
            this.sender = sender;
            this.text = text;
            this.color = color;
            this.isBold = isBold;
            this.isItalic = isItalic;
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
        String sender = "Evolution";
        String content = text;
        if (text.startsWith("You: ")) {
            sender = "You";
            content = text.substring(5);
        } else if (text.startsWith("User [SELF-DEV]: ")) {
            sender = "User [SELF-DEV]";
            content = text.substring(17);
        } else if (text.startsWith("Evolution: ")) {
            sender = "Evolution";
            content = text.substring(11);
        } else if (text.contains("Agent [")) {
            int start = text.indexOf("Agent [");
            int end = text.indexOf("]", start);
            if (end != -1) {
                sender = text.substring(start, end + 1);
                content = text.substring(end + 1).trim();
                if (content.startsWith(":")) content = content.substring(1).trim();
            }
        }

        String hexColor = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        boolean isBold = (style & SWT.BOLD) != 0;
        boolean isItalic = (style & SWT.ITALIC) != 0;

        messages.add(new ChatMessage(sender, content, hexColor, isBold, isItalic));
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
            sb.append("\"isItalic\":").append(m.isItalic);
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
            if (line.contains(": ")) {
                int colon = line.indexOf(": ");
                String sender = line.substring(0, colon);
                String content = line.substring(colon + 2);
                messages.add(new ChatMessage(sender, content, "#000000", false, false));
            } else {
                messages.add(new ChatMessage("System", line, "#666666", false, true));
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
            + "body { font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f9fafb; margin: 0; padding: 20px; overflow-y: auto; }"
            + ".chat-container { display: flex; flex-direction: column; gap: 16px; max-width: 900px; margin: 0 auto; }"
            + ".message { display: flex; flex-direction: column; max-width: 85%; position: relative; animation: fadeIn 0.3s ease-out; }"
            + ".message.user { align-self: flex-end; }"
            + ".message.ai { align-self: flex-start; }"
            + ".bubble { padding: 12px 16px; border-radius: 12px; font-size: 14px; line-height: 1.5; position: relative; box-shadow: 0 1px 2px rgba(0,0,0,0.05); cursor: pointer; transition: transform 0.1s; }"
            + ".bubble:hover { transform: scale(1.01); }"
            + ".user .bubble { background: #3b82f6; color: white; border-bottom-right-radius: 2px; }"
            + ".ai .bubble { background: white; color: #1f2937; border: 1px solid #e5e7eb; border-bottom-left-radius: 2px; }"
            + ".sender { font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 4px; color: #6b7280; }"
            + ".user .sender { text-align: right; }"
            + "pre { background: #1e293b; color: #e2e8f0; padding: 12px; border-radius: 8px; overflow-x: auto; font-family: 'Fira Code', monospace; font-size: 13px; position: relative; margin: 10px 0; }"
            + "code { font-family: 'Fira Code', monospace; background: rgba(0,0,0,0.1); padding: 2px 4px; border-radius: 4px; }"
            + ".user code { background: rgba(255,255,255,0.2); }"
            + ".copy-btn { position: absolute; top: 8px; right: 8px; background: rgba(255,255,255,0.1); border: none; color: white; padding: 4px 8px; border-radius: 4px; font-size: 10px; cursor: pointer; opacity: 0; transition: opacity 0.2s; }"
            + "pre:hover .copy-btn { opacity: 1; }"
            + ".copy-btn:hover { background: rgba(255,255,255,0.2); }"
            + "@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }"
            + ".thinking { display: flex; gap: 4px; padding: 8px 12px; background: white; border: 1px solid #e5e7eb; border-radius: 12px; align-self: flex-start; margin-bottom: 16px; }"
            + ".dot { width: 6px; height: 6px; background: #9ca3af; border-radius: 50%; animation: bounce 1.4s infinite ease-in-out; }"
            + ".dot:nth-child(2) { animation-delay: 0.2s; }"
            + ".dot:nth-child(3) { animation-delay: 0.4s; }"
            + "@keyframes bounce { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }"
            + "::-webkit-scrollbar { width: 8px; }"
            + "::-webkit-scrollbar-track { background: transparent; }"
            + "::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 4px; }"
            + "::-webkit-scrollbar-thumb:hover { background: #9ca3af; }"
            + "html { scroll-behavior: smooth; }"
            + "</style></head><body>"
            + "<div class='chat-container' id='chat'></div>"
            + "<div id='thinking' style='display:none'><div class='thinking'><div class='dot'></div><div class='dot'></div><div class='dot'></div></div></div>"
            + "<script>"
            + "function formatText(text) {"
            + "  text = text.replace(/```([\\s\\S]*?)```/g, function(match, code) {"
            + "    return '<pre><code>' + escapeHtml(code.trim()) + '</code><button class=\"copy-btn\" onclick=\"event.stopPropagation(); copyToClipboard(\\'' + escapeJs(code.trim()) + '\\')\">Copy</button></pre>';"
            + "  });"
            + "  text = text.replace(/`([^`]+)`/g, '<code>$1</code>');"
            + "  text = text.replace(/\\*\\*([^\\*]+)\\*\\*/g, '<b>$1</b>');"
            + "  text = text.replace(/\\*([^\\*]+)\\*/g, '<i>$1</i>');"
            + "  return text.replace(/\\n/g, '<br>');"
            + "}"
            + "function escapeHtml(unsafe) {"
            + "  return unsafe.replace(/[&<\\\"']/g, function(m) {"
            + "    switch(m) { case '&': return '&amp;'; case '<': return '&lt;'; case '>': return '&gt;'; case '\"': return '&quot;'; default: return '&#039;'; }"
            + "  });"
            + "}"
            + "function escapeJs(text) {"
            + "  return text.replace(/\\\\/g, '\\\\\\\\').replace(/'/g, \"\\\\'\").replace(/\\n/g, '\\\\n');"
            + "}"
            + "function updateMessages(messages) {"
            + "  var container = document.getElementById('chat');"
            + "  var wasAtBottom = (window.innerHeight + window.scrollY) >= document.body.offsetHeight - 50;"
            + "  container.innerHTML = '';"
            + "  messages.forEach(function(m) {"
            + "    var div = document.createElement('div');"
            + "    var isUser = m.sender.toLowerCase().includes('you') || m.sender.toLowerCase().includes('user');"
            + "    div.className = 'message ' + (isUser ? 'user' : 'ai');"
            + "    var sender = document.createElement('div');"
            + "    sender.className = 'sender';"
            + "    sender.textContent = m.sender;"
            + "    var bubble = document.createElement('div');"
            + "    bubble.className = 'bubble';"
            + "    bubble.style.color = isUser ? 'white' : m.color;"
            + "    if (m.isBold) bubble.style.fontWeight = 'bold';"
            + "    if (m.isItalic) bubble.style.fontStyle = 'italic';"
            + "    bubble.innerHTML = formatText(m.text);"
            + "    bubble.onclick = function() { callEditMessage(m.index, m.text); };"
            + "    div.appendChild(sender);"
            + "    div.appendChild(bubble);"
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
