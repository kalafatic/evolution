import { JavaBridge } from '../../core/JavaBridge.js';

export class FileItem {
    constructor(fileInfo, lastDiff) {
        this.fileInfo = fileInfo;
        this.lastDiff = lastDiff;
        this.status = 'M';
        this.path = fileInfo;
        this.parseFileInfo();
    }

    parseFileInfo() {
        if (this.fileInfo.includes(' ')) {
            const statusPart = this.fileInfo.substring(0, 2).trim();
            const pathPart = this.fileInfo.substring(2).trim();
            if (statusPart && pathPart) {
                this.status = statusPart.toUpperCase();
                this.path = pathPart;
            }
        }
    }

    getFileIcon(ext) {
        const icons = {
            java: '☕', js: '📜', ts: '📘', html: '🌐', css: '🎨',
            json: '🔧', xml: '📦', md: '📝', txt: '📄', py: '🐍'
        };
        return icons[ext] || '📄';
    }

    render() {
        const item = document.createElement('div');
        item.className = 'file-stack-item';
        item.dataset.path = this.path;

        const fileName = this.path.split('/').pop();
        const dirPath = this.path.substring(0, this.path.lastIndexOf('/') + 1);
        const ext = fileName.split('.').pop().toLowerCase();
        const icon = this.getFileIcon(ext);

        const statusClass = this.status === 'A' ? 'added' : (this.status === 'D' ? 'deleted' : 'modified');

        item.innerHTML = `
            <div class="file-info" onclick="window.dispatchEvent(new CustomEvent('ui:toggleFileDiff', {detail: '${this.path.replace(/'/g, "\\'")}'}))" oncontextmenu="window.dispatchEvent(new CustomEvent('ui:showContextMenu', {detail: {event: event, path: '${this.path.replace(/'/g, "\\'")}'}}))">
                <span class="file-status ${statusClass}">${this.status}</span>
                <span class="file-icon">${icon}</span>
                <div class="file-details">
                    <span class="file-name">${fileName}</span>
                    <span class="file-path">${dirPath}</span>
                </div>
                <span class="expand-icon">▶</span>
            </div>
            <div id="diff-inline-${this.path}" class="file-diff-inline">
                ${this.lastDiff ? '' : '<div style="padding: 20px; text-align: center; color: #94a3b8; font-size: 11px;">Loading diff...</div>'}
            </div>
        `;

        if (this.lastDiff) {
            import('./DiffViewer.js').then(m => {
                const diffContainer = item.querySelector(`#diff-inline-${this.path.replace(/(:|\.|\[|\]|,|=|@)/g, "\\$1")}`);
                if (diffContainer) {
                    diffContainer.innerHTML = '';
                    diffContainer.appendChild(m.DiffViewer.render({ diff: this.lastDiff }));
                }
            });
        }

        return item;
    }
}
