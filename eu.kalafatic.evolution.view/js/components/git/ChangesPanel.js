import { FileItem } from './FileItem.js';
import { DiffViewer } from './DiffViewer.js';
import { JavaBridge } from '../../core/JavaBridge.js';
import { eventBus } from '../../core/EventBus.js';

export class ChangesPanel {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
    }

    update(files, lastDiffs) {
        if (!this.container) return;
        this.container.innerHTML = '';

        if (!files || files.length === 0) {
            this.container.innerHTML = '<div style="font-size: 11px; color: #94a3b8; text-align: center; margin-top: 20px;">No changes detected</div>';
            return;
        }

        files.forEach(fileInfo => {
            const fileItem = new FileItem(fileInfo, lastDiffs[this.extractPath(fileInfo)]);
            this.container.appendChild(fileItem.render());
        });
    }

    extractPath(fileInfo) {
        if (fileInfo.includes(' ')) {
            return fileInfo.substring(2).trim();
        }
        return fileInfo;
    }

    showDiff(data) {
        const path = data.path;
        const diff = data.diff;
        const container = document.getElementById(`diff-inline-${path}`);
        if (!container) return;

        container.innerHTML = '';
        container.appendChild(DiffViewer.render(data));
    }

    toggleFileDiff(path) {
        const item = document.querySelector(`.file-stack-item[data-path="${path}"]`);
        if (!item) return;

        const isExpanded = item.classList.contains('expanded');
        if (isExpanded) {
            item.classList.remove('expanded');
        } else {
            item.classList.add('expanded');
            const diffContainer = document.getElementById(`diff-inline-${path}`);
            if (diffContainer && diffContainer.innerHTML.includes('Loading diff...')) {
                JavaBridge.call('getDiff', '-1', path);
            }
        }
    }

    toggleAllFiles() {
        const items = document.querySelectorAll('.file-stack-item');
        const anyCollapsed = Array.from(items).some(i => !i.classList.contains('expanded'));
        items.forEach(i => {
            if (anyCollapsed) {
                if (!i.classList.contains('expanded')) this.toggleFileDiff(i.dataset.path);
            } else {
                i.classList.remove('expanded');
            }
        });
    }

    filterFiles(query) {
        const items = document.querySelectorAll('.file-stack-item');
        query = query.toLowerCase();
        items.forEach(i => {
            const path = i.dataset.path.toLowerCase();
            i.style.display = path.includes(query) ? 'block' : 'none';
        });
    }
}
