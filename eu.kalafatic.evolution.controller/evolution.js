/**
 * Authentication integration for Evolution Web UI.
 * Handles environment detection and session verification.
 */

function isSWTBrowser() {
    return typeof JavaHandler !== 'undefined' || typeof JavaLog !== 'undefined';
}

async function checkAuthentication() {
    if (window.isEvoAuthChecking) return;
    if (isSWTBrowser() || window.location.protocol === 'file:' || window.location.search.includes('runtime=SWT')) {
        return;
    }

    const currentPath = window.location.pathname;
    // Skip login and dashboard (dashboard handled by auth.js)
    if (currentPath.endsWith('login.html') || currentPath.endsWith('dashboard.html')) return;

    window.isEvoAuthChecking = true;

    const sessionId = localStorage.getItem('sessionId') || sessionStorage.getItem('sessionId');
    const headers = {};
    if (sessionId && sessionId !== 'null' && sessionId !== 'undefined') {
        headers['Authorization'] = `Bearer ${sessionId}`;
    }

    try {
        const response = await fetch('/api/auth/me', {
            method: 'GET',
            headers: headers,
            credentials: 'include'
        });

        if (response.ok) {
            const data = await response.json();
            // Sync storage if needed
            if (!localStorage.getItem('sessionId') && !sessionStorage.getItem('sessionId')) {
                sessionStorage.setItem('sessionId', data.sessionId);
            }
        } else if (response.status === 401) {
            // If we sent a sessionId and it failed, clear it and try one more time (maybe cookie works)
            if (sessionId && sessionId !== 'null' && sessionId !== 'undefined') {
                console.log("Stored session ID invalid. Clearing and retrying with cookies...");
                localStorage.removeItem('sessionId');
                sessionStorage.removeItem('sessionId');
                // Second attempt will rely only on cookies
                const retryResponse = await fetch('/api/auth/me', { method: 'GET', credentials: 'include' });
                if (retryResponse.ok) {
                    const data = await retryResponse.json();
                    sessionStorage.setItem('sessionId', data.sessionId);
                    return;
                }
            }
            console.warn('Unauthorized. Redirecting to login.');
            window.location.href = '/login.html';
        }
    } catch (error) {
        console.error('Auth check failed:', error);
    } finally {
        window.isEvoAuthChecking = false;
    }
}

