import { Message } from './Message.js';

export class MessageList {
    constructor(containerId, icons) {
        this.container = document.getElementById(containerId);
        this.wrapper = document.getElementById('messages-wrapper');
        this.icons = icons;
        this.messages = [];
    }

    update(messages) {
        if (!messages) {
            console.warn('MessageList.update received null/undefined messages');
            return;
        }
        const wasAtBottom = (this.container.scrollHeight - this.container.scrollTop - this.container.clientHeight) < 100;

    render() {
        if (!this.container) {
            if (window.JavaLog) window.JavaLog('Error: MessageList container not found');
            return;
        }
        const existingElements = this.container.querySelectorAll('.message');
        const existingIndices = Array.from(existingElements).map(el => parseInt(el.dataset.index));

        this.messages.forEach((msg) => {
            if (!existingIndices.includes(msg.index)) {
                const icon = this.icons[msg.agentType.split(' ')[0]] || this.icons.ai;
                const messageComponent = new Message(msg, icon);
                const element = messageComponent.render();
                if (this.collapsedIndices.has(msg.index)) {
                    element.classList.add('collapsed');
                }
                this.container.appendChild(element);
            } else {
                const element = this.container.querySelector(`.message[data-index="${msg.index}"]`);
                if (element) {
                    const role = (msg.agentType || '').toLowerCase();
                    const wasApproved = element.classList.contains('approved');
                    const isApproved = role.includes('approved');

                    if (isApproved && !wasApproved) {
                        element.classList.add('approved');
                        // Update content for darwin variants to show watermarks
                        if (role.includes('darwin')) {
                             const block = element.querySelector('.agent-block');
                             if (block) {
                                 block.innerHTML = '';
                                 const darwin = new DarwinContainer(msg);
                                 block.appendChild(darwin.render());
                             }
                        }
                    }
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

        const chat = document.getElementById('chat');
        if (chat && (chat.scrollHeight - chat.scrollTop < chat.clientHeight + 200)) {
            this.scrollToBottom();
        }

        if (messages.some(m => (m.agentType || '').toLowerCase().includes('waiting'))) {
            this.scrollToLastWaiting();
        }
    }

    scrollToBottom() {
        const chat = document.getElementById('chat');
        if (chat) chat.scrollTop = chat.scrollHeight;
    }

    scrollToTop() {
        const chat = document.getElementById('chat');
        if (chat) chat.scrollTop = 0;
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
