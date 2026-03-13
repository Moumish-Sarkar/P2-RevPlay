/**
 * RevPlay Auth Page Logic
 * Handles login/register forms on login.html
 */

// ---- Redirect if already logged in ----
(function () {
    if (API.isLoggedIn()) {
        window.location.href = 'index.html';
    }
})();

// ---- Tab Switching ----
function switchTab(tab) {
    const loginTab = document.getElementById('loginTab');
    const registerTab = document.getElementById('registerTab');
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');
    const errorMsg = document.getElementById('authError');
    const successMsg = document.getElementById('authSuccess');

    // Clear messages
    errorMsg.classList.remove('show');
    errorMsg.textContent = '';
    successMsg.classList.remove('show');
    successMsg.textContent = '';

    if (tab === 'login') {
        loginTab.classList.add('active');
        registerTab.classList.remove('active');
        loginForm.classList.add('active');
        registerForm.classList.remove('active');
    } else {
        registerTab.classList.add('active');
        loginTab.classList.remove('active');
        registerForm.classList.add('active');
        loginForm.classList.remove('active');
    }
}

// ---- Toggle Artist Fields ----
function toggleArtistFields() {
    const role = document.getElementById('regRole').value;
    const artistFields = document.getElementById('artistFields');

    if (role === 'ARTIST') {
        artistFields.classList.add('visible');
    } else {
        artistFields.classList.remove('visible');
    }
}

// ---- Show Error ----
function showAuthError(message) {
    const errorMsg = document.getElementById('authError');
    errorMsg.textContent = message;
    errorMsg.classList.add('show');
    document.getElementById('authSuccess').classList.remove('show');
}

// ---- Show Success ----
function showAuthSuccess(message) {
    const successMsg = document.getElementById('authSuccess');
    successMsg.textContent = message;
    successMsg.classList.add('show');
    document.getElementById('authError').classList.remove('show');
}

// ---- Set Button Loading ----
function setButtonLoading(btn, loading) {
    if (loading) {
        btn.classList.add('loading');
        btn.disabled = true;
    } else {
        btn.classList.remove('loading');
        btn.disabled = false;
    }
}

// ---- Handle Login ----
async function handleLogin(event) {
    event.preventDefault();
    const btn = document.getElementById('loginBtn');
    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value;

    if (!username || !password) {
        showAuthError('Please fill in all fields');
        return;
    }

    setButtonLoading(btn, true);

    try {
        const res = await API.login(username, password);
        if (res.success) {
            showAuthSuccess('Login successful! Redirecting...');
            setTimeout(() => {
                window.location.href = 'index.html';
            }, 600);
        } else {
            showAuthError(res.message || 'Login failed');
        }
    } catch (error) {
        showAuthError(error.message || 'Login failed. Please try again.');
    } finally {
        setButtonLoading(btn, false);
    }
}

// ---- Handle Register ----
async function handleRegister(event) {
    event.preventDefault();
    const btn = document.getElementById('registerBtn');

    const username = document.getElementById('regUsername').value.trim();
    const email = document.getElementById('regEmail').value.trim();
    const password = document.getElementById('regPassword').value;
    const displayName = document.getElementById('regDisplayName').value.trim();
    const role = document.getElementById('regRole').value;
    const artistName = document.getElementById('regArtistName').value.trim();

    if (!username || !email || !password) {
        showAuthError('Please fill in all required fields');
        return;
    }

    if (username.length < 3) {
        showAuthError('Username must be at least 3 characters');
        return;
    }

    if (password.length < 6) {
        showAuthError('Password must be at least 6 characters');
        return;
    }

    setButtonLoading(btn, true);

    try {
        const userData = {
            username,
            email,
            password,
            displayName: displayName || username,
            role
        };

        if (role === 'ARTIST' && artistName) {
            userData.artistName = artistName;
        }

        const res = await API.register(userData);
        if (res.success) {
            showAuthSuccess('Account created! Redirecting...');
            setTimeout(() => {
                window.location.href = 'index.html';
            }, 600);
        } else {
            showAuthError(res.message || 'Registration failed');
        }
    } catch (error) {
        showAuthError(error.message || 'Registration failed. Please try again.');
    } finally {
        setButtonLoading(btn, false);
    }
}
