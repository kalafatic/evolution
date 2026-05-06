import Message from './Message.js';
import DarwinContainer from '../darwin/DarwinContainer.js';

class MessageList {
    constructor(containerId, icons) {
        this.container = document.getElementById(containerId);
        this.icons = icons;
        this.messages = [];
        this.collapsedIndices = new Set();
    }

    update(messages) {
        if (messages.length < this.messages.length) {
            // Thread was likely reset or switched
            this.container.innerHTML = '';
        }
        this.messages = messages;
        this.render();
    }

    render() {
        if (!this.container) {
            this.container = document.getElementById('messages-wrapper');
        }
        if (!this.container) {
            if (window.JavaLog) window.JavaLog('Error: MessageList container not found');
            return;
        }
        if (window.JavaLog) window.JavaLog(`Rendering ${this.messages.length} messages`);

        const existingElements = this.container.querySelectorAll('.message');
        const existingIndices = Array.from(existingElements).map(el => parseInt(el.dataset.index));

        this.messages.forEach((msg) => {
            if (!msg || typeof msg !== 'object') return;

            if (!existingIndices.includes(msg.index)) {
                const agentKey = (msg.agentType || 'ai').split(' ')[0];
                const icon = this.icons[agentKey] || this.icons.ai;
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
        });

        const chat = document.getElementById('chat');
        if (chat && (chat.scrollHeight - chat.scrollTop < chat.clientHeight + 200)) {
            this.scrollToBottom();
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
        const waiting = this.container.querySelectorAll('.message.waiting');
        if (waiting.length > 0) {
            waiting[waiting.length - 1].scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    }

    toggleCollapse(index) {
        if (this.collapsedIndices.has(index)) {
            this.collapsedIndices.delete(index);
        } else {
            this.collapsedIndices.add(index);
        }
        const element = this.container.querySelector(`.message[data-index="${index}"]`);
        if (element) {
            element.classList.toggle('collapsed');
        }
    }

    toggleAll() {
        const allMessages = this.container.querySelectorAll('.message');
        const shouldCollapse = Array.from(allMessages).some(el => !el.classList.contains('collapsed'));

        allMessages.forEach(el => {
            const index = parseInt(el.dataset.index);
            if (shouldCollapse) {
                el.classList.add('collapsed');
                this.collapsedIndices.add(index);
            } else {
                el.classList.remove('collapsed');
                this.collapsedIndices.delete(index);
            }
        });
    }
}

export default MessageList;
