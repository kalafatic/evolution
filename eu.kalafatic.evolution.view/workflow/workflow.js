(function() {
    const SVG_NS = "http://www.w3.org/2000/svg";
    const svg = document.getElementById("workflow-svg");
    const container = document.getElementById("workflow-container");

    const NODE_WIDTH = 200;
    const NODE_HEIGHT = 56;
    const LEVEL_SPACING = 270;
    const SIBLING_SPACING = 85;

    let width = container ? container.clientWidth || window.innerWidth : window.innerWidth;
    let height = container ? container.clientHeight || window.innerHeight : window.innerHeight;

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
    let transform = { x: 40, y: 40, k: 1 };
    let isPanning = false;
    let startPos = { x: 0, y: 0 };
    let selectedNodeId = null;
    let hasUserInteracted = false;

    function updateTransform() {
        gMain.setAttribute("transform", `translate(${transform.x}, ${transform.y}) scale(${transform.k})`);
    }

    svg.addEventListener("mousedown", (e) => {
        if (e.target === svg || e.target === gMain || e.target === gLinks || e.target === gNodes) {
            isPanning = true;
            hasUserInteracted = true;
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
        hasUserInteracted = true;
        const factor = e.deltaY < 0 ? 1.15 : 0.85;
        const newK = Math.min(Math.max(transform.k * factor, 0.15), 3.0);

        const rect = svg.getBoundingClientRect();
        const mouseX = e.clientX - rect.left;
        const mouseY = e.clientY - rect.top;

        transform.x = mouseX - (mouseX - transform.x) * (newK / transform.k);
        transform.y = mouseY - (mouseY - transform.y) * (newK / transform.k);
        transform.k = newK;

        updateTransform();
    }, { passive: false });

    window.resetZoom = function() {
        hasUserInteracted = true;
        transform = { x: 40, y: 40, k: 1 };
        updateTransform();
    };

    window.applyZoom = function(factor) {
        hasUserInteracted = true;
        const center = { x: width / 2, y: height / 2 };
        const newK = Math.min(Math.max(transform.k * factor, 0.15), 3.0);
        transform.x = center.x - (center.x - transform.x) * (newK / transform.k);
        transform.y = center.y - (center.y - transform.y) * (newK / transform.k);
        transform.k = newK;
        updateTransform();
    };

    window.fitToScreen = function() {
        if (!graphData || !graphData.nodes || graphData.nodes.length === 0) return;

        updateContainerSize();

        let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
        graphData.nodes.forEach(n => {
            if (n.x < minX) minX = n.x;
            if (n.y < minY) minY = n.y;
            if (n.x + NODE_WIDTH > maxX) maxX = n.x + NODE_WIDTH;
            if (n.y + NODE_HEIGHT > maxY) maxY = n.y + NODE_HEIGHT;
        });

        const graphWidth = maxX - minX;
        const graphHeight = maxY - minY;
        const padding = 50;

        const availW = Math.max(width - padding * 2, 100);
        const availH = Math.max(height - padding * 2, 100);

        let scale = Math.min(availW / graphWidth, availH / graphHeight);
        scale = Math.min(Math.max(scale, 0.25), 1.25);

        transform.k = scale;
        transform.x = (width - graphWidth * scale) / 2 - minX * scale;
        transform.y = (height - graphHeight * scale) / 2 - minY * scale;
        updateTransform();
    };

    function updateContainerSize() {
        if (container) {
            const rect = container.getBoundingClientRect();
            if (rect.width > 0) width = rect.width;
            if (rect.height > 0) height = rect.height;
        } else {
            width = window.innerWidth;
            height = window.innerHeight;
        }
        svg.setAttribute("width", width);
        svg.setAttribute("height", height);
    }

    const nodeNotes = {
        'user': 'The developer or operator directing actions, configuring hyperparameters, and reviewing outputs.',
        'forge_engine': 'The centralized LLM Forging Supervisor. Orchestrates the 7-stage training pipeline from scanning to Ollama registration.',
        'scanner': 'Source Scanner. Discovers project files, parses repository structures, and builds the raw textual corpus.',
        'enhancer': 'Dataset Enhancer. Trains BPE tokenizer, builds sliding-window instruction-response vectors, and formats .evodata containers.',
        'validator': 'Preflight Check. Verifies GPU/CPU resources, dataset sizes, model dimension consistency, and configuration validity.',
        'trainer': 'LLM Trainer. Executes causal Transformer backpropagation, computes cross-entropy losses, and saves epoch checkpoints.',
        'evaluator': 'Model Evaluator. Computes final convergence loss, evaluates validation metrics, and checks for model overfitting.',
        'exporter': 'GGUF Exporter. Serializes native .evo model container, applies quantization, and compiles compliant GGUF binaries.',
        'registration': 'Model Registration. Registers the compiled model with local Ollama runtime and performs native inference smoke testing.',
        'supervisor': 'Autonomous self-development supervisor coordinating coding iterations, testing, and patch verification.',
        'evolution_loop': 'Genetic mutation and evolution loop generating candidate variations for validation.',
        'orchestrator': 'Central evolutionary RCP orchestrator routing execution contexts and coordinating agents.',
        'local_llm': 'Local Large Language Model executing offline via local Ollama inference.',
        'remote_llm': 'Remote Frontier Language Model executing via secure cloud API proxy.',
        'assisted_coding': 'Assisted coding agent synthesizing clean Java code and completions.',
        'mediated_flow': 'Mediated human-in-the-loop review workflow generating patch bundles.',
        'workspace': 'Active workspace repository with sources, manifests, and test suites.'
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
        selectedNodeId = null;
        updateSelectedHighlight();
    };

    function updateSelectedHighlight() {
        const allNodes = svg.querySelectorAll(".node");
        allNodes.forEach(el => {
            if (el.getAttribute("data-id") === selectedNodeId) {
                el.classList.add("selected");
            } else {
                el.classList.remove("selected");
            }
        });
    }

    function showNodeDetails(node) {
        selectedNodeId = node.id;
        updateSelectedHighlight();

        const panel = document.getElementById("details-panel");
        if (!panel) return;
        panel.classList.remove("hidden");

        const title = document.getElementById("details-title");
        if (title) title.textContent = (node.label || node.id || "").toUpperCase();

        let html = `
            <div class="metric-row">
                <span class="metric-label">Stable ID:</span>
                <span class="metric-value" style="font-family:monospace; font-size:10px;">${node.id || 'N/A'}</span>
            </div>
            <div class="metric-row">
                <span class="metric-label">Node Type:</span>
                <span class="metric-value">${node.type || 'N/A'}</span>
            </div>
            <div class="metric-row">
                <span class="metric-label">Status:</span>
                <span class="node-status-badge status-${(node.status || '').toLowerCase()}">${node.status || 'N/A'}</span>
            </div>
        `;

        if (node.category) {
            html += `
                <div class="metric-row">
                    <span class="metric-label">Category:</span>
                    <span class="metric-value">${node.category}</span>
                </div>
            `;
        }

        if (node.runtimeState) {
            html += `
                <div class="metric-row">
                    <span class="metric-label">Runtime State:</span>
                    <span class="metric-value">${node.runtimeState}</span>
                </div>
            `;
        }

        if (node.description) {
            html += `
                <div class="metric-row">
                    <span class="metric-label">Description:</span>
                    <span class="metric-value">${node.description}</span>
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

        const note = nodeNotes[(node.id || '').toLowerCase()] || nodeNotes[(node.type || '').toLowerCase()] || node.description || "Active workflow execution node.";
        html += `
            <div class="node-notes">
                <strong>Architectural Details:</strong><br/>
                ${note}
            </div>
        `;

        const detailsContent = document.getElementById("details-content");
        if (detailsContent) detailsContent.innerHTML = html;
    }

    let graphData = { nodes: [], links: [] };

    window.updateGraph = function(data) {
        if (!data || !data.nodes) return;
        graphData = data;
        render();
        if (!hasUserInteracted) {
            window.fitToScreen();
        }
    };

    window.addEventListener('resize', () => {
        updateContainerSize();
        render();
        if (!hasUserInteracted) {
            window.fitToScreen();
        }
    });

    if (window.ResizeObserver && container) {
        const ro = new ResizeObserver(() => {
            updateContainerSize();
            render();
            if (!hasUserInteracted) {
                window.fitToScreen();
            }
        });
        ro.observe(container);
    }

    function computeHierarchicalLayout(nodes, links) {
        const nodeMap = new Map();
        nodes.forEach(n => nodeMap.set(n.id, n));

        // Find incoming & outgoing edges
        const inEdges = new Map();
        const outEdges = new Map();
        nodes.forEach(n => {
            inEdges.set(n.id, []);
            outEdges.set(n.id, []);
        });

        links.forEach(l => {
            if (outEdges.has(l.from)) outEdges.get(l.from).push(l.to);
            if (inEdges.has(l.to)) inEdges.get(l.to).push(l.from);
        });

        // Compute longest path from roots (DAG topological level)
        const levels = new Map();
        const visited = new Set();

        function getLevel(nodeId, stack) {
            if (stack.has(nodeId)) return 0; // Break cycles gracefully
            if (levels.has(nodeId)) return levels.get(nodeId);

            const incoming = inEdges.get(nodeId) || [];
            if (incoming.length === 0) {
                levels.set(nodeId, 0);
                return 0;
            }

            stack.add(nodeId);
            let maxParentLvl = 0;
            for (const pId of incoming) {
                const pLvl = getLevel(pId, stack);
                if (pLvl + 1 > maxParentLvl) maxParentLvl = pLvl + 1;
            }
            stack.delete(nodeId);

            levels.set(nodeId, maxParentLvl);
            return maxParentLvl;
        }

        nodes.forEach(n => getLevel(n.id, new Set()));

        // Group nodes by level
        const levelGroups = new Map();
        nodes.forEach(node => {
            const lvl = levels.get(node.id) || 0;
            if (!levelGroups.has(lvl)) levelGroups.set(lvl, []);
            levelGroups.get(lvl).push(node);
        });

        // Compute coordinate positions
        const centerY = height / 2;

        levelGroups.forEach((group, level) => {
            const totalHeight = (group.length - 1) * SIBLING_SPACING;
            const startY = centerY - (totalHeight / 2);

            group.forEach((node, i) => {
                node.x = level * LEVEL_SPACING + 50;
                node.y = startY + (i * SIBLING_SPACING);
            });
        });

        // Fine-tune Y position of single child nodes to align directly with their primary parent
        nodes.forEach(node => {
            const incoming = inEdges.get(node.id) || [];
            if (incoming.length === 1) {
                const parent = nodeMap.get(incoming[0]);
                const siblings = outEdges.get(incoming[0]) || [];
                if (parent && siblings.length === 1) {
                    node.y = parent.y;
                }
            }
        });
    }

    function render() {
        updateContainerSize();
        gLinks.innerHTML = "";
        gNodes.innerHTML = "";

        const nodes = graphData.nodes || [];
        const links = graphData.links || [];

        if (nodes.length === 0) return;

        const nodeMap = new Map();
        nodes.forEach(n => nodeMap.set(n.id, n));

        computeHierarchicalLayout(nodes, links);

        const tooltip = document.getElementById("tooltip");

        // Render Links
        links.forEach(l => {
            const s = nodeMap.get(l.from);
            const t = nodeMap.get(l.to);
            if (!s || !t) return;

            const x0 = s.x + NODE_WIDTH;
            const y0 = s.y + NODE_HEIGHT / 2;
            const x1 = t.x;
            const y1 = t.y + NODE_HEIGHT / 2;
            const mx = (x0 + x1) / 2;

            const path = document.createElementNS(SVG_NS, "path");
            let linkCls = "link";
            let markerId = "arrow";

            if (l.active || s.status === 'RUNNING' || t.status === 'RUNNING') {
                linkCls += " active";
                markerId = "arrow-active";
            } else if (s.status === 'DONE' && t.status === 'DONE') {
                linkCls += " done";
                markerId = "arrow-done";
            }

            path.setAttribute("class", linkCls);
            path.setAttribute("d", `M${x0},${y0} C${mx},${y0} ${mx},${y1} ${x1},${y1}`);
            path.setAttribute("marker-end", `url(#${markerId})`);
            gLinks.appendChild(path);
        });

        // Render Nodes
        nodes.forEach(d => {
            const g = document.createElementNS(SVG_NS, "g");
            const statusKey = (d.status || 'IDLE').toLowerCase();
            let cls = `node status-${statusKey}`;
            if (d.id === selectedNodeId) cls += " selected";
            g.setAttribute("class", cls);
            g.setAttribute("data-id", d.id);
            g.setAttribute("transform", `translate(${d.x}, ${d.y})`);

            // Node Background Rectangle
            const rect = document.createElementNS(SVG_NS, "rect");
            rect.setAttribute("class", "node-box");
            rect.setAttribute("width", NODE_WIDTH);
            rect.setAttribute("height", NODE_HEIGHT);
            g.appendChild(rect);

            // Icon
            const iconText = document.createElementNS(SVG_NS, "text");
            iconText.setAttribute("class", "node-type-icon");
            iconText.setAttribute("x", "14");
            iconText.setAttribute("y", String(NODE_HEIGHT / 2));
            iconText.textContent = typeIcons[d.type] || '\uD83D\uDCC4';
            g.appendChild(iconText);

            // Label (use label or id)
            const displayName = d.label || d.id;
            const shortName = displayName.length > 18 ? displayName.substring(0, 16) + "\u2026" : displayName;

            const labelText = document.createElementNS(SVG_NS, "text");
            labelText.setAttribute("class", "node-label");
            labelText.setAttribute("x", "42");
            labelText.setAttribute("y", "22");
            labelText.textContent = shortName;

            const titleEl = document.createElementNS(SVG_NS, "title");
            titleEl.textContent = `${displayName} (${d.id})\nStatus: ${d.status || 'N/A'}\n${d.description || ''}`;
            labelText.appendChild(titleEl);
            g.appendChild(labelText);

            // Subtitle / Runtime State
            const sub = d.runtimeState || d.description || d.category || "";
            if (sub) {
                const shortSub = sub.length > 25 ? sub.substring(0, 23) + "\u2026" : sub;
                const stateText = document.createElementNS(SVG_NS, "text");
                stateText.setAttribute("class", "node-subtitle");
                stateText.setAttribute("x", "42");
                stateText.setAttribute("y", "40");
                stateText.textContent = shortSub;
                const subTitle = document.createElementNS(SVG_NS, "title");
                subTitle.textContent = sub;
                stateText.appendChild(subTitle);
                g.appendChild(stateText);
            }

            // Status Badge Pill (Top Right)
            const badgeG = document.createElementNS(SVG_NS, "g");
            const badgeBg = document.createElementNS(SVG_NS, "rect");
            badgeBg.setAttribute("class", "badge-bg");
            badgeBg.setAttribute("x", String(NODE_WIDTH - 62));
            badgeBg.setAttribute("y", "7");
            badgeBg.setAttribute("width", "54");
            badgeBg.setAttribute("height", "15");

            let badgeFill = "#e2e8f0";
            let badgeTextColor = "#475569";
            if (statusKey === 'running') { badgeFill = "#dbeafe"; badgeTextColor = "#1d4ed8"; }
            else if (statusKey === 'done' || statusKey === 'ready') { badgeFill = "#dcfce7"; badgeTextColor = "#15803d"; }
            else if (statusKey === 'failed') { badgeFill = "#fee2e2"; badgeTextColor = "#b91c1c"; }
            else if (statusKey.startsWith('waiting')) { badgeFill = "#fef3c7"; badgeTextColor = "#b45309"; }

            badgeBg.setAttribute("fill", badgeFill);
            badgeG.appendChild(badgeBg);

            const badgeText = document.createElementNS(SVG_NS, "text");
            badgeText.setAttribute("class", "badge-text");
            badgeText.setAttribute("x", String(NODE_WIDTH - 35));
            badgeText.setAttribute("y", "15");
            badgeText.setAttribute("fill", badgeTextColor);
            badgeText.textContent = (d.status || "IDLE").toUpperCase();
            badgeG.appendChild(badgeText);
            g.appendChild(badgeG);

            // Interactive Actions if present
            if (d.actions && d.actions.length > 0) {
                const ag = document.createElementNS(SVG_NS, "g");
                ag.setAttribute("class", "actions-group");
                d.actions.forEach((action, i) => {
                    const circle = document.createElementNS(SVG_NS, "circle");
                    circle.setAttribute("cx", String(NODE_WIDTH - 15 - (i * 16)));
                    circle.setAttribute("cy", String(NODE_HEIGHT - 12));
                    circle.setAttribute("r", "5.5");
                    circle.setAttribute("class", "action-btn " + action.toLowerCase());

                    const actTitle = document.createElementNS(SVG_NS, "title");
                    actTitle.textContent = "Action: " + action;
                    circle.appendChild(actTitle);

                    circle.addEventListener("click", (e) => {
                        e.stopPropagation();
                        if (window.javaAction) window.javaAction(d.id, action);
                    });
                    ag.appendChild(circle);
                });
                g.appendChild(ag);
            }

            // Tooltip and Click events
            g.addEventListener("mouseover", (e) => {
                if (!tooltip) return;
                tooltip.style.opacity = "0.98";
                let tooltipHtml = `<h4>${displayName}</h4>`;
                tooltipHtml += `<p><strong>ID:</strong> <span style="font-family:monospace;">${d.id}</span></p>`;
                tooltipHtml += `<p><strong>Type:</strong> ${d.type}</p>`;
                tooltipHtml += `<p><strong>Status:</strong> ${d.status || 'N/A'}</p>`;
                if (d.category) tooltipHtml += `<p><strong>Category:</strong> ${d.category}</p>`;
                if (d.runtimeState) tooltipHtml += `<p><strong>State:</strong> ${d.runtimeState}</p>`;
                if (d.description) tooltipHtml += `<p><strong>Description:</strong> ${d.description}</p>`;
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
