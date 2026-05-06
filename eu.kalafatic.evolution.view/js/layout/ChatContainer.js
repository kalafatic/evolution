import { JavaBridge } from '../core/JavaBridge.js';

export class ChatContainer {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
    }

    showThinking(show) {
        const thinking = document.getElementById('thinking');
        if (!thinking) return;
        thinking.style.display = show ? 'block' : 'none';
        if (show) {
            this.container.scrollTo({ top: this.container.scrollHeight, behavior: 'smooth' });
        }
    }
}
