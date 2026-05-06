class BranchFooter {
    constructor(vIndex, mIndex, isWaiting, isApproved, isThisApproved) {
        this.vIndex = vIndex;
        this.mIndex = mIndex;
        this.isWaiting = isWaiting;
        this.isApproved = isApproved;
        this.isThisApproved = isThisApproved;
    }

    render() {
        const footer = document.createElement('div');
        footer.className = 'branch-footer';

        if (this.isWaiting) {
            const copyBtn = document.createElement('button');
            copyBtn.className = 'branch-btn';
            copyBtn.textContent = 'Copy';
            copyBtn.onclick = () => window.dispatchEvent(new CustomEvent('java:copy', { detail: `Proposal ${this.vIndex + 1}` }));

            const approveBtn = document.createElement('button');
            approveBtn.className = 'branch-btn approve';
            approveBtn.textContent = 'Approve';
            approveBtn.onclick = () => window.dispatchEvent(new CustomEvent('java:approveDarwinVariant', { detail: { index: this.mIndex, variantId: this.vIndex } }));

            footer.appendChild(copyBtn);
            footer.appendChild(approveBtn);
        } else if (this.isApproved) {
            const status = document.createElement('div');
            status.style.textAlign = 'center';
            status.style.width = '100%';
            status.style.fontWeight = 'bold';
            status.textContent = this.isThisApproved ? 'APPROVED' : 'REJECTED';
            footer.appendChild(status);
        } else {
            const copyBtn = document.createElement('button');
            copyBtn.className = 'branch-btn';
            copyBtn.textContent = 'Copy';
            copyBtn.style.flex = '1';
            copyBtn.onclick = () => window.dispatchEvent(new CustomEvent('java:copy', { detail: `Proposal ${this.vIndex + 1}` }));
            footer.appendChild(copyBtn);
        }

        return footer;
    }
}

export default BranchFooter;
