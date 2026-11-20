const API_BASE = window.location.origin.includes('localhost:8080') || window.location.protocol.startsWith('http')
    ? ''
    : 'http://localhost:8080';

const STORAGE_TOKEN_KEY = 'ff_auth_token';
const STORAGE_USER_KEY = 'ff_auth_user';
const COOKIE_CONSENT_NAME = 'ff_cookie_consent';
const STORAGE_KEEP_LOGGED_KEY = 'ff_keep_logged';

function getCookie(name) {
    const match = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
    return match ? decodeURIComponent(match[1]) : null;
}

function setCookie(name, value, days = 180) {
    const expires = new Date(Date.now() + days * 864e5).toUTCString();
    const secure = window.location.protocol === 'https:' ? '; Secure' : '';
    document.cookie = `${name}=${encodeURIComponent(value)}; expires=${expires}; path=/; SameSite=Lax${secure}`;
}

function obterConsentimentoCookies() {
    return getCookie(COOKIE_CONSENT_NAME);
}

function carregarSessaoInicial() {
    const sessionToken = sessionStorage.getItem(STORAGE_TOKEN_KEY);
    const sessionUser = sessionStorage.getItem(STORAGE_USER_KEY);
    if (sessionToken) {
        return {
            token: sessionToken,
            user: JSON.parse(sessionUser || 'null')
        };
    }

    const consent = obterConsentimentoCookies();
    const manterConectado = localStorage.getItem(STORAGE_KEEP_LOGGED_KEY) === 'true';
    if (consent === 'all' && manterConectado) {
        return {
            token: localStorage.getItem(STORAGE_TOKEN_KEY) || null,
            user: JSON.parse(localStorage.getItem(STORAGE_USER_KEY) || 'null')
        };
    }

    // Remove resquícios antigos de localStorage para impedir login automático indesejado em novas abas
    localStorage.removeItem(STORAGE_TOKEN_KEY);
    localStorage.removeItem(STORAGE_USER_KEY);
    return { token: null, user: null };
}

const sessaoInicial = carregarSessaoInicial();

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

const ICON_SVG = {
    check: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="20 6 9 17 4 12"/></svg>',
    clock: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>',
    alert: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>'
};

const state = {
    token: sessaoInicial.token,
    user: sessaoInicial.user,
    abaAtiva: 'dashboard',
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
    resumo: null,
    despesasFamilia: [],
    filtroResponsavelFamilia: 'todos'
};

let ultimoElementoFocado = null;
let modalAtivoEl = null;
let resolverConfirmacao = null;

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

function renderizarStatusPill(el, status) {
    if (!el) return;
    const st = status || 'LIMITE';
    const icon = st === 'POSITIVO' ? ICON_SVG.check : (st === 'DEFICIT' ? ICON_SVG.alert : ICON_SVG.clock);
    el.className = `status-pill status-${st}`;
    el.innerHTML = `${icon}<span>${escapeHtml(st)}</span>`;
}

function renderizarBadgePagamento(isPaga) {
    if (isPaga) {
        return `<span class="badge badge-paga">${ICON_SVG.check}<span>Paga</span></span>`;
    }
    return `<span class="badge badge-pendente">${ICON_SVG.clock}<span>Pendente</span></span>`;
}

// ==========================================
// GERENCIAMENTO DE FOCO & MODAL ACESSÍVEL (WCAG 2.2 AA)
// ==========================================
function abrirModalAcessivel(modalBackdropEl, elementoFocoInicial) {
    ultimoElementoFocado = document.activeElement;
    modalAtivoEl = modalBackdropEl;
    modalBackdropEl.classList.remove('hidden');

    setTimeout(() => {
        if (elementoFocoInicial) {
            elementoFocoInicial.focus();
        } else {
            const focaveis = obterElementosFocaveis(modalBackdropEl);
            if (focaveis.length > 0) focaveis[0].focus();
        }
    }, 20);
}

function fecharModalAcessivel(modalBackdropEl) {
    if (!modalBackdropEl) return;
    modalBackdropEl.classList.add('hidden');
    if (modalAtivoEl === modalBackdropEl) {
        modalAtivoEl = null;
    }
    if (ultimoElementoFocado && typeof ultimoElementoFocado.focus === 'function') {
        ultimoElementoFocado.focus();
    }
}

function obterElementosFocaveis(container) {
    return Array.from(
        container.querySelectorAll('button:not([disabled]), input:not([disabled]):not([type="hidden"]), select:not([disabled]), [tabindex]:not([tabindex="-1"])')
    ).filter(el => !el.closest('.hidden'));
}

document.addEventListener('keydown', (e) => {
    if (!modalAtivoEl) return;

    if (e.key === 'Escape') {
        e.preventDefault();
        if (modalAtivoEl.id === 'modalConfirmacao' && resolverConfirmacao) {
            const resolve = resolverConfirmacao;
            resolverConfirmacao = null;
            fecharModalAcessivel(modalAtivoEl);
            resolve(false);
        } else {
            fecharModalAcessivel(modalAtivoEl);
        }
        return;
    }

    if (e.key === 'Tab') {
        const focaveis = obterElementosFocaveis(modalAtivoEl);
        if (focaveis.length === 0) return;
        const primeiro = focaveis[0];
        const ultimo = focaveis[focaveis.length - 1];

        if (e.shiftKey && document.activeElement === primeiro) {
            e.preventDefault();
            ultimo.focus();
        } else if (!e.shiftKey && document.activeElement === ultimo) {
            e.preventDefault();
            primeiro.focus();
        }
    }
});

