import {
    COOKIE_CONSENT_NAME,
    MESES,
    MESES_LABEL,
    STORAGE_KEEP_LOGGED_KEY,
    STORAGE_TOKEN_KEY,
    STORAGE_USER_KEY,
    alternarTema,
    inicializarTema,
    obterConsentimentoCookies,
    resetarEstadoSessao,
    salvarSessao,
    setCookie,
    sincronizarUsuarioEmStorage,
    state
} from './state.js';
import { apiFetch, registrarHandlerSessaoExpirada } from './api.js';
import {
    ICON_SVG,
    abrirModalAcessivel,
    abrirSidebarMobile,
    atualizarChecklistSenha,
    atualizarDicaConfirmacao,
    brToIsoDate,
    confirmarAcaoDestrutiva,
    escapeHtml,
    fecharModalAcessivel,
    fecharSidebarMobile,
    formatCurrency,
    inicializarModaisEConfirmacao,
    isoToBrDate,
    renderizarAvatarElemento,
    renderizarBadgePagamento,
    renderizarBarrasCategorias,
    renderizarStatusPill,
    showToast
} from './ui.js';

/* ==========================================
   NAVEGAÇÃO, SESSÃO E AUTH VIEWS
   ========================================== */
function atualizarDadosUsuarioNaTela() {
    if (!state.user) return;
    document.getElementById('userNameDisplay').textContent = state.user.nome;
    document.getElementById('userEmailDisplay').textContent = state.user.email;

    renderizarAvatarElemento('userAvatarInitials', 'userAvatarImg', state.user.nome, state.user.fotoPerfil);
    renderizarAvatarElemento('perfilAvatarLg', 'perfilAvatarImgLg', state.user.nome, state.user.fotoPerfil);

    document.getElementById('btnRemoverFoto')?.classList.toggle('hidden', !state.user.fotoPerfil);

    document.getElementById('perfilNomeHeader').textContent = state.user.nome;
    document.getElementById('perfilEmailHeader').textContent = state.user.email;
    document.getElementById('perfilInputEmail').value = state.user.email;
    document.getElementById('perfilInputNome').value = state.user.nome;

    const telInput = document.getElementById('perfilInputTelefone');
    if (telInput) telInput.value = state.user.telefone || '';
    const emailRecInput = document.getElementById('perfilInputEmailRecuperacao');
    if (emailRecInput) emailRecInput.value = state.user.emailRecuperacao || '';

    const badge2Fa = document.getElementById('badgeStatus2Fa');
    const btnDesativar2Fa = document.getElementById('btnDesativar2Fa');
    if (badge2Fa) {
        const ativo = Boolean(state.user.doisFatoresAtivo);
        const tipo = state.user.tipoDoisFatores === 'APP' ? 'App Autenticador (TOTP)' : 'E-mail';
        badge2Fa.textContent = ativo ? `Ativo (${tipo})` : 'Desativado';
        badge2Fa.className = `badge ${ativo ? 'badge-paga' : 'badge-pendente'}`;
    }
    if (btnDesativar2Fa) {
        btnDesativar2Fa.classList.toggle('hidden', !state.user.doisFatoresAtivo);
    }
}

function navegarPara(aba) {
    state.abaAtiva = aba;
    fecharSidebarMobile();

    const dashboardView = document.getElementById('dashboardView');
    const familiaView = document.getElementById('familiaView');
    const accountView = document.getElementById('accountView');
    const navDashboardBtn = document.getElementById('navDashboardBtn');
    const navFamiliaBtn = document.getElementById('navFamiliaBtn');
    const navAccountBtn = document.getElementById('navAccountBtn');
    const monthSelectorBar = document.getElementById('monthSelectorBar');
    const topbarTitle = document.getElementById('topbarPageTitle');

    dashboardView.classList.add('hidden');
    familiaView?.classList.add('hidden');
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
        if (topbarTitle) topbarTitle.textContent = 'Minha Conta & Configurações';
        atualizarDadosUsuarioNaTela();
        document.getElementById('statTotalDespesas').textContent = String(state.totalDespesasCount || state.despesas.length);
        document.getElementById('statTotalSaldos').textContent = String(state.saldos.length);
        carregarModuloFamilia();
    } else if (aba === 'familia' && navFamiliaBtn && !navFamiliaBtn.classList.contains('hidden')) {
        familiaView?.classList.remove('hidden');
        navFamiliaBtn.classList.add('active');
        navFamiliaBtn.setAttribute('aria-current', 'page');
        monthSelectorBar.classList.remove('hidden');
        if (topbarTitle) topbarTitle.textContent = 'Visão Consolidada da Família';
        carregarModuloFamilia();
    } else {
        state.abaAtiva = 'dashboard';
        dashboardView.classList.remove('hidden');
        navDashboardBtn.classList.add('active');
        navDashboardBtn.setAttribute('aria-current', 'page');
        monthSelectorBar.classList.remove('hidden');
        if (topbarTitle) topbarTitle.textContent = 'Painel Financeiro';
    }
}

function mostrarTelaAuth(modo = 'login', emailVerificacao = '') {
    document.getElementById('authScreen').classList.remove('hidden');
    document.getElementById('appScreen').classList.add('hidden');

    const tabsWrap = document.getElementById('authTabsWrap');
    const tabLoginBtn = document.getElementById('tabLoginBtn');
    const tabRegisterBtn = document.getElementById('tabRegisterBtn');
    const forms = [
        'formLogin', 'formRegister', 'formVerifyEmail',
        'formVerificar2Fa', 'formEsqueciSenha', 'formRedefinirSenha'
    ];
    forms.forEach(id => document.getElementById(id)?.classList.add('hidden'));

    if (modo === 'verify') {
        tabsWrap.classList.add('hidden');
        document.getElementById('formVerifyEmail').classList.remove('hidden');
        state.pendingVerifyEmail = emailVerificacao;
        document.getElementById('verifyEmailTarget').textContent = emailVerificacao;
        document.getElementById('inputCodigoVerificacao').value = '';
    } else if (modo === '2fa') {
        tabsWrap.classList.add('hidden');
        document.getElementById('formVerificar2Fa')?.classList.remove('hidden');
        const input2Fa = document.getElementById('inputCodigo2Fa');
        if (input2Fa) {
            input2Fa.value = '';
            input2Fa.focus();
        }
    } else if (modo === 'forgot') {
        tabsWrap.classList.add('hidden');
        document.getElementById('formEsqueciSenha')?.classList.remove('hidden');
    } else if (modo === 'reset') {
        tabsWrap.classList.add('hidden');
        document.getElementById('formRedefinirSenha')?.classList.remove('hidden');
    } else if (modo === 'register') {
        tabsWrap.classList.remove('hidden');
        tabRegisterBtn.classList.add('active');
        tabRegisterBtn.setAttribute('aria-selected', 'true');
        tabLoginBtn.classList.remove('active');
        tabLoginBtn.setAttribute('aria-selected', 'false');
        document.getElementById('formRegister').classList.remove('hidden');
    } else {
        tabsWrap.classList.remove('hidden');
        tabLoginBtn.classList.add('active');
        tabLoginBtn.setAttribute('aria-selected', 'true');
        tabRegisterBtn.classList.remove('active');
        tabRegisterBtn.setAttribute('aria-selected', 'false');
        document.getElementById('formLogin').classList.remove('hidden');
    }
}

