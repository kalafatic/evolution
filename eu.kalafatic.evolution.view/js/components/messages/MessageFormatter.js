import CodeFormatter from './formatters/CodeFormatter.js';
import MarkdownFormatter from './formatters/MarkdownFormatter.js';
import ThinkingFormatter from './formatters/ThinkingFormatter.js';

class MessageFormatter {
    constructor() {
        this.formatters = [
            ThinkingFormatter,
            CodeFormatter,
            MarkdownFormatter
        ];
    }

    format(text) {
        if (!text) return '';

        let formattedText = text;
        this.formatters.forEach(formatter => {
            formattedText = formatter.format(formattedText);
        });

        return formattedText;
    }
}

export default new MessageFormatter();
