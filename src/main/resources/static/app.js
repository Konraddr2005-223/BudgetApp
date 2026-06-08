

const API_BASE = '/api';

// State management
let accounts = [];
let limits = [];
let transactions = [];
let summary = null;

// Initial load
document.addEventListener('DOMContentLoaded', () => {
    // Set default date in transaction form to current local datetime
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    document.getElementById('tx-date').value = now.toISOString().slice(0, 16);

    // Load initial data
    loadData();
});

// Main data orchestrator
async function loadData() {
    try {
        await Promise.all([
            fetchAccounts(),
            fetchLimits(),
            fetchTransactions(),
            fetchSummary()
        ]);
        
        renderAccounts();
        renderLimits();
        renderTransactions();
        renderSummary();
    } catch (err) {
        console.error("Error loading application data:", err);
        showToast("Nie udało się pobrać danych z serwera. Upewnij się, że backend działa.", "error");
    }
}

// Fetch helper functions
async function fetchAccounts() {
    const res = await fetch(`${API_BASE}/accounts`);
    if (!res.ok) throw new Error("Failed to fetch accounts");
    accounts = await res.json();
}

async function fetchLimits() {
    const res = await fetch(`${API_BASE}/category-limits`);
    if (!res.ok) throw new Error("Failed to fetch limits");
    limits = await res.json();
}

async function fetchTransactions() {
    const from = document.getElementById('filter-from').value;
    const to = document.getElementById('filter-to').value;
    const category = document.getElementById('filter-category').value.trim();

    let url = `${API_BASE}/transactions`;
    const params = new URLSearchParams();
    if (from) params.append('from', from);
    if (to) params.append('to', to);
    if (category) params.append('category', category);

    if (params.toString()) {
        url += `?${params.toString()}`;
    }

    const res = await fetch(url);
    if (!res.ok) throw new Error("Failed to fetch transactions");
    transactions = await res.json();
}

async function fetchSummary() {
    const from = document.getElementById('filter-from').value;
    const to = document.getElementById('filter-to').value;

    let url = `${API_BASE}/summary`;
    const params = new URLSearchParams();
    if (from) params.append('from', from);
    if (to) params.append('to', to);

    if (params.toString()) {
        url += `?${params.toString()}`;
    }

    const res = await fetch(url);
    if (!res.ok) throw new Error("Failed to fetch summary");
    summary = await res.json();
}

// Reset filters
function resetFilters() {
    document.getElementById('filter-from').value = '';
    document.getElementById('filter-to').value = '';
    document.getElementById('filter-category').value = '';
    loadData();
}

// RENDERERS

// Render accounts
function renderAccounts() {
    const listEl = document.getElementById('accounts-list');
    const selectEl = document.getElementById('tx-account');
    
    // Clear list
    listEl.innerHTML = '';
    
    // Reset account selection dropdown in transaction form
    const currentVal = selectEl.value;
    selectEl.innerHTML = '<option value="">Wybierz konto...</option>';

    if (accounts.length === 0) {
        listEl.innerHTML = `
            <div class="empty-state">
                <i class="fa-solid fa-folder-open"></i>
                <p>Brak kont. Dodaj swoje pierwsze konto.</p>
            </div>
        `;
        return;
    }

    let globalBalance = 0;

    accounts.forEach(acc => {
        globalBalance += acc.balance;
        
        // Add to account manager list
        const item = document.createElement('div');
        item.className = 'account-item';
        item.innerHTML = `
            <div class="acc-info">
                <span class="acc-name">${escapeHtml(acc.name)}</span>
                <span class="acc-balance">${formatCurrency(acc.balance)} zł</span>
            </div>
            <div class="acc-actions">
                <button class="btn btn-secondary btn-icon-only btn-sm" onclick="exportCsv(${acc.id})" title="Eksportuj do CSV">
                    <i class="fa-solid fa-file-csv"></i>
                </button>
                <button class="btn btn-danger btn-icon-only btn-sm" onclick="deleteAccount(${acc.id})" title="Usuń konto">
                    <i class="fa-solid fa-trash"></i>
                </button>
            </div>
        `;
        listEl.appendChild(item);

        // Add to transaction account dropdown select
        const option = document.createElement('option');
        option.value = acc.id;
        option.textContent = acc.name;
        selectEl.appendChild(option);
    });

    // Restore select value if still valid
    if (accounts.some(a => a.id == currentVal)) {
        selectEl.value = currentVal;
    }

    // Set top global balance
    document.getElementById('global-balance').textContent = `${formatCurrency(globalBalance)} zł`;
}

