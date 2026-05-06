(function(ns) {

    ns.renderer = {
        render(messages) {
            const root = document.getElementById("chat");
            if (!root) return;

            root.innerHTML = "";

            messages.forEach(m => {
                const div = document.createElement("div");

                let type = m.agentType || "ai";
                div.className = "msg " + type;

                if (type.includes("waiting")) {
                    div.classList.add("waiting");
                }

                div.innerHTML = `
                    <div><b>${ns.utils.escapeHtml(m.sender)}</b></div>
                    <div>${ns.utils.escapeHtml(m.text)}</div>
                    <div class="meta">${m.timestamp || ""}</div>
                `;

                root.appendChild(div);
            });

            root.scrollTop = root.scrollHeight;
        }
    };

})(window.Chat = window.Chat || {});