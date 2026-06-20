function renderEvolutionViz(snapshots) {
    const area = document.getElementById('viz-area');
    area.innerHTML = '';

    if (snapshots.length === 0) {
        area.innerHTML = '<div style="padding:20px; color:var(--text-dim)">No snapshots found for this session.</div>';
        return;
    }

    snapshots.forEach((s, i) => {
        const item = document.createElement('div');
        item.className = 'timeline-item';
        item.innerHTML = `
            <div style="font-weight:bold">${s.name || 'Snapshot ' + (i+1)}</div>
            <div style="font-size:0.8em; color:var(--text-dim)">${new Date(s.timestamp).toLocaleString()}</div>
        `;
        item.onclick = () => {
            document.getElementById('viz-details-content').innerHTML = `
                <h4>Snapshot Info</h4>
                <p>ID: ${s.id}</p>
                <p>Generation: ${s.generation || 0}</p>
                <p>Model Type: MLP</p>
                <p>Loss: 0.42</p>
                <div style="display:flex; flex-direction:column; gap:5px; margin-top:10px;">
                    <button class="btn btn-sm" style="width:100%" onclick="compareSnapshot('${s.id}')">Compare with Active</button>
                    <button class="btn btn-sm btn-primary" style="width:100%" onclick="exportSnapshot('${s.id}')">EXPORT FOR OLLAMA</button>
                </div>
            `;
        };
        area.appendChild(item);
    });
}
