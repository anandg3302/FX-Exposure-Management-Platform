// FX Exposure Management Platform - Frontend Client Application

let authToken = localStorage.getItem('fx_jwt_token') || '';
let currentUser = null;
let cachedExposures = [];
let cachedHedges = [];

// Initialize application on page load
document.addEventListener('DOMContentLoaded', async () => {
    if (!authToken) {
        await quickLogin('admin@example.com', 'Admin@123', false);
    } else {
        await loadCurrentUser();
    }
    loadDashboard();
});

// Toast notification helper
function showToast(message, isError = false) {
    const toast = document.getElementById('notification-toast');
    toast.innerText = message;
    toast.style.borderColor = isError ? 'var(--danger)' : 'var(--success)';
    toast.style.color = isError ? '#fca5a5' : '#6ee7b7';
    toast.style.display = 'block';
    setTimeout(() => {
        toast.style.display = 'none';
    }, 3500);
}

// HTTP API Request Wrapper
async function apiRequest(endpoint, method = 'GET', body = null) {
    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    };
    if (authToken) {
        headers['Authorization'] = `Bearer ${authToken}`;
    }

    try {
        const options = { method, headers };
        if (body) {
            options.body = JSON.stringify(body);
        }

        const response = await fetch(endpoint, options);
        const json = await response.json();

        if (!response.ok) {
            const errorMsg = json.message || (json.error ? `${json.error}: ${json.message}` : 'Request failed');
            throw new Error(errorMsg);
        }

        return json;
    } catch (err) {
        console.error(`API Error on ${endpoint}:`, err);
        showToast(err.message, true);
        throw err;
    }
}

// Tab Switching
function switchTab(tabId) {
    document.querySelectorAll('.tab-pane').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.nav-tab').forEach(el => el.classList.remove('active'));

    const targetPane = document.getElementById(`tab-${tabId}`);
    if (targetPane) targetPane.classList.add('active');

    // Highlight active nav tab
    event.currentTarget.classList.add('active');

    // Load data for selected tab
    switch (tabId) {
        case 'dashboard': loadDashboard(); break;
        case 'exposures': loadExposures(); break;
        case 'hedges': loadHedges(); break;
        case 'allocations': loadAllocations(); break;
        case 'stresstest': executeStressTest(); break;
        case 'policies': loadPolicies(); break;
        case 'audit': loadAuditLogs(); break;
    }
}

// Modals
function openModal(id) {
    document.getElementById(id).classList.add('active');
}

function closeModal(id) {
    document.getElementById(id).classList.remove('active');
}

// Number formatting helpers
function formatUSD(val) {
    if (val === null || val === undefined) return '$0.00';
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val);
}

function formatNumber(val) {
    if (val === null || val === undefined) return '0.00';
    return new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(val);
}

// Auth & User Profile
async function loadCurrentUser() {
    try {
        const res = await apiRequest('/api/auth/me');
        currentUser = res.data;
        document.getElementById('current-user-name').innerText = currentUser.name || currentUser.email;
        document.getElementById('current-user-role').innerText = currentUser.role;
    } catch (err) {
        console.warn('Could not load user profile with current token, re-authenticating as admin');
        await quickLogin('admin@example.com', 'Admin@123', false);
    }
}

async function quickLogin(email, password, reload = true) {
    try {
        const res = await apiRequest('/api/auth/login', 'POST', { email, password });
        authToken = res.data.token;
        localStorage.setItem('fx_jwt_token', authToken);
        currentUser = res.data.user;

        document.getElementById('current-user-name').innerText = currentUser.name;
        document.getElementById('current-user-role').innerText = currentUser.role;
        closeModal('modal-auth');
        showToast(`Logged in as ${currentUser.name} (${currentUser.role})`);

        if (reload) {
            loadDashboard();
        }
    } catch (err) {
        showToast('Login failed: ' + err.message, true);
    }
}

async function submitCustomLogin() {
    const email = document.getElementById('auth-email').value;
    const password = document.getElementById('auth-password').value;
    if (!email || !password) {
        showToast('Please enter email and password', true);
        return;
    }
    await quickLogin(email, password, true);
}

