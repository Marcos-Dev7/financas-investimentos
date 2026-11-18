const API_BASE = window.location.origin.includes('localhost:8080')
    ? ''
    : 'http://localhost:8080';

const MESES = [
    'janeiro', 'fevereiro', 'marco', 'abril', 'maio', 'junho',
    'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'
];

const MESES_LABEL = {
    janeiro: 'Janeiro',
    fevereiro: 'Fevereiro',
    marco: 'Março',
    abril: 'Abril',
    maio: 'Maio',
    junho: 'Junho',
    julho: 'Julho',
    agosto: 'Agosto',
    setembro: 'Setembro',
    outubro: 'Outubro',
    novembro: 'Novembro',
    dezembro: 'Dezembro'
};

const state = {
    mesAtual: MESES[new Date().getMonth()],
    filtroDespesa: 'mes', // 'mes' | 'pendentes' | 'pagas' | 'todas'
    buscaDespesa: '',
    apenasSaldosMes: true,
    page: 0,
    size: 20,
    totalPages: 1,
    despesas: [],
    saldos: []
};

// Formatadores
const formatCurrency = (valor) => {
    const num = Number(valor || 0);
    return num.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
};

// Converte YYYY-MM-DD (input date) para dd/MM/yyyy (backend @JsonFormat)
const toBackendDate = (isoDate) => {
    if (!isoDate) return null;
    const [yyyy, mm, dd] = isoDate.split('-');
    return `${dd}/${mm}/${yyyy}`;
};

// Converte dd/MM/yyyy (backend) para YYYY-MM-DD (input date)
const toInputDate = (brDate) => {
    if (!brDate) return '';
    if (brDate.includes('-')) return brDate;
    const [dd, mm, yyyy] = brDate.split('/');
    return `${yyyy}-${mm}-${dd}`;
};

const showToast = (msg, type = 'success') => {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = msg;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 3800);
};

const setApiOnline = (online) => {
    const badge = document.getElementById('apiStatusBadge');
    const text = document.getElementById('apiStatusText');
    badge.className = `api-badge ${online ? 'online' : 'offline'}`;
    text.textContent = online ? 'API Online' : 'API Offline';
};

async function apiFetch(path, options = {}) {
    try {
        const response = await fetch(`${API_BASE}${path}`, {
            headers: {
                'Content-Type': 'application/json',
                ...(options.headers || {})
            },
            ...options
        });

        setApiOnline(true);

        if (!response.ok) {
            let errMsg = `Erro HTTP ${response.status}`;
            try {
                const errData = await response.json();
                if (Array.isArray(errData)) {
                    errMsg = errData.map(e => `${e.campo}: ${e.mensagem}`).join(' | ');
                } else if (errData.mensagem) {
                    errMsg = errData.mensagem;
                }
            } catch (_) {
                const text = await response.text();
                if (text) errMsg = text;
            }
            throw new Error(errMsg);
        }

        if (response.status === 204 || response.status === 201) {
            return null;
        }

        const text = await response.text();
        return text ? JSON.parse(text) : null;
    } catch (error) {
        if (error.name === 'TypeError') {
            setApiOnline(false);
        }
        throw error;
    }
}

// 1. Carregar Resumo do Mês (/calculos/sobra/{mes})
async function carregarResumoMes() {
    try {
        const resumo = await apiFetch(`/calculos/sobra/${state.mesAtual}`);
        if (!resumo) return;

        document.getElementById('kpiReceitaTotal').textContent = formatCurrency(resumo.receitaTotal);
        document.getElementById('kpiDespesasPagas').textContent = formatCurrency(resumo.despesasPagas);
        document.getElementById('kpiDespesasBrutas').textContent = formatCurrency(resumo.despesasBrutas);
        document.getElementById('kpiSaldoDisponivel').textContent = formatCurrency(resumo.saldoDisponivel);

        const badge = document.getElementById('badgeStatusFinanceiro');
        badge.textContent = resumo.status;
        badge.className = `status-pill status-${resumo.status}`;

        const brutas = Number(resumo.despesasBrutas || 0);
        const pagas = Number(resumo.despesasPagas || 0);
        const pct = brutas > 0 ? Math.min(100, Math.round((pagas / brutas) * 100)) : 0;

        document.getElementById('progressPagas').style.width = `${pct}%`;
        document.getElementById('kpiProgressoTexto').textContent = `${pct}% das despesas do mês quitadas`;
    } catch (err) {
        console.error('Erro ao carregar resumo:', err);
    }
}

