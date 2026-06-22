
// ============== INTERACTIVE DEMOS ==============

let demoNetwork = [
    { name: "Input", neurons: 2 },
    { name: "Hidden", neurons: 3 },
    { name: "Output", neurons: 1 }
];
let demoSelected = null;

function renderInteractiveNeuronDemo() {
    const area = document.getElementById('viz-area');
    area.innerHTML = `
        <div id="demo-neuron-container" style="display:flex; flex-direction:column; height:100%; width:100%; background:white; color: #333;">
            <div id="demo-toolbar" style="height:40px; background:#333; color:white; display:flex; align-items:center; padding:0 10px; gap:8px; font-size: 0.8em;">
                <button class="btn btn-sm" onclick="addDemoLayer()">Add Layer</button>
                <button class="btn btn-sm" onclick="addDemoNeuron()">Add Neuron</button>
                <button class="btn btn-sm" onclick="removeDemoNeuron()">Delete Neuron</button>
                <button class="btn btn-sm" onclick="trainDemo()">Train</button>
                <button class="btn btn-sm" onclick="forwardDemo()">Forward</button>
            </div>
            <div id="demo-main" style="display:flex; flex:1; position:relative; overflow:hidden;">
                <div id="demo-canvas" style="flex:1; position:relative; background:white;">
                    <svg id="demo-lines" style="position:absolute; width:100%; height:100%; pointer-events:none;"></svg>
                </div>
            </div>
        </div>
    `;

    renderDemoNetwork();
}

function renderDemoNetwork() {
    const canvas = document.getElementById("demo-canvas");
    if (!canvas) return;

    // Clear previous neurons (keep svg)
    const neurons = canvas.querySelectorAll(".demo-neuron-el");
    neurons.forEach(n => n.remove());
    const layers = canvas.querySelectorAll(".demo-layer");
    layers.forEach(l => l.remove());

    demoNetwork.forEach((layer, l) => {
        let div = document.createElement("div");
        div.className = "demo-layer";
        div.style.position = "absolute";
        div.style.top = "40px";
        div.style.width = "120px";
        div.style.textAlign = "center";
        div.style.left = (40 + l * 180) + "px";

        div.innerHTML = `<div style="font-weight:bold; margin-bottom:10px; font-size:0.8em;">${layer.name}</div>`;

        for (let i = 0; i < layer.neurons; i++) {
            let n = document.createElement("div");
            n.className = "demo-neuron-el";
            n.style.width = "30px";
            n.style.height = "30px";
            n.style.borderRadius = "50%";
            n.style.background = "#4CAF50";
            n.style.color = "white";
            n.style.display = "flex";
            n.style.justifyContent = "center";
            n.style.alignItems = "center";
            n.style.margin = "10px auto";
            n.style.cursor = "pointer";
            n.style.userSelect = "none";
            n.style.fontSize = "0.7em";
            n.style.border = "2px solid transparent";

            n.innerHTML = i + 1;
            n.dataset.layer = l;
            n.dataset.index = i;

            n.onclick = function() {
                canvas.querySelectorAll(".demo-neuron-el").forEach(x => x.style.borderColor = "transparent");
                this.style.borderColor = "orange";
                demoSelected = this;
                inspectDemoNeuron(l, i);
            };

            div.appendChild(n);
        }
        canvas.appendChild(div);
    });

    setTimeout(drawDemoConnections, 50);
}

function drawDemoConnections() {
    const svg = document.getElementById("demo-lines");
    const canvas = document.getElementById("demo-canvas");
    if (!svg || !canvas) return;
    svg.innerHTML = '';

    const neurons = document.querySelectorAll(".demo-neuron-el");
    const cRect = canvas.getBoundingClientRect();

    neurons.forEach(a => {
        neurons.forEach(b => {
            if (parseInt(b.dataset.layer) === parseInt(a.dataset.layer) + 1) {
                const r1 = a.getBoundingClientRect();
                const r2 = b.getBoundingClientRect();

                const line = document.createElementNS("http://www.w3.org/2000/svg", "line");
                line.setAttribute("x1", r1.left + r1.width / 2 - cRect.left);
                line.setAttribute("y1", r1.top + r1.height / 2 - cRect.top);
                line.setAttribute("x2", r2.left + r2.width / 2 - cRect.left);
                line.setAttribute("y2", r2.top + r2.height / 2 - cRect.top);
                line.setAttribute("stroke", "#888");
                line.setAttribute("stroke-width", "1");
                svg.appendChild(line);
            }
        });
    });
}

