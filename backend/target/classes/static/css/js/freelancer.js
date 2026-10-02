/* ============================================================
   Freelancer Dashboard Logic
   — profile progress bar, project detail modal, char counter,
     notification badge, skeleton loaders, all features wired
   ============================================================ */

const freelancerUserId = parseInt(sessionStorage.getItem('userId'));

// ============================================================
// INIT
// ============================================================
document.addEventListener('DOMContentLoaded', async () => {
    // Form listeners
    const profileForm  = document.getElementById('profileForm');
    const proposalForm = document.getElementById('proposalForm');
    if (profileForm)  profileForm.addEventListener('submit',  async (e) => { e.preventDefault(); await saveProfile(); });
    if (proposalForm) proposalForm.addEventListener('submit', async (e) => { e.preventDefault(); await submitProposal(); });

    // Cover-letter char counter
    const coverLetter = document.getElementById('coverLetter');
    if (coverLetter) {
        coverLetter.addEventListener('input', () => updateCharCount(coverLetter, 'coverLetterCount', 1000));
    }

    // Load all data in parallel — profile first so progress bar can render
    const [,,hiring] = await Promise.all([
        loadFreelancerOverview(),
        loadMyProfile(),
        loadHiringRequests(),       // needed for badge in overview too
    ]);

    // Non-critical sections load independently
    loadOpenProjects();
    loadMyProposals();
    loadActiveProjects();
});

// ============================================================
// CHAR COUNTER (cover letter)
// ============================================================
function updateCharCount(textarea, counterId, max) {
    const counter = document.getElementById(counterId);
    if (!counter) return;
    const len = textarea.value.length;
    counter.textContent = `${len} / ${max}`;
    counter.className = 'char-count' +
        (len >= max     ? ' limit' :
         len >= max*0.8 ? ' warn'  : '');
}

// ============================================================
// OVERVIEW
// ============================================================
async function loadFreelancerOverview() {
    try {
        const [proposals, hiring, openProjects] = await Promise.all([
            Proposals.getMine(),
            Hiring.getReceived(),
            Projects.getOpen(),
        ]);

        document.getElementById('totalProposals').textContent    = proposals.length;
        document.getElementById('acceptedProposals').textContent = proposals.filter(p => p.status === 'ACCEPTED').length;
        document.getElementById('openProjectsCount').textContent = openProjects.length;

        const pending = hiring.filter(h => h.status === 'PENDING');
        document.getElementById('pendingHiring').textContent = pending.length;

        // Notification badge on sidebar
        updateHiringBadge(pending.length);

        // Recent hiring cards (compact, no action buttons)
        const recentEl = document.getElementById('recentHiringList');
        recentEl.innerHTML = hiring.length
            ? hiring.slice(0, 3).map(r => hiringCard(r, true)).join('')
            : emptyState('🤝', 'No hiring requests yet. Complete your profile to attract clients!');

        return hiring;   // caller can reuse
    } catch (err) {
        console.error('Overview error:', err);
    }
}

// ============================================================
// MY PROFILE  — load, save, progress bar
// ============================================================
async function loadMyProfile() {
    try {
        const profile = await Freelancers.getByUserId(freelancerUserId);
        setVal('profBio',        profile.bio);
        setVal('profSkills',     profile.skills);
        setVal('profExperience', profile.experience);
        setVal('profEducation',  profile.education);
        setVal('profPortfolio',  profile.portfolioUrl);
        setVal('profRate',       profile.hourlyRate);
        setVal('profPhone',      profile.phone);
        setVal('profLocation',   profile.location);
        updateProfileProgress(profile);   // defined in dashboard.js
    } catch (_) {
        // No profile yet — form stays blank, progress bar shows 0%
        updateProfileProgress(null);
    }
}

function setVal(id, val) {
    const el = document.getElementById(id);
    if (el && val !== null && val !== undefined) el.value = val;
}

async function saveProfile() {
    const btn = document.getElementById('saveProfileBtn');
    if (btn) { btn.disabled = true; btn.textContent = 'Saving…'; }
    const body = {
        bio:          document.getElementById('profBio').value.trim(),
        skills:       document.getElementById('profSkills').value.trim(),
        experience:   document.getElementById('profExperience').value.trim(),
        education:    document.getElementById('profEducation').value.trim(),
        portfolioUrl: document.getElementById('profPortfolio').value.trim(),
        hourlyRate:   document.getElementById('profRate').value     || null,
        phone:        document.getElementById('profPhone').value.trim(),
        location:     document.getElementById('profLocation').value.trim(),
    };
    try {
        const saved = await Freelancers.saveProfile(body);
        showInlineAlert('profileAlert', 'Profile saved successfully!', 'success');
        toast('Profile saved!');
        updateProfileProgress(saved);   // refresh progress bar immediately
    } catch (err) {
        showInlineAlert('profileAlert', err.message, 'error');
    } finally {
        if (btn) { btn.disabled = false; btn.textContent = 'Save Profile'; }
    }
}

