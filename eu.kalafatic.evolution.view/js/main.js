window.ChatApp = window.ChatApp || {};

(function() {
    const state = {
        messages: [],
        changes: [],
        lastDiffs: {},
        isUiReady: false,
        pendingMessages: null,
        pendingChanges: null
    };

    window.updateMessages = function(messages) {
        if (!state.isUiReady) {
            state.pendingMessages = messages;
            return;
        }
        state.messages = messages;
        const wrapper = document.getElementById('messages-wrapper');
        if (!wrapper) return;
        wrapper.innerHTML = '';
        messages.forEach(m => wrapper.appendChild(window.ChatApp.Renderer.renderMessage(m)));
        window.ChatApp.UI.scrollToBottom();
    };

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
        if (t) t.style.display = show ? 'block' : 'none';
        if (show) window.ChatApp.UI.scrollToBottom();
    };

    window.setFeedbackLevel = function(level) {
        document.body.className = level;
    };

    // Global Bridge Helpers
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
            window.updateMessages(state.pendingMessages);
            state.pendingMessages = null;
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