function inspectDemoNeuron(l, i) {
    const details = document.getElementById('viz-details-content');
    details.innerHTML = `
        <div class="panel">
            <h3>Neuron Settings</h3>
            <div class="control-group">
                <label>Name</label>
                <input id="demo-n-name" value="Neuron ${i+1}">
            </div>
            <div class="control-group">
                <label>Bias</label>
                <input id="demo-n-bias" type="number" value="0">
            </div>
            <div class="control-group">
                <label>Activation</label>
                <select id="demo-n-activation">
                    <option>relu</option>
                    <option>sigmoid</option>
                    <option>tanh</option>
                    <option>linear</option>
                </select>
            </div>
            <button class="btn btn-sm btn-primary" style="width:100%" onclick="saveDemoNeuron()">Apply Changes</button>
        </div>
    `;
}

function addDemoLayer() {
    demoNetwork.splice(demoNetwork.length - 1, 0, {
        name: "Hidden",
        neurons: 3
    });
    renderDemoNetwork();
}

function addDemoNeuron() {
    if (demoSelected == null) return;
    let l = parseInt(demoSelected.dataset.layer);
    demoNetwork[l].neurons++;
    renderDemoNetwork();
}

function removeDemoNeuron() {
    if (demoSelected == null) return;
    let l = parseInt(demoSelected.dataset.layer);
    if (demoNetwork[l].neurons > 1)
        demoNetwork[l].neurons--;
    renderDemoNetwork();
}

function saveDemoNeuron() {
    if (demoSelected == null) return;
    const nameInput = document.getElementById("demo-n-name");
    demoSelected.innerHTML = nameInput.value.substring(0, 1);
    log("Neuron settings applied.");
}

function trainDemo() {
    const loss = (Math.random() * 0.2).toFixed(4);
    log("Demo Training - Loss: " + loss);
    const details = document.getElementById('viz-details-content');
    const lossDiv = document.createElement('div');
    lossDiv.style.marginTop = "10px";
    lossDiv.style.padding = "10px";
    lossDiv.style.background = "#e8f0fe";
    lossDiv.innerHTML = `<b>Training Status</b><br>Current Loss: ${loss}`;
    details.appendChild(lossDiv);
}

function forwardDemo() {
    document.querySelectorAll(".demo-neuron-el").forEach(n => {
        let v = Math.random() * 255;
        n.style.background = "rgb(" + v + ",80,80)";
    });
    log("Forward pass simulated.");
}

// ============== INTERACTIVE LLM DEMO ==============

function renderInteractiveLlmDemo() {
    const area = document.getElementById('viz-area');
    area.innerHTML = `
        <div style="padding:20px; text-align:center; height:100%; width:100%; background:#fff; color:#333;">
            <h3 style="color:var(--accent)">Interactive LLM: Token Prediction</h3>
            <p style="font-size:0.9em; color:#666;">Observe how the transformer predicts the next token based on context.</p>

            <div style="margin:20px auto; max-width:500px; padding:15px; border:1px solid #ddd; border-radius:8px; background:#f9f9f9; text-align:left;">
                <div style="font-weight:bold; margin-bottom:10px; font-size:0.8em; color:#888;">CONTEXT WINDOW</div>
                <div id="llm-context" style="font-family:monospace; font-size:1.1em; line-height:1.6;">
                    The quick brown fox <span style="background:#fff9c4; border-bottom:2px solid #fbc02d;">jumps</span>
                </div>
            </div>

            <div style="display:flex; justify-content:center; gap:20px; margin-top:30px;">
                <div style="width:150px;">
                    <div style="font-size:0.7em; color:#999; margin-bottom:5px;">TOP CANDIDATES</div>
                    <div id="llm-candidates"></div>
                </div>
                <div style="flex:1; max-width:300px;">
                    <div style="height:100px; display:flex; align-items:flex-end; gap:5px; border-bottom:1px solid #ccc; padding-bottom:5px;">
                        <div style="flex:1; height:80%; background:var(--accent);"></div>
                        <div style="flex:1; height:40%; background:#ccc;"></div>
                        <div style="flex:1; height:20%; background:#ccc;"></div>
                        <div style="flex:1; height:10%; background:#ccc;"></div>
                    </div>
                    <div style="font-size:0.7em; color:#999; margin-top:5px;">PROBABILITY DISTRIBUTION</div>
                </div>
            </div>

            <button class="btn btn-primary" style="margin-top:30px;" onclick="stepLlmDemo()">Predict Next Token</button>
        </div>
    `;
    updateLlmCandidates();
}