// ============================================================
// BROWSE OPEN PROJECTS  — with Project Detail modal
// ============================================================
async function loadOpenProjects() {
    const el = document.getElementById('openProjectsList');
    if (!el) return;
    el.innerHTML = buildSkeletonList(3);
    try {
        const projects = await Projects.getOpen();
        renderProjects(projects, el);
    } catch (err) {
        el.innerHTML = emptyState('⚠️', 'Could not load projects. Make sure the backend is running on port 8080.');
    }
}

async function searchProjects() {
    const q  = document.getElementById('projectSearch').value.trim();
    const el = document.getElementById('openProjectsList');
    if (!q) { await loadOpenProjects(); return; }
    el.innerHTML = buildSkeletonList(3);
    try {
        const bySkill   = await Projects.search({ skill: q });
        if (bySkill.length) { renderProjects(bySkill, el); return; }
        const byKeyword = await Projects.search({ keyword: q });
        renderProjects(byKeyword, el);
    } catch (err) {
        toast(err.message, 'error');
        el.innerHTML = emptyState('⚠️', 'Search failed.');
    }
}

function renderProjects(projects, el) {
    if (!el) return;
    el.innerHTML = projects.length
        ? projects.map(p => projectCard(p)).join('')
        : emptyState('🔍', 'No open projects found. Try a different keyword or skill.');
}

function projectCard(project) {
    const desc = (project.description || '').substring(0, 160);
    const more = (project.description || '').length > 160;
    return `
    <div class="project-card">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;flex-wrap:wrap;gap:8px;margin-bottom:8px">
            <h3 class="card-title" style="margin:0">${escHtml(project.title)}</h3>
            ${statusBadge(project.status)}
        </div>
        <div class="card-meta">
            <span>👤 ${escHtml(project.client?.fullName || 'Client')}</span>
            <span>💰 ${formatCurrency(project.budget)}</span>
            <span>📅 Deadline: ${formatDate(project.deadline)}</span>
        </div>
        <p class="card-desc">${escHtml(desc)}${more ? '…' : ''}</p>
        ${renderSkillTags(project.requiredSkills)}
        <div style="display:flex;gap:8px;margin-top:12px;flex-wrap:wrap">
            <button class="btn btn-outline btn-sm"
                onclick="openProjectDetailModal(${project.id})">🔍 View Details</button>
            <button class="btn btn-primary btn-sm"
                onclick="openProposalModal(${project.id},
                    '${escHtml(project.title).replace(/'/g,"\\'")}',
                    '${formatCurrency(project.budget).replace(/'/g,"\\'")}')">
                📝 Submit Proposal
            </button>
        </div>
    </div>`;
}

// ---- Project Detail Modal ----
async function openProjectDetailModal(projectId) {
    const content = document.getElementById('projectDetailContent');
    if (!content) return;
    content.innerHTML = buildSkeletonList(2);
    openModal('projectDetailModal');
    try {
        const p = await Projects.getById(projectId);
        content.innerHTML = `
            <div style="display:flex;justify-content:space-between;align-items:flex-start;flex-wrap:wrap;gap:8px;margin-bottom:16px">
                <h2 style="font-size:20px;font-weight:800;color:var(--gray-900);margin:0">${escHtml(p.title)}</h2>
                ${statusBadge(p.status)}
            </div>
            <div class="card-meta" style="margin-bottom:16px;font-size:14px">
                <span>👤 Client: <strong>${escHtml(p.client?.fullName || 'N/A')}</strong></span>
                <span>💰 Budget: <strong>${formatCurrency(p.budget)}</strong></span>
                <span>📅 Deadline: <strong>${formatDate(p.deadline)}</strong></span>
                <span>📆 Posted: <strong>${formatDate(p.createdAt)}</strong></span>
            </div>
            <div style="margin-bottom:16px">
                <strong style="font-size:14px">Description</strong>
                <p style="margin-top:6px;color:var(--gray-600);line-height:1.7;font-size:14px;white-space:pre-line">${escHtml(p.description || '')}</p>
            </div>
            ${p.requiredSkills ? `
            <div style="margin-bottom:20px">
                <strong style="font-size:14px">Required Skills</strong>
                <div style="margin-top:6px">${renderSkillTags(p.requiredSkills)}</div>
            </div>` : ''}
            <div style="display:flex;gap:10px;flex-wrap:wrap">
                <button class="btn btn-primary"
                    onclick="closeModal('projectDetailModal');openProposalModal(${p.id},
                        '${escHtml(p.title).replace(/'/g,"\\'")}',
                        '${formatCurrency(p.budget).replace(/'/g,"\\'")}')">
                    📝 Submit Proposal
                </button>
                <button class="btn btn-outline" onclick="closeModal('projectDetailModal')">Close</button>
            </div>`;
    } catch (err) {
        content.innerHTML = emptyState('⚠️', 'Could not load project details: ' + err.message);
    }
}

