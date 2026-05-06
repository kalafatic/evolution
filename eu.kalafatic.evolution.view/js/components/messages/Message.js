import { MessageFormatter } from './MessageFormatter.js';
import { MessageActions } from './MessageActions.js';
import { JavaBridge } from '../../core/JavaBridge.js';

export class Message {
    constructor(data, icons) {
        this.data = data;
        this.icons = icons;
    }

    render() {
        const { index, sender, text, agentType, timestamp } = this.data;
        const role = (agentType || '').toLowerCase();
        const roles = role.split(' ');
        const primaryRole = roles[0];

        const senderLower = (sender || '').toLowerCase();
        const isUser = primaryRole === 'user' || senderLower.includes('you') || senderLower.includes('user');

        const div = document.createElement('div');
        div.className = `message ${isUser ? 'user' : 'ai'} ${role}`;
        div.id = `msg-${index}`;

        const header = document.createElement('div');
        header.className = 'header';

        const icon = document.createElement('span');
        icon.className = 'icon';
        icon.textContent = this.icons[primaryRole] || (isUser ? this.icons.user : this.icons.ai);

        const senderSpan = document.createElement('span');
        senderSpan.className = 'sender';
        senderSpan.textContent = sender;

        const time = document.createElement('span');
        time.className = 'timestamp';
        time.textContent = timestamp || '';

        header.appendChild(icon);
        header.appendChild(senderSpan);
        header.appendChild(time);

        const msgContent = document.createElement('div');
        msgContent.className = 'message-content';

        let isDarwin = role.includes('darwin');
        if (!isDarwin && !role.includes('thinking')) {
            try {
                const parsed = JSON.parse(text);
                if (parsed.variants || parsed.proposals || (Array.isArray(parsed) && parsed.length > 0 && parsed[0].strategy)) {
                    isDarwin = true;
                }
            } catch (e) {}
        }

        if (isDarwin) {
            this.renderDarwin(msgContent);
        } else {
            this.renderStandard(msgContent, primaryRole);
        }

        div.appendChild(header);
        div.appendChild(msgContent);
        return div;
    }

    renderStandard(container, primaryRole) {
        const { index, text, agentType } = this.data;
        const role = (agentType || '').toLowerCase();

        const block = document.createElement('div');
        block.className = 'agent-block';
        block.onclick = () => JavaBridge.call('edit', index, text);

        const content = document.createElement('div');
        content.className = 'bubble-content';
        content.innerHTML = MessageFormatter.formatText(text, primaryRole);
        block.appendChild(content);

        const actions = new MessageActions(index, text, role);

        container.appendChild(block);
        container.appendChild(actions.render());
    }

    async renderDarwin(container) {
        const { DarwinContainer } = await import('../darwin/DarwinContainer.js');
        const darwin = new DarwinContainer(this.data);
        container.appendChild(darwin.render());
    }
}
