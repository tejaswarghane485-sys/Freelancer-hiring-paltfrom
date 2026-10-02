/* ============================================================
   Client Dashboard Logic
   — edit project modal, notification badge, skeleton loaders,
     proposals, hiring requests
   ============================================================ */

const clientUserId = parseInt(sessionStorage.getItem('userId'));

// ============================================================
// INIT
// ============================================================
document.addEventListener('DOMContentLoaded', async () => {
    // Post-project form
    const form = document.getElementById('projectForm');
    if (form) form.addEventListener('submit', async (e) => { e.preventDefault(); await submitProject(); });

    // Edit-project form (inside modal)
    const editForm = document.getElementById('editProjectForm');
    if (editForm) editForm.addEventListener('submit', async (e) => { e.preventDefault(); await saveEditedProject(); });

    // Load everything in parallel where possible
    await Promise.all([
        loadOverview(),
        loadMyProjects(),
        loadAllFreelancers(),
        loadClientHiring(),
        populateProjectSelect(),
    ]);
});

// ============================================================
// OVERVIEW
// ============================================================
async function loadOverview() {
    try {
        const [projects, hiring] = await Promise.all([
            Projects.getMine(),
            Hiring.getSent(),
        ]);

        document.getElementById('totalProjects').textContent      = projects.length;
        document.getElementById('openProjects').textContent       = projects.filter(p => p.status === 'OPEN').length;
        document.getElementById('inProgressProjects').textContent = projects.filter(p => p.status === 'IN_PROGRESS').length;
        document.getElementById('totalHiring').textContent        = hiring.length;

        // Notification badge — pending responses from freelancers
        const pendingHiring = hiring.filter(h => h.status === 'PENDING').length;
        updateHiringBadge(pendingHiring);

        const el = document.getElementById('recentProjectsList');
        el.innerHTML = projects.length
            ? projects.slice(0, 3).map(p => projectCard(p, 'overview')).join('')
            : emptyState('📁', 'No projects yet. Post your first project!');
    } catch (err) {
        console.error('Overview error:', err);
    }
}

// ============================================================
// POST PROJECT
// ============================================================
async function submitProject() {
    const btn = document.getElementById('postProjectBtn');
    if (btn) { btn.disabled = true; btn.textContent = 'Posting…'; }
    try {
        await Projects.create({
            title:          document.getElementById('projTitle').value.trim(),
            description:    document.getElementById('projDesc').value.trim(),
            requiredSkills: document.getElementById('projSkills').value.trim(),
            budget:         document.getElementById('projBudget').value   || null,
            deadline:       document.getElementById('projDeadline').value || null,
        });
        showInlineAlert('projectFormAlert', 'Project posted successfully!', 'success');
        document.getElementById('projectForm').reset();
        await Promise.all([loadMyProjects(), loadOverview(), populateProjectSelect()]);
    } catch (err) {
        showInlineAlert('projectFormAlert', err.message, 'error');
    } finally {
        if (btn) { btn.disabled = false; btn.textContent = 'Post Project'; }
    }
}

// ============================================================
// MY PROJECTS
// ============================================================
async function loadMyProjects() {
    const el = document.getElementById('myProjectsList');
    try {
        const projects = await Projects.getMine();
        el.innerHTML = projects.length
            ? projects.map(p => projectCard(p, 'manage')).join('')
            : emptyState('📁', 'You have not posted any projects yet.');
    } catch (err) {
        el.innerHTML = emptyState('⚠️', 'Could not load projects.');
        console.error(err);
    }
}

function projectCard(project, context) {
    const manageActions = context === 'manage' ? `
        <div class="proposal-actions" style="margin-top:14px">
            <button class="btn btn-outline btn-sm"  onclick="viewProjectProposals(${project.id})">📝 Proposals</button>
            <button class="btn btn-outline btn-sm"  onclick="openEditProjectModal(${project.id})">✏️ Edit</button>
            <select class="status-select" onchange="updateProjectStatus(${project.id}, this.value)">
                <option value="">Change Status</option>
                <option value="OPEN"        ${project.status === 'OPEN'        ? 'selected':''}>Open</option>
                <option value="IN_PROGRESS" ${project.status === 'IN_PROGRESS' ? 'selected':''}>In Progress</option>
                <option value="COMPLETED"   ${project.status === 'COMPLETED'   ? 'selected':''}>Completed</option>
                <option value="CLOSED"      ${project.status === 'CLOSED'      ? 'selected':''}>Closed</option>
            </select>
            <button class="btn btn-danger btn-sm" onclick="deleteProject(${project.id})">🗑 Delete</button>
        </div>` : '';

    return `
    <div class="project-card">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;flex-wrap:wrap;gap:8px;margin-bottom:8px">
            <h3 class="card-title" style="margin:0">${escHtml(project.title)}</h3>
            ${statusBadge(project.status)}
        </div>
        <div class="card-meta">
            <span>💰 ${formatCurrency(project.budget)}</span>
            <span>📅 Deadline: ${formatDate(project.deadline)}</span>
            <span>📆 Posted: ${formatDate(project.createdAt)}</span>
        </div>
        <p class="card-desc">${escHtml((project.description||'').substring(0,200))}${(project.description||'').length>200?'…':''}</p>
        ${renderSkillTags(project.requiredSkills)}
        ${manageActions}
    </div>`;
}

