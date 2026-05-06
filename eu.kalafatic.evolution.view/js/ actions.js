(function(ns) {

    ns.actions = {

        callJava(action, index, text) {
            try {
                if (window.JavaHandler) {
                    JavaHandler(action, String(index ?? ""), text ?? "");
                } else {
                    console.log("JavaHandler not ready, retry...");
                    setTimeout(() => {
                        ns.actions.callJava(action, index, text);
                    }, 50);
                }
            } catch (e) {
                console.error("Java call failed", e);
            }
        }

    };

})(window.Chat = window.Chat || {});