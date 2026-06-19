/**
 * Authentication integration for Evolution Web UI.
 * Handles environment detection and session verification.
 */

function isSWTBrowser() {
    return typeof JavaHandler !== 'undefined' || typeof JavaLog !== 'undefined';
}

async function checkAuthentication() {
    if (isSWTBrowser() || window.location.protocol === 'file:' || window.location.search.includes('runtime=SWT')) {
        return;
    }

    const currentPath = window.location.pathname;
    if (currentPath.endsWith('login.html')) return;

    const sessionId = localStorage.getItem('sessionId') || sessionStorage.getItem('sessionId');
    const headers = {};
    if (sessionId) {
        headers['Authorization'] = `Bearer ${sessionId}`;
    }

    try {
        const response = await fetch('/api/auth/me', {
            method: 'GET',
            headers: headers,
            credentials: 'include'
        });

        if (response.ok) {
            const data = await response.json();
            // Sync storage if needed
            if (!localStorage.getItem('sessionId') && !sessionStorage.getItem('sessionId')) {
                sessionStorage.setItem('sessionId', data.sessionId);
            }
        } else {
            // If we sent a sessionId and it failed, clear it and try one more time (maybe cookie works)
            if (sessionId) {
                console.log("Stored session ID invalid. Clearing and retrying with cookies...");
                localStorage.removeItem('sessionId');
                sessionStorage.removeItem('sessionId');
                // Second attempt will rely only on cookies
                const retryResponse = await fetch('/api/auth/me', { method: 'GET', credentials: 'include' });
                if (retryResponse.ok) {
                    const data = await retryResponse.json();
                    sessionStorage.setItem('sessionId', data.sessionId);
                    return;
                }
            }
            window.location.href = '/login.html';
        }
    } catch (error) {
        console.error('Auth check failed:', error);
    }
}

document.addEventListener('DOMContentLoaded', checkAuthentication);