function showAuthModal() {
    openModal('modal-auth');
}

// ==================== DASHBOARD ====================
async function loadDashboard() {
    try {
        const res = await apiRequest('/api/dashboard/overview');
        const data = res.data;

        // Update KPIs
        document.getElementById('kpi-net-exposure').innerText = formatUSD(data.totalNetOpenExposure);
        document.getElementById('kpi-hedged-amount').innerText = formatUSD(data.totalHedgedAmount);
        document.getElementById('kpi-hedge-ratio').innerText = `${data.overallHedgeRatio}%`;
        document.getElementById('kpi-portfolio-mtm').innerText = formatUSD(data.portfolioMtM);
        document.getElementById('kpi-portfolio-mtm').className = `kpi-value ${data.portfolioMtM >= 0 ? 'positive' : 'negative'}`;
        document.getElementById('kpi-active-alerts').innerText = data.activeAlertsCount;
        document.getElementById('kpi-active-alerts').className = `kpi-value ${data.activeAlertsCount > 0 ? 'warning-txt' : ''}`;

        // Render Currency Breakdown
        const ccyBody = document.getElementById('currency-breakdown-body');
        if (data.currencyBreakdown && data.currencyBreakdown.length > 0) {
            ccyBody.innerHTML = data.currencyBreakdown.map(c => `
                <tr>
                    <td><strong>${c.currency}</strong></td>
                    <td>${formatNumber(c.grossExposure)} ${c.currency}</td>
                    <td>${formatNumber(c.hedgedAmount)} ${c.currency}</td>
                    <td><strong class="warning-txt">${formatNumber(c.netOpenExposure)} ${c.currency}</strong></td>
                    <td>
                        <div style="font-size: 0.8rem; font-weight: 600;">${c.hedgeRatioPercent}%</div>
                        <div class="progress-bar-bg">
                            <div class="progress-bar-fill" style="width: ${Math.min(100, c.hedgeRatioPercent)}%;"></div>
                        </div>
                    </td>
                    <td>${formatUSD(c.grossExposureInBase)}</td>
                </tr>
            `).join('');
        } else {
            ccyBody.innerHTML = `<tr><td colspan="6" style="text-align: center;">No active open exposures.</td></tr>`;
        }

        // Render Latest Rates
        const ratesBody = document.getElementById('latest-rates-body');
        if (data.latestRates && data.latestRates.length > 0) {
            ratesBody.innerHTML = data.latestRates.map(r => `
                <tr>
                    <td><strong>${r.baseCurrency}/${r.quoteCurrency}</strong></td>
                    <td>${r.rate.toFixed(4)}</td>
                    <td><span class="badge badge-active">${r.rateType}</span></td>
                </tr>
            `).join('');
        }

        // Render Maturity Ladder
        const ladderBody = document.getElementById('maturity-ladder-body');
        if (data.maturityLadder && data.maturityLadder.length > 0) {
            ladderBody.innerHTML = data.maturityLadder.map(b => `
                <tr>
                    <td><strong>${b.bucketName}</strong></td>
                    <td class="positive">${formatUSD(b.grossInflowsInBase)}</td>
                    <td class="negative">${formatUSD(b.grossOutflowsInBase)}</td>
                    <td>${formatUSD(b.netExposureInBase)}</td>
                    <td>${formatUSD(b.hedgedAmountInBase)}</td>
                    <td><strong class="warning-txt">${formatUSD(b.netOpenExposureInBase)}</strong></td>
                </tr>
            `).join('');
        }

        // Load VaR Analytics
        loadVaR();
    } catch (err) {
        console.error('Failed to load dashboard data', err);
    }
}

async function loadVaR() {
    try {
        const res = await apiRequest('/api/analytics/var');
        const varData = res.data;
        document.getElementById('var-95-val').innerText = `${formatUSD(varData.var95Percent)} (${varData.var95PercentOfPortfolio}%)`;
        document.getElementById('var-99-val').innerText = `${formatUSD(varData.var99Percent)} (${varData.var99PercentOfPortfolio}%)`;
        document.getElementById('var-notional-val').innerText = formatUSD(varData.totalPortfolioNotionalInBase);
    } catch (err) {
        console.error('Failed to load VaR metrics', err);
    }
}

