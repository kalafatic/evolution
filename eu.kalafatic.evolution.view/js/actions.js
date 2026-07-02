window.ChatApp = window.ChatApp || {};

window.ChatApp.Actions = {
    queue: [],
    isReady: false,

    callJava: function(action, index, payload) {
        if (window.JavaHandler) {
            const safeIndex = (index === undefined || index === null) ? "-1" : String(index);
            window.JavaHandler(String(action), safeIndex, payload !== undefined ? String(payload) : "");
        } else {
            console.warn('Java bridge not ready. Queuing action:', action);
            this.queue.push({ action, index, payload });
        }
    },

    processQueue: function() {
        if (window.JavaHandler) {
            console.log('Processing queued Java calls:', this.queue.length);
            while (this.queue.length > 0) {
                const item = this.queue.shift();
                this.callJava(item.action, item.index, item.payload);
            }
            this.isReady = true;
        }
    },

    addDimension: function() {
        const input = document.getElementById('add-dimension-input');
        if (input && input.value.trim()) {
            this.callJava('addDimension', '-1', input.value.trim());
            input.value = '';
        }
    },

    renderActions: function(m) {
        const container = document.createElement('div');
        container.className = 'actions';

        const copy = document.createElement('button');
        copy.className = 'action-btn';
        copy.textContent = '📋';
        copy.onclick = (e) => { e.stopPropagation(); this.callJava('copy', '-1', m.text); };

        const quote = document.createElement('button');
        quote.className = 'action-btn';
        quote.textContent = '”';
        quote.onclick = (e) => { e.stopPropagation(); this.callJava('quote', m.index, m.text); };

        container.appendChild(copy);
        container.appendChild(quote);

        const role = (m.agentType || '').toLowerCase();
        if (role.includes('waiting') && !role.includes('approved')) {
            const app = document.createElement('button');
            app.className = 'action-btn approve';
            app.textContent = '✅';
            app.onclick = (e) => { e.stopPropagation(); this.callJava('approve', m.index); };
            container.appendChild(app);
        }

        return container;
    }
};