// ============================================================
// SUBMIT PROPOSAL  — with char counter wired
// ============================================================
function openProposalModal(projectId, title, budget) {
    document.getElementById('proposalProjectId').value = projectId;
    document.getElementById('proposalProjectInfo').innerHTML = `
        <strong>${escHtml(title)}</strong>
        <span style="display:block;margin-top:2px;color:var(--gray-600);font-size:13px">
            Client Budget: ${escHtml(budget)}
        </span>`;
    document.getElementById('proposalForm').reset();
    // reset char counter
    const counter = document.getElementById('coverLetterCount');
    if (counter) counter.textContent = '0 / 1000';
    document.getElementById('proposalProjectId').value = projectId;
    const alertEl = document.getElementById('proposalAlert');
    if (alertEl) alertEl.classList.add('hidden');
    openModal('proposalModal');
}

async function submitProposal() {
    const coverLetter = document.getElementById('coverLetter').value.trim();
    if (!coverLetter) {
        showInlineAlert('proposalAlert', 'Please write a cover letter.', 'error');
        return;
    }
    if (coverLetter.length > 1000) {
        showInlineAlert('proposalAlert', 'Cover letter must be 1000 characters or less.', 'error');
        return;
    }
    const btn = document.getElementById('submitProposalBtn');
    if (btn) { btn.disabled = true; btn.textContent = 'Submitting…'; }
    try {
        await Proposals.submit({
            projectId:      parseInt(document.getElementById('proposalProjectId').value),
            coverLetter,
            proposedBudget: document.getElementById('proposedBudget').value || null,
            estimatedDays:  document.getElementById('estimatedDays').value  || null,
        });
        closeModal('proposalModal');
        toast('Proposal submitted successfully!');
        await Promise.all([loadMyProposals(), loadFreelancerOverview()]);
    } catch (err) {
        showInlineAlert('proposalAlert', err.message, 'error');
    } finally {
        if (btn) { btn.disabled = false; btn.textContent = 'Submit Proposal'; }
    }
}

// ============================================================
// MY PROPOSALS
// ============================================================
async function loadMyProposals() {
    const el = document.getElementById('myProposalsList');
    if (!el) return;
    el.innerHTML = buildSkeletonList(3);
    try {
        const userInfo = {
            userId: sessionStorage.getItem('userId'),
            fullName: sessionStorage.getItem('fullName'),
            email: sessionStorage.getItem('email'),
            role: sessionStorage.getItem('role'),
        };
        const token = sessionStorage.getItem('token');
        console.log('Loading proposals for logged-in user:', userInfo);
        console.log('Logged-in user token:', token);

        const proposals = await Proposals.getMine();
        console.log('Response from GET /api/proposals/my:', proposals);
        if (!proposals.length) {
            el.innerHTML = emptyState('📝', 'No proposals submitted yet. Browse open projects to get started!');
            return;
        }
        el.innerHTML = proposals.map(prop => `
        <div class="proposal-card">
            <div style="flex:1">
                <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap;margin-bottom:6px">
                    <span class="card-title" style="margin:0">${escHtml(prop.project?.title || 'Project')}</span>
                    ${statusBadge(prop.status)}
                </div>
                <div class="card-meta">
                    <span>💰 Bid: ${formatCurrency(prop.proposedBudget)}</span>
                    <span>⏱ ${prop.estimatedDays ? prop.estimatedDays+' days' : 'N/A'}</span>
                    <span>🗓 ${formatDate(prop.createdAt)}</span>
                </div>
                <p class="card-desc">
                    ${escHtml((prop.coverLetter||'').substring(0,220))}${(prop.coverLetter||'').length>220?'…':''}
                </p>
            </div>
            ${prop.status === 'PENDING' ? `
            <div class="proposal-actions">
                <button class="btn btn-danger btn-sm" onclick="withdrawProposal(${prop.id})">Withdraw</button>
            </div>` : ''}
        </div>`).join('');
    } catch (err) {
        console.error('Failed to load proposals from GET /api/proposals/my:', err);
        el.innerHTML = emptyState('⚠️', 'Could not load proposals.');
    }
}

