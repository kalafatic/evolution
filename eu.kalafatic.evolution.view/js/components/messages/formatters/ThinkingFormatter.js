class ThinkingFormatter {
    static format(text) {
        // Handle <think> tags or reasoning blocks
        return text.replace(/<think>([\s\S]*?)<\/think>/gi, (match, content) => {
            return `<div class="think-block">${content.trim()}</div>`;
        });
    }
}

export default ThinkingFormatter;
