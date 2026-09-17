(function() {
    const SVG_NS = "http://www.w3.org/2000/svg";
    const svg = document.getElementById("workflow-svg");
    const container = document.getElementById("workflow-container");

    let width = window.innerWidth;
    let height = window.innerHeight;

    // Create main viewport groups
    const gMain = document.createElementNS(SVG_NS, "g");
    gMain.setAttribute("id", "main-group");
    const gLinks = document.createElementNS(SVG_NS, "g");
    gLinks.setAttribute("class", "links");
    const gNodes = document.createElementNS(SVG_NS, "g");
    gNodes.setAttribute("class", "nodes");
    gMain.appendChild(gLinks);
    gMain.appendChild(gNodes);
    svg.appendChild(gMain);

    // Pan & Zoom state
    let transform = { x: 0, y: 0, k: 1 };
    let isPanning = false;
    let startPos = { x: 0, y: 0 };

    function updateTransform() {
        gMain.setAttribute("transform", `translate(${transform.x}, ${transform.y}) scale(${transform.k})`);
    }

    svg.addEventListener("mousedown", (e) => {
        if (e.target === svg || e.target === gMain || e.target === gLinks || e.target === gNodes) {
            isPanning = true;
            startPos = { x: e.clientX - transform.x, y: e.clientY - transform.y };
            svg.style.cursor = "grabbing";
        }
    });

    window.addEventListener("mousemove", (e) => {
        if (isPanning) {
            transform.x = e.clientX - startPos.x;
            transform.y = e.clientY - startPos.y;
            updateTransform();
        }
    });

    window.addEventListener("mouseup", () => {
        if (isPanning) {
            isPanning = false;
            svg.style.cursor = "default";
        }
    });

    svg.addEventListener("wheel", (e) => {
        e.preventDefault();
        const factor = e.deltaY < 0 ? 1.15 : 0.85;
        const newK = Math.min(Math.max(transform.k * factor, 0.1), 4);

        const rect = svg.getBoundingClientRect();
        const mouseX = e.clientX - rect.left;
        const mouseY = e.clientY - rect.top;

        transform.x = mouseX - (mouseX - transform.x) * (newK / transform.k);
        transform.y = mouseY - (mouseY - transform.y) * (newK / transform.k);
        transform.k = newK;

        updateTransform();
    }, { passive: false });

    window.resetZoom = function() {
        transform = { x: 0, y: 0, k: 1 };
        updateTransform();
    };

    window.applyZoom = function(factor) {
        const center = { x: width / 2, y: height / 2 };
        const newK = Math.min(Math.max(transform.k * factor, 0.1), 4);
        transform.x = center.x - (center.x - transform.x) * (newK / transform.k);
        transform.y = center.y - (center.y - transform.y) * (newK / transform.k);
        transform.k = newK;
        updateTransform();
    };

    window.fitToScreen = function() {
        if (!graphData || !graphData.nodes || graphData.nodes.length === 0) return;

        let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
        graphData.nodes.forEach(n => {
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

        transform.k = scale;
        transform.x = (width - graphWidth * scale) / 2 - minX * scale;
        transform.y = (height - graphHeight * scale) / 2 - minY * scale;
        updateTransform();
    };

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

    window.closeDetails = function() {
        const panel = document.getElementById("details-panel");
        if (panel) panel.classList.add("hidden");
    };

    function showNodeDetails(node) {
        const panel = document.getElementById("details-panel");
        if (!panel) return;
        panel.classList.remove("hidden");

        const title = document.getElementById("details-title");
        if (title) title.textContent = (node.id || "").toUpperCase();

        let html = `
            <div class="metric-row">
                <span class="metric-label">Node Type:</span>
                <span class="metric-value">${node.type || 'N/A'}</span>
            </div>
            <div class="metric-row">
                <span class="metric-label">Status:</span>
                <span class="node-status-badge status-${(node.status || '').toLowerCase()}">${node.status || 'N/A'}</span>
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

        const note = nodeNotes[(node.id || '').toLowerCase()] || nodeNotes[(node.type || '').toLowerCase()] || "Interactive node representing an active evolution task step.";
        html += `
            <div class="node-notes">
                <strong>Architectural Note:</strong><br/>
                ${note}
            </div>
        `;

        const detailsContent = document.getElementById("details-content");
        if (detailsContent) detailsContent.innerHTML = html;
    }

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
        svg.setAttribute("width", width);
        svg.setAttribute("height", height);
        render();
    });

    function render() {
        gLinks.innerHTML = "";
        gNodes.innerHTML = "";

        const nodes = graphData.nodes || [];
        const links = graphData.links || [];

        const nodeMap = new Map();
        nodes.forEach(n => nodeMap.set(n.id, n));

        // Hierarchical Level Assignment
        const levels = new Map();
        function assignLevel(nodeId, level) {
            if (levels.has(nodeId) && levels.get(nodeId) >= level) return;
            levels.set(nodeId, level);
            links.filter(l => l.from === nodeId).forEach(l => assignLevel(l.to, level + 1));
        }

        const targets = new Set(links.map(l => l.to));
        const roots = nodes.filter(n => !targets.has(n.id));
        if (roots.length === 0 && nodes.length > 0) roots.push(nodes[0]);

        roots.forEach(r => assignLevel(r.id, 0));

        // Group by level
        const levelGroups = new Map();
        nodes.forEach(node => {
            const lvl = levels.get(node.id) || 0;
            if (!levelGroups.has(lvl)) levelGroups.set(lvl, []);
            levelGroups.get(lvl).push(node);
        });

        const levelSpacing = 250;
        const siblingSpacing = 100;

        levelGroups.forEach((group, level) => {
            const totalHeight = (group.length - 1) * siblingSpacing;
            group.forEach((node, i) => {
                node.x = level * levelSpacing + 50;
                node.y = (height / 2) - (totalHeight / 2) + (i * siblingSpacing);
            });
        });

        // Branching for Darwin variants / mutations
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

        const tooltip = document.getElementById("tooltip");

        // Render Links
        links.forEach(l => {
            const s = nodeMap.get(l.from);
            const t = nodeMap.get(l.to);
            if (!s || !t) return;

            const x0 = s.x + 140;
            const y0 = s.y + 20;
            const x1 = t.x;
            const y1 = t.y + 20;
            const mx = (x0 + x1) / 2;

            const path = document.createElementNS(SVG_NS, "path");
            path.setAttribute("class", "link " + (l.active ? "active" : ""));
            path.setAttribute("d", `M${x0},${y0} C${mx},${y0} ${mx},${y1} ${x1},${y1}`);
            gLinks.appendChild(path);
        });

        // Render Nodes
        nodes.forEach(d => {
            const g = document.createElementNS(SVG_NS, "g");
            let cls = "node";
            if (d.status === 'RUNNING') cls += " active";
            if (d.status === 'WAITING_USER') cls += " waiting";
            if (d.status === 'FAILED') cls += " failed";
            g.setAttribute("class", cls);
            g.setAttribute("transform", `translate(${d.x}, ${d.y})`);

            // Node Rectangle
            const rect = document.createElementNS(SVG_NS, "rect");
            rect.setAttribute("width", "140");
            rect.setAttribute("height", "40");
            g.appendChild(rect);

            // Icon
            const iconText = document.createElementNS(SVG_NS, "text");
            iconText.setAttribute("class", "node-type-icon");
            iconText.setAttribute("x", "15");
            iconText.setAttribute("y", "26");
            iconText.textContent = typeIcons[d.type] || '\uD83D\uDCC4';
            g.appendChild(iconText);

            // Label
            const labelText = document.createElementNS(SVG_NS, "text");
            labelText.setAttribute("class", "node-id");
            labelText.setAttribute("x", "45");
            labelText.setAttribute("y", "25");
            labelText.textContent = d.id.length > 12 ? d.id.substring(0, 10) + ".." : d.id;
            g.appendChild(labelText);

            // Actions
            if (d.actions && d.actions.length > 0) {
                const ag = document.createElementNS(SVG_NS, "g");
                ag.setAttribute("class", "actions-group");
                d.actions.forEach((action, i) => {
                    const circle = document.createElementNS(SVG_NS, "circle");
                    circle.setAttribute("cx", 140 - (i * 15) - 10);
                    circle.setAttribute("cy", 5);
                    circle.setAttribute("r", 6);
                    circle.setAttribute("class", "action-btn " + action.toLowerCase());

                    const titleEl = document.createElementNS(SVG_NS, "title");
                    titleEl.textContent = action;
                    circle.appendChild(titleEl);

                    circle.addEventListener("click", (e) => {
                        e.stopPropagation();
                        if (window.javaAction) window.javaAction(d.id, action);
                    });
                    ag.appendChild(circle);
                });
                g.appendChild(ag);
            }

            // Runtime State
            if (d.runtimeState) {
                const stateText = document.createElementNS(SVG_NS, "text");
                stateText.setAttribute("class", "runtime-state");
                stateText.setAttribute("x", "5");
                stateText.setAttribute("y", "55");
                stateText.textContent = d.runtimeState;
                g.appendChild(stateText);
            }

            // Tooltip and Click events
            g.addEventListener("mouseover", (e) => {
                if (!tooltip) return;
                tooltip.style.opacity = "0.95";
                let tooltipHtml = `<h4>${d.id.toUpperCase()}</h4>`;
                tooltipHtml += `<p><strong>Type:</strong> ${d.type}</p>`;
                tooltipHtml += `<p><strong>Status:</strong> ${d.status}</p>`;
                if (d.runtimeState) {
                    tooltipHtml += `<p><strong>State:</strong> ${d.runtimeState}</p>`;
                }
                tooltip.innerHTML = tooltipHtml;
                tooltip.style.left = (e.pageX + 15) + "px";
                tooltip.style.top = (e.pageY - 15) + "px";
            });

            g.addEventListener("mousemove", (e) => {
                if (!tooltip) return;
                tooltip.style.left = (e.pageX + 15) + "px";
                tooltip.style.top = (e.pageY - 15) + "px";
            });

            g.addEventListener("mouseout", () => {
                if (!tooltip) return;
                tooltip.style.opacity = "0";
            });

            g.addEventListener("click", (e) => {
                e.stopPropagation();
                showNodeDetails(d);
                if (window.javaAction) window.javaAction(d.id, 'CLICK');
            });

            gNodes.appendChild(g);
        });
    }

})();