async function executeConversion() {
    const amount = document.getElementById('conv-amount').value;
    const fromCurrency = document.getElementById('conv-from').value;
    const toCurrency = document.getElementById('conv-to').value;

    if (!amount) return;

    try {
        const res = await apiRequest('/api/rates/convert', 'POST', { fromCurrency, toCurrency, amount: parseFloat(amount) });
        document.getElementById('conv-result').innerText = `${formatNumber(res.data.convertedAmount)} ${toCurrency} (Rate: ${res.data.exchangeRate.toFixed(4)})`;
    } catch (err) {
        document.getElementById('conv-result').innerText = 'Conversion failed';
    }
}

// Rates Modal
function showRateModal() {
    openModal('modal-rate');
}

async function submitRateUpdate() {
    const baseCurrency = document.getElementById('rate-base').value.toUpperCase();
    const quoteCurrency = document.getElementById('rate-quote').value.toUpperCase();
    const rate = parseFloat(document.getElementById('rate-value').value);
    const rateType = document.getElementById('rate-type').value;

    if (!baseCurrency || !quoteCurrency || isNaN(rate)) {
        showToast('Please provide valid currency pair and rate', true);
        return;
    }

    try {
        await apiRequest('/api/rates/update', 'POST', { baseCurrency, quoteCurrency, rate, rateType });
        closeModal('modal-rate');
        showToast(`Updated rate for ${baseCurrency}/${quoteCurrency}`);
        loadDashboard();
    } catch (err) {
        // handled in apiRequest
    }
}

// ==================== EXPOSURES ====================
async function loadExposures() {
    try {
        const res = await apiRequest('/api/exposures');
        cachedExposures = res.data;
        renderExposuresTable(cachedExposures);
    } catch (err) {
        console.error('Failed to load exposures', err);
    }
}

function renderExposuresTable(exposures) {
    const tbody = document.getElementById('exposures-table-body');
    if (!exposures || exposures.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" style="text-align: center;">No exposures found.</td></tr>`;
        return;
    }

    tbody.innerHTML = exposures.map(e => {
        let statusBadge = 'badge-unhedged';
        if (e.status === 'PARTIALLY_HEDGED') statusBadge = 'badge-partially';
        if (e.status === 'FULLY_HEDGED') statusBadge = 'badge-fully';
        if (e.status === 'SETTLED') statusBadge = 'badge-settled';

        const dirBadge = e.cashFlowDirection === 'INFLOW' ? 'badge-inflow' : 'badge-outflow';

        return `
            <tr>
                <td><strong>${e.exposureReference}</strong></td>
                <td>
                    <div>${e.companyEntity}</div>
                    <div style="font-size: 0.75rem; color: var(--text-muted);">${e.description || '-'}</div>
                </td>
                <td>
                    <span class="badge ${dirBadge}">${e.cashFlowDirection}</span>
                    <div style="font-size: 0.75rem; color: var(--text-muted);">${e.exposureType}</div>
                </td>
                <td><strong>${formatNumber(e.amount)} ${e.currency}</strong></td>
                <td>${e.valueDate}</td>
                <td><span class="badge ${statusBadge}">${e.status}</span></td>
                <td>${formatNumber(e.hedgedAmount)} ${e.currency}</td>
                <td><strong>${e.hedgeRatioPercent}%</strong></td>
                <td>
                    <button class="btn btn-danger btn-sm" onclick="deleteExposure(${e.id})">Delete</button>
                </td>
            </tr>
        `;
    }).join('');
}

function filterExposures() {
    const ccy = document.getElementById('exp-filter-currency').value;
    const status = document.getElementById('exp-filter-status').value;

    const filtered = cachedExposures.filter(e => {
        const matchesCcy = !ccy || e.currency === ccy;
        const matchesStatus = !status || e.status === status;
        return matchesCcy && matchesStatus;
    });

    renderExposuresTable(filtered);
}

function showCreateExposureModal() {
    const today = new Date();
    today.setDate(today.getDate() + 30);
    document.getElementById('exp-date').value = today.toISOString().split('T')[0];
    openModal('modal-exposure');
}