document.addEventListener('DOMContentLoaded', checkAuthentication);
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
            this.refreshInterval = 5000;
            this.refreshTimer = null;
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
            this.startAutoRefresh();
        }

        startAutoRefresh() {
            if (this.refreshTimer) clearInterval(this.refreshTimer);
            this.refreshTimer = setInterval(() => this.refresh(), this.refreshInterval);
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
                <div class="context-item" id="context-demo" style="display:none">🚀 Run E2E Demo</div>
                <div class="context-divider"></div>
                <div class="context-item" onclick="window.Creatic.toggle()">🛠 Toggle Assistant</div>
            `;
            document.body.appendChild(menu);
            this.contextMenu = menu;
        }

        setupContextMenuListeners() {
            document.addEventListener('contextmenu', (e) => {
                const target = e.target.closest('[data-guidance]') || e.target.closest('#model-type');
                if (!target && !e.target.closest('.creatic-root')) return;

                e.preventDefault();
                this.showContextMenu(e.pageX, e.pageY, target);
            });

            document.addEventListener('click', () => this.hideContextMenu());
        }

        showContextMenu(x, y, target) {
            if (!this.contextMenu) return;

            const helpItem = document.getElementById('context-help');
            const demoItem = document.getElementById('context-demo');

            if (target) {
                if (target.getAttribute('data-guidance')) {
                    helpItem.style.display = 'block';
                    helpItem.onclick = (e) => {
                        e.stopPropagation();
                        this.showTooltip(target, target.getAttribute('data-guidance'));
                        this.hideContextMenu();
                    };
                } else {
                    helpItem.style.display = 'none';
                }

                if (target.id === 'model-type') {
                    demoItem.style.display = 'block';
                    demoItem.onclick = (e) => {
                        e.stopPropagation();
                        if (window.startE2EDemo) window.startE2EDemo();
                        this.hideContextMenu();
                    };
                } else {
                    demoItem.style.display = 'none';
                }
            } else {
                helpItem.style.display = 'none';
                demoItem.style.display = 'none';
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
            if (loader) loader.style.display = 'none';
            if (content) content.style.display = 'block';

            if (content) {
                const summary = content.querySelector('.creatic-summary');
                if (summary) summary.textContent = data.summary || 'Contextual Guidance';

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

            // Integrated Page Guidance (if element exists)
            const integrated = document.getElementById('guidance-content');
            if (integrated) {
                this.renderIntegratedGuidance(integrated, data);
            }
        }

        renderIntegratedGuidance(container, data) {
            let html = `<div style="display:flex; flex-direction:column; gap:10px;">`;

            if (data.actions && data.actions.length > 0) {
                html += `<div><label style="font-size:0.7em; color:var(--text-dim); text-transform:uppercase;">Actions</label>`;
                data.actions.forEach(a => {
                    html += `<div style="margin-top:5px; padding:8px; background:rgba(0,122,204,0.1); border-left:3px solid var(--accent); font-size:0.9em;">
                                <b>${a.label}</b>: ${a.description}
                             </div>`;
                });
                html += `</div>`;
            }

            if (data.insights && data.insights.length > 0) {
                html += `<div><label style="font-size:0.7em; color:var(--text-dim); text-transform:uppercase;">Insights</label>`;
                data.insights.forEach(i => {
                    html += `<div style="margin-top:5px; font-size:0.85em; color:var(--text);">• ${i.text}</div>`;
                });
                html += `</div>`;
            }

            if (data.tips && data.tips.length > 0) {
                html += `<div><label style="font-size:0.7em; color:var(--text-dim); text-transform:uppercase;">Tips</label>`;
                data.tips.forEach(t => {
                    html += `<div style="margin-top:5px; font-size:0.85em; font-style:italic; color:var(--success);">💡 ${t.text}</div>`;
                });
                html += `</div>`;
            }

            html += `</div>`;
            container.innerHTML = html;
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
document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;
            const errorArea = document.getElementById('errorArea');
            const loader = document.getElementById('loader');
            const loginBtn = document.getElementById('loginBtn');

            errorArea.textContent = '';
            loader.style.display = 'block';
            loginBtn.disabled = true;

            try {
                const response = await fetch('/api/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, password }),
                    credentials: 'include'
                });

                const data = await response.json();
                if (data.success) {
                    const rememberMe = document.getElementById('rememberMe').checked;
                    if (rememberMe) {
                        localStorage.setItem('sessionId', data.sessionId);
                        sessionStorage.removeItem('sessionId');
                    } else {
                        sessionStorage.setItem('sessionId', data.sessionId);
                        localStorage.removeItem('sessionId');
                    }
                    window.location.href = '/dashboard.html';
                } else {
                    errorArea.textContent = data.message || 'Login failed';
                }
            } catch (error) {
                errorArea.textContent = 'Server error. Please try again.';
            } finally {
                loader.style.display = 'none';
                loginBtn.disabled = false;
            }
        });
    }

    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', async () => {
            const sessionId = localStorage.getItem('sessionId') || sessionStorage.getItem('sessionId');
            try {
                await fetch('/api/auth/logout', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${sessionId}`
                    }
                });
            } catch (error) {
                console.error('Logout failed', error);
            } finally {
                localStorage.removeItem('sessionId');
                sessionStorage.removeItem('sessionId');
                window.location.href = '/login.html';
            }
        });
    }

    if (window.location.pathname.endsWith('dashboard.html')) {
        loadUserInfo();
    }
});

function isSWTEnvironment() {
    return typeof JavaHandler !== 'undefined' ||
           typeof JavaLog !== 'undefined' ||
           window.location.search.includes('runtime=SWT') ||
           window.location.protocol === 'file:';
}

let isUserInfoLoading = false;

