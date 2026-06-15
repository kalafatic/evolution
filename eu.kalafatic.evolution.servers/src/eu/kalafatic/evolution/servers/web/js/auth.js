document.addEventListener('DOMContentLoaded', () => {
    const themeToggle = document.getElementById('themeToggle');
    const savedTheme = localStorage.getItem('theme') || 'light';
    document.documentElement.setAttribute('data-theme', savedTheme);

    if (themeToggle) {
        themeToggle.addEventListener('click', () => {
            const currentTheme = document.documentElement.getAttribute('data-theme');
            const newTheme = currentTheme === 'light' ? 'dark' : 'light';
            document.documentElement.setAttribute('data-theme', newTheme);
            localStorage.setItem('theme', newTheme);
        });
    }

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
                    body: JSON.stringify({ username, password })
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
    if (!sessionId) {
        window.location.href = '/login.html';
        return;
    }

    try {
        const response = await fetch('/api/auth/me', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${sessionId}`
            }
        });

        if (response.ok) {
            const data = await response.json();
            document.getElementById('displayUsername').textContent = data.username;
            document.getElementById('displayRole').textContent = data.role;
            document.getElementById('displaySessionId').textContent = data.sessionId;
            document.getElementById('displayTimestamp').textContent = data.loginTimestamp;
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
