/* ============================================================
   API Layer — all application data comes from the Spring Boot API
   ============================================================ */

const API_BASE = '/api';
async function apiCall(endpoint, options = {}) {
    const token = sessionStorage.getItem('token');
    const headers = {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(options.headers || {}),
    };

    let response;
    try {
        response = await fetch(`${API_BASE}${endpoint}`, { ...options, headers, cache: 'no-store' });
    } catch (error) {
        throw new Error(`Cannot connect to ${API_BASE}. Make sure the Spring Boot backend is running and the frontend origin is allowed by CORS.`);
    }

    if (response.status === 401) {
        sessionStorage.clear();
        window.location.href = 'login.html';
        throw new Error('Your session has expired. Please log in again.');
    }

    if (response.status === 204) return null;

    const responseText = await response.text();
    let data = {};
    if (responseText) {
        try {
            data = JSON.parse(responseText);
        } catch {
            data = { message: responseText };
        }
    }
    if (!response.ok) {
        throw new Error(data.error || data.message || `Request failed (${response.status})`);
    }
    return data;
}

const Auth = {
    register: (body) => apiCall('/auth/register', {
        method: 'POST',
        body: JSON.stringify(body),
    }),
    login: (body) => apiCall('/auth/login', {
        method: 'POST',
        body: JSON.stringify(body),
    }),
};

const Users = {
    me: () => apiCall('/users/me'),
};

const Projects = {
    getOpen: () => apiCall('/projects/open'),
    getMine: () => apiCall('/projects/my'),
    getById: (id) => apiCall(`/projects/${id}`),
    create: (body) => apiCall('/projects', {
        method: 'POST',
        body: JSON.stringify(body),
    }),
    update: (id, body) => apiCall(`/projects/${id}`, {
        method: 'PUT',
        body: JSON.stringify(body),
    }),
    updateStatus: (id, status) => apiCall(`/projects/${id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status }),
    }),
    submitWork: (id) => apiCall(`/projects/${id}/submit`, { method: 'POST' }),
    delete: (id) => apiCall(`/projects/${id}`, { method: 'DELETE' }),
    search: (params) => apiCall(`/projects/search?${new URLSearchParams(params)}`),
};

const Freelancers = {
    getAll: () => apiCall('/freelancers'),
    getById: (id) => apiCall(`/freelancers/${id}`),
    getByUserId: (userId) => apiCall(`/freelancers/user/${userId}`),
    saveProfile: (body) => apiCall('/freelancers/profile', {
        method: 'POST',
        body: JSON.stringify(body),
    }),
    search: (params) => apiCall(`/freelancers/search?${new URLSearchParams(params)}`),
};

const Proposals = {
    submit: (body) => apiCall('/proposals', {
        method: 'POST',
        body: JSON.stringify(body),
    }),
    getMine: () => apiCall('/proposals/my'),
    getForProject: (projectId) => apiCall(`/proposals/project/${projectId}`),
    updateStatus: (id, status) => apiCall(`/proposals/${id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status }),
    }),
    delete: (id) => apiCall(`/proposals/${id}`, { method: 'DELETE' }),
};

const Hiring = {
    send: (body) => apiCall('/hiring-requests', {
        method: 'POST',
        body: JSON.stringify(body),
    }),
    accept: (id) => apiCall(`/hiring-requests/${id}/accept`, { method: 'PUT' }),
    reject: (id) => apiCall(`/hiring-requests/${id}/reject`, { method: 'PUT' }),
    getReceived: () => apiCall('/hiring-requests/received'),
    getSent: () => apiCall('/hiring-requests/sent'),
    getForProject: (projectId) => apiCall(`/hiring-requests/project/${projectId}`),
};

const Payments = {
    getClientTransactions: () => apiCall('/payments/client'),
    getFreelancerEarnings: () => apiCall('/payments/freelancer'),
    pay: (projectId, paymentMethod) => apiCall('/payments/pay', {
        method: 'POST',
        body: JSON.stringify({ projectId, paymentMethod }),
    }),
};
