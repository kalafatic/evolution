function renderDatasetViz(stats, sample) {
    const area = document.getElementById('viz-area');
    area.innerHTML = `
        <div class="sample-nav">
            <span>Sample #${currentSampleIndex}</span>
        </div>
        <div>
            <label>Raw Text:</label>
            <div class="data-block">${sample.raw || 'N/A'}</div>
        </div>
        <div style="margin-top:10px;">
            <label>Tokenized Form:</label>
            <div class="data-block">${JSON.stringify(sample.tokens || [])}</div>
        </div>
        <div style="margin-top:10px;">
            <label>Training Pair:</label>
            <div class="data-block">[${(sample.tokens || []).slice(0,-1)}] -> [${(sample.tokens || []).slice(-1)}]</div>
        </div>
    `;

    document.getElementById('viz-details-content').innerHTML = `
        <h4>Dataset Stats</h4>
        <p>Size: ${stats.size || 0}</p>
        <p>Vocab: ${stats.vocab || 0}</p>
        <p>Tokens: ${(stats.size || 0) * 100}</p>
    `;
}
