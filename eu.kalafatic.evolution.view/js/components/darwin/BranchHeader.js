class BranchHeader {
    constructor(index, confidence) {
        this.index = index;
        this.confidence = confidence;
    }

    render() {
        const header = document.createElement('div');
        header.className = 'branch-header';
        header.innerHTML = `<span>PROPOSAL ${this.index + 1}</span>`;
        if (this.confidence) {
            header.innerHTML += `<span style="font-size: 10px; opacity: 0.6;">${this.confidence}% Match</span>`;
        }
        return header;
    }
}

export default BranchHeader;