function mostrarTelaApp() {
    document.getElementById('authScreen').classList.add('hidden');
    document.getElementById('appScreen').classList.remove('hidden');
    navegarPara('dashboard');
    atualizarDadosUsuarioNaTela();
}

function limparSessao() {
    resetarEstadoSessao();
    document.getElementById('navFamiliaBtn')?.classList.add('hidden');
    fecharSidebarMobile();
    mostrarTelaAuth('login');
}

registrarHandlerSessaoExpirada(limparSessao);

/* ==========================================
   PAINEL FINANCEIRO (RESUMO, DESPESAS, SALDOS)
   ========================================== */
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
        const caixaAtual = resumo.saldoCaixaAtual !== undefined ? Number(resumo.saldoCaixaAtual) : (receita - pagas);

        document.getElementById('kpiReceitaTotal').textContent = formatCurrency(receita);
        document.getElementById('kpiDespesasPagas').textContent = formatCurrency(pagas);
        document.getElementById('kpiFaltaPagar').textContent = `Pendente no mês: ${formatCurrency(faltaPagar)}`;
        document.getElementById('kpiDespesasBrutas').textContent = formatCurrency(brutas);

        const elSaldo = document.getElementById('kpiSaldoDisponivel');
        elSaldo.textContent = formatCurrency(saldoPrevisto);
        elSaldo.classList.remove('text-danger', 'text-healthy');
        if (saldoPrevisto < 0) elSaldo.classList.add('text-danger');
        else if (saldoPrevisto > 0) elSaldo.classList.add('text-healthy');

        document.getElementById('kpiSaldoSubtexto').textContent = `Em caixa hoje (sem baixas): ${formatCurrency(caixaAtual)}`;
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

async function carregarDespesas() {
    try {
        const pageData = await apiFetch(`/despesas?page=${state.page}&size=${state.size}&sort=dataVencimento,asc`);
        state.despesas = pageData?.content || [];
        state.totalPages = Math.max(1, pageData?.totalPages || 1);
        state.totalDespesasCount = pageData?.totalElements ?? state.despesas.length;
        renderizarDespesas();
        const despesasMes = state.despesas.filter(d => (d.mes || '').toLowerCase() === state.mesAtual);
        renderizarBarrasCategorias(
            document.getElementById('categoryBreakdown'),
            despesasMes,
            `Nenhuma despesa em <strong>${MESES_LABEL[state.mesAtual]}</strong> para exibir por categoria.`
        );
    } catch (err) {
        document.getElementById('tbodyDespesas').innerHTML = `
            <tr><td colspan="7" class="empty-state">Não foi possível carregar as despesas (${escapeHtml(err.message)}).</td></tr>
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
            if (!desc.includes(termo) && !cat.includes(termo)) return false;
        }
        return true;
    });

    if (filtradas.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="empty-state">
                    Nenhuma despesa encontrada em <strong>${MESES_LABEL[state.mesAtual]}</strong> com os filtros atuais.
                </td>
            </tr>
        `;
    } else {
        tbody.innerHTML = filtradas.map(d => `
            <tr>
                <td data-label="Status">${renderizarBadgePagamento(d.isPaga)}</td>
                <td data-label="Descrição">
                    <div class="desc-cell">
                        <strong>${escapeHtml(d.descricao)}</strong>
                        <small>${MESES_LABEL[(d.mes || '').toLowerCase()] || d.mes}</small>
                    </div>
                </td>
                <td data-label="Categoria"><span class="badge badge-cat">${escapeHtml(d.categoria || 'Outros')}</span></td>
                <td data-label="Vencimento">${escapeHtml(d.dataVencimento || '-')}</td>
                <td data-label="Tipo">${d.isFixa ? '<span class="badge badge-fixa">Fixa</span>' : '<span class="income-meta">Variável</span>'}</td>
                <td data-label="Valor" class="text-right"><strong>${formatCurrency(d.valor)}</strong></td>
                <td data-label="Ações">
                    <div class="action-group">
                        ${d.isPaga
                            ? `<button type="button" class="btn btn-outline btn-sm" onclick="estornarDespesa(${d.id})">Estornar</button>`
                            : `<button type="button" class="btn btn-pay btn-sm" onclick="pagarDespesa(${d.id})">Pagar</button>`
                        }
                        <button type="button" class="btn btn-outline btn-sm" onclick="abrirEditarDespesa(${d.id})">Editar</button>
                        <button type="button" class="btn btn-danger-ghost btn-sm" onclick="excluirDespesa(${d.id})">Excluir</button>
                    </div>
                </td>
            </tr>
        `).join('');
    }

    document.getElementById('infoPaginacao').textContent = `Página ${state.page + 1} de ${state.totalPages}`;
    document.getElementById('btnPrevPage').disabled = state.page <= 0;
    document.getElementById('btnNextPage').disabled = state.page + 1 >= state.totalPages;
}

async function carregarSaldos() {
    try {
        const saldos = await apiFetch('/saldos');
        state.saldos = Array.isArray(saldos) ? saldos : [];
        renderizarSaldos();
    } catch (err) {
        document.getElementById('listaSaldos').innerHTML = `<div class="empty-state">Erro ao carregar receitas (${escapeHtml(err.message)}).</div>`;
    }
}