function confirmarAcaoDestrutiva(titulo, mensagem, textoBotao = 'Confirmar Exclusão') {
    return new Promise((resolve) => {
        resolverConfirmacao = resolve;
        document.getElementById('modalConfirmacaoTitulo').textContent = titulo;
        document.getElementById('modalConfirmacaoTexto').textContent = mensagem;
        const btnConfirmar = document.getElementById('btnConfirmarAcaoDestrutiva');
        const btnCancelar = document.getElementById('btnCancelarConfirmacao');
        btnConfirmar.textContent = textoBotao;

        const modalEl = document.getElementById('modalConfirmacao');
        abrirModalAcessivel(modalEl, btnCancelar);
    });
}

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

function salvarSessao(authData, manterConectado = null) {
    state.token = authData.token;
    state.user = {
        id: authData.id,
        nome: authData.nome,
        email: authData.email,
        emailVerificado: true,
        fotoPerfil: authData.fotoPerfil !== undefined ? authData.fotoPerfil : null,
        telefone: authData.telefone !== undefined ? authData.telefone : (state.user?.telefone || ''),
        emailRecuperacao: authData.emailRecuperacao !== undefined ? authData.emailRecuperacao : (state.user?.emailRecuperacao || '')
    };

    sessionStorage.setItem(STORAGE_TOKEN_KEY, state.token);
    sessionStorage.setItem(STORAGE_USER_KEY, JSON.stringify(state.user));

    const persistir = manterConectado !== null
        ? manterConectado
        : (localStorage.getItem(STORAGE_KEEP_LOGGED_KEY) === 'true' && obterConsentimentoCookies() === 'all');

    if (persistir && obterConsentimentoCookies() === 'all') {
        localStorage.setItem(STORAGE_KEEP_LOGGED_KEY, 'true');
        localStorage.setItem(STORAGE_TOKEN_KEY, state.token);
        localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(state.user));
    } else {
        localStorage.removeItem(STORAGE_KEEP_LOGGED_KEY);
        localStorage.removeItem(STORAGE_TOKEN_KEY);
        localStorage.removeItem(STORAGE_USER_KEY);
    }
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

    document.getElementById('perfilNomeHeader').textContent = state.user.nome;
    document.getElementById('perfilEmailHeader').textContent = state.user.email;
    document.getElementById('perfilInputEmail').value = state.user.email;
    document.getElementById('perfilInputNome').value = state.user.nome;
    const telInput = document.getElementById('perfilInputTelefone');
    if (telInput) telInput.value = state.user.telefone || '';
    const emailRecInput = document.getElementById('perfilInputEmailRecuperacao');
    if (emailRecInput) emailRecInput.value = state.user.emailRecuperacao || '';
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

    [navDashboardBtn, navFamiliaBtn, navAccountBtn].forEach(btn => {
        if (!btn) return;
        btn.classList.remove('active');
        btn.removeAttribute('aria-current');
    });

    if (aba === 'account') {
        accountView.classList.remove('hidden');
        navAccountBtn.classList.add('active');
        navAccountBtn.setAttribute('aria-current', 'page');
        monthSelectorBar.classList.add('hidden');
        atualizarDadosUsuarioNaTela();
        document.getElementById('statTotalDespesas').textContent = String(state.totalDespesasCount || state.despesas.length);
        document.getElementById('statTotalSaldos').textContent = String(state.saldos.length);
        carregarModuloFamilia();
    } else if (aba === 'familia' && navFamiliaBtn && !navFamiliaBtn.classList.contains('hidden')) {
        if (familiaView) familiaView.classList.remove('hidden');
        navFamiliaBtn.classList.add('active');
        navFamiliaBtn.setAttribute('aria-current', 'page');
        monthSelectorBar.classList.remove('hidden');
        carregarModuloFamilia();
    } else {
        state.abaAtiva = 'dashboard';
        dashboardView.classList.remove('hidden');
        navDashboardBtn.classList.add('active');
        navDashboardBtn.setAttribute('aria-current', 'page');
        monthSelectorBar.classList.remove('hidden');
    }
}

function limparSessao() {
    state.token = null;
    state.user = null;
    state.despesasFamilia = [];
    state.filtroResponsavelFamilia = 'todos';
    const navFamiliaBtn = document.getElementById('navFamiliaBtn');
    if (navFamiliaBtn) navFamiliaBtn.classList.add('hidden');
    sessionStorage.removeItem(STORAGE_TOKEN_KEY);
    sessionStorage.removeItem(STORAGE_USER_KEY);
    localStorage.removeItem(STORAGE_TOKEN_KEY);
    localStorage.removeItem(STORAGE_USER_KEY);
    localStorage.removeItem(STORAGE_KEEP_LOGGED_KEY);
    mostrarTelaAuth('login');
}

