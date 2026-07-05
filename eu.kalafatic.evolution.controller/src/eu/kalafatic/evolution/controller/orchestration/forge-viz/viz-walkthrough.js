// ============== WALKTHROUGH DEMO LOGIC ==============
let walkthroughNeurons = [];
let walkthroughConnections = [];
let walkthroughAnimationFrame = null;
let isStudyMode = false;
let walkthroughStep = 0;

class WalkthroughNeuron {
    constructor(x, y, layer) {
        this.x = x;
        this.y = y;
        this.layer = layer;
        this.activation = 0;
        this.targetActivation = 0;
    }
}

window.renderWalkthroughViz = function() {
    const area = document.getElementById('viz-area');
    area.innerHTML = `
        <div style="display:flex; flex-direction:column; align-items:center; width:100%;">
            <canvas id="walkthrough-canvas" width="800" height="400" style="border:1px solid var(--border); background:#111; box-shadow: 0 0 10px rgba(0,255,0,0.2);"></canvas>
            <div id="walkthrough-info" class="data-block" style="width:90%; margin-top:10px; min-height:50px;">
                Click "Step" or "Play" to begin the neural network evolution walkthrough.
            </div>
            <div id="study-messages" style="display:${isStudyMode ? 'block' : 'none'}; width:90%; margin-top:10px;">
                <textarea id="study-msg-edit" class="control-group" style="width:100%; height:60px; font-size:0.8em;" onchange="saveStudyMessage(this.value)"></textarea>
            </div>
        </div>
    `;

    drawWalkthrough();
    updateStudyMessageUI();
}

function saveStudyMessage(val) {
    if (!activeSessionId) return;
    const messages = getStudyMessages();
    messages[walkthroughStep] = val;
    persistUiState('walkthrough_messages', JSON.stringify(messages));

    const info = document.getElementById('walkthrough-info');
    if (info) info.innerHTML = val;
}

function getStudyMessages() {
    const session = sessions.find(s => s.id === activeSessionId);
    const saved = session && session.uiState && session.uiState.walkthrough_messages;
    if (saved) {
        try { return JSON.parse(saved); } catch(e) { return {}; }
    }
    return {
        0: "Welcome to the Neural Network Walkthrough. Here we see how information flows and evolves.",
        1: "STEP 1: ARCHITECTURE. We define layers of neurons. Input layer receives data, Hidden layers process features, Output layer gives predictions.",
        2: "STEP 2: FORWARD PASS. Data flows from left to right. Each neuron calculates a weighted sum of its inputs and applies an activation function.",
        3: "STEP 3: TRAINING. We compare output to target and adjust weights. Notice how signals propagate across multiple hidden layers.",
        4: "STEP 4: GENERATION. Once trained, the network can predict the next token in a sequence, effectively 'thinking'."
    };
}

function updateStudyMessageUI() {
    const info = document.getElementById('walkthrough-info');
    const edit = document.getElementById('study-msg-edit');
    const messages = getStudyMessages();
    const msg = messages[walkthroughStep] || "Proceed to the next step...";

    if (info) info.innerHTML = msg;
    if (edit) edit.value = msg;
}

