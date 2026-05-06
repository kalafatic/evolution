/**
 * Bridge to Java backend via JavaHandler function.
 */
export const JavaBridge = {
    /**
     * Bridge to Java backend with retry logic.
     */
    call(action, index = '-1', payload = '') {
        try {
            if (typeof window.JavaHandler === 'function') {
                // Ensure index is a string as expected by the backend bridge.
                const safeIndex = (index === null || index === undefined) ? '-1' : String(index);
                const safePayload = (payload === null || payload === undefined) ? '' : String(payload);

                return window.JavaHandler(action, safeIndex, safePayload);
            } else {
                console.log(`JavaHandler not ready, retrying call: ${action}`);
                setTimeout(() => this.call(action, index, payload), 100);
            }
        } catch (e) {
            console.error(`JavaBridge call failed: ${action}`, e);
        }
        return null;
    }
};
