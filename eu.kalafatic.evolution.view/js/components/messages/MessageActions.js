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
            const btn = document.createElement('button');
            btn.className = 'action-btn approve';
            btn.title = 'Approve';
            btn.textContent = '✅';
            btn.onclick = (e) => {
                e.stopPropagation();
                window.dispatchEvent(new CustomEvent('java:approve', { detail: this.index }));
            };
            actions.appendChild(btn);
        }

        const quoteBtn = document.createElement('button');
        quoteBtn.className = 'action-btn';
        quoteBtn.title = 'Quote';
        quoteBtn.textContent = '”';
        quoteBtn.onclick = (e) => {
            e.stopPropagation();
            window.dispatchEvent(new CustomEvent('java:quote', { detail: { index: this.index, text: this.text } }));
        };
        actions.appendChild(quoteBtn);

        const copyBtn = document.createElement('button');
        copyBtn.className = 'action-btn';
        copyBtn.title = 'Copy Message';
        copyBtn.textContent = '📋';
        copyBtn.onclick = (e) => {
            e.stopPropagation();
            window.dispatchEvent(new CustomEvent('java:copy', { detail: this.text }));
        };
        actions.appendChild(copyBtn);

        const collapseBtn = document.createElement('button');
        collapseBtn.className = 'action-btn';
        collapseBtn.title = 'Collapse/Expand';
        collapseBtn.textContent = '↕️';
        collapseBtn.onclick = (e) => {
            e.stopPropagation();
            window.dispatchEvent(new CustomEvent('ui:toggleCollapse', { detail: this.index }));
        };
        actions.appendChild(collapseBtn);

        return actions;
    }
}
