(function(ns) {
    ns.utils = {
        escapeHtml(text) {
            if (!text) return "";
            return text
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;");
        }
    };
})(window.Chat = window.Chat || {});