function mostrarTelaAuth(modo = 'login', emailVerificacao = '') {
    document.getElementById('authScreen').classList.remove('hidden');
    document.getElementById('appScreen').classList.add('hidden');

    const tabsWrap = document.getElementById('authTabsWrap');
    const tabLoginBtn = document.getElementById('tabLoginBtn');
    const tabRegisterBtn = document.getElementById('tabRegisterBtn');
    const formLogin = document.getElementById('formLogin');
    const formRegister = document.getElementById('formRegister');
    const formVerifyEmail = document.getElementById('formVerifyEmail');
    const formEsqueciSenha = document.getElementById('formEsqueciSenha');
    const formRedefinirSenha = document.getElementById('formRedefinirSenha');

    formLogin.classList.add('hidden');
    formRegister.classList.add('hidden');
    formVerifyEmail.classList.add('hidden');
    if (formEsqueciSenha) formEsqueciSenha.classList.add('hidden');
    if (formRedefinirSenha) formRedefinirSenha.classList.add('hidden');

    if (modo === 'verify') {
        tabsWrap.classList.add('hidden');
        formVerifyEmail.classList.remove('hidden');
        state.pendingVerifyEmail = emailVerificacao;
        document.getElementById('verifyEmailTarget').textContent = emailVerificacao;
        document.getElementById('inputCodigoVerificacao').value = '';
    } else if (modo === 'forgot') {
        tabsWrap.classList.add('hidden');
        if (formEsqueciSenha) formEsqueciSenha.classList.remove('hidden');
    } else if (modo === 'reset') {
        tabsWrap.classList.add('hidden');
        if (formRedefinirSenha) formRedefinirSenha.classList.remove('hidden');
    } else if (modo === 'register') {
        tabsWrap.classList.remove('hidden');
        tabRegisterBtn.classList.add('active');
        tabRegisterBtn.setAttribute('aria-selected', 'true');
        tabLoginBtn.classList.remove('active');
        tabLoginBtn.setAttribute('aria-selected', 'false');
        formRegister.classList.remove('hidden');
    } else {
        tabsWrap.classList.remove('hidden');
        tabLoginBtn.classList.add('active');
        tabLoginBtn.setAttribute('aria-selected', 'true');
        tabRegisterBtn.classList.remove('active');
        tabRegisterBtn.setAttribute('aria-selected', 'false');
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

        const saldoPrevisto = Number(resumo.saldoDisponivel || 0);
        const caixaAtual = resumo.saldoCaixaAtual !== undefined
            ? Number(resumo.saldoCaixaAtual)
            : (receita - pagas);

        document.getElementById('kpiReceitaTotal').textContent = formatCurrency(receita);
        document.getElementById('kpiDespesasPagas').textContent = formatCurrency(pagas);
        document.getElementById('kpiFaltaPagar').textContent = `Pendente no mês: ${formatCurrency(faltaPagar)}`;
        document.getElementById('kpiDespesasBrutas').textContent = formatCurrency(brutas);

        const elSaldo = document.getElementById('kpiSaldoDisponivel');
        elSaldo.textContent = formatCurrency(saldoPrevisto);
        elSaldo.classList.remove('text-danger', 'text-healthy');
        if (saldoPrevisto < 0) elSaldo.classList.add('text-danger');
        else if (saldoPrevisto > 0) elSaldo.classList.add('text-healthy');

        const elSaldoSub = document.getElementById('kpiSaldoSubtexto');
        if (elSaldoSub) {
            elSaldoSub.textContent = `Em caixa hoje (sem baixas): ${formatCurrency(caixaAtual)}`;
        }

        renderizarStatusPill(document.getElementById('badgeStatusFinanceiro'), resumo.status);

        const pctQuitado = brutas > 0 ? Math.min(100, Math.round((pagas / brutas) * 100)) : 0;
        document.getElementById('progressPagas').style.width = `${pctQuitado}%`;
        document.getElementById('kpiProgressoTexto').textContent = `${pctQuitado}% das despesas do mês quitadas`;

        const pctComprometido = receita > 0 ? Math.min(100, Math.round((brutas / receita) * 100)) : (brutas > 0 ? 100 : 0);
        const lblComp = document.getElementById('lblComprometimento');
        const barComp = document.getElementById('barComprometimento');
        const txtRec = document.getElementById('txtSaudeRecomendacao');

        lblComp.textContent = `${pctComprometido}%`;
        barComp.style.width = `${pctComprometido}%`;
        barComp.classList.remove('progress-bar--warning', 'progress-bar--danger');
        lblComp.classList.remove('text-healthy', 'text-warning', 'text-danger');
        txtRec.classList.remove('text-healthy', 'text-warning', 'text-danger');

        if (receita === 0 && brutas === 0) {
            txtRec.textContent = 'Cadastre suas receitas e despesas do mês para acompanhar a saúde do orçamento.';
        } else if (pctComprometido <= 70) {
            lblComp.classList.add('text-healthy');
            txtRec.textContent = 'Mais de 30% da sua renda está livre para reserva ou investimentos.';
        } else if (pctComprometido <= 90) {
            barComp.classList.add('progress-bar--warning');
            lblComp.classList.add('text-warning');
            txtRec.classList.add('text-warning');
            txtRec.textContent = 'Atenção: Suas despesas previstas consomem boa parte da renda mensal.';
        } else {
            barComp.classList.add('progress-bar--danger');
            lblComp.classList.add('text-danger');
            txtRec.classList.add('text-danger');
            txtRec.textContent = 'Alerta crítico: As despesas previstas atingiram ou superaram a receita cadastrada no mês.';
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
            <tr><td colspan="7" class="empty-state">Não foi possível carregar as despesas (${escapeHtml(err.message)}).</td></tr>
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
                <div class="progress-wrap">
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
                ${renderizarBadgePagamento(d.isPaga)}
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
                        ? `<button type="button" class="btn btn-outline btn-sm" onclick="estornarDespesa(${d.id})" aria-label="Estornar pagamento de ${escapeHtml(d.descricao)}">Estornar</button>`
                        : `<button type="button" class="btn btn-success btn-sm" onclick="pagarDespesa(${d.id})" aria-label="Dar baixa em ${escapeHtml(d.descricao)}">Pagar</button>`
                    }
                    <button type="button" class="btn btn-outline btn-sm" onclick="abrirEditarDespesa(${d.id})" aria-label="Editar ${escapeHtml(d.descricao)}">Editar</button>
                    <button type="button" class="btn btn-danger-ghost btn-sm" onclick="excluirDespesa(${d.id})" aria-label="Excluir ${escapeHtml(d.descricao)}">Excluir</button>
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
            <div class="empty-state">Não foi possível carregar as receitas (${escapeHtml(err.message)}).</div>
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
                <button type="button" class="btn btn-outline btn-sm" onclick="abrirEditarSaldo(${s.id})" aria-label="Editar receita ${escapeHtml(s.descricao)}">Editar</button>
                <button type="button" class="btn btn-danger-ghost btn-sm" onclick="excluirSaldo(${s.id})" aria-label="Excluir receita ${escapeHtml(s.descricao)}">Excluir</button>
            </div>
        </div>
    `).join('');
}

// Ações de Despesa (Com Confirmação Acessível)
window.pagarDespesa = async (id) => {
    try {
        await apiFetch(`/despesas/${id}/pagar`, { method: 'PATCH' });
        showToast('Despesa marcada como paga.');
        await atualizarTudo();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

window.estornarDespesa = async (id) => {
    try {
        await apiFetch(`/despesas/${id}/estornar`, { method: 'PATCH' });
        showToast('Pagamento estornado.');
        await atualizarTudo();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

window.excluirDespesa = async (id) => {
    const d = state.despesas.find(item => item.id === id);
    const nome = d ? `"${d.descricao}"` : 'esta despesa';
    const confirmado = await confirmarAcaoDestrutiva(
        'Excluir Despesa',
        `Tem certeza que deseja excluir permanentemente ${nome}? Esta ação não pode ser desfeita.`,
        'Excluir Despesa'
    );
    if (!confirmado) return;

    try {
        await apiFetch(`/despesas/${id}`, { method: 'DELETE' });
        showToast('Despesa excluída.');
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

    abrirModalAcessivel(document.getElementById('modalDespesa'), document.getElementById('despesaDescricao'));
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

    abrirModalAcessivel(document.getElementById('modalSaldo'), document.getElementById('saldoDescricao'));
};

window.excluirSaldo = async (id) => {
    const s = state.saldos.find(item => item.id === id);
    const nome = s ? `"${s.descricao}"` : 'esta receita';
    const confirmado = await confirmarAcaoDestrutiva(
        'Excluir Receita',
        `Deseja realmente remover a fonte de renda ${nome}?`,
        'Excluir Receita'
    );
    if (!confirmado) return;

    try {
        await apiFetch(`/saldos/${id}`, { method: 'DELETE' });
        showToast('Receita removida.');
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
        carregarSaldos(),
        carregarModuloFamilia()
    ]);
    if (state.abaAtiva === 'account') {
        document.getElementById('statTotalDespesas').textContent = String(state.totalDespesasCount || state.despesas.length);
        document.getElementById('statTotalSaldos').textContent = String(state.saldos.length);
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

        const navFamiliaBtn = document.getElementById('navFamiliaBtn');
        if (navFamiliaBtn) {
            navFamiliaBtn.classList.toggle('hidden', !painel.possuiFamiliaAtiva);
        }
        if (!painel.possuiFamiliaAtiva && state.abaAtiva === 'familia') {
            navegarPara('dashboard');
        }

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
                        <button type="button" class="btn btn-primary btn-sm" onclick="aceitarConviteDireto('${escapeHtml(c.tokenConvite)}')">
                            Aceitar Convite
                        </button>
                    </div>
                </div>
            `).join('');
        } else {
            boxConvites.classList.add('hidden');
        }

        const chkRenda = document.getElementById('chkCompartilharMinhaRenda');
        chkRenda.checked = Boolean(painel.minhaRendaCompartilhada);
        chkRenda.disabled = !painel.possuiFamiliaAtiva && (painel.membros || []).length === 0;

        const chkDespesas = document.getElementById('chkCompartilharMinhasDespesas');
        if (chkDespesas) {
            chkDespesas.checked = painel.minhasDespesasCompartilhadas !== undefined
                ? Boolean(painel.minhasDespesasCompartilhadas)
                : true;
            chkDespesas.disabled = !painel.possuiFamiliaAtiva && (painel.membros || []).length === 0;
        }

        const blocoConvidar = document.getElementById('blocoConvidarMembro');
        blocoConvidar.classList.toggle('hidden', !painel.souTitular);

        const blocoCodigo = document.getElementById('blocoAceitarCodigoConvite');
        const membros = painel.membros || [];
        if (blocoCodigo) {
            blocoCodigo.classList.toggle('hidden', painel.possuiFamiliaAtiva || membros.length > 0);
        }

        const containerMembros = document.getElementById('listaMembrosFamilia');

        if (membros.length === 0) {
            containerMembros.innerHTML = `
                <div class="empty-state-sm">
                    Nenhum membro vinculado ainda. Utilize o campo ao lado para convidar um familiar.
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
                                ${isAceito ? `${ICON_SVG.check}<span>Membro Ativo</span>` : `${ICON_SVG.clock}<span>Pendente</span>`}
                            </span>
                        </div>
                        <div class="income-bottom" style="justify-content: space-between;">
                            <span class="income-meta">
                                ${isAceito
                                    ? `${m.compartilhaRenda ? '✓ Renda visível' : 'Renda oculta'} &bull; ${m.compartilhaDespesas !== false ? '✓ Despesas visíveis' : 'Despesas ocultas'}`
                                    : `Expira em: ${escapeHtml(m.dataExpiracao || '48h')}`
                                }
                            </span>
                            <div class="action-group">
                                ${linkConvite ? `<button type="button" class="btn btn-outline btn-sm" onclick="copiarLinkConvite('${escapeHtml(linkConvite)}', '${escapeHtml(m.tokenConvite)}')">Copiar Link</button>` : ''}
                                ${(painel.souTitular && !m.isTitular)
                                    ? `<button type="button" class="btn btn-danger-ghost btn-sm" onclick="removerMembroFamilia(${m.vinculoId}, '${escapeHtml(m.nome)}', true)">${isAceito ? 'Remover Membro' : 'Cancelar Convite'}</button>`
                                    : (!painel.souTitular && !m.isTitular && m.email.toLowerCase() === (state.user?.email || '').toLowerCase())
                                        ? `<button type="button" class="btn btn-danger-ghost btn-sm" onclick="removerMembroFamilia(${m.vinculoId}, '${escapeHtml(painel.nomeTitular)}', false)">Sair da Família</button>`
                                        : ''
                                }
                            </div>
                        </div>
                    </div>
                `;
            }).join('');
        }

        const painelConsolidado = document.getElementById('painelConsolidadoFamilia');
        const estadoSemFamilia = document.getElementById('estadoSemFamiliaAtiva');
        if (painel.possuiFamiliaAtiva) {
            painelConsolidado.classList.remove('hidden');
            if (estadoSemFamilia) estadoSemFamilia.classList.add('hidden');
            await Promise.all([
                carregarResumoFamiliaMes(),
                carregarDespesasFamiliaMes()
            ]);
        } else {
            painelConsolidado.classList.add('hidden');
            if (estadoSemFamilia) estadoSemFamilia.classList.remove('hidden');
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

        const saldoFamPrevisto = Number(resumo.saldoDisponivelFamiliar || 0);
        const caixaFamAtual = resumo.saldoCaixaAtualFamiliar !== undefined
            ? Number(resumo.saldoCaixaAtualFamiliar)
            : (receita - pagas);

        document.getElementById('kpiFamReceitaTotal').textContent = formatCurrency(receita);
        document.getElementById('kpiFamDespesasPagas').textContent = formatCurrency(pagas);
        document.getElementById('kpiFamFaltaPagar').textContent = `Pendente no mês: ${formatCurrency(falta)}`;
        document.getElementById('kpiFamDespesasBrutas').textContent = formatCurrency(brutas);

        const elFamSaldo = document.getElementById('kpiFamSaldoDisponivel');
        elFamSaldo.textContent = formatCurrency(saldoFamPrevisto);
        elFamSaldo.classList.remove('text-danger', 'text-healthy');
        if (saldoFamPrevisto < 0) elFamSaldo.classList.add('text-danger');
        else if (saldoFamPrevisto > 0) elFamSaldo.classList.add('text-healthy');

        const elFamSub = document.getElementById('kpiFamSaldoSubtexto');
        if (elFamSub) {
            elFamSub.textContent = `Em caixa hoje (sem baixas): ${formatCurrency(caixaFamAtual)}`;
        }

        renderizarStatusPill(document.getElementById('badgeFamStatus'), resumo.status);

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
        state.despesasFamilia = Array.isArray(lista) ? lista : [];

        // Se o responsável filtrado não existir mais na lista atual, volta para "todos"
        if (state.filtroResponsavelFamilia !== 'todos') {
            const existe = state.despesasFamilia.some(d => d.responsavelNome === state.filtroResponsavelFamilia);
            if (!existe) {
                state.filtroResponsavelFamilia = 'todos';
            }
        }

        renderizarTotaisEFiltroResponsavelFamilia();
        renderizarTabelaDespesasFamilia();
        renderizarDistribuicaoCategoriasFamilia();
    } catch (err) {
        state.despesasFamilia = [];
        tbody.innerHTML = `<tr><td colspan="7" class="empty-state">${escapeHtml(err.message)}</td></tr>`;
        renderizarTotaisEFiltroResponsavelFamilia();
        renderizarDistribuicaoCategoriasFamilia();
    }
}

