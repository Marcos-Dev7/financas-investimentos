const API_BASE = window.location.origin.includes('localhost:8080') || window.location.protocol.startsWith('http')
    ? ''
    : 'http://localhost:8080';

const STORAGE_TOKEN_KEY = 'ff_auth_token';
const STORAGE_USER_KEY = 'ff_auth_user';

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
    token: localStorage.getItem(STORAGE_TOKEN_KEY) || null,
    user: JSON.parse(localStorage.getItem(STORAGE_USER_KEY) || 'null'),
    abaAtiva: 'dashboard', // 'dashboard' | 'account'
    pendingVerifyEmail: '',
    mesAtual: MESES[new Date().getMonth()],
    filtroDespesa: 'mes',
    buscaDespesa: '',
    apenasSaldosMes: true,
    page: 0,
    size: 50,
    totalPages: 1,
    totalDespesasCount: 0,
    despesas: [],
    saldos: [],
    resumo: null
};

const formatCurrency = (valor) => {
    const num = Number(valor || 0);
    return num.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
};

const toBackendDate = (isoDate) => {
    if (!isoDate) return null;
    const [yyyy, mm, dd] = isoDate.split('-');
    return `${dd}/${mm}/${yyyy}`;
};

const toInputDate = (brDate) => {
    if (!brDate) return '';
    if (brDate.includes('-')) return brDate;
    const [dd, mm, yyyy] = brDate.split('/');
    return `${yyyy}-${mm}-${dd}`;
};

const getInitials = (name) => {
    if (!name) return 'U';
    return name
        .trim()
        .split(/\s+/)
        .slice(0, 2)
        .map(p => p[0].toUpperCase())
        .join('');
};

const showToast = (msg, type = 'success') => {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = msg;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 4200);
};

// Regras de Senha Forte
function avaliarRegrasSenha(senha) {
    const s = senha || '';
    return {
        len: s.length >= 8 && s.length <= 100,
        upper: /[A-Z]/.test(s),
        lower: /[a-z]/.test(s),
        num: /[0-9]/.test(s),
        spec: /[!@#$%^&*(),.?":{}|<>_\-+=/\\\[\];'~`]/.test(s)
    };
}

function atualizarChecklistSenha(senha, containerId) {
    const rules = avaliarRegrasSenha(senha);
    const container = document.getElementById(containerId);
    if (!container) return false;

    Object.entries(rules).forEach(([key, ok]) => {
        const el = container.querySelector(`[data-rule="${key}"]`);
        if (el) {
            el.classList.toggle('valid', ok);
        }
    });

    return Object.values(rules).every(Boolean);
}

function atualizarDicaConfirmacao(senha, confirmacao, hintId) {
    const hint = document.getElementById(hintId);
    if (!hint) return;
    if (!confirmacao) {
        hint.textContent = '';
        hint.className = 'match-hint';
        return;
    }
    if (senha === confirmacao) {
        hint.textContent = '✓ As senhas coincidem';
        hint.className = 'match-hint ok';
    } else {
        hint.textContent = '✗ As senhas não coincidem';
        hint.className = 'match-hint err';
    }
}

function salvarSessao(authData) {
    state.token = authData.token;
    state.user = {
        id: authData.id,
        nome: authData.nome,
        email: authData.email,
        emailVerificado: true,
        fotoPerfil: authData.fotoPerfil !== undefined ? authData.fotoPerfil : null
    };
    localStorage.setItem(STORAGE_TOKEN_KEY, state.token);
    localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(state.user));
}

function renderizarAvatarElemento(initialsElId, imgElId, nome, fotoBase64) {
    const initialsEl = document.getElementById(initialsElId);
    const imgEl = document.getElementById(imgElId);
    if (!initialsEl || !imgEl) return;

    if (fotoBase64) {
        imgEl.src = fotoBase64;
        imgEl.classList.remove('hidden');
        initialsEl.classList.add('hidden');
    } else {
        imgEl.src = '';
        imgEl.classList.add('hidden');
        initialsEl.textContent = getInitials(nome);
        initialsEl.classList.remove('hidden');
    }
}

function atualizarDadosUsuarioNaTela() {
    if (!state.user) return;
    document.getElementById('userNameDisplay').textContent = state.user.nome;
    document.getElementById('userEmailDisplay').textContent = state.user.email;

    renderizarAvatarElemento('userAvatarInitials', 'userAvatarImg', state.user.nome, state.user.fotoPerfil);
    renderizarAvatarElemento('perfilAvatarLg', 'perfilAvatarImgLg', state.user.nome, state.user.fotoPerfil);

    const btnRemoverFoto = document.getElementById('btnRemoverFoto');
    if (btnRemoverFoto) {
        btnRemoverFoto.classList.toggle('hidden', !state.user.fotoPerfil);
    }

    // Área Minha Conta
    document.getElementById('perfilNomeHeader').textContent = state.user.nome;
    document.getElementById('perfilEmailHeader').textContent = state.user.email;
    document.getElementById('perfilInputEmail').value = state.user.email;
    document.getElementById('perfilInputNome').value = state.user.nome;
}

