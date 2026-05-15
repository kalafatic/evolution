(function() {
    const svg = d3.select("#workflow-svg");
    const container = d3.select("#workflow-container");

    let width = window.innerWidth;
    let height = window.innerHeight;

    const gMain = svg.append("g");
    const gLinks = gMain.append("g").attr("class", "links");
    const gNodes = gMain.append("g").attr("class", "nodes");

    // Zoom setup
    const zoom = d3.zoom()
        .scaleExtent([0.1, 4])
        .on("zoom", (event) => {
            gMain.attr("transform", event.transform);
        });

    svg.call(zoom);

    window.resetZoom = function() {
        svg.transition().duration(750).call(
            zoom.transform,
            d3.zoomIdentity
        );
    };

    const typeIcons = {
        'USER': '\uD83D\uDC64',        // 👤
        'SUPERVISOR': '\uD83E\uDD16',  // 🤖
        'EVOLUTION_LOOP': '\uD83D\uDD04', // 🔄
        'LOCAL_LLM': '\uD83C\uDFE0',   // 🏠
        'REMOTE_LLM': '\u2601\uFE0F',   // ☁️
        'ZIP_EXPORT': '\uD83D\uDCE6',   // 📦
        'DEPLOYMENT_TARGET': '\uD83D\uDE80', // 🚀
        'DARWIN_VARIANT': '\uD83E\uDDEC', // 🧬
        'SELF_DEV_TASK': '\uD83D\uDCC4', // 📄
        'MEDIATED_FLOW': '\uD83D\uDD34'  // 🔴
    };

    let graphData = { nodes: [], links: [] };

    window.updateGraph = function(data) {
        console.log("[WorkflowJS] Updating graph", data);
        if (!data || !data.nodes) return;
        graphData = data;
        render();
    };

    window.addEventListener('resize', () => {
        width = window.innerWidth;
        height = window.innerHeight;
        render();
    });

    function render() {
        const nodes = graphData.nodes;
        const links = graphData.links;

        // Simple Hierarchical Layout logic
        const nodeMap = new Map();
        nodes.forEach(n => nodeMap.set(n.id, n));

        // Assign levels
        const levels = new Map();
        const visited = new Set();

        function assignLevel(nodeId, level) {
            if (levels.has(nodeId) && levels.get(nodeId) >= level) return;
            levels.set(nodeId, level);
            links.filter(l => l.from === nodeId).forEach(l => assignLevel(l.to, level + 1));
        }

        // Find roots (nodes with no incoming links)
        const targets = new Set(links.map(l => l.to));
        const roots = nodes.filter(n => !targets.has(n.id));
        if (roots.length === 0 && nodes.length > 0) roots.push(nodes[0]);

        roots.forEach(r => assignLevel(r.id, 0));

        // Group by level for X positioning
        const levelGroups = d3.group(nodes, n => levels.get(n.id) || 0);
        const nodeWidth = 180;
        const nodeHeight = 60;
        const levelSpacing = 250;
        const siblingSpacing = 100;

        levelGroups.forEach((group, level) => {
            const totalHeight = (group.length - 1) * siblingSpacing;
            group.forEach((node, i) => {
                node.x = level * levelSpacing + 50;
                node.y = (height / 2) - (totalHeight / 2) + (i * siblingSpacing);
            });
        });

        // Specific Darwin Variant Positioning (Branching)
        // If a node has multiple outgoing 'mutation' links, position them as branches
        nodes.forEach(node => {
            const children = links.filter(l => l.from === node.id);
            const mutations = children.filter(l => l.type === 'mutation');
            if (mutations.length > 1) {
                const variants = mutations.map(l => nodeMap.get(l.to)).filter(n => n);
                const totalVarHeight = (variants.length - 1) * siblingSpacing;
                variants.forEach((v, i) => {
                    v.x = node.x + levelSpacing;
                    v.y = node.y - (totalVarHeight / 2) + (i * siblingSpacing);
                });
            }
        });

        // Draw Links
        const linkGenerator = d3.linkHorizontal()
            .x(d => d.x + (d.width || 140))
            .y(d => d.y + 20);

        const linkData = links.map(l => {
            const source = nodeMap.get(l.from);
            const target = nodeMap.get(l.to);
            if (!source || !target) return null;
            return {
                source: { x: source.x, y: source.y, width: 140 },
                target: { x: target.x - 140, y: target.y } // Adjust for horizontal link
            };
        }).filter(l => l);

        const path = gLinks.selectAll(".link")
            .data(links)
            .join("path")
            .attr("class", d => "link " + (d.active ? "active" : ""))
            .attr("d", d => {
                const s = nodeMap.get(d.from);
                const t = nodeMap.get(d.to);
                if (!s || !t) return "";
                // Use curved diagonal
                const x0 = s.x + 140;
                const y0 = s.y + 20;
                const x1 = t.x;
                const y1 = t.y + 20;
                return `M${x0},${y0} C${(x0 + x1) / 2},${y0} ${(x0 + x1) / 2},${y1} ${x1},${y1}`;
            });

        // Draw Nodes
        const nodeGroups = gNodes.selectAll(".node")
            .data(nodes, d => d.id)
            .join("g")
            .attr("class", d => {
                let cls = "node";
                if (d.status === 'RUNNING') cls += " active";
                if (d.status === 'WAITING_USER') cls += " waiting";
                if (d.status === 'FAILED') cls += " failed";
                return cls;
            })
            .attr("transform", d => `translate(${d.x}, ${d.y})`)
            .on("click", (event, d) => {
                if (window.javaAction) window.javaAction(d.id, 'CLICK');
            });

        nodeGroups.selectAll("rect")
            .data(d => [d])
            .join("rect")
            .attr("width", 140)
            .attr("height", 40);

        nodeGroups.selectAll(".node-type-icon")
            .data(d => [d])
            .join("text")
            .attr("class", "node-type-icon")
            .attr("x", 15)
            .attr("y", 26)
            .text(d => typeIcons[d.type] || '\uD83D\uDCC4');

        nodeGroups.selectAll(".node-id")
            .data(d => [d])
            .join("text")
            .attr("class", "node-id")
            .attr("x", 45)
            .attr("y", 25)
            .text(d => d.id.length > 12 ? d.id.substring(0, 10) + ".." : d.id);

        // Actions
        nodeGroups.each(function(d) {
            if (d.actions && d.actions.length > 0) {
                const ag = d3.select(this).selectAll(".actions-group")
                    .data([d])
                    .join("g")
                    .attr("class", "actions-group");

                ag.selectAll("circle")
                    .data(d.actions)
                    .join("circle")
                    .attr("cx", (action, i) => 140 - (i * 15) - 10)
                    .attr("cy", 5)
                    .attr("r", 6)
                    .attr("class", action => "action-btn " + action.toLowerCase())
                    .on("click", (event, action) => {
                        event.stopPropagation();
                        if (window.javaAction) window.javaAction(d.id, action);
                    })
                    .append("title")
                    .text(action => action);
            }
        });

        // Runtime State
        nodeGroups.selectAll(".runtime-state")
            .data(d => d.runtimeState ? [d] : [])
            .join("text")
            .attr("class", "runtime-state")
            .attr("x", 5)
            .attr("y", 55)
            .text(d => d.runtimeState);
    }

})();