async function withdrawProposal(proposalId) {
    if (!confirm('Withdraw this proposal? This cannot be undone.')) return;
    try {
        await Proposals.delete(proposalId);
        toast('Proposal withdrawn');
        await Promise.all([loadMyProposals(), loadFreelancerOverview()]);
    } catch (err) {
        toast(err.message, 'error');
    }
}

// ============================================================
// HIRING REQUESTS (received)
// ============================================================
async function loadHiringRequests() {
    const el = document.getElementById('hiringRequestsList');
    if (!el) return;
    el.innerHTML = buildSkeletonList(3);
    try {
        const requests = await Hiring.getReceived();

        // Keep badge in sync
        updateHiringBadge(requests.filter(r => r.status === 'PENDING').length);

        if (!requests.length) {
            el.innerHTML = emptyState('🤝', 'No hiring requests received yet. Keep your profile up to date!');
            return;
        }
        el.innerHTML = requests.map(r => hiringCard(r, false)).join('');
    } catch (err) {
        el.innerHTML = emptyState('⚠️', 'Could not load hiring requests.');
    }
}

function hiringCard(r, compact) {
    const actions = !compact && r.status === 'PENDING' ? `
        <div class="proposal-actions">
            <button class="btn btn-success btn-sm" onclick="respondToHiring(${r.id},'ACCEPTED')">✅ Accept</button>
            <button class="btn btn-danger btn-sm"  onclick="respondToHiring(${r.id},'REJECTED')">❌ Reject</button>
        </div>` : '';

    return `
    <div class="proposal-card">
        <div style="flex:1">
            <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap;margin-bottom:6px">
                <span class="card-title" style="margin:0">
                    From: ${escHtml(r.client?.fullName || 'Client')}
                </span>
                ${statusBadge(r.status)}
            </div>
            <div class="card-meta">
                <span>📁 ${escHtml(r.project?.title || 'N/A')}</span>
                <span>💰 ${formatCurrency(r.project?.budget)}</span>
                <span>🗓 ${formatDate(r.createdAt)}</span>
            </div>
            ${r.message ? `<p class="card-desc">${escHtml(r.message)}</p>` : ''}
        </div>
        ${actions}
    </div>`;
}

async function respondToHiring(requestId, status) {
    if (!confirm(`${status === 'ACCEPTED' ? 'Accept' : 'Reject'} this hiring request?`)) return;
    try {
        await Hiring.respond(requestId, status);
        toast(status === 'ACCEPTED' ? '🎉 Hired! Project is now active.' : 'Request rejected.');
        await Promise.all([
            loadHiringRequests(),
            loadFreelancerOverview(),
            loadActiveProjects(),
        ]);
    } catch (err) {
        toast(err.message, 'error');
    }
}

// ============================================================
// ACTIVE PROJECTS (accepted hiring requests)
// ============================================================
async function loadActiveProjects() {
    const el = document.getElementById('activeProjectsList');
    if (!el) return;
    el.innerHTML = buildSkeletonList(2);
    try {
        const requests = await Hiring.getReceived();
        const accepted = requests.filter(r => r.status === 'ACCEPTED');
        if (!accepted.length) {
            el.innerHTML = emptyState('📁', 'No active projects yet. Accept a hiring request to start working!');
            return;
        }
        el.innerHTML = accepted.map(r => `
        <div class="project-card">
            <div style="display:flex;justify-content:space-between;align-items:flex-start;flex-wrap:wrap;gap:8px;margin-bottom:8px">
                <h3 class="card-title" style="margin:0">${escHtml(r.project?.title || 'Project')}</h3>
                ${statusBadge(r.project?.status || 'IN_PROGRESS')}
            </div>
            <div class="card-meta">
                <span>👤 Client: ${escHtml(r.client?.fullName || 'N/A')}</span>
                <span>💰 Budget: ${formatCurrency(r.project?.budget)}</span>
                <span>📅 Deadline: ${formatDate(r.project?.deadline)}</span>
            </div>
            <p class="card-desc">
                ${escHtml((r.project?.description||'').substring(0,200))}
            </p>
            ${renderSkillTags(r.project?.requiredSkills)}
        </div>`).join('');
    } catch (err) {
        el.innerHTML = emptyState('⚠️', 'Could not load active projects.');
    }
}