function navegarPara(aba) {
    state.abaAtiva = aba;
    const dashboardView = document.getElementById('dashboardView');
    const familiaView = document.getElementById('familiaView');
    const accountView = document.getElementById('accountView');
    const navDashboardBtn = document.getElementById('navDashboardBtn');
    const navFamiliaBtn = document.getElementById('navFamiliaBtn');
    const navAccountBtn = document.getElementById('navAccountBtn');
    const monthSelectorBar = document.getElementById('monthSelectorBar');

    dashboardView.classList.add('hidden');
    if (familiaView) familiaView.classList.add('hidden');
    accountView.classList.add('hidden');

    navDashboardBtn.classList.remove('active');
    if (navFamiliaBtn) navFamiliaBtn.classList.remove('active');
    navAccountBtn.classList.remove('active');

    if (aba === 'account') {
        accountView.classList.remove('hidden');
        navAccountBtn.classList.add('active');
        monthSelectorBar.classList.add('hidden');
        atualizarDadosUsuarioNaTela();
        document.getElementById('statTotalDespesas').textContent = String(state.totalDespesasCount || state.despesas.length);
        document.getElementById('statTotalSaldos').textContent = String(state.saldos.length);
    } else if (aba === 'familia') {
        if (familiaView) familiaView.classList.remove('hidden');
        if (navFamiliaBtn) navFamiliaBtn.classList.add('active');
        monthSelectorBar.classList.remove('hidden');
        carregarModuloFamilia();
    } else {
        dashboardView.classList.remove('hidden');
        navDashboardBtn.classList.add('active');
        monthSelectorBar.classList.remove('hidden');
    }
}

function limparSessao() {
    state.token = null;
    state.user = null;
    localStorage.removeItem(STORAGE_TOKEN_KEY);
    localStorage.removeItem(STORAGE_USER_KEY);
    mostrarTelaAuth('login');
}

function mostrarTelaAuth(modo = 'login', emailVerificacao = '', codigoDemo = null) {
    document.getElementById('authScreen').classList.remove('hidden');
    document.getElementById('appScreen').classList.add('hidden');

    const tabsWrap = document.getElementById('authTabsWrap');
    const tabLoginBtn = document.getElementById('tabLoginBtn');
    const tabRegisterBtn = document.getElementById('tabRegisterBtn');
    const formLogin = document.getElementById('formLogin');
    const formRegister = document.getElementById('formRegister');
    const formVerifyEmail = document.getElementById('formVerifyEmail');

    formLogin.classList.add('hidden');
    formRegister.classList.add('hidden');
    formVerifyEmail.classList.add('hidden');

    if (modo === 'verify') {
        tabsWrap.classList.add('hidden');
        formVerifyEmail.classList.remove('hidden');
        state.pendingVerifyEmail = emailVerificacao;
        document.getElementById('verifyEmailTarget').textContent = emailVerificacao;
        document.getElementById('inputCodigoVerificacao').value = '';
    } else if (modo === 'register') {
        tabsWrap.classList.remove('hidden');
        tabRegisterBtn.classList.add('active');
        tabLoginBtn.classList.remove('active');
        formRegister.classList.remove('hidden');
    } else {
        tabsWrap.classList.remove('hidden');
        tabLoginBtn.classList.add('active');
        tabRegisterBtn.classList.remove('active');
        formLogin.classList.remove('hidden');
    }
}

function mostrarTelaApp() {
    document.getElementById('authScreen').classList.add('hidden');
    document.getElementById('appScreen').classList.remove('hidden');
    navegarPara('dashboard');
    atualizarDadosUsuarioNaTela();
}

async function apiFetch(path, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...(options.headers || {})
    };

    if (state.token) {
        headers['Authorization'] = `Bearer ${state.token}`;
    }

    const response = await fetch(`${API_BASE}${path}`, {
        ...options,
        headers
    });

    if (response.status === 401 && !path.startsWith('/auth/')) {
        limparSessao();
        throw new Error('Sessão expirada. Faça login novamente.');
    }

    if (!response.ok) {
        let errMsg = `Erro HTTP ${response.status}`;
        const rawText = await response.text();
        if (rawText) {
            try {
                const errData = JSON.parse(rawText);
                if (Array.isArray(errData)) {
                    errMsg = errData.map(e => `${e.campo}: ${e.mensagem}`).join(' | ');
                } else if (errData.mensagem) {
                    errMsg = errData.mensagem;
                }
            } catch (_) {
                errMsg = rawText;
            }
        }
        throw new Error(errMsg);
    }

    if (response.status === 204) {
        return null;
    }

    const text = await response.text();
    return text ? JSON.parse(text) : null;
}