// ---- Edit Project Modal ----
async function openEditProjectModal(projectId) {
    try {
        const p = await Projects.getById(projectId);
        document.getElementById('editProjectId').value    = p.id;
        document.getElementById('editProjTitle').value    = p.title   || '';
        document.getElementById('editProjDesc').value     = p.description || '';
        document.getElementById('editProjSkills').value   = p.requiredSkills || '';
        document.getElementById('editProjBudget').value   = p.budget  || '';
        document.getElementById('editProjDeadline').value = p.deadline ? p.deadline.substring(0,10) : '';
        document.getElementById('editProjectStatus').value = p.status || '';
        document.getElementById('editProjectAlert').classList.add('hidden');
        openModal('editProjectModal');
    } catch (err) {
        toast('Could not load project: ' + err.message, 'error');
    }
}

async function saveEditedProject() {
    const id  = document.getElementById('editProjectId').value;
    const btn = document.getElementById('editProjectBtn');
    if (btn) { btn.disabled = true; btn.textContent = 'Saving…'; }
    try {
        await Projects.update(id, {
            title:          document.getElementById('editProjTitle').value.trim(),
            description:    document.getElementById('editProjDesc').value.trim(),
            requiredSkills: document.getElementById('editProjSkills').value.trim(),
            budget:         document.getElementById('editProjBudget').value   || null,
            deadline:       document.getElementById('editProjDeadline').value || null,
            status:         document.getElementById('editProjectStatus').value,
        });
        closeModal('editProjectModal');
        toast('Project updated successfully!');
        await Promise.all([loadMyProjects(), loadOverview()]);
    } catch (err) {
        showInlineAlert('editProjectAlert', err.message, 'error');
    } finally {
        if (btn) { btn.disabled = false; btn.textContent = 'Save Changes'; }
    }
}

async function updateProjectStatus(projectId, status) {
    if (!status) return;
    try {
        await Projects.updateStatus(projectId, status);
        toast('Project status updated');
        await Promise.all([loadMyProjects(), loadOverview()]);
    } catch (err) {
        toast(err.message, 'error');
    }
}

async function deleteProject(projectId) {
    if (!confirm('Delete this project? This cannot be undone.')) return;
    try {
        await Projects.delete(projectId);
        toast('Project deleted');
        await Promise.all([loadMyProjects(), loadOverview(), populateProjectSelect()]);
    } catch (err) {
        toast(err.message, 'error');
    }
}

// ============================================================
// BROWSE FREELANCERS
// ============================================================
async function loadAllFreelancers() {
    const el = document.getElementById('freelancersList');
    try {
        const profiles = await Freelancers.getAll();
        renderFreelancers(profiles);
    } catch (err) {
        el.innerHTML = emptyState('⚠️', 'Could not load freelancers.');
    }
}

async function searchFreelancers() {
    const q = document.getElementById('freelancerSearch').value.trim();
    if (!q) { await loadAllFreelancers(); return; }
    try {
        const bySkill = await Freelancers.search({ skill: q });
        if (bySkill.length) { renderFreelancers(bySkill); return; }
        const byName  = await Freelancers.search({ name: q });
        renderFreelancers(byName);
    } catch (err) {
        toast(err.message, 'error');
    }
}

function renderFreelancers(profiles) {
    const el = document.getElementById('freelancersList');
    if (!el) return;
    el.innerHTML = profiles.length
        ? profiles.map(p => freelancerCard(p)).join('')
        : emptyState('👥', 'No freelancers found. Try a different search term.');
}

