window.ChatApp = window.ChatApp || {};

window.ChatApp.Utils = {
    escapeHtml: function(unsafe) {
        if (typeof unsafe !== 'string') return String(unsafe);
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    },

    escapeJs: function(text) {
        if (!text) return "";
        return text.replace(/\\/g, '\\\\')
                   .replace(/'/g, "\\'")
                   .replace(/"/g, "&quot;")
                   .replace(/`/g, "\\`")
                   .replace(/\n/g, '\\n')
                   .replace(/\r/g, '');
    },

    stripTechnicalMarkers: function(text) {
        if (!text) return "";
        return text.replace(/\[KERNEL\]/g, '')
                   .replace(/\[STRATEGY\]/g, '')
                   .replace(/\[ANALYSIS\]/g, '')
                   .replace(/\[DIAGNOSIS\]/g, '')
                   .replace(/\[SUPERVISOR\]/g, '')
                   .replace(/\[EVO\]/g, '')
                   .replace(/\[DARWIN\]/g, '')
                   .replace(/\[DARWINENGINE\]/g, '')
                   .replace(/\[THINKING\]/g, '')
                   .replace(/\[ORCHESTRATOR\]/g, '')
                   .replace(/\[AUDIT\]/g, '')
                   .trim();
    }
};