// 2. Carregar Despesas (/despesas)
async function carregarDespesas() {
    try {
        const pageData = await apiFetch(`/despesas?page=${state.page}&size=${state.size}&sort=dataVencimento,asc`);
        state.despesas = pageData?.content || [];
        state.totalPages = Math.max(1, pageData?.totalPages || 1);
        renderizarDespesas();
    } catch (err) {
        document.getElementById('tbodyDespesas').innerHTML = `
            <tr><td colspan="7" class="empty-state">Não foi possível carregar as despesas (${err.message}).</td></tr>
        `;
    }
}

function renderizarDespesas() {
    const tbody = document.getElementById('tbodyDespesas');
    const termo = state.buscaDespesa.trim().toLowerCase();

    const filtradas = state.despesas.filter(d => {
        const mesItem = (d.mes || '').toLowerCase();
        if (state.filtroDespesa === 'mes' && mesItem !== state.mesAtual) return false;
        if (state.filtroDespesa === 'pendentes' && (mesItem !== state.mesAtual || d.isPaga)) return false;
        if (state.filtroDespesa === 'pagas' && (mesItem !== state.mesAtual || !d.isPaga)) return false;

        if (termo) {
            const desc = (d.descricao || '').toLowerCase();
            const cat = (d.categoria || '').toLowerCase();
            return desc.includes(termo) || cat.includes(termo);
        }
        return true;
    });

    document.getElementById('pageInfo').textContent = `Página ${state.page + 1} de ${state.totalPages}`;
    document.getElementById('btnPrevPage').disabled = state.page <= 0;
    document.getElementById('btnNextPage').disabled = state.page + 1 >= state.totalPages;

    if (filtradas.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="empty-state">
                    Nenhuma despesa encontrada para este filtro em <strong>${MESES_LABEL[state.mesAtual]}</strong>.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = filtradas.map(d => `
        <tr>
            <td>
                <span class="badge ${d.isPaga ? 'badge-paga' : 'badge-pendente'}">
                    ${d.isPaga ? '✓ Paga' : '⏳ Pendente'}
                </span>
            </td>
            <td>
                <strong>${escapeHtml(d.descricao)}</strong>
                <div class="income-meta">${MESES_LABEL[(d.mes || '').toLowerCase()] || d.mes}</div>
            </td>
            <td>
                <span class="badge badge-cat">${escapeHtml(d.categoria || 'Sem categoria')}</span>
            </td>
            <td>${escapeHtml(d.dataVencimento || '-')}</td>
            <td>
                ${d.isFixa ? '<span class="badge badge-fixa">Fixa</span>' : '<span class="income-meta">Variável</span>'}
            </td>
            <td class="text-right"><strong>${formatCurrency(d.valor)}</strong></td>
            <td class="text-right">
                <div class="action-group">
                    ${d.isPaga
                        ? `<button class="btn btn-outline btn-sm" onclick="estornarDespesa(${d.id})" title="Estornar pagamento">↩ Estornar</button>`
                        : `<button class="btn btn-success btn-sm" onclick="pagarDespesa(${d.id})" title="Dar baixa como paga">✓ Pagar</button>`
                    }
                    <button class="btn btn-outline btn-sm" onclick="abrirEditarDespesa(${d.id})" title="Editar">✎</button>
                    <button class="btn btn-danger-ghost btn-sm" onclick="excluirDespesa(${d.id})" title="Excluir">🗑</button>
                </div>
            </td>
        </tr>
    `).join('');
}

// 3. Carregar Saldos (/saldos)
async function carregarSaldos() {
    try {
        const lista = await apiFetch('/saldos');
        state.saldos = Array.isArray(lista) ? lista : [];
        renderizarSaldos();
    } catch (err) {
        document.getElementById('listaSaldos').innerHTML = `
            <div class="empty-state">Não foi possível carregar as receitas (${err.message}).</div>
        `;
    }
}

function renderizarSaldos() {
    const container = document.getElementById('listaSaldos');
    const filtrados = state.saldos.filter(s => {
        if (!state.apenasSaldosMes) return true;
        return (s.mes || '').toLowerCase() === state.mesAtual;
    });

    if (filtrados.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                Nenhuma receita cadastrada ${state.apenasSaldosMes ? `em <strong>${MESES_LABEL[state.mesAtual]}</strong>` : ''}.
            </div>
        `;
        return;
    }

    container.innerHTML = filtrados.map(s => `
        <div class="income-item">
            <div class="income-info">
                <h4>${escapeHtml(s.descricao)}</h4>
                <span class="income-meta">${MESES_LABEL[(s.mes || '').toLowerCase()] || s.mes} / ${s.ano}</span>
            </div>
            <div class="income-right">
                <span class="income-amount">${formatCurrency(s.valor)}</span>
                <div class="action-group">
                    <button class="btn btn-outline btn-sm" onclick="abrirEditarSaldo(${s.id})" title="Editar">✎</button>
                    <button class="btn btn-danger-ghost btn-sm" onclick="excluirSaldo(${s.id})" title="Excluir">🗑</button>
                </div>
            </div>
        </div>
    `).join('');
}

// Ações de Despesa
window.pagarDespesa = async (id) => {
    try {
        await apiFetch(`/despesas/${id}/pagar`, { method: 'PATCH' });
        showToast('Despesa marcada como paga!');
        await atualizarTudo();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

window.estornarDespesa = async (id) => {
    try {
        await apiFetch(`/despesas/${id}/estornar`, { method: 'PATCH' });
        showToast('Pagamento estornado com sucesso!');
        await atualizarTudo();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

window.excluirDespesa = async (id) => {
    if (!confirm('Deseja realmente excluir esta despesa?')) return;
    try {
        await apiFetch(`/despesas/${id}`, { method: 'DELETE' });
        showToast('Despesa excluída!');
        await atualizarTudo();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

window.abrirEditarDespesa = (id) => {
    const d = state.despesas.find(item => item.id === id);
    if (!d) return;

    document.getElementById('modalDespesaTitulo').textContent = 'Editar Despesa';
    document.getElementById('despesaId').value = d.id;
    document.getElementById('despesaDescricao').value = d.descricao || '';
    document.getElementById('despesaValor').value = d.valor || '';
    document.getElementById('despesaDataVencimento').value = toInputDate(d.dataVencimento);
    document.getElementById('despesaMes').value = (d.mes || state.mesAtual).toLowerCase();
    document.getElementById('despesaCategoria').value = d.categoria || '';
    document.getElementById('despesaIsFixa').checked = Boolean(d.isFixa);

    document.getElementById('modalDespesa').classList.remove('hidden');
};

// Ações de Saldo
window.abrirEditarSaldo = (id) => {
    const s = state.saldos.find(item => item.id === id);
    if (!s) return;

    document.getElementById('modalSaldoTitulo').textContent = 'Editar Receita';
    document.getElementById('saldoId').value = s.id;
    document.getElementById('saldoDescricao').value = s.descricao || '';
    document.getElementById('saldoValor').value = s.valor || '';
    document.getElementById('saldoMes').value = (s.mes || state.mesAtual).toLowerCase();
    document.getElementById('saldoAno').value = s.ano || new Date().getFullYear();

    document.getElementById('modalSaldo').classList.remove('hidden');
};

window.excluirSaldo = async (id) => {
    if (!confirm('Deseja realmente remover esta fonte de renda?')) return;
    try {
        await apiFetch(`/saldos/${id}`, { method: 'DELETE' });
        showToast('Receita removida!');
        await atualizarTudo();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

function escapeHtml(str) {
    return String(str ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

async function atualizarTudo() {
    await Promise.all([
        carregarResumoMes(),
        carregarDespesas(),
        carregarSaldos()
    ]);
}

// Inicialização de Eventos
document.addEventListener('DOMContentLoaded', () => {
    const selectMes = document.getElementById('selectMesGlobal');
    selectMes.value = state.mesAtual;

    selectMes.addEventListener('change', (e) => {
        state.mesAtual = e.target.value;
        atualizarTudo();
    });

    document.getElementById('btnPrevMes').addEventListener('click', () => {
        const idx = (MESES.indexOf(state.mesAtual) - 1 + MESES.length) % MESES.length;
        state.mesAtual = MESES[idx];
        selectMes.value = state.mesAtual;
        atualizarTudo();
    });

    document.getElementById('btnNextMes').addEventListener('click', () => {
        const idx = (MESES.indexOf(state.mesAtual) + 1) % MESES.length;
        state.mesAtual = MESES[idx];
        selectMes.value = state.mesAtual;
        atualizarTudo();
    });

    // Abas de filtro de despesas
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            state.filtroDespesa = btn.dataset.filter;
            renderizarDespesas();
        });
    });

    document.getElementById('inputBuscaDespesa').addEventListener('input', (e) => {
        state.buscaDespesa = e.target.value;
        renderizarDespesas();
    });

    document.getElementById('chkApenasSaldosMes').addEventListener('change', (e) => {
        state.apenasSaldosMes = e.target.checked;
        renderizarSaldos();
    });

    // Paginação
    document.getElementById('btnPrevPage').addEventListener('click', () => {
        if (state.page > 0) {
            state.page--;
            carregarDespesas();
        }
    });

    document.getElementById('btnNextPage').addEventListener('click', () => {
        if (state.page + 1 < state.totalPages) {
            state.page++;
            carregarDespesas();
        }
    });

    // Modal Nova Despesa
    const modalDespesa = document.getElementById('modalDespesa');
    document.getElementById('btnNovaDespesa').addEventListener('click', () => {
        document.getElementById('formDespesa').reset();
        document.getElementById('despesaId').value = '';
        document.getElementById('modalDespesaTitulo').textContent = 'Nova Despesa';
        document.getElementById('despesaMes').value = state.mesAtual;
        document.getElementById('despesaDataVencimento').value = new Date().toISOString().split('T')[0];
        modalDespesa.classList.remove('hidden');
    });

    const fecharModalDespesa = () => modalDespesa.classList.add('hidden');
    document.getElementById('btnFecharModalDespesa').addEventListener('click', fecharModalDespesa);
    document.getElementById('btnCancelarDespesa').addEventListener('click', fecharModalDespesa);

    document.getElementById('formDespesa').addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('despesaId').value;
        const payload = {
            descricao: document.getElementById('despesaDescricao').value.trim(),
            valor: Number(document.getElementById('despesaValor').value),
            mes: document.getElementById('despesaMes').value,
            dataVencimento: toBackendDate(document.getElementById('despesaDataVencimento').value),
            categoria: document.getElementById('despesaCategoria').value.trim() || 'Outros',
            isFixa: document.getElementById('despesaIsFixa').checked
        };

        try {
            if (id) {
                await apiFetch(`/despesas/${id}`, {
                    method: 'PUT',
                    body: JSON.stringify(payload)
                });
                showToast('Despesa atualizada com sucesso!');
            } else {
                await apiFetch('/despesas', {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });
                showToast('Despesa cadastrada com sucesso!');
            }
            fecharModalDespesa();
            await atualizarTudo();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Modal Novo Saldo
    const modalSaldo = document.getElementById('modalSaldo');
    document.getElementById('btnNovoSaldo').addEventListener('click', () => {
        document.getElementById('formSaldo').reset();
        document.getElementById('saldoId').value = '';
        document.getElementById('modalSaldoTitulo').textContent = 'Nova Receita';
        document.getElementById('saldoMes').value = state.mesAtual;
        document.getElementById('saldoAno').value = new Date().getFullYear();
        modalSaldo.classList.remove('hidden');
    });

    const fecharModalSaldo = () => modalSaldo.classList.add('hidden');
    document.getElementById('btnFecharModalSaldo').addEventListener('click', fecharModalSaldo);
    document.getElementById('btnCancelarSaldo').addEventListener('click', fecharModalSaldo);

    document.getElementById('formSaldo').addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('saldoId').value;
        const payload = {
            descricao: document.getElementById('saldoDescricao').value.trim(),
            valor: Number(document.getElementById('saldoValor').value),
            mes: document.getElementById('saldoMes').value,
            ano: Number(document.getElementById('saldoAno').value)
        };

        try {
            if (id) {
                await apiFetch(`/saldos/${id}`, {
                    method: 'PUT',
                    body: JSON.stringify(payload)
                });
                showToast('Receita atualizada com sucesso!');
            } else {
                await apiFetch('/saldos', {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });
                showToast('Receita cadastrada com sucesso!');
            }
            fecharModalSaldo();
            await atualizarTudo();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    atualizarTudo();
});