function renderizarSaldos() {
    const container = document.getElementById('listaSaldos');
    const filtrados = state.saldos.filter(s => !state.apenasSaldosMes || (s.mes || '').toLowerCase() === state.mesAtual);

    if (filtrados.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                Nenhuma receita cadastrada para <strong>${MESES_LABEL[state.mesAtual]}</strong>.<br>
                Clique em <strong>+ Nova Receita</strong> para registrar sua renda.
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
                <span class="income-val">${formatCurrency(s.valor)}</span>
            </div>
            <div class="income-bottom">
                <button type="button" class="btn btn-outline btn-sm" onclick="abrirEditarSaldo(${s.id})">Editar</button>
                <button type="button" class="btn btn-danger-ghost btn-sm" onclick="excluirSaldo(${s.id})">Excluir</button>
            </div>
        </div>
    `).join('');
}

/* ==========================================
   MÓDULO FAMÍLIA & GESTOR (/familia)
   ========================================== */
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

        state.souTitularFamilia = Boolean(painel.souTitular);
        document.getElementById('thFamAcoes')?.classList.toggle('hidden', !state.souTitularFamilia);

        const badgeModoFamilia = document.getElementById('badgeModoFamilia');
        const subtituloDespesasFamilia = document.getElementById('subtituloDespesasFamilia');
        if (badgeModoFamilia && subtituloDespesasFamilia) {
            if (state.souTitularFamilia) {
                badgeModoFamilia.textContent = 'Gestor da Família';
                badgeModoFamilia.className = 'badge badge-paga';
                subtituloDespesasFamilia.innerHTML = 'Modo <strong>Gestor</strong> — você pode realizar baixas e estornos de pagamento nas despesas compartilhadas da família';
            } else {
                badgeModoFamilia.textContent = 'Somente Leitura';
                badgeModoFamilia.className = 'badge badge-cat';
                subtituloDespesasFamilia.innerHTML = 'Visão consolidada em modo <strong>Somente Leitura</strong> — membros nunca alteram ou apagam dados de outros';
            }
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

        const membros = painel.membros || [];
        const semVinculos = !painel.possuiFamiliaAtiva && membros.length === 0;

        const chkRenda = document.getElementById('chkCompartilharMinhaRenda');
        chkRenda.checked = Boolean(painel.minhaRendaCompartilhada);
        chkRenda.disabled = semVinculos;

        const chkDespesas = document.getElementById('chkCompartilharMinhasDespesas');
        if (chkDespesas) {
            chkDespesas.checked = painel.minhasDespesasCompartilhadas !== undefined ? Boolean(painel.minhasDespesasCompartilhadas) : true;
            chkDespesas.disabled = semVinculos;
        }

        document.getElementById('blocoConvidarMembro')?.classList.toggle('hidden', !painel.souTitular);
        document.getElementById('blocoAceitarCodigoConvite')?.classList.toggle('hidden', painel.possuiFamiliaAtiva || membros.length > 0);

        const containerMembros = document.getElementById('listaMembrosFamilia');
        if (membros.length === 0) {
            containerMembros.innerHTML = `<div class="empty-state-sm">Nenhum membro vinculado ainda. Utilize o campo ao lado para convidar um familiar.</div>`;
        } else {
            containerMembros.innerHTML = membros.map(m => {
                const isAceito = m.status === 'ACEITO';
                const linkConvite = m.tokenConvite ? `${window.location.origin}/?convite=${m.tokenConvite}` : null;
                return `
                    <div class="income-item">
                        <div class="income-top">
                            <div class="income-info">
                                <h4>${escapeHtml(m.nome)}</h4>
                                <span class="income-meta">${escapeHtml(m.email)}</span>
                                ${m.tokenConvite ? `<div class="token-pill">Código: ${escapeHtml(m.tokenConvite)}</div>` : ''}
                            </div>
                            <span class="badge ${isAceito ? 'badge-paga' : 'badge-pendente'}">
                                ${isAceito ? `${ICON_SVG.check}<span>${m.isTitular ? 'Gestor' : 'Membro Ativo'}</span>` : `${ICON_SVG.clock}<span>Pendente</span>`}
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
            estadoSemFamilia?.classList.add('hidden');
            await Promise.all([
                carregarResumoFamiliaMes(),
                carregarDespesasFamiliaMes()
            ]);
        } else {
            painelConsolidado.classList.add('hidden');
            estadoSemFamilia?.classList.remove('hidden');
        }
    } catch (err) {
        console.error('Erro ao carregar módulo família:', err);
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
        const caixaFamAtual = resumo.saldoCaixaAtualFamiliar !== undefined ? Number(resumo.saldoCaixaAtualFamiliar) : (receita - pagas);

        document.getElementById('kpiFamReceitaTotal').textContent = formatCurrency(receita);
        document.getElementById('kpiFamDespesasPagas').textContent = formatCurrency(pagas);
        document.getElementById('kpiFamFaltaPagar').textContent = `Pendente no mês: ${formatCurrency(falta)}`;
        document.getElementById('kpiFamDespesasBrutas').textContent = formatCurrency(brutas);

        const elFamSaldo = document.getElementById('kpiFamSaldoDisponivel');
        elFamSaldo.textContent = formatCurrency(saldoFamPrevisto);
        elFamSaldo.classList.remove('text-danger', 'text-healthy');
        if (saldoFamPrevisto < 0) elFamSaldo.classList.add('text-danger');
        else if (saldoFamPrevisto > 0) elFamSaldo.classList.add('text-healthy');

        document.getElementById('kpiFamSaldoSubtexto').textContent = `Em caixa hoje (sem baixas): ${formatCurrency(caixaFamAtual)}`;
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
    try {
        const lista = await apiFetch(`/familia/despesas?mes=${state.mesAtual}`);
        state.despesasFamilia = Array.isArray(lista) ? lista : [];
        if (state.filtroResponsavelFamilia !== 'todos') {
            const existe = state.despesasFamilia.some(d => d.responsavelNome === state.filtroResponsavelFamilia);
            if (!existe) state.filtroResponsavelFamilia = 'todos';
        }
        renderizarPainelDespesasFamilia();
    } catch (err) {
        state.despesasFamilia = [];
        const totalColunas = state.souTitularFamilia ? 8 : 7;
        document.getElementById('tbodyDespesasFamilia').innerHTML = `<tr><td colspan="${totalColunas}" class="empty-state">${escapeHtml(err.message)}</td></tr>`;
        renderizarPainelDespesasFamilia();
    }
}

