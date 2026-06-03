window.ChatApp = window.ChatApp || {};

window.ChatApp.Renderer = {
    renderMessage: function(m) {
        if (!m || (m.text === undefined || m.text === null)) return null;
        const role = (m.agentType || '').toLowerCase();

        // Skip rendering non-user empty messages to avoid broken bubbles
        if (m.text.trim() === "" && !role.includes('user')) return null;

        const roles = role.split(' ');
        const primaryRole = roles[0];
        const isUser = primaryRole === 'user' || (m.sender || '').toLowerCase().includes('you') || (m.sender || '').toLowerCase().includes('user');

        const div = document.createElement('div');
        div.className = 'message ' + (isUser ? 'user' : 'ai') + ' ' + role;
        div.dataset.index = m.index;
        if (role.includes('approved')) div.classList.add('approved');
        if (!role.includes('waiting') && (role.includes('darwin') || role.includes('branch'))) div.classList.add('sealed');

        const header = document.createElement('div');
        header.className = 'header';
        const icon = window.ChatApp.Constants.icons[primaryRole] || (isUser ? window.ChatApp.Constants.icons.user : window.ChatApp.Constants.icons.ai);
        header.innerHTML = `<span class="timestamp">${m.timestamp || ''}</span><span class="icon">${icon}</span><span class="sender">${m.sender}</span>`;

        const content = document.createElement('div');
        content.className = 'message-content';

        let isDarwin = !isUser && (role.includes('darwin') || role.includes('branch')) && (m.text.includes('{') || m.text.includes('['));
        if (!isDarwin && !isUser) {
             try {
                const data = JSON.parse(m.text);
                if (data.variants || data.proposals || (Array.isArray(data) && data.length > 0 && (data[0].strategy || data[0].id || data[0].description))) isDarwin = true;
             } catch(e) {}
        }

        if (isDarwin) {
            content.style.flexDirection = 'column';
            content.appendChild(this.renderDarwin(m));

            // Move Force Solution button out of branch container to appear UNDER all branches
            const role = (m.agentType || '').toLowerCase();
            const isApproved = role.includes('approved');
            const isWaiting = role.includes('waiting');
            if (!isApproved && isWaiting) {
                const forceDiv = document.createElement('div');
                forceDiv.className = 'force-solution-container';
                forceDiv.innerHTML = `<button class="branch-btn force" onclick="window.ChatApp.Actions.callJava('forceSolution', '${m.index}', '')">⚡ Force Solution</button>
                                      <div class="force-hint">Bypass expansion & execute winner</div>`;
                content.appendChild(forceDiv);
            }
        } else {
            const bubble = document.createElement('div');
            bubble.className = 'bubble';
            bubble.onclick = () => window.ChatApp.Actions.callJava('edit', m.index.toString(), m.text);

            const bubbleContent = document.createElement('div');
            bubbleContent.className = 'bubble-content';
            bubbleContent.innerHTML = this.formatText(m.text, primaryRole);
            bubble.appendChild(bubbleContent);
            content.appendChild(bubble);
            content.appendChild(window.ChatApp.Actions.renderActions(m));
        }

        div.appendChild(header);
        div.appendChild(content);
        return div;
    },

    highlightMatches: function(el, query) {
        this.clearHighlight(el);
        if (!query) return;

        const regex = query instanceof RegExp ? query : new RegExp(query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'gi');

        const walk = (node) => {
            if (node.nodeType === 3) { // Text node
                const text = node.nodeValue;
                let match;
                const spans = [];
                let lastIdx = 0;

                while ((match = regex.exec(text)) !== null) {
                    if (match.index > lastIdx) {
                        spans.push(document.createTextNode(text.substring(lastIdx, match.index)));
                    }
                    const mark = document.createElement('mark');
                    mark.className = 'search-match';
                    mark.innerText = match[0];
                    spans.push(mark);
                    lastIdx = regex.lastIndex;
                    if (regex.lastIndex === match.index) regex.lastIndex++; // Avoid infinite loop for empty matches
                }

                if (lastIdx < text.length) {
                    spans.push(document.createTextNode(text.substring(lastIdx)));
                }

                if (spans.length > 0) {
                    const fragment = document.createDocumentFragment();
                    spans.forEach(s => fragment.appendChild(s));
                    node.parentNode.replaceChild(fragment, node);
                }
            } else if (node.nodeType === 1 && node.childNodes && !['SCRIPT', 'STYLE', 'MARK'].includes(node.tagName)) {
                Array.from(node.childNodes).forEach(walk);
            }
        };

        // Target only bubble and header
        const content = el.querySelector('.bubble-content') || el;
        const sender = el.querySelector('.sender');
        if (content) walk(content);
        if (sender) walk(sender);
    },

    clearHighlight: function(el) {
        el.querySelectorAll('mark.search-match').forEach(mark => {
            const parent = mark.parentNode;
            parent.replaceChild(document.createTextNode(mark.innerText), mark);
            parent.normalize(); // Merge adjacent text nodes
        });
    },

    formatText: function(text, role) {
        if (!text) return "";
        let clean = window.ChatApp.Utils.stripTechnicalMarkers(text);

        // Handle think blocks BEFORE escaping
        let thinkBlocks = [];
        clean = clean.replace(/<think>([\s\S]*?)<\/think>/gi, (match, content) => {
            thinkBlocks.push(content);
            return '___THINK_BLOCK_' + (thinkBlocks.length - 1) + '___';
        });

        // Try JSON rendering first
        if (clean.includes('{') && clean.includes('}')) {
            try {
                let jsonText = clean;
                if (jsonText.startsWith('```')) {
                    jsonText = jsonText.replace(/^```[a-z]*\n/i, '').replace(/\n```$/i, '');
                }
                const match = jsonText.match(/[\{\[][\s\S]*[\}\]]/);
                if (match) {
                    const data = JSON.parse(match[0]);
                    return this.renderJson(data);
                }
            } catch(e) {}
        }

        // Markdown-ish formatting
        let html = window.ChatApp.Utils.escapeHtml(clean);

        // Links & Interactions
        html = html.replace(/\bCREATE\b/g, '<a class="link-go" onclick="window.ChatApp.Actions.callJava(\'create\')">CREATE</a>');
        html = html.replace(/\bCLARIFY\b/g, '<a class="link-clarify" onclick="window.ChatApp.Actions.callJava(\'clarify\')">CLARIFY</a>');

        // Conversational phrase mapping
        html = html.replace(/could you tell me a bit more about what you(?:’|'|&#039;)re trying to accomplish\?/g, '<a class="link-clarify" onclick="window.ChatApp.Actions.callJava(\'clarify\')">$&</a>');
        html = html.replace(/Are you looking for a simple example to get started/g, '<a class="link-go" onclick="window.ChatApp.Actions.callJava(\'helloworld\')">$&</a>');
        html = html.replace(/are you working on a more complex project that requires a specific file structure\?/g, '<a class="link-clarify" onclick="window.ChatApp.Actions.callJava(\'clarify\')">$&</a>');

        // Proposal link [PROPOSAL: Label | Request]
        html = html.replace(/\[PROPOSAL:\s*(.*?)\s*\|\s*(.*?)\s*\]/g, (match, label, req) => {
             return `<a class="link-go" onclick="window.ChatApp.Actions.callJava('executeProposal', '-1', '${window.ChatApp.Utils.escapeJs(req)}')"><b>${label}</b></a>`;
        });

        // File link [FILE: path]
        html = html.replace(/\[FILE:\s*(.*?)\s*\]/g, (match, path) => {
            return `<a onclick="window.ChatApp.Actions.callJava('openDiff', '-1', '${window.ChatApp.Utils.escapeJs(path)}')"><b>${path}</b></a>`;
        });

        // Proposal: print to console
        html = html.replace(/\bprint to console\b/gi, '<a class="link-go" onclick="window.ChatApp.Actions.callJava(\'executeProposal\', \'-1\', \'print to console\')">$&</a>');

        // Phase Transition Yes/No links
        html = html.replace(/\((Yes\/No)\)/gi, (match) => {
            return `(<a class="link-go" onclick="window.ChatApp.Actions.callJava('executeProposal', '-1', 'Yes')">Yes</a>/<a class="link-go" onclick="window.ChatApp.Actions.callJava('executeProposal', '-1', 'No')">No</a>)`;
        });

        // Basic Markdown
        html = html.replace(/\*\*([\s\S]*?)\*\*/g, '<b>$1</b>');
        html = html.replace(/\*([\s\S]*?)\*/g, '<i>$1</i>');
        html = html.replace(/`([^`]+)`/g, '<code>$1</code>');

        // Markdown Links [text](url)
        html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (match, text, url) => {
             if (url.startsWith('file://')) {
                  return `<a onclick="window.ChatApp.Actions.callJava('openDiff', '-1', '${window.ChatApp.Utils.escapeJs(url)}')"><b>${text}</b></a>`;
             }
             return `<a href="${url}" target="_blank">${text}</a>`;
        });

        // Headers
        html = html.replace(/^\s*###### (.*$)/gim, '<h6>$1</h6>');
        html = html.replace(/^\s*##### (.*$)/gim, '<h5>$1</h5>');
        html = html.replace(/^\s*#### (.*$)/gim, '<h4>$1</h4>');
        html = html.replace(/^\s*### (.*$)/gim, '<h3>$1</h3>');
        html = html.replace(/^\s*## (.*$)/gim, '<h2>$1</h2>');
        html = html.replace(/^\s*# (.*$)/gim, '<h1>$1</h1>');

        // Lists
        html = html.replace(/^\s*[\-\*]\s+(.*)$/gm, '<li>$1</li>');
        html = html.replace(/(?:<li>.*<\/li>\n?)+/g, '<ul>$&</ul>');

        // Blocks
        html = html.replace(/```([a-z]*)\n?([\s\S]*?)\n?```/gi, (match, lang, code) => {
            return `<pre><code>${window.ChatApp.Utils.escapeHtml(code.trim())}</code><button class="copy-btn" style="position:absolute;top:8px;right:8px;" onclick="window.ChatApp.Actions.callJava('copy', '-1', '${window.ChatApp.Utils.escapeJs(code)}')">Copy</button></pre>`;
        });

        // Restore think blocks
        html = html.replace(/___THINK_BLOCK_(\d+)___/g, (match, index) => {
            return `<div class="think-block">${this.formatText(thinkBlocks[parseInt(index)], role)}</div>`;
        });

        if (html.includes("PROJECT ROOT:") || html.includes("INSTRUCTIONS:")) {
            html = '<div class="non-essential">' + html + '</div>';
        }

        return html.replace(/\n/g, '<br>');
    },

    renderJson: function(data) {
        if (!data) return "";
        const renderValue = (val, key) => {
            if (val === null || val === undefined) return "";
            if (Array.isArray(val)) {
                if (val.length === 0) return "";
                return `<ul style="margin: 4px 0; padding-left: 18px;">${val.map(v => `<li>${renderValue(v, key)}</li>`).join('')}</ul>`;
            }
            if (typeof val === 'object') {
                return Object.entries(val).map(([k, v]) => `<div style="margin-bottom: 2px;"><b>${k}:</b> ${renderValue(v, k)}</div>`).join('');
            }

            const str = String(val);
            if (['files', 'path', 'file', 'target'].includes(key) && (str.includes('.') || str.includes('/'))) {
                return `<a onclick="window.ChatApp.Actions.callJava('openDiff', '-1', '${window.ChatApp.Utils.escapeJs(str)}')"><b>${window.ChatApp.Utils.escapeHtml(str)}</b></a>`;
            }
            return window.ChatApp.Utils.escapeHtml(str);
        };

        const humanKeys = ['explanation', 'strategy', 'thought', 'objective', 'refinedPrompt', 'rootCause', 'plan', 'workDone', 'summary', 'description', 'hypothesis', 'expected_effects', 'expected_effect', 'clarificationQuestion'];
        const technicalKeys = ['id', 'suffix', 'score', 'risk', 'reversibility', 'confidence', 'intent', 'category', 'isAmbiguous', 'missingInformation'];

        // If data is a simple object with just one or two human keys, render it as plain text
        if (typeof data === 'object' && !Array.isArray(data)) {
            const keys = Object.keys(data);
            const humanPresent = keys.filter(k => humanKeys.includes(k));
            if (humanPresent.length === 1 && keys.length <= 3) {
                 const key = humanPresent[0];
                 if (typeof data[key] === 'string') return this.formatText(data[key]);
            }
        }

        let html = '<div style="display: flex; flex-direction: column; gap: 6px;">';
        if (Array.isArray(data)) {
            data.forEach((item, idx) => {
                html += `<div style="border-bottom: 1px solid var(--border); padding-bottom: 6px; margin-bottom: 6px;"><b>PROPOSAL ${idx+1}</b><br>${this.renderJson(item)}</div>`;
            });
        } else {
            // First render prioritized human keys
            humanKeys.forEach(f => {
                if (data[f]) {
                    if (f === 'refinedPrompt') {
                        html += `<div><div style="font-size: 10px; font-weight: 800; color: #64748b;">REFINED PROMPT</div><a class="link-go" onclick="window.ChatApp.Actions.callJava('executeProposal', '-1', '${window.ChatApp.Utils.escapeJs(data[f])}')"><b>${window.ChatApp.Utils.escapeHtml(data[f])}</b></a></div>`;
                    } else if (f === 'hypothesis' && typeof data[f] === 'object') {
                         html += `<div><div style="font-size: 10px; font-weight: 800; color: #64748b;">HYPOTHESIS</div>${renderValue(data[f].description)}</div>`;
                         if (data[f].expected_effects) html += `<div><div style="font-size: 10px; font-weight: 800; color: #64748b;">EXPECTED EFFECTS</div>${renderValue(data[f].expected_effects)}</div>`;
                    } else if (f === 'expected_effect' && typeof data[f] === 'object') {
                         html += `<div><div style="font-size: 10px; font-weight: 800; color: #64748b;">EXPECTED EFFECT</div>${data[f].short_term || data[f].long_term || ''}</div>`;
                    } else if (f === 'clarificationQuestion') {
                         html += `<div style="font-size: 1.1em; color: var(--ai-text); border-left: 3px solid var(--primary); padding-left: 8px;">${renderValue(data[f], f)}</div>`;
                    } else {
                        // Less technical header for common narrative fields
                        const usePlainLabel = ['explanation', 'thought', 'summary', 'description'].includes(f);
                        if (usePlainLabel) {
                            html += `<div style="line-height: 1.4;">${renderValue(data[f], f)}</div>`;
                        } else {
                            html += `<div><div style="font-size: 10px; font-weight: 800; color: #64748b;">${f.toUpperCase().replace('_', ' ')}</div>${renderValue(data[f], f)}</div>`;
                        }
                    }
                }
            });

            // Then render non-technical, non-prioritized keys
            Object.entries(data).forEach(([k, v]) => {
                if (!humanKeys.includes(k) && !technicalKeys.includes(k) && k !== 'actions' && k !== 'variants' && k !== 'proposals' && k !== 'type') {
                    html += `<div><b>${k}:</b> ${renderValue(v, k)}</div>`;
                }
            });

            // Special handling for actions array
            if (data.actions && Array.from(data.actions).length > 0) {
                html += `<div><div style="font-size: 10px; font-weight: 800; color: #64748b;">ACTIONS</div>`;
                data.actions.forEach(a => {
                    html += `<div style="margin-left: 8px; font-size: 11px;">• <b>${a.operation}</b> ${renderValue(a.target, 'target')} - <i>${a.description}</i></div>`;
                });
                html += `</div>`;
            }
        }
        return html + '</div>';
    },

    renderDarwin: function(m) {
        const container = document.createElement('div');
        container.className = 'branch-container';
        try {
            let text = (m.text || '').trim();

            // Strip Log Contamination (e.g., [Default] [80ff...])
            // We strip leading brackets that don't look like the start of a JSON array/object
            text = text.replace(/^(\[[^\{\"\[]+\]\s*)+/g, '');

            if (text.startsWith('```')) text = text.replace(/^```[a-z]*\n?/i, '').replace(/\n?```$/i, '').trim();

            let data;
            const jsonMatch = text.match(/[\{\[][\s\S]*[\}\]]/);
            if (jsonMatch) {
                data = JSON.parse(jsonMatch[0]);
            } else {
                data = JSON.parse(text);
            }

            const variants = Array.isArray(data) ? data : (data.variants || data.proposals || data.hypotheses || []);
            if (!Array.isArray(variants)) {
                 throw new Error("Darwin message must contain an array of variants/proposals/hypotheses");
            }

            const iteration = data.iteration !== undefined ? data.iteration : null;

            const role = (m.agentType || '').toLowerCase();
            const isApproved = role.includes('approved');
            const approvedId = isApproved && role.includes(':') ? role.split(':').pop().trim() : null;
            const isRejected = role.includes('rejected');
            const rejectedId = isRejected && role.includes(':') ? role.split(':').pop().trim() : null;
            const isWaiting = role.includes('waiting');

            variants.forEach((v, index) => {
                const vId = String(v.id || index);
                const isThisApproved = isApproved && (approvedId === null || approvedId === vId);
                const isThisRejected = (isApproved && approvedId !== null && approvedId !== vId) || (isRejected && rejectedId === vId);
                const isThisKept = v.approved === true || v.status === 'KEPT' || (v.id && m.text.includes(v.id + '"') && m.text.includes('"status":"KEPT"'));

                const col = document.createElement('div');
                col.className = 'branch-column' + (v.isBest ? ' best' : '') + (isThisApproved ? ' approved' : '') + (isThisRejected ? ' rejected' : '') + (isThisKept ? ' kept' : '');

                // Simplified Darwin Header
                let headerLabel = iteration !== null ? `ITERATION ${iteration} - PROPOSAL ${index + 1}` : `PROPOSAL ${index + 1}`;
                let headerHtml = `<div class="branch-header">${headerLabel}${isThisKept ? ' [KEPT]' : ''}</div>`;
                let strategy = v.strategy || v.description;
                if (strategy) headerHtml += `<div class="branch-strategy">${strategy}</div>`;

                let footerContent = "";
                if (isThisApproved) {
                    footerContent = '<div style="color: #16a34a; font-weight: bold;">APPROVED</div>';
                } else if (isThisRejected) {
                    footerContent = '<div style="color: #dc2626; font-weight: bold;">REJECTED</div>';
                } else if (isThisKept && !isWaiting) {
                    footerContent = '<div style="color: #166534; font-weight: bold;">KEPT</div>';
                } else if (isWaiting) {
                    footerContent = `
                        <button class="branch-btn select" onclick="window.ChatApp.Actions.callJava('approveDarwinVariant', '${m.index}', '${vId}')">Select</button>
                        <button class="branch-btn keep" onclick="window.ChatApp.Actions.callJava('keepDarwinVariant', '${m.index}', '${vId}')">Keep</button>
                        <button class="branch-btn reject" onclick="window.ChatApp.Actions.callJava('rejectDarwinVariant', '${m.index}', '${vId}')">Reject</button>
                        <button class="branch-btn" onclick="window.ChatApp.Actions.callJava('editDarwinVariant', '${m.index}', '${vId}')">Edit</button>`;
                }

                col.innerHTML = `
                    ${headerHtml}
                    <div class="branch-body" style="font-size: 11px;">${this.renderJson(v)}</div>
                    <div class="branch-footer">${footerContent}</div>
                `;
                container.appendChild(col);
            });

        } catch(e) { container.innerHTML = `<div class="bubble error">Failed to parse Darwin: ${e.message}</div>`; }
        return container;
    }
};
