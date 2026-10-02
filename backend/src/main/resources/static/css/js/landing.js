document.addEventListener('DOMContentLoaded', () => {
    loadLandingData();
});

async function loadLandingData() {
    const container = document.getElementById('featuredFreelancers');
    try {
        const [projects, freelancers] = await Promise.all([
            Projects.getOpen(),
            Freelancers.getAll(),
        ]);
        document.getElementById('openProjectCount').textContent = projects.length.toLocaleString();
        document.getElementById('freelancerCount').textContent = freelancers.length.toLocaleString();

        container.innerHTML = freelancers.length
            ? freelancers.slice(0, 3).map((profile, index) => {
                const name = profile.user?.fullName || 'Freelancer';
                const initials = name.trim().split(/\s+/).slice(0, 2).map(part => part[0]).join('');
                const skills = profile.skills || profile.bio || 'Freelancer profile';
                return `
                    <div class="hero-card card-${index + 1}">
                        <div class="card-avatar">${escapeHtml(initials)}</div>
                        <div><strong>${escapeHtml(name)}</strong><p>${escapeHtml(skills)}</p></div>
                    </div>`;
            }).join('')
            : `<div class="hero-card card-1">
                    <div class="card-avatar">💼</div>
                    <div><strong>No freelancer profiles yet</strong><p>Sign up to create a profile.</p></div>
                </div>`;
    } catch (error) {
        container.textContent = 'Freelancer profiles could not be loaded from the backend.';
        console.error('Could not load landing page data from the backend.', error);
    }
}

function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, character => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;',
    })[character]);
}