function freelancerCard(profile) {
    const name = profile.user?.fullName || 'Freelancer';
    return `
    <div class="freelancer-card" onclick="openFreelancerModal(${profile.id})"
         tabindex="0" role="button" aria-label="View ${escHtml(name)}'s profile"
         onkeydown="if(event.key==='Enter')openFreelancerModal(${profile.id})">
        <div class="freelancer-header">
            <div class="freelancer-avatar">${escHtml(name.charAt(0).toUpperCase())}</div>
            <div style="flex:1;min-width:0">
                <div class="card-title">${escHtml(name)}</div>
                <div class="card-meta" style="margin:2px 0 0">
                    <span>📍 ${escHtml(profile.location || 'Location N/A')}</span>
                    <span>💰 ${profile.hourlyRate ? formatCurrency(profile.hourlyRate)+'/hr' : 'Rate N/A'}</span>
                </div>
            </div>
        </div>
        ${renderSkillTags(profile.skills)}
        <p class="card-desc" style="margin-top:8px">${escHtml((profile.bio||'No bio provided.').substring(0,110))}…</p>
    </div>`;
}

async function openFreelancerModal(profileId) {
    const content = document.getElementById('freelancerModalContent');
    content.innerHTML = buildSkeletonList(2);
    openModal('freelancerModal');
    try {
        const [profile, myProjects] = await Promise.all([
            Freelancers.getById(profileId),
            Projects.getMine(),
        ]);
        const name         = profile.user?.fullName || 'Freelancer';
        const openProjects = myProjects.filter(p => p.status === 'OPEN');
        const projectOpts  = openProjects
            .map(p => `<option value="${p.id}">${escHtml(p.title)}</option>`)
            .join('');

        content.innerHTML = `
            <div class="freelancer-header" style="margin-bottom:20px">
                <div class="freelancer-avatar" style="width:60px;height:60px;font-size:28px;flex-shrink:0">
                    ${escHtml(name.charAt(0).toUpperCase())}
                </div>
                <div>
                    <h2 style="font-size:20px;font-weight:800;margin-bottom:4px">${escHtml(name)}</h2>
                    <div class="card-meta">
                        <span>📍 ${escHtml(profile.location || 'N/A')}</span>
                        <span>💰 ${profile.hourlyRate ? formatCurrency(profile.hourlyRate)+'/hr' : 'N/A'}</span>
                        ${profile.phone ? `<span>📞 ${escHtml(profile.phone)}</span>` : ''}
                    </div>
                </div>
            </div>

            <div style="display:grid;gap:14px;font-size:14px;margin-bottom:20px">
                ${profile.bio        ? `<div><strong>About</strong><p style="margin-top:4px;color:var(--gray-600);line-height:1.6">${escHtml(profile.bio)}</p></div>` : ''}
                ${profile.skills     ? `<div><strong>Skills</strong><div style="margin-top:6px">${renderSkillTags(profile.skills)}</div></div>` : ''}
                ${profile.experience ? `<div><strong>Experience</strong><p style="margin-top:2px;color:var(--gray-600)">${escHtml(profile.experience)}</p></div>` : ''}
                ${profile.education  ? `<div><strong>Education</strong><p style="margin-top:2px;color:var(--gray-600)">${escHtml(profile.education)}</p></div>` : ''}
                ${profile.portfolioUrl
                    ? `<div><strong>Portfolio</strong>
                       <a href="${encodeURI(profile.portfolioUrl)}" target="_blank" rel="noopener noreferrer"
                          style="color:var(--primary);word-break:break-all">${escHtml(profile.portfolioUrl)}</a></div>`
                    : ''}
            </div>

            <div class="modal-hire-section">
                <h3>Send Hiring Request</h3>
                ${openProjects.length ? `
                    <div class="form-group">
                        <label>Select Your Project</label>
                        <select id="hireProjectSelect">${projectOpts}</select>
                    </div>
                    <div class="form-group">
                        <label>Message <span class="hint">(optional)</span></label>
                        <textarea id="hireMessage" rows="3"
                            placeholder="Briefly describe the work and why you'd like to hire this freelancer…"></textarea>
                    </div>
                    <button class="btn btn-primary" onclick="sendHireRequest(${profile.user.id})">
                        📤 Send Hiring Request
                    </button>`
                : `<p style="color:var(--gray-500);font-size:14px">
                       You need an open project to send a hiring request.
                       <button class="btn btn-outline btn-sm" style="margin-left:8px"
                           onclick="closeModal('freelancerModal');showSection('post-project')">
                           Post a Project
                       </button>
                   </p>`}
            </div>`;
    } catch (err) {
        content.innerHTML = emptyState('⚠️', 'Could not load profile: ' + err.message);
    }
}

