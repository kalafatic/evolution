window.ChatApp = window.ChatApp || {};

window.ChatApp.UI = {
    m_pos: 0,
    currentContextFile: null,

    init: function() {
        this.initResizing();
        this.initQuote();
    },

    initResizing: function() {
        this.setupPanelResizing('side-panel', 'resize-handle');
        this.setupPanelResizing('progress-panel', 'progress-resize-handle');
    },

    setupPanelResizing: function(panelId, handleId) {
        const panel = document.getElementById(panelId);
        const handle = document.getElementById(handleId);
        if (!handle || !panel) return;

        let startX, startWidth;

        handle.addEventListener("mousedown", (e) => {
            startX = e.clientX;
            startWidth = parseInt(getComputedStyle(panel).width);
            panel.style.transition = 'none';

            const onMouseMove = (moveEvent) => {
                const dx = startX - moveEvent.clientX;
                const newWidth = startWidth + dx;
                if (newWidth > 0) {
                    panel.style.width = newWidth + "px";
                }
            };

            const onMouseUp = () => {
                panel.style.transition = 'width 0.3s ease';
                document.removeEventListener("mousemove", onMouseMove);
                document.removeEventListener("mouseup", onMouseUp);
                document.body.style.cursor = '';
            };

            document.addEventListener("mousemove", onMouseMove);
            document.addEventListener("mouseup", onMouseUp);
            document.body.style.cursor = 'col-resize';
            e.preventDefault();
        }, false);
    },

    initQuote: function() {
        document.addEventListener('mouseup', (e) => {
            const btn = document.getElementById('floating-quote-btn');
            const sel = window.getSelection();
            if (sel.rangeCount > 0 && !sel.isCollapsed) {
                const rect = sel.getRangeAt(0).getBoundingClientRect();
                btn.style.display = 'block';
                btn.style.left = (rect.left + rect.width / 2 - btn.offsetWidth / 2) + 'px';
                btn.style.top = (rect.top - btn.offsetHeight - 8) + 'px';
            } else {
                setTimeout(() => { if (window.getSelection().isCollapsed) btn.style.display = 'none'; }, 100);
            }
        });
    },

    showContextMenu: function(e, path) {
        e.preventDefault();
        this.currentContextFile = path;
        const menu = document.getElementById('context-menu');
        menu.style.display = 'block';
        menu.style.left = e.pageX + 'px';
        menu.style.top = e.pageY + 'px';
        const hide = () => { menu.style.display = 'none'; document.removeEventListener('click', hide); };
        setTimeout(() => document.addEventListener('click', hide), 10);
    },

    menuAction: function(action) {
        if (!this.currentContextFile) return;
        let path = this.currentContextFile;
        // Strip status prefix
        if (path.length > 2 && (path.startsWith('M ') || path.startsWith('A ') || path.startsWith('D '))) path = path.substring(2);

        switch(action) {
            case 'workspace': window.ChatApp.Actions.callJava('openInWorkspace', '-1', path); break;
            case 'review': window.ChatApp.Actions.callJava('openInReviewEditor', '-1', path); break;
            case 'revert': if (confirm('Revert ' + path + '?')) window.ChatApp.Actions.callJava('revertFile', '-1', this.currentContextFile); break;
            case 'copyPath': window.ChatApp.Actions.callJava('copy', '-1', path); break;
        }
    },

    scrollToBottom: function() {
        const chat = document.getElementById('chat');
        if (chat) chat.scrollTop = chat.scrollHeight;
    },

    toggleSidePanel: function() {
        const panel = document.getElementById('side-panel');
        if (panel) panel.style.width = (panel.style.width === '0px' || panel.style.width === '0') ? '320px' : '0px';
    },

    ensureSidePanelOpen: function() {
        const panel = document.getElementById('side-panel');
        if (panel && (panel.style.width === '0px' || panel.style.width === '0')) {
            panel.style.width = '320px';
        }
    },

    updateLayout: function() {
        // Trigger reflow if needed, though flexbox should handle it
        window.dispatchEvent(new Event('resize'));
    }
};
