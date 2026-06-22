/**
 * Authentication integration for Evolution Web UI.
 * Handles environment detection and session verification.
 */

function isSWTBrowser() {
    return typeof JavaHandler !== 'undefined' || typeof JavaLog !== 'undefined';
}

async function checkAuthentication() {
    if (window.isEvoAuthChecking) return;
    if (isSWTBrowser() || window.location.protocol === 'file:' || window.location.search.includes('runtime=SWT')) {
        return;
    }

    const currentPath = window.location.pathname;
    // Skip login and dashboard (dashboard handled by auth.js)
    if (currentPath.endsWith('login.html') || currentPath.endsWith('dashboard.html')) return;

    window.isEvoAuthChecking = true;

    const sessionId = localStorage.getItem('sessionId') || sessionStorage.getItem('sessionId');
    const headers = {};
    if (sessionId && sessionId !== 'null' && sessionId !== 'undefined') {
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
        } else if (response.status === 401) {
            // If we sent a sessionId and it failed, clear it and try one more time (maybe cookie works)
            if (sessionId && sessionId !== 'null' && sessionId !== 'undefined') {
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
            console.warn('Unauthorized. Redirecting to login.');
            window.location.href = '/login.html';
        }
    } catch (error) {
        console.error('Auth check failed:', error);
    } finally {
        window.isEvoAuthChecking = false;
    }
}

document.addEventListener('DOMContentLoaded', checkAuthentication);
