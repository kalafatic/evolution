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

    // Panning/Dragging state
    let isPanning = false;
    let draggedNode = null;
    let startX, startY;

    container.addEventListener('mousedown', (e) => {
        if (e.button === 0) { // Left click
            // Check if we clicked a node
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

            // Optimization: Only update the dragged node's position in the DOM
            const nodeEl = document.querySelector(`.node[data-id="${draggedNode.id}"]`);
            if (nodeEl) {
                nodeEl.setAttribute("transform", `translate(${draggedNode.x}, ${draggedNode.y})`);

                // Also update connected links
                const links = document.querySelectorAll('.link');
                links.forEach((line, i) => {
                    const l = graphData.links[i];
                    if (l.source.id === draggedNode.id) {
                        line.setAttribute("x1", draggedNode.x);
                        line.setAttribute("y1", draggedNode.y);
                    } else if (l.target.id === draggedNode.id) {
                        line.setAttribute("x2", draggedNode.x);
                        line.setAttribute("y2", draggedNode.y);
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
            y: 150 + Math.floor(i / 3) * 250
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

            const rect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
            const w = 200 + (n.importance * 50);
            const h = 100;
            rect.setAttribute("width", w);
            rect.setAttribute("height", h);
            rect.setAttribute("x", -w/2);
            rect.setAttribute("y", -h/2);
            rect.setAttribute("rx", 4);
            rect.setAttribute("stroke", getRoleColor(n.type));
            nodeG.appendChild(rect);

            const nameText = document.createElementNS("http://www.w3.org/2000/svg", "text");
            nameText.setAttribute("text-anchor", "middle");
            nameText.setAttribute("dy", "-0.5em");
            nameText.setAttribute("style", "font-weight: bold; font-size: 12px;");
            nameText.textContent = n.name.length > 25 ? n.name.substring(0, 22) + '...' : n.name;
            nodeG.appendChild(nameText);

            const descText = document.createElementNS("http://www.w3.org/2000/svg", "text");
            descText.setAttribute("text-anchor", "middle");
            descText.setAttribute("dy", "1.5em");
            descText.setAttribute("style", "font-size: 10px; fill: #666;");
            let desc = n.description || "";
            descText.textContent = desc.length > 40 ? desc.substring(0, 37) + '...' : desc;
            nodeG.appendChild(descText);

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
        content.innerHTML = "<p>None found.</p>";
    }
};
