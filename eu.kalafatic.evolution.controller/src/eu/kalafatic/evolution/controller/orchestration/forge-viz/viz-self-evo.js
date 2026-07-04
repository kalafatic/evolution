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

        const progress = stats.progress || 0;

        area.innerHTML = `
            <div style="width:100%; max-width:600px; padding:20px; background:var(--bg-secondary); border-radius:8px; border:1px solid var(--border);">
                <h2 style="margin-top:0; color:var(--accent);">Self-Evo Forging Pipeline</h2>
                <p>Status: <strong style="color:${statusColor(stats.status)}">${stats.status}</strong></p>

                <div style="width:100%; height:20px; background:#ddd; border-radius:10px; overflow:hidden; margin:20px 0;">
                    <div style="width:${progress}%; height:100%; background:var(--accent); transition:width 0.5s;"></div>
                </div>
                <p style="text-align:right; font-size:0.8em; color:var(--text-dim);">${progress}% Complete</p>

                <div style="display:grid; grid-template-columns: 1fr 1fr; gap:15px; margin-top:20px;">
                    <div class="viz-card">
                        <h4>Scanner</h4>
                        <p>${stats.filesScanned} / ${stats.totalFiles} Files</p>
                    </div>
                    <div class="viz-card">
                        <h4>AI Enhancer</h4>
                        <p>${stats.instructionsGenerated} Instructions</p>
                    </div>
                    <div class="viz-card">
                        <h4>Fine-Tuning</h4>
                        <p>Loss: ${stats.currentLoss.toFixed(4)}</p>
                    </div>
                    <div class="viz-card">
                        <h4>Epoch</h4>
                        <p>${stats.currentEpoch}</p>
                    </div>
                </div>

                <div style="margin-top:30px; text-align:center;">
                    <button class="btn btn-primary" onclick="startForging()" ${stats.status !== 'IDLE' ? 'disabled' : ''}>Start Forging Process</button>
                </div>
            </div>
        `;

        details.innerHTML = `
            <h4>Pipeline Phase</h4>
            <p>${getPhaseDescription(stats.status)}</p>
            <hr/>
            <h4>Project Data</h4>
            <p>Codebase scanning is ${stats.status === 'SCANNING' ? 'active' : 'complete'}.</p>
        `;
    };

    function statusColor(status) {
        switch(status) {
            case 'IDLE': return 'var(--text-dim)';
            case 'SCANNING': return 'var(--accent)';
            case 'ENHANCING': return '#c586c0';
            case 'TRAINING': return '#dcdcaa';
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
            case 'EXPORT_GGUF': return 'Converting LoRA adapters to GGUF format for Ollama.';
            case 'COMPLETE': return 'Custom "evo" model is ready and registered in Ollama.';
            default: return 'Unknown phase.';
        }
    }

    window.startForging = async function() {
        log("Initiating Self-Evo forging...");
        try {
            await fetch(getBaseUrl() + `/forge/session/${activeSessionId}/forging/start?runtime=SWT`, { method: 'POST' });
        } catch (e) { log("Start forging error: " + e.message); }
    };
})();
