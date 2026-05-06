(function(ns) {

    ns.app = {

        queue: [],
        ready: false,

        updateMessages(messages) {
            if (!this.ready) {
                this.queue = messages;
                console.log("Queueing messages...");
                return;
            }

            ns.renderer.render(messages);
        },

        init() {
            console.log("Chat UI init");

            this.ready = true;

            if (this.queue.length) {
                ns.renderer.render(this.queue);
                this.queue = [];
            }

            // optional: notify Java
            ns.actions.callJava("ready");
        }
    };

    // expose to Java
    window.updateMessages = function(messages) {
        ns.app.updateMessages(messages);
    };

    // bootstrap
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", () => ns.app.init());
    } else {
        ns.app.init();
    }

})(window.Chat = window.Chat || {});