// 1. Carregar Resumo do Mês (/calculos/sobra/{mes})
async function carregarResumoMes() {
    try {
        const resumo = await apiFetch(`/calculos/sobra/${state.mesAtual}`);
        if (!resumo) return;
        state.resumo = resumo;

        const receita = Number(resumo.receitaTotal || 0);
        const pagas = Number(resumo.despesasPagas || 0);
        const brutas = Number(resumo.despesasBrutas || 0);
        const faltaPagar = Math.max(0, brutas - pagas);

        document.getElementById('kpiReceitaTotal').textContent = formatCurrency(receita);
        document.getElementById('kpiDespesasPagas').textContent = formatCurrency(pagas);
        document.getElementById('kpiFaltaPagar').textContent = `Falta pagar no mês: ${formatCurrency(faltaPagar)}`;
        document.getElementById('kpiDespesasBrutas').textContent = formatCurrency(brutas);
        document.getElementById('kpiSaldoDisponivel').textContent = formatCurrency(resumo.saldoDisponivel);

        const badge = document.getElementById('badgeStatusFinanceiro');
        badge.textContent = resumo.status;
        badge.className = `status-pill status-${resumo.status}`;

        const pctQuitado = brutas > 0 ? Math.min(100, Math.round((pagas / brutas) * 100)) : 0;
        document.getElementById('progressPagas').style.width = `${pctQuitado}%`;
        document.getElementById('kpiProgressoTexto').textContent = `${pctQuitado}% das despesas do mês quitadas`;

        const pctComprometido = receita > 0 ? Math.min(100, Math.round((brutas / receita) * 100)) : (brutas > 0 ? 100 : 0);
        document.getElementById('lblComprometimento').textContent = `${pctComprometido}%`;
        document.getElementById('barComprometimento').style.width = `${pctComprometido}%`;

        const txtRec = document.getElementById('txtSaudeRecomendacao');
        if (receita === 0 && brutas === 0) {
            txtRec.textContent = 'Cadastre suas receitas e despesas do mês para acompanhar a saúde do orçamento.';
        } else if (pctComprometido <= 70) {
            txtRec.textContent = 'Excelente! Mais de 30% da sua renda está livre para reserva ou investimentos.';
        } else if (pctComprometido <= 95) {
            txtRec.textContent = 'Atenção: Suas despesas previstas consomem boa parte da renda mensal.';
        } else {
            txtRec.textContent = 'Alerta: As despesas previstas atingiram ou superaram a receita cadastrada no mês.';
        }
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
        state.totalDespesasCount = pageData?.totalElements ?? state.despesas.length;
        renderizarDespesas();
        renderizarDistribuicaoCategorias();
    } catch (err) {
        document.getElementById('tbodyDespesas').innerHTML = `
            <tr><td colspan="7" class="empty-state">Não foi possível carregar as despesas (${err.message}).</td></tr>
        `;
    }
}

