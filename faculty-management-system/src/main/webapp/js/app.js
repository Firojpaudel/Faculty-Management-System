document.addEventListener('DOMContentLoaded', () => {

    // Check if already logged in
    const token = localStorage.getItem('jwt_token');
    const role = localStorage.getItem('user_role');

    if (token && role) {
        showDashboard(role);
    }

    // Login Form Handler
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;
            const errorDiv = document.getElementById('login-error');
            const btn = loginForm.querySelector('button');

            btn.innerHTML = 'Signing in...';
            btn.disabled = true;
            errorDiv.textContent = '';

            try {
                // In a real environment, this connects to the servlet /api/v1/auth/login
                const response = await fetch('/api/v1/auth/login', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ email, password })
                });

                const result = await response.json();

                if (result.success) {
                    localStorage.setItem('jwt_token', result.data.token);
                    localStorage.setItem('user_role', result.data.role);
                    showDashboard(result.data.role);
                } else {
                    errorDiv.textContent = result.error.message || 'Login failed';
                }
            } catch (error) {
                console.error('Error connecting to API', error);
                errorDiv.textContent = 'Server connection error. Ensure backend is running.';
            } finally {
                btn.innerHTML = 'Sign In';
                btn.disabled = false;
            }
        });
    }

    // Logout Handler
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', (e) => {
            e.preventDefault();
            localStorage.removeItem('jwt_token');
            localStorage.removeItem('user_role');

            document.getElementById('dashboard-container').classList.add('hidden');
            document.getElementById('login-container').classList.remove('hidden');
        });
    }

    function showDashboard(role) {
        document.getElementById('login-container').classList.add('hidden');
        document.getElementById('dashboard-container').classList.remove('hidden');

        document.getElementById('user-role-badge').textContent = role.replace('_', ' ');
    }
});
