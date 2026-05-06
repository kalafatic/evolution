import FileItem from './FileItem.js';
import DiffViewer from './DiffViewer.js';

class ChangesPanel {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.files = [];
        this.lastDiffs = {};
    }

    update(files, lastDiffs) {
        this.files = files;
        this.lastDiffs = lastDiffs;
        this.render();
    }

    render() {
        this.container.innerHTML = '';
        if (this.files.length === 0) {
            this.container.innerHTML = '<div style="padding: 20px; color: #94a3b8; text-align: center; font-size: 11px;">No pending changes</div>';
            return;
        }

        this.files.forEach(filePath => {
            const fileItem = new FileItem(filePath, this.lastDiffs[filePath]);
            this.container.appendChild(fileItem.render());
        });
    }

    toggleFileDiff(path) {
        const item = this.container.querySelector(`.file-stack-item[data-path="${path}"]`);
        if (item) {
            item.classList.toggle('expanded');
            if (item.classList.contains('expanded') && !this.lastDiffs[path]) {
                window.dispatchEvent(new CustomEvent('java:getDiff', { detail: path }));
            }
        }
    }

    showDiff(data) {
        const item = this.container.querySelector(`.file-stack-item[data-path="${data.path}"]`);
        if (item) {
            const diffContainer = item.querySelector('.file-diff-inline');
            if (diffContainer) {
                diffContainer.innerHTML = DiffViewer.render(data.diff);
            }
        }
    }

    toggleAllFiles() {
        const allItems = this.container.querySelectorAll('.file-stack-item');
        const shouldExpand = Array.from(allItems).some(el => !el.classList.contains('expanded'));
        allItems.forEach(el => {
            if (shouldExpand) el.classList.add('expanded');
            else el.classList.remove('expanded');
        });
    }

    filterFiles(query) {
        const q = query.toLowerCase();
        const allItems = this.container.querySelectorAll('.file-stack-item');
        allItems.forEach(el => {
            const path = el.dataset.path.toLowerCase();
            el.style.display = path.includes(q) ? 'block' : 'none';
        });
    }
}

export default ChangesPanel;
