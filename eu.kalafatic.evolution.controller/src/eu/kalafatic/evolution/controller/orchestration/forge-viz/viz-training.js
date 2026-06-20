function renderTrainingViz(metrics, events) {
    const area = document.getElementById('viz-area');
    let eventsHtml = (events || []).map(e => `<div style="font-size:0.8em; margin-bottom:2px; border-bottom:1px solid #333; padding-bottom:2px;">${e}</div>`).join('');

    area.innerHTML = `
        <div style="display:grid; grid-template-columns: 1fr 1fr; gap: 10px; width:100%;">
            <div class="viz-card"><h4>Loss</h4><div style="font-size:1.5em;color:var(--accent)">${metrics.loss || '0.000'}</div></div>
            <div class="viz-card"><h4>Accuracy</h4><div style="font-size:1.5em;color:var(--success)">${metrics.acc || '0.000'}</div></div>
        </div>
        <div style="margin-top:10px; height: 150px; width:100%; border: 1px solid var(--border); position: relative; background:#111; overflow:hidden;">
            <svg width="100%" height="100%" id="training-anim-svg">
                 <!-- Multi-layer propagation simulation -->
                 <g id="layer-signals"></g>
            </svg>
        </div>
        <div style="margin-top:10px; height: 100px; width:100%; border: 1px solid var(--border); position: relative;">
            <svg width="100%" height="100%">
                 <path d="M 0 100 L 50 80 L 100 90 L 150 50 L 200 60 L 250 20 L 300 30" fill="none" stroke="var(--accent)" stroke-width="2" />
            </svg>
            <div style="position:absolute; bottom:5px; right:5px; font-size:0.7em; color:var(--text-dim)">Loss over time (Simulated)</div>
        </div>
        <div style="margin-top:15px; width:100%;">
            <label style="font-size:0.7em; color:var(--text-dim); text-transform:uppercase;">Event Stream</label>
            <div id="viz-event-stream" style="background:#ffffff; color:#333333; padding:10px; height:80px; overflow-y:auto; font-family:monospace; margin-top:5px; border:1px solid var(--border);">
                ${eventsHtml || '<div style="color:#888">No events recorded.</div>'}
            </div>
        </div>
    `;

    // Start propagation animation in Training Monitor
    setTimeout(() => {
        const g = document.getElementById('layer-signals');
        if (!g) return;
        const layers = [4, 6, 6, 4];
        const width = g.closest('svg').clientWidth;
        const height = g.closest('svg').clientHeight;

        layers.forEach((count, lIdx) => {
            const x = 50 + lIdx * ((width-100) / (layers.length - 1));
            for (let i = 0; i < count; i++) {
                const y = (height / (count + 1)) * (i + 1);
                const circle = document.createElementNS("http://www.w3.org/2000/svg", "circle");
                circle.setAttribute("cx", x);
                circle.setAttribute("cy", y);
                circle.setAttribute("r", 4);
                circle.setAttribute("fill", "#444");
                g.appendChild(circle);
            }
        });
    }, 100);

    document.getElementById('viz-details-content').innerHTML = `
        <h4>Training Status</h4>
        <p>Epoch: 5</p>
        <p>Batch: 128/1000</p>
        <p>LR: 0.0001</p>
        <p>Elapsed: 00:12:45</p>
    `;
}
