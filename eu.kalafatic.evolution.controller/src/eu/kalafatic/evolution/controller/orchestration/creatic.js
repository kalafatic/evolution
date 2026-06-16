/**
 * Creatic Assistant UI Component
 */
(function() {
    const CREATIC_PANEL_ID = 'creatic-panel-root';

    class CreaticUI {
        constructor() {
            this.container = null;
            this.content = null;
            this.pageId = this.detectPage();
            this.isCollapsed = true;
            this.isEnabled = true;
        }

        detectPage() {
            const path = window.location.pathname;
            if (path.includes('forge')) return 'forge';
            if (path.includes('chat')) return 'chat';
            if (document.title.includes('Architecture')) return 'architecture';
            return 'general';
        }

        async init() {
            if (!this.isEnabled) return;
            this.renderBaseStructure();
            await this.refresh();
        }

        renderBaseStructure() {
            if (document.getElementById(CREATIC_PANEL_ID)) return;

            const root = document.createElement('div');
            root.id = CREATIC_PANEL_ID;
            root.className = 'creatic-root collapsed';

            root.innerHTML = `
                <div class="creatic-header">
                    <span class="creatic-title">✨ Creatic Assistant</span>
                    <button class="creatic-toggle">◀</button>
                </div>
                <div class="creatic-body">
                    <div id="creatic-loader" class="creatic-loader">Analyzing context...</div>
                    <div id="creatic-content" style="display:none">
                        <div class="creatic-summary"></div>
                        <div class="creatic-section">
                            <label>Next Actions</label>
                            <div class="creatic-actions"></div>
                        </div>
                        <div class="creatic-section">
                            <label>Insights</label>
                            <div class="creatic-insights"></div>
                        </div>
                        <div class="creatic-section">
                            <label>Warnings</label>
                            <div class="creatic-warnings"></div>
                        </div>
                        <div class="creatic-section">
                            <label>Tips</label>
                            <div class="creatic-tips"></div>
                        </div>
                    </div>
                </div>
            `;

            document.body.appendChild(root);
            this.container = root;

            const toggle = root.querySelector('.creatic-toggle');
            toggle.onclick = () => this.toggle();
        }

        toggle() {
            this.isCollapsed = !this.isCollapsed;
            this.container.classList.toggle('collapsed', this.isCollapsed);
            this.container.querySelector('.creatic-toggle').textContent = this.isCollapsed ? '◀' : '▶';
        }

        async refresh() {
            try {
                const response = await fetch('/creatic/analyze', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ pageId: this.pageId })
                });
                const data = await response.json();
                this.updateUI(data);
            } catch (e) {
                console.error('Creatic refresh failed', e);
            }
        }

        updateUI(data) {
            const loader = document.getElementById('creatic-loader');
            const content = document.getElementById('creatic-content');
            if (!loader || !content) return;

            loader.style.display = 'none';
            content.style.display = 'block';

            content.querySelector('.creatic-summary').textContent = data.summary || 'Contextual Guidance';

            this.renderList(content.querySelector('.creatic-actions'), data.actions, (a) => {
                const btn = document.createElement('button');
                btn.className = 'creatic-btn';
                btn.textContent = a.label;
                btn.title = a.description;
                btn.onclick = () => alert('Action: ' + a.label + '\n' + a.description);
                return btn;
            });

            this.renderList(content.querySelector('.creatic-insights'), data.insights, (i) => {
                const div = document.createElement('div');
                div.className = 'creatic-item insight';
                div.textContent = i.text;
                return div;
            });

            this.renderList(content.querySelector('.creatic-warnings'), data.warnings, (w) => {
                const div = document.createElement('div');
                div.className = 'creatic-item warning';
                div.textContent = w.text;
                return div;
            });

            this.renderList(content.querySelector('.creatic-tips'), data.tips, (t) => {
                const div = document.createElement('div');
                div.className = 'creatic-item tip';
                div.textContent = t.text;
                return div;
            });
        }

        renderList(container, items, renderer) {
            if (!container) return;
            container.innerHTML = '';
            if (!items || items.length === 0) {
                container.innerHTML = '<span class="creatic-empty">None available</span>';
                return;
            }
            items.forEach(item => {
                container.appendChild(renderer(item));
            });
        }
    }

    // Initialize when DOM ready
    window.Creatic = new CreaticUI();
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => window.Creatic.init());
    } else {
        window.Creatic.init();
    }
})();
