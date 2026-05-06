import BranchColumn from './BranchColumn.js';

class DarwinContainer {
    constructor(messageData) {
        this.data = messageData;
    }

    render() {
        const { text, agentType, index: mIndex } = this.data;
        const role = (agentType || '').toLowerCase();

        // Darwin state logic: A variant is "waiting" if the parent message role contains "waiting"
        // and it hasn't been approved yet.
        const isWaiting = role.includes('waiting') && !role.includes('approved');
        const isApproved = role.includes('approved');

        let approvedVariantId = null;
        if (isApproved && role.includes(':')) {
            approvedVariantId = role.split(':').pop().trim();
        }

        const container = document.createElement('div');
        container.className = 'branch-container darwin-container';

        try {
            let jsonText = text.trim();
            // Handle markdown code blocks
            if (jsonText.startsWith('```')) {
                jsonText = jsonText.replace(/^```[a-z]*\n?/i, '').replace(/\n?```$/i, '').trim();
            }

            let data;
            try {
                data = JSON.parse(jsonText);
            } catch (e) {
                // Fallback: search for JSON array pattern
                const jsonMatch = jsonText.match(/\[\s*\{[\s\S]*\}\s*\]/);
                if (jsonMatch) {
                    data = JSON.parse(jsonMatch[0]);
                } else {
                    throw e;
                }
            }

            const variants = Array.isArray(data) ? data : (data.variants || data.proposals || []);

            variants.forEach((v, index) => {
                const column = new BranchColumn(v, index, mIndex, isWaiting, isApproved, approvedVariantId);
                container.appendChild(column.render());
            });

        } catch (e) {
            console.error('Failed to parse Darwin branches', e);
            const error = document.createElement('div');
            error.className = 'agent-block error';
            error.textContent = 'Failed to parse Darwin branches: ' + e.message;
            return error;
        }

        return container;
    }
}

export default DarwinContainer;
