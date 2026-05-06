class MarkdownFormatter {
    static format(text) {
        if (!text) return "";
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

        // File Paths
        html = html.replace(/([a-zA-Z0-9_\-\.\/]+\.(java|js|html|css|xml|md|pom))/g, '<a onclick="window.dispatchEvent(new CustomEvent(\'java:openDiff\', { detail: \'$1\' }))">$1</a>');

        // AI Interaction
        html = html.replace(/\[(CREATE|MODIFY|DELETE|ADD|UPDATE|PATCH)\s+FILE:\s*([^\]]+)\]/gi, '<a class="link-go" onclick="window.dispatchEvent(new CustomEvent(\'java:executeProposal\', { detail: \'$2\' }))">$1 $2</a>');

        html = html.replace(/CLARIFY:/g, '<a class="link-clarify" onclick="window.dispatchEvent(new CustomEvent(\'java:clarify\'))">CLARIFY:</a>');

        html = html.replace(/(tell me a bit more|more information|could you clarify|please clarify)/gi, '<a class="link-clarify" onclick="window.dispatchEvent(new CustomEvent(\'java:clarify\'))">$1</a>');

        // Line Breaks
        html = html.replace(/\n/g, '<br>');

        return html;
    }
}

export default MarkdownFormatter;
