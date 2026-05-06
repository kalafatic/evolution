const JavaBridge = {
    call(action, index, payload) {
        if (window.JavaHandler) {
            // Safe index handling: if index is not provided, use "-1"
            const safeIndex = (index === undefined || index === null) ? "-1" : String(index);
            // Ensure arguments are strings as JavaHandler expects
            window.JavaHandler(String(action), safeIndex, payload !== undefined ? String(payload) : "");
        } else {
            console.warn('JavaHandler not found for action:', action);
        }
    }
};

export default JavaBridge;