async function submitCreateExposure() {
    const companyEntity = document.getElementById('exp-entity').value;
    const exposureType = document.getElementById('exp-type').value;
    const cashFlowDirection = document.getElementById('exp-direction').value;
    const currency = document.getElementById('exp-currency').value.toUpperCase();
    const amount = parseFloat(document.getElementById('exp-amount').value);
    const valueDate = document.getElementById('exp-date').value;
    const description = document.getElementById('exp-desc').value;

    if (!companyEntity || !currency || isNaN(amount) || !valueDate) {
        showToast('Please fill all required exposure fields', true);
        return;
    }

    try {
        await apiRequest('/api/exposures', 'POST', {
            companyEntity, exposureType, cashFlowDirection, currency, amount, valueDate, description
        });
        closeModal('modal-exposure');
        showToast('Exposure created successfully');
        loadExposures();
    } catch (err) {
    }
}

async function deleteExposure(id) {
    if (!confirm('Are you sure you want to delete this exposure?')) return;
    try {
        await apiRequest(`/api/exposures/${id}`, 'DELETE');
        showToast('Exposure deleted');
        loadExposures();
    } catch (err) {
    }
}

// ==================== HEDGE CONTRACTS ====================
async function loadHedges() {
    try {
        const res = await apiRequest('/api/hedges');
        cachedHedges = res.data;
        renderHedgesTable(cachedHedges);
    } catch (err) {
        console.error('Failed to load hedge deals', err);
    }
}

function renderHedgesTable(hedges) {
    const tbody = document.getElementById('hedges-table-body');
    if (!hedges || hedges.length === 0) {
        tbody.innerHTML = `<tr><td colspan="13" style="text-align: center;">No hedge contracts found.</td></tr>`;
        return;
    }

    tbody.innerHTML = hedges.map(h => {
        const mtmClass = h.currentMtM >= 0 ? 'positive' : 'negative';
        const plClass = h.realizedGainLoss >= 0 ? 'positive' : 'negative';

        return `
            <tr>
                <td><strong>${h.dealReference}</strong></td>
                <td><span class="badge badge-active">${h.hedgeType}</span></td>
                <td><span class="badge ${h.direction === 'BUY' ? 'badge-inflow' : 'badge-outflow'}">${h.direction}</span></td>
                <td><strong>${h.primaryCurrency}/${h.secondaryCurrency}</strong></td>
                <td>${formatNumber(h.primaryAmount)} ${h.primaryCurrency}</td>
                <td>${h.strikeRate.toFixed(4)}</td>
                <td>${h.currentMarketRate ? h.currentMarketRate.toFixed(4) : '-'}</td>
                <td>${h.valueDate}</td>
                <td>${h.counterpartyBank}</td>
                <td><span class="badge badge-partially">${h.status}</span></td>
                <td><strong class="${mtmClass}">${formatUSD(h.currentMtM)}</strong></td>
                <td><span class="${plClass}">${formatUSD(h.realizedGainLoss)}</span></td>
                <td style="display: flex; gap: 0.3rem;">
                    ${h.status === 'ACTIVE' ? `
                        <button class="btn btn-success btn-sm" onclick="showSettleModal(${h.id}, '${h.dealReference}')">Settle</button>
                    ` : ''}
                    <button class="btn btn-danger btn-sm" onclick="deleteHedge(${h.id})">Delete</button>
                </td>
            </tr>
        `;
    }).join('');
}

function showBookHedgeModal() {
    const today = new Date();
    today.setDate(today.getDate() + 30);
    document.getElementById('hdg-date').value = today.toISOString().split('T')[0];
    openModal('modal-hedge');
}

