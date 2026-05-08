(function() {
    const svg = document.getElementById('workflow-svg');
    const rootGroup = document.createElementNS("http://www.w3.org/2000/svg", "g");
    const nodesGroup = document.createElementNS("http://www.w3.org/2000/svg", "g");
    const linksGroup = document.createElementNS("http://www.w3.org/2000/svg", "g");

    svg.appendChild(rootGroup);
    rootGroup.appendChild(linksGroup);
    rootGroup.appendChild(nodesGroup);

    // Zoom/Pan State
    let scale = 1;
    let translateX = 0;
    let translateY = 0;
    let isDragging = false;
    let startX, startY;

    svg.onmousedown = (e) => {
        isDragging = true;
        startX = e.clientX - translateX;
        startY = e.clientY - translateY;
    };

    window.onmousemove = (e) => {
        if (isDragging) {
            translateX = e.clientX - startX;
            translateY = e.clientY - startY;
            updateTransform();
        }
    };

    window.onmouseup = () => isDragging = false;

    svg.onwheel = (e) => {
        e.preventDefault();
        const delta = e.deltaY > 0 ? 0.9 : 1.1;
        scale *= delta;
        updateTransform();
    };

    function updateTransform() {
        rootGroup.setAttribute("transform", `translate(${translateX}, ${translateY}) scale(${scale})`);
    }

    const typeIcons = {
        'USER': '👤',
        'SUPERVISOR': '🤖',
        'EVOLUTION_LOOP': '🔄',
        'LOCAL_LLM': '🏠',
        'REMOTE_LLM': '☁️',
        'ZIP_EXPORT': '📦',
        'DEPLOYMENT_TARGET': '🚀'
    };

    // Arrowhead definition
    const defs = document.createElementNS("http://www.w3.org/2000/svg", "defs");
    const marker = document.createElementNS("http://www.w3.org/2000/svg", "marker");
    marker.setAttribute("id", "arrowhead");
    marker.setAttribute("markerWidth", "10");
    marker.setAttribute("markerHeight", "7");
    marker.setAttribute("refX", "10");
    marker.setAttribute("refY", "3.5");
    marker.setAttribute("orient", "auto");
    const polygon = document.createElementNS("http://www.w3.org/2000/svg", "polygon");
    polygon.setAttribute("points", "0 0, 10 3.5, 0 7");
    polygon.setAttribute("fill", "#9ca3af");
    marker.appendChild(polygon);
    defs.appendChild(marker);
    svg.appendChild(defs);

    let graphData = { nodes: [], links: [] };

    window.updateGraph = function(data) {
        graphData = data;
        render();
    };

    function render() {
        nodesGroup.innerHTML = '';
        linksGroup.innerHTML = '';

        const nodeMap = {};

        // Layout: Basic horizontal flow for now
        let x = 50, y = 100;
        graphData.nodes.forEach(node => {
            node.x = node.x || x;
            node.y = node.y || y;
            nodeMap[node.id] = node;

            const g = document.createElementNS("http://www.w3.org/2000/svg", "g");
            g.setAttribute("class", "node " + (node.status === 'RUNNING' ? 'active' : ''));
            g.setAttribute("transform", `translate(${node.x}, ${node.y})`);
            g.onclick = () => { if (window.javaAction) window.javaAction(node.id, 'CLICK'); };

            const rect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
            rect.setAttribute("width", "120");
            rect.setAttribute("height", "40");
            g.appendChild(rect);

            const icon = document.createElementNS("http://www.w3.org/2000/svg", "text");
            icon.setAttribute("x", "15");
            icon.setAttribute("y", "25");
            icon.style.fontSize = "16px";
            icon.textContent = typeIcons[node.type] || '📄';
            g.appendChild(icon);

            const text = document.createElementNS("http://www.w3.org/2000/svg", "text");
            text.setAttribute("x", "70");
            text.setAttribute("y", "25");
            text.textContent = node.id;
            g.appendChild(text);

            if (node.actions && node.actions.length > 0) {
                const badge = document.createElementNS("http://www.w3.org/2000/svg", "circle");
                badge.setAttribute("cx", "110");
                badge.setAttribute("cy", "10");
                badge.setAttribute("r", "5");
                badge.setAttribute("fill", "#10b981");
                g.appendChild(badge);
                g.onclick = () => { if (window.javaAction) window.javaAction(node.id, node.actions[0]); };
            }

            nodesGroup.appendChild(g);
            x += 180;
            if (x > 800) { x = 50; y += 100; }
        });

        graphData.links.forEach(link => {
            const source = nodeMap[link.from];
            const target = nodeMap[link.to];
            if (source && target) {
                const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
                const d = `M ${source.x + 120} ${source.y + 20} L ${target.x} ${target.y + 20}`;
                path.setAttribute("d", d);
                path.setAttribute("class", "link " + (link.active ? 'active' : ''));
                linksGroup.appendChild(path);
            }
        });
    }

    window.resetZoom = function() {
        scale = 1;
        translateX = 0;
        translateY = 0;
        updateTransform();
    };

})();
