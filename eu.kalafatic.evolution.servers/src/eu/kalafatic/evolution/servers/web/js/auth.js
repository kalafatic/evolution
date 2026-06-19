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

async function loadUserInfo() {
    const sessionId = localStorage.getItem('sessionId') || sessionStorage.getItem('sessionId');
    const headers = {};
    if (sessionId) {
        headers['Authorization'] = `Bearer ${sessionId}`;
    } else {
        console.log("No sessionId in storage for user info, relying on cookies.");
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
                console.log("Syncing sessionId to sessionStorage from valid cookie.");
                sessionStorage.setItem('sessionId', data.sessionId);
            }

            document.getElementById('displayUsername').textContent = data.username;
            document.getElementById('displayRole').textContent = data.role;
            document.getElementById('displaySessionId').textContent = data.sessionId;
            document.getElementById('displayTimestamp').textContent = data.loginTimestamp;

            const workflowEl = document.getElementById('displayWorkflow');
            if (workflowEl) {
                workflowEl.textContent = data.workflowType || 'GENERAL';
            }
        } else {
            localStorage.removeItem('sessionId');
            sessionStorage.removeItem('sessionId');
            window.location.href = '/login.html';
        }
    } catch (error) {
        console.error('Failed to load user info', error);
        localStorage.removeItem('sessionId');
        sessionStorage.removeItem('sessionId');
        window.location.href = '/login.html';
    }
}
