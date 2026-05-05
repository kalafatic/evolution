/**
 * Bridge to Java backend via JavaHandler function.
 */
export const JavaBridge = {
    call(action, index = '-1', payload = '') {
        if (typeof window.JavaHandler === 'function') {
            try {
                // Ensure index is a string as expected by the backend bridge.
                // Use '-1' as default if index is null or undefined.
                const safeIndex = (index === null || index === undefined) ? '-1' : String(index);
                return window.JavaHandler(action, safeIndex, payload);
            } catch (e) {
                console.error(`JavaBridge call failed: ${action}`, e);
            }
        } else {
            console.warn(`JavaHandler not found. Action: ${action}, Index: ${index}, Payload: ${payload}`);
        }
        return null;
    }
};
