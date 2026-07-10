/**
 * Visualization for Self-Evo Forging process.
 */
(function() {
    window.renderSelfEvoViz = function(stats) {
        const area = document.getElementById('viz-area');
        const details = document.getElementById('viz-details-content');

        const progress = stats ? (stats.progress || 0) : 0;
        const status = stats ? (stats.status || 'IDLE') : 'IDLE';

        area.innerHTML = `
            <div style="padding:20px; text-align:center; height:100%; width:100%; background:#fff; color:#333; overflow-y:auto;">
                <h3 style="color:var(--accent)">Self-Evo Forging: Live Pipeline</h3>
                <p style="font-size:0.9em; color:#666;">Autonomous codebase analysis and project-specific LLM generation.</p>

                <div style="display:flex; justify-content:center; gap:10px; margin-top:20px; flex-wrap:wrap;">
                    <div class="evo-stage-card ${status === 'SCANNING' ? 'active' : (status !== 'IDLE' && status !== 'STARTING' ? 'completed' : '')}" id="stage-scan">
                        <div style="font-size:1.2em;">🔍</div>
                        <div style="font-size:0.7em; font-weight:bold;">Scanner</div>
                        <div style="font-size:0.6em; color:#888;">Code Discovery</div>
                    </div>
                    <div class="evo-stage-arrow">➜</div>
                    <div class="evo-stage-card ${status === 'ENHANCING' ? 'active' : (['TRAINING', 'EXPORT_GGUF', 'COMPLETE'].includes(status) ? 'completed' : '')}" id="stage-enhance">
                        <div style="font-size:1.2em;">🪄</div>
                        <div style="font-size:0.7em; font-weight:bold;">Enhancer</div>
                        <div style="font-size:0.6em; color:#888;">Synthetic Data</div>
                    </div>
                    <div class="evo-stage-arrow">➜</div>
                    <div class="evo-stage-card ${status === 'TRAINING' ? 'active' : (['EXPORT_GGUF', 'COMPLETE'].includes(status) ? 'completed' : '')}" id="stage-train">
                        <div style="font-size:1.2em;">⚙️</div>
                        <div style="font-size:0.7em; font-weight:bold;">Trainer</div>
                        <div style="font-size:0.6em; color:#888;">Fine-Tuning</div>
                    </div>
                    <div class="evo-stage-arrow">➜</div>
                    <div class="evo-stage-card ${status === 'EXPORT_GGUF' || status === 'EXPORTING' ? 'active' : (status === 'COMPLETE' ? 'completed' : '')}" id="stage-export">
                        <div style="font-size:1.2em;">📦</div>
                        <div style="font-size:0.7em; font-weight:bold;">Deployment</div>
                        <div style="font-size:0.6em; color:#888;">Ollama GGUF</div>
                    </div>
                </div>

                <div style="width:100%; max-width:500px; height:12px; background:#eee; border-radius:6px; overflow:hidden; margin:25px auto 10px; border: 1px solid #ddd;">
                    <div style="width:${progress}%; height:100%; background:var(--accent); transition:width 0.5s; position:relative;">
                        ${status !== 'IDLE' && status !== 'COMPLETE' ? '<div class="progress-glow"></div>' : ''}
                    </div>
                </div>
                <p style="font-size:0.75em; color:var(--text-dim); font-weight:bold;">${progress}% - ${status}</p>

                <div id="forging-display-area" style="margin-top:15px; padding:15px; border:1px solid #ddd; border-radius:8px; background:#f9f9f9; min-height:140px; text-align:left; max-width:600px; margin-left:auto; margin-right:auto; font-family: monospace;">
                    <div style="font-weight:bold; margin-bottom:10px; font-size:0.8em; color:#888; font-family: sans-serif;">PIPELINE TELEMETRY</div>
                    <div id="forging-display-content" style="font-size:0.85em; line-height:1.4; color: #444;">
                        ${renderTelemetry(stats || { status: 'IDLE' })}
                    </div>
                </div>

                <div style="margin-top:20px; display: flex; justify-content: center; gap: 10px;">
                    <button class="btn btn-primary" onclick="startForging()" ${status !== 'IDLE' && status !== 'COMPLETE' && status !== 'ERROR' ? 'disabled' : ''} style="padding: 10px 25px; font-weight: bold;">
                        ${status === 'IDLE' ? '🚀 Start Forging Process' : '🔄 Restart Forging'}
                    </button>
                    ${status === 'COMPLETE' ? `
                        <button class="btn" onclick="alert('Model ready in Ollama!')">Check Model</button>
                        <button class="btn" onclick="openModelFolder()"><span style="margin-right:4px;">📁</span>Locate Model</button>
                    ` : ''}
                </div>
            </div>
        `;

        if (details) {
            details.innerHTML = `
                <h4>Pipeline Status</h4>
                <div style="font-size: 1.1em; font-weight: bold; color: ${statusColor(status)}; margin-bottom: 10px;">${status}</div>
                <p style="font-size: 0.85em; color: #666;">${getPhaseDescription(status)}</p>
                <hr/>
                <h4 style="font-size: 0.85em; color: #888; text-transform: uppercase;">Metrics</h4>
                <div style="font-size:0.9em; display: flex; flex-direction: column; gap: 5px;">
                    <div style="display: flex; justify-content: space-between;"><span>Files Scanned:</span> <b>${stats ? (stats.filesScanned || 0) : 0}</b></div>
                    <div style="display: flex; justify-content: space-between;"><span>Instructions:</span> <b>${stats ? (stats.instructionsGenerated || 0) : 0}</b></div>
                    <div style="display: flex; justify-content: space-between;"><span>Current Loss:</span> <b>${stats && typeof stats.currentLoss === 'number' ? stats.currentLoss.toFixed(4) : (stats ? stats.currentLoss : '0.0000')}</b></div>
                    <div style="display: flex; justify-content: space-between;"><span>Epoch:</span> <b>${stats ? (stats.currentEpoch || '0') : '0'}</b></div>
                </div>
                ${status === 'COMPLETE' ? `
                    <div style="margin-top: 15px; padding: 10px; background: #f6fff6; border: 1px solid var(--success); border-radius: 4px; font-size: 0.8em; color: var(--success);">
                        <b>Deployment Success:</b> The model has been registered in Ollama as 'evo'.
                    </div>
                ` : ''}
            `;
        }
    };

    function renderTelemetry(stats) {
        const status = stats.status;
        switch(status) {
            case 'SCANNING':
                return `Discovering files in project root...<br/>Found: ${stats.filesScanned || 0} source files.`;
            case 'ENHANCING':
                return `Generating synthetic instructions using local model...<br/>Progress: ${stats.instructionsGenerated || 0} pairs created.`;
            case 'TRAINING':
                return `Running Unsloth fine-tuning...<br/>Loss: ${stats.currentLoss || '---'}<br/>Epoch: ${stats.currentEpoch || 0}`;
            case 'EXPORT_GGUF':
                return `Converting LoRA adapters to GGUF format...<br/>Preparing manifest for Ollama registration.`;
            case 'COMPLETE':
                return `<b style="color:var(--success)">Forging Complete!</b><br/>Model 'evo' is now available in Ollama.`;
            case 'ERROR':
                return `<b style="color:var(--error)">Pipeline Failed</b><br/>Check console logs for details.`;
            default:
                return `Pipeline is ready. Click 'Start' to begin codebase analysis and model forging.`;
        }
    }

    function statusColor(status) {
        switch(status) {
            case 'IDLE': return 'var(--text-dim)';
            case 'SCANNING': return 'var(--accent)';
            case 'ENHANCING': return '#c586c0';
            case 'TRAINING': return '#dcdcaa';
            case 'EXPORTING':
            case 'EXPORT_GGUF': return '#4fc1ff';
            case 'COMPLETE': return 'var(--success)';
            case 'ERROR': return 'var(--error)';
            default: return 'var(--text)';
        }
    }

    function getPhaseDescription(status) {
        switch(status) {
            case 'IDLE': return 'System ready to start project-specific model creation.';
            case 'SCANNING': return 'Crawling project directory and applying .gitignore filters.';
            case 'ENHANCING': return 'Using LLM to generate synthetic programming instructions from discovered code.';
            case 'TRAINING': return 'Running Axolotl/Unsloth fine-tuning on the prepared dataset.';
            case 'EXPORTING':
            case 'EXPORT_GGUF': return 'Converting LoRA adapters to GGUF format for Ollama.';
            case 'COMPLETE': return 'Custom "evo" model is ready and registered in Ollama.';
            default: return 'Unknown phase.';
        }
    }

    window.startForging = async function() {
        log("Initiating Self-Evo forging...");
        try {
            const res = await fetch(getBaseUrl() + `/forge/session/${activeSessionId}/forging/start?runtime=SWT`, { method: 'POST' });
            if (res.ok) {
                log("Forging process started successfully.");
                if (!vizAutoRefresh) {
                    vizAutoRefresh = true;
                    document.getElementById('viz-selector').value = 'SELF_EVO';
                    if (window.setupAutoRefresh) window.setupAutoRefresh();
                }
            } else {
                throw new Error("Failed to start forging process");
            }
        } catch (e) { log("Start forging error: " + e.message); }
    };

    window.openModelFolder = async function() {
        log("Opening model folder on host...");
        try {
            const res = await fetch(getBaseUrl() + `/forge/session/${activeSessionId}/open-folder?runtime=SWT`, { method: 'POST' });
            if (res.ok) {
                log("Model folder opened successfully.");
            } else {
                throw new Error("Failed to open model folder");
            }
        } catch (e) { log("Open folder error: " + e.message); }
    };

    // ============== INTERACTIVE SELF-EVO DEMO (SIMULATION) ==============

    window.renderInteractiveSelfEvoDemo = function() {
        const area = document.getElementById('viz-area');
        area.innerHTML = `
            <div style="padding:20px; text-align:center; height:100%; width:100%; background:#fff; color:#333; overflow-y:auto;">
                <h3 style="color:var(--accent)">Self-Evo: Forging Simulation</h3>
                <p style="font-size:0.9em; color:#666;">Experience the autonomous model creation pipeline.</p>

                <div style="display:flex; justify-content:center; gap:10px; margin-top:20px; flex-wrap:wrap;">
                    <div class="evo-stage-card" id="stage-scan">
                        <div style="font-size:1.2em;">🔍</div>
                        <div style="font-size:0.7em; font-weight:bold;">Scanner</div>
                        <div style="font-size:0.6em; color:#888;">Code Discovery</div>
                    </div>
                    <div class="evo-stage-arrow">➜</div>
                    <div class="evo-stage-card" id="stage-enhance">
                        <div style="font-size:1.2em;">🪄</div>
                        <div style="font-size:0.7em; font-weight:bold;">Enhancer</div>
                        <div style="font-size:0.6em; color:#888;">Synthetic Data</div>
                    </div>
                    <div class="evo-stage-arrow">➜</div>
                    <div class="evo-stage-card" id="stage-train">
                        <div style="font-size:1.2em;">⚙️</div>
                        <div style="font-size:0.7em; font-weight:bold;">Trainer</div>
                        <div style="font-size:0.6em; color:#888;">Fine-Tuning</div>
                    </div>
                    <div class="evo-stage-arrow">➜</div>
                    <div class="evo-stage-card" id="stage-export">
                        <div style="font-size:1.2em;">📦</div>
                        <div style="font-size:0.7em; font-weight:bold;">Deployment</div>
                        <div style="font-size:0.6em; color:#888;">Ollama GGUF</div>
                    </div>
                </div>

                <div id="evo-display-area" style="margin-top:25px; padding:15px; border:1px solid #ddd; border-radius:8px; background:#f9f9f9; min-height:150px; text-align:left; max-width:600px; margin-left:auto; margin-right:auto;">
                    <div style="font-weight:bold; margin-bottom:10px; font-size:0.8em; color:#888;" id="evo-display-title">LIFECYCLE STATUS</div>
                    <div id="evo-display-content" style="font-size:0.9em; line-height:1.4;">
                        Click 'Run Simulation' to see how the Self-Evo pipeline works.
                    </div>
                </div>

                <div style="margin-top:20px; display:flex; justify-content:center; gap:10px;">
                    <button class="btn btn-primary" onclick="runSelfEvoSimulation()">Run Simulation</button>
                    <button class="btn" onclick="resetSelfEvoSimulation()">Reset</button>
                </div>
            </div>
        `;
    };

    let selfEvoInterval = null;
    window.runSelfEvoSimulation = function() {
        resetSelfEvoSimulation();
        const stages = ['scan', 'enhance', 'train', 'export'];
        let i = 0;

        selfEvoInterval = setInterval(() => {
            if (i >= stages.length) {
                clearInterval(selfEvoInterval);
                return;
            }
            showSelfEvoStage(stages[i]);
            i++;
        }, 2500);
    };

    window.resetSelfEvoSimulation = function() {
        if (selfEvoInterval) clearInterval(selfEvoInterval);
        document.querySelectorAll('.evo-stage-card').forEach(c => c.classList.remove('active', 'completed'));
        document.getElementById('evo-display-title').textContent = 'LIFECYCLE STATUS';
        document.getElementById('evo-display-content').textContent = "Click 'Run Simulation' to see how the Self-Evo pipeline works.";
    };

    function showSelfEvoStage(stage) {
        document.querySelectorAll('.evo-stage-card').forEach(c => c.classList.remove('active'));
        const el = document.getElementById('stage-' + stage);
        if(el) el.classList.add('active');

        // Mark previous as completed
        if (stage === 'enhance') document.getElementById('stage-scan').classList.add('completed');
        if (stage === 'train') document.getElementById('stage-enhance').classList.add('completed');
        if (stage === 'export') document.getElementById('stage-train').classList.add('completed');

        const title = document.getElementById('evo-display-title');
        const content = document.getElementById('evo-display-content');

        switch(stage) {
            case 'scan':
                title.textContent = 'STAGE 1: CODEBASE SCANNING';
                content.innerHTML = `
                    <div style="font-family:monospace; font-size:0.85em; color:#444;">
                        > Scanning /project/src...<br>
                        > Found 142 source files (.java, .js, .py)<br>
                        > Respecting .gitignore: skipped 23 files.<br>
                        > Discovering architecture patterns...
                    </div>
                `;
                break;
            case 'enhance':
                title.textContent = 'STAGE 2: DATA ENHANCEMENT';
                content.innerHTML = `
                    <div style="font-family:monospace; font-size:0.85em; color:#444;">
                        > Extracting snippets for instruction generation...<br>
                        > Using local LLM to create synthetic Q&A pairs...<br>
                        > Generated 1,250 training instructions.<br>
                        > Preparing JSONL dataset for fine-tuning.
                    </div>
                `;
                break;
            case 'train':
                title.textContent = 'STAGE 3: FINE-TUNING (UNSLOTH)';
                content.innerHTML = `
                    <div style="font-family:monospace; font-size:0.85em; color:#444;">
                        > Initializing LoRA adapters...<br>
                        > Training on 4-bit quantized base model...<br>
                        > Loss: 1.2435 (Epoch 0.2)<br>
                        > Loss: 0.8211 (Epoch 0.5)<br>
                        > Loss: 0.4567 (Epoch 1.0)
                    </div>
                `;
                break;
            case 'export':
                title.textContent = 'STAGE 4: GGUF EXPORT & OLLAMA';
                content.innerHTML = `
                    <div style="font-family:monospace; font-size:0.85em; color:#444;">
                        > Merging LoRA weights...<br>
                        > Exporting to GGUF format...<br>
                        > Registering model 'evo' in Ollama.<br>
                        > <b style="color:var(--success)">SUCCESS:</b> Model ready for deployment.
                    </div>
                `;
                if(el) el.classList.add('completed');
                break;
        }
    }
})();
