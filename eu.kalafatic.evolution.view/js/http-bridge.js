(function() {
    // This script bridges the gap when running in a standard browser instead of SWT Browser.
    // It provides JavaHandler/JavaLog mocks and handles message polling.

    if (window.JavaHandler) {
        console.log("SWT Bridge detected, skipping HTTP Bridge.");
        return;
    }

    console.log("HTTP Bridge initializing for standard browser...");

    function getBaseUrl() {
        if (window.location.protocol === 'file:') {
            return 'http://localhost:48080';
        }
        return '';
    }

    // Mock JavaLog
    window.JavaLog = function(msg) {
        console.log("[JAVA LOG] " + msg);
    };

    // Mock JavaHandler for browser-to-server actions
    window.JavaHandler = async function(action, index, text) {
        console.log("Action: " + action + ", index: " + index + ", text: " + text);
        const baseUrl = getBaseUrl();
        const sessionId = (window.ChatApp && window.ChatApp.lastSessionId) ? window.ChatApp.lastSessionId : 'Default';

        try {
            switch(action) {
                case 'ready':
                    // Just a handshake
                    break;
                case 'approve':
                case 'create':
                    await fetch(`${baseUrl}/conversation/${sessionId}/approve?runtime=HTTP`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ approved: true })
                    });
                    break;
                case 'approveDarwinVariant':
                    await fetch(`${baseUrl}/task?runtime=HTTP`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ prompt: `Approve variant ${text}`, sessionId: sessionId })
                    });
                    break;
                case 'rejectDarwinVariant':
                    await fetch(`${baseUrl}/task?runtime=HTTP`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ prompt: `Reject variant ${text}`, sessionId: sessionId })
                    });
                    break;
                case 'keepDarwinVariant':
                    await fetch(`${baseUrl}/task?runtime=HTTP`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ prompt: `Keep variant ${text}`, sessionId: sessionId })
                    });
                    break;
                case 'executeProposal':
                case 'helloworld':
                case 'forceSolution':
                    let promptText = text;
                    if (action === 'helloworld') promptText = "Execute the simplest working solution.";
                    if (action === 'forceSolution') promptText = "Force Solution";

                    await fetch(`${baseUrl}/task?runtime=HTTP`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ prompt: promptText, sessionId: sessionId })
                    });
                    break;
                case 'copy':
                    navigator.clipboard.writeText(text);
                    break;
                default:
                    console.warn("Unhandled action in HTTP Bridge: " + action);
            }
        } catch (err) {
            console.error("Failed to execute action via HTTP Bridge:", err);
        }
    };

    // Message Polling
    let lastSequenceNumber = -1;
    let isPolling = false;

    async function pollMessages() {
        if (isPolling || !window.ChatApp || !window.updateMessages) return;
        const sessionId = window.ChatApp.lastSessionId || 'Default';
        if (!sessionId) return;

        isPolling = true;
        try {
            const baseUrl = getBaseUrl();
            const response = await fetch(`${baseUrl}/server/conversation/${sessionId}?runtime=HTTP`);
            const messages = await response.json();

            if (messages && Array.isArray(messages) && messages.length > 0) {
                const maxSeq = Math.max(...messages.map(m => m.sequenceNumber || 0));
                const currentMsgCount = (window.ChatApp.state && window.ChatApp.state.messages) ? window.ChatApp.state.messages.length : 0;

                if (maxSeq > lastSequenceNumber || messages.length !== currentMsgCount) {
                    window.updateMessages(messages, sessionId);
                    lastSequenceNumber = maxSeq;
                }
            }
        } catch (err) {
            // console.error("Polling error in HTTP Bridge:", err);
        } finally {
            isPolling = false;
        }
    }

    setInterval(pollMessages, 1000);

})();