function renderizarPainelDespesasFamilia() {
    const lista = state.despesasFamilia || [];
    const totaisEl = document.getElementById('totaisPorMembroFamilia');
    const barraFiltro = document.getElementById('barraFiltroResponsavelFamilia');
    const tabsEl = document.getElementById('filtroResponsavelFamilia');
    const resumoFiltradoEl = document.getElementById('resumoTotalFiltradoFamilia');
    const tbody = document.getElementById('tbodyDespesasFamilia');
    const totalColunas = state.souTitularFamilia ? 8 : 7;

    if (lista.length === 0) {
        totaisEl?.classList.add('hidden');
        barraFiltro?.classList.add('hidden');
        tbody.innerHTML = `<tr><td colspan="${totalColunas}" class="empty-state">Nenhuma despesa familiar compartilhada em <strong>${MESES_LABEL[state.mesAtual]}</strong>.</td></tr>`;
        renderizarBarrasCategorias(
            document.getElementById('famCategoryBreakdown'),
            [],
            `Nenhuma despesa familiar compartilhada em <strong>${MESES_LABEL[state.mesAtual]}</strong> para exibir por categoria.`
        );
        return;
    }

    const porMembro = {};
    let totalGeral = 0;
    lista.forEach(d => {
        const nome = d.responsavelNome || 'Família';
        const valor = Number(d.valor || 0);
        if (!porMembro[nome]) porMembro[nome] = { total: 0, pagas: 0, qtd: 0 };
        porMembro[nome].total += valor;
        if (d.isPaga) porMembro[nome].pagas += valor;
        porMembro[nome].qtd += 1;
        totalGeral += valor;
    });

    const ordenados = Object.entries(porMembro).sort((a, b) => b[1].total - a[1].total);

    totaisEl.classList.remove('hidden');
    totaisEl.innerHTML = ordenados.map(([nome, dados]) => {
        const pct = totalGeral > 0 ? Math.round((dados.total / totalGeral) * 100) : 0;
        const pendente = Math.max(0, dados.total - dados.pagas);
        return `
            <div class="family-member-total-card">
                <div class="family-member-total-top">
                    <span>${escapeHtml(nome)}</span>
                    <span>${pct}%</span>
                </div>
                <div class="family-member-total-val">${formatCurrency(dados.total)}</div>
                <div class="family-member-total-meta">${dados.qtd} ${dados.qtd === 1 ? 'despesa' : 'despesas'} &bull; Pendente: ${formatCurrency(pendente)}</div>
            </div>
        `;
    }).join('');

    barraFiltro.classList.remove('hidden');
    tabsEl.innerHTML = [
        `<button type="button" class="filter-btn ${state.filtroResponsavelFamilia === 'todos' ? 'active' : ''}" data-resp="todos">Todos (${lista.length})</button>`,
        ...ordenados.map(([nome, dados]) =>
            `<button type="button" class="filter-btn ${state.filtroResponsavelFamilia === nome ? 'active' : ''}" data-resp="${escapeHtml(nome)}">${escapeHtml(nome)} (${dados.qtd})</button>`
        )
    ].join('');

    tabsEl.querySelectorAll('.filter-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            state.filtroResponsavelFamilia = btn.dataset.resp || 'todos';
            renderizarPainelDespesasFamilia();
        });
    });

    const filtradas = state.filtroResponsavelFamilia === 'todos'
        ? lista
        : lista.filter(d => (d.responsavelNome || 'Família') === state.filtroResponsavelFamilia);

    const somaFiltrada = filtradas.reduce((acc, d) => acc + Number(d.valor || 0), 0);
    if (resumoFiltradoEl) resumoFiltradoEl.textContent = `Total exibido: ${formatCurrency(somaFiltrada)}`;

    tbody.innerHTML = filtradas.map(d => `
        <tr>
            <td data-label="Status">${renderizarBadgePagamento(d.isPaga)}</td>
            <td data-label="Responsável"><span class="badge badge-fixa">${escapeHtml(d.responsavelNome)}</span></td>
            <td data-label="Descrição"><strong>${escapeHtml(d.descricao)}</strong></td>
            <td data-label="Categoria"><span class="badge badge-cat">${escapeHtml(d.categoria || 'Outros')}</span></td>
            <td data-label="Vencimento">${escapeHtml(d.dataVencimento || '-')}</td>
            <td data-label="Tipo">${d.isFixa ? '<span class="badge badge-fixa">Fixa</span>' : '<span class="income-meta">Variável</span>'}</td>
            <td data-label="Valor" class="text-right"><strong>${formatCurrency(d.valor)}</strong></td>
            ${state.souTitularFamilia ? `
            <td data-label="Ações">
                <div class="action-group">
                    ${d.isPaga
                        ? `<button type="button" class="btn btn-outline btn-sm" onclick="estornarDespesaFamilia(${d.id})">Estornar</button>`
                        : `<button type="button" class="btn btn-pay btn-sm" onclick="pagarDespesaFamilia(${d.id})">Pagar</button>`
                    }
                </div>
            </td>
            ` : ''}
        </tr>
    `).join('');

    renderizarBarrasCategorias(
        document.getElementById('famCategoryBreakdown'),
        filtradas,
        `Nenhuma despesa familiar compartilhada em <strong>${MESES_LABEL[state.mesAtual]}</strong> para exibir por categoria.`
    );
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

/* ==========================================
   AÇÕES GLOBAIS (CHAMADAS POR ONCLICK NA TABELA)
   ========================================== */
window.pagarDespesa = async (id) => {
    try {
        await apiFetch(`/despesas/${id}/pagar`, { method: 'PATCH' });
        showToast('Despesa marcada como paga.');
        await atualizarTudo();
    } catch (err) { showToast(err.message, 'error'); }
};

window.estornarDespesa = async (id) => {
    try {
        await apiFetch(`/despesas/${id}/estornar`, { method: 'PATCH' });
        showToast('Pagamento estornado.');
        await atualizarTudo();
    } catch (err) { showToast(err.message, 'error'); }
};

window.abrirEditarDespesa = (id) => {
    const d = state.despesas.find(item => item.id === id);
    if (!d) return;
    document.getElementById('modalDespesaTitulo').textContent = 'Editar Despesa';
    document.getElementById('despesaId').value = d.id;
    document.getElementById('despesaDescricao').value = d.descricao;
    document.getElementById('despesaValor').value = d.valor;
    document.getElementById('despesaDataVencimento').value = brToIsoDate(d.dataVencimento);
    document.getElementById('despesaMes').value = (d.mes || state.mesAtual).toLowerCase();
    document.getElementById('despesaCategoria').value = d.categoria || '';
    document.getElementById('despesaIsFixa').checked = Boolean(d.isFixa);
    abrirModalAcessivel(document.getElementById('modalDespesa'), document.getElementById('despesaDescricao'));
};

window.excluirDespesa = async (id) => {
    const d = state.despesas.find(item => item.id === id);
    const confirmado = await confirmarAcaoDestrutiva(
        'Excluir Despesa',
        `Deseja realmente excluir "${d?.descricao || 'esta despesa'}"? Esta ação não pode ser desfeita.`,
        'Excluir Despesa'
    );
    if (!confirmado) return;
    try {
        await apiFetch(`/despesas/${id}`, { method: 'DELETE' });
        showToast('Despesa excluída.');
        await atualizarTudo();
    } catch (err) { showToast(err.message, 'error'); }
};

window.abrirEditarSaldo = (id) => {
    const s = state.saldos.find(item => item.id === id);
    if (!s) return;
    document.getElementById('modalSaldoTitulo').textContent = 'Editar Receita';
    document.getElementById('saldoId').value = s.id;
    document.getElementById('saldoDescricao').value = s.descricao;
    document.getElementById('saldoValor').value = s.valor;
    document.getElementById('saldoMes').value = (s.mes || state.mesAtual).toLowerCase();
    document.getElementById('saldoAno').value = s.ano;
    abrirModalAcessivel(document.getElementById('modalSaldo'), document.getElementById('saldoDescricao'));
};

window.excluirSaldo = async (id) => {
    const s = state.saldos.find(item => item.id === id);
    const confirmado = await confirmarAcaoDestrutiva(
        'Excluir Receita',
        `Deseja realmente excluir a receita "${s?.descricao || 'selecionada'}"?`,
        'Excluir Receita'
    );
    if (!confirmado) return;
    try {
        await apiFetch(`/saldos/${id}`, { method: 'DELETE' });
        showToast('Receita excluída.');
        await atualizarTudo();
    } catch (err) { showToast(err.message, 'error'); }
};

window.pagarDespesaFamilia = async (id) => {
    try {
        await apiFetch(`/familia/despesas/${id}/pagar`, { method: 'PATCH' });
        showToast('Despesa familiar marcada como paga.');
        await atualizarTudo();
    } catch (err) { showToast(err.message, 'error'); }
};

