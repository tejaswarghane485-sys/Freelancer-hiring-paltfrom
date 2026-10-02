/* ============================================================
   Shared Dashboard Logic — auth guard, nav, utilities, toasts,
   skeletons, notification badges
   ============================================================ */

// ---- Auth Guard ----
(function authGuard() {
    const token = sessionStorage.getItem('token');
    if (!token) {
        // Dashboards live in /pages/ — login.html is a sibling
        window.location.href = 'login.html';
    }
})();

// ---- Populate user info ----
document.addEventListener('DOMContentLoaded', () => {
    const name = sessionStorage.getItem('fullName') || 'User';

    const sidebarName = document.getElementById('sidebarName');
    const topbarName  = document.getElementById('topbarName');
    const avatar      = document.getElementById('sidebarAvatar');

    if (sidebarName) sidebarName.textContent = name;
    if (topbarName)  topbarName.textContent  = name;
    if (avatar)      avatar.textContent      = name.charAt(0).toUpperCase();

    // Toast container
    if (!document.getElementById('toast-container')) {
        const tc = document.createElement('div');
        tc.id = 'toast-container';
        document.body.appendChild(tc);
    }

    // Render skeletons immediately on page load
    document.querySelectorAll('.skeleton-list').forEach(el => {
        el.innerHTML = buildSkeletonList(3);
    });
    document.querySelectorAll('.skeleton-grid').forEach(el => {
        el.innerHTML = buildSkeletonGrid(6);
    });
});

// ---- Section switching ----
function showSection(sectionId) {
    document.querySelectorAll('.content-section').forEach(s => s.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

    const section = document.getElementById(`section-${sectionId}`);
    if (section) section.classList.add('active');

    const navItem = document.querySelector(`.nav-item[data-section="${sectionId}"]`);
    if (navItem) navItem.classList.add('active');

    const titles = {
        'overview':        'Overview',
        'post-project':    'Post a Project',
        'my-projects':     'My Projects',
        'freelancers':     'Browse Freelancers',
        'proposals':       'View Proposals',
        'hiring':          'Hiring Requests',
        'my-profile':      'My Profile',
        'browse-projects': 'Browse Projects',
        'my-proposals':    'My Proposals',
        'hiring-requests': 'Hiring Requests',
        'manage-projects': 'Manage Projects',
    };
    const pageTitle = document.getElementById('pageTitle');
    if (pageTitle) pageTitle.textContent = titles[sectionId] || sectionId;

    if (window.innerWidth <= 768) {
        document.getElementById('sidebar')?.classList.remove('open');
    }
}

// ---- Wire nav items ----
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.nav-item[data-section]').forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            showSection(item.dataset.section);
        });
    });

    // ── Task 6: Enter-key search on all search inputs ──
    document.querySelectorAll('.search-bar input').forEach(input => {
        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                // Click the first button sibling in the same search-bar
                const btn = input.closest('.search-bar')?.querySelector('button');
                if (btn) btn.click();
            }
        });
    });
});

// ---- Sidebar toggle (mobile) ----
function toggleSidebar() {
    document.getElementById('sidebar')?.classList.toggle('open');
}
document.addEventListener('click', (e) => {
    const sidebar = document.getElementById('sidebar');
    const menuBtn = document.querySelector('.menu-toggle');
    if (sidebar && menuBtn && window.innerWidth <= 768) {
        if (!sidebar.contains(e.target) && !menuBtn.contains(e.target)) {
            sidebar.classList.remove('open');
        }
    }
});

// ---- Logout ----
function logout() {
    if (confirm('Are you sure you want to logout?')) {
        sessionStorage.clear();
        window.location.href = 'login.html';
    }
}

// ---- Modal helpers ----
function openModal(id) {
    const el = document.getElementById(id);
    if (!el) return;
    el.classList.remove('hidden');
    el.addEventListener('click', (e) => {
        if (e.target === el) closeModal(id);
    }, { once: true });
    // Focus first input for accessibility
    setTimeout(() => {
        const first = el.querySelector('input:not([type="hidden"]), textarea, select');
        if (first) first.focus();
    }, 80);
}
function closeModal(id) {
    document.getElementById(id)?.classList.add('hidden');
}
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        document.querySelectorAll('.modal-overlay:not(.hidden)').forEach(m => m.classList.add('hidden'));
    }
});

// ---- Inline alert ----
function showInlineAlert(containerId, message, type = 'error') {
    const el = document.getElementById(containerId);
    if (!el) return;
    el.textContent = message;
    el.className   = `alert ${type}`;
    el.classList.remove('hidden');
    setTimeout(() => el.classList.add('hidden'), 5000);
}

