/**
 * Bridge to Java backend via JavaHandler function.
 */
export const JavaBridge = {
    call(action, index = '-1', payload = '') {
        if (typeof window.JavaHandler === 'function') {
            try {
                // Ensure index is a string as expected by the backend bridge
                return window.JavaHandler(action, String(index), payload);
            } catch (e) {
                console.error(`JavaBridge call failed: ${action}`, e);
            }
        } else {
            console.warn(`JavaHandler not found. Action: ${action}, Index: ${index}, Payload: ${payload}`);
        }
        return null;
    }
};
