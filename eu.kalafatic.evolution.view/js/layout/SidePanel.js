import JavaBridge from '../core/JavaBridge.js';

class SidePanel {
    constructor(panelId) {
        this.panel = document.getElementById(panelId);
        this.contextMenu = document.getElementById('context-menu');
    }

    toggle() {
        if (!this.panel) return;
        const currentWidth = this.panel.style.width;
        if (currentWidth === '0px' || currentWidth === '0') {
            this.panel.style.width = '320px';
        } else {
            this.panel.style.width = '0px';
        }
    }

    showContextMenu(e, path) {
        if (!this.contextMenu) return;
        e.preventDefault();
        window.dispatchEvent(new CustomEvent('ui:contextFileChanged', { detail: path }));

        this.contextMenu.style.display = 'block';
        this.contextMenu.style.left = e.clientX + 'px';
        this.contextMenu.style.top = e.clientY + 'px';

        const closeMenu = () => {
            this.contextMenu.style.display = 'none';
            document.removeEventListener('click', closeMenu);
        };
        setTimeout(() => document.addEventListener('click', closeMenu), 10);
    }

    menuAction(action, path, diff) {
        if (!path) return;

        // Strip status prefix if present
        const relativePath = path.length > 2 && (path.startsWith('M ') || path.startsWith('A ') || path.startsWith('D ')) ? path.substring(2) : path;

        switch (action) {
            case 'workspace':
                JavaBridge.call('openInWorkspace', '-1', relativePath);
                break;
            case 'review':
                JavaBridge.call('openInReviewEditor', '-1', relativePath);
                break;
            case 'revert':
                if (confirm('Revert all changes to ' + path + '?')) {
                    JavaBridge.call('revertFile', '-1', path);
                }
                break;
            case 'copyPath':
                JavaBridge.call('copy', '-1', relativePath);
                break;
            case 'copyDiff':
                if (diff) JavaBridge.call('copy', '-1', diff);
                break;
        }
    }
}

export default SidePanel;