function selecionarFiltroResponsavelFamilia(responsavel) {
    state.filtroResponsavelFamilia = responsavel;
    renderizarTotaisEFiltroResponsavelFamilia();
    renderizarTabelaDespesasFamilia();
    renderizarDistribuicaoCategoriasFamilia();
}

function renderizarTotaisEFiltroResponsavelFamilia() {
    const totaisEl = document.getElementById('totaisPorMembroFamilia');
    const barraFiltro = document.getElementById('barraFiltroResponsavelFamilia');
    const tabsEl = document.getElementById('filtroResponsavelFamilia');
    const resumoFiltradoEl = document.getElementById('resumoTotalFiltradoFamilia');
    if (!totaisEl || !barraFiltro || !tabsEl) return;

    const lista = state.despesasFamilia || [];
    if (lista.length === 0) {
        totaisEl.classList.add('hidden');
        totaisEl.innerHTML = '';
        barraFiltro.classList.add('hidden');
        tabsEl.innerHTML = '';
        if (resumoFiltradoEl) resumoFiltradoEl.textContent = '';
        return;
    }

    const porMembro = {};
    let totalGeral = 0;

    lista.forEach(d => {
        const nome = d.responsavelNome || 'Família';
        const valor = Number(d.valor || 0);
        if (!porMembro[nome]) {
            porMembro[nome] = { total: 0, pagas: 0, qtd: 0 };
        }
        porMembro[nome].total += valor;
        if (d.isPaga) porMembro[nome].pagas += valor;
        porMembro[nome].qtd += 1;
        totalGeral += valor;
    });

    const membrosOrdenados = Object.entries(porMembro).sort((a, b) => b[1].total - a[1].total);

    totaisEl.classList.remove('hidden');
    totaisEl.innerHTML = membrosOrdenados.map(([nome, dados]) => {
        const pct = totalGeral > 0 ? Math.round((dados.total / totalGeral) * 100) : 0;
        const pendente = Math.max(0, dados.total - dados.pagas);
        return `
            <div class="family-member-total-card">
                <div class="family-member-total-top">
                    <span>${escapeHtml(nome)}</span>
                    <span>${pct}%</span>
                </div>
                <div class="family-member-total-val">${formatCurrency(dados.total)}</div>
                <div class="family-member-total-meta">
                    ${dados.qtd} ${dados.qtd === 1 ? 'despesa' : 'despesas'} &bull; Pendente: ${formatCurrency(pendente)}
                </div>
            </div>
        `;
    }).join('');

    barraFiltro.classList.remove('hidden');
    const botoes = [
        `<button type="button" class="filter-btn ${state.filtroResponsavelFamilia === 'todos' ? 'active' : ''}" data-resp="todos">Todos (${lista.length})</button>`,
        ...membrosOrdenados.map(([nome, dados]) =>
            `<button type="button" class="filter-btn ${state.filtroResponsavelFamilia === nome ? 'active' : ''}" data-resp="${escapeHtml(nome)}">${escapeHtml(nome)} (${dados.qtd})</button>`
        )
    ];
    tabsEl.innerHTML = botoes.join('');

    tabsEl.querySelectorAll('.filter-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            selecionarFiltroResponsavelFamilia(btn.dataset.resp || 'todos');
        });
    });

    const filtradas = state.filtroResponsavelFamilia === 'todos'
        ? lista
        : lista.filter(d => (d.responsavelNome || 'Família') === state.filtroResponsavelFamilia);
    const somaFiltrada = filtradas.reduce((acc, d) => acc + Number(d.valor || 0), 0);

    if (resumoFiltradoEl) {
        resumoFiltradoEl.textContent = `Total exibido: ${formatCurrency(somaFiltrada)}`;
    }
}

