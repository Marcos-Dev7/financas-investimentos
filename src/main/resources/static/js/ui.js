export const ICON_SVG = {
    check: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="20 6 9 17 4 12"/></svg>',
    clock: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>',
    alert: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>'
};

const CAT_COLORS = [
    'var(--cat-1)', 'var(--cat-2)', 'var(--cat-3)', 'var(--cat-4)',
    'var(--cat-5)', 'var(--cat-6)', 'var(--cat-7)', 'var(--cat-8)'
];

let ultimoElementoFocado = null;
let modalAtivoEl = null;
let resolverConfirmacao = null;

export function formatCurrency(valor) {
    const num = Number(valor || 0);
    return num.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

export function brToIsoDate(brDate) {
    if (!brDate || !brDate.includes('/')) return brDate || '';
    const [d, m, y] = brDate.split('/');
    return `${y}-${m.padStart(2, '0')}-${d.padStart(2, '0')}`;
}

export function isoToBrDate(isoDate) {
    if (!isoDate || !isoDate.includes('-')) return isoDate || '';
    const [y, m, d] = isoDate.split('-');
    return `${d.padStart(2, '0')}/${m.padStart(2, '0')}/${y}`;
}

export function getInitials(nome) {
    if (!nome) return 'FF';
    const parts = nome.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

export function escapeHtml(str) {
    return String(str ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

export function showToast(message, type = 'success') {
    const container = document.getElementById('toastContainer');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.setAttribute('role', type === 'error' ? 'alert' : 'status');
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 4200);
}

export function renderizarStatusPill(el, status) {
    if (!el) return;
    const mapa = {
        POSITIVO: { cls: 'status-POSITIVO', icon: ICON_SVG.check, texto: 'POSITIVO' },
        LIMITE: { cls: 'status-LIMITE', icon: ICON_SVG.clock, texto: 'LIMITE' },
        DEFICIT: { cls: 'status-DEFICIT', icon: ICON_SVG.alert, texto: 'DÉFICIT' }
    };
    const cfg = mapa[status] || mapa.LIMITE;
    el.className = `status-pill ${cfg.cls}`;
    el.innerHTML = `${cfg.icon}<span>${cfg.texto}</span>`;
}

export function renderizarBadgePagamento(isPaga) {
    return isPaga
        ? `<span class="badge badge-paga">${ICON_SVG.check}<span>Paga</span></span>`
        : `<span class="badge badge-pendente">${ICON_SVG.clock}<span>Pendente</span></span>`;
}

const SAFE_DATA_IMAGE_RE = /^data:image\/(jpeg|jpg|png|webp);base64,[A-Za-z0-9+/=]+$/;

export function renderizarAvatarElemento(initialsElId, imgElId, nome, fotoBase64) {
    const initialsEl = document.getElementById(initialsElId);
    const imgEl = document.getElementById(imgElId);
    if (!initialsEl || !imgEl) return;

    if (typeof fotoBase64 === 'string' && SAFE_DATA_IMAGE_RE.test(fotoBase64.trim())) {
        imgEl.src = fotoBase64.trim();
        imgEl.classList.remove('hidden');
        initialsEl.classList.add('hidden');
    } else {
        imgEl.src = '';
        imgEl.classList.add('hidden');
        initialsEl.textContent = getInitials(nome);
        initialsEl.classList.remove('hidden');
    }
}

export function renderizarBarrasCategorias(containerEl, itens, mensagemVazia) {
    if (!containerEl) return;
    if (!Array.isArray(itens) || itens.length === 0) {
        containerEl.innerHTML = `<div class="empty-state-sm">${mensagemVazia}</div>`;
        return;
    }

    const totaisPorCat = {};
    let somaTotal = 0;

    itens.forEach(d => {
        const cat = d.categoria || 'Outros';
        const val = Number(d.valor || 0);
        totaisPorCat[cat] = (totaisPorCat[cat] || 0) + val;
        somaTotal += val;
    });

    const ordenadas = Object.entries(totaisPorCat).sort((a, b) => b[1] - a[1]);

    containerEl.innerHTML = ordenadas.map(([cat, valor], idx) => {
        const pct = somaTotal > 0 ? Math.round((valor / somaTotal) * 100) : 0;
        const cor = CAT_COLORS[idx % CAT_COLORS.length];
        return `
            <div class="cat-bar-item">
                <div class="cat-bar-top">
                    <span class="cat-label-dot">
                        <span class="cat-dot" style="background-color: ${cor};" aria-hidden="true"></span>
                        <span>${escapeHtml(cat)}</span>
                    </span>
                    <span>${formatCurrency(valor)} (${pct}%)</span>
                </div>
                <div class="progress-wrap">
                    <div class="progress-bar" style="width: ${pct}%; background-color: ${cor};"></div>
                </div>
            </div>
        `;
    }).join('');
}

export function avaliarRegrasSenha(senha) {
    return {
        len: senha.length >= 8,
        upper: /[A-Z]/.test(senha),
        lower: /[a-z]/.test(senha),
        num: /[0-9]/.test(senha),
        spec: /[^A-Za-z0-9\s]/.test(senha)
    };
}

export function atualizarChecklistSenha(senha, containerId) {
    const regras = avaliarRegrasSenha(senha);
    const container = document.getElementById(containerId);
    if (!container) return false;
    Object.entries(regras).forEach(([key, ok]) => {
        const item = container.querySelector(`[data-rule="${key}"]`);
        if (item) item.classList.toggle('valid', ok);
    });
    return Object.values(regras).every(Boolean);
}

export function atualizarDicaConfirmacao(senha, confirmacao, hintId) {
    const hint = document.getElementById(hintId);
    if (!hint) return;
    if (!confirmacao) {
        hint.textContent = '';
        hint.className = 'match-hint';
        return;
    }
    if (senha === confirmacao) {
        hint.textContent = '✓ As senhas coincidem perfeitamente';
        hint.className = 'match-hint ok';
    } else {
        hint.textContent = '✗ As senhas não coincidem';
        hint.className = 'match-hint err';
    }
}

/* ==========================================
   MODAIS ACESSÍVEIS (FOCUS TRAP + ESC)
   ========================================== */
function obterFocaveis(container) {
    return Array.from(container.querySelectorAll(
        'button:not([disabled]), [href], input:not([disabled]):not([type="hidden"]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
    )).filter(el => el.offsetParent !== null);
}

export function abrirModalAcessivel(backdropEl, focoInicialEl = null) {
    if (!backdropEl) return;
    ultimoElementoFocado = document.activeElement;
    modalAtivoEl = backdropEl;
    backdropEl.classList.remove('hidden');

    requestAnimationFrame(() => {
        const alvo = focoInicialEl || obterFocaveis(backdropEl)[0];
        if (alvo) alvo.focus();
    });
}

export function fecharModalAcessivel(backdropEl) {
    if (!backdropEl || backdropEl.classList.contains('hidden')) return;
    backdropEl.classList.add('hidden');
    if (modalAtivoEl === backdropEl) modalAtivoEl = null;

    if (backdropEl.id === 'modalConfirmacao' && resolverConfirmacao) {
        const resolver = resolverConfirmacao;
        resolverConfirmacao = null;
        resolver(false);
    }

    if (ultimoElementoFocado && typeof ultimoElementoFocado.focus === 'function') {
        ultimoElementoFocado.focus();
        ultimoElementoFocado = null;
    }
}

export function confirmarAcaoDestrutiva(titulo, texto, rotuloBotao = 'Confirmar') {
    const backdrop = document.getElementById('modalConfirmacao');
    document.getElementById('modalConfirmacaoTitulo').textContent = titulo;
    document.getElementById('modalConfirmacaoTexto').textContent = texto;
    const btnConfirmar = document.getElementById('btnConfirmarAcaoDestrutiva');
    btnConfirmar.textContent = rotuloBotao;

    return new Promise((resolve) => {
        resolverConfirmacao = resolve;
        abrirModalAcessivel(backdrop, document.getElementById('btnCancelarConfirmacao'));
    });
}

export function inicializarModaisEConfirmacao() {
    const backdropConf = document.getElementById('modalConfirmacao');
    const cancelar = () => fecharModalAcessivel(backdropConf);

    document.getElementById('btnFecharConfirmacao')?.addEventListener('click', cancelar);
    document.getElementById('btnCancelarConfirmacao')?.addEventListener('click', cancelar);
    document.getElementById('btnConfirmarAcaoDestrutiva')?.addEventListener('click', () => {
        if (resolverConfirmacao) {
            const resolver = resolverConfirmacao;
            resolverConfirmacao = null;
            backdropConf.classList.add('hidden');
            modalAtivoEl = null;
            if (ultimoElementoFocado && typeof ultimoElementoFocado.focus === 'function') {
                ultimoElementoFocado.focus();
                ultimoElementoFocado = null;
            }
            resolver(true);
        }
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            if (modalAtivoEl) {
                e.preventDefault();
                fecharModalAcessivel(modalAtivoEl);
            } else {
                fecharSidebarMobile();
            }
            return;
        }
        if (e.key === 'Tab' && modalAtivoEl) {
            const focaveis = obterFocaveis(modalAtivoEl);
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
}

/* ==========================================
   SIDEBAR MOBILE DRAWER
   ========================================== */
export function abrirSidebarMobile() {
    const sidebar = document.getElementById('appSidebar');
    const backdrop = document.getElementById('sidebarBackdrop');
    const btn = document.getElementById('btnOpenSidebar');
    if (sidebar) sidebar.classList.add('is-open');
    if (backdrop) backdrop.classList.remove('hidden');
    if (btn) btn.setAttribute('aria-expanded', 'true');
}

export function fecharSidebarMobile() {
    const sidebar = document.getElementById('appSidebar');
    const backdrop = document.getElementById('sidebarBackdrop');
    const btn = document.getElementById('btnOpenSidebar');
    if (sidebar) sidebar.classList.remove('is-open');
    if (backdrop) backdrop.classList.add('hidden');
    if (btn) btn.setAttribute('aria-expanded', 'false');
}
