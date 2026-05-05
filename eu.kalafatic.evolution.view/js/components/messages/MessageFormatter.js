import { JavaBridge } from '../../core/JavaBridge.js';

export const MessageFormatter = {
    escapeHtml(unsafe) {
        if (!unsafe) return "";
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    },

    escapeJs(text) {
        if (!text) return "";
        return text.replace(/\\/g, '\\\\')
                   .replace(/'/g, "\\'")
                   .replace(/"/g, "&quot;")
                   .replace(/`/g, "\\`")
                   .replace(/\n/g, '\\n')
                   .replace(/\r/g, '');
    },

    formatJson(text, role) {
        try {
            let jsonText = text.trim();
            if (jsonText.startsWith('```')) {
                jsonText = jsonText.replace(/^```[a-z]*\n/i, '').replace(/\n```$/i, '');
            }
            const jsonMatch = jsonText.match(/[\{\[][\s\S]*[\}\]]/);
            if (jsonMatch) {
                jsonText = jsonMatch[0];
            }

            const data = JSON.parse(jsonText);
            return this.renderJsonObject(data, role);
        } catch (e) {
            return null; // Fallback to markdown
        }
    },

    renderJsonObject(data, role) {
        const renderValue = (val, key) => {
            if (val === null || val === undefined) return "";
            if (Array.isArray(val)) {
                if (val.length === 0) return "";
                return `<ul style="margin: 2px 0; padding-left: 15px;">${val.map(item => `<li>${renderValue(item, key)}</li>`).join('')}</ul>`;
            }
            if (typeof val === 'object') {
                return Object.entries(val).map(([k, v]) => `<div><b>${k}:</b> ${renderValue(v, k)}</div>`).join('');
            }
            const strVal = String(val);
            const fileKeys = ['files', 'path', 'target', 'file'];
            if (fileKeys.includes(key) || (key === 'description' && (strVal.includes('/') || strVal.includes('.')))) {
                if (strVal.length > 3 && (strVal.includes('.') || strVal.includes('/'))) {
                    return `<a onclick="event.stopPropagation(); window.dispatchEvent(new CustomEvent('java:openDiff', {detail: '${this.escapeJs(strVal)}'}))"><b>${this.escapeHtml(strVal)}</b></a>`;
                }
            }
            return this.escapeHtml(strVal);
        };

        let html = '<div style="display: flex; flex-direction: column; gap: 6px;">';

        if (Array.isArray(data)) {
            data.forEach((item, idx) => {
                html += `<div style="border-bottom: 1px solid var(--border); padding-bottom: 4px;">`;
                html += `<div style="font-weight: bold; color: var(--orchestrator-text); margin-bottom: 2px;">PROPOSAL ${idx + 1}</div>`;
                html += renderValue(item);
                html += `</div>`;
            });
        } else {
            const topLevelFields = ['workDone', 'plan', 'files', 'troubles', 'category', 'objective', 'strategy', 'rootCause', 'clarificationQuestion', 'explanation', 'refinedPrompt'];

            topLevelFields.forEach(field => {
                if (data[field]) {
                    let label = field.replace(/([A-Z])/g, ' $1').toUpperCase();
                    let value = data[field];

                    if (field === 'refinedPrompt') {
                        html += `<div style="margin-top: 4px; border-top: 1px solid var(--border); padding-top: 4px;"><a class="link-go" onclick="event.stopPropagation(); window.dispatchEvent(new CustomEvent('java:executeProposal', {detail: '${this.escapeJs(value)}'}))"><b>${this.escapeHtml(value)}</b></a></div>`;
                    } else {
                        html += `<div><div style="font-weight: 800; font-size: 10px; color: #64748b; letter-spacing: 0.05em; margin-top: 4px;">${label}</div>`;
                        html += `<div style="margin-top: 1px;">${renderValue(value, field)}</div></div>`;
                    }
                }
            });

            Object.entries(data).forEach(([key, val]) => {
                if (!topLevelFields.includes(key)) {
                    html += `<div><b>${key}:</b> ${renderValue(val, key)}</div>`;
                }
            });
        }

        html += '</div>';
        return html;
    },

    formatMarkdown(text, role) {
        // Strip technical markers
        text = text.replace(/\[KERNEL\]/g, '')
                   .replace(/\[STRATEGY\]/g, '')
                   .replace(/\[ANALYSIS\]/g, '')
                   .replace(/\[SUPERVISOR\]/g, '')
                   .replace(/\[EVO\]/g, '')
                   .replace(/\[DARWIN\]/g, '')
                   .trim();

        const thinkBlocks = [];
        text = text.replace(/<think>([\s\S]*?)<\/think>/g, (match, content) => {
            thinkBlocks.push(content.trim());
            return '___THINK_BLOCK_' + (thinkBlocks.length - 1) + '___';
        });

        const codeBlocks = [];
        text = text.replace(/```([\s\S]*?)```/g, (match, code) => {
            codeBlocks.push(code.trim());
            return '___CODE_BLOCK_' + (codeBlocks.length - 1) + '___';
        });

        text = this.escapeHtml(text);

        // Links and phrases
        text = text.replace(/\bCREATE\b/g, '<a class="link-go" onclick="event.stopPropagation(); window.dispatchEvent(new CustomEvent(\'java:approve\'))">CREATE</a>');
        text = text.replace(/\bCLARIFY\b/g, '<a class="link-clarify" onclick="event.stopPropagation(); window.dispatchEvent(new CustomEvent(\'java:clarify\'))">CLARIFY</a>');

        // Problem patterns
        const problemPatterns = [/server not running/gi, /model not active/gi, /connection lost/gi, /connection refused/gi, /error during execution/gi, /exception:/gi, /failed/gi];
        problemPatterns.forEach(pattern => {
            text = text.replace(pattern, '<a class="link-problem">$&</a>');
        });

        // General proposal link [PROPOSAL: Label | Request]
        text = text.replace(/\[PROPOSAL:\s*(.*?)\s*\|\s*(.*?)\s*\]/g, (match, p1, p2) =>
            `<a class="link-go" onclick="event.stopPropagation(); window.dispatchEvent(new CustomEvent('java:executeProposal', {detail: '${this.escapeJs(p2)}'}))"><b>${p1}</b></a>`
        );

        // File link [FILE: path]
        text = text.replace(/\[FILE:\s*(.*?)\s*\]/g, (match, p1) =>
            `<a onclick="event.stopPropagation(); window.dispatchEvent(new CustomEvent('java:openDiff', {detail: '${this.escapeJs(p1)}'}))"><b>${p1}</b></a>`
        );

        // Markdown-like formatting
        text = text.replace(/^\s*###### (.*$)/gim, '<h6>$1</h6>');
        text = text.replace(/^\s*##### (.*$)/gim, '<h5>$1</h5>');
        text = text.replace(/^\s*#### (.*$)/gim, '<h4>$1</h4>');
        text = text.replace(/^\s*### (.*$)/gim, '<h3>$1</h3>');
        text = text.replace(/^\s*## (.*$)/gim, '<h2>$1</h2>');
        text = text.replace(/^\s*# (.*$)/gim, '<h1>$1</h1>');

        text = text.replace(/^\s*(?:\*|-) (.*$)/gim, '<li>$1</li>');
        text = text.replace(/(?:<li>.*<\/li>\n?)+/g, '<ul>$&</ul>');

        // Restore blocks
        text = text.replace(/___THINK_BLOCK_(\d+)___/g, (match, index) => {
            return '<div class="think-block">' + this.formatText(thinkBlocks[parseInt(index)], role) + '</div>';
        });

        text = text.replace(/___CODE_BLOCK_(\d+)___/g, (match, index) => {
            const code = codeBlocks[parseInt(index)];
            return `<pre><code>${this.escapeHtml(code)}</code><button class="copy-btn" style="position:absolute;top:8px;right:8px;background:rgba(0,0,0,0.05);border:none;color:#64748b;padding:4px 8px;border-radius:4px;font-size:10px;cursor:pointer;user-select:none;" onclick="event.stopPropagation(); window.dispatchEvent(new CustomEvent('java:copy', {detail: '${this.escapeJs(code)}'}))">Copy</button></pre>`;
        });

        text = text.replace(/`([^`]+)`/g, '<code>$1</code>');

        if (text.includes("PROJECT ROOT:") || text.includes("INSTRUCTIONS:") ||
            text.includes("BEST PRACTICES &amp; GUIDELINES") ||
            text.includes("ITERATIVE LOOP CONTEXT") ||
            text.includes("STATE MODEL:") ||
            text.includes("FAILURE FINGERPRINTING")) {
            text = '<div class="non-essential">' + text + '</div>';
        }

        text = text.replace(/\*\*([^\*]+)\*\*/g, '<b>$1</b>');
        text = text.replace(/\*([^\*]+)\*/g, '<i>$1</i>');
        text = text.replace(/\n/g, '<br>');

        // Cleanup breaks
        text = text.replace(/<\/h[1-6]><br>/gi, m => m.substring(0, m.length - 4))
                   .replace(/<\/ul><br>/gi, m => m.substring(0, m.length - 4))
                   .replace(/<\/li><br>/gi, m => m.substring(0, m.length - 4))
                   .replace(/<br><li>/gi, m => m.substring(4))
                   .replace(/<\/pre><br>/gi, m => m.substring(0, m.length - 4))
                   .replace(/<\/div><br>/gi, m => m.substring(0, m.length - 4))
                   .replace(/<br><ul>/gi, m => m.substring(4))
                   .replace(/<\/p><br>/gi, m => m.substring(0, m.length - 4))
                   .replace(/<br><h[1-6]>/gi, m => m.substring(4))
                   .replace(/<br><pre>/gi, m => m.substring(4));

        return text;
    },

    formatText(text, role) {
        if (!text) return "";
        const rolesToFormatAsJson = ['analytic', 'waiting', 'response', 'thinking', 'darwin', 'orchestrator', 'ai', 'final-response'];
        if (rolesToFormatAsJson.includes(role)) {
            const jsonHtml = this.formatJson(text, role);
            if (jsonHtml) return jsonHtml;
        }
        return this.formatMarkdown(text, role);
    }
};
