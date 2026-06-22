window.ChatApp = window.ChatApp || {};

window.ChatApp.Panel = {
    renderChanges: function(files, lastDiffs) {
        const list = document.getElementById('changes-list');
        if (!list) return;
        list.innerHTML = '';
        if (!files || files.length === 0) {
            list.innerHTML = '<div style="font-size: 11px; color: #94a3b8; text-align: center; margin-top: 20px;">No changes detected</div>';
            return;
        }

        files.forEach(itemData => {
            let fileInfo = typeof itemData === 'string' ? itemData : itemData.info;
            let date = typeof itemData === 'string' ? '' : itemData.date;

            let status = 'M';
            let path = fileInfo;
            if (fileInfo.includes(' ')) {
                status = fileInfo.substring(0, 2).trim().toUpperCase();
                path = fileInfo.substring(2).trim();
            }

            const item = document.createElement('div');
            item.className = 'file-stack-item' + (status === 'A' ? ' newly-created' : '');
            item.dataset.path = path;
            const fileName = path.split('/').pop();
            const dir = path.substring(0, path.lastIndexOf('/') + 1);
            const statusClass = status === 'A' ? 'added' : (status === 'D' ? 'deleted' : 'modified');

            item.innerHTML = `
                <div class="file-info" onclick="window.ChatApp.Panel.toggleFileDiff('${path}')" oncontextmenu="window.ChatApp.UI.showContextMenu(event, '${path}')">
                    <span class="file-status ${statusClass}">${status}</span>
                    <div class="file-details">
                        <span class="file-name">${fileName}</span>
                        <span class="file-path">${dir}</span>
                        ${date ? `<span class="file-date" style="font-size: 8px; color: #94a3b8;">${date}</span>` : ''}
                    </div>
                    <span class="expand-icon">▶</span>
                </div>
                <div id="diff-inline-${path}" class="file-diff-inline">
                    ${lastDiffs[path] ? this.renderDiff(lastDiffs[path]) : '<div style="padding: 10px; color: #94a3b8;">Loading diff...</div>'}
                </div>
            `;
            list.appendChild(item);
        });
    },

    toggleFileDiff: function(path) {
        const item = document.querySelector(`.file-stack-item[data-path="${path}"]`);
        if (!item) return;
        item.classList.toggle('expanded');
        if (item.classList.contains('expanded')) {
            window.ChatApp.Actions.callJava('getDiff', '-1', path);
        }
    },

    renderDiff: function(diff) {
        let html = '';
        let left = 0, right = 0;
        diff.split('\n').forEach(line => {
            let cls = '';
            if (line.startsWith('+')) { cls = 'added'; right++; }
            else if (line.startsWith('-')) { cls = 'deleted'; left++; }
            else if (line.startsWith('@@')) { cls = 'hunk-header'; }
            else { left++; right++; }

            const num = line.startsWith('+') ? right : (line.startsWith('-') ? left : right);
            html += `<div class="diff-line ${cls}"><div class="line-num">${num}</div><div class="line-content">${window.ChatApp.Utils.escapeHtml(line)}</div></div>`;
        });
        return html;
    },

    selectFile: function(path) {
        const item = document.querySelector(`.file-stack-item[data-path="${path}"]`);
        if (!item) return;

        // Ensure panel is open when selecting a file
        window.ChatApp.UI.ensureSidePanelOpen();

        // Remove active-file from any other item
        document.querySelectorAll('.file-stack-item.active-file').forEach(el => el.classList.remove('active-file'));

        item.classList.add('active-file');
        item.scrollIntoView({ behavior: 'smooth', block: 'center' });

        // Optionally expand it if it's not already
        if (!item.classList.contains('expanded')) {
            this.toggleFileDiff(path);
        }
    }
};