function renderizarTabelaDespesasFamilia() {
    const tbody = document.getElementById('tbodyDespesasFamilia');
    const lista = state.despesasFamilia || [];
    const filtradas = state.filtroResponsavelFamilia === 'todos'
        ? lista
        : lista.filter(d => (d.responsavelNome || 'Família') === state.filtroResponsavelFamilia);

    if (filtradas.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="empty-state">
                    Nenhuma despesa familiar compartilhada em <strong>${MESES_LABEL[state.mesAtual]}</strong>.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = filtradas.map(d => `
        <tr>
            <td data-label="Status">
                ${renderizarBadgePagamento(d.isPaga)}
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
}

function renderizarDistribuicaoCategoriasFamilia() {
    const container = document.getElementById('famCategoryBreakdown');
    if (!container) return;

    const lista = state.despesasFamilia || [];
    const filtradas = state.filtroResponsavelFamilia === 'todos'
        ? lista
        : lista.filter(d => (d.responsavelNome || 'Família') === state.filtroResponsavelFamilia);

    if (filtradas.length === 0) {
        container.innerHTML = `<div class="empty-state-sm">Nenhuma despesa familiar compartilhada em <strong>${MESES_LABEL[state.mesAtual]}</strong> para exibir por categoria.</div>`;
        return;
    }

    const totaisPorCat = {};
    let somaTotal = 0;

    filtradas.forEach(d => {
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
                <div class="progress-wrap">
                    <div class="progress-bar" style="width: ${pct}%"></div>
                </div>
            </div>
        `;
    }).join('');
}

