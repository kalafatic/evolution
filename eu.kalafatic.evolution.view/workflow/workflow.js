(function() {
    const svg = d3.select("#workflow-svg");
    const container = d3.select("#workflow-container");

    let width = window.innerWidth;
    let height = window.innerHeight;

    const nodeNotes = {
        'user': 'Represents the active developer triggering evolutionary tasks, reviewing mutant code proposals, and directing training pipelines.',
        'forge_engine': 'The centralized LLM Forging Supervisor. Oversees the complete pipeline of scanning codebase files, synthesizing instructions, training deep representations, compiling GGUF models, and registering them inside Ollama.',
        'scanner': 'Codebase Scanning & Corpus Discovery. Crawls the target repository (respecting .gitignore files), filters non-relevant formats, and cleans documentation into a synchronized character corpus.',
        'enhancer': 'Instruction Synthesizer & Tokenizer. Trains a local BPE vocabulary (4096 merges) and generates sliding window instruction-response sample vectors (context size 16, stride 8) to prepare optimal training dataset inputs.',
        'trainer': 'Causal Backpropagation & Loss Optimization. Implements a zero-dependency Decoder-only Transformer inside JVM. Calculates cross-entropy losses, estimates soft-probabilities, and adjusts network weights with an AdamW optimizer.',
        'exporter': 'Ollama GGUF compiler. Programmatically constructs fully compliant, aligned GGUF little-endian binaries. Serializes float weights, formats Modelfile instructions, and exports assets to dist/ and workspace target directories.',
        'registration': 'Ollama Server Integration. Checks if Ollama is online, matches available tags to prevent download timeouts, replaces Windows path backslashes, and registers the newly forged model programmatically via HTTP/CLI.',
        'orchestrator': 'The main evolutionary RCP orchestrator. Receives tasks, routes execution contexts, schedules background jobs, and synchronizes EMF model configurations.',
        'local_llm': 'Local Large Language Model (e.g., Llama-3.2, Qwen-2). Runs entirely offline in the local Ollama instance for secure private reasoning.',
        'remote_llm': 'Remote Frontier Language Model (e.g., OpenAI GPT-4o, Anthropic Claude 3.5 Sonnet). Accessible via secure API proxies for heavy reasoning workloads.',
        'assisted_coding': 'Assisted coding agent. Translates natural language requirements into concrete, clean Java source implementations with auto-complete proposals.',
        'mediated_flow': 'Mediated orchestration engine. Runs highly structured workflows requiring human-in-the-loop validation of generated patch bundles.',
        'zip_export': 'ZIP patch bundler. Compiles modified source files, resource assets, and dependency metadata into a compact ZIP package ready for deployment.',
        'workspace': 'Active Eclipse Developer Workspace. Contains the target project sources, EMF models, configurations, and test suites.'
    };

    window.closeDetails = function() {
        d3.select("#details-panel").classed("hidden", true);
    };

    function showNodeDetails(node) {
        const panel = d3.select("#details-panel");
        panel.classed("hidden", false);

        d3.select("#details-title").text(node.id.toUpperCase());

        let html = `
            <div class="metric-row">
                <span class="metric-label">Node Type:</span>
                <span class="metric-value">${node.type}</span>
            </div>
            <div class="metric-row">
                <span class="metric-label">Status:</span>
                <span class="node-status-badge status-${node.status.toLowerCase()}">${node.status}</span>
            </div>
        `;

        if (node.runtimeState) {
            html += `
                <div class="metric-row">
                    <span class="metric-label">Runtime State:</span>
                    <span class="metric-value">${node.runtimeState}</span>
                </div>
            `;
        }

        if (node.metadata && Object.keys(node.metadata).length > 0) {
            html += `<h4 style="margin: 14px 0 8px 0; border-bottom: 1px solid #e2e8f0; padding-bottom: 4px; font-size:11px; color:#475569; font-weight:700;">METRICS & TELEMETRY</h4>`;
            for (const [key, val] of Object.entries(node.metadata)) {
                html += `
                    <div class="metric-row">
                        <span class="metric-label">${key}:</span>
                        <span class="metric-value">${val}</span>
                    </div>
                `;
            }
        }

        const note = nodeNotes[node.id.toLowerCase()] || nodeNotes[node.type.toLowerCase()] || "Interactive node representing an active evolution task step.";
        html += `
            <div class="node-notes">
                <strong>Architectural Note:</strong><br/>
                ${note}
            </div>
        `;

        d3.select("#details-content").html(html);
    }

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

    window.fitToScreen = function() {
        if (!graphData || !graphData.nodes || graphData.nodes.length === 0) return;

        const nodes = graphData.nodes;
        let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
        nodes.forEach(n => {
            if (n.x < minX) minX = n.x;
            if (n.y < minY) minY = n.y;
            if (n.x + 140 > maxX) maxX = n.x + 140;
            if (n.y + 60 > maxY) maxY = n.y + 60;
        });

        const graphWidth = maxX - minX;
        const graphHeight = maxY - minY;
        const padding = 40;

        const scale = Math.min(
            (width - padding * 2) / graphWidth,
            (height - padding * 2) / graphHeight,
            1.5
        );

        const translateX = (width - graphWidth * scale) / 2 - minX * scale;
        const translateY = (height - graphHeight * scale) / 2 - minY * scale;

        svg.transition().duration(750).call(
            zoom.transform,
            d3.zoomIdentity.translate(translateX, translateY).scale(scale)
        );
    };

    window.applyZoom = function(factor) {
        svg.transition().duration(300).call(
            zoom.scaleBy, factor
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
        svg.attr("width", width).attr("height", height);
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

        const tooltip = d3.select("#tooltip");

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
            .on("mouseover", (event, d) => {
                tooltip.transition().duration(200).style("opacity", 0.95);
                let tooltipHtml = `<h4>${d.id.toUpperCase()}</h4>`;
                tooltipHtml += `<p><strong>Type:</strong> ${d.type}</p>`;
                tooltipHtml += `<p><strong>Status:</strong> ${d.status}</p>`;
                if (d.runtimeState) {
                    tooltipHtml += `<p><strong>State:</strong> ${d.runtimeState}</p>`;
                }
                tooltip.html(tooltipHtml)
                    .style("left", (event.pageX + 15) + "px")
                    .style("top", (event.pageY - 15) + "px");
            })
            .on("mousemove", (event) => {
                tooltip
                    .style("left", (event.pageX + 15) + "px")
                    .style("top", (event.pageY - 15) + "px");
            })
            .on("mouseout", () => {
                tooltip.transition().duration(200).style("opacity", 0);
            })
            .on("click", (event, d) => {
                event.stopPropagation();
                showNodeDetails(d);
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
