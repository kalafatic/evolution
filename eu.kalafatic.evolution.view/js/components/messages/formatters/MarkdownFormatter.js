class MarkdownFormatter {
    static format(text) {
        let html = text;

        // Bold
        html = html.replace(/\*\*([\s\S]*?)\*\*/g, '<strong>$1</strong>');

        // Italic
        html = html.replace(/\*([\s\S]*?)\*/g, '<em>$1</em>');

        // Inline Code
        html = html.replace(/`([^`]+)`/g, '<code>$1</code>');

        // Lists
        html = html.replace(/^\s*[\-\*]\s+(.*)$/gm, '<li>$1</li>');
        html = html.replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>');

        // File Paths (Interactive)
        html = html.replace(/([a-zA-Z0-9_\-\.\/]+\.(java|js|html|css|xml|md|pom))/g, (match) => {
            return `<a onclick="window.dispatchEvent(new CustomEvent('java:openDiff', { detail: '${match}' }))">${match}</a>`;
        });

        // AI Platform Interaction Links
        html = html.replace(/\[(CREATE|MODIFY|DELETE|ADD|UPDATE|PATCH)\s+FILE:\s*([^\]]+)\]/gi, (match, op, path) => {
            return `<a class="link-go" onclick="window.dispatchEvent(new CustomEvent('java:executeProposal', { detail: '${path.trim()}' }))">${op} ${path.trim()}</a>`;
        });

        html = html.replace(/CLARIFY:/g, '<a class="link-clarify" onclick="window.dispatchEvent(new CustomEvent('java:clarify'))">CLARIFY:</a>');

        // Conversational Clarify
        html = html.replace(/(tell me a bit more|more information|could you clarify|please clarify)/gi, (match) => {
            return `<a class="link-clarify" onclick="window.dispatchEvent(new CustomEvent('java:clarify'))">${match}</a>`;
        });

        // Line Breaks
        html = html.replace(/\n/g, '<br>');

        return html;
    }
}

export default MarkdownFormatter;
