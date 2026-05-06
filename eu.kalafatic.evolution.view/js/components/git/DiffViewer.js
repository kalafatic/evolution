const DiffViewer = {
    render(diffText) {
        if (!diffText) return '';

        const lines = diffText.split('\n');
        let html = '<div class="diff-viewer">';

        lines.forEach(line => {
            let type = '';
            if (line.startsWith('+')) type = 'added';
            else if (line.startsWith('-')) type = 'deleted';
            else if (line.startsWith('@@')) type = 'hunk-header';

            const content = this.escapeHtml(line);
            html += `<div class="diff-line ${type}"><div class="line-content">${content}</div></div>`;
        });

        html += '</div>';
        return html;
    },

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
};

export default DiffViewer;
