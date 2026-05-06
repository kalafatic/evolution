import { BranchHeader } from './BranchHeader.js';
import { BranchActions } from './BranchActions.js';
import { BranchFooter } from './BranchFooter.js';

export class BranchColumn {
    constructor(v, index, mIndex, isWaiting, isApproved, approvedVariantId) {
        this.v = v;
        this.index = index;
        this.mIndex = mIndex;
        this.isWaiting = isWaiting;
        this.isApproved = isApproved;
        this.approvedVariantId = approvedVariantId;
    }

    render() {
        const vId = String(this.v.id || this.index);
        const isThisApproved = this.isApproved && (this.approvedVariantId === null || this.approvedVariantId === vId);
        const isThisRejected = this.isApproved && this.approvedVariantId !== null && this.approvedVariantId !== vId;

        const column = document.createElement('div');
        column.className = 'branch-column' + (this.v.isBest ? ' best' : '') +
                         (this.isWaiting ? ' waiting' : '') +
                         (isThisApproved ? ' approved' : '') +
                         (isThisRejected ? ' rejected' : '');

        column.appendChild(new BranchHeader(this.index).render());

        const strategy = document.createElement('div');
        strategy.className = 'branch-strategy';
        strategy.innerText = this.v.strategy || this.v.id || 'Variant';
        column.appendChild(strategy);

        const body = document.createElement('div');
        body.style.fontSize = '12px';
        body.style.display = 'flex';
        body.style.flexDirection = 'column';
        body.style.gap = '4px';

        const fields = ['id', 'strategy', 'score', 'suffix', 'actions', 'hypothesis', 'expected_effects', 'expected_effect', 'short_term', 'long_term', 'risk', 'reversibility'];

        fields.forEach(f => {
            if (this.v[f] && f !== 'strategy') {
                body.appendChild(this.renderPair(f, this.v[f]));
            }
        });

        Object.entries(this.v).forEach(([k, val]) => {
            if (!fields.includes(k)) {
                body.appendChild(this.renderPair(k, val));
            }
        });

        column.appendChild(body);
        column.appendChild(new BranchFooter(this.v, this.mIndex, vId, this.isWaiting, isThisApproved, isThisRejected).render());

        return column;
    }

    renderPair(key, val) {
        const container = document.createElement('div');
        container.style.marginBottom = '4px';

        const label = document.createElement('span');
        label.style.fontWeight = '800';
        label.style.opacity = '0.9';
        label.style.fontSize = '11px';
        label.style.textTransform = 'lowercase';
        label.textContent = `${key}: `;
        container.appendChild(label);

        if (key === 'actions' && Array.isArray(val)) {
            container.appendChild(new BranchActions(val).render());
        } else if (Array.isArray(val)) {
            const ul = document.createElement('ul');
            ul.style.margin = '2px 0';
            ul.style.paddingLeft = '18px';
            ul.style.listStyleType = 'disc';
            val.forEach(item => {
                const li = document.createElement('li');
                li.style.marginBottom = '2px';
                if (typeof item === 'object') {
                    li.appendChild(this.renderObject(item));
                } else {
                    li.textContent = String(item);
                }
                ul.appendChild(li);
            });
            container.appendChild(ul);
        } else if (typeof val === 'object') {
            container.appendChild(this.renderObject(val));
        } else {
            const span = document.createElement('span');
            span.style.fontWeight = '500';
            span.textContent = String(val);
            container.appendChild(span);
        }
        return container;
    }

    renderObject(obj) {
        const div = document.createElement('div');
        div.style.paddingLeft = '12px';
        div.style.borderLeft = '2px solid rgba(255,255,255,0.2)';
        div.style.marginTop = '4px';
        Object.entries(obj).forEach(([k, v]) => {
            div.appendChild(this.renderPair(k, v));
        });
        return div;
    }
}
