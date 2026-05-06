class ChatContainer {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.thinking = document.getElementById('thinking');
    }

    showThinking(show) {
        if (this.thinking) {
            this.thinking.style.display = show ? 'block' : 'none';
        }
    }
}

export default ChatContainer;