window.copiarLinkConvite = async (link, token) => {
    try {
        await navigator.clipboard.writeText(link);
        showToast('Link de convite copiado para a área de transferência.');
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

window.removerMembroFamilia = async (vinculoId, nomeAlvo, souTitular) => {
    const titulo = souTitular ? 'Remover Membro da Família' : 'Sair da Família';
    const msg = souTitular
        ? `Tem certeza que deseja encerrar o acesso de "${nomeAlvo}" à visão familiar? O acesso será cortado imediatamente.`
        : `Tem certeza que deseja sair da família de "${nomeAlvo}"? Você perderá acesso à visão consolidada imediatamente.`;
    const botao = souTitular ? 'Remover Membro' : 'Sair da Família';

    const confirmado = await confirmarAcaoDestrutiva(titulo, msg, botao);
    if (!confirmado) return;

    try {
        await apiFetch(`/familia/membros/${vinculoId}`, { method: 'DELETE' });
        showToast('Vínculo familiar encerrado imediatamente.');
        await carregarModuloFamilia();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

document.addEventListener('DOMContentLoaded', async () => {
    // Modal de Confirmação Destrutiva
    const modalConfirmacao = document.getElementById('modalConfirmacao');
    const encerrarConfirmacao = (resultado) => {
        const resolve = resolverConfirmacao;
        resolverConfirmacao = null;
        fecharModalAcessivel(modalConfirmacao);
        if (resolve) resolve(resultado);
    };
    document.getElementById('btnConfirmarAcaoDestrutiva').addEventListener('click', () => encerrarConfirmacao(true));
    document.getElementById('btnCancelarConfirmacao').addEventListener('click', () => encerrarConfirmacao(false));
    document.getElementById('btnFecharConfirmacao').addEventListener('click', () => encerrarConfirmacao(false));

    // Abas Login / Cadastro
    document.getElementById('tabLoginBtn').addEventListener('click', () => mostrarTelaAuth('login'));
    document.getElementById('tabRegisterBtn').addEventListener('click', () => mostrarTelaAuth('register'));
    document.getElementById('btnBackToLogin').addEventListener('click', () => mostrarTelaAuth('login'));

    // Navegação entre Painel Financeiro, Família e Minha Conta
    document.getElementById('navDashboardBtn').addEventListener('click', () => navegarPara('dashboard'));
    document.getElementById('navFamiliaBtn').addEventListener('click', () => navegarPara('familia'));
    document.getElementById('brandHomeLink').addEventListener('click', () => navegarPara('dashboard'));
    document.getElementById('btnVoltarPainel').addEventListener('click', () => navegarPara('dashboard'));
    document.getElementById('navAccountBtn').addEventListener('click', () => navegarPara('account'));
    document.getElementById('btnAbrirMinhaConta').addEventListener('click', () => navegarPara('account'));

    const irParaConfigFamilia = () => {
        navegarPara('account');
        setTimeout(() => {
            document.getElementById('secaoConfigFamilia')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }, 100);
    };
    document.getElementById('btnIrConfigurarFamilia')?.addEventListener('click', irParaConfigFamilia);
    document.getElementById('btnAtalhoConfigFamilia')?.addEventListener('click', irParaConfigFamilia);

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
            showToast('Convite gerado com validade de 48h.');
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

    document.getElementById('chkCompartilharMinhasDespesas')?.addEventListener('change', async (e) => {
        const compartilhar = e.target.checked;
        try {
            await apiFetch('/familia/compartilhar-despesas', {
                method: 'PATCH',
                body: JSON.stringify({ compartilhar })
            });
            showToast(compartilhar
                ? 'Suas despesas agora aparecem na visão consolidada da família.'
                : 'Suas despesas foram ocultadas da visão consolidada e dos totais da família.');
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
        const manterConectado = Boolean(document.getElementById('loginManterConectado')?.checked);

        try {
            const data = await apiFetch('/auth/login', {
                method: 'POST',
                body: JSON.stringify({
                    email: emailDigitado,
                    senha: document.getElementById('loginSenha').value
                })
            });
            if (manterConectado && obterConsentimentoCookies() !== 'all') {
                setCookie(COOKIE_CONSENT_NAME, 'all');
                const banner = document.getElementById('cookieBanner');
                if (banner) banner.classList.add('hidden');
            }
            salvarSessao(data, manterConectado);
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
            btn.textContent = 'Entrar no Sistema';
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
            mostrarTelaAuth('verify', resp.email);
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Criar Conta e Receber Código';
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
            btn.textContent = 'Validar Código e Acessar';
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
            mostrarTelaAuth('verify', resp.email);
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
                    showToast('Foto de perfil atualizada.');
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
        const confirmado = await confirmarAcaoDestrutiva(
            'Remover Foto de Perfil',
            'Deseja remover sua foto de perfil atual e voltar para o avatar com suas iniciais?',
            'Remover Foto'
        );
        if (!confirmado) return;

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
                    email: document.getElementById('perfilInputEmail').value.trim(),
                    telefone: document.getElementById('perfilInputTelefone')?.value.trim() || '',
                    emailRecuperacao: document.getElementById('perfilInputEmailRecuperacao')?.value.trim() || ''
                })
            });
            const perfilAtualizado = await apiFetch('/auth/me');
            salvarSessao({ ...authAtualizado, ...perfilAtualizado });
            atualizarDadosUsuarioNaTela();
            showToast('Dados da conta atualizados.');
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
            showToast('Sua senha foi alterada com sucesso.');
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            btn.disabled = false;
        }
    });

    // Excluir Minha Conta (Com Confirmação Acessível)
    document.getElementById('btnExcluirMinhaConta').addEventListener('click', async () => {
        const confirmado = await confirmarAcaoDestrutiva(
            'Excluir Minha Conta Permanentemente',
            'Tem certeza absoluta que deseja excluir sua conta e todos os seus saldos e despesas registrados? Esta operação é irreversível.',
            'Sim, Excluir Minha Conta'
        );
        if (!confirmado) return;

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
        showToast('Sessão encerrada.');
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

    // Filtros de despesas com aria-selected
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.tab-btn').forEach(b => {
                b.classList.remove('active');
                b.setAttribute('aria-selected', 'false');
            });
            btn.classList.add('active');
            btn.setAttribute('aria-selected', 'true');
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

    // Modal Despesa (Foco Acessível)
    const modalDespesa = document.getElementById('modalDespesa');
    document.getElementById('btnNovaDespesa').addEventListener('click', () => {
        document.getElementById('formDespesa').reset();
        document.getElementById('despesaId').value = '';
        document.getElementById('modalDespesaTitulo').textContent = 'Nova Despesa';
        document.getElementById('despesaMes').value = state.mesAtual;
        document.getElementById('despesaDataVencimento').value = new Date().toISOString().split('T')[0];
        abrirModalAcessivel(modalDespesa, document.getElementById('despesaDescricao'));
    });

    const fecharModalDespesa = () => fecharModalAcessivel(modalDespesa);
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
                showToast('Despesa atualizada.');
            } else {
                await apiFetch('/despesas', {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });
                showToast('Despesa cadastrada.');
            }
            fecharModalDespesa();
            await atualizarTudo();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Modal Saldo (Foco Acessível)
    const modalSaldo = document.getElementById('modalSaldo');
    document.getElementById('btnNovoSaldo').addEventListener('click', () => {
        document.getElementById('formSaldo').reset();
        document.getElementById('saldoId').value = '';
        document.getElementById('modalSaldoTitulo').textContent = 'Nova Receita';
        document.getElementById('saldoMes').value = state.mesAtual;
        document.getElementById('saldoAno').value = new Date().getFullYear();
        abrirModalAcessivel(modalSaldo, document.getElementById('saldoDescricao'));
    });

    const fecharModalSaldo = () => fecharModalAcessivel(modalSaldo);
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
                showToast('Receita atualizada.');
            } else {
                await apiFetch('/saldos', {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });
                showToast('Receita cadastrada.');
            }
            fecharModalSaldo();
            await atualizarTudo();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // ==========================================
    // RECUPERAÇÃO DE SENHA (ESQUECI MINHA SENHA)
    // ==========================================
    const recCanal = document.getElementById('recCanal');
    const recIdentificador = document.getElementById('recIdentificador');
    const recIdentificadorLabel = document.getElementById('recIdentificadorLabel');

    if (recCanal) {
        recCanal.addEventListener('change', () => {
            const val = recCanal.value;
            if (val === 'TELEFONE') {
                recIdentificadorLabel.textContent = 'Telefone cadastrado';
                recIdentificador.type = 'tel';
                recIdentificador.placeholder = '(11) 99999-9999';
            } else if (val === 'EMAIL_RECUPERACAO') {
                recIdentificadorLabel.textContent = 'E-mail de recuperação';
                recIdentificador.type = 'email';
                recIdentificador.placeholder = 'secundario@dominio.com';
            } else {
                recIdentificadorLabel.textContent = 'E-mail principal';
                recIdentificador.type = 'email';
                recIdentificador.placeholder = 'nome@dominio.com';
            }
            recIdentificador.value = '';
        });
    }

    document.getElementById('btnEsqueciSenha')?.addEventListener('click', () => {
        const emailLogin = document.getElementById('loginEmail')?.value.trim();
        if (emailLogin && recCanal && recCanal.value === 'EMAIL') {
            recIdentificador.value = emailLogin;
        }
        mostrarTelaAuth('forgot');
    });
    document.getElementById('btnVoltarLoginEsqueci')?.addEventListener('click', () => mostrarTelaAuth('login'));
    document.getElementById('btnTenhoTokenReset')?.addEventListener('click', () => mostrarTelaAuth('reset'));
    document.getElementById('btnVoltarLoginReset')?.addEventListener('click', () => mostrarTelaAuth('login'));

    document.getElementById('formEsqueciSenha')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = document.getElementById('btnEnviarRecuperacao');
        btn.disabled = true;
        btn.textContent = 'Enviando...';
        try {
            const resp = await apiFetch('/auth/esqueci-senha', {
                method: 'POST',
                body: JSON.stringify({
                    canal: recCanal.value,
                    identificador: recIdentificador.value.trim()
                })
            });
            showToast(resp.mensagem);
            mostrarTelaAuth('reset');
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Enviar Código / Link';
        }
    });

    const resetNovaSenha = document.getElementById('resetNovaSenha');
    const resetConfSenha = document.getElementById('resetConfirmacaoSenha');
    if (resetNovaSenha && resetConfSenha) {
        resetNovaSenha.addEventListener('input', () => {
            atualizarChecklistSenha(resetNovaSenha.value, 'resetPwdRules');
            atualizarDicaConfirmacao(resetNovaSenha.value, resetConfSenha.value, 'resetMatchHint');
        });
        resetConfSenha.addEventListener('input', () => {
            atualizarDicaConfirmacao(resetNovaSenha.value, resetConfSenha.value, 'resetMatchHint');
        });
    }

    document.getElementById('formRedefinirSenha')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const novaSenha = resetNovaSenha.value;
        const confirmacaoNovaSenha = resetConfSenha.value;
        if (!atualizarChecklistSenha(novaSenha, 'resetPwdRules')) {
            showToast('A nova senha precisa atender a todas as regras de segurança.', 'error');
            return;
        }
        if (novaSenha !== confirmacaoNovaSenha) {
            showToast('A confirmação da nova senha não coincide.', 'error');
            return;
        }
        const btn = document.getElementById('btnConfirmarResetSenha');
        btn.disabled = true;
        btn.textContent = 'Redefinindo...';
        try {
            const resp = await apiFetch('/auth/redefinir-senha', {
                method: 'POST',
                body: JSON.stringify({
                    token: document.getElementById('resetTokenInput').value.trim(),
                    novaSenha,
                    confirmacaoNovaSenha
                })
            });
            document.getElementById('formRedefinirSenha').reset();
            showToast(resp.mensagem || 'Senha redefinida com sucesso!');
            mostrarTelaAuth('login');
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Redefinir Senha';
        }
    });

    // ==========================================
    // CONSENTIMENTO & PREFERÊNCIAS DE COOKIES
    // ==========================================
    const cookieBanner = document.getElementById('cookieBanner');
    const modalCookies = document.getElementById('modalCookies');
    const chkCookiePreferencia = document.getElementById('chkCookiePreferencia');

    const aplicarPreferenciaCookies = (tipo) => {
        setCookie(COOKIE_CONSENT_NAME, tipo);
        if (tipo !== 'all') {
            localStorage.removeItem(STORAGE_KEEP_LOGGED_KEY);
            localStorage.removeItem(STORAGE_TOKEN_KEY);
            localStorage.removeItem(STORAGE_USER_KEY);
        }
        if (cookieBanner) cookieBanner.classList.add('hidden');
        fecharModalAcessivel(modalCookies);
        showToast(tipo === 'all'
            ? 'Preferências salvas: todos os cookies habilitados.'
            : 'Preferências salvas: apenas cookies essenciais ativos.');
    };

    if (!obterConsentimentoCookies() && cookieBanner) {
        cookieBanner.classList.remove('hidden');
    }

    const abrirModalCookies = () => {
        if (chkCookiePreferencia) {
            chkCookiePreferencia.checked = obterConsentimentoCookies() === 'all';
        }
        abrirModalAcessivel(modalCookies, chkCookiePreferencia);
    };

    document.getElementById('btnAbrirCookiesAuth')?.addEventListener('click', abrirModalCookies);
    document.getElementById('btnAbrirCookiesConta')?.addEventListener('click', abrirModalCookies);
    document.getElementById('btnCookiePersonalizar')?.addEventListener('click', abrirModalCookies);
    document.getElementById('btnFecharCookies')?.addEventListener('click', () => fecharModalAcessivel(modalCookies));
    document.getElementById('btnCookieEssenciais')?.addEventListener('click', () => aplicarPreferenciaCookies('essential'));
    document.getElementById('btnCookieAceitarTodos')?.addEventListener('click', () => aplicarPreferenciaCookies('all'));
    document.getElementById('btnSalvarCookiesEssenciais')?.addEventListener('click', () => aplicarPreferenciaCookies('essential'));
    document.getElementById('btnSalvarCookiesTodos')?.addEventListener('click', () => {
        aplicarPreferenciaCookies(chkCookiePreferencia?.checked ? 'all' : 'essential');
    });

    // Verificar sessão existente
    const urlParams = new URLSearchParams(window.location.search);
    const tokenConviteUrl = urlParams.get('convite');
    const resetTokenUrl = urlParams.get('resetToken');
    if (tokenConviteUrl) {
        document.getElementById('inputTokenConvite').value = tokenConviteUrl;
    }

    if (resetTokenUrl) {
        document.getElementById('resetTokenInput').value = resetTokenUrl;
        window.history.replaceState({}, document.title, window.location.pathname);
        mostrarTelaAuth('reset');
        return;
    }

    if (state.token) {
        try {
            const perfil = await apiFetch('/auth/me');
            if (perfil) {
                state.user = perfil;
                sessionStorage.setItem(STORAGE_USER_KEY, JSON.stringify(perfil));
                if (localStorage.getItem(STORAGE_KEEP_LOGGED_KEY) === 'true' && obterConsentimentoCookies() === 'all') {
                    localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(perfil));
                }
                mostrarTelaApp();
                await atualizarTudo();
                if (tokenConviteUrl) {
                    navegarPara('account');
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