window.estornarDespesaFamilia = async (id) => {
    try {
        await apiFetch(`/familia/despesas/${id}/estornar`, { method: 'PATCH' });
        showToast('Pagamento da despesa familiar estornado.');
        await atualizarTudo();
    } catch (err) { showToast(err.message, 'error'); }
};

window.aceitarConviteDireto = async (token) => {
    try {
        await apiFetch('/familia/convites/aceitar', { method: 'POST', body: JSON.stringify({ token }) });
        showToast('Convite aceito! Você agora faz parte da família.');
        await carregarModuloFamilia();
        navegarPara('familia');
    } catch (err) { showToast(err.message, 'error'); }
};

window.copiarLinkConvite = async (link, token) => {
    try {
        await navigator.clipboard.writeText(link);
        showToast('Link de convite copiado!');
    } catch (_) {
        showToast(`Código do convite: ${token}`);
    }
};

window.removerMembroFamilia = async (vinculoId, nome, souTitular) => {
    const titulo = souTitular ? 'Remover Membro / Convite' : 'Sair da Família';
    const texto = souTitular
        ? `Deseja remover "${nome}" da sua família? O acesso aos dados consolidados será cortado imediatamente.`
        : `Deseja sair da família de "${nome}"? Você deixará de visualizar o painel consolidado.`;
    const confirmado = await confirmarAcaoDestrutiva(titulo, texto, souTitular ? 'Remover' : 'Sair da Família');
    if (!confirmado) return;
    try {
        await apiFetch(`/familia/membros/${vinculoId}`, { method: 'DELETE' });
        showToast(souTitular ? 'Membro removido com sucesso.' : 'Você saiu da família.');
        await carregarModuloFamilia();
    } catch (err) { showToast(err.message, 'error'); }
};

/* ==========================================
   INICIALIZAÇÃO E EVENT LISTENERS
   ========================================== */
