import { MessageFormatter } from './MessageFormatter.js';

export class MessageActions {
    constructor(index, text, role) {
        this.index = index;
        this.text = text;
        this.role = role;
    }

    render() {
        const actions = document.createElement('div');
        actions.className = 'actions';

        let approveBtn = '';
        if (this.role.includes('waiting') && !this.role.includes('approved')) {
            approveBtn = `<button class="action-btn approve" title="Approve" onclick="event.stopPropagation(); window.dispatchEvent(new CustomEvent('java:approve', {detail: ${this.index}}))">✅</button>`;
        }

        actions.innerHTML = `
            ${approveBtn}
            <button class="action-btn" title="Quote" onclick="event.stopPropagation(); window.dispatchEvent(new CustomEvent('java:quote', {detail: {index: ${this.index}, text: \`${MessageFormatter.escapeJs(this.text)}\` }}))">”</button>
            <button class="action-btn" title="Copy Message" onclick="event.stopPropagation(); window.dispatchEvent(new CustomEvent('java:copy', {detail: \`${MessageFormatter.escapeJs(this.text)}\` }))">📋</button>
            <button class="action-btn" title="Collapse/Expand" onclick="event.stopPropagation(); window.dispatchEvent(new CustomEvent('ui:toggleCollapse', {detail: ${this.index}}))">↕️</button>
        `;

        return actions;
    }
}
