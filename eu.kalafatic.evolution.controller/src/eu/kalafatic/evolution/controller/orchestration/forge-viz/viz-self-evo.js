/**
 * Visualization for Self-Evo Forging process.
 */
(function() {
    let lastLoadedSessionId = null;
    window.currentDatasources = [];
    
    window.loadDatasources = async function() {
        try {
            const res = await fetch(getBaseUrl() + `/forge/session/${activeSessionId}/datasources?runtime=SWT`);
            if (res.ok) {
                window.currentDatasources = await res.json();
                window.renderDatasourcesTable();
            }
        } catch (e) {
            console.error("Failed to load data sources", e);
        }
    };

    window.renderDatasourcesTable = function() {
        const listBody = document.getElementById('datasources-list');
        if (!listBody) return;
        
        if (!window.currentDatasources || window.currentDatasources.length === 0) {
            listBody.innerHTML = `<tr><td colspan="2" style="padding: 10px; text-align: center; color: #888; font-style: italic;">No data sources selected. Default: c:\\Users\\petrk\\git\\evolution</td></tr>`;
            return;
        }
        
        listBody.innerHTML = window.currentDatasources.map((src, idx) => `
            <tr style="border-bottom: 1px solid #eee;">
                <td style="padding: 6px; word-break: break-all; font-family: monospace; text-align: left;">${src}</td>
                <td style="padding: 6px; text-align: center;">
                    <button class="btn btn-sm" onclick="removeDatasource(${idx})" style="padding: 2px 6px; font-size: 0.75em; color: var(--error); border-color: var(--error); background: transparent;">Remove</button>
                </td>
            </tr>
        `).join('');
    };

    window.triggerBrowseFolder = function() {
        if (window.browseDirectory) {
            const path = window.browseDirectory();
            if (path) {
                document.getElementById('datasource-input').value = path;
            }
        } else {
            const path = prompt("Enter directory path manually:");
            if (path) {
                document.getElementById('datasource-input').value = path;
            }
        }
    };
    
    window.triggerBrowseFile = function() {
        if (window.browseFile) {
            const path = window.browseFile();
            if (path) {
                document.getElementById('datasource-input').value = path;
            }
        } else {
            const path = prompt("Enter file path manually:");
            if (path) {
                document.getElementById('datasource-input').value = path;
            }
        }
    };

    window.addDatasource = function() {
        const input = document.getElementById('datasource-input');
        if (!input) return;
        const val = input.value.trim();
        if (val) {
            if (!window.currentDatasources.includes(val)) {
                window.currentDatasources.push(val);
                window.renderDatasourcesTable();
                input.value = '';
            } else {
                alert("This path is already added.");
            }
        }
    };
    
    window.removeDatasource = function(idx) {
        window.currentDatasources.splice(idx, 1);
        window.renderDatasourcesTable();
    };
    
    window.saveDatasources = async function() {
        try {
            const res = await fetch(getBaseUrl() + `/forge/session/${activeSessionId}/datasources?runtime=SWT`, {
                method: 'POST',
                headers: { 'Content-Type': 'text/plain' },
                body: JSON.stringify(window.currentDatasources)
            });
            if (res.ok) {
                alert("Data sources saved successfully.");
            } else {
                alert("Failed to save data sources: " + res.statusText);
            }
        } catch (e) {
            alert("Error saving data sources: " + e.message);
        }
    };

    window.renderSelfEvoViz = function(stats) {
        window.currentSelfEvoStats = stats;
        const area = document.getElementById('viz-area');
        const details = document.getElementById('viz-details-content');

        const progress = stats ? (stats.progress || 0) : 0;
        const status = stats ? (stats.status || 'IDLE') : 'IDLE';

        if (lastLoadedSessionId !== activeSessionId) {
            lastLoadedSessionId = activeSessionId;
            window.loadDatasources();
        }

        area.innerHTML = `
            <div style="padding:20px; text-align:center; height:100%; width:100%; background:#fff; color:#333; overflow-y:auto;">
                <h3 style="color:var(--accent)">Self-Evo Forging: Live Pipeline</h3>
                <p style="font-size:0.9em; color:#666;">Autonomous codebase analysis and project-specific LLM generation.</p>

                <div style="display:flex; justify-content:center; gap:10px; margin-top:20px; flex-wrap:wrap;">
                    <div class="evo-stage-card ${status === 'SCANNING' ? 'active' : (status !== 'IDLE' && status !== 'STARTING' ? 'completed' : '')}" id="stage-scan" onclick="window.showStageInfo('Scanner')">
                        <div style="font-size:1.2em;">🔍</div>
                        <div style="font-size:0.7em; font-weight:bold;">Scanner</div>
                        <div style="font-size:0.6em; color:#888;">Code Discovery</div>
                    </div>
                    <div class="evo-stage-arrow">➜</div>
                    <div class="evo-stage-card ${status === 'ENHANCING' ? 'active' : (['TRAINING', 'EXPORT_GGUF', 'COMPLETE'].includes(status) ? 'completed' : '')}" id="stage-enhance" onclick="window.showStageInfo('Enhancer')">
                        <div style="font-size:1.2em;">🪄</div>
                        <div style="font-size:0.7em; font-weight:bold;">Enhancer</div>
                        <div style="font-size:0.6em; color:#888;">Synthetic Data</div>
                    </div>
                    <div class="evo-stage-arrow">➜</div>
                    <div class="evo-stage-card ${status === 'TRAINING' ? 'active' : (['EXPORT_GGUF', 'COMPLETE'].includes(status) ? 'completed' : '')}" id="stage-train" onclick="window.showStageInfo('Trainer')">
                        <div style="font-size:1.2em;">⚙️</div>
                        <div style="font-size:0.7em; font-weight:bold;">Trainer</div>
                        <div style="font-size:0.6em; color:#888;">Fine-Tuning</div>
                    </div>
                    <div class="evo-stage-arrow">➜</div>
                    <div class="evo-stage-card ${status === 'EXPORT_GGUF' || status === 'EXPORTING' ? 'active' : (status === 'COMPLETE' ? 'completed' : '')}" id="stage-export" onclick="window.showStageInfo('Deployment')">
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

                <!-- LLM Forge Data Sources Card -->
                <div style="max-width:600px; margin: 20px auto; padding: 15px; border: 1px solid #ddd; border-radius: 8px; background: #fafafa; text-align: left;">
                    <h4 style="margin-top: 0; color: var(--accent); font-size: 0.95em; display: flex; align-items: center; gap: 6px;">
                        <span>📁</span> LLM Forge Data Sources Target Model Object
                    </h4>
                    <p style="font-size: 0.8em; color: #666; margin-bottom: 10px;">Select directories/files with markdown (.md) training data for Self-Evo forging.</p>
                    
                    <div style="max-height: 120px; overflow-y: auto; border: 1px solid #ccc; border-radius: 4px; background: #fff; margin-bottom: 10px;">
                        <table style="width: 100%; border-collapse: collapse; font-size: 0.85em;">
                            <thead>
                                <tr style="background: #f0f0f0; border-bottom: 1px solid #ddd;">
                                    <th style="padding: 6px; text-align: left; width: 80%;">Path</th>
                                    <th style="padding: 6px; text-align: center; width: 20%;">Action</th>
                                </tr>
                            </thead>
                            <tbody id="datasources-list">
                                <tr><td colspan="2" style="padding: 10px; text-align: center; color: #888; font-style: italic;">Loading data sources...</td></tr>
                            </tbody>
                        </table>
                    </div>
                    
                    <div style="display: flex; gap: 8px; align-items: center; justify-content: space-between;">
                        <div style="display: flex; gap: 6px; flex-grow: 1;">
                            <input type="text" id="datasource-input" placeholder="Enter file or folder path..." style="padding: 6px; font-size: 0.85em; flex-grow: 1; border: 1px solid #ccc; border-radius: 4px;" />
                            <button class="btn btn-sm" onclick="triggerBrowseFolder()" style="padding: 4px 8px; font-size: 0.8em; background: #e2e8f0; border: 1px solid #cbd5e1; color: #334155; border-radius: 4px; cursor: pointer;">Folder 📂</button>
                            <button class="btn btn-sm" onclick="triggerBrowseFile()" style="padding: 4px 8px; font-size: 0.8em; background: #e2e8f0; border: 1px solid #cbd5e1; color: #334155; border-radius: 4px; cursor: pointer;">File 📄</button>
                        </div>
                        <div style="display: flex; gap: 6px;">
                            <button class="btn btn-sm btn-primary" onclick="addDatasource()" style="padding: 6px 12px; font-size: 0.8em;">Add</button>
                            <button class="btn btn-sm" onclick="saveDatasources()" style="padding: 6px 12px; font-size: 0.8em; background: var(--success); color: white; border: none; cursor: pointer;">Save</button>
                        </div>
                    </div>
                </div>

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

        window.renderDatasourcesTable();
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
                    <div class="evo-stage-card" id="stage-scan" onclick="window.showStageInfo('Scanner')">
                        <div style="font-size:1.2em;">🔍</div>
                        <div style="font-size:0.7em; font-weight:bold;">Scanner</div>
                        <div style="font-size:0.6em; color:#888;">Code Discovery</div>
                    </div>
                    <div class="evo-stage-arrow">➜</div>
                    <div class="evo-stage-card" id="stage-enhance" onclick="window.showStageInfo('Enhancer')">
                        <div style="font-size:1.2em;">🪄</div>
                        <div style="font-size:0.7em; font-weight:bold;">Enhancer</div>
                        <div style="font-size:0.6em; color:#888;">Synthetic Data</div>
                    </div>
                    <div class="evo-stage-arrow">➜</div>
                    <div class="evo-stage-card" id="stage-train" onclick="window.showStageInfo('Trainer')">
                        <div style="font-size:1.2em;">⚙️</div>
                        <div style="font-size:0.7em; font-weight:bold;">Trainer</div>
                        <div style="font-size:0.6em; color:#888;">Fine-Tuning</div>
                    </div>
                    <div class="evo-stage-arrow">➜</div>
                    <div class="evo-stage-card" id="stage-export" onclick="window.showStageInfo('Deployment')">
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

    window.showStageInfo = function(stageName) {
        const stats = window.currentSelfEvoStats;
        const folder = (stats && stats.outputFolder) ? stats.outputFolder : "dist/";

        let overlay = document.getElementById('stage-info-modal');
        if (!overlay) {
            overlay = document.createElement('div');
            overlay.id = 'stage-info-modal';
            overlay.style.position = 'fixed';
            overlay.style.top = '0';
            overlay.style.left = '0';
            overlay.style.width = '100%';
            overlay.style.height = '100%';
            overlay.style.backgroundColor = 'rgba(0,0,0,0.5)';
            overlay.style.display = 'flex';
            overlay.style.justifyContent = 'center';
            overlay.style.alignItems = 'center';
            overlay.style.zIndex = '10000';
            document.body.appendChild(overlay);
        }

        overlay.style.display = 'flex';
        overlay.innerHTML = `
            <div style="background: white; padding: 25px; border-radius: 8px; max-width: 450px; width: 90%; box-shadow: 0 4px 15px rgba(0,0,0,0.25); text-align: left; position: relative; font-family: 'Outfit', 'Segoe UI', system-ui, sans-serif; color: #333;">
                <button onclick="document.getElementById('stage-info-modal').style.display='none'" style="position: absolute; top: 12px; right: 12px; border: none; background: transparent; font-size: 1.6em; cursor: pointer; color: #888; font-weight: bold; line-height: 1;">&times;</button>
                <h3 style="margin-top: 0; color: #ff5200; border-bottom: 2px solid #ff5200; padding-bottom: 8px; font-size: 1.25em; text-transform: uppercase; font-weight: 800; letter-spacing: 0.5px;">${stageName} Stage Info</h3>
                <p style="font-size: 0.9em; line-height: 1.5; color: #555; margin-bottom: 15px;">The Self-Evo pipeline executes this stage to analyze code and generate custom models. Artifacts and diagnostics are actively logged below.</p>
                <div style="margin: 15px 0; padding: 12px; background: #fafafa; border: 1px solid #e2e8f0; border-radius: 6px; word-break: break-all;">
                    <span style="font-size: 0.75em; color: #718096; font-weight: 900; display: block; margin-bottom: 4px; letter-spacing: 0.5px; text-transform: uppercase;">OUTPUT FOLDER LINK:</span>
                    <a href="file:///${folder.replace(/\\/g, '/')}" target="_blank" style="color: #ff5200; font-weight: 800; text-decoration: underline; font-family: monospace; font-size: 0.85em;">${folder}</a>
                </div>
                <div style="text-align: right; margin-top: 20px;">
                    <button onclick="document.getElementById('stage-info-modal').style.display='none'" class="btn btn-primary" style="padding: 6px 18px; font-size: 0.8em; font-weight: 800; border-radius: 20px; cursor: pointer; text-transform: uppercase; border: none; background: linear-gradient(135deg, #ff8a00 0%, #e52e00 100%); color: white; box-shadow: 0 4px 10px rgba(255,82,0,0.2);">Close</button>
                </div>
            </div>
        `;
    };
})();
