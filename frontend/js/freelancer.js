/* ============================================================
   Freelancer Dashboard Logic
   — profile progress bar, project detail modal, char counter,
     notification badge, skeleton loaders, all features wired
   ============================================================ */

const freelancerUserId = parseInt(sessionStorage.getItem('userId'));
let freelancerPaymentRecords = [];

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
    loadFreelancerPayments();
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
            ? hiring.slice(0, 3).map(hiringCard).join('')
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
        await Freelancers.saveProfile(body);
        showInlineAlert('profileAlert', 'Profile saved successfully!', 'success');
        toast('Profile saved!');
        await loadMyProfile();
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
    const openProjects = projects.filter(project => project.status === 'OPEN');
    el.innerHTML = openProjects.length
        ? openProjects.map(p => projectCard(p)).join('')
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
async function openProjectDetailModal(projectId, allowProposal = true) {
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
                ${allowProposal ? `<button class="btn btn-primary"
                    onclick="closeModal('projectDetailModal');openProposalModal(${p.id},
                        '${escHtml(p.title).replace(/'/g,"\\'")}',
                        '${formatCurrency(p.budget).replace(/'/g,"\\'")}')">
                    📝 Submit Proposal
                </button>` : ''}
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
                    ${prop.project?.status === 'SUBMITTED' ? statusBadge(prop.project.status) : ''}
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
            ${prop.status === 'ACCEPTED' && prop.project?.id && ['HIRED', 'IN_PROGRESS'].includes(prop.project?.status) ? `
            <div class="proposal-actions">
                <button class="btn btn-primary btn-sm" onclick="submitProjectWork(${prop.project.id})">Submit Work</button>
            </div>` : prop.status === 'PENDING' ? `
            <div class="proposal-actions">
                <button class="btn btn-danger btn-sm" onclick="withdrawProposal(${prop.id})">Withdraw</button>
            </div>` : ''}
        </div>`).join('');
    } catch (err) {
        console.error('Failed to load proposals from GET /api/proposals/my:', err);
        el.innerHTML = emptyState('⚠️', 'Could not load proposals.');
    }
}

async function submitProjectWork(projectId) {
    if (!confirm('Submit your work for client review?')) return;
    try {
        await Projects.submitWork(projectId);
        toast('Work submitted to the client!');
        await Promise.all([loadMyProposals(), loadActiveProjects()]);
    } catch (err) {
        toast(err.message, 'error');
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
        el.innerHTML = requests.map(hiringCard).join('');
    } catch (err) {
        el.innerHTML = emptyState('⚠️', 'Could not load hiring requests.');
    }
}

function hiringCard(r) {
    const actions = r.status === 'PENDING' ? `
        <div class="proposal-actions">
            <button class="btn btn-success btn-sm" onclick="respondToHiring(${r.id},'ACCEPTED')">✅ Accept</button>
            <button class="btn btn-danger btn-sm" onclick="respondToHiring(${r.id},'REJECTED')">❌ Reject</button>
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
    if (status !== 'ACCEPTED' && status !== 'REJECTED') return;
    const action = status === 'ACCEPTED' ? Hiring.accept : Hiring.reject;
    const verb = status === 'ACCEPTED' ? 'Accept' : 'Reject';
    if (!confirm(`${verb} this hiring request?`)) return;
    try {
        await action(requestId);
        toast(status === 'ACCEPTED' ? '🎉 Hired! Project is now active.' : 'Request rejected.');
        await Promise.all([
            loadHiringRequests(),
            loadFreelancerOverview(),
            loadActiveProjects(),
            loadFreelancerPayments(),
        ]);
    } catch (err) {
        toast(err.message, 'error');
    }
}

// ============================================================
// ASSIGNED PROJECTS
// ============================================================
async function loadActiveProjects() {
    const activeEl = document.getElementById('activeProjectsList');
    const completedEl = document.getElementById('completedProjectsList');
    if (!activeEl || !completedEl) return;
    activeEl.innerHTML = buildSkeletonList(2);
    completedEl.innerHTML = buildSkeletonList(2);
    try {
        const [requests, proposals] = await Promise.all([
            Hiring.getReceived(),
            Proposals.getMine(),
        ]);
        const assignedProjects = new Map();
        requests
            .filter(request => request.status === 'ACCEPTED' && request.project)
            .forEach(request => assignedProjects.set(request.project.id, request.project));
        proposals
            .filter(proposal => proposal.status === 'ACCEPTED' && proposal.project)
            .forEach(proposal => assignedProjects.set(proposal.project.id, proposal.project));

        const projects = [...assignedProjects.values()];
        const activeProjects = projects.filter(project =>
            project.status === 'IN_PROGRESS' || project.status === 'ACTIVE');
        const completedProjects = projects.filter(project => project.status === 'COMPLETED');

        activeEl.innerHTML = activeProjects.length
            ? activeProjects.map(project => assignedProjectCard(project, true)).join('')
            : emptyState('📁', 'No active projects');
        completedEl.innerHTML = completedProjects.length
            ? completedProjects.map(project => assignedProjectCard(project, false)).join('')
            : emptyState('✅', 'No completed projects');
    } catch (err) {
        activeEl.innerHTML = emptyState('⚠️', 'Could not load active projects.');
        completedEl.innerHTML = emptyState('⚠️', 'Could not load completed projects.');
    }
}

