/* ============================================================
   Auth Page Logic  —  Login / Signup
   ============================================================ */

// ── Helpers ───────────────────────────────────────────────
function showAlert(message, type = 'error') {
    const box = document.getElementById('alert-box');
    if (!box) return;
    box.textContent = message;
    box.className   = `alert ${type}`;
    box.classList.remove('hidden');
    setTimeout(() => box.classList.add('hidden'), 6000);
}

function setLoading(btnId, textId, spinnerId, loading) {
    const btn     = document.getElementById(btnId);
    const text    = document.getElementById(textId);
    const spinner = document.getElementById(spinnerId);
    if (btn)     btn.disabled        = loading;
    if (text)    text.style.opacity  = loading ? '0.5' : '1';
    if (spinner) spinner.classList.toggle('hidden', !loading);
}

function togglePassword(fieldId) {
    const f = document.getElementById(fieldId);
    if (f) f.type = f.type === 'password' ? 'text' : 'password';
}

function selectRole(role) {
    document.getElementById('selectedRole').value = role;
    document.getElementById('clientBtn').classList.toggle('active',     role === 'CLIENT');
    document.getElementById('freelancerBtn').classList.toggle('active', role === 'FREELANCER');
}

// ── Redirect after auth ───────────────────────────────────
function redirectToDashboard(role) {
    // All auth pages are in /pages/, so dashboard links are siblings.
    if (role === 'CLIENT') {
        window.location.href = 'client-dashboard.html';
    } else {
        window.location.href = 'freelancer-dashboard.html';
    }
}

// ── Login ─────────────────────────────────────────────────
async function handleLogin(email, password) {
    setLoading('loginBtn', 'loginBtnText', 'loginSpinner', true);
    try {
        const data = await Auth.login({ email, password });
        _storeSession(data);
        redirectToDashboard(data.role);
    } catch (err) {
        showAlert(err.message || 'Login failed. Please check your credentials.');
    } finally {
        setLoading('loginBtn', 'loginBtnText', 'loginSpinner', false);
    }
}

// ── Register ──────────────────────────────────────────────
async function handleRegister(fullName, email, password, role) {
    setLoading('signupBtn', 'signupBtnText', 'signupSpinner', true);
    try {
        const data = await Auth.register({ fullName, email, password, role });
        _storeSession(data);
        redirectToDashboard(data.role);
    } catch (err) {
        showAlert(err.message || 'Registration failed. Please try again.');
    } finally {
        setLoading('signupBtn', 'signupBtnText', 'signupSpinner', false);
    }
}

function _storeSession(data) {
    sessionStorage.setItem('token',    data.token);
    sessionStorage.setItem('userId',   data.userId);
    sessionStorage.setItem('fullName', data.fullName);
    sessionStorage.setItem('email',    data.email);
    sessionStorage.setItem('role',     data.role);
}

// ── Auto-redirect if already logged in ───────────────────
(function checkAuth() {
    const token = sessionStorage.getItem('token');
    const role  = sessionStorage.getItem('role');
    if (token && role) redirectToDashboard(role);
})();
