/**
 * Fetch interceptor to automatically add SWT runtime header for local API calls.
 */
(function() {
    const originalFetch = window.fetch;
    window.fetch = function(resource, config) {
        if (typeof isSWTEnvironment !== 'undefined' && isSWTEnvironment()) {
            let url = "";
            if (typeof resource === 'string') {
                url = resource;
            } else if (resource instanceof URL) {
                url = resource.href;
            } else if (resource && typeof resource === 'object' && resource.url) {
                url = resource.url;
            }

            const isLocal = url.startsWith('/') ||
                          url.startsWith(window.location.origin) ||
                          url.includes('localhost:') ||
                          url.includes('127.0.0.1:');

            if (isLocal) {
                if (resource instanceof Request) {
                    resource.headers.set('x-evo-runtime', 'SWT');
                } else {
                    config = config || {};
                    config.headers = config.headers || {};
                    if (config.headers instanceof Headers) {
                        config.headers.set('x-evo-runtime', 'SWT');
                    } else {
                        config.headers['x-evo-runtime'] = 'SWT';
                    }
                }
            }
        }
        return originalFetch(resource, config);
    };
})();

document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;
            const errorArea = document.getElementById('errorArea');
            const loader = document.getElementById('loader');
            const loginBtn = document.getElementById('loginBtn');

            errorArea.textContent = '';
            loader.style.display = 'block';
            loginBtn.disabled = true;

            try {
                const response = await fetch('/api/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, password }),
                    credentials: 'include'
                });

                const data = await response.json();
                if (data.success) {
                    const rememberMe = document.getElementById('rememberMe').checked;
                    if (rememberMe) {
                        localStorage.setItem('sessionId', data.sessionId);
                        sessionStorage.removeItem('sessionId');
                    } else {
                        sessionStorage.setItem('sessionId', data.sessionId);
                        localStorage.removeItem('sessionId');
                    }
                    window.location.href = '/dashboard.html';
                } else {
                    errorArea.textContent = data.message || 'Login failed';
                }
            } catch (error) {
                errorArea.textContent = 'Server error. Please try again.';
            } finally {
                loader.style.display = 'none';
                loginBtn.disabled = false;
            }
        });
    }

    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', async () => {
            const sessionId = localStorage.getItem('sessionId') || sessionStorage.getItem('sessionId');
            try {
                await fetch('/api/auth/logout', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${sessionId}`
                    }
                });
            } catch (error) {
                console.error('Logout failed', error);
            } finally {
                localStorage.removeItem('sessionId');
                sessionStorage.removeItem('sessionId');
                window.location.href = '/login.html';
            }
        });
    }

    if (window.location.pathname.endsWith('dashboard.html')) {
        loadUserInfo();
    }
});

function isSWTEnvironment() {
    return typeof JavaHandler !== 'undefined' ||
           typeof JavaLog !== 'undefined' ||
           typeof JavaBridgeReady !== 'undefined' ||
           window.location.search.includes('runtime=SWT') ||
           window.location.protocol === 'file:';
}

let isUserInfoLoading = false;

async function loadUserInfo() {
    if (isUserInfoLoading) return;
    const sessionId = localStorage.getItem('sessionId') || sessionStorage.getItem('sessionId');
    const headers = {};
    if (sessionId && sessionId !== 'null' && sessionId !== 'undefined') {
        headers['Authorization'] = `Bearer ${sessionId}`;
    } else {
        console.log("No sessionId in storage for user info, relying on cookies.");
    }

    try {
        isUserInfoLoading = true;
        const response = await fetch('/api/auth/me', {
            method: 'GET',
            headers: headers,
            credentials: 'include'
        });

        if (response.ok) {
            const data = await response.json();

            // Sync storage if needed
            if (!localStorage.getItem('sessionId') && !sessionStorage.getItem('sessionId')) {
                console.log("Syncing sessionId to sessionStorage from valid cookie.");
                sessionStorage.setItem('sessionId', data.sessionId);
            } else if (data.sessionId && data.sessionId !== localStorage.getItem('sessionId') && data.sessionId !== sessionStorage.getItem('sessionId')) {
                console.log("Session mismatch detected. Updating storage with server-provided ID.");
                if (localStorage.getItem('sessionId')) {
                    localStorage.setItem('sessionId', data.sessionId);
                } else {
                    sessionStorage.setItem('sessionId', data.sessionId);
                }
            }

            const setEl = (id, val) => {
                const el = document.getElementById(id);
                if (el) el.textContent = val;
            };

            setEl('displayUsername', data.username);
            setEl('displayRole', data.role);
            setEl('displaySessionId', data.sessionId);
            setEl('displayTimestamp', data.loginTimestamp);
            setEl('displayWorkflow', data.workflowType || 'GENERAL');

        } else if (response.status === 401) {
            console.warn('Unauthorized detected.');
            if (!isSWTEnvironment()) {
                console.warn('Redirecting to login.');
                localStorage.removeItem('sessionId');
                sessionStorage.removeItem('sessionId');
                window.location.href = '/login.html';
            } else {
                console.log('SWT Environment: Suppressing redirect.');
            }
        } else {
            console.error('Failed to load user info with status:', response.status);
        }
    } catch (error) {
        console.error('Network error while loading user info:', error);
    } finally {
        isUserInfoLoading = false;
    }
}
