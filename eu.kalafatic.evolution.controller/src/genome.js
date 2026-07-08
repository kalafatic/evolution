(function() {
    const svg = document.getElementById("genome-svg");
    const container = document.getElementById("genome-container");
    const detailsPanel = document.getElementById("details-panel");

    let zoomScale = 1;
    let zoomX = 0;
    let zoomY = 0;
    let isPanning = false;
    let startX, startY;

    // Interaction handling
    container.addEventListener('mousedown', (e) => {
        if (e.button === 0 && !e.target.closest('.node')) {
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
        const oldScale = zoomScale;
        zoomScale *= delta;
        zoomScale = Math.max(0.1, Math.min(zoomScale, 5));

        // Zoom towards mouse
        const rect = container.getBoundingClientRect();
        const mouseX = e.clientX - rect.left;
        const mouseY = e.clientY - rect.top;
        zoomX = mouseX - (mouseX - zoomX) * (zoomScale / oldScale);
        zoomY = mouseY - (mouseY - zoomY) * (zoomScale / oldScale);

        updateTransform();
    }, { passive: false });

    function updateTransform() {
        const g = svg.querySelector(".graph-root");
        if (g) {
            g.setAttribute("transform", `translate(${zoomX}, ${zoomY}) scale(${zoomScale})`);
        }
    }

    window.zoomIn = () => { zoomScale *= 1.2; updateTransform(); };
    window.zoomOut = () => { zoomScale /= 1.2; updateTransform(); };
    window.resetZoom = () => {
        const containerRect = container.getBoundingClientRect();
        zoomScale = 1;
        zoomX = containerRect.width / 2;
        zoomY = containerRect.height / 2;
        updateTransform();
    };

    let allNodes = [];
    let currentFilter = 'ALL';
    let currentSearch = '';

    window.searchNodes = (val) => {
        currentSearch = val.toLowerCase();
        applyFilter();
    };

    window.filterNodes = (val) => {
        currentFilter = val;
        applyFilter();
    };

    function applyFilter() {
        const nodes = document.querySelectorAll('.node');
        nodes.forEach(nodeEl => {
            const id = nodeEl.getAttribute('data-id');
            const node = allNodes.find(n => n.id === id);
            if (!node) return;

            const matchesFilter = currentFilter === 'ALL' || node.type === currentFilter;
            const matchesSearch = !currentSearch || node.name.toLowerCase().includes(currentSearch) || node.description.toLowerCase().includes(currentSearch);

            nodeEl.style.opacity = (matchesFilter && matchesSearch) ? '1' : '0.1';
            nodeEl.style.pointerEvents = (matchesFilter && matchesSearch) ? 'auto' : 'none';
        });

        const links = document.querySelectorAll('.link');
        links.forEach(linkEl => {
            const sourceId = linkEl.getAttribute('data-source');
            const targetId = linkEl.getAttribute('data-target');
            const s = allNodes.find(n => n.id === sourceId);
            const t = allNodes.find(n => n.id === targetId);

            if (s && t) {
                const sVisible = (currentFilter === 'ALL' || s.type === currentFilter) && (!currentSearch || s.name.toLowerCase().includes(currentSearch) || s.description.toLowerCase().includes(currentSearch));
                const tVisible = (currentFilter === 'ALL' || t.type === currentFilter) && (!currentSearch || t.name.toLowerCase().includes(currentSearch) || t.description.toLowerCase().includes(currentSearch));
                linkEl.style.opacity = (sVisible && tVisible) ? '1' : '0.1';
            }
        });
    }

    window.updateGenome = function(data) {
        if (!data || (!data.identity && !data.name)) {
            const emptyState = document.getElementById("empty-state");
            if (emptyState) emptyState.classList.add("active");
            svg.innerHTML = '';
            return;
        }
        const emptyState = document.getElementById("empty-state");
        if (emptyState) emptyState.classList.remove("active");

        // Handle both full genome and simple model
        if (!data.identity && data.name) {
            data = {
                identity: { name: data.name, version: '1.0' },
                concepts: data.components ? data.components.map(c => c.name) : [],
                moduleMap: {}
            };
        }

        renderGenome(data);
    };

    function renderGenome(genome) {
        svg.innerHTML = `
            <defs>
                <marker id="arrowhead" viewBox="-0 -5 10 10" refX="25" refY="0" orient="auto" markerWidth="6" markerHeight="6" xoverflow="visible">
                    <path d="M 0,-5 L 10 ,0 L 0,5" fill="#999" style="stroke: none;"></path>
                </marker>
            </defs>
            <g class="graph-root"></g>
        `;
        const gRoot = svg.querySelector(".graph-root");

        const nodes = [];
        const links = [];

        // 1. Central Genome Node
        const center = {
            id: 'genome',
            name: 'Genome',
            type: 'CORE',
            description: (genome.identity.name || 'EVO') + ' v' + (genome.identity.version || '1.0'),
            x: 0,
            y: 0,
            data: genome.identity
        };
        nodes.push(center);

        // 2. Module Group Nodes (arranged around center)
        const groups = [
            { id: 'architecture', name: 'Architecture', type: 'VIEW', desc: 'System structure and components', angle: -60 },
            { id: 'knowledge', name: 'Knowledge', type: 'STORE', desc: 'Domain knowledge and documentation', angle: 0 },
            { id: 'integrations', name: 'Integrations', type: 'BRIDGE', desc: 'External tools and VCS', angle: 60 },
            { id: 'evolution', name: 'Evolution', type: 'FLOW', desc: 'Development history and iterations', angle: 120 },
            { id: 'metadata', name: 'Metadata', type: 'DATA', desc: 'AI-assisted file understanding', angle: 180 },
            { id: 'relationships', name: 'Relationships', type: 'LINK', desc: 'Semantic connections between nodes', angle: 240 }
        ];

        const radius = 300;
        groups.forEach(g => {
            const rad = g.angle * (Math.PI / 180);
            const node = {
                id: g.id,
                name: g.name,
                type: g.type,
                description: g.desc,
                x: Math.cos(rad) * radius,
                y: Math.sin(rad) * radius,
                data: g
            };
            nodes.push(node);
            links.push({ source: center, target: node, label: 'contains' });

            // Sub-nodes from genome data if available
            if (g.id === 'architecture' && genome.moduleMap) {
                renderSubNodes(gRoot, node, Object.keys(genome.moduleMap), 150, nodes, links);
            } else if (g.id === 'metadata' && genome.concepts) {
                renderSubNodes(gRoot, node, genome.concepts.slice(0, 8), 150, nodes, links);
            } else if (g.id === 'evolution' && genome.identity.timestamp) {
                renderSubNodes(gRoot, node, ['Snapshot ' + genome.identity.timestamp], 150, nodes, links);
            }
        });

        allNodes = nodes;

        // Render Links
        links.forEach(l => {
            const line = document.createElementNS("http://www.w3.org/2000/svg", "line");
            line.setAttribute("class", "link");
            line.setAttribute("data-source", l.source.id);
            line.setAttribute("data-target", l.target.id);
            line.setAttribute("x1", l.source.x);
            line.setAttribute("y1", l.source.y);
            line.setAttribute("x2", l.target.x);
            line.setAttribute("y2", l.target.y);
            line.setAttribute("marker-end", "url(#arrowhead)");
            gRoot.appendChild(line);
        });

        // Render Nodes
        nodes.forEach(n => {
            const nodeG = document.createElementNS("http://www.w3.org/2000/svg", "g");
            nodeG.setAttribute("class", "node");
            nodeG.setAttribute("data-id", n.id);
            nodeG.setAttribute("transform", `translate(${n.x}, ${n.y})`);
            nodeG.onclick = (e) => {
                e.stopPropagation();
                showDetails(n);
                highlightConnected(n, links);
            };

            const rect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
            const w = 120;
            const h = 50;
            rect.setAttribute("width", w);
            rect.setAttribute("height", h);
            rect.setAttribute("x", -w/2);
            rect.setAttribute("y", -h/2);
            rect.setAttribute("rx", 6);
            rect.setAttribute("stroke", getColor(n.type));
            nodeG.appendChild(rect);

            const text = document.createElementNS("http://www.w3.org/2000/svg", "text");
            text.setAttribute("text-anchor", "middle");
            text.setAttribute("dy", "0.3em");
            text.textContent = n.name;
            nodeG.appendChild(text);

            gRoot.appendChild(nodeG);
        });

        // Initial centering
        const containerRect = container.getBoundingClientRect();
        zoomX = containerRect.width / 2;
        zoomY = containerRect.height / 2;
        updateTransform();
    }

    function renderSubNodes(gRoot, parent, items, radius, nodes, links) {
        if (!items) return;
        items.forEach((item, i) => {
            const angle = (i / items.length) * 360;
            const rad = angle * (Math.PI / 180);
            const node = {
                id: parent.id + '_' + i,
                name: item,
                type: 'SUB',
                description: 'Member of ' + parent.name,
                x: parent.x + Math.cos(rad) * radius,
                y: parent.y + Math.sin(rad) * radius,
                data: item
            };
            nodes.push(node);
            links.push({ source: parent, target: node, label: 'member' });
        });
    }

    function getColor(type) {
        const colors = {
            'CORE': '#007acc',
            'VIEW': '#3b82f6',
            'STORE': '#10b981',
            'BRIDGE': '#f59e0b',
            'FLOW': '#8b5cf6',
            'DATA': '#6366f1',
            'LINK': '#f43f5e',
            'SUB': '#94a3b8'
        };
        return colors[type] || '#ccc';
    }

    function highlightConnected(node, links) {
        const connectedIds = new Set();
        connectedIds.add(node.id);
        links.forEach(l => {
            if (l.source.id === node.id) connectedIds.add(l.target.id);
            if (l.target.id === node.id) connectedIds.add(l.source.id);
        });

        document.querySelectorAll('.node').forEach(nodeEl => {
            const id = nodeEl.getAttribute('data-id');
            nodeEl.style.filter = connectedIds.has(id) ? 'none' : 'grayscale(1) opacity(0.3)';
        });
    }

    container.onclick = () => {
        detailsPanel.classList.remove("active");
        document.querySelectorAll('.node').forEach(nodeEl => {
            nodeEl.style.filter = 'none';
        });
    };

    function showDetails(node) {
        detailsPanel.classList.add("active");
        detailsPanel.innerHTML = `
            <div class="panel-header">
                <div>
                    <h2 style="margin:0; font-size: 1.1em; color:var(--accent);">${node.name}</h2>
                    <span class="type-badge">${node.type}</span>
                </div>
                <button onclick="document.getElementById('details-panel').classList.remove('active')" class="btn btn-sm" style="background:none;">&times;</button>
            </div>
            <div class="panel-body">
                <p><strong>Description:</strong><br>${node.description}</p>
                <div style="margin-top:20px;">
                    <label style="font-size:10px; color:var(--text-dim); text-transform:uppercase;">Raw Data</label>
                    <pre style="background:#f3f3f3; color:#333; padding:10px; border-radius:4px; font-size:10px; overflow:auto; border:1px solid #ccc;">${JSON.stringify(node.data, null, 2)}</pre>
                </div>
            </div>
        `;
    }

})();
