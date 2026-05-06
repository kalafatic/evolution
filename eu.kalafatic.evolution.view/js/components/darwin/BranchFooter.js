import { MessageFormatter } from '../messages/MessageFormatter.js';

export class BranchFooter {
    constructor(v, mIndex, vId, isWaiting, isThisApproved, isThisRejected) {
        this.v = v;
        this.mIndex = mIndex;
        this.vId = vId;
        this.isWaiting = isWaiting;
        this.isThisApproved = isThisApproved;
        this.isThisRejected = isThisRejected;
    }

    render() {
        const footer = document.createElement('div');
        footer.className = 'branch-footer';

        if (this.v.isApproved || this.isThisApproved) {
            footer.innerHTML = `<div style="color: #16a34a; font-weight: bold; width: 100%; text-align: center;">APPROVED</div>`;
        } else if (this.isThisRejected) {
            footer.innerHTML = `<div style="color: #dc2626; font-weight: bold; width: 100%; text-align: center;">REJECTED</div>`;
        } else {
            const approveBtn = this.isWaiting ? `<button class="branch-btn approve" onclick="event.stopPropagation(); window.dispatchEvent(new CustomEvent('java:approveDarwinVariant', {detail: {index: ${this.mIndex}, variantId: '${this.vId}'}}))">Approve</button>` : '';
            footer.innerHTML = `
                <button class="branch-btn" onclick="window.dispatchEvent(new CustomEvent('java:copy', {detail: \`${MessageFormatter.escapeJs(JSON.stringify(this.v))}\`}))">Copy</button>
                ${approveBtn}
            `;
        }
        return footer;
    }
}
