import BranchHeader from './BranchHeader.js';
import BranchActions from './BranchActions.js';
import BranchFooter from './BranchFooter.js';

class BranchColumn {
    constructor(vData, vIndex, mIndex, isWaiting, isApproved, approvedVariantId) {
        this.vData = vData;
        this.vIndex = vIndex;
        this.mIndex = mIndex;
        this.isWaiting = isWaiting;
        this.isApproved = isApproved;
        this.approvedVariantId = approvedVariantId;
    }

    render() {
        const column = document.createElement('div');
        column.className = 'branch-column';

        const isThisApproved = this.isApproved && String(this.vIndex) === String(this.approvedVariantId);

        if (isThisApproved) {
            column.classList.add('approved');
        } else if (this.isApproved) {
            column.classList.add('rejected');
        }

        const header = new BranchHeader(this.vIndex, this.vData.confidence);
        const actions = new BranchActions(this.vData.actions);
        const footer = new BranchFooter(this.vIndex, this.mIndex, this.isWaiting, this.isApproved, isThisApproved);

        column.appendChild(header.render());

        const strategy = document.createElement('div');
        strategy.className = 'branch-strategy';
        strategy.textContent = this.vData.strategy || 'Adaptive Proposal';
        column.appendChild(strategy);

        column.appendChild(actions.render());

        if (this.vData.content) {
            const content = document.createElement('div');
            content.className = 'branch-content';
            content.style.fontSize = '11px';
            content.style.marginTop = '8px';
            content.innerHTML = `<strong>content:</strong> ${this.vData.content}`;
            column.appendChild(content);
        }

        column.appendChild(footer.render());

        return column;
    }
}

export default BranchColumn;
