/**
 * Creatic Assistant UI Component
 */
(function() {
    const CREATIC_PANEL_ID = 'creatic-panel-root';

    class CreaticUI {
        constructor() {
            this.container = null;
            this.content = null;
            this.tooltip = null;
            this.contextMenu = null;
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
            this.renderTooltip();
            this.renderContextMenu();
            this.setupGuidanceListeners();
            this.setupContextMenuListeners();
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

        renderTooltip() {
            if (document.getElementById('creatic-tooltip')) return;
            const tooltip = document.createElement('div');
            tooltip.id = 'creatic-tooltip';
            tooltip.className = 'creatic-tooltip';
            tooltip.style.display = 'none';
            document.body.appendChild(tooltip);
            this.tooltip = tooltip;
        }

        setupGuidanceListeners() {
            document.addEventListener('mouseover', (e) => {
                const target = e.target.closest('[data-guidance]');
                if (target) {
                    this.showTooltip(target, target.getAttribute('data-guidance'));
                }
            });

            document.addEventListener('mouseout', (e) => {
                const target = e.target.closest('[data-guidance]');
                if (target) {
                    this.hideTooltip();
                }
            });
        }

        showTooltip(element, text) {
            if (!this.tooltip) return;

            const rect = element.getBoundingClientRect();
            this.tooltip.innerHTML = `
                <div class="tooltip-header">✨ Guidance</div>
                <div class="tooltip-content">${text.split('.').map(s => s.trim()).filter(s => s).map(s => `• ${s}`).join('<br>')}</div>
            `;

            this.tooltip.style.display = 'block';

            let top = rect.top + window.scrollY - this.tooltip.offsetHeight - 10;
            let left = rect.left + window.scrollX + (rect.width / 2) - (this.tooltip.offsetWidth / 2);

            // Bounds check
            if (top < 0) top = rect.bottom + window.scrollY + 10;
            if (left < 10) left = 10;
            if (left + this.tooltip.offsetWidth > window.innerWidth - 10) left = window.innerWidth - this.tooltip.offsetWidth - 10;

            this.tooltip.style.top = top + 'px';
            this.tooltip.style.left = left + 'px';
        }

        hideTooltip() {
            if (this.tooltip) this.tooltip.style.display = 'none';
        }

        renderContextMenu() {
            if (document.getElementById('creatic-context-menu')) return;
            const menu = document.createElement('div');
            menu.id = 'creatic-context-menu';
            menu.className = 'creatic-context-menu';
            menu.style.display = 'none';
            menu.innerHTML = `
                <div class="context-item" id="context-help">✨ Get Help</div>
                <div class="context-divider"></div>
                <div class="context-item" onclick="window.Creatic.toggle()">🛠 Toggle Assistant</div>
            `;
            document.body.appendChild(menu);
            this.contextMenu = menu;
        }

        setupContextMenuListeners() {
            document.addEventListener('contextmenu', (e) => {
                const target = e.target.closest('[data-guidance]');
                if (!target && !e.target.closest('.creatic-root')) return;

                e.preventDefault();
                this.showContextMenu(e.pageX, e.pageY, target);
            });

            document.addEventListener('click', () => this.hideContextMenu());
        }

        showContextMenu(x, y, target) {
            if (!this.contextMenu) return;

            const helpItem = document.getElementById('context-help');
            if (target) {
                helpItem.style.display = 'block';
                helpItem.onclick = (e) => {
                    e.stopPropagation();
                    this.showTooltip(target, target.getAttribute('data-guidance'));
                    this.hideContextMenu();
                };
            } else {
                helpItem.style.display = 'none';
            }

            this.contextMenu.style.display = 'block';
            this.contextMenu.style.left = x + 'px';
            this.contextMenu.style.top = y + 'px';
        }

        hideContextMenu() {
            if (this.contextMenu) this.contextMenu.style.display = 'none';
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
