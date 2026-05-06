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

        // Line Breaks
        html = html.replace(/\n/g, '<br>');

        return html;
    }
}

export default MarkdownFormatter;
