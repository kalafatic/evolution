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
            document.getElementById('viz-details-content').innerHTML = `
                <h4>${n.name}</h4>
                <p>Type: ${n.type}</p>
                <p>Params: 1.2M</p>
                <p>Input: [Batch, 512]</p>
                <p>Output: [Batch, 512]</p>
                <p>Status: OK</p>
            `;
        };

        const title = document.createElement('h4');
        title.textContent = n.name;
        card.appendChild(title);

        const type = document.createElement('div');
        type.style.fontSize = '0.7em';
        type.textContent = n.type;
        card.appendChild(type);

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
