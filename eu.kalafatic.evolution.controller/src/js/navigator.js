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
    let currentLayout = 'GRID';
    let zoomScale = 1;
    let zoomX = 0;
    let zoomY = 0;

    // Panning/Dragging state
    let isPanning = false;
    let draggedNode = null;
    let startX, startY;

    container.addEventListener('mousedown', (e) => {
        if (e.button === 0) { // Left click
            const nodeElement = e.target.closest('.node');
            if (nodeElement) {
                const nodeId = nodeElement.getAttribute('data-id');
                draggedNode = graphData.nodes.find(n => n.id === nodeId);
                if (draggedNode) {
                    startX = (e.clientX - zoomX) / zoomScale - draggedNode.x;
                    startY = (e.clientY - zoomY) / zoomScale - draggedNode.y;
                    container.style.cursor = 'grabbing';
                    return;
                }
            }

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
        } else if (draggedNode) {
            draggedNode.x = (e.clientX - zoomX) / zoomScale - startX;
            draggedNode.y = (e.clientY - zoomY) / zoomScale - startY;

            const nodeEl = document.querySelector(`.node[data-id="${escapeCssId(draggedNode.id)}"]`);
            if (nodeEl) {
                nodeEl.setAttribute("transform", `translate(${draggedNode.x}, ${draggedNode.y})`);

                // Update connected links
                const paths = document.querySelectorAll('.link');
                paths.forEach((path, i) => {
                    const l = graphData.links[i];
                    if (l && (l.source.id === draggedNode.id || l.target.id === draggedNode.id)) {
                        path.setAttribute("d", calculateLinkPath(l.source, l.target));
                    }
                });
            }
        }
    });

    window.addEventListener('mouseup', () => {
        isPanning = false;
        draggedNode = null;
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

    function escapeCssId(id) {
        return id ? id.replace(/(:|\.|\[|\]|,|=|@|\/)/g, "\\$1") : "";
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
        if (typeof log === 'function') log("Switching layout to: " + type);
        currentLayout = type || 'GRID';
        applyLayout();
        render();
    };

    window.updateGraph = function(data) {
        if (typeof log === 'function') log("updateGraph called with " + (data && data.components ? data.components.length : 0) + " components.");

        if (!data || !data.components || data.components.length === 0) {
            if (typeof log === 'function') log("updateGraph: No components to render.");
            document.getElementById("empty-state").classList.add("active");
            svg.innerHTML = '';
            return;
        }

        const rawNodes = data.components.map((c) => ({
            id: c.id,
            name: c.name,
            type: c.type || 'COMPONENT',
            description: c.description || '',
            importance: c.importanceScore || 0.5,
            path: c.path || '',
            useCases: c.useCases || [],
            keyClasses: c.keyClasses || [],
            x: 0,
            y: 0
        }));

        const nodeMap = new Map(rawNodes.map(n => [n.id, n]));

        const links = (data.relationships || [])
            .filter(r => nodeMap.has(r.from) && nodeMap.has(r.to))
            .map(r => ({
                source: nodeMap.get(r.from),
                target: nodeMap.get(r.to),
                type: r.type || 'DEPENDS_ON'
            }));

        graphData = { nodes: rawNodes, links: links };
        document.getElementById("empty-state").classList.toggle("active", rawNodes.length === 0);

        applyLayout();
        render();
    };

    function applyLayout() {
        const nodes = graphData.nodes;
        if (!nodes || nodes.length === 0) return;

        const count = nodes.length;
        const cardW = 240;
        const cardH = 110;

        if (currentLayout === 'HIERARCHICAL') {
            // Group nodes by type layer or link hierarchy
            const layers = {
                'DOMAIN': [],
                'SUBSYSTEM': [],
                'BUNDLE': [],
                'MAVEN_MODULE': [],
                'MODULE': [],
                'USE_CASE': [],
                'HOTSPOT': [],
                'OTHER': []
            };

            nodes.forEach(n => {
                const type = (n.type || '').toUpperCase();
                if (layers[type]) {
                    layers[type].push(n);
                } else {
                    layers['OTHER'].push(n);
                }
            });

            let currentY = 100;
            Object.keys(layers).forEach(layerKey => {
                const group = layers[layerKey];
                if (group.length > 0) {
                    const rowWidth = group.length * (cardW + 60);
                    let startX = 200;
                    group.forEach((node, idx) => {
                        node.x = startX + idx * (cardW + 60);
                        node.y = currentY;
                    });
                    currentY += cardH + 120;
                }
            });
        } else if (currentLayout === 'COMPACT') {
            const centerX = 500;
            const centerY = 400;
            const radius = Math.max(220, count * 35);
            nodes.forEach((n, i) => {
                const angle = (i / count) * 2 * Math.PI;
                n.x = centerX + radius * Math.cos(angle);
                n.y = centerY + radius * Math.sin(angle);
            });
        } else {
            // Default GRID layout
            const cols = Math.min(4, Math.max(2, Math.ceil(Math.sqrt(count))));
            nodes.forEach((n, i) => {
                const col = i % cols;
                const row = Math.floor(i / cols);
                n.x = 180 + col * (cardW + 70);
                n.y = 120 + row * (cardH + 70);
            });
        }
    }

    function render() {
        svg.innerHTML = `
            <defs>
                <marker id="arrowhead" viewBox="-0 -5 10 10" refX="22" refY="0" orient="auto" markerWidth="7" markerHeight="7" xoverflow="visible">
                    <path d="M 0,-5 L 10 ,0 L 0,5" fill="#38bdf8" style="stroke: none;"></path>
                </marker>
                <filter id="card-shadow" x="-20%" y="-20%" width="140%" height="140%">
                    <feDropShadow dx="0" dy="4" stdDeviation="6" flood-color="#000000" flood-opacity="0.4"/>
                </filter>
            </defs>
        `;

        const gRoot = document.createElementNS("http://www.w3.org/2000/svg", "g");
        gRoot.setAttribute("class", "graph-root");
        svg.appendChild(gRoot);

        // Render Links
        graphData.links.forEach(l => {
            const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
            path.setAttribute("class", "link");
            path.setAttribute("d", calculateLinkPath(l.source, l.target));
            path.setAttribute("stroke", getLinkColor(l.type));
            path.setAttribute("stroke-width", "2");
            path.setAttribute("marker-end", "url(#arrowhead)");
            if (l.type === 'DEPENDS_ON' || l.type === 'EVIDENCE') {
                path.setAttribute("stroke-dasharray", "5,4");
            }
            gRoot.appendChild(path);
        });

        // Render Nodes
        graphData.nodes.forEach(n => {
            const nodeG = document.createElementNS("http://www.w3.org/2000/svg", "g");
            nodeG.setAttribute("class", "node");
            nodeG.setAttribute("data-id", n.id);
            nodeG.setAttribute("transform", `translate(${n.x}, ${n.y})`);
            nodeG.onclick = (e) => {
                e.stopPropagation();
                showDetails(n);
            };
            nodeG.oncontextmenu = (e) => {
                e.preventDefault();
                showContextMenu(e, n);
            };

            const w = 240;
            const h = 110;
            const roleColor = getRoleColor(n.type);

            // Card Container
            const rect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
            rect.setAttribute("class", "node-card");
            rect.setAttribute("width", w);
            rect.setAttribute("height", h);
            rect.setAttribute("x", -w/2);
            rect.setAttribute("y", -h/2);
            rect.setAttribute("rx", "8");
            rect.setAttribute("fill", "#1e293b");
            rect.setAttribute("stroke", roleColor);
            rect.setAttribute("stroke-width", "1.5");
            rect.setAttribute("filter", "url(#card-shadow)");
            nodeG.appendChild(rect);

            // Header Banner
            const header = document.createElementNS("http://www.w3.org/2000/svg", "rect");
            header.setAttribute("width", w);
            header.setAttribute("height", "28");
            header.setAttribute("x", -w/2);
            header.setAttribute("y", -h/2);
            header.setAttribute("rx", "8");
            header.setAttribute("fill", roleColor);
            header.setAttribute("fill-opacity", "0.25");
            nodeG.appendChild(header);

            // Role Badge/Icon Text
            const badgeText = document.createElementNS("http://www.w3.org/2000/svg", "text");
            badgeText.setAttribute("x", -w/2 + 10);
            badgeText.setAttribute("y", -h/2 + 18);
            badgeText.setAttribute("style", `font-weight: 700; font-size: 10px; fill: ${roleColor}; letter-spacing: 0.5px;`);
            badgeText.textContent = getRoleIcon(n.type) + " " + (n.type || 'COMPONENT');
            nodeG.appendChild(badgeText);

            // Component Name Text
            const nameText = document.createElementNS("http://www.w3.org/2000/svg", "text");
            nameText.setAttribute("x", -w/2 + 10);
            nameText.setAttribute("y", -h/2 + 48);
            nameText.setAttribute("style", "font-weight: 700; font-size: 13px; fill: #f8fafc;");
            nameText.textContent = n.name.length > 24 ? n.name.substring(0, 21) + '...' : n.name;
            nodeG.appendChild(nameText);

            // Description / Subtitle
            const descText = document.createElementNS("http://www.w3.org/2000/svg", "text");
            descText.setAttribute("x", -w/2 + 10);
            descText.setAttribute("y", -h/2 + 68);
            descText.setAttribute("style", "font-size: 10px; fill: #94a3b8;");
            let desc = n.description || n.path || "";
            descText.textContent = desc.length > 34 ? desc.substring(0, 31) + '...' : desc;
            nodeG.appendChild(descText);

            // Progress/Significance Bar Background
            const barBg = document.createElementNS("http://www.w3.org/2000/svg", "rect");
            barBg.setAttribute("width", w - 20);
            barBg.setAttribute("height", "3");
            barBg.setAttribute("x", -w/2 + 10);
            barBg.setAttribute("y", -h/2 + 92);
            barBg.setAttribute("rx", "1.5");
            barBg.setAttribute("fill", "#334155");
            nodeG.appendChild(barBg);

            // Progress Fill
            const barFill = document.createElementNS("http://www.w3.org/2000/svg", "rect");
            const impScore = Math.max(0.1, Math.min(n.importance, 1.0));
            barFill.setAttribute("width", (w - 20) * impScore);
            barFill.setAttribute("height", "3");
            barFill.setAttribute("x", -w/2 + 10);
            barFill.setAttribute("y", -h/2 + 92);
            barFill.setAttribute("rx", "1.5");
            barFill.setAttribute("fill", roleColor);
            nodeG.appendChild(barFill);

            gRoot.appendChild(nodeG);
        });

        updateTransform();
    }

    function calculateLinkPath(source, target) {
        if (!source || !target) return "";
        const dx = target.x - source.x;
        const dy = target.y - source.y;
        const cx1 = source.x + dx * 0.5;
        const cy1 = source.y;
        const cx2 = source.x + dx * 0.5;
        const cy2 = target.y;
        return `M ${source.x} ${source.y} C ${cx1} ${cy1}, ${cx2} ${cy2}, ${target.x} ${target.y}`;
    }

    function getRoleIcon(type) {
        const icons = {
            'USE_CASE': '🎯',
            'SUBSYSTEM': '⚙️',
            'DOMAIN': '🌐',
            'BUNDLE': '📦',
            'MAVEN_MODULE': '🧱',
            'MODULE': '🧩',
            'ORCHESTRATION': '🧬',
            'MEDIATION': '🔀',
            'SUPERVISION': '🛡️',
            'HOTSPOT': '🔥',
            'OBJECTIVE': '📌',
            'DOCS': '📚',
            'COMPONENT': '⚙️'
        };
        return icons[type] || '📄';
    }

    function getRoleColor(type) {
        const colors = {
            'USE_CASE': '#ef4444',
            'SUBSYSTEM': '#0284c7',
            'DOMAIN': '#8b5cf6',
            'BUNDLE': '#38bdf8',
            'MAVEN_MODULE': '#6366f1',
            'MODULE': '#64748b',
            'ORCHESTRATION': '#10b981',
            'MEDIATION': '#f59e0b',
            'SUPERVISION': '#a855f7',
            'HOTSPOT': '#f43f5e',
            'OBJECTIVE': '#22c55e',
            'DOCS': '#06b6d4',
            'COMPONENT': '#38bdf8'
        };
        return colors[type] || '#94a3b8';
    }

    function getLinkColor(type) {
        const colors = {
            'CONTAINS': '#38bdf8',
            'DEPENDS_ON': '#64748b',
            'SUPPORTED_BY': '#10b981',
            'EVIDENCE': '#f59e0b',
            'PART_OF': '#8b5cf6',
            'HIGHLIGHTS': '#f43f5e'
        };
        return colors[type] || '#475569';
    }

    function showDetails(node) {
        const panel = document.getElementById("details-panel");
        panel.classList.add("active");
        const roleColor = getRoleColor(node.type);

        panel.innerHTML = `
            <div class="panel-header">
                <div>
                    <h2 style="margin:0; font-size: 1.1em; color:${roleColor};">${node.name}</h2>
                    <span class="type-badge" style="border: 1px solid ${roleColor}; color: ${roleColor};">${getRoleIcon(node.type)} ${node.type}</span>
                </div>
                <button onclick="document.getElementById('details-panel').classList.remove('active')" class="btn btn-sm" style="background:none; border:none; color:var(--text); font-size:18px;">&times;</button>
            </div>
            <div class="panel-body">
                <div style="margin-bottom: 16px;">
                    <label style="font-size:10px; color:var(--text-dim); text-transform:uppercase; font-weight:bold; letter-spacing:0.5px;">Description</label>
                    <p style="margin:6px 0; color:var(--text);">${node.description || 'No description available.'}</p>
                </div>

                <div style="margin-bottom: 16px;">
                    <label style="font-size:10px; color:var(--text-dim); text-transform:uppercase; font-weight:bold; letter-spacing:0.5px;">Physical Location / Path</label>
                    <code style="display:block; background:#0f172a; border: 1px solid var(--border); padding:8px; border-radius:6px; margin-top:6px; font-size:11px; word-break:break-all; color:#38bdf8;">${node.path || 'N/A'}</code>
                </div>

                <div style="margin-bottom: 16px;">
                    <label style="font-size:10px; color:var(--text-dim); text-transform:uppercase; font-weight:bold; letter-spacing:0.5px;">Architectural Significance (${Math.round(node.importance * 100)}%)</label>
                    <div style="height:6px; background:#334155; border-radius:3px; margin-top:8px; overflow:hidden;">
                        <div style="width:${node.importance * 100}%; height:100%; background:${roleColor};"></div>
                    </div>
                </div>

                ${node.keyClasses && node.keyClasses.length > 0 ? `
                    <div style="margin-bottom: 16px;">
                        <label style="font-size:10px; color:var(--text-dim); text-transform:uppercase; font-weight:bold; letter-spacing:0.5px;">Key Classes</label>
                        <ul style="margin:8px 0; padding-left:18px; font-size:0.88em; color:var(--text);">${node.keyClasses.map(c => `<li><code>${c}</code></li>`).join('')}</ul>
                    </div>
                ` : ''}

                ${node.useCases && node.useCases.length > 0 ? `
                    <div style="margin-bottom: 16px;">
                        <label style="font-size:10px; color:var(--text-dim); text-transform:uppercase; font-weight:bold; letter-spacing:0.5px;">Use Cases</label>
                        <ul style="margin:8px 0; padding-left:18px; font-size:0.88em; color:var(--text);">${node.useCases.map(u => `<li>${u}</li>`).join('')}</ul>
                    </div>
                ` : ''}

                <div style="display:flex; gap:8px; margin-top:24px;">
                    <button onclick="javaAction('${node.id}', 'OPEN')" class="btn btn-primary" style="flex:1;">📂 Open Source</button>
                    <button onclick="javaAction('${node.id}', 'SHOW_CHILDREN')" class="btn" style="flex:1;">🔍 Explore</button>
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
            <div class="menu-item" onclick="focusNode('${node.id}')">🎯 <b>Focus Node</b></div>
            <hr>
            <div class="menu-item" onclick="javaAction('${node.id}', 'SHOW_PARENTS')">⬆️ Show Parent Nodes</div>
            <div class="menu-item" onclick="javaAction('${node.id}', 'SHOW_CHILDREN')">⬇️ Show Child Nodes</div>
            <hr>
            <div class="menu-item" onclick="javaAction('${node.id}', 'SHOW_USE_CASES')">🎯 Show Use Cases</div>
            <div class="menu-item" onclick="javaAction('${node.id}', 'SHOW_CLASSES')">☕ Show Key Classes</div>
            <div class="menu-item" onclick="javaAction('${node.id}', 'OPEN')">📂 Open Source File</div>
        `;

        const closeMenu = () => {
            menu.classList.remove("active");
            document.removeEventListener("click", closeMenu);
        };
        setTimeout(() => document.addEventListener("click", closeMenu), 10);
    }

    window.focusNode = function(id) {
        if (typeof log === 'function') log("Focusing node: " + id);
        const node = graphData.nodes.find(n => n.id === id);
        if (node) {
            const rect = container.getBoundingClientRect();
            zoomX = rect.width / 2 - node.x;
            zoomY = rect.height / 2 - node.y;
            zoomScale = 1.2;
            updateTransform();
            showDetails(node);
        }
    };

    window.javaAction = function(id, action) {
        if (window.navigatorFunction) {
            window.navigatorFunction(id, action);
        } else {
            if (typeof log === 'function') log("Java action (Offline): " + id + " " + action);
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
        ul.style.paddingLeft = "20px";
        items.forEach(item => {
            const li = document.createElement("li");
            li.style.marginBottom = "8px";
            if (item.trim().startsWith('<')) {
                li.innerHTML = item;
                li.style.listStyle = "none";
            } else {
                li.textContent = item;
            }
            ul.appendChild(li);
        });
        content.appendChild(ul);
    } else {
        content.innerHTML = "<p style='color:var(--text-dim);'>No additional items found.</p>";
    }
};