let llmTokens = ["The", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog"];
let llmIndex = 5;

function stepLlmDemo() {
    if (llmIndex >= llmTokens.length) llmIndex = 0;

    const context = document.getElementById('llm-context');
    let html = "";
    for(let i=0; i<llmIndex; i++) {
        html += llmTokens[i] + " ";
    }
    html += `<span style="background:#fff9c4; border-bottom:2px solid #fbc02d; padding:2px 4px; border-radius:3px;">${llmTokens[llmIndex]}</span>`;
    context.innerHTML = html;

    llmIndex++;
    updateLlmCandidates();
}

function updateLlmCandidates() {
    const candidates = document.getElementById('llm-candidates');
    if (!candidates) return;

    const nextToken = llmTokens[llmIndex] || "The";
    const others = ["runs", "leaps", "sleeps", "walks"];

    candidates.innerHTML = `
        <div style="padding:5px; border-bottom:1px solid #eee; display:flex; justify-content:space-between; font-size:0.9em;">
            <b style="color:var(--accent)">${nextToken}</b> <span>82%</span>
        </div>
        <div style="padding:5px; border-bottom:1px solid #eee; display:flex; justify-content:space-between; font-size:0.8em; color:#888;">
            <span>${others[0]}</span> <span>12%</span>
        </div>
        <div style="padding:5px; border-bottom:1px solid #eee; display:flex; justify-content:space-between; font-size:0.8em; color:#888;">
            <span>${others[1]}</span> <span>4%</span>
        </div>
    `;
}

// ============== INTERACTIVE CNN DEMO ==============

function renderInteractiveCnnDemo() {
    const area = document.getElementById('viz-area');
    area.innerHTML = `
        <div style="padding:20px; text-align:center; height:100%; width:100%; background:#fff; color:#333;">
            <h3 style="color:var(--accent)">Interactive CNN: Feature Extraction</h3>
            <p style="font-size:0.9em; color:#666;">Hover over the input image to see how filters detect patterns.</p>

            <div style="display:flex; justify-content:center; gap:40px; margin-top:30px; align-items:center;">
                <div>
                    <div style="font-size:0.8em; font-weight:bold; margin-bottom:10px;">INPUT IMAGE (7x7)</div>
                    <div id="cnn-input" style="display:grid; grid-template-columns:repeat(7, 20px); gap:2px; padding:5px; border:2px solid #333; background:#eee;">
                        ${Array(49).fill(0).map(() => `<div class="cnn-pixel" style="width:20px; height:20px; background:#fff; border:1px solid #ddd;"></div>`).join('')}
                    </div>
                </div>

                <div style="font-size:1.5em; color:#999;">➜</div>

                <div>
                    <div style="font-size:0.8em; font-weight:bold; margin-bottom:10px;">FILTER (3x3)</div>
                    <div style="display:grid; grid-template-columns:repeat(3, 15px); gap:2px; padding:5px; border:1px solid #666; background:#fff;">
                        <div style="background:#333; width:15px; height:15px;"></div>
                        <div style="background:#eee; width:15px; height:15px;"></div>
                        <div style="background:#333; width:15px; height:15px;"></div>
                        <div style="background:#eee; width:15px; height:15px;"></div>
                        <div style="background:#333; width:15px; height:15px;"></div>
                        <div style="background:#eee; width:15px; height:15px;"></div>
                        <div style="background:#333; width:15px; height:15px;"></div>
                        <div style="background:#eee; width:15px; height:15px;"></div>
                        <div style="background:#333; width:15px; height:15px;"></div>
                    </div>
                </div>

                <div style="font-size:1.5em; color:#999;">➜</div>

                <div>
                    <div style="font-size:0.8em; font-weight:bold; margin-bottom:10px;">FEATURE MAP (5x5)</div>
                    <div id="cnn-output" style="display:grid; grid-template-columns:repeat(5, 20px); gap:2px; padding:5px; border:1px solid var(--accent); background:#e3f2fd;">
                        ${Array(25).fill(0).map(() => `<div class="cnn-out-pixel" style="width:20px; height:20px; background:#fff; border:1px solid #bbdefb;"></div>`).join('')}
                    </div>
                </div>
            </div>

            <div style="margin-top:40px; font-size:0.8em; color:#888;">
                The filter slides over the input image to calculate activations.
            </div>
        </div>
    `;

    // Add some "image" content
    const pixels = document.querySelectorAll('.cnn-pixel');
    [10,11,12, 17,18,19, 24,25,26, 31,32,33].forEach(i => {
        if(pixels[i]) pixels[i].style.background = "#333";
    });

    pixels.forEach((p, i) => {
        p.onmouseover = () => highlightCnnFilter(i);
    });
}

function highlightCnnFilter(index) {
    const x = index % 7;
    const y = Math.floor(index / 7);

    if (x > 4 || y > 4) return;

    const pixels = document.querySelectorAll('.cnn-pixel');
    const outputs = document.querySelectorAll('.cnn-out-pixel');

    pixels.forEach(p => p.style.outline = "none");
    outputs.forEach(p => p.style.background = "#fff");

    // Highlight 3x3 window
    for(let dy=0; dy<3; dy++) {
        for(let dx=0; dx<3; dx++) {
            const pi = (y+dy)*7 + (x+dx);
            if(pixels[pi]) pixels[pi].style.outline = "1px solid red";
        }
    }

    // Highlight output
    const oi = y*5 + x;
    if(outputs[oi]) outputs[oi].style.background = "var(--accent)";
}