// ---- Toast notifications ----
function toast(message, type = 'success', duration = 3500) {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }
    const t = document.createElement('div');
    t.className = `toast toast-${type}`;
    const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️';
    t.innerHTML = `<span class="toast-icon">${icon}</span><span>${escHtml(message)}</span>`;
    container.appendChild(t);
    requestAnimationFrame(() => t.classList.add('show'));
    setTimeout(() => {
        t.classList.remove('show');
        setTimeout(() => t.remove(), 300);
    }, duration);
}

// ============================================================
// Task 9 — Skeleton Loaders
// ============================================================
function buildSkeletonCard() {
    return `<div class="skeleton-card">
        <div style="display:flex;justify-content:space-between;align-items:center">
            <div class="sk-line sk-line-title"></div>
            <div class="sk-badge"></div>
        </div>
        <div class="sk-line sk-line-sm"></div>
        <div class="sk-line sk-line-full"></div>
        <div class="sk-line sk-line-3q"></div>
    </div>`;
}
function buildSkeletonList(n = 3) {
    return Array.from({ length: n }, buildSkeletonCard).join('');
}
function buildSkeletonGrid(n = 6) {
    return `<div class="skeleton-grid">${Array.from({ length: n }, buildSkeletonCard).join('')}</div>`;
}

// ============================================================
// Task 2 — Notification Badge updater
// ============================================================
function updateHiringBadge(count) {
    const badge = document.getElementById('hiringBadge');
    if (!badge) return;
    if (count > 0) {
        badge.textContent = count > 99 ? '99+' : count;
        badge.classList.remove('hidden');
    } else {
        badge.classList.add('hidden');
    }
}

// ============================================================
// Task 3 — Profile completion (freelancer)
// ============================================================
function updateProfileProgress(profile) {
    const fields = [
        { key: 'bio',         label: 'Bio'          },
        { key: 'skills',      label: 'Skills'       },
        { key: 'experience',  label: 'Experience'   },
        { key: 'education',   label: 'Education'    },
        { key: 'portfolioUrl',label: 'Portfolio URL' },
        { key: 'phone',       label: 'Phone'        },
        { key: 'location',    label: 'Location'     },
        { key: 'hourlyRate',  label: 'Hourly Rate'  },
    ];

    const filled  = fields.filter(f => profile && profile[f.key]);
    const missing = fields.filter(f => !profile || !profile[f.key]);
    const pct     = Math.round((filled.length / fields.length) * 100);

    const bar   = document.getElementById('profileProgressBar');
    const label = document.getElementById('profileProgressLabel');
    const miss  = document.getElementById('profileMissingFields');
    if (!bar || !label || !miss) return;

    bar.style.width  = pct + '%';

    const colour = pct < 40 ? 'var(--danger)' : pct < 70 ? 'var(--warning)' : 'var(--success)';
    bar.style.background = colour;

    label.textContent = `${pct}% complete — ${filled.length} of ${fields.length} fields filled`;

    if (missing.length === 0) {
        miss.innerHTML = '<span style="color:var(--success);font-size:13px;font-weight:600">✅ Profile is 100% complete!</span>';
    } else {
        miss.innerHTML = missing.map(f =>
            `<span class="missing-tag">+ ${escHtml(f.label)}</span>`
        ).join('');
    }
}

// ============================================================
// Utilities
// ============================================================
function formatDate(dateStr) {
    if (!dateStr) return 'N/A';
    try {
        return new Date(dateStr).toLocaleDateString('en-IN', {
            year: 'numeric', month: 'short', day: 'numeric'
        });
    } catch { return dateStr; }
}

function formatCurrency(amount) {
    if (amount === null || amount === undefined || amount === '') return 'N/A';
    return '₹' + Number(amount).toLocaleString('en-IN');
}

function renderSkillTags(skillsStr) {
    if (!skillsStr) return '';
    return `<div class="skills-tags">${
        skillsStr.split(',')
            .map(s => s.trim()).filter(Boolean)
            .map(s => `<span class="skill-tag">${escHtml(s)}</span>`)
            .join('')
    }</div>`;
}

function statusBadge(status) {
    if (!status) return '';
    return `<span class="badge status-${status}">${status.replace('_', ' ')}</span>`;
}

function emptyState(icon, message) {
    return `<div class="empty-state">
        <div class="empty-icon">${icon}</div>
        <p>${escHtml(message)}</p>
    </div>`;
}

function escHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
