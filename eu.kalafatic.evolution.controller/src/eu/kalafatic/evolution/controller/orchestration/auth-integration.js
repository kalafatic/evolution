/**
 * Authentication integration for Evolution Web UI.
 * Handles environment detection and session verification.
 */

function isSWTBrowser() {
    // Detect Eclipse SWT Browser environment by checking for injected Java bridges
    return typeof JavaHandler !== 'undefined' || typeof JavaLog !== 'undefined';
}

async function checkAuthentication() {
    if (isSWTBrowser() || window.location.protocol === 'file:' || window.location.search.includes('runtime=SWT')) {
        console.log("SWT environment or runtime bypass detected. Skipping authentication.");
        return;
    }

    const currentPath = window.location.pathname;
    if (currentPath.endsWith('login.html')) {
        return;
    }

    const sessionId = localStorage.getItem('sessionId') || sessionStorage.getItem('sessionId');
    const headers = {};
    if (sessionId) {
        headers['Authorization'] = `Bearer ${sessionId}`;
    }

    try {
        const response = await fetch('/api/auth/me', {
            method: 'GET',
            headers: headers
        });

        if (!response.ok) {
            console.log("Unauthorized or session expired. Redirecting to login.");
            window.location.href = '/login.html';
        }
    } catch (error) {
        console.error('Authentication check failed:', error);
        // If the server is down or auth endpoint is missing, we might still want to redirect
        // but for robustness in dev we only redirect on 401/403.
    }
}

// Auto-run on load
document.addEventListener('DOMContentLoaded', () => {
    checkAuthentication();
});
