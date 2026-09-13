(function() {
    // Global Error Bridge
    window.onerror = function(message, source, lineno, colno, error) {
        const errorMsg = "JS Error: " + message + " at " + source + ":" + lineno + ":" + colno;
        if (window.logFunction) window.logFunction(errorMsg);
        return false;
    };

    const svg = document.getElementById("architecture-svg");
    const container = document.getElementById("architecture-container");

    let allNodes = [];
    let allLinks = [];
    let visibleGraph = { nodes: [], links: [] };

    let currentLayout = 'GRID';
    let zoomScale = 1;
    let zoomX = 0;
    let zoomY = 0;

    // Hierarchy & Filters State
    let expandedNodeIds = new Set(['repo']); // Root is expanded by default
    let activeFilters = new Set(['CONTAINS', 'DEPENDS', 'IMPORTS', 'INHERIT', 'CALLS', 'REFS', 'EXT']);
    let focusedNodeId = null;

    // Dragging & Panning state
    let isPanning = false;
    let draggedNode = null;
    let startX, startY;

    container.addEventListener('mousedown', (e) => {
        if (e.button === 0) {
            const nodeElement = e.target.closest('.node');
            if (nodeElement) {
                const nodeId = nodeElement.getAttribute('data-id');
                draggedNode = visibleGraph.nodes.find(n => n.id === nodeId);
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

                const paths = document.querySelectorAll('.link');
                paths.forEach((path, i) => {
                    const l = visibleGraph.links[i];
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

        // Continuous Semantic Zooming Level Adjustment
        if (zoomScale < 0.5) {
            // Collapse lower level nodes
            expandedNodeIds = new Set(['repo']);
            recomputeGraphAndRender();
        } else if (zoomScale > 1.4 && expandedNodeIds.size <= 2) {
            // Auto expand modules
            allNodes.filter(n => n.level <= 2).forEach(n => expandedNodeIds.add(n.id));
            recomputeGraphAndRender();
        }

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
        currentLayout = type || 'GRID';
        applyLayout();
        render();
    };

    window.updateGraph = function(data) {
        if (!data || !data.components || data.components.length === 0) {
            document.getElementById("empty-state").classList.add("active");
            svg.innerHTML = '';
            return;
        }

        allNodes = data.components.map((c) => ({
            id: c.id,
            name: c.name || c.id,
            type: c.type || 'COMPONENT',
            description: c.description || '',
            importance: c.importanceScore || 0.5,
            path: c.path || '',
            parentId: c.parentId || '',
            level: c.level || 2,
            qualifiedName: c.qualifiedName || c.name || c.id,
            superClass: c.superClass || '',
            interfaces: c.interfaces || [],
            fields: c.fields || [],
            methods: c.methods || [],
            useCases: c.useCases || [],
            keyClasses: c.keyClasses || [],
            incomingCount: c.incomingCount || 0,
            outgoingCount: c.outgoingCount || 0,
            x: 0,
            y: 0
        }));

        const nodeMap = new Map(allNodes.map(n => [n.id, n]));

        allLinks = (data.relationships || []).map(r => ({
            sourceId: r.from,
            targetId: r.to,
            type: r.type || 'DEPENDS_ON',
            count: r.count || 1,
            details: r.details || []
        }));

        // Default expanded state: Expand repo & top modules
        expandedNodeIds = new Set(['repo']);
        allNodes.filter(n => n.level === 2).forEach(n => expandedNodeIds.add(n.id));

        document.getElementById("empty-state").classList.toggle("active", allNodes.length === 0);

        recomputeGraphAndRender();
    };

    function recomputeGraphAndRender() {
        const visibleNodesMap = new Map();

        // Determine node visibility based on hierarchy expansion
        allNodes.forEach(n => {
            if (isNodeVisible(n)) {
                visibleNodesMap.set(n.id, n);
            }
        });

        // Compute Aggregated Edges
        const linkMap = new Map();

        allLinks.forEach(l => {
            if (!isLinkTypeFiltered(l.type)) return;

            const visSource = findVisibleAncestor(l.sourceId, visibleNodesMap);
            const visTarget = findVisibleAncestor(l.targetId, visibleNodesMap);

            if (visSource && visTarget && visSource.id !== visTarget.id) {
                const key = visSource.id + "->" + visTarget.id + ":" + getFilterCategory(l.type);
                let aggLink = linkMap.get(key);
                if (!aggLink) {
                    aggLink = {
                        source: visSource,
                        target: visTarget,
                        type: l.type,
                        count: 0,
                        details: []
                    };
                    linkMap.set(key, aggLink);
                }
                aggLink.count += (l.count || 1);
                if (l.details) {
                    l.details.forEach(d => { if (!aggLink.details.includes(d)) aggLink.details.push(d); });
                }
            }
        });

        visibleGraph = {
            nodes: Array.from(visibleNodesMap.values()),
            links: Array.from(linkMap.values())
        };

        applyLayout();
        render();
    }

    function isNodeVisible(node) {
        if (!node.parentId || node.parentId === 'repo' || node.id === 'repo') return true;

        let current = node;
        const nodeMap = new Map(allNodes.map(n => [n.id, n]));

        while (current && current.parentId) {
            if (!expandedNodeIds.has(current.parentId)) {
                return false;
            }
            current = nodeMap.get(current.parentId);
        }
        return true;
    }

    function findVisibleAncestor(nodeId, visibleNodesMap) {
        if (visibleNodesMap.has(nodeId)) return visibleNodesMap.get(nodeId);

        const nodeMap = new Map(allNodes.map(n => [n.id, n]));
        let current = nodeMap.get(nodeId);

        while (current) {
            if (visibleNodesMap.has(current.id)) {
                return visibleNodesMap.get(current.id);
            }
            current = nodeMap.get(current.parentId);
        }
        return null;
    }

    function getFilterCategory(type) {
        if (type === 'CONTAINS') return 'CONTAINS';
        if (['DEPENDENCY', 'MAVEN_DEPENDENCY', 'OSGI_REQUIRE_BUNDLE'].includes(type)) return 'DEPENDS';
        if (['IMPORT', 'OSGI_IMPORT_PACKAGE', 'OSGI_EXPORT_PACKAGE'].includes(type)) return 'IMPORTS';
        if (['EXTENDS', 'IMPLEMENTS'].includes(type)) return 'INHERIT';
        if (type === 'METHOD_CALL') return 'CALLS';
        if (['TYPE_REFERENCE', 'FIELD_REFERENCE'].includes(type)) return 'REFS';
        if (['EXTENSION', 'EXTENSION_POINT'].includes(type)) return 'EXT';
        return 'DEPENDS';
    }

    function isLinkTypeFiltered(type) {
        const cat = getFilterCategory(type);
        return activeFilters.has(cat);
    }

    window.toggleFilter = function(category) {
        if (activeFilters.has(category)) {
            activeFilters.delete(category);
        } else {
            activeFilters.add(category);
        }
        recomputeGraphAndRender();
    };

    function applyLayout() {
        const nodes = visibleGraph.nodes;
        if (!nodes || nodes.length === 0) return;

        const count = nodes.length;
        const cardW = 240;
        const cardH = 110;

        if (currentLayout === 'HIERARCHICAL') {
            const layers = { 1: [], 2: [], 3: [], 4: [], 5: [] };
            nodes.forEach(n => {
                const lvl = n.level || 2;
                if (layers[lvl]) layers[lvl].push(n);
                else layers[2].push(n);
            });

            let currentY = 100;
            [1, 2, 3, 4, 5].forEach(lvl => {
                const group = layers[lvl];
                if (group.length > 0) {
                    let startX = 200;
                    group.forEach((node, idx) => {
                        node.x = startX + idx * (cardW + 50);
                        node.y = currentY;
                    });
                    currentY += cardH + 120;
                }
            });
        } else if (currentLayout === 'COMPACT') {
            const centerX = 600;
            const centerY = 450;
            const radius = Math.max(240, count * 35);
            nodes.forEach((n, i) => {
                const angle = (i / count) * 2 * Math.PI;
                n.x = centerX + radius * Math.cos(angle);
                n.y = centerY + radius * Math.sin(angle);
            });
        } else {
            // Default Grid layout
            const cols = Math.min(5, Math.max(2, Math.ceil(Math.sqrt(count))));
            nodes.forEach((n, i) => {
                const col = i % cols;
                const row = Math.floor(i / cols);
                n.x = 180 + col * (cardW + 60);
                n.y = 120 + row * (cardH + 60);
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

        // Find connected links for focus highlighting
        const connectedLinkKeys = new Set();
        if (focusedNodeId) {
            visibleGraph.links.forEach(l => {
                if (l.source.id === focusedNodeId || l.target.id === focusedNodeId) {
                    connectedLinkKeys.add(l.source.id + "->" + l.target.id);
                }
            });
        }

        // Render Links
        visibleGraph.links.forEach(l => {
            const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
            let linkClass = "link";

            if (focusedNodeId) {
                if (l.source.id === focusedNodeId) {
                    linkClass += " outgoing-link";
                } else if (l.target.id === focusedNodeId) {
                    linkClass += " incoming-link";
                } else {
                    linkClass += " dimmed";
                }
            }

            path.setAttribute("class", linkClass);
            path.setAttribute("d", calculateLinkPath(l.source, l.target));
            path.setAttribute("stroke", getLinkColor(l.type));
            path.setAttribute("stroke-width", Math.min(6, 1.5 + Math.log2(l.count || 1)));
            path.setAttribute("marker-end", "url(#arrowhead)");

            path.onclick = (e) => {
                e.stopPropagation();
                showEdgeDetails(e, l);
            };

            gRoot.appendChild(path);

            // Edge Count Badge if count > 1
            if (l.count > 1) {
                const midX = (l.source.x + l.target.x) / 2;
                const midY = (l.source.y + l.target.y) / 2;

                const text = document.createElementNS("http://www.w3.org/2000/svg", "text");
                text.setAttribute("x", midX);
                text.setAttribute("y", midY);
                text.setAttribute("class", "edge-badge");
                text.setAttribute("text-anchor", "middle");
                text.setAttribute("style", "font-size:10px; fill:#38bdf8; font-weight:bold; cursor:pointer;");
                text.textContent = l.count + " refs";
                text.onclick = (e) => {
                    e.stopPropagation();
                    showEdgeDetails(e, l);
                };
                gRoot.appendChild(text);
            }
        });

        // Render Nodes
        visibleGraph.nodes.forEach(n => {
            const nodeG = document.createElementNS("http://www.w3.org/2000/svg", "g");
            let nodeClass = "node";

            if (focusedNodeId) {
                if (n.id === focusedNodeId) {
                    nodeClass += " focused";
                } else if (!isNodeConnectedToFocus(n.id)) {
                    nodeClass += " dimmed";
                }
            }

            nodeG.setAttribute("class", nodeClass);
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
            badgeText.textContent = getRoleIcon(n.type) + " " + (n.type || 'COMPONENT') + " [L" + (n.level || 2) + "]";
            nodeG.appendChild(badgeText);

            // Expand / Collapse Badge
            const childrenCount = getChildrenCount(n.id);
            if (childrenCount > 0) {
                const isExpanded = expandedNodeIds.has(n.id);
                const expGroup = document.createElementNS("http://www.w3.org/2000/svg", "g");
                expGroup.setAttribute("class", "expand-badge");
                expGroup.setAttribute("transform", `translate(${w/2 - 24}, ${-h/2 + 14})`);
                expGroup.onclick = (e) => {
                    e.stopPropagation();
                    toggleNodeExpand(n.id);
                };

                const expRect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
                expRect.setAttribute("width", "20");
                expRect.setAttribute("height", "16");
                expRect.setAttribute("rx", "4");
                expRect.setAttribute("fill", isExpanded ? "#0284c7" : "#334155");
                expRect.setAttribute("stroke", roleColor);
                expGroup.appendChild(expRect);

                const expText = document.createElementNS("http://www.w3.org/2000/svg", "text");
                expText.setAttribute("x", "10");
                expText.setAttribute("y", "12");
                expText.setAttribute("text-anchor", "middle");
                expText.setAttribute("style", "font-size:10px; font-weight:bold; fill:#ffffff;");
                expText.textContent = isExpanded ? "−" : "+";
                expGroup.appendChild(expText);

                nodeG.appendChild(expGroup);
            }

            // Component Name Text
            const nameText = document.createElementNS("http://www.w3.org/2000/svg", "text");
            nameText.setAttribute("x", -w/2 + 10);
            nameText.setAttribute("y", -h/2 + 48);
            nameText.setAttribute("style", "font-weight: 700; font-size: 13px; fill: #f8fafc;");
            nameText.textContent = n.name.length > 24 ? n.name.substring(0, 21) + '...' : n.name;
            nodeG.appendChild(nameText);

            // Qualified Name / Description Subtitle
            const descText = document.createElementNS("http://www.w3.org/2000/svg", "text");
            descText.setAttribute("x", -w/2 + 10);
            descText.setAttribute("y", -h/2 + 68);
            descText.setAttribute("style", "font-size: 10px; fill: #94a3b8;");
            let desc = n.qualifiedName || n.path || n.description || "";
            descText.textContent = desc.length > 34 ? desc.substring(0, 31) + '...' : desc;
            nodeG.appendChild(descText);

            // References Count Indicators
            const refText = document.createElementNS("http://www.w3.org/2000/svg", "text");
            refText.setAttribute("x", -w/2 + 10);
            refText.setAttribute("y", -h/2 + 86);
            refText.setAttribute("style", "font-size: 9px; fill: #64748b; font-weight:bold;");
            refText.textContent = `↓ in:${n.incomingCount || 0}  ↑ out:${n.outgoingCount || 0}`;
            nodeG.appendChild(refText);

            // Progress Fill Bar
            const barFill = document.createElementNS("http://www.w3.org/2000/svg", "rect");
            const impScore = Math.max(0.1, Math.min(n.importance, 1.0));
            barFill.setAttribute("width", (w - 20) * impScore);
            barFill.setAttribute("height", "2");
            barFill.setAttribute("x", -w/2 + 10);
            barFill.setAttribute("y", -h/2 + 96);
            barFill.setAttribute("rx", "1");
            barFill.setAttribute("fill", roleColor);
            nodeG.appendChild(barFill);

            gRoot.appendChild(nodeG);
        });

        updateTransform();
    }

    function isNodeConnectedToFocus(nodeId) {
        if (!focusedNodeId) return true;
        if (nodeId === focusedNodeId) return true;
        return visibleGraph.links.some(l =>
            (l.source.id === focusedNodeId && l.target.id === nodeId) ||
            (l.target.id === focusedNodeId && l.source.id === nodeId)
        );
    }

    function getChildrenCount(parentId) {
        return allNodes.filter(n => n.parentId === parentId).length;
    }

    function toggleNodeExpand(nodeId) {
        if (expandedNodeIds.has(nodeId)) {
            expandedNodeIds.delete(nodeId);
        } else {
            expandedNodeIds.add(nodeId);
        }
        recomputeGraphAndRender();
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
            'REPOSITORY': '🧬',
            'BUNDLE': '📦',
            'MAVEN_MODULE': '🧱',
            'MODULE': '📁',
            'PACKAGE': '📂',
            'CLASS': '☕',
            'INTERFACE': '🔌',
            'ENUM': '🏷️',
            'RECORD': '📋',
            'METHOD': '⚙️',
            'FIELD': '📌',
            'USE_CASE': '🎯',
            'SUBSYSTEM': '⚙️',
            'DOMAIN': '●',
            'HOTSPOT': '🔥',
            'COMPONENT': '■'
        };
        return icons[type] || '■';
    }

    function getRoleColor(type) {
        const colors = {
            'REPOSITORY': '#8b5cf6',
            'BUNDLE': '#38bdf8',
            'MAVEN_MODULE': '#6366f1',
            'MODULE': '#64748b',
            'PACKAGE': '#f59e0b',
            'CLASS': '#10b981',
            'INTERFACE': '#06b6d4',
            'ENUM': '#ec4899',
            'RECORD': '#a855f7',
            'METHOD': '#eab308',
            'FIELD': '#64748b',
            'USE_CASE': '#ef4444',
            'SUBSYSTEM': '#0284c7',
            'HOTSPOT': '#f43f5e',
            'COMPONENT': '#38bdf8'
        };
        return colors[type] || '#94a3b8';
    }

    function getLinkColor(type) {
        const colors = {
            'CONTAINS': '#38bdf8',
            'DEPENDENCY': '#64748b',
            'OSGI_REQUIRE_BUNDLE': '#0284c7',
            'MAVEN_DEPENDENCY': '#6366f1',
            'IMPORT': '#f59e0b',
            'OSGI_IMPORT_PACKAGE': '#d97706',
            'EXTENDS': '#10b981',
            'IMPLEMENTS': '#06b6d4',
            'METHOD_CALL': '#eab308',
            'FIELD_REFERENCE': '#a855f7',
            'TYPE_REFERENCE': '#ec4899',
            'EXTENSION': '#22c55e',
            'EXTENSION_POINT': '#8b5cf6'
        };
        return colors[type] || '#475569';
    }

    function showDetails(node) {
        const panel = document.getElementById("details-panel");
        panel.classList.add("active");
        const roleColor = getRoleColor(node.type);

        const incomingRefs = allLinks.filter(l => l.targetId === node.id || (l.target && l.target.id === node.id));
        const outgoingRefs = allLinks.filter(l => l.sourceId === node.id || (l.source && l.source.id === node.id));

        panel.innerHTML = `
            <div class="panel-header">
                <div>
                    <h2 style="margin:0; font-size: 1.1em; color:${roleColor};">${node.name}</h2>
                    <span class="type-badge" style="border: 1px solid ${roleColor}; color: ${roleColor};">${getRoleIcon(node.type)} ${node.type} [L${node.level || 2}]</span>
                </div>
                <button onclick="document.getElementById('details-panel').classList.remove('active')" class="btn btn-sm" style="background:none; border:none; color:var(--text); font-size:18px;">&times;</button>
            </div>
            <div class="panel-body">
                <div style="margin-bottom: 14px;">
                    <label style="font-size:10px; color:var(--text-dim); text-transform:uppercase; font-weight:bold;">Qualified Name</label>
                    <code style="display:block; background:#0f172a; border: 1px solid var(--border); padding:6px 8px; border-radius:6px; margin-top:4px; font-size:11px; word-break:break-all; color:#38bdf8;">${node.qualifiedName || node.id}</code>
                </div>

                <div style="margin-bottom: 14px;">
                    <label style="font-size:10px; color:var(--text-dim); text-transform:uppercase; font-weight:bold;">Source Path</label>
                    <code style="display:block; background:#0f172a; border: 1px solid var(--border); padding:6px 8px; border-radius:6px; margin-top:4px; font-size:11px; word-break:break-all; color:#94a3b8;">${node.path || 'N/A'}</code>
                </div>

                ${node.superClass ? `
                    <div style="margin-bottom: 12px;">
                        <label style="font-size:10px; color:var(--text-dim); text-transform:uppercase; font-weight:bold;">Superclass</label>
                        <div style="font-size:11px; color:#10b981; font-weight:bold;">extends <code>${node.superClass}</code></div>
                    </div>
                ` : ''}

                ${node.interfaces && node.interfaces.length > 0 ? `
                    <div style="margin-bottom: 12px;">
                        <label style="font-size:10px; color:var(--text-dim); text-transform:uppercase; font-weight:bold;">Interfaces</label>
                        <div style="font-size:11px; color:#06b6d4;">implements ${node.interfaces.map(i => `<code>${i}</code>`).join(', ')}</div>
                    </div>
                ` : ''}

                ${node.methods && node.methods.length > 0 ? `
                    <div style="margin-bottom: 14px;">
                        <label style="font-size:10px; color:var(--text-dim); text-transform:uppercase; font-weight:bold;">Methods (${node.methods.length})</label>
                        <ul style="margin:4px 0; padding-left:16px; font-size:11px; color:var(--text); max-height:120px; overflow-y:auto;">
                            ${node.methods.map(m => `<li><code>${m}</code></li>`).join('')}
                        </ul>
                    </div>
                ` : ''}

                ${node.fields && node.fields.length > 0 ? `
                    <div style="margin-bottom: 14px;">
                        <label style="font-size:10px; color:var(--text-dim); text-transform:uppercase; font-weight:bold;">Fields (${node.fields.length})</label>
                        <ul style="margin:4px 0; padding-left:16px; font-size:11px; color:var(--text); max-height:100px; overflow-y:auto;">
                            ${node.fields.map(f => `<li><code>${f}</code></li>`).join('')}
                        </ul>
                    </div>
                ` : ''}

                <div style="margin-bottom: 14px;">
                    <label style="font-size:10px; color:var(--text-dim); text-transform:uppercase; font-weight:bold;">Incoming References (${incomingRefs.length})</label>
                    <ul style="margin:4px 0; padding-left:16px; font-size:11px; color:#38bdf8; max-height:90px; overflow-y:auto;">
                        ${incomingRefs.map(r => `<li style="cursor:pointer;" onclick="focusNode('${r.sourceId}')">← ${r.sourceId.replace('class:', '').replace('module:', '')} <span style="color:#64748b;">(${r.type})</span></li>`).join('')}
                    </ul>
                </div>

                <div style="margin-bottom: 14px;">
                    <label style="font-size:10px; color:var(--text-dim); text-transform:uppercase; font-weight:bold;">Outgoing References (${outgoingRefs.length})</label>
                    <ul style="margin:4px 0; padding-left:16px; font-size:11px; color:#10b981; max-height:90px; overflow-y:auto;">
                        ${outgoingRefs.map(r => `<li style="cursor:pointer;" onclick="focusNode('${r.targetId}')">→ ${r.targetId.replace('class:', '').replace('module:', '')} <span style="color:#64748b;">(${r.type})</span></li>`).join('')}
                    </ul>
                </div>

                <div style="display:flex; flex-direction:column; gap:8px; margin-top:16px;">
                    <div style="display:flex; gap:8px;">
                        <button onclick="javaAction('${node.path}', 'OPEN')" class="btn btn-primary" style="flex:1;">📂 Open Source File</button>
                        <button onclick="focusNode('${node.id}')" class="btn" style="flex:1;">🎯 Show References</button>
                    </div>
                    <button onclick="toggleNodeExpand('${node.id}')" class="btn" style="width:100%;">
                        ${expandedNodeIds.has(node.id) ? '➖ Collapse Children' : '➕ Expand Children'}
                    </button>
                </div>
            </div>
        `;
    }

    function showEdgeDetails(event, link) {
        const popup = document.getElementById("popup-panel");
        if (!popup) return;
        popup.style.display = "flex";
        document.getElementById("popup-title").textContent = `Relationship: ${link.source.name} ➔ ${link.target.name}`;

        const content = document.getElementById("popup-content");
        content.innerHTML = `
            <div style="margin-bottom:12px;">
                <b>Type:</b> <span style="color:${getLinkColor(link.type)}; font-weight:bold;">${link.type}</span> | <b>Count:</b> ${link.count}
            </div>
            <div><b>Underlying References:</b></div>
            <ul style="margin-top:8px; padding-left:20px; max-height:220px; overflow-y:auto; font-size:12px;">
                ${link.details.map(d => `<li><code>${d}</code></li>`).join('')}
            </ul>
        `;
    }

    function showContextMenu(event, node) {
        const menu = document.getElementById("context-menu");
        if (!menu) return;

        menu.style.left = event.pageX + "px";
        menu.style.top = event.pageY + "px";
        menu.classList.add("active");

        menu.innerHTML = `
            <div class="menu-item" onclick="focusNode('${node.id}')">🎯 <b>Focus Node & References</b></div>
            <div class="menu-item" onclick="toggleNodeExpand('${node.id}')">${expandedNodeIds.has(node.id) ? '➖ Collapse Children' : '➕ Expand Children'}</div>
            <hr>
            <div class="menu-item" onclick="javaAction('${node.path}', 'OPEN')">📂 Open Source File</div>
            <div class="menu-item" onclick="navigator.clipboard.writeText('${node.qualifiedName || node.id}')">📋 Copy Qualified Name</div>
        `;

        const closeMenu = () => {
            menu.classList.remove("active");
            document.removeEventListener("click", closeMenu);
        };
        setTimeout(() => document.addEventListener("click", closeMenu), 10);
    }

    window.focusNode = function(id) {
        focusedNodeId = id;
        const node = allNodes.find(n => n.id === id);
        if (node) {
            // Auto expand parents so the focused node is visible
            let curr = node;
            const nodeMap = new Map(allNodes.map(n => [n.id, n]));
            while (curr && curr.parentId) {
                expandedNodeIds.add(curr.parentId);
                curr = nodeMap.get(curr.parentId);
            }

            recomputeGraphAndRender();

            const visNode = visibleGraph.nodes.find(n => n.id === id);
            if (visNode) {
                const rect = container.getBoundingClientRect();
                zoomX = rect.width / 2 - visNode.x;
                zoomY = rect.height / 2 - visNode.y;
                zoomScale = 1.2;
                updateTransform();
                showDetails(visNode);
            }

            const clearBtn = document.getElementById("clear-focus-btn");
            if (clearBtn) clearBtn.style.display = "inline-flex";
        }
    };

    window.clearFocus = function() {
        focusedNodeId = null;
        const clearBtn = document.getElementById("clear-focus-btn");
        if (clearBtn) clearBtn.style.display = "none";
        recomputeGraphAndRender();
    };

    window.onGraphSearch = function(query) {
        const dropdown = document.getElementById("search-dropdown");
        if (!query || query.trim().length === 0) {
            dropdown.classList.remove("active");
            dropdown.innerHTML = "";
            return;
        }

        const q = query.toLowerCase().trim();
        const matches = allNodes.filter(n =>
            n.name.toLowerCase().includes(q) ||
            n.qualifiedName.toLowerCase().includes(q) ||
            n.type.toLowerCase().includes(q)
        ).slice(0, 15);

        if (matches.length === 0) {
            dropdown.classList.add("active");
            dropdown.innerHTML = `<div class="search-item" style="color:var(--text-dim);">No matching components found.</div>`;
            return;
        }

        dropdown.classList.add("active");
        dropdown.innerHTML = matches.map(m => `
            <div class="search-item" onclick="selectSearchResult('${m.id}')">
                <div class="search-item-title">
                    <span>${getRoleIcon(m.type)}</span>
                    <span>${m.name}</span>
                    <span class="type-badge" style="font-size:9px;">${m.type}</span>
                </div>
                <div class="search-item-sub">${m.qualifiedName || m.path}</div>
            </div>
        `).join('');
    };

    window.selectSearchResult = function(id) {
        const dropdown = document.getElementById("search-dropdown");
        if (dropdown) dropdown.classList.remove("active");
        focusNode(id);
    };

    window.javaAction = function(id, action) {
        if (window.navigatorFunction) {
            window.navigatorFunction(id, action);
        } else {
            console.log("Java action (Offline): " + id + " " + action);
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
            if (typeof item === 'string' && item.trim().startsWith('<')) {
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
