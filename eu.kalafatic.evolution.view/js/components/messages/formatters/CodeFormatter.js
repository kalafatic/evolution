class CodeFormatter {
    static format(text) {
        return text.replace(/```([a-z]*)\n?([\s\S]*?)\n?```/gi, (match, lang, code) => {
            return `<pre><code class="language-${lang}">${this.escapeHtml(code.trim())}</code></pre>`;
        });
    }

    static escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

export default CodeFormatter;