document.addEventListener('DOMContentLoaded', async () => {
    inicializarTema();
    inicializarModaisEConfirmacao();

    document.querySelectorAll('[data-theme-toggle]').forEach(btn => {
        btn.addEventListener('click', alternarTema);
    });

    // Sidebar Mobile Drawer
    document.getElementById('btnOpenSidebar')?.addEventListener('click', abrirSidebarMobile);
    document.getElementById('btnCloseSidebar')?.addEventListener('click', fecharSidebarMobile);
    document.getElementById('sidebarBackdrop')?.addEventListener('click', fecharSidebarMobile);

    // Navegação
    document.getElementById('brandHomeLink')?.addEventListener('click', () => navegarPara('dashboard'));
    document.getElementById('navDashboardBtn')?.addEventListener('click', () => navegarPara('dashboard'));
    document.getElementById('navFamiliaBtn')?.addEventListener('click', () => navegarPara('familia'));
    document.getElementById('navAccountBtn')?.addEventListener('click', () => navegarPara('account'));
    document.getElementById('btnPerfilRapido')?.addEventListener('click', () => navegarPara('account'));
    document.getElementById('btnVoltarPainel')?.addEventListener('click', () => navegarPara('dashboard'));
    document.getElementById('btnIrConfigurarFamilia')?.addEventListener('click', () => navegarPara('account'));
    document.getElementById('btnGerenciarFamilia')?.addEventListener('click', () => navegarPara('account'));

    document.getElementById('btnLogout')?.addEventListener('click', () => {
        limparSessao();
        showToast('Sessão encerrada.');
    });

    // Seletor global de mês
    const selectMesGlobal = document.getElementById('selectMesGlobal');
    selectMesGlobal.value = state.mesAtual;
    selectMesGlobal.addEventListener('change', async (e) => {
        state.mesAtual = e.target.value;
        await atualizarTudo();
    });
    document.getElementById('btnPrevMes')?.addEventListener('click', async () => {
        const idx = MESES.indexOf(state.mesAtual);
        state.mesAtual = MESES[(idx - 1 + 12) % 12];
        selectMesGlobal.value = state.mesAtual;
        await atualizarTudo();
    });
    document.getElementById('btnNextMes')?.addEventListener('click', async () => {
        const idx = MESES.indexOf(state.mesAtual);
        state.mesAtual = MESES[(idx + 1) % 12];
        selectMesGlobal.value = state.mesAtual;
        await atualizarTudo();
    });

    // Filtros e paginação de despesas
    document.querySelectorAll('#dashboardView .filter-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#dashboardView .filter-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            state.filtroDespesa = btn.dataset.filter;
            renderizarDespesas();
        });
    });
    document.getElementById('inputBuscaDespesa')?.addEventListener('input', (e) => {
        state.buscaDespesa = e.target.value;
        renderizarDespesas();
    });
    document.getElementById('btnPrevPage')?.addEventListener('click', async () => {
        if (state.page > 0) { state.page--; await carregarDespesas(); }
    });
    document.getElementById('btnNextPage')?.addEventListener('click', async () => {
        if (state.page + 1 < state.totalPages) { state.page++; await carregarDespesas(); }
    });
    document.getElementById('chkApenasSaldosMes')?.addEventListener('change', (e) => {
        state.apenasSaldosMes = e.target.checked;
        renderizarSaldos();
    });

    // Tabs Login / Cadastro
    document.getElementById('tabLoginBtn')?.addEventListener('click', () => mostrarTelaAuth('login'));
    document.getElementById('tabRegisterBtn')?.addEventListener('click', () => mostrarTelaAuth('register'));
    document.getElementById('btnBackToLogin')?.addEventListener('click', () => mostrarTelaAuth('login'));

    // Checklist em tempo real das senhas
    const vincularChecklist = (pwdId, confId, rulesId, hintId) => {
        const pwd = document.getElementById(pwdId);
        const conf = document.getElementById(confId);
        if (!pwd || !conf) return;
        pwd.addEventListener('input', () => {
            atualizarChecklistSenha(pwd.value, rulesId);
            atualizarDicaConfirmacao(pwd.value, conf.value, hintId);
        });
        conf.addEventListener('input', () => atualizarDicaConfirmacao(pwd.value, conf.value, hintId));
    };
    vincularChecklist('regSenha', 'regConfirmacaoSenha', 'regPwdRules', 'regMatchHint');
    vincularChecklist('novaSenhaInput', 'confirmacaoNovaSenhaInput', 'perfilPwdRules', 'perfilMatchHint');
    vincularChecklist('resetNovaSenha', 'resetConfirmacaoSenha', 'resetPwdRules', 'resetMatchHint');

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
                body: JSON.stringify({ email: emailDigitado, senha: document.getElementById('loginSenha').value })
            });

            if (data.requerDoisFatores && data.desafioToken) {
                state.pending2FaToken = data.desafioToken;
                state.pending2FaTipo = data.tipoDoisFatores || 'EMAIL';
                state.pending2FaManterConectado = manterConectado;
                document.getElementById('twoFaPromptText').textContent = state.pending2FaTipo === 'APP'
                    ? 'Abra seu aplicativo autenticador (Google Authenticator / Authy / Apple Senhas) e informe o código de 6 dígitos.'
                    : `Enviamos um código de segurança de 6 dígitos para ${data.email} (válido por 5 minutos).`;
                formLogin.reset();
                mostrarTelaAuth('2fa');
                showToast('Verificação em 2 fatores necessária para concluir o login.');
                return;
            }

            if (manterConectado && obterConsentimentoCookies() !== 'all') {
                setCookie(COOKIE_CONSENT_NAME, 'all');
                document.getElementById('cookieBanner')?.classList.add('hidden');
            }
            salvarSessao(data, manterConectado);
            formLogin.reset();
            const perfil = await apiFetch('/auth/me');
            if (perfil) sincronizarUsuarioEmStorage(perfil);
            mostrarTelaApp();
            showToast(`Bem-vindo(a), ${data.nome}!`);
            await atualizarTudo();
        } catch (err) {
            if (err.message?.startsWith('EMAIL_NAO_VERIFICADO:')) {
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
        const senha = document.getElementById('regSenha').value;
        const confirmacao = document.getElementById('regConfirmacaoSenha').value;
        if (!atualizarChecklistSenha(senha, 'regPwdRules')) {
            showToast('A senha precisa atender a todos os requisitos de segurança.', 'error');
            return;
        }
        if (senha !== confirmacao) {
            showToast('A confirmação de senha não coincide.', 'error');
            return;
        }
        const btn = document.getElementById('btnSubmitRegister');
        btn.disabled = true;
        btn.textContent = 'Criando conta...';
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
            showToast(resp.mensagem);
            mostrarTelaAuth('verify', resp.email);
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Criar Conta e Receber Código';
        }
    });

    // Verificação de E-mail
    document.getElementById('formVerifyEmail').addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = document.getElementById('btnSubmitVerify');
        btn.disabled = true;
        btn.textContent = 'Validando...';
        try {
            const data = await apiFetch('/auth/verificar-email', {
                method: 'POST',
                body: JSON.stringify({
                    email: state.pendingVerifyEmail,
                    codigo: document.getElementById('inputCodigoVerificacao').value.trim()
                })
            });
            salvarSessao(data, false);
            document.getElementById('formVerifyEmail').reset();
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

    document.getElementById('btnResendCode')?.addEventListener('click', async () => {
        if (!state.pendingVerifyEmail) return;
        try {
            const resp = await apiFetch('/auth/reenviar-codigo', {
                method: 'POST',
                body: JSON.stringify({ email: state.pendingVerifyEmail })
            });
            showToast(resp.mensagem);
        } catch (err) { showToast(err.message, 'error'); }
    });

    // 2FA Login e Minha Conta
    document.getElementById('btnVoltarLogin2Fa')?.addEventListener('click', () => {
        state.pending2FaToken = null;
        mostrarTelaAuth('login');
    });

    document.getElementById('formVerificar2Fa')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!state.pending2FaToken) return mostrarTelaAuth('login');
        const btn = document.getElementById('btnSubmit2Fa');
        btn.disabled = true;
        btn.textContent = 'Verificando...';
        try {
            const data = await apiFetch('/auth/verificar-2fa', {
                method: 'POST',
                body: JSON.stringify({
                    desafioToken: state.pending2FaToken,
                    codigo: document.getElementById('inputCodigo2Fa').value.trim()
                })
            });
            const manter = state.pending2FaManterConectado;
            state.pending2FaToken = null;
            state.pending2FaTipo = null;
            state.pending2FaManterConectado = false;

            if (manter && obterConsentimentoCookies() !== 'all') {
                setCookie(COOKIE_CONSENT_NAME, 'all');
                document.getElementById('cookieBanner')?.classList.add('hidden');
            }
            salvarSessao(data, manter);
            document.getElementById('formVerificar2Fa').reset();
            const perfil = await apiFetch('/auth/me');
            if (perfil) sincronizarUsuarioEmStorage(perfil);
            mostrarTelaApp();
            showToast(`Verificação 2FA concluída! Bem-vindo(a), ${data.nome}!`);
            await atualizarTudo();
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Confirmar e Entrar';
        }
    });

    document.getElementById('btnAtivar2FaEmail')?.addEventListener('click', async () => {
        const senhaInput = document.getElementById('twoFaSenhaAtualInput');
        if (!senhaInput?.value) {
            showToast('Digite sua senha atual acima para ativar o 2FA.', 'error');
            return senhaInput?.focus();
        }
        try {
            const perfil = await apiFetch('/auth/2fa/ativar-email', {
                method: 'POST',
                body: JSON.stringify({ senhaAtual: senhaInput.value })
            });
            sincronizarUsuarioEmStorage(perfil);
            senhaInput.value = '';
            document.getElementById('boxSetupTotp')?.classList.add('hidden');
            atualizarDadosUsuarioNaTela();
            showToast('2FA via E-mail ativado com sucesso!');
        } catch (err) { showToast(err.message, 'error'); }
    });

    document.getElementById('btnIniciarTotp')?.addEventListener('click', async () => {
        try {
            const resp = await apiFetch('/auth/2fa/totp/iniciar', { method: 'POST' });
            document.getElementById('totpSecretCode').textContent = resp.segredo;
            document.getElementById('linkOtpAuth').href = resp.otpauthUri;
            document.getElementById('inputCodigoConfirmarTotp').value = '';
            document.getElementById('boxSetupTotp').classList.remove('hidden');
            showToast('Chave gerada! Cadastre no app autenticador e confirme com o código de 6 dígitos.');
        } catch (err) { showToast(err.message, 'error'); }
    });

    document.getElementById('btnCopiarSegredoTotp')?.addEventListener('click', async () => {
        const segredo = document.getElementById('totpSecretCode')?.textContent?.trim();
        if (!segredo || segredo === '---') return;
        try {
            await navigator.clipboard.writeText(segredo);
            showToast('Chave secreta copiada!');
        } catch (_) { showToast('Selecione e copie a chave manualmente.'); }
    });

    document.getElementById('btnConfirmarTotp')?.addEventListener('click', async () => {
        const senhaInput = document.getElementById('twoFaSenhaAtualInput');
        const codigo = document.getElementById('inputCodigoConfirmarTotp')?.value.trim() || '';
        if (!senhaInput?.value) {
            showToast('Digite sua senha atual acima para confirmar o 2FA.', 'error');
            return senhaInput?.focus();
        }
        if (!/^\d{6}$/.test(codigo)) {
            showToast('Informe o código de 6 dígitos do aplicativo autenticador.', 'error');
            return;
        }
        try {
            const perfil = await apiFetch('/auth/2fa/totp/confirmar', {
                method: 'POST',
                body: JSON.stringify({ senhaAtual: senhaInput.value, codigo })
            });
            sincronizarUsuarioEmStorage(perfil);
            senhaInput.value = '';
            document.getElementById('inputCodigoConfirmarTotp').value = '';
            document.getElementById('boxSetupTotp')?.classList.add('hidden');
            atualizarDadosUsuarioNaTela();
            showToast('2FA via App Autenticador (TOTP) ativado!');
        } catch (err) { showToast(err.message, 'error'); }
    });

    document.getElementById('btnDesativar2Fa')?.addEventListener('click', async () => {
        const senhaInput = document.getElementById('twoFaSenhaAtualInput');
        if (!senhaInput?.value) {
            showToast('Digite sua senha atual acima para desativar o 2FA.', 'error');
            return senhaInput?.focus();
        }
        const ok = await confirmarAcaoDestrutiva(
            'Desativar Verificação em 2 Fatores',
            'Sua conta deixará de exigir código de segurança no login. Deseja realmente desativar o 2FA?',
            'Desativar 2FA'
        );
        if (!ok) return;
        try {
            const perfil = await apiFetch('/auth/2fa/desativar', {
                method: 'POST',
                body: JSON.stringify({ senhaAtual: senhaInput.value })
            });
            sincronizarUsuarioEmStorage(perfil);
            senhaInput.value = '';
            document.getElementById('boxSetupTotp')?.classList.add('hidden');
            atualizarDadosUsuarioNaTela();
            showToast('Verificação em 2 fatores desativada.');
        } catch (err) { showToast(err.message, 'error'); }
    });

    // Recuperação de Senha
    const recCanal = document.getElementById('recCanal');
    const recIdentificador = document.getElementById('recIdentificador');
    const recLabel = document.getElementById('recIdentificadorLabel');
    recCanal?.addEventListener('change', () => {
        const val = recCanal.value;
        if (val === 'TELEFONE') {
            recLabel.textContent = 'Telefone cadastrado';
            recIdentificador.type = 'tel';
            recIdentificador.placeholder = '(11) 99999-9999';
        } else if (val === 'EMAIL_RECUPERACAO') {
            recLabel.textContent = 'E-mail de recuperação';
            recIdentificador.type = 'email';
            recIdentificador.placeholder = 'secundario@dominio.com';
        } else {
            recLabel.textContent = 'E-mail principal';
            recIdentificador.type = 'email';
            recIdentificador.placeholder = 'nome@dominio.com';
        }
        recIdentificador.value = '';
    });

    document.getElementById('btnEsqueciSenha')?.addEventListener('click', () => {
        const emailLogin = document.getElementById('loginEmail')?.value.trim();
        if (emailLogin && recCanal?.value === 'EMAIL') recIdentificador.value = emailLogin;
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
                body: JSON.stringify({ canal: recCanal.value, identificador: recIdentificador.value.trim() })
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

    document.getElementById('formRedefinirSenha')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const novaSenha = document.getElementById('resetNovaSenha').value;
        const confirmacaoNovaSenha = document.getElementById('resetConfirmacaoSenha').value;
        if (!atualizarChecklistSenha(novaSenha, 'resetPwdRules')) {
            return showToast('A nova senha precisa atender a todas as regras de segurança.', 'error');
        }
        if (novaSenha !== confirmacaoNovaSenha) {
            return showToast('A confirmação da nova senha não coincide.', 'error');
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

    // Foto de Perfil (Canvas 256x256)
    const inputFotoPerfil = document.getElementById('inputFotoPerfil');
    document.getElementById('btnEscolherFoto')?.addEventListener('click', () => inputFotoPerfil.click());
    inputFotoPerfil?.addEventListener('change', (e) => {
        const file = e.target.files?.[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = (ev) => {
            const img = new Image();
            img.onload = async () => {
                const canvas = document.createElement('canvas');
                canvas.width = 256;
                canvas.height = 256;
                const ctx = canvas.getContext('2d');
                const minSide = Math.min(img.width, img.height);
                ctx.drawImage(img, (img.width - minSide) / 2, (img.height - minSide) / 2, minSide, minSide, 0, 0, 256, 256);
                const dataUrl = canvas.toDataURL('image/jpeg', 0.85);
                try {
                    const authAtualizado = await apiFetch('/auth/perfil', {
                        method: 'PUT',
                        body: JSON.stringify({ nome: state.user.nome, email: state.user.email, fotoPerfil: dataUrl })
                    });
                    salvarSessao(authAtualizado);
                    atualizarDadosUsuarioNaTela();
                    showToast('Foto de perfil atualizada.');
                } catch (err) { showToast(err.message, 'error'); }
                finally { inputFotoPerfil.value = ''; }
            };
            img.src = ev.target.result;
        };
        reader.readAsDataURL(file);
    });

    document.getElementById('btnRemoverFoto')?.addEventListener('click', async () => {
        const ok = await confirmarAcaoDestrutiva('Remover Foto de Perfil', 'Deseja voltar ao avatar com suas iniciais?', 'Remover Foto');
        if (!ok) return;
        try {
            const authAtualizado = await apiFetch('/auth/perfil', {
                method: 'PUT',
                body: JSON.stringify({ nome: state.user.nome, email: state.user.email, fotoPerfil: 'REMOVER' })
            });
            salvarSessao({ ...authAtualizado, fotoPerfil: null });
            atualizarDadosUsuarioNaTela();
            showToast('Foto de perfil removida.');
        } catch (err) { showToast(err.message, 'error'); }
    });

    // Perfil & Alterar Senha & Excluir Conta
    document.getElementById('formAtualizarPerfil')?.addEventListener('submit', async (e) => {
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
        } catch (err) { showToast(err.message, 'error'); }
        finally { btn.disabled = false; }
    });

    document.getElementById('formAlterarSenha')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const novaSenha = document.getElementById('novaSenhaInput').value;
        const confirmacaoNovaSenha = document.getElementById('confirmacaoNovaSenhaInput').value;
        if (!atualizarChecklistSenha(novaSenha, 'perfilPwdRules')) {
            return showToast('A nova senha precisa atender a todas as regras de segurança.', 'error');
        }
        if (novaSenha !== confirmacaoNovaSenha) {
            return showToast('A confirmação da nova senha não coincide.', 'error');
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
            document.getElementById('formAlterarSenha').reset();
            showToast('Senha alterada com sucesso.');
        } catch (err) { showToast(err.message, 'error'); }
        finally { btn.disabled = false; }
    });

    document.getElementById('btnExcluirMinhaConta')?.addEventListener('click', async () => {
        const ok = await confirmarAcaoDestrutiva(
            'Excluir Conta Permanentemente',
            'ATENÇÃO: Todos os seus saldos, despesas e vínculos familiares serão apagados definitivamente. Deseja continuar?',
            'Excluir Minha Conta'
        );
        if (!ok) return;
        try {
            await apiFetch('/auth/conta', { method: 'DELETE' });
            limparSessao();
            showToast('Sua conta foi excluída permanentemente.');
        } catch (err) { showToast(err.message, 'error'); }
    });

    // Família: Convite, Aceite por código, Switches Renda e Despesas
    document.getElementById('formEnviarConviteFamilia')?.addEventListener('submit', async (e) => {
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
            showToast('Convite enviado com validade de 48h.');
            await carregarModuloFamilia();
        } catch (err) { showToast(err.message, 'error'); }
        finally { btn.disabled = false; }
    });

    document.getElementById('formAceitarConviteToken')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = document.getElementById('btnAceitarConviteToken');
        btn.disabled = true;
        try {
            const input = document.getElementById('inputTokenConvite');
            let token = input.value.trim();
            if (token.includes('convite=')) token = new URL(token).searchParams.get('convite') || token;
            await apiFetch('/familia/convites/aceitar', { method: 'POST', body: JSON.stringify({ token }) });
            input.value = '';
            showToast('Convite aceito com sucesso!');
            await carregarModuloFamilia();
            navegarPara('familia');
        } catch (err) { showToast(err.message, 'error'); }
        finally { btn.disabled = false; }
    });

    document.getElementById('chkCompartilharMinhaRenda')?.addEventListener('change', async (e) => {
        const compartilhar = e.target.checked;
        try {
            await apiFetch('/familia/compartilhar-renda', { method: 'PATCH', body: JSON.stringify({ compartilhar }) });
            showToast(compartilhar ? 'Sua renda agora entra na soma familiar.' : 'Sua renda foi ocultada da soma familiar.');
            await carregarModuloFamilia();
        } catch (err) {
            e.target.checked = !compartilhar;
            showToast(err.message, 'error');
        }
    });

    document.getElementById('chkCompartilharMinhasDespesas')?.addEventListener('change', async (e) => {
        const compartilhar = e.target.checked;
        try {
            await apiFetch('/familia/compartilhar-despesas', { method: 'PATCH', body: JSON.stringify({ compartilhar }) });
            showToast(compartilhar ? 'Suas despesas agora aparecem no consolidado familiar.' : 'Suas despesas foram ocultadas do consolidado familiar.');
            await carregarModuloFamilia();
        } catch (err) {
            e.target.checked = !compartilhar;
            showToast(err.message, 'error');
        }
    });

    // Modais de Despesa e Receita
    const modalDespesa = document.getElementById('modalDespesa');
    document.getElementById('btnNovaDespesa')?.addEventListener('click', () => {
        document.getElementById('formDespesa').reset();
        document.getElementById('despesaId').value = '';
        document.getElementById('modalDespesaTitulo').textContent = 'Nova Despesa';
        document.getElementById('despesaMes').value = state.mesAtual;
        abrirModalAcessivel(modalDespesa, document.getElementById('despesaDescricao'));
    });
    const fecharDespesa = () => fecharModalAcessivel(modalDespesa);
    document.getElementById('btnFecharModalDespesa')?.addEventListener('click', fecharDespesa);
    document.getElementById('btnCancelarDespesa')?.addEventListener('click', fecharDespesa);

    document.getElementById('formDespesa')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('despesaId').value;
        const payload = {
            descricao: document.getElementById('despesaDescricao').value.trim(),
            valor: Number(document.getElementById('despesaValor').value),
            mes: document.getElementById('despesaMes').value,
            dataVencimento: isoToBrDate(document.getElementById('despesaDataVencimento').value),
            isFixa: document.getElementById('despesaIsFixa').checked,
            categoria: document.getElementById('despesaCategoria').value.trim() || 'Outros'
        };
        try {
            if (id) {
                await apiFetch(`/despesas/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
                showToast('Despesa atualizada.');
            } else {
                await apiFetch('/despesas', { method: 'POST', body: JSON.stringify(payload) });
                showToast('Despesa cadastrada.');
            }
            fecharDespesa();
            await atualizarTudo();
        } catch (err) { showToast(err.message, 'error'); }
    });

    const modalSaldo = document.getElementById('modalSaldo');
    document.getElementById('btnNovoSaldo')?.addEventListener('click', () => {
        document.getElementById('formSaldo').reset();
        document.getElementById('saldoId').value = '';
        document.getElementById('modalSaldoTitulo').textContent = 'Nova Receita';
        document.getElementById('saldoMes').value = state.mesAtual;
        document.getElementById('saldoAno').value = new Date().getFullYear();
        abrirModalAcessivel(modalSaldo, document.getElementById('saldoDescricao'));
    });
    const fecharSaldo = () => fecharModalAcessivel(modalSaldo);
    document.getElementById('btnFecharModalSaldo')?.addEventListener('click', fecharSaldo);
    document.getElementById('btnCancelarSaldo')?.addEventListener('click', fecharSaldo);

    document.getElementById('formSaldo')?.addEventListener('submit', async (e) => {
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
                await apiFetch(`/saldos/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
                showToast('Receita atualizada.');
            } else {
                await apiFetch('/saldos', { method: 'POST', body: JSON.stringify(payload) });
                showToast('Receita cadastrada.');
            }
            fecharSaldo();
            await atualizarTudo();
        } catch (err) { showToast(err.message, 'error'); }
    });

    // Consentimento & Preferências de Cookies
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
        cookieBanner?.classList.add('hidden');
        fecharModalAcessivel(modalCookies);
        showToast(tipo === 'all' ? 'Preferências salvas: todos os cookies habilitados.' : 'Preferências salvas: apenas cookies essenciais ativos.');
    };

    if (!obterConsentimentoCookies()) cookieBanner?.classList.remove('hidden');

    const abrirModalCookies = () => {
        if (chkCookiePreferencia) chkCookiePreferencia.checked = obterConsentimentoCookies() === 'all';
        abrirModalAcessivel(modalCookies, chkCookiePreferencia);
    };

    ['btnAbrirCookiesAuth', 'btnAbrirCookiesConta', 'btnAbrirCookiesFooter', 'btnCookiePersonalizar'].forEach(id => {
        document.getElementById(id)?.addEventListener('click', abrirModalCookies);
    });
    document.getElementById('btnFecharCookies')?.addEventListener('click', () => fecharModalAcessivel(modalCookies));
    document.getElementById('btnCookieEssenciais')?.addEventListener('click', () => aplicarPreferenciaCookies('essential'));
    document.getElementById('btnCookieAceitarTodos')?.addEventListener('click', () => aplicarPreferenciaCookies('all'));
    document.getElementById('btnSalvarCookiesEssenciais')?.addEventListener('click', () => aplicarPreferenciaCookies('essential'));
    document.getElementById('btnSalvarCookiesTodos')?.addEventListener('click', () => {
        aplicarPreferenciaCookies(chkCookiePreferencia?.checked ? 'all' : 'essential');
    });

    // Parâmetros da URL (?resetToken= ou ?convite=) e restauração de sessão ativa na aba
    const urlParams = new URLSearchParams(window.location.search);
    const tokenConviteUrl = urlParams.get('convite');
    const resetTokenUrl = urlParams.get('resetToken');
    if (tokenConviteUrl) document.getElementById('inputTokenConvite').value = tokenConviteUrl;

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
                sincronizarUsuarioEmStorage(perfil);
                mostrarTelaApp();
                await atualizarTudo();
                if (tokenConviteUrl) navegarPara('account');
                return;
            }
        } catch (_) {
            limparSessao();
        }
    } else {
        mostrarTelaAuth('login');
    }
});