async function loadUserInfo() {
    if (isUserInfoLoading) return;
    const sessionId = localStorage.getItem('sessionId') || sessionStorage.getItem('sessionId');
    const headers = {};
    if (sessionId && sessionId !== 'null' && sessionId !== 'undefined') {
        headers['Authorization'] = `Bearer ${sessionId}`;
    } else {
        console.log("No sessionId in storage for user info, relying on cookies.");
    }

    try {
        isUserInfoLoading = true;
        const response = await fetch('/api/auth/me', {
            method: 'GET',
            headers: headers,
            credentials: 'include'
        });

        if (response.ok) {
            const data = await response.json();

            // Sync storage if needed
            if (!localStorage.getItem('sessionId') && !sessionStorage.getItem('sessionId')) {
                console.log("Syncing sessionId to sessionStorage from valid cookie.");
                sessionStorage.setItem('sessionId', data.sessionId);
            } else if (data.sessionId && data.sessionId !== localStorage.getItem('sessionId') && data.sessionId !== sessionStorage.getItem('sessionId')) {
                console.log("Session mismatch detected. Updating storage with server-provided ID.");
                if (localStorage.getItem('sessionId')) {
                    localStorage.setItem('sessionId', data.sessionId);
                } else {
                    sessionStorage.setItem('sessionId', data.sessionId);
                }
            }

            const setEl = (id, val) => {
                const el = document.getElementById(id);
                if (el) el.textContent = val;
            };

            setEl('displayUsername', data.username);
            setEl('displayRole', data.role);
            setEl('displaySessionId', data.sessionId);
            setEl('displayTimestamp', data.loginTimestamp);
            setEl('displayWorkflow', data.workflowType || 'GENERAL');

        } else if (response.status === 401) {
            console.warn('Unauthorized detected.');
            if (!isSWTEnvironment()) {
                console.warn('Redirecting to login.');
                localStorage.removeItem('sessionId');
                sessionStorage.removeItem('sessionId');
                window.location.href = '/login.html';
            } else {
                console.log('SWT Environment: Suppressing redirect.');
            }
        } else {
            console.error('Failed to load user info with status:', response.status);
        }
    } catch (error) {
        console.error('Network error while loading user info:', error);
    } finally {
        isUserInfoLoading = false;
    }
}
(function() {
    // Global Error Bridge
    window.onerror = function(message, source, lineno, colno, error) {
        const errorMsg = "JS Error: " + message + " at " + source + ":" + lineno + ":" + colno;
        if (window.logFunction) window.logFunction(errorMsg);
        return false;
    };

    const svg = document.getElementById("architecture-svg");
    const container = document.getElementById("architecture-container");

    let graphData = { nodes: [], links: [] };
    let zoomScale = 1;
    let zoomX = 0;
    let zoomY = 0;

    // Panning state
    let isPanning = false;
    let startX, startY;

    container.addEventListener('mousedown', (e) => {
        if (e.button === 0) { // Left click for pan
            isPanning = true;
            startX = e.clientX - zoomX;
            startY = e.clientY - zoomY;
            container.style.cursor = 'grabbing';
        }
    });

    window.addEventListener('mousemove', (e) => {
        if (isPanning) {
            zoomX = e.clientX - startX;
            zoomY = e.clientY - startY;
            updateTransform();
        }
    });

    window.addEventListener('mouseup', () => {
        isPanning = false;
        container.style.cursor = 'grab';
    });

    container.addEventListener('wheel', (e) => {
        e.preventDefault();
        const delta = e.deltaY > 0 ? 0.9 : 1.1;
        zoomScale *= delta;
        zoomScale = Math.max(0.1, Math.min(zoomScale, 5));
        updateTransform();
    }, { passive: false });

    function updateTransform() {
        const g = svg.querySelector(".graph-root");
        if (g) {
            g.setAttribute("transform", `translate(${zoomX}, ${zoomY}) scale(${zoomScale})`);
        }
    }

    window.zoomIn = function() {
        zoomScale *= 1.2;
        updateTransform();
    };

    window.zoomOut = function() {
        zoomScale /= 1.2;
        updateTransform();
    };

    window.resetZoom = function() {
        zoomScale = 1;
        zoomX = 0;
        zoomY = 0;
        updateTransform();
    };

    window.switchLayout = function(type) {
        log("Switching layout to: " + type + " (Layouts disabled in Forge engine)");
        // In the future, simple manual layouts can be added here.
    };

    window.updateGraph = function(data) {
        if (typeof log === 'function') log("updateGraph called with " + (data && data.components ? data.components.length : 0) + " components.");

        if (!data || !data.components || data.components.length === 0) {
            if (typeof log === 'function') log("updateGraph: No components to render.");
            document.getElementById("empty-state").classList.add("active");
            svg.innerHTML = '';
            return;
        }

        const nodes = data.components.map((c, i) => ({
            id: c.id,
            name: c.name,
            type: c.type,
            description: c.description,
            importance: c.importanceScore || 0.5,
            path: c.path,
            useCases: c.useCases || [],
            keyClasses: c.keyClasses || [],
            // Grid layout
            x: 150 + (i % 3) * 300,
            y: 150 + Math.floor(i / 3) * 200
        }));

        const nodeIds = new Set(nodes.map(n => n.id));

        const links = data.relationships
            .filter(r => nodeIds.has(r.from) && nodeIds.has(r.to))
            .map(r => ({
                source: nodes.find(n => n.id === r.from),
                target: nodes.find(n => n.id === r.to),
                type: r.type
            }));

        graphData = { nodes, links };
        document.getElementById("empty-state").classList.toggle("active", nodes.length === 0);
        render();
    };

    function render() {
        svg.innerHTML = `
            <defs>
                <marker id="arrowhead" viewBox="-0 -5 10 10" refX="20" refY="0" orient="auto" markerWidth="6" markerHeight="6" xoverflow="visible">
                    <path d="M 0,-5 L 10 ,0 L 0,5" fill="#555" style="stroke: none;"></path>
                </marker>
            </defs>
        `;

        const gRoot = document.createElementNS("http://www.w3.org/2000/svg", "g");
        gRoot.setAttribute("class", "graph-root");
        svg.appendChild(gRoot);

        graphData.links.forEach(l => {
            const line = document.createElementNS("http://www.w3.org/2000/svg", "line");
            line.setAttribute("class", "link");
            line.setAttribute("x1", l.source.x);
            line.setAttribute("y1", l.source.y);
            line.setAttribute("x2", l.target.x);
            line.setAttribute("y2", l.target.y);
            line.setAttribute("stroke", getLinkColor(l.type));
            if (l.type === 'DEPENDS_ON' || l.type === 'EVIDENCE') {
                line.setAttribute("stroke-dasharray", "5,5");
            }
            gRoot.appendChild(line);
        });

        graphData.nodes.forEach(n => {
            const nodeG = document.createElementNS("http://www.w3.org/2000/svg", "g");
            nodeG.setAttribute("class", "node");
            nodeG.setAttribute("transform", `translate(${n.x}, ${n.y})`);
            nodeG.onclick = (e) => {
                e.stopPropagation();
                showDetails(n);
            };
            nodeG.oncontextmenu = (e) => {
                e.preventDefault();
                showContextMenu(e, n);
            };

            const rect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
            const w = 200 + (n.importance * 50);
            const h = 60;
            rect.setAttribute("width", w);
            rect.setAttribute("height", h);
            rect.setAttribute("x", -w/2);
            rect.setAttribute("y", -h/2);
            rect.setAttribute("rx", 4);
            rect.setAttribute("stroke", getRoleColor(n.type));
            nodeG.appendChild(rect);

            const text = document.createElementNS("http://www.w3.org/2000/svg", "text");
            text.setAttribute("text-anchor", "middle");
            text.setAttribute("dy", "0.35em");
            text.textContent = n.name.length > 25 ? n.name.substring(0, 22) + '...' : n.name;
            nodeG.appendChild(text);

            gRoot.appendChild(nodeG);
        });

        updateTransform();
    }

    function getRoleColor(type) {
        const colors = {
            'USE_CASE': '#ef4444',
            'SUBSYSTEM': '#3b82f6',
            'DOMAIN': '#8b5cf6',
            'ORCHESTRATION': '#10b981',
            'MEDIATION': '#f59e0b',
            'SUPERVISION': '#6366f1',
            'HOTSPOT': '#f43f5e',
            'OBJECTIVE': '#22c55e',
            'RISK': '#f97316',
            'MODULE': '#64748b',
            'COMPONENT': '#3b82f6'
        };
        return colors[type] || '#94a3b8';
    }

    function getLinkColor(type) {
        const colors = {
            'CONTAINS': '#3b82f6',
            'DEPENDS_ON': '#64748b',
            'SUPPORTED_BY': '#10b981',
            'EVIDENCE': '#f59e0b',
            'PART_OF': '#8b5cf6'
        };
        return colors[type] || '#444';
    }

    function showDetails(node) {
        const panel = document.getElementById("details-panel");
        panel.classList.add("active");
        panel.innerHTML = `
            <div class="panel-header">
                <div>
                    <h2 style="margin:0; font-size: 1.1em; color:var(--accent);">${node.name}</h2>
                    <span class="type-badge">${node.type}</span>
                </div>
                <button onclick="document.getElementById('details-panel').classList.remove('active')" class="btn btn-sm" style="background:none;">&times;</button>
            </div>
            <div class="panel-body">
                <div style="margin-bottom: 15px;">
                    <label style="font-size:10px; color:var(--text-dim); text-transform:uppercase; font-weight:bold;">Description</label>
                    <p style="margin:5px 0;">${node.description || 'No description available.'}</p>
                </div>

                <div style="margin-bottom: 15px;">
                    <label style="font-size:10px; color:var(--text-dim); text-transform:uppercase; font-weight:bold;">Physical Path</label>
                    <code style="display:block; background:#000; padding:5px; border-radius:3px; margin-top:5px; font-size:10px; word-break:break-all; color:#89d185;">${node.path || 'N/A'}</code>
                </div>

                <div style="margin-bottom: 15px;">
                    <label style="font-size:10px; color:var(--text-dim); text-transform:uppercase; font-weight:bold;">Significance</label>
                    <div style="height:4px; background:#444; border-radius:2px; margin-top:8px; overflow:hidden;">
                        <div style="width:${node.importance * 100}%; height:100%; background:var(--accent);"></div>
                    </div>
                </div>

                ${node.keyClasses && node.keyClasses.length > 0 ? `
                    <div style="margin-bottom: 15px;">
                        <label style="font-size:10px; color:var(--text-dim); text-transform:uppercase; font-weight:bold;">Key Classes</label>
                        <ul style="margin:8px 0; padding-left:15px; font-size:0.9em;">${node.keyClasses.map(c => `<li>${c}</li>`).join('')}</ul>
                    </div>
                ` : ''}

                ${node.useCases && node.useCases.length > 0 ? `
                    <div style="margin-bottom: 15px;">
                        <label style="font-size:10px; color:var(--text-dim); text-transform:uppercase; font-weight:bold;">Use Cases</label>
                        <ul style="margin:8px 0; padding-left:15px; font-size:0.9em;">${node.useCases.map(u => `<li>${u}</li>`).join('')}</ul>
                    </div>
                ` : ''}

                <div style="display:flex; gap:8px; margin-top:20px;">
                    <button onclick="javaAction('${node.id}', 'OPEN')" class="btn btn-primary" style="flex:1;">Open File</button>
                    <button onclick="javaAction('${node.id}', 'CONTEXT')" class="btn" style="flex:1; text-align:center;">Context</button>
                </div>
            </div>
        `;
    }

    function showContextMenu(event, node) {
        const menu = document.getElementById("context-menu");
        if (!menu) return;

        menu.style.left = event.pageX + "px";
        menu.style.top = event.pageY + "px";
        menu.classList.add("active");

        menu.innerHTML = `
            <div class="menu-item" onclick="focusNode('${node.id}')"><b>🎯 Focus Node</b></div>
            <hr>
            <div class="menu-item" onclick="javaAction('${node.id}', 'SHOW_PARENTS')">Show Parent Nodes</div>
            <div class="menu-item" onclick="javaAction('${node.id}', 'SHOW_CHILDREN')">Show Child Nodes</div>
            <hr>
            <div class="menu-item" onclick="javaAction('${node.id}', 'SHOW_USE_CASES')">Show Use Cases</div>
            <div class="menu-item" onclick="javaAction('${node.id}', 'SHOW_CLASSES')">Show Key Classes</div>
            <div class="menu-item" onclick="javaAction('${node.id}', 'OPEN')">Open Source</div>
        `;

        const closeMenu = () => {
            menu.classList.remove("active");
            document.removeEventListener("click", closeMenu);
        };
        setTimeout(() => document.addEventListener("click", closeMenu), 10);
    }

    window.focusNode = function(id) {
        log("Focusing node: " + id);
        const node = graphData.nodes.find(n => n.id === id);
        if (node) {
            const rect = container.getBoundingClientRect();
            zoomX = rect.width / 2 - node.x;
            zoomY = rect.height / 2 - node.y;
            zoomScale = 1.5;
            updateTransform();
            showDetails(node);
        }
    };

    window.javaAction = function(id, action) {
        if (window.navigatorFunction) {
            window.navigatorFunction(id, action);
        } else {
            log("Java action (Offline): " + id + " " + action);
        }
    };

})();