function renderizarDistribuicaoCategorias() {
    const container = document.getElementById('categoryBreakdown');
    const despesasMes = state.despesas.filter(d => (d.mes || '').toLowerCase() === state.mesAtual);

    if (despesasMes.length === 0) {
        container.innerHTML = `<div class="empty-state-sm">Nenhuma despesa em <strong>${MESES_LABEL[state.mesAtual]}</strong> para exibir por categoria.</div>`;
        return;
    }

    const totaisPorCat = {};
    let somaTotal = 0;

    despesasMes.forEach(d => {
        const cat = d.categoria || 'Outros';
        const val = Number(d.valor || 0);
        totaisPorCat[cat] = (totaisPorCat[cat] || 0) + val;
        somaTotal += val;
    });

    const categoriasOrdenadas = Object.entries(totaisPorCat).sort((a, b) => b[1] - a[1]);

    container.innerHTML = categoriasOrdenadas.map(([cat, valor]) => {
        const pct = somaTotal > 0 ? Math.round((valor / somaTotal) * 100) : 0;
        return `
            <div class="cat-bar-item">
                <div class="cat-bar-top">
                    <span>${escapeHtml(cat)}</span>
                    <span>${formatCurrency(valor)} (${pct}%)</span>
                </div>
                <div class="progress-wrap" style="margin-bottom:0">
                    <div class="progress-bar" style="width: ${pct}%"></div>
                </div>
            </div>
        `;
    }).join('');
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
            <td data-label="Status">
                <span class="badge ${d.isPaga ? 'badge-paga' : 'badge-pendente'}">
                    ${d.isPaga ? '✓ Paga' : 'Pendente'}
                </span>
            </td>
            <td data-label="Descrição">
                <div class="td-desc-wrap">
                    <strong>${escapeHtml(d.descricao)}</strong>
                    <div class="income-meta">${MESES_LABEL[(d.mes || '').toLowerCase()] || d.mes}</div>
                </div>
            </td>
            <td data-label="Categoria">
                <span class="badge badge-cat">${escapeHtml(d.categoria || 'Outros')}</span>
            </td>
            <td data-label="Vencimento">${escapeHtml(d.dataVencimento || '-')}</td>
            <td data-label="Tipo">
                ${d.isFixa ? '<span class="badge badge-fixa">Fixa</span>' : '<span class="income-meta">Variável</span>'}
            </td>
            <td data-label="Valor" class="text-right"><strong>${formatCurrency(d.valor)}</strong></td>
            <td data-label="Ações" class="text-right td-actions">
                <div class="action-group">
                    ${d.isPaga
                        ? `<button class="btn btn-outline btn-sm" onclick="estornarDespesa(${d.id})" title="Estornar pagamento">Estornar</button>`
                        : `<button class="btn btn-success btn-sm" onclick="pagarDespesa(${d.id})" title="Dar baixa como paga">Pagar</button>`
                    }
                    <button class="btn btn-outline btn-sm" onclick="abrirEditarDespesa(${d.id})" title="Editar">Editar</button>
                    <button class="btn btn-danger-ghost btn-sm" onclick="excluirDespesa(${d.id})" title="Excluir">Excluir</button>
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
            <div class="income-top">
                <div class="income-info">
                    <h4>${escapeHtml(s.descricao)}</h4>
                    <span class="income-meta">${MESES_LABEL[(s.mes || '').toLowerCase()] || s.mes} / ${s.ano}</span>
                </div>
                <span class="income-amount">${formatCurrency(s.valor)}</span>
            </div>
            <div class="income-bottom">
                <button class="btn btn-outline btn-sm" onclick="abrirEditarSaldo(${s.id})" title="Editar receita">Editar</button>
                <button class="btn btn-danger-ghost btn-sm" onclick="excluirSaldo(${s.id})" title="Excluir receita">Excluir</button>
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
    if (!state.token) return;
    await Promise.all([
        carregarResumoMes(),
        carregarDespesas(),
        carregarSaldos()
    ]);
    if (state.abaAtiva === 'account') {
        document.getElementById('statTotalDespesas').textContent = String(state.totalDespesasCount || state.despesas.length);
        document.getElementById('statTotalSaldos').textContent = String(state.saldos.length);
    } else if (state.abaAtiva === 'familia') {
        await carregarModuloFamilia();
    }
}

