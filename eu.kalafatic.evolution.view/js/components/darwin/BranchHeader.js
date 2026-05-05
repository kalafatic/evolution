export class BranchHeader {
    constructor(index) {
        this.index = index;
    }

    render() {
        const header = document.createElement('div');
        header.className = 'branch-header';
        header.innerHTML = `<span>PROPOSAL ${this.index + 1}</span>`;
        return header;
    }
}