window.showPopup = function(title, items) {
    const popup = document.getElementById("popup-panel");
    if (!popup) {
        alert(title + "\n" + (items ? items.join("\n") : "None"));
        return;
    }
    popup.style.display = "flex";
    document.getElementById("popup-title").textContent = title;
    const content = document.getElementById("popup-content");
    content.innerHTML = "";
    if (items && items.length > 0) {
        const ul = document.createElement("ul");
        items.forEach(item => {
            const li = document.createElement("li");
            li.textContent = item;
            ul.appendChild(li);
        });
        content.appendChild(ul);
    } else {
        content.innerHTML = "<p>None found.</p>";
    }
};
function renderArchitectureViz(data) {
    const area = document.getElementById('viz-area');
    area.innerHTML = '';

    const nodes = data.nodes && data.nodes.length > 0 ? data.nodes : [
        {id: 'in', name: 'Input', type: 'DATA'},
        {id: 'emb', name: 'Embedding', type: 'LAYER'},
        {id: 't1', name: 'Transformer Block #1', type: 'TRANSFORMER'},
        {id: 'out', name: 'Output Head', type: 'LAYER'}
    ];

    const container = document.createElement('div');
    container.style.padding = '20px';
    container.style.display = 'flex';
    container.style.flexDirection = 'column';
    container.style.alignItems = 'center';
    container.style.gap = '20px';

    nodes.forEach((n, i) => {
        const card = document.createElement('div');
        card.className = 'viz-card';
        card.style.width = '200px';
        card.style.cursor = 'pointer';
        card.onclick = () => {
            document.getElementById('viz-details-content').innerHTML = `
                <h4>${n.name}</h4>
                <p>Type: ${n.type}</p>
                <p>Params: 1.2M</p>
                <p>Input: [Batch, 512]</p>
                <p>Output: [Batch, 512]</p>
                <p>Status: OK</p>
            `;
        };

        const title = document.createElement('h4');
        title.textContent = n.name;
        card.appendChild(title);

        const type = document.createElement('div');
        type.style.fontSize = '0.7em';
        type.textContent = n.type;
        card.appendChild(type);

        container.appendChild(card);

        if (i < nodes.length - 1) {
            const arrow = document.createElement('div');
            arrow.innerHTML = `
                <svg width="20" height="30">
                    <line x1="10" y1="0" x2="10" y2="25" stroke="#555" stroke-width="2" />
                    <path d="M 5,20 L 10,25 L 15,20" fill="none" stroke="#555" stroke-width="2" />
                </svg>
            `;
            container.appendChild(arrow);
        }
    });

    area.appendChild(container);
}
function renderDatasetViz(stats, sample) {
    const area = document.getElementById('viz-area');
    area.innerHTML = `
        <div class="sample-nav">
            <span>Sample #${currentSampleIndex}</span>
        </div>
        <div>
            <label>Raw Text:</label>
            <div class="data-block">${sample.raw || 'N/A'}</div>
        </div>
        <div style="margin-top:10px;">
            <label>Tokenized Form:</label>
            <div class="data-block">${JSON.stringify(sample.tokens || [])}</div>
        </div>
        <div style="margin-top:10px;">
            <label>Training Pair:</label>
            <div class="data-block">[${(sample.tokens || []).slice(0,-1)}] -> [${(sample.tokens || []).slice(-1)}]</div>
        </div>
    `;

    document.getElementById('viz-details-content').innerHTML = `
        <h4>Dataset Stats</h4>
        <p>Size: ${stats.size || 0}</p>
        <p>Vocab: ${stats.vocab || 0}</p>
        <p>Tokens: ${(stats.size || 0) * 100}</p>
    `;
}
function renderEvolutionViz(snapshots) {
    const area = document.getElementById('viz-area');
    area.innerHTML = '';

    if (snapshots.length === 0) {
        area.innerHTML = '<div style="padding:20px; color:var(--text-dim)">No snapshots found for this session.</div>';
        return;
    }

    snapshots.forEach((s, i) => {
        const item = document.createElement('div');
        item.className = 'timeline-item';
        item.innerHTML = `
            <div style="font-weight:bold">${s.name || 'Snapshot ' + (i+1)}</div>
            <div style="font-size:0.8em; color:var(--text-dim)">${new Date(s.timestamp).toLocaleString()}</div>
        `;
        item.onclick = () => {
            document.getElementById('viz-details-content').innerHTML = `
                <h4>Snapshot Info</h4>
                <p>ID: ${s.id}</p>
                <p>Generation: ${s.generation || 0}</p>
                <p>Model Type: MLP</p>
                <p>Loss: 0.42</p>
                <div style="display:flex; flex-direction:column; gap:5px; margin-top:10px;">
                    <button class="btn btn-sm" style="width:100%" onclick="compareSnapshot('${s.id}')">Compare with Active</button>
                    <button class="btn btn-sm btn-primary" style="width:100%" onclick="exportSnapshot('${s.id}')">EXPORT FOR OLLAMA</button>
                </div>
            `;
        };
        area.appendChild(item);
    });
}
function renderTrainingViz(metrics, events) {
    const area = document.getElementById('viz-area');
    let eventsHtml = (events || []).map(e => `<div style="font-size:0.8em; margin-bottom:2px; border-bottom:1px solid #333; padding-bottom:2px;">${e}</div>`).join('');

    area.innerHTML = `
        <div style="display:grid; grid-template-columns: 1fr 1fr; gap: 10px; width:100%;">
            <div class="viz-card"><h4>Loss</h4><div style="font-size:1.5em;color:var(--accent)">${metrics.loss || '0.000'}</div></div>
            <div class="viz-card"><h4>Accuracy</h4><div style="font-size:1.5em;color:var(--success)">${metrics.acc || '0.000'}</div></div>
        </div>
        <div style="margin-top:10px; height: 150px; width:100%; border: 1px solid var(--border); position: relative; background:#111; overflow:hidden;">
            <svg width="100%" height="100%" id="training-anim-svg">
                 <!-- Multi-layer propagation simulation -->
                 <g id="layer-signals"></g>
            </svg>
        </div>
        <div style="margin-top:10px; height: 100px; width:100%; border: 1px solid var(--border); position: relative;">
            <svg width="100%" height="100%">
                 <path d="M 0 100 L 50 80 L 100 90 L 150 50 L 200 60 L 250 20 L 300 30" fill="none" stroke="var(--accent)" stroke-width="2" />
            </svg>
            <div style="position:absolute; bottom:5px; right:5px; font-size:0.7em; color:var(--text-dim)">Loss over time (Simulated)</div>
        </div>
        <div style="margin-top:15px; width:100%;">
            <label style="font-size:0.7em; color:var(--text-dim); text-transform:uppercase;">Event Stream</label>
            <div id="viz-event-stream" style="background:#ffffff; color:#333333; padding:10px; height:80px; overflow-y:auto; font-family:monospace; margin-top:5px; border:1px solid var(--border);">
                ${eventsHtml || '<div style="color:#888">No events recorded.</div>'}
            </div>
        </div>
    `;

    // Start propagation animation in Training Monitor
    setTimeout(() => {
        const g = document.getElementById('layer-signals');
        if (!g) return;
        const layers = [4, 6, 6, 4];
        const width = g.closest('svg').clientWidth;
        const height = g.closest('svg').clientHeight;

        layers.forEach((count, lIdx) => {
            const x = 50 + lIdx * ((width-100) / (layers.length - 1));
            for (let i = 0; i < count; i++) {
                const y = (height / (count + 1)) * (i + 1);
                const circle = document.createElementNS("http://www.w3.org/2000/svg", "circle");
                circle.setAttribute("cx", x);
                circle.setAttribute("cy", y);
                circle.setAttribute("r", 4);
                circle.setAttribute("fill", "#444");
                g.appendChild(circle);
            }
        });
    }, 100);

    document.getElementById('viz-details-content').innerHTML = `
        <h4>Training Status</h4>
        <p>Epoch: 5</p>
        <p>Batch: 128/1000</p>
        <p>LR: 0.0001</p>
        <p>Elapsed: 00:12:45</p>
    `;
}
// ============== WALKTHROUGH DEMO LOGIC ==============
let walkthroughNeurons = [];
let walkthroughConnections = [];
let walkthroughAnimationFrame = null;
let isStudyMode = false;
let walkthroughStep = 0;

