function renderArchitectureViz(data) {
    const area = document.getElementById('viz-area');
    area.innerHTML = '';

    const nodes = data.nodes && data.nodes.length > 0 ? data.nodes : [
        {id: 'in', name: 'Input', type: 'DATA'},
        {id: 'emb', name: 'Embedding', type: 'LAYER'},
        {id: 't1', name: 'Transformer Block #1', type: 'TRANSFORMER'},
        {id: 'out', name: 'Output Head', type: 'LAYER'}
    ];

    const container = document.createElement('div');
    container.style.padding = '20px';
    container.style.display = 'flex';
    container.style.flexDirection = 'column';
    container.style.alignItems = 'center';
    container.style.gap = '20px';

    nodes.forEach((n, i) => {
        const card = document.createElement('div');
        card.className = 'viz-card';
        card.style.width = '200px';
        card.style.cursor = 'pointer';
        card.onclick = () => {
            const params = n.type === 'TRANSFORMER' ? '12.4M' : n.type === 'ATTENTION' ? '4.2M' : '1.2M';
            const inputDim = n.type === 'DATA' ? '[Batch, 28, 28, 1]' : '[Batch, 512]';
            const outputDim = '[Batch, 512]';

            document.getElementById('viz-details-content').innerHTML = `
                <div style="padding: 10px; background: white; border: 1px solid var(--accent); border-radius: 4px;">
                    <h4 style="margin: 0 0 10px 0; border-bottom: 1px solid #eee;">${n.name}</h4>
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; font-size: 0.85em;">
                        <div style="color: #666;">Type:</div><div>${n.type}</div>
                        <div style="color: #666;">Parameters:</div><div style="font-weight: bold; color: var(--accent);">${params}</div>
                        <div style="color: #666;">Input:</div><div style="font-family: monospace;">${inputDim}</div>
                        <div style="color: #666;">Output:</div><div style="font-family: monospace;">${outputDim}</div>
                        <div style="color: #666;">Status:</div><div style="color: var(--success);">● Active</div>
                    </div>
                    <div style="margin-top: 15px;">
                        <div style="font-size: 0.7em; color: #999; margin-bottom: 4px;">WEIGHT DISTRIBUTION</div>
                        <div style="height: 30px; background: #f0f0f0; border-radius: 2px; overflow: hidden; display: flex; align-items: flex-end; gap: 1px;">
                            ${Array(15).fill(0).map(() => `<div style="flex: 1; height: ${Math.random()*100}%; background: var(--accent); opacity: 0.6;"></div>`).join('')}
                        </div>
                    </div>
                </div>
            `;
        };

        const title = document.createElement('h4');
        title.textContent = n.name;
        card.appendChild(title);

        const typeRow = document.createElement('div');
        typeRow.style.display = 'flex';
        typeRow.style.justifyContent = 'space-between';
        typeRow.style.alignItems = 'center';

        const type = document.createElement('div');
        type.style.fontSize = '0.75em';
        type.style.color = 'var(--text-dim)';
        type.textContent = n.type;
        typeRow.appendChild(type);

        // IO Indicators
        const io = document.createElement('div');
        io.style.display = 'flex';
        io.style.gap = '4px';
        io.innerHTML = `
            <div style="width: 6px; height: 6px; border-radius: 50%; background: #4fc1ff;" title="Input available"></div>
            <div style="width: 6px; height: 6px; border-radius: 50%; background: #89d185;" title="Output healthy"></div>
        `;
        typeRow.appendChild(io);

        card.appendChild(typeRow);

        container.appendChild(card);

        if (i < nodes.length - 1) {
            const arrow = document.createElement('div');
            arrow.innerHTML = `
                <svg width="20" height="30">
                    <line x1="10" y1="0" x2="10" y2="25" stroke="#555" stroke-width="2" />
                    <path d="M 5,20 L 10,25 L 15,20" fill="none" stroke="#555" stroke-width="2" />
                </svg>
            `;
            container.appendChild(arrow);
        }
    });

    area.appendChild(container);
}
