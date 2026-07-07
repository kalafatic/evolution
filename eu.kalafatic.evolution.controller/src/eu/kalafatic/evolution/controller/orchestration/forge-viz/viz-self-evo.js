/**
 * Visualization for Self-Evo Forging process.
 */
(function() {
    window.renderSelfEvoViz = function(stats) {
        const area = document.getElementById('viz-area');
        const details = document.getElementById('viz-details-content');

        if (!stats) {
            area.innerHTML = '<div style="color:var(--text-dim)">No forging stats available.</div>';
            return;
        }

        const progress = stats ? (stats.progress || 0) : 0;
        const status = stats ? (stats.status || 'IDLE') : 'IDLE';

        area.innerHTML = `
            <div style="padding:20px; text-align:center; height:100%; width:100%; background:#fff; color:#333; overflow-y:auto;">
                <h3 style="color:var(--accent)">Self-Evo Forging: Live Pipeline</h3>
                <p style="font-size:0.9em; color:#666;">Autonomous codebase analysis and project-specific LLM generation.</p>

                <div style="display:flex; justify-content:center; gap:10px; margin-top:20px; flex-wrap:wrap;">
                    <div class="evo-stage-card ${status === 'SCANNING' ? 'active' : (status !== 'IDLE' && status !== 'STARTING' ? 'completed' : '')}" id="real-stage-scan">
                        <div style="font-size:1.2em;">🔍</div>
                        <div style="font-size:0.7em; font-weight:bold;">Scanner</div>
                        <div style="font-size:0.6em; color:#888;">Code Discovery</div>
                    </div>
                    <div class="evo-stage-arrow">➜</div>
                    <div class="evo-stage-card ${status === 'ENHANCING' ? 'active' : (['TRAINING', 'EXPORT_GGUF', 'COMPLETE'].includes(status) ? 'completed' : '')}" id="real-stage-enhance">
                        <div style="font-size:1.2em;">🪄</div>
                        <div style="font-size:0.7em; font-weight:bold;">Enhancer</div>
                        <div style="font-size:0.6em; color:#888;">Synthetic Data</div>
                    </div>
                    <div class="evo-stage-arrow">➜</div>
                    <div class="evo-stage-card ${status === 'TRAINING' ? 'active' : (['EXPORT_GGUF', 'COMPLETE'].includes(status) ? 'completed' : '')}" id="real-stage-train">
                        <div style="font-size:1.2em;">⚙️</div>
                        <div style="font-size:0.7em; font-weight:bold;">Trainer</div>
                        <div style="font-size:0.6em; color:#888;">Fine-Tuning</div>
                    </div>
                    <div class="evo-stage-arrow">➜</div>
                    <div class="evo-stage-card ${status === 'EXPORT_GGUF' || status === 'EXPORTING' ? 'active' : (status === 'COMPLETE' ? 'completed' : '')}" id="real-stage-export">
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
                        ${renderTelemetry(stats)}
                    </div>
                </div>

                <div style="margin-top:20px; display: flex; justify-content: center; gap: 10px;">
                    <button class="btn btn-primary" onclick="startForging()" ${status !== 'IDLE' && status !== 'COMPLETE' && status !== 'ERROR' ? 'disabled' : ''} style="padding: 10px 25px; font-weight: bold;">
                        ${status === 'IDLE' ? '🚀 Start Forging Process' : '🔄 Restart Forging'}
                    </button>
                    ${status === 'COMPLETE' ? `<button class="btn" onclick="alert('Model ready in Ollama!')">Check Model</button>` : ''}
                </div>

                <style>
                    .evo-stage-card {
                        width: 90px; padding: 12px; border: 1px solid #ddd; border-radius: 8px;
                        transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275); background: #fff;
                    }
                    .evo-stage-card.active { border-color: var(--accent); background: #e8f0fe; box-shadow: 0 0 15px rgba(0,122,204,0.4); transform: scale(1.1); z-index: 10; }
                    .evo-stage-card.completed { border-color: var(--success); background: #f6fff6; }
                    .evo-stage-card.completed div { color: var(--success) !important; }
                    .evo-stage-arrow { align-self: center; color: #ccc; font-weight: bold; }
                    .progress-glow {
                        position: absolute; top: 0; right: 0; bottom: 0; width: 30px;
                        background: linear-gradient(90deg, transparent, rgba(255,255,255,0.4), transparent);
                        animation: slide-glow 1.5s infinite;
                    }
                    @keyframes slide-glow {
                        from { transform: translateX(-500px); }
                        to { transform: translateX(500px); }
                    }
                </style>
            </div>
        `;

        details.innerHTML = `
            <h4>Pipeline Status</h4>
            <div style="font-size: 1.1em; font-weight: bold; color: ${statusColor(status)}; margin-bottom: 10px;">${status}</div>
            <p style="font-size: 0.85em; color: #666;">${getPhaseDescription(status)}</p>
            <hr/>
            <h4 style="font-size: 0.85em; color: #888; text-transform: uppercase;">Metrics</h4>
            <div style="font-size:0.9em; display: flex; flex-direction: column; gap: 5px;">
                <div style="display: flex; justify-content: space-between;"><span>Files Scanned:</span> <b>${stats.filesScanned || 0}</b></div>
                <div style="display: flex; justify-content: space-between;"><span>Instructions:</span> <b>${stats.instructionsGenerated || 0}</b></div>
                <div style="display: flex; justify-content: space-between;"><span>Current Loss:</span> <b>${typeof stats.currentLoss === 'number' ? stats.currentLoss.toFixed(4) : stats.currentLoss}</b></div>
                <div style="display: flex; justify-content: space-between;"><span>Epoch:</span> <b>${stats.currentEpoch || '0'}</b></div>
            </div>
            ${status === 'COMPLETE' ? `
                <div style="margin-top: 15px; padding: 10px; background: #f6fff6; border: 1px solid var(--success); border-radius: 4px; font-size: 0.8em; color: var(--success);">
                    <b>Deployment Success:</b> The model has been registered in Ollama as 'evo'.
                </div>
            ` : ''}
        `;
    };

    function renderTelemetry(stats) {
        const status = stats.status;
        switch(status) {
            case 'SCANNING':
                return `Discovering files in project root...<br/>Found: ${stats.filesScanned} source files.`;
            case 'ENHANCING':
                return `Generating synthetic instructions using local model...<br/>Progress: ${stats.instructionsGenerated} pairs created.`;
            case 'TRAINING':
                return `Running Unsloth fine-tuning...<br/>Loss: ${stats.currentLoss}<br/>Epoch: ${stats.currentEpoch}`;
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
            const res = await fetch(getBaseUrl() + \`/forge/session/\${activeSessionId}/forging/start?runtime=SWT\`, { method: 'POST' });
            if (res.ok) {
                log("Forging process started successfully.");
                if (!vizAutoRefresh) {
                    vizAutoRefresh = true;
                    document.getElementById('viz-selector').value = 'SELF_EVO';
                    if (window.setupAutoRefresh) window.setupAutoRefresh();
                }
            } else {
                throw new Error(await res.text());
            }
        } catch (e) { log("Start forging error: " + e.message); }
    };
})();