class WalkthroughNeuron {
    constructor(x, y, layer) {
        this.x = x;
        this.y = y;
        this.layer = layer;
        this.activation = 0;
        this.targetActivation = 0;
    }
}

function renderWalkthroughViz() {
    const area = document.getElementById('viz-area');
    area.innerHTML = `
        <div style="display:flex; flex-direction:column; align-items:center; width:100%;">
            <canvas id="walkthrough-canvas" width="800" height="400" style="border:1px solid var(--border); background:#111; box-shadow: 0 0 10px rgba(0,255,0,0.2);"></canvas>
            <div id="walkthrough-info" class="data-block" style="width:90%; margin-top:10px; min-height:50px;">
                Click "Step" or "Play" to begin the neural network evolution walkthrough.
            </div>
            <div id="study-messages" style="display:${isStudyMode ? 'block' : 'none'}; width:90%; margin-top:10px;">
                <textarea id="study-msg-edit" class="control-group" style="width:100%; height:60px; font-size:0.8em;" onchange="saveStudyMessage(this.value)"></textarea>
            </div>
        </div>
    `;

    drawWalkthrough();
    updateStudyMessageUI();
}

function saveStudyMessage(val) {
    if (!activeSessionId) return;
    const messages = getStudyMessages();
    messages[walkthroughStep] = val;
    persistUiState('walkthrough_messages', JSON.stringify(messages));

    const info = document.getElementById('walkthrough-info');
    if (info) info.innerHTML = val;
}

