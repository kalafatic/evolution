(function() {
    const svg = d3.select("#architecture-svg");
    const container = d3.select("#architecture-container");
    const width = window.innerWidth;
    const height = window.innerHeight;

    const gMain = svg.append("g");
    const gLinks = gMain.append("g").attr("class", "links");
    const gNodes = gMain.append("g").attr("class", "nodes");

    const zoom = d3.zoom()
        .scaleExtent([0.1, 4])
        .on("zoom", (event) => {
            gMain.attr("transform", event.transform);
        });

    svg.call(zoom);

    let simulation;
    let graphData = { nodes: [], links: [] };

    window.updateGraph = function(data) {
        console.log("[NavigatorJS] Updating graph", data);
        if (!data || !data.components) return;

        const nodes = data.components.map(c => ({
            id: c.id,
            name: c.name,
            type: c.type,
            description: c.description,
            importance: c.importanceScore,
            path: c.path,
            useCases: c.useCases || [],
            keyClasses: c.keyClasses || []
        }));

        const links = data.relationships.map(r => ({
            source: r.from,
            target: r.to,
            type: r.type
        }));

        graphData = { nodes, links };
        render();
    };

    function render() {
        if (simulation) simulation.stop();

        simulation = d3.forceSimulation(graphData.nodes)
            .force("link", d3.forceLink(graphData.links).id(d => d.id).distance(150))
            .force("charge", d3.forceManyBody().strength(-500))
            .force("center", d3.forceCenter(width / 2, height / 2))
            .on("tick", ticked);

        const link = gLinks.selectAll(".link")
            .data(graphData.links)
            .join("line")
            .attr("class", "link")
            .attr("stroke", "#94a3b8")
            .attr("stroke-width", 2)
            .attr("marker-end", "url(#arrowhead)");

        const node = gNodes.selectAll(".node")
            .data(graphData.nodes)
            .join("g")
            .attr("class", d => "node " + d.type.toLowerCase())
            .call(d3.drag()
                .on("start", dragstarted)
                .on("drag", dragged)
                .on("end", dragended))
            .on("click", (event, d) => showDetails(d))
            .on("contextmenu", (event, d) => {
                event.preventDefault();
                showContextMenu(event, d);
            });

        node.selectAll("rect")
            .data(d => [d])
            .join("rect")
            .attr("width", d => 120 + (d.importance * 40))
            .attr("height", 40)
            .attr("x", d => -(60 + (d.importance * 20)))
            .attr("y", -20)
            .attr("rx", 8)
            .attr("fill", "white")
            .attr("stroke", d => getRoleColor(d.type));

        node.selectAll("text")
            .data(d => [d])
            .join("text")
            .attr("text-anchor", "middle")
            .attr("dy", ".35em")
            .attr("font-size", "12px")
            .attr("font-weight", "bold")
            .text(d => d.name);

        function ticked() {
            link
                .attr("x1", d => d.source.x)
                .attr("y1", d => d.source.y)
                .attr("x2", d => d.target.x)
                .attr("y2", d => d.target.y);

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
            'MODULE': '#64748b'
        };
        return colors[type] || '#94a3b8';
    }

    function showDetails(node) {
        const panel = d3.select("#details-panel");
        panel.classed("active", true);
        panel.html(`
            <div class="panel-header">
                <h2>${node.name}</h2>
                <span class="type-badge">${node.type}</span>
                <button onclick="d3.select('#details-panel').classed('active', false)">×</button>
            </div>
            <div class="panel-body">
                <p><strong>Path:</strong> ${node.path || 'N/A'}</p>
                <p><strong>Importance:</strong> ${Math.round(node.importance * 100)}%</p>
                <p>${node.description || 'No description available.'}</p>

                ${node.keyClasses.length > 0 ? `
                    <h3>Key Classes</h3>
                    <ul>${node.keyClasses.map(c => `<li>${c}</li>`).join('')}</ul>
                ` : ''}

                ${node.useCases.length > 0 ? `
                    <h3>Use Cases</h3>
                    <ul>${node.useCases.map(u => `<li>${u}</li>`).join('')}</ul>
                ` : ''}

                <div class="panel-actions">
                    <button class="action-btn" onclick="javaAction('${node.id}', 'OPEN')">Open Source</button>
                    <button class="action-btn" onclick="javaAction('${node.id}', 'CONTEXT')">Generate Context</button>
                </div>
            </div>
        `);
    }

    function showContextMenu(event, node) {
        const menu = d3.select("#context-menu");
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
            <hr>
            <div class="menu-item" onclick="javaAction('${node.id}', 'CENTER')">Center Graph Here</div>
        `);

        d3.select("body").on("click.menu-close", () => {
            menu.classed("active", false);
        });
    }

    window.javaAction = function(id, action) {
        if (window.navigatorFunction) {
            window.navigatorFunction(id, action);
        } else {
            console.log("Java action:", id, action);
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