async function submitBookHedge() {
    const hedgeType = document.getElementById('hdg-type').value;
    const direction = document.getElementById('hdg-direction').value;
    const primaryCurrency = document.getElementById('hdg-prim-curr').value.toUpperCase();
    const secondaryCurrency = document.getElementById('hdg-sec-curr').value.toUpperCase();
    const primaryAmount = parseFloat(document.getElementById('hdg-prim-amount').value);
    const strikeRate = parseFloat(document.getElementById('hdg-strike').value);
    const valueDate = document.getElementById('hdg-date').value;
    const counterpartyBank = document.getElementById('hdg-bank').value;
    const premiumAmount = parseFloat(document.getElementById('hdg-premium').value || '0');

    if (!primaryCurrency || !secondaryCurrency || isNaN(primaryAmount) || isNaN(strikeRate) || !valueDate || !counterpartyBank) {
        showToast('Please fill all required deal fields', true);
        return;
    }

    try {
        await apiRequest('/api/hedges', 'POST', {
            hedgeType, direction, primaryCurrency, secondaryCurrency, primaryAmount,
            strikeRate, valueDate, counterpartyBank, premiumAmount, tradeDate: new Date().toISOString().split('T')[0]
        });
        closeModal('modal-hedge');
        showToast('Hedge deal booked successfully');
        loadHedges();
    } catch (err) {
    }
}

function showSettleModal(id, dealRef) {
    document.getElementById('settle-deal-id').value = id;
    document.getElementById('settle-deal-ref').value = dealRef;
    openModal('modal-settle');
}

async function submitSettleHedge() {
    const id = document.getElementById('settle-deal-id').value;
    const rateStr = document.getElementById('settle-rate').value;
    const notes = document.getElementById('settle-notes').value;

    const payload = { notes };
    if (rateStr) {
        payload.settlementRate = parseFloat(rateStr);
    }

    try {
        await apiRequest(`/api/hedges/${id}/settle`, 'POST', payload);
        closeModal('modal-settle');
        showToast('Hedge deal settled successfully');
        loadHedges();
    } catch (err) {
    }
}

async function revalueAllHedges() {
    try {
        await apiRequest('/api/hedges/revalue-all', 'POST');
        showToast('All active hedge contracts revalued');
        loadHedges();
    } catch (err) {
    }
}

async function deleteHedge(id) {
    if (!confirm('Are you sure you want to delete this hedge contract?')) return;
    try {
        await apiRequest(`/api/hedges/${id}`, 'DELETE');
        showToast('Hedge contract deleted');
        loadHedges();
    } catch (err) {
    }
}

// ==================== ALLOCATIONS ====================
async function loadAllocations() {
    try {
        const res = await apiRequest('/api/allocations');
        const allocations = res.data;
        const tbody = document.getElementById('allocations-table-body');

        if (!allocations || allocations.length === 0) {
            tbody.innerHTML = `<tr><td colspan="9" style="text-align: center;">No active allocations.</td></tr>`;
            return;
        }

        tbody.innerHTML = allocations.map(a => `
            <tr>
                <td>#${a.id}</td>
                <td><strong>${a.exposureReference}</strong></td>
                <td><strong>${a.dealReference}</strong></td>
                <td><span class="badge badge-active">${a.exposureCurrency}</span></td>
                <td>${formatNumber(a.allocatedAmount)} ${a.exposureCurrency}</td>
                <td>${a.effectiveRate ? a.effectiveRate.toFixed(4) : '-'}</td>
                <td>${a.allocationDate}</td>
                <td>${a.notes || '-'}</td>
                <td>
                    <button class="btn btn-danger btn-sm" onclick="deallocate(${a.id})">Unlink</button>
                </td>
            </tr>
        `).join('');
    } catch (err) {
        console.error('Failed to load allocations', err);
    }
}

async function showCreateAllocationModal() {
    await loadExposures();
    await loadHedges();

    const expSelect = document.getElementById('alloc-exposure-select');
    expSelect.innerHTML = cachedExposures
        .filter(e => e.status !== 'SETTLED' && e.status !== 'FULLY_HEDGED')
        .map(e => `<option value="${e.id}">${e.exposureReference} (${e.currency} ${formatNumber(e.unhedgedAmount)} unhedged)</option>`)
        .join('');

    const hdgSelect = document.getElementById('alloc-hedge-select');
    hdgSelect.innerHTML = cachedHedges
        .filter(h => h.status === 'ACTIVE' && h.unallocatedAmount > 0)
        .map(h => `<option value="${h.id}">${h.dealReference} (${h.primaryCurrency} ${formatNumber(h.unallocatedAmount)} available)</option>`)
        .join('');

    openModal('modal-allocation');
}