function getStudyMessages() {
    const session = sessions.find(s => s.id === activeSessionId);
    const saved = session && session.uiState && session.uiState.walkthrough_messages;
    if (saved) {
        try { return JSON.parse(saved); } catch(e) { return {}; }
    }
    return {
        0: "Welcome to the Neural Network Walkthrough. Here we see how information flows and evolves.",
        1: "STEP 1: ARCHITECTURE. We define layers of neurons. Input layer receives data, Hidden layers process features, Output layer gives predictions.",
        2: "STEP 2: FORWARD PASS. Data flows from left to right. Each neuron calculates a weighted sum of its inputs and applies an activation function.",
        3: "STEP 3: TRAINING. We compare output to target and adjust weights. Notice how signals propagate across multiple hidden layers.",
        4: "STEP 4: GENERATION. Once trained, the network can predict the next token in a sequence, effectively 'thinking'."
    };
}

function updateStudyMessageUI() {
    const info = document.getElementById('walkthrough-info');
    const edit = document.getElementById('study-msg-edit');
    const messages = getStudyMessages();
    const msg = messages[walkthroughStep] || "Proceed to the next step...";

    if (info) info.innerHTML = msg;
    if (edit) edit.value = msg;
}

function drawWalkthrough() {
    const canvas = document.getElementById('walkthrough-canvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Draw connections
    walkthroughConnections.forEach(c => {
        const from = walkthroughNeurons[c.from];
        const to = walkthroughNeurons[c.to];
        const intensity = Math.abs(c.weight) * 0.8 + 0.2;
        ctx.strokeStyle = to.activation > 0.1 ? `rgba(0, 255, 100, ${intensity})` : '#334';
        ctx.lineWidth = Math.abs(c.weight) * 3 + 1;
        ctx.beginPath();
        ctx.moveTo(from.x, from.y);
        ctx.lineTo(to.x, to.y);
        ctx.stroke();
    });

    // Draw neurons
    walkthroughNeurons.forEach((n, i) => {
        const size = 15;
        ctx.fillStyle = n.activation > 0.3 ? '#0f0' : '#4488ff';
        ctx.beginPath();
        ctx.arc(n.x, n.y, size, 0, Math.PI * 2);
        ctx.fill();

        ctx.fillStyle = '#fff';
        ctx.font = '8px monospace';
        ctx.textAlign = 'center';
        ctx.fillText(n.activation.toFixed(1), n.x, n.y + 3);
    });
}

function walkthroughStep1Create() {
    if (walkthroughAnimationFrame) cancelAnimationFrame(walkthroughAnimationFrame);
    walkthroughNeurons = [];
    walkthroughConnections = [];
    walkthroughStep = 1;

    const layers = [3, 5, 4, 2]; // Multiple hidden layers
    const xStep = 200;
    const canvas = document.getElementById('walkthrough-canvas');
    const width = canvas ? canvas.width : 800;
    const height = canvas ? canvas.height : 400;

    layers.forEach((count, lIdx) => {
        const x = 100 + lIdx * xStep;
        for (let i = 0; i < count; i++) {
            const y = (height / (count + 1)) * (i + 1);
            walkthroughNeurons.push(new WalkthroughNeuron(x, y, lIdx));
        }
    });

    for (let i = 0; i < walkthroughNeurons.length; i++) {
        const n = walkthroughNeurons[i];
        if (n.layer === 0) continue;
        for (let j = 0; j < walkthroughNeurons.length; j++) {
            if (walkthroughNeurons[j].layer === n.layer - 1) {
                walkthroughConnections.push({
                    from: j, to: i, weight: Math.random() * 2 - 1
                });
            }
        }
    }
    drawWalkthrough();
    updateStudyMessageUI();
}

function walkthroughStep2Forward() {
    if (walkthroughNeurons.length === 0) walkthroughStep1Create();
    walkthroughStep = 2;

    // Input values
    walkthroughNeurons.filter(n => n.layer === 0).forEach(n => n.activation = Math.random());

    let currentLayer = 1;
    const animate = () => {
        const layerNeurons = walkthroughNeurons.filter(n => n.layer === currentLayer);
        layerNeurons.forEach(n => {
            let sum = 0;
            walkthroughConnections.filter(c => walkthroughNeurons[c.to] === n).forEach(c => {
                sum += walkthroughNeurons[c.from].activation * c.weight;
            });
            n.activation = 1 / (1 + Math.exp(-sum));
        });

        drawWalkthrough();
        currentLayer++;
        if (currentLayer < 4) {
            setTimeout(animate, 500);
        } else {
            updateStudyMessageUI();
        }
    };
    animate();
}

function walkthroughStep3Train() {
    if (walkthroughNeurons.length === 0) walkthroughStep1Create();
    walkthroughStep = 3;

    // Nudge weights
    walkthroughConnections.forEach(c => {
        c.weight += (Math.random() - 0.5) * 0.4;
    });

    walkthroughStep2Forward();
    updateStudyMessageUI();
}

function walkthroughStep4Generate() {
    walkthroughStep = 4;
    updateStudyMessageUI();
    const info = document.getElementById('walkthrough-info');
    const responses = [
        "Neural Evolution: Success. Model converged.",
        "Cognition level rising. Patterns identified.",
        "Intelligence synthesized. Ready for deployment."
    ];
    info.innerHTML += `<br><br><b style="color:var(--accent)">LLM OUTPUT:</b> ${responses[Math.floor(Math.random()*responses.length)]}`;
}

async function startWalkthroughAutoPlay() {
    log("Starting automated walkthrough sequence...");
    const delay = 3000;

    walkthroughStep1Create();
    await new Promise(r => setTimeout(r, delay));

    walkthroughStep2Forward();
    await new Promise(r => setTimeout(r, delay + 2000)); // Extra time for signal propagation

    walkthroughStep3Train();
    await new Promise(r => setTimeout(r, delay + 2000));

    walkthroughStep4Generate();
    log("Automated walkthrough complete.");
}
