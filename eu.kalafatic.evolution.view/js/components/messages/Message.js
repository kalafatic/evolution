import MessageFormatter from './MessageFormatter.js';
import MessageActions from './MessageActions.js';
import DarwinContainer from '../darwin/DarwinContainer.js';
import JavaBridge from '../../core/JavaBridge.js';

class Message {
    constructor(data, icon) {
        this.data = data;
        this.icon = icon;
    }

    render() {
        const { index, sender, text, timestamp, agentType } = this.data;
        const role = (agentType || '').toLowerCase();
        const roles = role.split(' ');
        const primaryRole = roles[0];

        const senderLower = (sender || '').toLowerCase();
        const isUser = primaryRole === 'user' || senderLower.includes('you') || senderLower.includes('user');

        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${isUser ? 'user' : 'ai'} ${role}`;
        messageDiv.dataset.index = index;
        if (role.includes('approved')) messageDiv.classList.add('approved');

        const contentWrapper = document.createElement('div');
        contentWrapper.className = 'message-content';

        const header = document.createElement('div');
        header.className = 'header';
        header.innerHTML = `
            <span class="icon">${this.icon}</span>
            <span class="sender">${sender}</span>
            <span class="timestamp">${timestamp || ''}</span>
        `;

        const block = document.createElement('div');
        block.className = 'agent-block';
        block.onclick = () => window.dispatchEvent(new CustomEvent('ui:toggleCollapse', { detail: index }));

        let isDarwin = role.includes('darwin');
        if (isDarwin || role.includes('darwin-branches')) {
            const darwin = new DarwinContainer(this.data);
            block.appendChild(darwin.render());
        } else {
            const bubble = document.createElement('div');
            bubble.className = 'bubble-content';
            bubble.innerHTML = MessageFormatter.format(text);
            block.appendChild(bubble);
        }

        div.appendChild(header);
        div.appendChild(msgContent);
        return div;
    }

    renderStandard(container, primaryRole) {
        const { index, text, agentType } = this.data;
        const role = (agentType || '').toLowerCase();

        const block = document.createElement('div');
        block.className = 'bubble';
        block.onclick = () => JavaBridge.call('edit', index, text);

        const content = document.createElement('div');
        content.className = 'bubble-content';
        content.innerHTML = MessageFormatter.formatText(text, primaryRole);
        block.appendChild(content);

        const actions = new MessageActions(index, text, role);

        contentWrapper.appendChild(header);
        contentWrapper.appendChild(block);

        messageDiv.appendChild(contentWrapper);
        messageDiv.appendChild(actions.render());

        return messageDiv;
    }
}

export default Message;
