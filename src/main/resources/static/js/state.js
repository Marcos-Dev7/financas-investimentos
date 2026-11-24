export const API_BASE = window.location.origin.includes('localhost:8080') || window.location.protocol.startsWith('http')
    ? ''
    : 'http://localhost:8080';

export const STORAGE_TOKEN_KEY = 'ff_auth_token';
export const STORAGE_USER_KEY = 'ff_auth_user';
export const STORAGE_KEEP_LOGGED_KEY = 'ff_keep_logged';
export const STORAGE_THEME_KEY = 'ff_theme_pref';
export const COOKIE_CONSENT_NAME = 'ff_cookie_consent';

export const MESES = [
    'janeiro', 'fevereiro', 'marco', 'abril', 'maio', 'junho',
    'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'
];

export const MESES_LABEL = {
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

export function getCookie(name) {
    const match = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
    return match ? decodeURIComponent(match[1]) : null;
}

export function setCookie(name, value, days = 180) {
    const expires = new Date(Date.now() + days * 864e5).toUTCString();
    const secure = window.location.protocol === 'https:' ? '; Secure' : '';
    document.cookie = `${name}=${encodeURIComponent(value)}; expires=${expires}; path=/; SameSite=Lax${secure}`;
}

export function obterConsentimentoCookies() {
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

    localStorage.removeItem(STORAGE_TOKEN_KEY);
    localStorage.removeItem(STORAGE_USER_KEY);
    return { token: null, user: null };
}

const sessaoInicial = carregarSessaoInicial();

export const state = {
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
    filtroResponsavelFamilia: 'todos',
    souTitularFamilia: false,
    pending2FaToken: null,
    pending2FaTipo: null,
    pending2FaManterConectado: false
};

export function salvarSessao(authData, manterConectado = null) {
    state.token = authData.token;
    state.user = {
        id: authData.id,
        nome: authData.nome,
        email: authData.email,
        emailVerificado: true,
        fotoPerfil: authData.fotoPerfil !== undefined ? authData.fotoPerfil : null,
        telefone: authData.telefone !== undefined ? authData.telefone : (state.user?.telefone || ''),
        emailRecuperacao: authData.emailRecuperacao !== undefined ? authData.emailRecuperacao : (state.user?.emailRecuperacao || ''),
        doisFatoresAtivo: authData.doisFatoresAtivo !== undefined ? Boolean(authData.doisFatoresAtivo) : Boolean(state.user?.doisFatoresAtivo),
        tipoDoisFatores: authData.tipoDoisFatores !== undefined ? authData.tipoDoisFatores : (state.user?.tipoDoisFatores || null)
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

export function sincronizarUsuarioEmStorage(perfil) {
    state.user = perfil;
    sessionStorage.setItem(STORAGE_USER_KEY, JSON.stringify(perfil));
    if (localStorage.getItem(STORAGE_KEEP_LOGGED_KEY) === 'true' && obterConsentimentoCookies() === 'all') {
        localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(perfil));
    }
}

export function resetarEstadoSessao() {
    state.token = null;
    state.user = null;
    state.despesasFamilia = [];
    state.filtroResponsavelFamilia = 'todos';
    state.souTitularFamilia = false;
    state.pending2FaToken = null;
    state.pending2FaTipo = null;
    state.pending2FaManterConectado = false;

    sessionStorage.removeItem(STORAGE_TOKEN_KEY);
    sessionStorage.removeItem(STORAGE_USER_KEY);
    localStorage.removeItem(STORAGE_TOKEN_KEY);
    localStorage.removeItem(STORAGE_USER_KEY);
    localStorage.removeItem(STORAGE_KEEP_LOGGED_KEY);
}

const SUN_SVG = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>';
const MOON_SVG = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>';

export function aplicarTema(tema) {
    const modo = tema === 'light' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', modo);

    document.querySelectorAll('[data-theme-toggle]').forEach(btn => {
        const iconEl = btn.querySelector('.theme-icon');
        const labelEl = btn.querySelector('.theme-label');
        if (iconEl) iconEl.innerHTML = modo === 'dark' ? SUN_SVG : MOON_SVG;
        if (labelEl) labelEl.textContent = modo === 'dark' ? 'Claro' : 'Escuro';
        btn.setAttribute('aria-label', modo === 'dark' ? 'Mudar para tema claro' : 'Mudar para tema escuro');
    });
}

export function inicializarTema() {
    const salvo = sessionStorage.getItem(STORAGE_THEME_KEY)
        || (obterConsentimentoCookies() === 'all' ? localStorage.getItem(STORAGE_THEME_KEY) : null);
    if (salvo === 'light' || salvo === 'dark') {
        aplicarTema(salvo);
    } else {
        aplicarTema('dark');
    }
}

export function alternarTema() {
    const atual = document.documentElement.getAttribute('data-theme') === 'light' ? 'light' : 'dark';
    const proximo = atual === 'dark' ? 'light' : 'dark';
    aplicarTema(proximo);
    sessionStorage.setItem(STORAGE_THEME_KEY, proximo);
    if (obterConsentimentoCookies() === 'all') {
        localStorage.setItem(STORAGE_THEME_KEY, proximo);
    }
}
