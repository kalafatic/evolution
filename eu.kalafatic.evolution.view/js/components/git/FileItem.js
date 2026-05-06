import DiffViewer from './DiffViewer.js';

class FileItem {
    constructor(path, diff) {
        this.path = path;
        this.diff = diff;
    }

    render() {
        const item = document.createElement('div');
        item.className = 'file-stack-item';
        item.dataset.path = this.path;

        // Strip status prefix (e.g. "M ", "A ", "D ")
        const statusChar = this.path.substring(0, 1);
        const cleanPath = this.path.substring(2);
        const fileName = cleanPath.split('/').pop();
        const folderPath = cleanPath.includes('/') ? cleanPath.substring(0, cleanPath.lastIndexOf('/')) : './';

        let statusClass = 'modified';
        if (statusChar === 'A') statusClass = 'added';
        if (statusChar === 'D') statusClass = 'deleted';

        item.innerHTML = `
            <div class="file-info" onclick="window.dispatchEvent(new CustomEvent('ui:toggleFileDiff', { detail: '${this.path}' }))"
                 oncontextmenu="window.dispatchEvent(new CustomEvent('ui:showContextMenu', { detail: { event: event, path: '${this.path}' } }))">
                <div class="file-status ${statusClass}">${statusChar}</div>
                <div class="file-icon">${this.getFileIcon(fileName)}</div>
                <div class="file-details">
                    <div class="file-name">${fileName}</div>
                    <div class="file-path">${folderPath}</div>
                </div>
                <div class="expand-icon">▶</div>
            </div>
            <div class="file-diff-inline">
                ${this.diff ? DiffViewer.render(this.diff) : '<div style="padding: 10px; font-size: 10px; color: #94a3b8;">Loading diff...</div>'}
            </div>
        `;

        return item;
    }

    getFileIcon(name) {
        if (name.endsWith('.java')) return '☕';
        if (name.endsWith('.js')) return '📜';
        if (name.endsWith('.html')) return '🌐';
        if (name.endsWith('.css')) return '🎨';
        if (name.endsWith('.xml') || name.endsWith('.pom')) return '⚙️';
        if (name.endsWith('.md')) return '📝';
        return '📄';
    }
}

export default FileItem;
