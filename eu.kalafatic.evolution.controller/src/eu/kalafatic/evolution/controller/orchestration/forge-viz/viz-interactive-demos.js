
// ============== INTERACTIVE DEMOS ==============

(function() {

let demoNetwork = [
    { name: "Input", neurons: 2 },
    { name: "Hidden", neurons: 3 },
    { name: "Output", neurons: 1 }
];
let demoSelected = null;

window.renderInteractiveNeuronDemo = function() {
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
};

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
                const x1 = r1.left + r1.width / 2 - cRect.left;
                const y1 = r1.top + r1.height / 2 - cRect.top;
                const x2 = r2.left + r2.width / 2 - cRect.left;
                const y2 = r2.top + r2.height / 2 - cRect.top;

                const weight = (Math.random() * 2 - 1).toFixed(2);
                const opacity = Math.abs(weight);

                const line = document.createElementNS("http://www.w3.org/2000/svg", "line");
                line.setAttribute("x1", x1);
                line.setAttribute("y1", y1);
                line.setAttribute("x2", x2);
                line.setAttribute("y2", y2);
                line.setAttribute("stroke", weight > 0 ? "#4fc1ff" : "#f48771");
                line.setAttribute("stroke-width", Math.abs(weight) * 3 + 0.5);
                line.setAttribute("stroke-opacity", opacity * 0.8 + 0.2);
                svg.appendChild(line);

                // Add weight label on hover or if strong
                if (opacity > 0.8) {
                    const label = document.createElementNS("http://www.w3.org/2000/svg", "text");
                    label.setAttribute("x", (x1 + x2) / 2);
                    label.setAttribute("y", (y1 + y2) / 2 - 5);
                    label.setAttribute("font-size", "6px");
                    label.setAttribute("fill", "#666");
                    label.setAttribute("text-anchor", "middle");
                    label.textContent = weight;
                    svg.appendChild(label);
                }
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

window.addDemoLayer = function() {
    demoNetwork.splice(demoNetwork.length - 1, 0, {
        name: "Hidden",
        neurons: 3
    });
    renderDemoNetwork();
};

window.addDemoNeuron = function() {
    if (demoSelected == null) return;
    let l = parseInt(demoSelected.dataset.layer);
    demoNetwork[l].neurons++;
    renderDemoNetwork();
};

window.removeDemoNeuron = function() {
    if (demoSelected == null) return;
    let l = parseInt(demoSelected.dataset.layer);
    if (demoNetwork[l].neurons > 1)
        demoNetwork[l].neurons--;
    renderDemoNetwork();
};

window.saveDemoNeuron = function() {
    if (demoSelected == null) return;
    const nameInput = document.getElementById("demo-n-name");
    demoSelected.innerHTML = nameInput.value.substring(0, 1);
    log("Neuron settings applied.");
};

window.trainDemo = function() {
    const loss = (Math.random() * 0.2).toFixed(4);
    log("Demo Training - Loss: " + loss);
    const details = document.getElementById('viz-details-content');
    const lossDiv = document.createElement('div');
    lossDiv.style.marginTop = "10px";
    lossDiv.style.padding = "10px";
    lossDiv.style.background = "#e8f0fe";
    lossDiv.innerHTML = `<b>Training Status</b><br>Current Loss: ${loss}`;
    details.appendChild(lossDiv);
};

window.forwardDemo = function() {
    const neurons = document.querySelectorAll(".demo-neuron-el");
    neurons.forEach(n => {
        let val = Math.random();
        let color = Math.floor(val * 255);
        n.style.background = "rgb(" + color + ", 122, 204)";
        n.style.boxShadow = `0 0 ${val * 15}px rgba(0, 122, 204, 0.8)`;

        // Show activation value
        let badge = n.querySelector(".activation-badge");
        if (!badge) {
            badge = document.createElement("div");
            badge.className = "activation-badge";
            badge.style.position = "absolute";
            badge.style.top = "-15px";
            badge.style.fontSize = "8px";
            badge.style.background = "white";
            badge.style.padding = "1px 3px";
            badge.style.border = "1px solid #ccc";
            badge.style.borderRadius = "3px";
            badge.style.color = "#333";
            n.style.position = "relative";
            n.appendChild(badge);
        }
        badge.textContent = val.toFixed(2);
    });
    log("Forward pass simulated with numerical I/O.");
};

// ============== INTERACTIVE LLM EVO DEMO ==============

window.renderInteractiveLlmEvoDemo = function() {
    const area = document.getElementById('viz-area');
    area.innerHTML = `
        <div style="padding:20px; text-align:center; height:100%; width:100%; background:#fff; color:#333; overflow-y:auto;">
            <h3 style="color:var(--accent)">LLM-EVO: Educational Lifecycle</h3>
            <p style="font-size:0.9em; color:#666;">Visualizing the complete lifecycle from raw documents to GGUF deployment.</p>

            <div style="display:flex; justify-content:center; gap:10px; margin-top:20px; flex-wrap:wrap;">
                <div class="evo-stage-card" id="stage-data">
                    <div style="font-size:1.2em;">📄</div>
                    <div style="font-size:0.7em; font-weight:bold;">Data Loading</div>
                    <div style="font-size:0.6em; color:#888;">Markdown/PDF</div>
                </div>
                <div class="evo-stage-arrow">➜</div>
                <div class="evo-stage-card" id="stage-tokenizer">
                    <div style="font-size:1.2em;">🔡</div>
                    <div style="font-size:0.7em; font-weight:bold;">Tokenization</div>
                    <div style="font-size:0.6em; color:#888;">SimpleBPE</div>
                </div>
                <div class="evo-stage-arrow">➜</div>
                <div class="evo-stage-card" id="stage-transformer">
                    <div style="font-size:1.2em;">🧠</div>
                    <div style="font-size:0.7em; font-weight:bold;">Transformer</div>
                    <div style="font-size:0.6em; color:#888;">Attention/FFN</div>
                </div>
                <div class="evo-stage-arrow">➜</div>
                <div class="evo-stage-card" id="stage-export">
                    <div style="font-size:1.2em;">📦</div>
                    <div style="font-size:0.7em; font-weight:bold;">GGUF Export</div>
                    <div style="font-size:0.6em; color:#888;">Ollama/Local</div>
                </div>
            </div>

            <div id="evo-display-area" style="margin-top:25px; padding:15px; border:1px solid #ddd; border-radius:8px; background:#f9f9f9; min-height:150px; text-align:left;">
                <div style="font-weight:bold; margin-bottom:10px; font-size:0.8em; color:#888;" id="evo-display-title">LIFECYCLE STATUS</div>
                <div id="evo-display-content" style="font-size:0.9em; line-height:1.4;">
                    Select a stage above to see details or click 'Run Lifecycle' to simulate the process.
                </div>
            </div>

            <div style="margin-top:20px; display:flex; justify-content:center; gap:10px;">
                <button class="btn btn-primary" onclick="runLlmEvoLifecycle()">Run Lifecycle</button>
                <button class="btn" onclick="resetLlmEvoLifecycle()">Reset</button>
            </div>
        </div>
    `;

    document.getElementById('stage-data').onclick = () => showEvoStage('data');
    document.getElementById('stage-tokenizer').onclick = () => showEvoStage('tokenizer');
    document.getElementById('stage-transformer').onclick = () => showEvoStage('transformer');
    document.getElementById('stage-export').onclick = () => showEvoStage('export');
};

// Aliasing for backward compatibility if needed, but we will update forge.html
window.renderInteractiveLlmDemo = window.renderInteractiveLlmEvoDemo;

let evoInterval = null;
window.runLlmEvoLifecycle = async function() {
    resetLlmEvoLifecycle();
    const stages = ['data', 'tokenizer', 'transformer', 'export'];
    let i = 0;

    const nextStage = () => {
        if (i >= stages.length) {
            clearInterval(evoInterval);
            return;
        }
        showEvoStage(stages[i]);
        i++;
    };

    nextStage();
    evoInterval = setInterval(nextStage, 2000);
};

window.resetLlmEvoLifecycle = function() {
    if (evoInterval) clearInterval(evoInterval);
    document.querySelectorAll('.evo-stage-card').forEach(c => c.classList.remove('active'));
    document.getElementById('evo-display-title').textContent = 'LIFECYCLE STATUS';
    document.getElementById('evo-display-content').textContent = "Select a stage above to see details or click 'Run Lifecycle' to simulate the process.";
};

function showEvoStage(stage) {
    document.querySelectorAll('.evo-stage-card').forEach(c => c.classList.remove('active'));
    document.getElementById('stage-' + stage).classList.add('active');

    const title = document.getElementById('evo-display-title');
    const content = document.getElementById('evo-display-content');

    switch(stage) {
        case 'data':
            title.textContent = 'DATA LOADING & CLEANING';
            content.innerHTML = `
                <div style="font-family:monospace; color:#444; background:#eee; padding:8px; border-radius:4px; font-size:0.8em;">
                    # Project Readme<br>
                    This is a sample project for evolution.<br>
                    ---<br>
                    Scanning: 12 files discovered.<br>
                    Filtering: .gitignore applied.
                </div>
                <div style="margin-top:10px; font-size:0.85em;">Discovered information carriers and normalized Markdown content for training.</div>
            `;
            break;
        case 'tokenizer':
            title.textContent = 'TOKENIZATION (SimpleBPE)';
            content.innerHTML = `
                <div style="display:flex; flex-wrap:wrap; gap:4px;">
                    <span style="background:#e8f0fe; padding:2px 4px; border-radius:3px;">The</span>
                    <span style="background:#e8f0fe; padding:2px 4px; border-radius:3px;">quick</span>
                    <span style="background:#e8f0fe; padding:2px 4px; border-radius:3px;">brown</span>
                    <span style="background:#e8f0fe; padding:2px 4px; border-radius:3px;">fox</span>
                </div>
                <div style="margin-top:10px; font-size:0.85em;">Converting text to numerical indices using a Byte Pair Encoding vocabulary.</div>
            `;
            break;
        case 'transformer':
            title.textContent = 'TRANSFORMER ARCHITECTURE';
            content.innerHTML = `
                <ul style="font-size:0.85em; padding-left:20px;">
                    <li><b>Embedding:</b> Continuous vector representation.</li>
                    <li><b>Attention:</b> Multi-head self-attention mechanisms.</li>
                    <li><b>FFN:</b> Feed-forward neural networks.</li>
                    <li><b>LayerNorm:</b> Maintaining activation stability.</li>
                </ul>
                <div style="margin-top:5px; font-style:italic; font-size:0.8em; color:var(--accent);">Forward pass executing...</div>
            `;
            break;
        case 'export':
            title.textContent = 'GGUF EXPORT & DEPLOYMENT';
            content.innerHTML = `
                <div style="background:#333; color:#89d185; padding:10px; border-radius:4px; font-family:monospace; font-size:0.8em;">
                    > Exporting LoRA adapters...<br>
                    > Merging weights...<br>
                    > Quantizing to Q4_K_M...<br>
                    > Registering 'evo-model' in Ollama.<br>
                    SUCCESS: Model ready at localhost:11434
                </div>
            `;
            break;
    }
}

let llmTokens = ["The", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog"];
let llmIndex = 5;

window.stepLlmDemo = function() {
    if (llmIndex >= llmTokens.length) llmIndex = 0;

    const context = document.getElementById('llm-context');
    if (!context) return;
    let html = "";
    for(let i=0; i<llmIndex; i++) {
        html += llmTokens[i] + " ";
    }
    html += `<span style="background:#fff9c4; border-bottom:2px solid #fbc02d; padding:2px 4px; border-radius:3px;">${llmTokens[llmIndex]}</span>`;
    context.innerHTML = html;

    llmIndex++;
    updateLlmCandidates();
};

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

window.renderInteractiveCnnDemo = function() {
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
};

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

// ============== INTERACTIVE MLP DEMO ==============

let mlpW1=[], mlpW2=[], mlpb1=[], mlpb2=[];
let mlpH=3;

window.renderInteractiveMlpDemo = function() {
    const area = document.getElementById('viz-area');
    area.innerHTML = `
        <div id="demo-mlp-container" style="padding:20px; text-align:center; height:100%; width:100%; background:#1f232b; color:white; overflow:auto;">
            <h2 style="margin:0 0 10px 0;">Mini MLP (Feedforward Neural Network)</h2>
            <div id="demo-mlp-net" style="text-align:center; padding:20px;"></div>
            <div style="display:flex; gap:15px; padding:15px; text-align:left;">
                <div style="flex:1; background:#2d3340; padding:15px; border-radius:8px;">
                    <h3 style="margin-top:0;">Input</h3>
                    x1 <input id="demo-mlp-x1" type="number" value="1" style="width:100%; margin:5px 0 10px; background:#fff; color:#000; padding:4px; box-sizing:border-box;">
                    x2 <input id="demo-mlp-x2" type="number" value="0" style="width:100%; margin:5px 0 10px; background:#fff; color:#000; padding:4px; box-sizing:border-box;">
                    <button class="btn btn-primary" onclick="forwardMlp()" style="width:100%;">Forward</button>
                    <h3 style="margin-top:15px; margin-bottom:5px;">Output</h3>
                    <div id="demo-mlp-out" style="font-size:1.2em; font-weight:bold; color:#4CAF50;">-</div>
                </div>
                <div style="flex:1; background:#2d3340; padding:15px; border-radius:8px;">
                    <h3 style="margin-top:0;">Model</h3>
                    Hidden neurons:
                    <input id="demo-mlp-hidden" type="number" value="3" style="width:100%; margin:5px 0 10px; background:#fff; color:#000; padding:4px; box-sizing:border-box;">
                    <button class="btn" onclick="buildMlp()" style="width:100%;">Rebuild Network</button>
                    <p style="font-size:0.8em; margin-top:10px; color:#aaa;">Activation: Sigmoid</p>
                </div>
            </div>
            <style>
                .demo-mlp-node {
                    width:40px; height:40px; border-radius:50%; background:#444;
                    display:flex; align-items:center; justify-content:center;
                    margin:10px auto; transition:.3s; font-size:0.8em;
                }
                .demo-mlp-node.active { background:#4CAF50; box-shadow: 0 0 10px #4CAF50; }
                .demo-mlp-layer { display:inline-block; margin:0 20px; vertical-align:top; }
            </style>
        </div>
    `;
    buildMlp();
};

function mlpSigmoid(x){ return 1/(1+Math.exp(-x)); }
function mlpRand(){ return Math.random()*2-1; }

window.buildMlp = function() {
    const hiddenInput = document.getElementById('demo-mlp-hidden');
    if (!hiddenInput) return;
    mlpH = parseInt(hiddenInput.value);
    mlpW1=[]; mlpb1=[]; mlpW2=[]; 
    for(let i=0; i<mlpH; i++){
        mlpW1[i]=[mlpRand(), mlpRand()];
        mlpb1[i]=mlpRand();
        mlpW2[i]=mlpRand();
    }
    mlpb2=mlpRand();
    renderMlp();
};

function renderMlp() {
    const net = document.getElementById('demo-mlp-net');
    if (!net) return;
    let html = "";
    html += "<div class='demo-mlp-layer'><div>Input</div>";
    html += "<div class='demo-mlp-node'>x1</div>";
    html += "<div class='demo-mlp-node'>x2</div></div>";
    html += "<div class='demo-mlp-layer'><div>Hidden</div>";
    for(let i=0; i<mlpH; i++){ html += "<div class='demo-mlp-node'>h"+i+"</div>"; }
    html += "</div>";
    html += "<div class='demo-mlp-layer'><div>Output</div><div class='demo-mlp-node'>y</div></div>";

    const params = (2 * mlpH) + mlpH + (mlpH * 1) + 1; // input->hidden + hidden bias + hidden->out + out bias
    html += `<div style="font-size: 0.7em; color: #aaa; margin-top: 10px;">Total Parameters: ${params}</div>`;

    net.innerHTML = html;
}

window.forwardMlp = function() {
    const x1Val = parseFloat(document.getElementById('demo-mlp-x1').value);
    const x2Val = parseFloat(document.getElementById('demo-mlp-x2').value);
    let h = [];
    for(let i=0; i<mlpH; i++){
        let z = mlpW1[i][0]*x1Val + mlpW1[i][1]*x2Val + mlpb1[i];
        h[i] = mlpSigmoid(z);
    }
    let y = 0;
    for(let i=0; i<mlpH; i++){ y += h[i]*mlpW2[i]; }
    y += mlpb2;
    y = mlpSigmoid(y);
    const nodes = document.querySelectorAll(".demo-mlp-node");
    nodes.forEach(n => n.classList.add("active"));
    setTimeout(() => { nodes.forEach(n => n.classList.remove("active")); }, 300);
    document.getElementById('demo-mlp-out').innerText = y.toFixed(4);
};

// ============== INTERACTIVE TRANSFORMER EVO DEMO ==============

window.renderInteractiveTransformerEvoDemo = function() {
    const area = document.getElementById('viz-area');
    area.innerHTML = `
        <div id="demo-tf-container" style="padding:20px; text-align:center; height:100%; width:100%; background:#20242b; color:white; overflow:auto;">
            <h2 style="margin:0 0 10px 0;">LLM-EVO: Transformer Architecture</h2>
            <div id="demo-tf-pipeline" style="display:flex; justify-content:center; align-items:center; gap:12px; padding:20px; flex-wrap:wrap;"></div>
            <div style="display:flex; gap:15px; padding:15px; text-align:left;">
                <div style="flex:1; background:#2d323b; border-radius:8px; padding:15px;">
                    <h3 style="margin-top:0;">Input Tokens</h3>
                    <textarea id="demo-tf-prompt" rows="4" style="width:100%; box-sizing:border-box; background:#fff; color:#000; padding:8px; border-radius:4px; font-family:inherit;">The quick brown fox</textarea>
                    <button class="btn btn-primary" onclick="runTransformer()" style="margin-top:10px; width:100%;">Run Forward Pass</button>
                    <div id="demo-tf-tokens" style="margin-top:15px; min-height:40px; border:1px solid #444; padding:5px; border-radius:4px; background:#1a1d23;"></div>
                </div>
                <div style="flex:1; background:#2d323b; border-radius:8px; padding:15px;">
                    <h3 style="margin-top:0;">Hyperparameters</h3>
                    <div style="margin-bottom:10px;">
                        <div style="font-size:0.8em; color:#aaa; margin-bottom:4px;">Model Dimension (d_model)</div>
                        <input id="demo-tf-hidden" type="number" value="256" style="width:100%; background:#fff; color:#000; padding:4px; box-sizing:border-box;">
                    </div>
                    <div style="margin-bottom:10px;">
                        <div style="font-size:0.8em; color:#aaa; margin-bottom:4px;">Attention Heads</div>
                        <input id="demo-tf-heads" type="number" value="4" style="width:100%; background:#fff; color:#000; padding:4px; box-sizing:border-box;">
                    </div>
                    <button class="btn" onclick="buildTransformer()" style="width:100%;">Apply Architecture</button>
                    <h3 style="margin-top:15px;">Execution Output</h3>
                    <div id="demo-tf-output" style="margin-top:10px; font-style:italic; color:#ff9800;">-</div>
                </div>
            </div>
            <style>
                .demo-tf-block {
                    width:110px; height:50px; background:#394150; border-radius:8px;
                    display:flex; justify-content:center; align-items:center;
                    text-align:center; transition:.3s; font-size:0.75em; font-weight:bold;
                    border: 1px solid #555; cursor:pointer;
                }
                .demo-tf-block:hover { border-color: var(--accent); }
                .demo-tf-block.active { background:#4CAF50; transform:scale(1.1); box-shadow: 0 0 10px #4CAF50; border-color:white; }
                .demo-tf-arrow { font-size:18px; color:#888; }
                .demo-tf-token {
                    display:inline-block; padding:4px 8px; margin:4px;
                    border-radius:4px; background:#444; transition:.3s; font-size:0.8em;
                    border: 1px solid #555;
                }
                .demo-tf-token.active { background:#ff9800; color:black; border-color:white; }
            </style>
        </div>
    `;
    buildTransformer();
};

// Aliasing for backward compatibility
window.renderInteractiveTransformerDemo = window.renderInteractiveTransformerEvoDemo;

function tfBlock(name, desc){ 
    return `<div class='demo-tf-block' onclick='alert("${desc}")'>${name}</div>`; 
}
function tfArrow(){ return "<div class='demo-tf-arrow'>➜</div>"; }

window.buildTransformer = function() {
    const pipeline = document.getElementById('demo-tf-pipeline');
    if (!pipeline) return;
    let html="";
    html+=tfBlock("Embedding", "Converts token IDs into continuous vectors."); html+=tfArrow();
    html+=tfBlock("Attention", "Computes context-aware representations by looking at other tokens."); html+=tfArrow();
    html+=tfBlock("FFN", "Applies non-linear transformations to each position independently."); html+=tfArrow();
    html+=tfBlock("LayerNorm", "Normalizes activations to improve training stability."); html+=tfArrow();
    html+=tfBlock("Output", "Final linear layer and softmax for token prediction.");
    pipeline.innerHTML=html;
};

window.runTransformer = async function() {
    const prompt = document.getElementById('demo-tf-prompt');
    const tokens = document.getElementById('demo-tf-tokens');
    const output = document.getElementById('demo-tf-output');
    if (!prompt || !tokens || !output) return;

    let words = prompt.value.trim().split(/\s+/);
    tokens.innerHTML="";
    for(let w of words){
        const span = document.createElement("span");
        span.className = "demo-tf-token";
        span.textContent = w;
        tokens.appendChild(span);
    }

    let blocks = document.querySelectorAll(".demo-tf-block");
    for(let b of blocks){
        b.classList.add("active");
        let t = document.querySelectorAll(".demo-tf-token");
        for(let x of t){ x.classList.add("active"); }
        await new Promise(r => setTimeout(r, 400));
        b.classList.remove("active");
        for(let x of t){ x.classList.remove("active"); }
    }
    output.textContent = "Generated: " + prompt.value + " ...";
};

})();
