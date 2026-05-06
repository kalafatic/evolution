import EventBus from './core/EventBus.js';
import StateStore from './core/StateStore.js';
import JavaBridge from './core/JavaBridge.js';
import MessageList from './components/messages/MessageList.js';
import ChangesPanel from './components/git/ChangesPanel.js';
import ChatContainer from './layout/ChatContainer.js';
import SidePanel from './layout/SidePanel.js';

// Icons
const icons = {
    user: '👤', ai: '🤖', planner: '📋', architect: '📐',
    javadev: '💻', tester: '🧪', reviewer: '⚖️', tool: '⚙️',
    analytic: '🔍', general: '🧠', terminal: '📟', file: '📂',
    maven: '📦', git: '🌿', structure: '🌳', websearch: '🌐',
    quality: '✨', observability: '📊', orchestrator: '🎼', darwin: '🧬',
    'final-response': '✅', 'result-summary': 'ℹ️', waiting: '❓', error: '❌',
    thinking: '💭', response: '💬'
};

// Initial State
const initialState = {
    messages: [],
    changes: [],
    lastDiffs: {},
    isThinking: false,
    feedbackLevel: 'full',
    currentContextFile: null
};

// Initialize Core
const eventBus = new EventBus();
const stateStore = new StateStore(initialState, eventBus);

// Initialize Components
const messageList = new MessageList('messages-wrapper', icons);
const changesPanel = new ChangesPanel('changes-list');
const chatContainer = new ChatContainer('chat');
const sidePanel = new SidePanel('side-panel');

if (window.JavaLog) window.JavaLog('Components initialized');

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

// Java Bridge Event Handlers
window.addEventListener('java:approve', (e) => {
    if (window.JavaLog) window.JavaLog('Approve requested for index ' + e.detail);
    JavaBridge.call('approve', e.detail);
});
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
window.addEventListener('java:getDiff', (e) => JavaBridge.call('getDiff', '-1', e.detail));
window.addEventListener('java:executeProposal', (e) => JavaBridge.call('executeProposal', '-1', e.detail));
window.addEventListener('java:clarify', () => JavaBridge.call('clarify'));
window.addEventListener('java:helloworld', () => JavaBridge.call('helloworld'));

// Expose global functions for Java interaction
window.updateMessages = (messages) => {
    if (window.JavaLog) window.JavaLog(`Updating ${messages.length} messages`);
    stateStore.setState({ messages });
};
window.updateChanges = (files) => stateStore.setState({ changes: files });
window.showDiff = (data) => {
    const lastDiffs = { ...stateStore.getState().lastDiffs };
    lastDiffs[data.path] = data.diff;
    stateStore.setState({ lastDiffs });
    changesPanel.showDiff(data);
};
window.showThinking = (show) => stateStore.setState({ isThinking: show });
window.setFeedbackLevel = (level) => stateStore.setState({ feedbackLevel: level });

// Navigation & Global Actions
window.scrollToTop = () => messageList.scrollToTop();
window.scrollToBottom = () => messageList.scrollToBottom();
window.scrollToLastWaiting = () => messageList.scrollToLastWaiting();
window.toggleAll = () => messageList.toggleAll();
window.toggleSidePanel = () => sidePanel.toggle();
window.toggleAllFiles = () => changesPanel.toggleAllFiles();
window.filterFiles = (query) => changesPanel.filterFiles(query);
window.downloadZip = () => JavaBridge.call('downloadZip');
window.commitChanges = () => {
    const message = prompt("Enter commit message:", "Improvement from AI Chat");
    if (message) JavaBridge.call('commitGit', '0', message);
};
window.menuAction = (action) => {
    const state = stateStore.getState();
    sidePanel.menuAction(action, state.currentContextFile, state.lastDiffs[state.currentContextFile]);
};

window.quoteSelection = () => {
    const sel = window.getSelection();
    const text = sel.toString().trim();
    if (text) {
        JavaBridge.call('quote', '-1', text);
        sel.removeAllRanges();
    }
    document.getElementById('floating-quote-btn').style.display = 'none';
};

window.selectAll = () => {
    let text = '';
    const { messages } = stateStore.getState();
    messages.forEach(m => {
        text += `${m.sender} [${m.timestamp || ''}]: ${m.text}\n\n`;
    });
    if (text) JavaBridge.call('copy', '-1', text.trim());
};

// Ready handshake
if (window.JavaLog) window.JavaLog('Handshake initiated');

if (typeof window.hideLoading === 'function') {
    window.hideLoading();
}

if (window.JavaHandler) {
    JavaBridge.call('ready');
}

console.log('AI Chat Kernel (ESM) Initialized');
