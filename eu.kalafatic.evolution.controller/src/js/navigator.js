(function() {
    // Global Error Bridge
    window.onerror = function(message, source, lineno, colno, error) {
        const errorMsg = "JS Error: " + message + " at " + source + ":" + lineno + ":" + colno;
        if (window.logFunction) window.logFunction(errorMsg);
        return false;
    };

    const svg = d3.select("#architecture-svg");
    const container = d3.select("#architecture-container");

    let rect = container.node().getBoundingClientRect();
    let width = rect.width || window.innerWidth || 800;
    let height = rect.height || window.innerHeight || 600;

    const gMain = svg.append("g");
    const gLinks = gMain.append("g").attr("class", "links");
    const gEdgeLabels = gMain.append("g").attr("class", "edge-labels");
    const gNodes = gMain.append("g").attr("class", "nodes");

    const zoom = d3.zoom()
        .scaleExtent([0.1, 4])
        .on("zoom", (event) => {
            gMain.attr("transform", event.transform);
        });

    svg.call(zoom);

    window.addEventListener('resize', () => {
        let newRect = container.node().getBoundingClientRect();
        width = newRect.width || window.innerWidth || 800;
        height = newRect.height || window.innerHeight || 600;
        if (simulation) simulation.force("center", d3.forceCenter(width / 2, height / 2)).alpha(0.3).restart();
    });

    let simulation;
    let graphData = { nodes: [], links: [] };

    window.updateGraph = function(data) {
        if (typeof log === 'function') log("updateGraph called with " + (data && data.components ? data.components.length : 0) + " components.");

        if (!data || !data.components || data.components.length === 0) {
            d3.select("#empty-state").classed("active", true);
            return;
        }

        const nodes = data.components.map(c => ({
            id: c.id,
            name: c.name,
            type: c.type,
            description: c.description,
            importance: c.importanceScore || 0.5,
            path: c.path,
            useCases: c.useCases || [],
            keyClasses: c.keyClasses || []
        }));

        const nodeIds = new Set(nodes.map(n => n.id));

        const links = data.relationships
            .filter(r => nodeIds.has(r.from) && nodeIds.has(r.to))
            .map(r => ({
                source: r.from,
                target: r.to,
                type: r.type
            }));

        graphData = { nodes, links };
        d3.select("#empty-state").classed("active", nodes.length === 0);
        render();
    };

    function render() {
        if (simulation) simulation.stop();

        simulation = d3.forceSimulation(graphData.nodes)
            .force("link", d3.forceLink(graphData.links).id(d => d.id).distance(180))
            .force("charge", d3.forceManyBody().strength(-600))
            .force("center", d3.forceCenter(width / 2, height / 2))
            .force("collision", d3.forceCollide().radius(80))
            .on("tick", ticked);

        const link = gLinks.selectAll(".link")
            .data(graphData.links)
            .join("line")
            .attr("class", "link")
            .attr("stroke", "#cbd5e1")
            .attr("stroke-width", 1.5)
            .attr("marker-end", "url(#arrowhead)");

        const edgeLabel = gEdgeLabels.selectAll(".edge-label")
            .data(graphData.links)
            .join("text")
            .attr("class", "edge-label")
            .attr("font-size", "10px")
            .attr("fill", "#94a3b8")
            .attr("text-anchor", "middle")
            .text(d => d.type);

        const node = gNodes.selectAll(".node")
            .data(graphData.nodes)
            .join("g")
            .attr("class", d => "node " + d.type.toLowerCase())
            .call(d3.drag()
                .on("start", dragstarted)
                .on("drag", dragged)
                .on("end", dragended))
            .on("click", (event, d) => {
                event.stopPropagation();
                showDetails(d);
            })
            .on("contextmenu", (event, d) => {
                event.preventDefault();
                showContextMenu(event, d);
            });

        node.selectAll("rect")
            .data(d => [d])
            .join("rect")
            .attr("width", d => 140 + (d.importance * 30))
            .attr("height", 44)
            .attr("x", d => -(70 + (d.importance * 15)))
            .attr("y", -22)
            .attr("rx", 6)
            .attr("fill", "white")
            .attr("stroke", d => getRoleColor(d.type));

        node.selectAll("text")
            .data(d => [d])
            .join("text")
            .attr("text-anchor", "middle")
            .attr("dy", ".35em")
            .attr("font-size", "11px")
            .attr("fill", "#1e293b")
            .text(d => d.name.length > 20 ? d.name.substring(0, 17) + '...' : d.name);

        function ticked() {
            link
                .attr("x1", d => d.source.x)
                .attr("y1", d => d.source.y)
                .attr("x2", d => d.target.x)
                .attr("y2", d => d.target.y);

            edgeLabel
                .attr("x", d => (d.source.x + d.target.x) / 2)
                .attr("y", d => (d.source.y + d.target.y) / 2 - 5);

            node
                .attr("transform", d => `translate(${d.x}, ${d.y})`);
        }
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

    function showDetails(node) {
        const panel = d3.select("#details-panel");
        panel.classed("active", true);
        panel.html(`
            <div class="panel-header">
                <div>
                    <h2 style="margin:0; font-size: 16px;">${node.name}</h2>
                    <span class="type-badge">${node.type}</span>
                </div>
                <button onclick="d3.select('#details-panel').classed('active', false)" style="background:none; border:none; color:#64748b; font-size:20px; cursor:pointer; padding:0;">&times;</button>
            </div>
            <div class="panel-body">
                <div style="margin-bottom: 20px;">
                    <label style="font-size:11px; color:#94a3b8; text-transform:uppercase; font-weight:bold;">Description</label>
                    <p style="margin:5px 0;">${node.description || 'No description available.'}</p>
                </div>

                <div style="margin-bottom: 15px;">
                    <label style="font-size:11px; color:#94a3b8; text-transform:uppercase; font-weight:bold;">Physical Path</label>
                    <code style="display:block; background:#f8fafc; padding:5px; border-radius:4px; margin-top:5px; font-size:11px; word-break:break-all;">${node.path || 'N/A'}</code>
                </div>

                <div style="margin-bottom: 20px;">
                    <label style="font-size:11px; color:#94a3b8; text-transform:uppercase; font-weight:bold;">Significance</label>
                    <div style="height:6px; background:#e2e8f0; border-radius:3px; margin-top:8px; overflow:hidden;">
                        <div style="width:${node.importance * 100}%; height:100%; background:var(--accent);"></div>
                    </div>
                </div>

                ${node.keyClasses.length > 0 ? `
                    <div style="margin-bottom: 20px;">
                        <label style="font-size:11px; color:#94a3b8; text-transform:uppercase; font-weight:bold;">Key Classes</label>
                        <ul style="margin:8px 0; padding-left:18px;">${node.keyClasses.map(c => `<li>${c}</li>`).join('')}</ul>
                    </div>
                ` : ''}

                ${node.useCases.length > 0 ? `
                    <div style="margin-bottom: 20px;">
                        <label style="font-size:11px; color:#94a3b8; text-transform:uppercase; font-weight:bold;">Use Cases</label>
                        <ul style="margin:8px 0; padding-left:18px;">${node.useCases.map(u => `<li>${u}</li>`).join('')}</ul>
                    </div>
                ` : ''}

                <div style="display:flex; gap:10px; margin-top:30px;">
                    <button onclick="javaAction('${node.id}', 'OPEN')" style="background:var(--accent); color:white; border:none; padding:8px; border-radius:6px; flex:1; text-align:center; font-weight:600;">Open File</button>
                    <button onclick="javaAction('${node.id}', 'CONTEXT')" style="background:#f1f5f9; color:#475569; border:none; padding:8px; border-radius:6px; flex:1; text-align:center;">Generate Context</button>
                </div>
            </div>
        `);
    }

    function showContextMenu(event, node) {
        const menu = d3.select("#context-menu");
        if (menu.empty()) return;

        menu.style("left", event.pageX + "px")
            .style("top", event.pageY + "px")
            .classed("active", true);

        menu.html(`
            <div class="menu-item" onclick="javaAction('${node.id}', 'EXPAND')">Expand Neighborhood</div>
            <div class="menu-item" onclick="javaAction('${node.id}', 'COLLAPSE')">Collapse Neighborhood</div>
            <hr>
            <div class="menu-item" onclick="javaAction('${node.id}', 'SHOW_CHILDREN')">Show Child Nodes</div>
            <div class="menu-item" onclick="javaAction('${node.id}', 'SHOW_USE_CASES')">Show Use Cases</div>
            <div class="menu-item" onclick="javaAction('${node.id}', 'SHOW_CLASSES')">Show Key Classes</div>
        `);

        d3.select("body").on("click.menu-close", () => {
            menu.classed("active", false);
        });
    }

    window.javaAction = function(id, action) {
        if (window.navigatorFunction) {
            window.navigatorFunction(id, action);
        } else {
            console.log("Java action (Offline):", id, action);
        }
    };

    function dragstarted(event, d) {
        if (!event.active) simulation.alphaTarget(0.3).restart();
        d.fx = d.x;
        d.fy = d.y;
    }

    function dragged(event, d) {
        d.fx = event.x;
        d.fy = event.y;
    }

    function dragended(event, d) {
        if (!event.active) simulation.alphaTarget(0);
        d.fx = null;
        d.fy = null;
    }

})();

window.showPopup = function(title, items) {
    const popup = d3.select("#popup-panel");
    if (popup.empty()) {
        alert(title + "\n" + (items ? items.join("\n") : "None"));
        return;
    }
    popup.style("display", "block");
    d3.select("#popup-title").text(title);
    const content = d3.select("#popup-content");
    content.html("");
    if (items && items.length > 0) {
        const ul = content.append("ul");
        items.forEach(item => {
            ul.append("li").text(item);
        });
    } else {
        content.append("p").text("None found.");
    }
};
