export class BranchActions {
    constructor(actions) {
        this.actions = actions;
    }

    render() {
        const container = document.createElement('div');
        container.className = 'branch-actions';
        this.actions.forEach(item => {
            const desc = typeof item === 'object' ? (item.description || (item.operation + " " + item.target)) : String(item);
            const div = document.createElement('div');
            div.className = 'branch-action-item';
            div.textContent = desc;
            container.appendChild(div);
        });
        return container;
    }
}
