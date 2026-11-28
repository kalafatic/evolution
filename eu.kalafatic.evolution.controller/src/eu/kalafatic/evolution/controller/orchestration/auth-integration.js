/**
 * Authentication & Session integration for Evolution Web UI.
 * Handles environment detection, session persistence, and fetch authorization.
 */


/**
 * Defense in depth for every EVO web page: never let a non-JSON response be parsed
 * as JSON. An unrelated HTTP server occupying an EVO port can answer HTTP 200 with
 * an HTML page; parsing that produced cryptic SyntaxErrors. This turns it into an
 * explicit, actionable backend-identity error at the call site.
 */
(function guardNonJsonParsing() {
    if (typeof Response === 'undefined' || !Response.prototype || !Response.prototype.json) return;
    const nativeJson = Response.prototype.json;
    Response.prototype.json = function() {
        const contentType = ((this.headers && this.headers.get('content-type')) || '').toLowerCase();
        if (!contentType.includes('json')) {
            // Consume the body before failing. Rejecting without reading it makes the
            // browser cancel the still-streaming response, which the server logs as a
            // connection reset; reading it lets the response complete cleanly.
            return this.text().catch(() => '').then(() => {
                throw new Error(
                    `Expected JSON from ${this.url} but received '${contentType || 'unknown'}' ` +
                    `(HTTP ${this.status}) — a different application may be occupying the EVO server port.`);
            });
        }
        return nativeJson.call(this);
    };
})();

function isSWTBrowser() {
    return typeof JavaHandler !== 'undefined' ||
           typeof JavaLog !== 'undefined' ||
           typeof JavaBridgeReady !== 'undefined';
}

function getSessionId() {
    let sid = localStorage.getItem('sessionId') || sessionStorage.getItem('sessionId');
    if (!sid || sid === 'null' || sid === 'undefined') {
        sid = 'ui-' + Math.random().toString(36).substring(2, 11) + '-' + Date.now();
        sessionStorage.setItem('sessionId', sid);
    }
    return sid;
}

/**
 * Intercept fetch calls to inject Authorization header and runtime parameters.
 */
(function() {
    const originalFetch = window.fetch;
    window.fetch = function(resource, config) {
        config = config || {};
        const sid = getSessionId();

        if (typeof resource === 'string') {
            if (resource.startsWith('/') && !resource.includes('sessionId=')) {
                const sep = resource.includes('?') ? '&' : '?';
                resource = resource + sep + 'sessionId=' + encodeURIComponent(sid);
            }
        }

        let headers = config.headers || {};
        if (headers instanceof Headers) {
            if (!headers.has('Authorization')) headers.set('Authorization', 'Bearer ' + sid);
            if (isSWTBrowser() || window.location.search.includes('runtime=SWT')) {
                headers.set('x-evo-runtime', 'SWT');
            }
        } else {
            if (!headers['Authorization'] && !headers['authorization']) {
                headers['Authorization'] = 'Bearer ' + sid;
            }
            if (isSWTBrowser() || window.location.search.includes('runtime=SWT')) {
                headers['x-evo-runtime'] = 'SWT';
            }
            config.headers = headers;
        }

        return originalFetch(resource, config);
    };
})();

async function checkAuthentication() {
    const sid = getSessionId();
    const badge = document.getElementById('global-session-badge');
    if (badge) {
        badge.innerText = 'Session: ' + sid.substring(0, 16) + '...';
    }

    if (window.isEvoAuthChecking) return;
    if (isSWTBrowser() || window.location.protocol === 'file:' || window.location.search.includes('runtime=SWT')) {
        return;
    }

    const currentPath = window.location.pathname;
    if (currentPath.endsWith('login.html') || currentPath.endsWith('dashboard.html')) return;

    window.isEvoAuthChecking = true;

    try {
        const response = await fetch('/api/auth/me', {
            method: 'GET',
            credentials: 'include'
        });

        if (response.ok) {
            const data = await response.json();
            if (data.sessionId) {
                sessionStorage.setItem('sessionId', data.sessionId);
                if (badge) badge.innerText = 'Session: ' + data.sessionId.substring(0, 16) + '...';
            }
        }
    } catch (error) {
        console.warn('Auth validation note:', error);
    } finally {
        window.isEvoAuthChecking = false;
    }
}

document.addEventListener('DOMContentLoaded', checkAuthentication);
