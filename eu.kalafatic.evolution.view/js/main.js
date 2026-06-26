window.ChatApp = window.ChatApp || {};

(function() {
    const state = {
        messages: [],
        changes: [],
        lastDiffs: {},
        isUiReady: false,
        pendingMessages: null,
        pendingChanges: null,
        searchQuery: '',
        searchMatches: [],
        currentMatchIndex: -1,
        lastSessionId: null
    };

    window.updateMessages = function(messages, sessionId) {
        if (!state.isUiReady) {
            state.pendingMessages = messages;
            state.pendingSessionId = sessionId;
            return;
        }

        if (!messages || !Array.isArray(messages)) {
            console.error('updateMessages: expected array, got', typeof messages);
            return;
        }

        // Reset UI components that should not persist across session switches
        if (sessionId !== state.lastSessionId) {
            if (window.ChatApp.Renderer.resetProgressPanel) {
                window.ChatApp.Renderer.resetProgressPanel();
            }
            state.lastSessionId = sessionId;
        }

        // Ensure strictly monotonic order by sequence number before rendering
        messages.sort((a, b) => (a.sequenceNumber || 0) - (b.sequenceNumber || 0));

        state.messages = messages;
        const wrapper = document.getElementById('messages-wrapper');
        if (!wrapper) return;

        wrapper.innerHTML = '';
        window.ChatApp.Renderer.updateTreePanel(messages);
        window.ChatApp.Renderer.updateProgressPanel(messages);
        window.ChatApp.Renderer.updateCognitiveStatePanel(messages);

        messages.forEach(m => {
            try {
                const el = window.ChatApp.Renderer.renderMessage(m);
                if (!el) return;

                const text = (m.text || '').toLowerCase();
                const sender = (m.sender || '').toLowerCase();
                const query = state.searchQuery.toLowerCase();

                if (query && !text.includes(query) && !sender.includes(query)) {
                    el.style.display = 'none';
                } else if (state.searchQuery) {
                    let regex;
                    if (state.searchQuery.startsWith('/') && state.searchQuery.endsWith('/') && state.searchQuery.length > 2) {
                        try { regex = new RegExp(state.searchQuery.substring(1, state.searchQuery.length - 1), 'gi'); } catch(e) {}
                    }
                    window.ChatApp.Renderer.highlightMatches(el, regex || state.searchQuery.toLowerCase());
                }
                wrapper.appendChild(el);
            } catch (e) {
                console.error('Error rendering message at index', m.index, e);
                if (window.JavaLog) window.JavaLog('Error rendering message: ' + e.message);
            }
        });
        window.ChatApp.UI.scrollToBottom();
    };

    window.filterMessages = function(q) {
        state.searchQuery = q;
        const wrapper = document.getElementById('messages-wrapper');
        if (!wrapper) return;

        // Show/hide clear button
        const clearBtn = document.getElementById('chat-search-clear');
        if (clearBtn) clearBtn.style.display = q ? 'block' : 'none';

        state.searchMatches = [];
        state.currentMatchIndex = -1;

        if (!q) {
            Array.from(wrapper.children).forEach(el => {
                el.style.display = '';
                window.ChatApp.Renderer.clearHighlight(el);
            });
            updateMatchCounter();
            return;
        }

        let regex;
        if (q.startsWith('/') && q.endsWith('/') && q.length > 2) {
            try { regex = new RegExp(q.substring(1, q.length - 1), 'gi'); } catch(e) {}
        }
        const queryLower = q.toLowerCase();

        Array.from(wrapper.children).forEach((el, idx) => {
            const m = state.messages[idx];
            if (!m) return;
            const text = m.text || '';
            const sender = m.sender || '';

            let matches = false;
            if (regex) {
                matches = regex.test(text) || regex.test(sender);
            } else {
                matches = text.toLowerCase().includes(queryLower) || sender.toLowerCase().includes(queryLower);
            }

            el.style.display = matches ? '' : 'none';
            if (matches) {
                state.searchMatches.push(idx);
                window.ChatApp.Renderer.highlightMatches(el, regex || queryLower);
            } else {
                window.ChatApp.Renderer.clearHighlight(el);
            }
        });

        if (state.searchMatches.length > 0) {
            state.currentMatchIndex = 0;
            window.navigateSearch(0);
        }
        updateMatchCounter();
    };

    window.hasSearchMatches = function() {
        return state.searchMatches && state.searchMatches.length > 0;
    };

    window.navigateSearch = function(dir) {
        if (state.searchMatches.length === 0) return;

        // Remove 'current' from previous match
        if (state.currentMatchIndex >= 0) {
            const prevEl = document.getElementById('messages-wrapper').children[state.searchMatches[state.currentMatchIndex]];
            if (prevEl) prevEl.classList.remove('active-match');
        }

        state.currentMatchIndex += dir;
        if (state.currentMatchIndex >= state.searchMatches.length) state.currentMatchIndex = 0;
        if (state.currentMatchIndex < 0) state.currentMatchIndex = state.searchMatches.length - 1;

        const targetIdx = state.searchMatches[state.currentMatchIndex];
        const el = document.getElementById('messages-wrapper').children[targetIdx];
        if (el) {
            el.classList.add('active-match');
            el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
        updateMatchCounter();
    };

    function updateMatchCounter() {
        const counter = document.getElementById('search-match-count');
        if (!counter) return;
        if (!state.searchQuery) {
            counter.innerText = '';
            return;
        }
        counter.innerText = `${state.searchMatches.length > 0 ? state.currentMatchIndex + 1 : 0} / ${state.searchMatches.length}`;
    }

    window.updateChanges = function(files) {
        if (!state.isUiReady) {
            state.pendingChanges = files;
            return;
        }
        state.changes = files;
        window.ChatApp.Panel.renderChanges(files, state.lastDiffs);
    };

    window.showDiff = function(data) {
        state.lastDiffs[data.path] = data.diff;
        const container = document.getElementById(`diff-inline-${data.path}`);
        if (container) container.innerHTML = window.ChatApp.Panel.renderDiff(data.diff);
    };

    window.showThinking = function(show) {
        const t = document.getElementById('thinking');
        if (t) t.style.display = show ? 'flex' : 'none';
        if (show) window.ChatApp.UI.scrollToBottom();
    };

    window.setFeedbackLevel = function(level) {
        document.body.className = level;
    };

    // Global Bridge Helpers
    document.addEventListener('keydown', (e) => {
        if (e.target.id === 'chat-search-input' && e.key === 'Enter') {
            window.navigateSearch(1);
        }

        // Handle Ctrl+A for chat content
        if ((e.ctrlKey || e.metaKey) && e.key === 'a' && e.target.tagName !== 'INPUT' && e.target.tagName !== 'TEXTAREA') {
            e.preventDefault();
            window.selectAll();
        }

        // Remove newly-created highlights on any user interaction
        document.querySelectorAll('.file-stack-item.newly-created').forEach(el => el.classList.remove('newly-created'));
    });

    document.addEventListener('mousedown', () => {
        document.querySelectorAll('.file-stack-item.newly-created').forEach(el => el.classList.remove('newly-created'));
    });

    window.selectAll = function() {
        const wrapper = document.getElementById('messages-wrapper');
        if (!wrapper) return;
        const range = document.createRange();
        range.selectNodeContents(wrapper);
        const sel = window.getSelection();
        sel.removeAllRanges();
        sel.addRange(range);
    };

    window.quoteSelection = function() {
        const text = window.getSelection().toString().trim();
        if (text) window.ChatApp.Actions.callJava('quote', '-1', text);
        document.getElementById('floating-quote-btn').style.display = 'none';
    };
    window.scrollToTop = () => { const c = document.getElementById('chat'); if(c) c.scrollTop = 0; };
    window.scrollToBottom = () => window.ChatApp.UI.scrollToBottom();
    window.toggleSidePanel = () => window.ChatApp.UI.toggleSidePanel();
    window.toggleAllFiles = () => {
        const items = document.querySelectorAll('.file-stack-item');
        const expand = Array.from(items).some(i => !i.classList.contains('expanded'));
        items.forEach(i => {
            if (expand) { if(!i.classList.contains('expanded')) window.ChatApp.Panel.toggleFileDiff(i.dataset.path); }
            else i.classList.remove('expanded');
        });
    };
    window.filterFiles = (q) => {
        const query = q.toLowerCase();
        document.querySelectorAll('.file-stack-item').forEach(i => {
            i.style.display = i.dataset.path.toLowerCase().includes(query) ? 'block' : 'none';
        });
    };
    window.downloadZip = () => window.ChatApp.Actions.callJava('downloadZip');
    window.commitChanges = () => {
        const msg = prompt("Enter commit message:", "Improvement from AI Chat");
        if (msg) window.ChatApp.Actions.callJava('commitGit', '0', msg);
    };
    window.menuAction = (a) => window.ChatApp.UI.menuAction(a);

    // Initialization
    function init() {
        console.log('AI Chat initializing...');
        window.ChatApp.UI.init();

        state.isUiReady = true;
        if (state.pendingMessages) {
            window.updateMessages(state.pendingMessages, state.pendingSessionId);
            state.pendingMessages = null;
            state.pendingSessionId = null;
        }
        if (state.pendingChanges) {
            window.updateChanges(state.pendingChanges);
            state.pendingChanges = null;
        }

        // Try to connect to Java
        let attempts = 0;
        const checkBridge = setInterval(() => {
            attempts++;
            if (window.JavaHandler) {
                clearInterval(checkBridge);
                window.ChatApp.Actions.processQueue();
                window.ChatApp.Actions.callJava('ready');
                if (window.hideLoading) window.hideLoading();
            } else if (attempts > 20) { // 4 seconds
                clearInterval(checkBridge);
                console.warn('Java bridge connection timed out');
                if (window.hideLoading) window.hideLoading();
            }
        }, 200);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