async function sendHireRequest(freelancerUid) {
    const projectId = document.getElementById('hireProjectSelect')?.value;
    const message   = document.getElementById('hireMessage')?.value.trim() || '';
    if (!projectId) { toast('Please select a project', 'error'); return; }
    const btn = document.querySelector('#freelancerModalContent .btn-primary');
    if (btn) { btn.disabled = true; btn.textContent = 'Sending…'; }
    try {
        await Hiring.send({ projectId: parseInt(projectId), freelancerId: freelancerUid, message });
        closeModal('freelancerModal');
        toast('Hiring request sent!');
        await Promise.all([loadClientHiring(), loadOverview()]);
    } catch (err) {
        toast(err.message, 'error');
        if (btn) { btn.disabled = false; btn.textContent = '📤 Send Hiring Request'; }
    }
}

// ============================================================
// PROPOSALS
// ============================================================
async function populateProjectSelect() {
    try {
        const projects = await Projects.getMine();
        const select   = document.getElementById('proposalProjectSelect');
        if (!select) return;
        const current  = select.value;
        select.innerHTML = '<option value="">-- Select a project --</option>' +
            projects.map(p => `<option value="${p.id}">${escHtml(p.title)}</option>`).join('');
        // restore previous selection
        if (current) select.value = current;
    } catch (err) { console.error(err); }
}

async function loadProposalsForProject(projectId) {
    const el = document.getElementById('proposalsList');
    if (!projectId) { if (el) el.innerHTML = ''; return; }
    el.innerHTML = buildSkeletonList(2);
    try {
        const proposals = await Proposals.getForProject(projectId);
        if (!proposals.length) {
            el.innerHTML = emptyState('📝', 'No proposals received for this project yet.');
            return;
        }
        el.innerHTML = proposals.map(prop => `
        <div class="proposal-card">
            <div style="flex:1">
                <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap;margin-bottom:6px">
                    <span class="card-title" style="margin:0">${escHtml(prop.freelancer?.fullName || 'Freelancer')}</span>
                    ${statusBadge(prop.status)}
                </div>
                <div class="card-meta">
                    <span>💰 Bid: ${formatCurrency(prop.proposedBudget)}</span>
                    <span>⏱ ${prop.estimatedDays ? prop.estimatedDays+' days' : 'Timeline N/A'}</span>
                    <span>🗓 ${formatDate(prop.createdAt)}</span>
                </div>
                <p class="card-desc">
                    ${escHtml((prop.coverLetter||'').substring(0,300))}${(prop.coverLetter||'').length>300?'…':''}
                </p>
            </div>
            ${prop.status === 'PENDING' ? `
            <div class="proposal-actions">
                <button class="btn btn-success btn-sm"
                    onclick="updateProposal(${prop.id},'ACCEPTED',${projectId})">✅ Accept</button>
                <button class="btn btn-danger btn-sm"
                    onclick="updateProposal(${prop.id},'REJECTED',${projectId})">❌ Reject</button>
            </div>` : ''}
        </div>`).join('');
    } catch (err) {
        el.innerHTML = emptyState('⚠️', 'Could not load proposals.');
    }
}

async function updateProposal(proposalId, status, projectId) {
    if (!confirm(`${status === 'ACCEPTED' ? 'Accept' : 'Reject'} this proposal?`)) return;
    try {
        await Proposals.updateStatus(proposalId, status);
        toast(`Proposal ${status.toLowerCase()}`);
        await loadProposalsForProject(projectId);
    } catch (err) {
        toast(err.message, 'error');
    }
}

async function viewProjectProposals(projectId) {
    showSection('proposals');
    const select = document.getElementById('proposalProjectSelect');
    if (select) select.value = projectId;
    await loadProposalsForProject(projectId);
}

// ============================================================
// HIRING REQUESTS (sent by client)
// ============================================================
async function loadClientHiring() {
    const el = document.getElementById('hiringList');
    if (!el) return;
    try {
        const requests = await Hiring.getSent();

        // Update notification badge (pending = freelancer hasn't responded yet)
        const pending = requests.filter(r => r.status === 'PENDING').length;
        updateHiringBadge(pending);

        if (!requests.length) {
            el.innerHTML = emptyState('🤝', 'No hiring requests sent yet. Browse freelancers to get started.');
            return;
        }
        el.innerHTML = requests.map(r => `
        <div class="proposal-card">
            <div style="flex:1">
                <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap;margin-bottom:6px">
                    <span class="card-title" style="margin:0">
                        To: ${escHtml(r.freelancer?.fullName || 'Freelancer')}
                    </span>
                    ${statusBadge(r.status)}
                </div>
                <div class="card-meta">
                    <span>📁 ${escHtml(r.project?.title || 'N/A')}</span>
                    <span>🗓 Sent: ${formatDate(r.createdAt)}</span>
                </div>
                ${r.message ? `<p class="card-desc">${escHtml(r.message)}</p>` : ''}
            </div>
        </div>`).join('');
    } catch (err) {
        console.error('Hiring error:', err);
    }
}
