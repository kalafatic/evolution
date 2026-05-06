class BranchActions {
    constructor(actions) {
        this.actions = actions || [];
    }

    render() {
        const container = document.createElement('div');
        container.className = 'branch-actions';

        const title = document.createElement('div');
        title.style.fontWeight = 'bold';
        title.style.marginBottom = '6px';
        title.textContent = 'actions:';
        container.appendChild(title);

        if (this.actions.length === 0) {
            const item = document.createElement('div');
            item.className = 'branch-action-item';
            item.textContent = 'Analysis only';
            container.appendChild(item);
        } else {
            this.actions.forEach(action => {
                const item = document.createElement('div');
                item.className = 'branch-action-item';
                item.textContent = action;
                container.appendChild(item);
            });
        }

        return container;
    }
}

export default BranchActions;