async function submitCreateAllocation() {
    const exposureId = parseInt(document.getElementById('alloc-exposure-select').value);
    const hedgeContractId = parseInt(document.getElementById('alloc-hedge-select').value);
    const allocatedAmount = parseFloat(document.getElementById('alloc-amount').value);
    const notes = document.getElementById('alloc-notes').value;

    if (isNaN(exposureId) || isNaN(hedgeContractId) || isNaN(allocatedAmount)) {
        showToast('Please select exposure, hedge deal and specify amount', true);
        return;
    }

    try {
        await apiRequest('/api/allocations', 'POST', {
            exposureId, hedgeContractId, allocatedAmount, notes
        });
        closeModal('modal-allocation');
        showToast('Hedge successfully allocated to exposure');
        loadAllocations();
    } catch (err) {
    }
}

async function deallocate(id) {
    if (!confirm('Are you sure you want to unlink this allocation?')) return;
    try {
        await apiRequest(`/api/allocations/${id}`, 'DELETE');
        showToast('Allocation unlinked');
        loadAllocations();
    } catch (err) {
    }
}

// ==================== SCENARIO STRESS TESTING ====================
function updateSliderLabel(type) {
    const val = document.getElementById(`shock-${type}`).value;
    const label = document.getElementById(`shock-${type}-val`);
    label.innerText = `${val > 0 ? '+' : ''}${val}%`;
    label.className = `slider-val ${val >= 0 ? 'positive' : 'negative'}`;
}

async function executeStressTest() {
    const uniform = parseFloat(document.getElementById('shock-uniform').value);
    const eur = parseFloat(document.getElementById('shock-eur').value);
    const gbp = parseFloat(document.getElementById('shock-gbp').value);
    const jpy = parseFloat(document.getElementById('shock-jpy').value);
    const inr = parseFloat(document.getElementById('shock-inr').value);
    const cad = parseFloat(document.getElementById('shock-cad').value);

    const currencyShocks = {
        EUR: eur,
        GBP: gbp,
        JPY: jpy,
        INR: inr,
        CAD: cad
    };

    try {
        const res = await apiRequest('/api/analytics/stress-test', 'POST', {
            uniformPercentageShock: uniform,
            currencyShocks
        });

        const data = res.data;
        document.getElementById('sim-exp-impact').innerText = formatUSD(data.exposureGainLossInBase);
        document.getElementById('sim-exp-impact').className = `kpi-value ${data.exposureGainLossInBase >= 0 ? 'positive' : 'negative'}`;

        document.getElementById('sim-hdg-impact').innerText = formatUSD(data.hedgeGainLossInBase);
        document.getElementById('sim-hdg-impact').className = `kpi-value ${data.hedgeGainLossInBase >= 0 ? 'positive' : 'negative'}`;

        document.getElementById('sim-net-impact').innerText = formatUSD(data.netPortfolioImpactInBase);
        document.getElementById('sim-net-impact').className = `kpi-value ${data.netPortfolioImpactInBase >= 0 ? 'positive' : 'negative'}`;

        document.getElementById('sim-net-percent').innerText = `${data.netImpactPercent}% Net Portfolio Impact`;
        document.getElementById('sim-narrative-summary').innerText = data.summary;
    } catch (err) {
        console.error('Failed to run stress test', err);
    }
}

// ==================== POLICIES & ALERTS ====================
async function loadPolicies() {
    try {
        const res = await apiRequest('/api/risk-policies');
        const policies = res.data;
        const tbody = document.getElementById('policies-table-body');

        if (!policies || policies.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align: center;">No risk policies defined.</td></tr>`;
        } else {
            tbody.innerHTML = policies.map(p => `
                <tr>
                    <td><strong>${p.policyName}</strong></td>
                    <td><span class="badge badge-active">${p.currency}</span></td>
                    <td>${formatUSD(p.maxUnhedgedExposure)}</td>
                    <td><strong>${p.minHedgeRatio}%</strong></td>
                    <td>${formatUSD(p.maxCounterpartyExposure)}</td>
                    <td><span class="badge ${p.active ? 'badge-fully' : 'badge-settled'}">${p.active ? 'ACTIVE' : 'INACTIVE'}</span></td>
                </tr>
            `).join('');
        }

        loadAlerts();
    } catch (err) {
        console.error('Failed to load risk policies', err);
    }
}