// Render budget limits
function renderLimits() {
    const listEl = document.getElementById('limits-list');
    listEl.innerHTML = '';

    if (limits.length === 0) {
        listEl.innerHTML = `
            <div class="empty-state">
                <i class="fa-solid fa-shield-halved"></i>
                <p>Brak limitów. Zdefiniuj limity wydatków dla kategorii.</p>
            </div>
        `;
        return;
    }

    limits.forEach(lim => {
        const item = document.createElement('div');
        item.className = 'limit-item';
        item.innerHTML = `
            <div class="lim-info">
                <span class="lim-category">${escapeHtml(lim.category)}</span>
                <span class="lim-amount">Limit: <strong>${formatCurrency(lim.monthlyLimit)} zł</strong></span>
            </div>
            <div class="lim-actions">
                <button class="btn btn-danger btn-icon-only btn-sm" onclick="deleteLimit(${lim.id})" title="Usuń limit">
                    <i class="fa-solid fa-trash"></i>
                </button>
            </div>
        `;
        listEl.appendChild(item);
    });
}

// Render transactions
function renderTransactions() {
    const tbody = document.getElementById('transactions-list-body');
    tbody.innerHTML = '';

    if (transactions.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="text-center">
                    <div class="empty-state">
                        <i class="fa-solid fa-receipt"></i>
                        <p>Brak transakcji pasujących do filtrów.</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    transactions.forEach(tx => {
        const tr = document.createElement('tr');
        
        const dateStr = formatDate(tx.transactionDate);
        const amountClass = tx.type === 'INCOME' ? 'income' : 'expense';
        const amountPrefix = tx.type === 'INCOME' ? '+' : '-';
        
        tr.innerHTML = `
            <td class="tx-date-cell">${dateStr}</td>
            <td>
                <div class="tx-info-cell">
                    <span class="tx-description-text">${escapeHtml(tx.description || 'Transakcja')}</span>
                    <span class="tx-cat-badge">${escapeHtml(tx.category)}</span>
                </div>
            </td>
            <td class="tx-acc-cell">${escapeHtml(tx.accountName)}</td>
            <td class="tx-amount-cell ${amountClass}">${amountPrefix}${formatCurrency(tx.amount)} zł</td>
            <td>
                <button class="btn btn-danger btn-icon-only btn-sm" onclick="deleteTransaction(${tx.id})" title="Usuń transakcję">
                    <i class="fa-solid fa-trash"></i>
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// Render financial summary and charts
function renderSummary() {
    if (!summary) return;

    document.getElementById('summary-income').textContent = `${formatCurrency(summary.totalIncome)} zł`;
    document.getElementById('summary-expense').textContent = `${formatCurrency(summary.totalExpenses)} zł`;
    
    const net = summary.totalIncome - summary.totalExpenses;
    const netEl = document.getElementById('summary-net');
    netEl.textContent = `${formatCurrency(net)} zł`;
    
    if (net >= 0) {
        netEl.style.color = 'var(--color-income)';
    } else {
        netEl.style.color = 'var(--color-expense)';
    }

    // Render category expenses breakdown
    const breakdownEl = document.getElementById('expenses-breakdown');
    breakdownEl.innerHTML = '';

    const expensesMap = summary.expensesByCategory || {};
    const categories = Object.keys(expensesMap);

    if (categories.length === 0) {
        breakdownEl.innerHTML = `
            <div class="empty-state">
                <i class="fa-solid fa-chart-line"></i>
                <p>Brak wydatków do wyświetlenia struktury.</p>
            </div>
        `;
        return;
    }

    // Sort categories by expense amount descending
    categories.sort((a, b) => expensesMap[b] - expensesMap[a]);

    // Find the maximum category expense to scale the progress bars
    const maxExpense = Math.max(...Object.values(expensesMap));

    categories.forEach(cat => {
        const amount = expensesMap[cat];
        const percentOfMax = (amount / maxExpense) * 100;

        // Check if there is a limit for this category and if it is exceeded
        const limitObj = limits.find(l => l.category.toLowerCase() === cat.toLowerCase());
        const limitAmount = limitObj ? limitObj.monthlyLimit : null;
        const isExceeded = limitAmount !== null && amount > limitAmount;

        const chartItem = document.createElement('div');
        chartItem.className = `chart-item ${isExceeded ? 'exceeded' : ''}`;
        
        let limitText = '';
        if (limitAmount !== null) {
            limitText = ` <span class="text-muted" style="font-size:11px;">(Limit: ${formatCurrency(limitAmount)} zł)</span>`;
        }

        chartItem.innerHTML = `
            <div class="chart-header">
                <span class="chart-cat-name">${escapeHtml(cat)}${limitText}</span>
                <span class="chart-cat-amount">${formatCurrency(amount)} zł</span>
            </div>
            <div class="chart-track">
                <div class="chart-fill" style="width: ${percentOfMax}%"></div>
            </div>
        `;
        breakdownEl.appendChild(chartItem);
    });
}

// ACTIONS HANDLERS

// Create Account
async function handleAccountSubmit(e) {
    e.preventDefault();
    const name = document.getElementById('account-name').value.trim();
    const balance = parseFloat(document.getElementById('account-balance').value) || 0.00;

    try {
        const res = await fetch(`${API_BASE}/accounts`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, balance })
        });

        if (!res.ok) {
            const errorData = await res.json();
            throw new Error(errorData.message || "Błąd podczas tworzenia konta");
        }

        showToast("Konto zostało pomyślnie dodane!", "success");
        closeModal('account-modal');
        document.getElementById('account-form').reset();
        loadData();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// Create Limit
async function handleLimitSubmit(e) {
    e.preventDefault();
    const category = document.getElementById('limit-category').value.trim();
    const monthlyLimit = parseFloat(document.getElementById('limit-amount').value) || 0.00;

    try {
        const res = await fetch(`${API_BASE}/category-limits`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ category, monthlyLimit })
        });

        if (!res.ok) {
            const errorData = await res.json();
            throw new Error(errorData.message || "Błąd podczas tworzenia limitu");
        }

        showToast("Limit budżetowy został ustawiony!", "success");
        closeModal('limit-modal');
        document.getElementById('limit-form').reset();
        loadData();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// Create Transaction
async function handleTransactionSubmit(e) {
    e.preventDefault();
    
    const accountId = parseInt(document.getElementById('tx-account').value);
    const amount = parseFloat(document.getElementById('tx-amount').value);
    const type = document.querySelector('input[name="tx-type"]:checked').value;
    const category = document.getElementById('tx-category').value.trim();
    const description = document.getElementById('tx-description').value.trim();
    const transactionDateRaw = document.getElementById('tx-date').value;

    if (!accountId) {
        showToast("Wybierz konto", "error");
        return;
    }

    // Convert local datetime-local format to ISO standard for backend (YYYY-MM-DDTHH:MM:SS)
    const transactionDate = transactionDateRaw ? new Date(transactionDateRaw).toISOString().slice(0, 19) : null;

    try {
        const res = await fetch(`${API_BASE}/transactions`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                accountId,
                amount,
                type,
                category,
                description,
                transactionDate
            })
        });

        if (!res.ok) {
            const errorData = await res.json();
            throw new Error(errorData.message || "Błąd podczas dodawania transakcji");
        }

        // Check for budget limit warning header
        const budgetWarning = res.headers.get('X-Budget-Warning');
        if (budgetWarning) {
            const decodedWarning = decodeURIComponent(budgetWarning);
            showToast(decodedWarning, "warning");
        } else {
            showToast("Transakcja została zapisana pomyślnie!", "success");
        }

        closeModal('transaction-modal');
        
        // Reset form except select and date
        document.getElementById('tx-amount').value = '';
        document.getElementById('tx-category').value = '';
        document.getElementById('tx-description').value = '';
        
        loadData();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// Delete Account
async function deleteAccount(id) {
    if (!confirm("Czy na pewno chcesz usunąć to konto?")) return;

    try {
        const res = await fetch(`${API_BASE}/accounts/${id}`, { method: 'DELETE' });

        if (!res.ok) {
            const errorData = await res.json();
            throw new Error(errorData.message || "Błąd podczas usuwania konta");
        }

        showToast("Konto zostało usunięte.", "success");
        loadData();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// Delete Transaction
async function deleteTransaction(id) {
    if (!confirm("Czy na pewno chcesz usunąć tę transakcję? Saldo konta zostanie przywrócone.")) return;

    try {
        const res = await fetch(`${API_BASE}/transactions/${id}`, { method: 'DELETE' });

        if (!res.ok) {
            const errorData = await res.json();
            throw new Error(errorData.message || "Błąd podczas usuwania transakcji");
        }

        showToast("Transakcja została usunięta.", "success");
        loadData();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// Delete Limit
async function deleteLimit(id) {
    if (!confirm("Czy na pewno chcesz usunąć ten limit budżetowy?")) return;

    try {
        const res = await fetch(`${API_BASE}/category-limits/${id}`, { method: 'DELETE' });

        if (!res.ok) {
            const errorData = await res.json();
            throw new Error(errorData.message || "Błąd podczas usuwania limitu");
        }

        showToast("Limit budżetowy został usunięty.", "success");
        loadData();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// Export transactions to CSV
function exportCsv(accountId) {
    const url = `${API_BASE}/accounts/${accountId}/transactions/export`;
    // Create temporary download link
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', '');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showToast("Rozpoczęto pobieranie pliku CSV.", "success");
}

// MODALS UTILS
function showModal(id) {
    const modal = document.getElementById(id);
    modal.style.display = 'flex';
    setTimeout(() => modal.classList.add('active'), 10);
}

function closeModal(id) {
    const modal = document.getElementById(id);
    modal.classList.remove('active');
    setTimeout(() => modal.style.display = 'none', 300);
}

// Close modals when clicking overlay
window.onclick = function(event) {
    if (event.target.classList.contains('modal-overlay')) {
        closeModal(event.target.id);
    }
};

// TOAST SYSTEM UTILS
function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    let iconClass = 'fa-circle-check';
    if (type === 'error') iconClass = 'fa-triangle-exclamation';
    if (type === 'warning') iconClass = 'fa-triangle-exclamation';

    toast.innerHTML = `
        <i class="fa-solid ${iconClass}"></i>
        <div class="toast-message">${escapeHtml(message)}</div>
        <button class="toast-close" onclick="this.parentElement.remove()">&times;</button>
    `;
    
    container.appendChild(toast);

    // Auto remove after 5 seconds
    setTimeout(() => {
        toast.style.animation = 'slideIn 0.3s ease reverse forwards';
        setTimeout(() => toast.remove(), 300);
    }, type === 'warning' ? 8000 : 5000); // Give user more time to read warnings
}

// HELPERS
function formatCurrency(value) {
    if (value === undefined || value === null) return '0.00';
    return Number(value).toLocaleString('pl-PL', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleString('pl-PL', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function escapeHtml(text) {
    if (!text) return '';
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
