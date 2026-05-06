export class DiffViewer {
    static render(data) {
        const { diff } = data;
        const container = document.createElement('div');

        const lines = diff.split('\n');
        let leftLine = 0;
        let rightLine = 0;

        lines.forEach(line => {
            const div = document.createElement('div');
            div.className = 'diff-line';

            let content = line;
            if (line.startsWith('+')) {
                div.classList.add('added');
                rightLine++;
            } else if (line.startsWith('-')) {
                div.classList.add('deleted');
                leftLine++;
            } else if (line.startsWith('@@')) {
                div.classList.add('hunk-header');
            } else {
                leftLine++;
                rightLine++;
            }

            const lineNum = document.createElement('div');
            lineNum.className = 'line-num';
            lineNum.textContent = line.startsWith('+') ? rightLine : (line.startsWith('-') ? leftLine : rightLine);

            const lineContent = document.createElement('div');
            lineContent.className = 'line-content';
            lineContent.textContent = content;

            div.appendChild(lineNum);
            div.appendChild(lineContent);
            container.appendChild(div);
        });

        return container;
    }
}