async function loadAlerts() {
    try {
        const res = await apiRequest('/api/alerts/active');
        const alerts = res.data;
        const tbody = document.getElementById('alerts-table-body');

        if (!alerts || alerts.length === 0) {
            tbody.innerHTML = `<tr><td colspan="4" style="text-align: center; color: var(--success);">🟢 All risk policies compliant. No active alerts.</td></tr>`;
            return;
        }

        tbody.innerHTML = alerts.map(a => {
            let sevBadge = 'badge-info';
            if (a.severity === 'WARNING') sevBadge = 'badge-warning';
            if (a.severity === 'CRITICAL') sevBadge = 'badge-critical';

            return `
                <tr>
                    <td><span class="badge ${sevBadge}">${a.severity}</span></td>
                    <td><strong>${a.alertType}</strong></td>
                    <td>${a.message}</td>
                    <td>
                        <button class="btn btn-secondary btn-sm" onclick="acknowledgeAlert(${a.id})">Ack</button>
                        <button class="btn btn-success btn-sm" onclick="resolveAlert(${a.id})">Resolve</button>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        console.error('Failed to load alerts', err);
    }
}

async function runComplianceCheck() {
    try {
        const res = await apiRequest('/api/risk-policies/check-compliance', 'POST');
        showToast(`Compliance check finished: ${res.data.length} alerts generated/active`);
        loadAlerts();
    } catch (err) {
    }
}

async function acknowledgeAlert(id) {
    try {
        await apiRequest(`/api/alerts/${id}/acknowledge`, 'PATCH');
        showToast('Alert acknowledged');
        loadAlerts();
    } catch (err) {
    }
}

async function resolveAlert(id) {
    try {
        await apiRequest(`/api/alerts/${id}/resolve`, 'PATCH');
        showToast('Alert resolved');
        loadAlerts();
    } catch (err) {
    }
}

function showCreatePolicyModal() {
    openModal('modal-policy');
}

async function submitCreatePolicy() {
    const policyName = document.getElementById('pol-name').value;
    const currency = document.getElementById('pol-currency').value.toUpperCase();
    const minHedgeRatio = parseFloat(document.getElementById('pol-min-ratio').value);
    const maxUnhedgedExposure = parseFloat(document.getElementById('pol-max-unhedged').value);
    const maxCounterpartyExposure = parseFloat(document.getElementById('pol-max-bank').value);
    const description = document.getElementById('pol-desc').value;

    if (!policyName || isNaN(minHedgeRatio) || isNaN(maxUnhedgedExposure) || isNaN(maxCounterpartyExposure)) {
        showToast('Please fill all policy fields', true);
        return;
    }

    try {
        await apiRequest('/api/risk-policies', 'POST', {
            policyName, currency, minHedgeRatio, maxUnhedgedExposure, maxCounterpartyExposure, description, active: true
        });
        closeModal('modal-policy');
        showToast('Risk policy created successfully');
        loadPolicies();
    } catch (err) {
    }
}

// ==================== AUDIT TRAIL ====================
async function loadAuditLogs() {
    try {
        const res = await apiRequest('/api/audit-logs');
        const logs = res.data;
        const tbody = document.getElementById('audit-table-body');

        if (!logs || logs.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align: center;">No audit logs recorded yet.</td></tr>`;
            return;
        }

        tbody.innerHTML = logs.map(l => `
            <tr>
                <td>${l.timestamp ? l.timestamp.replace('T', ' ').substring(0, 19) : '-'}</td>
                <td><span class="badge badge-active">${l.action}</span></td>
                <td>${l.entityName}</td>
                <td>#${l.entityId || '-'}</td>
                <td>${l.performedBy}</td>
                <td>${l.details || '-'}</td>
            </tr>
        `).join('');
    } catch (err) {
        console.error('Failed to load audit logs', err);
    }
}

