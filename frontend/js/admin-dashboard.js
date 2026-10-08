// Admin dashboard data and actions.
document.addEventListener('DOMContentLoaded', () => {
    if (sessionStorage.getItem('role') !== 'ADMIN') {
        window.location.href = 'login.html';
        return;
    }

    const name = sessionStorage.getItem('fullName') || 'Admin';
    document.getElementById('sidebarName').textContent = name;
    document.getElementById('topbarName').textContent = name;
    document.getElementById('sidebarAvatar').textContent = name.trim().charAt(0).toUpperCase() || 'A';

    document.getElementById('logoutButton').addEventListener('click', logoutAdmin);
    document.getElementById('usersTableBody').addEventListener('click', handleUserAction);
    document.getElementById('projectsTableBody').addEventListener('click', handleProjectAction);
    loadAdminDashboard();
});

async function loadAdminDashboard() {
    await Promise.all([
        loadAdminStats(),
        loadUsers(),
        loadProjects(),
    ]);
}

async function loadAdminStats() {
    try {
        const stats = await apiCall('/admin/stats');
        document.getElementById('totalUsers').textContent = stats.totalUsers;
        document.getElementById('totalProjects').textContent = stats.totalProjects;
        document.getElementById('totalProposals').textContent = stats.totalProposals;
    } catch (error) {
        showAdminError(`Could not load dashboard statistics: ${error.message}`);
    }
}

async function loadUsers() {
    const body = document.getElementById('usersTableBody');
    try {
        const users = await apiCall('/admin/users');
        body.replaceChildren();
        if (!users.length) {
            appendEmptyRow(body, 5, 'No users found.');
            return;
        }
        users.forEach(user => {
            const row = document.createElement('tr');
            appendCell(row, user.id);
            appendCell(row, user.name);
            appendCell(row, user.email);
            appendCell(row, user.role);
            const actionCell = document.createElement('td');
            const button = createDeleteButton('Delete user');
            button.dataset.userId = user.id;
            button.disabled = user.role === 'ADMIN';
            if (button.disabled) {
                button.title = 'Admin accounts cannot be deleted here';
            }
            actionCell.append(button);
            row.append(actionCell);
            body.append(row);
        });
    } catch (error) {
        appendEmptyRow(body, 5, `Could not load users: ${error.message}`);
        showAdminError(`Could not load users: ${error.message}`);
    }
}

async function loadProjects() {
    const body = document.getElementById('projectsTableBody');
    try {
        const projects = await apiCall('/admin/projects');
        body.replaceChildren();
        if (!projects.length) {
            appendEmptyRow(body, 8, 'No projects found.');
            return;
        }
        projects.forEach(project => {
            const row = document.createElement('tr');
            appendCell(row, project.id);
            appendCell(row, project.title);
            appendCell(row, project.clientName);
            appendCell(row, project.freelancerName || 'Not Assigned');
            appendCell(row, formatCurrency(project.clientBudget));
            appendCell(row, formatCurrency(project.freelancerPrice));
            appendCell(row, project.status);
            const actionCell = document.createElement('td');
            const button = createDeleteButton('Delete project');
            button.dataset.projectId = project.id;
            actionCell.append(button);
            row.append(actionCell);
            body.append(row);
        });
    } catch (error) {
        appendEmptyRow(body, 8, `Could not load projects: ${error.message}`);
        showAdminError(`Could not load projects: ${error.message}`);
    }
}

function formatCurrency(amount) {
    if (amount === null || amount === undefined) return '—';
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 2,
    }).format(Number(amount));
}

async function handleUserAction(event) {
    const button = event.target.closest('button[data-user-id]');
    if (!button) return;
    const userId = button.dataset.userId;
    if (!window.confirm(`Delete user #${userId}? This will also remove their related records.`)) return;
    await deleteAdminRecord(button, `/admin/users/${encodeURIComponent(userId)}`, 'User deleted.');
}

async function handleProjectAction(event) {
    const button = event.target.closest('button[data-project-id]');
    if (!button) return;
    const projectId = button.dataset.projectId;
    if (!window.confirm(`Delete project #${projectId} and its related records?`)) return;
    await deleteAdminRecord(button, `/admin/projects/${encodeURIComponent(projectId)}`, 'Project deleted.');
}

async function deleteAdminRecord(button, endpoint, successMessage) {
    button.disabled = true;
    try {
        await apiCall(endpoint, { method: 'DELETE' });
        showAdminMessage(successMessage, 'success');
        await loadAdminDashboard();
    } catch (error) {
        showAdminError(error.message);
        button.disabled = false;
    }
}

function appendCell(row, value) {
    const cell = document.createElement('td');
    cell.textContent = value === null || value === undefined ? '—' : String(value);
    row.append(cell);
}

function appendEmptyRow(body, columnCount, message) {
    body.replaceChildren();
    const row = document.createElement('tr');
    const cell = document.createElement('td');
    cell.colSpan = columnCount;
    cell.textContent = message;
    row.append(cell);
    body.append(row);
}

function createDeleteButton(label) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'btn btn-danger btn-sm';
    button.textContent = 'Delete';
    button.setAttribute('aria-label', label);
    return button;
}

function showAdminError(message) {
    showAdminMessage(message, 'error');
}

function showAdminMessage(message, type) {
    const alert = document.getElementById('adminAlert');
    alert.textContent = message;
    alert.className = `alert ${type}`;
    alert.classList.remove('hidden');
}

function logoutAdmin() {
    sessionStorage.clear();
    localStorage.clear();
    window.location.href = 'login.html';
}