// ==========================================
// 4. MÓDULO FAMÍLIA & MEMBROS (/familia)
// ==========================================
async function carregarModuloFamilia() {
    if (!state.token) return;
    try {
        const painel = await apiFetch('/familia');
        if (!painel) return;

        // 1. Convites Recebidos Pendentes para o usuário logado
        const boxConvites = document.getElementById('boxConvitesRecebidos');
        const listaConvites = document.getElementById('listaConvitesRecebidos');
        const pendentes = painel.convitesRecebidosPendentes || [];

        if (pendentes.length > 0) {
            boxConvites.classList.remove('hidden');
            listaConvites.innerHTML = pendentes.map(c => `
                <div class="income-item">
                    <div class="income-top">
                        <div class="income-info">
                            <h4>Família de ${escapeHtml(c.nome)}</h4>
                            <span class="income-meta">${escapeHtml(c.email)} &bull; Válido até ${escapeHtml(c.dataExpiracao || '')}</span>
                        </div>
                        <button class="btn btn-primary btn-sm" onclick="aceitarConviteDireto('${escapeHtml(c.tokenConvite)}')">
                            Aceitar Convite
                        </button>
                    </div>
                </div>
            `).join('');
        } else {
            boxConvites.classList.add('hidden');
        }

        // 2. Toggle de compartilhamento da própria renda
        const chkRenda = document.getElementById('chkCompartilharMinhaRenda');
        chkRenda.checked = Boolean(painel.minhaRendaCompartilhada);
        chkRenda.disabled = !painel.possuiFamiliaAtiva && (painel.membros || []).length === 0;

        // 3. Bloco de convidar (apenas titular pode convidar novos membros)
        const blocoConvidar = document.getElementById('blocoConvidarMembro');
        blocoConvidar.classList.toggle('hidden', !painel.souTitular);

        // 4. Lista de Membros
        const containerMembros = document.getElementById('listaMembrosFamilia');
        const membros = painel.membros || [];

        if (membros.length === 0) {
            containerMembros.innerHTML = `
                <div class="empty-state">
                    Nenhum membro na família ainda. Envie um convite ao lado para compartilhar a visão familiar.
                </div>
            `;
        } else {
            containerMembros.innerHTML = membros.map(m => {
                const isAceito = m.status === 'ACEITO';
                const linkConvite = m.tokenConvite
                    ? `${window.location.origin}/?convite=${m.tokenConvite}`
                    : null;

                return `
                    <div class="income-item">
                        <div class="income-top">
                            <div class="income-info">
                                <h4>${escapeHtml(m.nome)}</h4>
                                <span class="income-meta">${escapeHtml(m.email)}</span>
                                ${m.tokenConvite ? `<div class="token-pill">Código: ${escapeHtml(m.tokenConvite)}</div>` : ''}
                            </div>
                            <span class="badge ${isAceito ? 'badge-paga' : 'badge-pendente'}">
                                ${isAceito ? '✓ Membro Ativo' : 'Convite Pendente'}
                            </span>
                        </div>
                        <div class="income-bottom" style="justify-content: space-between;">
                            <span class="income-meta">
                                ${isAceito
                                    ? (m.compartilhaRenda ? '✓ Compartilhando renda' : 'Renda oculta na soma')
                                    : `Expira em: ${escapeHtml(m.dataExpiracao || '48h')}`
                                }
                            </span>
                            <div class="action-group">
                                ${linkConvite ? `<button class="btn btn-outline btn-sm" onclick="copiarLinkConvite('${escapeHtml(linkConvite)}', '${escapeHtml(m.tokenConvite)}')">Copiar Link</button>` : ''}
                                ${(painel.souTitular && !m.isTitular)
                                    ? `<button class="btn btn-danger-ghost btn-sm" onclick="removerMembroFamilia(${m.vinculoId})">${isAceito ? 'Remover Membro' : 'Cancelar'}</button>`
                                    : (!painel.souTitular && !m.isTitular && m.email.toLowerCase() === (state.user?.email || '').toLowerCase())
                                        ? `<button class="btn btn-danger-ghost btn-sm" onclick="removerMembroFamilia(${m.vinculoId})">Sair da Família</button>`
                                        : ''
                                }
                            </div>
                        </div>
                    </div>
                `;
            }).join('');
        }

        // 5. Se possui família ativa, carrega Resumo Consolidado + Despesas Familiares
        const painelConsolidado = document.getElementById('painelConsolidadoFamilia');
        if (painel.possuiFamiliaAtiva) {
            painelConsolidado.classList.remove('hidden');
            await Promise.all([
                carregarResumoFamiliaMes(),
                carregarDespesasFamiliaMes()
            ]);
        } else {
            painelConsolidado.classList.add('hidden');
        }
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function carregarResumoFamiliaMes() {
    try {
        const resumo = await apiFetch(`/familia/resumo/${state.mesAtual}`);
        if (!resumo) return;

        const receita = Number(resumo.receitaTotalFamiliar || 0);
        const pagas = Number(resumo.despesasPagasFamiliares || 0);
        const brutas = Number(resumo.despesasBrutasFamiliares || 0);
        const falta = Math.max(0, brutas - pagas);

        document.getElementById('kpiFamReceitaTotal').textContent = formatCurrency(receita);
        document.getElementById('kpiFamDespesasPagas').textContent = formatCurrency(pagas);
        document.getElementById('kpiFamFaltaPagar').textContent = `Falta pagar: ${formatCurrency(falta)}`;
        document.getElementById('kpiFamDespesasBrutas').textContent = formatCurrency(brutas);
        document.getElementById('kpiFamSaldoDisponivel').textContent = formatCurrency(resumo.saldoDisponivelFamiliar);

        const badge = document.getElementById('badgeFamStatus');
        badge.textContent = resumo.status;
        badge.className = `status-pill status-${resumo.status}`;

        const pct = brutas > 0 ? Math.min(100, Math.round((pagas / brutas) * 100)) : 0;
        document.getElementById('progressFamPagas').style.width = `${pct}%`;
        document.getElementById('kpiFamProgressoTexto').textContent = `${pct}% quitado pela família`;
        document.getElementById('badgeTotalMembrosFamilia').textContent = `${resumo.totalMembrosAtivos} Membros Ativos`;
    } catch (err) {
        console.error('Erro ao carregar resumo familiar:', err);
    }
}

async function carregarDespesasFamiliaMes() {
    const tbody = document.getElementById('tbodyDespesasFamilia');
    try {
        const lista = await apiFetch(`/familia/despesas?mes=${state.mesAtual}`);
        if (!Array.isArray(lista) || lista.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="empty-state">
                        Nenhuma despesa familiar registrada em <strong>${MESES_LABEL[state.mesAtual]}</strong>.
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = lista.map(d => `
            <tr>
                <td data-label="Status">
                    <span class="badge ${d.isPaga ? 'badge-paga' : 'badge-pendente'}">
                        ${d.isPaga ? '✓ Paga' : 'Pendente'}
                    </span>
                </td>
                <td data-label="Responsável">
                    <span class="badge badge-fixa">${escapeHtml(d.responsavelNome)}</span>
                </td>
                <td data-label="Descrição">
                    <strong>${escapeHtml(d.descricao)}</strong>
                </td>
                <td data-label="Categoria">
                    <span class="badge badge-cat">${escapeHtml(d.categoria || 'Outros')}</span>
                </td>
                <td data-label="Vencimento">${escapeHtml(d.dataVencimento || '-')}</td>
                <td data-label="Tipo">
                    ${d.isFixa ? '<span class="badge badge-fixa">Fixa</span>' : '<span class="income-meta">Variável</span>'}
                </td>
                <td data-label="Valor" class="text-right"><strong>${formatCurrency(d.valor)}</strong></td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="7" class="empty-state">${escapeHtml(err.message)}</td></tr>`;
    }
}

window.copiarLinkConvite = async (link, token) => {
    try {
        await navigator.clipboard.writeText(link);
        showToast('Link de convite copiado! Envie para o seu familiar.');
    } catch (_) {
        prompt('Copie o código ou link do convite:', token);
    }
};

window.aceitarConviteDireto = async (token) => {
    try {
        await apiFetch('/familia/convites/aceitar', {
            method: 'POST',
            body: JSON.stringify({ token })
        });
        showToast('Convite aceito! Você agora faz parte da família.');
        await carregarModuloFamilia();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

window.removerMembroFamilia = async (vinculoId) => {
    if (!confirm('Tem certeza que deseja encerrar este vínculo familiar? O acesso será cortado imediatamente.')) return;
    try {
        await apiFetch(`/familia/membros/${vinculoId}`, { method: 'DELETE' });
        showToast('Vínculo familiar removido e acesso encerrado imediatamente.');
        await carregarModuloFamilia();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

document.addEventListener('DOMContentLoaded', async () => {
    // Abas Login / Cadastro
    document.getElementById('tabLoginBtn').addEventListener('click', () => mostrarTelaAuth('login'));
    document.getElementById('tabRegisterBtn').addEventListener('click', () => mostrarTelaAuth('register'));
    document.getElementById('btnBackToLogin').addEventListener('click', () => mostrarTelaAuth('login'));

    // Navegação entre Visão Geral, Família e Minha Conta
    document.getElementById('navDashboardBtn').addEventListener('click', () => navegarPara('dashboard'));
    document.getElementById('navFamiliaBtn').addEventListener('click', () => navegarPara('familia'));
    document.getElementById('brandHomeLink').addEventListener('click', () => navegarPara('dashboard'));
    document.getElementById('btnVoltarPainel').addEventListener('click', () => navegarPara('dashboard'));
    document.getElementById('navAccountBtn').addEventListener('click', () => navegarPara('account'));
    document.getElementById('btnAbrirMinhaConta').addEventListener('click', () => navegarPara('account'));

    // Eventos do Módulo Família
    document.getElementById('formEnviarConviteFamilia').addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = document.getElementById('btnEnviarConviteFamilia');
        btn.disabled = true;
        try {
            const input = document.getElementById('inputEmailConvidado');
            await apiFetch('/familia/convites', {
                method: 'POST',
                body: JSON.stringify({ emailConvidado: input.value.trim() })
            });
            input.value = '';
            showToast('Convite gerado com validade de 48h! Você também pode copiar o link na lista.');
            await carregarModuloFamilia();
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            btn.disabled = false;
        }
    });

    document.getElementById('formAceitarConviteToken').addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = document.getElementById('btnAceitarConviteToken');
        btn.disabled = true;
        try {
            const input = document.getElementById('inputTokenConvite');
            let token = input.value.trim();
            if (token.includes('convite=')) {
                token = new URL(token).searchParams.get('convite') || token;
            }
            await apiFetch('/familia/convites/aceitar', {
                method: 'POST',
                body: JSON.stringify({ token })
            });
            input.value = '';
            showToast('Convite aceito com sucesso! Bem-vindo(a) à família.');
            await carregarModuloFamilia();
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            btn.disabled = false;
        }
    });

    document.getElementById('chkCompartilharMinhaRenda').addEventListener('change', async (e) => {
        const compartilhar = e.target.checked;
        try {
            await apiFetch('/familia/compartilhar-renda', {
                method: 'PATCH',
                body: JSON.stringify({ compartilhar })
            });
            showToast(compartilhar
                ? 'Sua renda agora está incluída na soma familiar.'
                : 'Sua renda foi removida da soma familiar.');
            await carregarModuloFamilia();
        } catch (err) {
            e.target.checked = !compartilhar;
            showToast(err.message, 'error');
        }
    });

    // Checklist em tempo real da senha no Cadastro
    const regSenha = document.getElementById('regSenha');
    const regConf = document.getElementById('regConfirmacaoSenha');
    regSenha.addEventListener('input', () => {
        atualizarChecklistSenha(regSenha.value, 'regPwdRules');
        atualizarDicaConfirmacao(regSenha.value, regConf.value, 'regMatchHint');
    });
    regConf.addEventListener('input', () => {
        atualizarDicaConfirmacao(regSenha.value, regConf.value, 'regMatchHint');
    });

    // Checklist em tempo real na alteração de senha (Minha Conta)
    const novaSenhaInput = document.getElementById('novaSenhaInput');
    const confNovaSenhaInput = document.getElementById('confirmacaoNovaSenhaInput');
    novaSenhaInput.addEventListener('input', () => {
        atualizarChecklistSenha(novaSenhaInput.value, 'perfilPwdRules');
        atualizarDicaConfirmacao(novaSenhaInput.value, confNovaSenhaInput.value, 'perfilMatchHint');
    });
    confNovaSenhaInput.addEventListener('input', () => {
        atualizarDicaConfirmacao(novaSenhaInput.value, confNovaSenhaInput.value, 'perfilMatchHint');
    });

    // Submit Login
    const formLogin = document.getElementById('formLogin');
    formLogin.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = document.getElementById('btnSubmitLogin');
        btn.disabled = true;
        btn.textContent = 'Autenticando...';

        const emailDigitado = document.getElementById('loginEmail').value.trim();

        try {
            const data = await apiFetch('/auth/login', {
                method: 'POST',
                body: JSON.stringify({
                    email: emailDigitado,
                    senha: document.getElementById('loginSenha').value
                })
            });
            salvarSessao(data);
            formLogin.reset();
            mostrarTelaApp();
            showToast(`Bem-vindo(a), ${data.nome}!`);
            await atualizarTudo();
        } catch (err) {
            if (err.message && err.message.startsWith('EMAIL_NAO_VERIFICADO:')) {
                const emailPendente = err.message.split(':')[1] || emailDigitado;
                showToast('Confirme seu e-mail com o código de 6 dígitos para entrar.', 'error');
                mostrarTelaAuth('verify', emailPendente);
            } else {
                showToast(err.message, 'error');
            }
        } finally {
            btn.disabled = false;
            btn.textContent = 'Acessar Plataforma';
        }
    });

    // Submit Cadastro
    const formRegister = document.getElementById('formRegister');
    formRegister.addEventListener('submit', async (e) => {
        e.preventDefault();
        const senha = regSenha.value;
        const confirmacao = regConf.value;

        if (!atualizarChecklistSenha(senha, 'regPwdRules')) {
            showToast('A senha precisa atender a todos os requisitos de segurança.', 'error');
            return;
        }
        if (senha !== confirmacao) {
            showToast('A confirmação de senha não coincide com a senha informada.', 'error');
            return;
        }

        const btn = document.getElementById('btnSubmitRegister');
        btn.disabled = true;
        btn.textContent = 'Enviando código...';

        try {
            const resp = await apiFetch('/auth/registrar', {
                method: 'POST',
                body: JSON.stringify({
                    nome: document.getElementById('regNome').value.trim(),
                    email: document.getElementById('regEmail').value.trim(),
                    senha,
                    confirmacaoSenha: confirmacao
                })
            });
            formRegister.reset();
            atualizarChecklistSenha('', 'regPwdRules');
            atualizarDicaConfirmacao('', '', 'regMatchHint');
            showToast(resp.mensagem);
            mostrarTelaAuth('verify', resp.email, resp.codigoDemonstracao);
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Cadastrar e Enviar Código';
        }
    });

    // Submit Verificação de E-mail
    const formVerifyEmail = document.getElementById('formVerifyEmail');
    formVerifyEmail.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = document.getElementById('btnSubmitVerify');
        btn.disabled = true;
        btn.textContent = 'Validando código...';

        try {
            const data = await apiFetch('/auth/verificar-email', {
                method: 'POST',
                body: JSON.stringify({
                    email: state.pendingVerifyEmail,
                    codigo: document.getElementById('inputCodigoVerificacao').value.trim()
                })
            });
            salvarSessao(data);
            formVerifyEmail.reset();
            mostrarTelaApp();
            showToast(`E-mail confirmado! Bem-vindo(a), ${data.nome}!`);
            await atualizarTudo();
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Confirmar Código e Entrar';
        }
    });

    // Reenviar Código
    document.getElementById('btnResendCode').addEventListener('click', async () => {
        if (!state.pendingVerifyEmail) return;
        try {
            const resp = await apiFetch('/auth/reenviar-codigo', {
                method: 'POST',
                body: JSON.stringify({ email: state.pendingVerifyEmail })
            });
            showToast(resp.mensagem);
            mostrarTelaAuth('verify', resp.email, resp.codigoDemonstracao);
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Upload e Redimensionamento de Foto de Perfil via Canvas (256x256)
    const inputFotoPerfil = document.getElementById('inputFotoPerfil');
    document.getElementById('btnEscolherFoto').addEventListener('click', () => {
        inputFotoPerfil.click();
    });

    inputFotoPerfil.addEventListener('change', (e) => {
        const file = e.target.files?.[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = (ev) => {
            const img = new Image();
            img.onload = async () => {
                const canvas = document.createElement('canvas');
                const size = 256;
                canvas.width = size;
                canvas.height = size;
                const ctx = canvas.getContext('2d');

                // Crop central quadrado
                const minSide = Math.min(img.width, img.height);
                const sx = (img.width - minSide) / 2;
                const sy = (img.height - minSide) / 2;

                ctx.drawImage(img, sx, sy, minSide, minSide, 0, 0, size, size);
                const dataUrl = canvas.toDataURL('image/jpeg', 0.85);

                try {
                    const authAtualizado = await apiFetch('/auth/perfil', {
                        method: 'PUT',
                        body: JSON.stringify({
                            nome: state.user.nome,
                            email: state.user.email,
                            fotoPerfil: dataUrl
                        })
                    });
                    salvarSessao(authAtualizado);
                    atualizarDadosUsuarioNaTela();
                    showToast('Foto de perfil atualizada com sucesso!');
                } catch (err) {
                    showToast(err.message, 'error');
                } finally {
                    inputFotoPerfil.value = '';
                }
            };
            img.src = ev.target.result;
        };
        reader.readAsDataURL(file);
    });

    document.getElementById('btnRemoverFoto').addEventListener('click', async () => {
        try {
            const authAtualizado = await apiFetch('/auth/perfil', {
                method: 'PUT',
                body: JSON.stringify({
                    nome: state.user.nome,
                    email: state.user.email,
                    fotoPerfil: 'REMOVER'
                })
            });
            state.user.fotoPerfil = null;
            salvarSessao({ ...authAtualizado, fotoPerfil: null });
            atualizarDadosUsuarioNaTela();
            showToast('Foto de perfil removida.');
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Atualizar Dados na Área "Minha Conta"
    document.getElementById('formAtualizarPerfil').addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = document.getElementById('btnSalvarPerfil');
        btn.disabled = true;
        try {
            const authAtualizado = await apiFetch('/auth/perfil', {
                method: 'PUT',
                body: JSON.stringify({
                    nome: document.getElementById('perfilInputNome').value.trim(),
                    email: document.getElementById('perfilInputEmail').value.trim()
                })
            });
            salvarSessao(authAtualizado);
            atualizarDadosUsuarioNaTela();
            showToast('Dados da conta atualizados com sucesso!');
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            btn.disabled = false;
        }
    });

    // Alterar Senha na Área "Minha Conta"
    const formAlterarSenha = document.getElementById('formAlterarSenha');
    formAlterarSenha.addEventListener('submit', async (e) => {
        e.preventDefault();
        const novaSenha = novaSenhaInput.value;
        const confirmacaoNovaSenha = confNovaSenhaInput.value;

        if (!atualizarChecklistSenha(novaSenha, 'perfilPwdRules')) {
            showToast('A nova senha precisa atender a todas as regras de segurança.', 'error');
            return;
        }
        if (novaSenha !== confirmacaoNovaSenha) {
            showToast('A confirmação da nova senha não coincide.', 'error');
            return;
        }

        const btn = document.getElementById('btnAtualizarSenha');
        btn.disabled = true;
        try {
            await apiFetch('/auth/alterar-senha', {
                method: 'PUT',
                body: JSON.stringify({
                    senhaAtual: document.getElementById('senhaAtualInput').value,
                    novaSenha,
                    confirmacaoNovaSenha
                })
            });
            formAlterarSenha.reset();
            atualizarChecklistSenha('', 'perfilPwdRules');
            atualizarDicaConfirmacao('', '', 'perfilMatchHint');
            showToast('Sua senha foi alterada com sucesso!');
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            btn.disabled = false;
        }
    });

    // Excluir Minha Conta
    document.getElementById('btnExcluirMinhaConta').addEventListener('click', async () => {
        if (!confirm('Tem certeza absoluta que deseja excluir sua conta e todos os seus dados financeiros?')) return;
        try {
            await apiFetch('/auth/conta', { method: 'DELETE' });
            limparSessao();
            showToast('Sua conta foi excluída permanentemente.');
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Logout
    document.getElementById('btnLogout').addEventListener('click', () => {
        limparSessao();
        showToast('Você saiu da sua conta.');
    });

    // Seletor de Mês
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

    // Filtros de despesas
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

    // Modal Despesa
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

    // Modal Saldo
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

    // Verificar sessão existente
    const urlParams = new URLSearchParams(window.location.search);
    const tokenConviteUrl = urlParams.get('convite');
    if (tokenConviteUrl) {
        document.getElementById('inputTokenConvite').value = tokenConviteUrl;
    }

    if (state.token) {
        try {
            const perfil = await apiFetch('/auth/me');
            if (perfil) {
                state.user = perfil;
                localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(perfil));
                mostrarTelaApp();
                await atualizarTudo();
                if (tokenConviteUrl) {
                    navegarPara('familia');
                }
                return;
            }
        } catch (_) {
            limparSessao();
        }
    } else {
        mostrarTelaAuth('login');
    }
});
