window.renderTrainingViz = function(data, events) {
    const area = document.getElementById('viz-area');
    const details = document.getElementById('viz-details-content');

    if (!data || (!data.sessionId && !data.loss)) {
        area.innerHTML = `
            <div style="display:flex; flex-direction:column; align-items:center; justify-content:center; height:100%; color:var(--evo-text-muted); text-align:center; padding:30px;">
                <div style="font-size:2.5em; margin-bottom:12px; opacity:0.6;">🎛️</div>
                <div style="font-weight:bold; font-size:1.1em; margin-bottom:6px; color:var(--evo-text);">No Active Training Telemetry</div>
                <div style="font-size:0.85em; max-width:400px; line-height:1.4;">
                    Select or start a Forge training run from AI Commander or the Training panel to observe real-time metrics, loss curves, and diagnostic events.
                </div>
            </div>
        `;
        if (details) details.innerHTML = `<div style="color:var(--evo-text-muted); font-size:0.85em;">No session details available.</div>`;
        return;
    }

    // Handle legacy or plain dictionary formats cleanly
    const isTelemetry = !!data.sessionId;
    const status = data.status || 'RUNNING';
    const statusColors = {
        'RUNNING': '#1d5296',
        'STOPPING': '#b45309',
        'COMPLETED': '#1e7e34',
        'FAILED': '#b91c1c',
        'IDLE': '#718096'
    };
    const badgeColor = statusColors[status] || '#1d5296';

    const currentEpoch = data.currentEpoch || 0;
    const totalEpochs = data.totalEpochs || 0;
    const currentStep = data.currentStep || 0;
    const totalSteps = data.totalSteps || 0;
    const currentLoss = typeof data.currentLoss === 'number' ? data.currentLoss : parseFloat(data.loss || 0);
    const valLoss = typeof data.valLoss === 'number' ? data.valLoss : 0;
    const bestValLoss = typeof data.bestValLoss === 'number' ? data.bestValLoss : 0;
    const tokensPerSec = data.tokensPerSec || 0;
    const stepsPerSec = data.stepsPerSec || 0;
    const processedTokens = data.processedTokens || 0;
    const totalTokens = data.totalTokens || 0;
    const ramUsedMb = data.ramUsedMb || 0;
    const ramTotalMb = data.ramTotalMb || 0;
    const cpuPercent = data.cpuPercent || 0;
    const gpuStatus = data.gpuStatus || 'N/A (CPU Mode)';
    const etaSec = data.estimatedRemainingSeconds || 0;
    const durationMs = data.durationMs || 0;

    // Formatting helpers
    const formatTime = (seconds) => {
        if (!seconds || seconds <= 0 || !isFinite(seconds)) return '--:--:--';
        const h = Math.floor(seconds / 3600);
        const m = Math.floor((seconds % 3600) / 60);
        const s = Math.floor(seconds % 60);
        return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    };

    const formatTokens = (tokens) => {
        if (tokens >= 1e6) return (tokens / 1e6).toFixed(2) + ' M';
        if (tokens >= 1e3) return (tokens / 1e3).toFixed(1) + ' K';
        return tokens.toString();
    };

    const history = data.history || [];
    const logStream = data.logStream || (Array.isArray(events) ? events.map(e => ({ timestamp: Date.now(), level: 'INFO', message: typeof e === 'string' ? e : JSON.stringify(e) })) : []);

    const progressPct = totalSteps > 0 ? Math.min(100, Math.round((currentStep / totalSteps) * 100)) : (totalEpochs > 0 ? Math.min(100, Math.round((currentEpoch / totalEpochs) * 100)) : 0);

    // Build Loss Chart Path
    let lossSvgPath = '';
    let valLossSvgPath = '';
    if (history.length > 1) {
        const svgW = 600;
        const svgH = 140;
        const pad = 15;

        let maxL = 0.001;
        let minL = Double_MAX();

        function Double_MAX() { return 999999; }

        history.forEach(h => {
            if (h.loss > maxL) maxL = h.loss;
            if (h.loss < minL) minL = h.loss;
            if (h.valLoss > 0) {
                if (h.valLoss > maxL) maxL = h.valLoss;
                if (h.valLoss < minL) minL = h.valLoss;
            }
        });

        if (minL >= maxL) minL = 0;
        const range = (maxL - minL) || 1.0;

        const pts = history.map((h, idx) => {
            const x = pad + (idx / (history.length - 1)) * (svgW - 2 * pad);
            const y = svgH - pad - ((h.loss - minL) / range) * (svgH - 2 * pad);
            return `${x.toFixed(1)},${y.toFixed(1)}`;
        });
        lossSvgPath = 'M ' + pts.join(' L ');

        const valPts = history.filter(h => h.valLoss > 0).map((h, idx, arr) => {
            const x = pad + (idx / (arr.length - 1)) * (svgW - 2 * pad);
            const y = svgH - pad - ((h.valLoss - minL) / range) * (svgH - 2 * pad);
            return `${x.toFixed(1)},${y.toFixed(1)}`;
        });
        if (valPts.length > 1) {
            valLossSvgPath = 'M ' + valPts.join(' L ');
        }
    }

    area.innerHTML = `
        <div style="display:flex; flex-direction:column; gap:12px; width:100%; max-width:1100px; margin:0 auto; padding:6px; box-sizing:border-box;">
            <!-- Control Room Header Banner -->
            <div style="background:var(--evo-surface); border:1px solid var(--evo-border); border-radius:var(--evo-radius); padding:12px 16px; box-shadow:var(--evo-shadow-sm);">
                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;">
                    <div style="display:flex; align-items:center; gap:10px;">
                        <span style="font-weight:bold; font-size:1.05em; color:var(--evo-text);">FORGE TRAINING CONTROL ROOM</span>
                        <span style="background:${badgeColor}; color:white; font-size:0.7em; font-weight:bold; padding:2px 8px; border-radius:10px; letter-spacing:0.5px;">${status}</span>
                    </div>
                    <div style="font-size:0.8em; color:var(--evo-text-secondary); font-family:var(--evo-font-mono);">
                        Session: <span style="color:var(--evo-primary); font-weight:bold;">${data.sessionId || 'N/A'}</span>
                    </div>
                </div>

                <!-- Overall Progress Bar -->
                <div style="margin-top:6px;">
                    <div style="display:flex; justify-content:space-between; font-size:0.75em; color:var(--evo-text-secondary); margin-bottom:3px;">
                        <span>Training Progress: <b>${progressPct}%</b></span>
                        <span>Elapsed: <b>${formatTime(durationMs / 1000)}</b> | ETA: <b>${formatTime(etaSec)}</b></span>
                    </div>
                    <div style="height:8px; background:var(--evo-surface-alt); border:1px solid var(--evo-border); border-radius:4px; overflow:hidden;">
                        <div style="width:${progressPct}%; height:100%; background:var(--evo-primary); transition:width 0.4s ease;"></div>
                    </div>
                </div>
            </div>

            <!-- KPI Metric Grid -->
            <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap:10px;">
                <div class="viz-card" style="margin-bottom:0;" title="Current Cross-Entropy Loss per token">
                    <h4>Train Loss</h4>
                    <div style="font-size:1.4em; font-weight:bold; color:var(--evo-primary);">${currentLoss > 0 ? currentLoss.toFixed(4) : '--'}</div>
                </div>
                <div class="viz-card" style="margin-bottom:0;" title="Validation set Loss">
                    <h4>Val Loss</h4>
                    <div style="font-size:1.4em; font-weight:bold; color:${valLoss > 0 ? 'var(--evo-info)' : 'var(--evo-text-muted)'};">${valLoss > 0 ? valLoss.toFixed(4) : 'N/A'}</div>
                </div>
                <div class="viz-card" style="margin-bottom:0;" title="Best validation loss achieved during this run">
                    <h4>Best Val Loss</h4>
                    <div style="font-size:1.4em; font-weight:bold; color:var(--evo-success);">${bestValLoss > 0 ? bestValLoss.toFixed(4) : 'N/A'}</div>
                </div>
                <div class="viz-card" style="margin-bottom:0;" title="Current Epoch out of Total Planned Epochs">
                    <h4>Epoch</h4>
                    <div style="font-size:1.3em; font-weight:bold; color:var(--evo-text);">${currentEpoch} / ${totalEpochs || '--'}</div>
                </div>
                <div class="viz-card" style="margin-bottom:0;" title="Current Optimization Step">
                    <h4>Step</h4>
                    <div style="font-size:1.3em; font-weight:bold; color:var(--evo-text);">${currentStep} / ${totalSteps || '--'}</div>
                </div>
                <div class="viz-card" style="margin-bottom:0;" title="Training Speed in Tokens per Second">
                    <h4>Throughput</h4>
                    <div style="font-size:1.3em; font-weight:bold; color:var(--evo-text);">${tokensPerSec > 0 ? tokensPerSec.toFixed(1) + ' t/s' : '--'}</div>
                </div>
                <div class="viz-card" style="margin-bottom:0;" title="Tokens processed in dataset">
                    <h4>Tokens</h4>
                    <div style="font-size:1.3em; font-weight:bold; color:var(--evo-text);">${formatTokens(processedTokens)}</div>
                </div>
                <div class="viz-card" style="margin-bottom:0;" title="JVM Memory consumption">
                    <h4>RAM Usage</h4>
                    <div style="font-size:1.3em; font-weight:bold; color:var(--evo-text);">${ramUsedMb > 0 ? Math.round(ramUsedMb) + ' MB' : '--'}</div>
                </div>
            </div>

            <!-- Synchronized Loss Chart -->
            <div style="background:var(--evo-surface); border:1px solid var(--evo-border); border-radius:var(--evo-radius); padding:12px; box-shadow:var(--evo-shadow-sm);">
                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;">
                    <span style="font-size:0.8em; font-weight:bold; color:var(--evo-text-secondary); text-transform:uppercase;">Training & Validation Loss Trajectory</span>
                    <div style="display:flex; gap:12px; font-size:0.75em;">
                        <span style="color:var(--evo-primary); font-weight:bold;">― Training Loss</span>
                        <span style="color:var(--evo-info); font-weight:bold;">― Validation Loss</span>
                    </div>
                </div>
                <div style="height:140px; width:100%; background:#111827; border-radius:4px; overflow:hidden; position:relative;">
                    ${lossSvgPath ? `
                        <svg width="100%" height="100%" viewBox="0 0 600 140" preserveAspectRatio="none">
                            <path d="${lossSvgPath}" fill="none" stroke="#3b82f6" stroke-width="2" />
                            ${valLossSvgPath ? `<path d="${valLossSvgPath}" fill="none" stroke="#38bdf8" stroke-width="2" stroke-dasharray="4,4" />` : ''}
                        </svg>
                    ` : `
                        <div style="display:flex; align-items:center; justify-content:center; height:100%; color:#9ca3af; font-size:0.8em;">
                            Accumulating training step telemetry...
                        </div>
                    `}
                </div>
            </div>

            <!-- Diagnostic Log Stream -->
            <div style="background:var(--evo-surface); border:1px solid var(--evo-border); border-radius:var(--evo-radius); padding:12px; box-shadow:var(--evo-shadow-sm);">
                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px;">
                    <span style="font-size:0.8em; font-weight:bold; color:var(--evo-text-secondary); text-transform:uppercase;">Live Diagnostic Event Stream</span>
                    <span style="font-size:0.7em; color:var(--evo-text-muted);">${logStream.length} event(s)</span>
                </div>
                <div id="viz-event-stream" style="background:#1e293b; color:#f8fafc; padding:8px 12px; height:110px; overflow-y:auto; font-family:var(--evo-font-mono); font-size:0.78em; border-radius:4px; border:1px solid #334155; line-height:1.4;">
                    ${logStream.length > 0 ? logStream.map(e => {
                        const time = e.timestamp ? new Date(e.timestamp).toLocaleTimeString() : '';
                        const lvl = e.level || 'INFO';
                        const lvlColor = lvl === 'ERROR' ? '#f87171' : (lvl === 'WARN' ? '#fbbf24' : '#60a5fa');
                        return `<div style="margin-bottom:3px; word-break:break-all;"><span style="color:#94a3b8;">[${time}]</span> <span style="color:${lvlColor}; font-weight:bold;">[${lvl}]</span> ${e.message}</div>`;
                    }).join('') : '<div style="color:#94a3b8;">No events recorded.</div>'}
                </div>
            </div>
        </div>
    `;

    if (details) {
        details.innerHTML = `
            <div style="font-size:0.85em; display:flex; flex-direction:column; gap:8px;">
                <h4 style="margin:0 0 6px 0; color:var(--evo-primary); border-bottom:1px solid var(--evo-border); padding-bottom:4px;">Session Telemetry</h4>
                <div><b>Status:</b> ${status}</div>
                <div><b>Session ID:</b> <br/><span style="font-family:var(--evo-font-mono); font-size:0.85em; color:var(--evo-text-secondary);">${data.sessionId || 'N/A'}</span></div>
                <div><b>Current Step:</b> ${currentStep} / ${totalSteps || '--'}</div>
                <div><b>Epochs:</b> ${currentEpoch} / ${totalEpochs || '--'}</div>
                <div><b>Train Loss:</b> ${currentLoss > 0 ? currentLoss.toFixed(4) : '--'}</div>
                <div><b>Val Loss:</b> ${valLoss > 0 ? valLoss.toFixed(4) : 'N/A'}</div>
                <div><b>Best Val Loss:</b> ${bestValLoss > 0 ? bestValLoss.toFixed(4) : 'N/A'}</div>
                <div><b>Speed:</b> ${tokensPerSec > 0 ? tokensPerSec.toFixed(1) + ' tok/s' : '--'}</div>
                <div><b>RAM:</b> ${ramUsedMb > 0 ? Math.round(ramUsedMb) + ' MB' : '--'}</div>
                <div><b>GPU:</b> ${gpuStatus}</div>
            </div>
        `;
    }
};
