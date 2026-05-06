const JavaBridge = {
    call(action, index, payload) {
        if (window.JavaHandler) {
            // Ensure arguments are strings as JavaHandler expects
            window.JavaHandler(String(action), String(index), payload !== undefined ? String(payload) : "");
        } else {
            console.warn('JavaHandler not found for action:', action);
        }
    }
};

export default JavaBridge;
