import JavaBridge from '../../core/JavaBridge.js';

class MessageActions {
    constructor(index, text, role) {
        this.index = index;
        this.text = text;
        this.role = role;
    }

    render() {
        const container = document.createElement('div');
        container.className = 'actions';

        const copyBtn = this.createButton('📋', 'Copy text', () => {
            window.dispatchEvent(new CustomEvent('java:copy', { detail: this.text }));
        });

        const quoteBtn = this.createButton('💬', 'Quote message', () => {
            window.dispatchEvent(new CustomEvent('java:quote', { detail: { index: this.index, text: this.text } }));
        });

        container.appendChild(copyBtn);
        container.appendChild(quoteBtn);

        if (this.role.includes('waiting') || this.role.includes('final-response')) {
            const approveBtn = this.createButton('✅', 'Approve', () => {
                window.dispatchEvent(new CustomEvent('java:approve', { detail: this.index }));
            });
            approveBtn.classList.add('approve');
            container.appendChild(approveBtn);
        }

        return container;
    }

    createButton(icon, title, onClick) {
        const btn = document.createElement('button');
        btn.className = 'action-btn';
        btn.textContent = icon;
        btn.title = title;
        btn.onclick = (e) => {
            e.stopPropagation();
            onClick();
        };
        return btn;
    }
}

export default MessageActions;