function drawWalkthrough() {
    const canvas = document.getElementById('walkthrough-canvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Draw connections
    walkthroughConnections.forEach(c => {
        const from = walkthroughNeurons[c.from];
        const to = walkthroughNeurons[c.to];
        const intensity = Math.abs(c.weight) * 0.8 + 0.2;
        ctx.strokeStyle = to.activation > 0.1 ? `rgba(0, 255, 100, ${intensity})` : '#334';
        ctx.lineWidth = Math.abs(c.weight) * 3 + 1;
        ctx.beginPath();
        ctx.moveTo(from.x, from.y);
        ctx.lineTo(to.x, to.y);
        ctx.stroke();
    });

    // Draw neurons
    walkthroughNeurons.forEach((n, i) => {
        const size = 15;
        ctx.fillStyle = n.activation > 0.3 ? '#0f0' : '#4488ff';
        ctx.beginPath();
        ctx.arc(n.x, n.y, size, 0, Math.PI * 2);
        ctx.fill();

        ctx.fillStyle = '#fff';
        ctx.font = '8px monospace';
        ctx.textAlign = 'center';
        ctx.fillText(n.activation.toFixed(1), n.x, n.y + 3);
    });
}

function walkthroughStep1Create() {
    if (walkthroughAnimationFrame) cancelAnimationFrame(walkthroughAnimationFrame);
    walkthroughNeurons = [];
    walkthroughConnections = [];
    walkthroughStep = 1;

    const layers = [3, 5, 4, 2]; // Multiple hidden layers
    const xStep = 200;
    const canvas = document.getElementById('walkthrough-canvas');
    const width = canvas ? canvas.width : 800;
    const height = canvas ? canvas.height : 400;

    layers.forEach((count, lIdx) => {
        const x = 100 + lIdx * xStep;
        for (let i = 0; i < count; i++) {
            const y = (height / (count + 1)) * (i + 1);
            walkthroughNeurons.push(new WalkthroughNeuron(x, y, lIdx));
        }
    });

    for (let i = 0; i < walkthroughNeurons.length; i++) {
        const n = walkthroughNeurons[i];
        if (n.layer === 0) continue;
        for (let j = 0; j < walkthroughNeurons.length; j++) {
            if (walkthroughNeurons[j].layer === n.layer - 1) {
                walkthroughConnections.push({
                    from: j, to: i, weight: Math.random() * 2 - 1
                });
            }
        }
    }
    drawWalkthrough();
    updateStudyMessageUI();
}

function walkthroughStep2Forward() {
    if (walkthroughNeurons.length === 0) walkthroughStep1Create();
    walkthroughStep = 2;

    // Input values
    walkthroughNeurons.filter(n => n.layer === 0).forEach(n => n.activation = Math.random());

    let currentLayer = 1;
    const animate = () => {
        const layerNeurons = walkthroughNeurons.filter(n => n.layer === currentLayer);
        layerNeurons.forEach(n => {
            let sum = 0;
            walkthroughConnections.filter(c => walkthroughNeurons[c.to] === n).forEach(c => {
                sum += walkthroughNeurons[c.from].activation * c.weight;
            });
            n.activation = 1 / (1 + Math.exp(-sum));
        });

        drawWalkthrough();
        currentLayer++;
        if (currentLayer < 4) {
            setTimeout(animate, 500);
        } else {
            updateStudyMessageUI();
        }
    };
    animate();
}

function walkthroughStep3Train() {
    if (walkthroughNeurons.length === 0) walkthroughStep1Create();
    walkthroughStep = 3;

    // Nudge weights
    walkthroughConnections.forEach(c => {
        c.weight += (Math.random() - 0.5) * 0.4;
    });

    walkthroughStep2Forward();
    updateStudyMessageUI();
}

function walkthroughStep4Generate() {
    walkthroughStep = 4;
    updateStudyMessageUI();
    const info = document.getElementById('walkthrough-info');
    const responses = [
        "Neural Evolution: Success. Model converged.",
        "Cognition level rising. Patterns identified.",
        "Intelligence synthesized. Ready for deployment."
    ];
    info.innerHTML += `<br><br><b style="color:var(--accent)">LLM OUTPUT:</b> ${responses[Math.floor(Math.random()*responses.length)]}`;
}

async function startWalkthroughAutoPlay() {
    log("Starting automated walkthrough sequence...");
    const delay = 3000;

    walkthroughStep1Create();
    await new Promise(r => setTimeout(r, delay));

    walkthroughStep2Forward();
    await new Promise(r => setTimeout(r, delay + 2000)); // Extra time for signal propagation

    walkthroughStep3Train();
    await new Promise(r => setTimeout(r, delay + 2000));

    walkthroughStep4Generate();
    log("Automated walkthrough complete.");
}
