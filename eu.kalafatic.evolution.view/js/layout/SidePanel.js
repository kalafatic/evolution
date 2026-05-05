import { JavaBridge } from '../core/JavaBridge.js';

export class SidePanel {
    constructor(panelId) {
        this.panel = document.getElementById(panelId);
        this.isOpen = true;
        this.m_pos = 0;
        this.setupResizer();
    }

    setupResizer() {
        const resize_el = document.getElementById("resize-handle");
        if (!resize_el) return;

        const resize = (e) => {
            const dx = this.m_pos - e.x;
            this.m_pos = e.x;
            const newWidth = (parseInt(getComputedStyle(this.panel, '').width) + dx);
            if (newWidth > 50) {
                this.panel.style.width = newWidth + "px";
            }
        };

        resize_el.addEventListener("mousedown", (e) => {
            this.m_pos = e.x;
            this.panel.style.transition = 'none';
            document.addEventListener("mousemove", resize, false);
            document.body.style.cursor = 'col-resize';
            e.preventDefault();
        }, false);

        document.addEventListener("mouseup", () => {
            this.panel.style.transition = 'width 0.3s ease';
            document.removeEventListener("mousemove", resize, false);
            document.body.style.cursor = '';
        }, false);
    }

    toggle() {
        if (this.isOpen) {
            this.panel.style.width = '0px';
            this.isOpen = false;
        } else {
            this.panel.style.width = '250px';
            this.isOpen = true;
        }
    }

    showContextMenu(e, path) {
        e.preventDefault();
        window.dispatchEvent(new CustomEvent('ui:contextFileChanged', { detail: path }));

        const menu = document.getElementById('context-menu');
        menu.style.display = 'block';
        menu.style.left = e.pageX + 'px';
        menu.style.top = e.pageY + 'px';

        const hideMenu = () => {
            menu.style.display = 'none';
            document.removeEventListener('click', hideMenu);
        };
        document.addEventListener('click', hideMenu);
    }

    menuAction(action, currentContextFile, lastDiff) {
        if (!currentContextFile) return;
        switch(action) {
            case 'workspace':
                JavaBridge.call('openInWorkspace', '-1', currentContextFile);
                break;
            case 'review':
                JavaBridge.call('openInReviewEditor', '-1', currentContextFile);
                break;
            case 'revert':
                if (confirm(`Are you sure you want to revert changes to ${currentContextFile}?`)) {
                    JavaBridge.call('revertFile', '-1', currentContextFile);
                }
                break;
            case 'copyPath':
                JavaBridge.call('copy', '-1', currentContextFile);
                break;
            case 'copyDiff':
                JavaBridge.call('copy', '-1', lastDiff || '');
                break;
        }
    }
}
