import { eventBus } from './core/EventBus.js';
import { StateStore, initialState } from './core/StateStore.js';
import { JavaBridge } from './core/JavaBridge.js';
import { MessageList } from './components/messages/MessageList.js';
import { ChangesPanel } from './components/git/ChangesPanel.js';
import { ChatContainer } from './layout/ChatContainer.js';
import { SidePanel } from './layout/SidePanel.js';

// Initialize Core
const stateStore = new StateStore(initialState, eventBus);

const icons = {
    user: '👤', ai: '🤖', planner: '📋', architect: '📐',
    javadev: '💻', tester: '🧪', reviewer: '⚖️', tool: '⚙️',
    analytic: '🔍', general: '🧠', terminal: '📟', file: '📂',
    maven: '📦', git: '🌿', structure: '🌳', websearch: '🌐',
    quality: '✨', observability: '📊', orchestrator: '🎼', darwin: '🧬',
    'final-response': '✅', 'result-summary': 'ℹ️', waiting: '❓', error: '❌',
    thinking: '💭', response: '💬'
};

// Initialize Components
const messageList = new MessageList('chat', icons);
const changesPanel = new ChangesPanel('changes-list');
const chatContainer = new ChatContainer('chat');
const sidePanel = new SidePanel('side-panel');

// State Subscriptions
stateStore.subscribe((state, oldState) => {
    if (!oldState || state.messages !== oldState.messages) {
        messageList.update(state.messages);
    }
    if (!oldState || state.changes !== oldState.changes || state.lastDiffs !== oldState.lastDiffs) {
        changesPanel.update(state.changes, state.lastDiffs);
    }
    if (!oldState || state.isThinking !== oldState.isThinking) {
        chatContainer.showThinking(state.isThinking);
    }
    if (!oldState || state.feedbackLevel !== oldState.feedbackLevel) {
        document.body.className = state.feedbackLevel;
    }
});

// Event Listeners for UI Actions
window.addEventListener('ui:toggleCollapse', (e) => messageList.toggleCollapse(e.detail));
window.addEventListener('ui:toggleFileDiff', (e) => changesPanel.toggleFileDiff(e.detail));
window.addEventListener('ui:showContextMenu', (e) => sidePanel.showContextMenu(e.detail.event, e.detail.path));
window.addEventListener('ui:contextFileChanged', (e) => stateStore.setState({ currentContextFile: e.detail }));

// Java Bridge Event Handlers (Dispatched from components)
window.addEventListener('java:approve', (e) => JavaBridge.call('approve', e.detail));
window.addEventListener('java:approveDarwinVariant', (e) => {
    JavaBridge.call('approveDarwinVariant', e.detail.index, e.detail.variantId);
});
window.addEventListener('java:copy', (e) => JavaBridge.call('copy', '-1', e.detail));
window.addEventListener('java:quote', (e) => {
    if (typeof e.detail === 'object') {
        JavaBridge.call('quote', e.detail.index, e.detail.text);
    } else {
        JavaBridge.call('quote', '-1', e.detail);
    }
});
window.addEventListener('java:openDiff', (e) => JavaBridge.call('openDiff', '-1', e.detail));
window.addEventListener('java:executeProposal', (e) => JavaBridge.call('executeProposal', '-1', e.detail));
window.addEventListener('java:clarify', () => JavaBridge.call('clarify'));

// Expose global functions for legacy/simple calls from Java or HTML
window._updateMessages = (messages) => stateStore.setState({ messages });
window._updateChanges = (files) => stateStore.setState({ changes: files });
window._showDiff = (data) => {
    const lastDiffs = { ...stateStore.getState().lastDiffs, [data.path]: data.diff };
    stateStore.setState({ lastDiffs });
    changesPanel.showDiff(data);
};
window._showThinking = (show) => stateStore.setState({ isThinking: show });
window._setFeedbackLevel = (level) => stateStore.setState({ feedbackLevel: level });
window._scrollToTop = () => messageList.scrollToTop();
window._scrollToBottom = () => messageList.scrollToBottom();
window._scrollToLastWaiting = () => messageList.scrollToLastWaiting();
window._toggleAll = () => messageList.toggleAll();
window._selectAll = () => {
    let text = '';
    const { messages } = stateStore.getState();
    messages.forEach(m => {
        text += `${m.sender} [${m.timestamp || ''}]: ${m.text}\n\n`;
    });
    if (text) JavaBridge.call('copy', '-1', text.trim());
};
window._quoteSelection = () => {
    const sel = window.getSelection();
    const text = sel.toString().trim();
    if (text) {
        JavaBridge.call('quote', '-1', text);
        sel.removeAllRanges();
    }
    document.getElementById('floating-quote-btn').style.display = 'none';
};

// Process queued calls from robust bootstrap
const processQueue = () => {
    if (window._evoQueue && window._evoQueue.length > 0) {
        console.log(`Processing ${window._evoQueue.length} queued UI calls`);
        window._evoQueue.forEach(item => {
            const internalFn = "_" + item.fn;
            if (typeof window[internalFn] === 'function') {
                window[internalFn](...item.args);
            } else {
                console.warn(`Internal function ${internalFn} not found in window`);
            }
        });
        window._evoQueue = [];
    }
};

// Ensure all global assignments are done before setting ready
setTimeout(() => {
    window.uiReady = true;
    processQueue();
    // Signal to Java that the JS environment is ready
    JavaBridge.call('ready', '0', 'AI Chat Kernel Initialized');
}, 50);

// Navigation & Global Actions
// Note: legacy global names are handled via queueOrRun in chat.html
window.toggleSidePanel = () => sidePanel.toggle();
window.toggleAllFiles = () => changesPanel.toggleAllFiles();
window.filterFiles = (query) => changesPanel.filterFiles(query);
window.downloadZip = () => JavaBridge.call('downloadZip');
window.commitChanges = () => {
    const message = prompt("Enter commit message:", "Improvement from AI Chat");
    if (message) JavaBridge.call('commitGit', '0', message);
};
window.menuAction = (action) => {
    const { currentContextFile, lastDiffs } = stateStore.getState();
    sidePanel.menuAction(action, currentContextFile, lastDiffs[currentContextFile]);
};

// Selection / Quote Logic
window.quoteSelection = () => {
    const sel = window.getSelection();
    const text = sel.toString().trim();
    if (text) {
        JavaBridge.call('quote', '-1', text);
        sel.removeAllRanges();
    }
    document.getElementById('floating-quote-btn').style.display = 'none';
};

const showFloatingQuote = (e) => {
    const btn = document.getElementById('floating-quote-btn');
    const sel = window.getSelection();
    if (sel.rangeCount > 0 && !sel.isCollapsed) {
        const range = sel.getRangeAt(0);
        const rect = range.getBoundingClientRect();
        btn.style.display = 'block';
        btn.style.left = (rect.left + rect.width / 2 - btn.offsetWidth / 2) + 'px';
        btn.style.top = (rect.top - btn.offsetHeight - 8) + 'px';
    } else {
        btn.style.display = 'none';
    }
};

document.addEventListener('mouseup', (e) => setTimeout(() => showFloatingQuote(e), 10));
document.getElementById('chat').addEventListener('scroll', () => {
    document.getElementById('floating-quote-btn').style.display = 'none';
});

// Select All function
window.selectAll = () => {
    let text = '';
    const { messages } = stateStore.getState();
    messages.forEach(m => {
        text += `${m.sender} [${m.timestamp || ''}]: ${m.text}\n\n`;
    });
    if (text) JavaBridge.call('copy', '-1', text.trim());
};

console.log('AI Chat Kernel Initialized');
