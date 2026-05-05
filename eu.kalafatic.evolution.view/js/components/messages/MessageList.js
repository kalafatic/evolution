import { Message } from './Message.js';

export class MessageList {
    constructor(containerId, icons) {
        this.container = document.getElementById(containerId);
        this.wrapper = document.getElementById('messages-wrapper');
        this.icons = icons;
        this.messages = [];
    }

    update(messages) {
        const wasAtBottom = (this.container.scrollHeight - this.container.scrollTop - this.container.clientHeight) < 100;

        // Sort messages so that 'waiting' status (active) is always at the end
        const sortedMessages = [...messages].sort((a, b) => {
            const aRole = (a.agentType || '').toLowerCase();
            const bRole = (b.agentType || '').toLowerCase();
            const aWaiting = aRole.includes('waiting') && !aRole.includes('approved');
            const bWaiting = bRole.includes('waiting') && !bRole.includes('approved');

            if (aWaiting && !bWaiting) return 1;
            if (!aWaiting && bWaiting) return -1;

            return (a.index || 0) - (b.index || 0);
        });

        // Optimization: Find what changed
        const newCount = sortedMessages.length;
        const oldCount = this.messages.length;

        let startIndex = 0;
        // If it's just new messages appended, don't clear all
        if (oldCount > 0 && newCount >= oldCount) {
            let matches = true;
            for (let i = 0; i < oldCount; i++) {
                if (JSON.stringify(sortedMessages[i]) !== JSON.stringify(this.messages[i])) {
                    matches = false;
                    startIndex = i;
                    break;
                }
            }
            if (matches) startIndex = oldCount;
        }

        if (startIndex === 0) {
            this.wrapper.innerHTML = '';
        } else {
            // Remove everything from startIndex onwards
            while (this.wrapper.children.length > startIndex) {
                this.wrapper.removeChild(this.wrapper.lastChild);
            }
        }

        for (let i = startIndex; i < newCount; i++) {
            const messageComponent = new Message(sortedMessages[i], this.icons);
            this.wrapper.appendChild(messageComponent.render());
        }

        this.messages = sortedMessages;

        if (wasAtBottom || messages.length === 1) {
            this.scrollToBottom();
        }

        if (messages.some(m => (m.agentType || '').toLowerCase().includes('waiting'))) {
            this.scrollToLastWaiting();
        }
    }

    scrollToBottom() {
        requestAnimationFrame(() => {
            this.container.scrollTo({ top: this.container.scrollHeight, behavior: 'smooth' });
        });
    }

    scrollToTop() {
        requestAnimationFrame(() => {
            this.container.scrollTo({ top: 0, behavior: 'smooth' });
        });
    }

    scrollToLastWaiting() {
        const messages = document.querySelectorAll('.message.waiting');
        if (messages.length > 0) {
            const lastWaiting = messages[messages.length - 1];
            const bubble = lastWaiting.querySelector('.bubble');
            lastWaiting.scrollIntoView({ behavior: 'smooth', block: 'center' });
            if (bubble) {
                bubble.classList.add('highlight-waiting');
                setTimeout(() => bubble.classList.remove('highlight-waiting'), 5000);
            }
        }
    }

    toggleCollapse(index) {
        const msg = document.getElementById('msg-' + index);
        if (msg) msg.classList.toggle('collapsed');
    }

    toggleAll() {
        const messages = document.querySelectorAll('.message');
        let anyOpen = Array.from(messages).some(m => !m.classList.contains('collapsed'));
        messages.forEach(m => {
            if (anyOpen) m.classList.add('collapsed');
            else m.classList.remove('collapsed');
        });
    }
}