function assignedProjectCard(project, active) {
    return `
        <article class="project-card">
            <div style="display:flex;justify-content:space-between;align-items:flex-start;flex-wrap:wrap;gap:8px;margin-bottom:8px">
                <h3 class="card-title" style="margin:0">${escHtml(project.title || 'Project')}</h3>
                ${statusBadge(active ? project.status : 'COMPLETED')}
            </div>
            <div class="card-meta">
                <span>👤 Client: ${escHtml(project.client?.fullName || 'N/A')}</span>
                <span>💰 Budget: ${formatCurrency(project.budget)}</span>
                <span>📅 Deadline: ${formatDate(project.deadline)}</span>
            </div>
            <p class="card-desc">${escHtml((project.description || '').substring(0, 200))}</p>
            ${renderSkillTags(project.requiredSkills)}
            <div class="proposal-actions" style="margin-top:12px">
                <button class="btn btn-outline btn-sm"
                    onclick="openProjectDetailModal(${project.id}, false)">View Details</button>
                ${active && project.status === 'IN_PROGRESS'
                    ? `<button class="btn btn-primary btn-sm"
                        onclick="submitProjectWork(${project.id})">Submit Work</button>`
                    : ''}
            </div>
        </article>`;
}

async function loadFreelancerPayments() {
    const el = document.getElementById('freelancerPaymentsList');
    if (!el) return;
    el.innerHTML = buildSkeletonList(2);
    try {
        const earnings = await Payments.getFreelancerEarnings();
        const payments = earnings.payments || [];
        freelancerPaymentRecords = payments;
        const total = document.getElementById('totalEarnings');
        if (total) total.textContent = formatCurrency(earnings.totalEarnings);
        if (!payments.length) {
            el.innerHTML = emptyState('💳', 'Your payment history will appear here.');
            return;
        }
        el.innerHTML = `
            <table class="payments-table freelancer-payments-table">
                <thead><tr>
                    <th>Transaction ID</th>
                    <th>Project Name</th>
                    <th>Received From</th>
                    <th>Payment Date &amp; Time</th>
                    <th>Payment Method</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th>Action</th>
                </tr></thead>
                <tbody>${payments.map(payment => `
                    <tr>
                        <td class="payment-transaction-id">${escHtml(payment.transactionId || '—')}</td>
                        <td class="payment-project-name">${escHtml(payment.projectTitle || 'Project')}</td>
                        <td>${escHtml(payment.clientName || 'Client')}</td>
                        <td>${formatDateTime(payment.paymentDate)}</td>
                        <td>${payment.paymentMethod ? paymentMethodLabel(payment.paymentMethod) : '—'}</td>
                        <td class="payment-amount">${payment.status === 'REFUNDED' ? '−' : ''}${formatCurrency(payment.amount)}</td>
                        <td>${payment.status === 'PAID'
                            ? '<span class="payment-status-badge is-paid">Paid · Completed</span>'
                            : payment.status === 'REFUNDED'
                                ? `<span class="payment-status-badge is-refunded">Refunded · ${escHtml(formatDateTime(payment.refundDate))}</span>`
                                : '<span class="payment-status-badge is-pending">Pending</span>'}</td>
                        <td class="payment-actions">${payment.status === 'PAID' || payment.status === 'REFUNDED'
                            ? `<button class="btn btn-outline btn-sm" onclick="viewFreelancerPaymentReceipt(${payment.projectId})">View Receipt</button>`
                            : '<span class="payment-action-placeholder">Awaiting payment</span>'}</td>
                    </tr>`).join('')}</tbody>
            </table>`;
    } catch (err) {
        el.innerHTML = emptyState('⚠️', 'Could not load payment history.');
        console.error('Payment history error:', err);
    }
}

function viewFreelancerPaymentReceipt(projectId) {
    const payment = freelancerPaymentRecords.find(record => record.projectId === projectId);
    if (payment) viewPaymentReceipt(payment, 'Received From', payment.clientName);
}
