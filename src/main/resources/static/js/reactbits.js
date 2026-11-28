/* ==========================================================================
   REACT BITS INSPIRED VANILLA ES MODULE (CSP-SAFE, ZERO DEPENDENCIES)
   - DotGrid Interactive Canvas Background (com botão Pause e Reduced Motion)
   - SpotlightCard Cursor Reflection
   - CountUp Numeric Currency Animation
   - ClickSpark Micro-Celebration
   - Hero Split Entrance (data-rb-enter)
   ========================================================================== */

const prefersReducedMotion = () =>
    window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

const isFinePointer = () =>
    window.matchMedia && window.matchMedia('(hover: hover) and (pointer: fine)').matches;

/* 1. Hero Entrance (executa uma única vez no carregamento) */
export function inicializarEntradaHero() {
    const hero = document.querySelector('.auth-hero');
    if (!hero || prefersReducedMotion()) return;

    const items = hero.querySelectorAll('[data-rb-enter]');
    items.forEach((el, idx) => {
        el.style.setProperty('--rb-delay', `${idx * 75}ms`);
    });

    hero.classList.add('rb-enter-ready');
    requestAnimationFrame(() => {
        requestAnimationFrame(() => {
            hero.classList.add('rb-entered');
        });
    });
}

/* 2. SpotlightCard (delegado global para cards estáticos e dinâmicos) */
export function inicializarSpotlightCards() {
    if (!isFinePointer() || prefersReducedMotion()) return;

    let cardAtivo = null;

    document.addEventListener('pointermove', (e) => {
        const card = e.target.closest('.rb-spotlight');
        if (cardAtivo && cardAtivo !== card) {
            cardAtivo.style.setProperty('--spot-opacity', '0');
            cardAtivo = null;
        }
        if (!card) return;
        cardAtivo = card;
        const rect = card.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        card.style.setProperty('--spot-x', `${x}px`);
        card.style.setProperty('--spot-y', `${y}px`);
        card.style.setProperty('--spot-opacity', '1');
    }, { passive: true });

    document.addEventListener('pointerleave', () => {
        if (cardAtivo) {
            cardAtivo.style.setProperty('--spot-opacity', '0');
            cardAtivo = null;
        }
    });
}

/* 3. CountUp Animado para KPIs Financeiros (ease-out 680ms) */
const animacoesAtivas = new WeakMap();

export function animarValorMoeda(el, valorFinal, formatador) {
    if (!el) return;
    const alvo = Number(valorFinal || 0);

    if (prefersReducedMotion()) {
        el.textContent = formatador(alvo);
        el.dataset.lastValue = String(alvo);
        return;
    }

    const anterior = animacoesAtivas.get(el);
    if (anterior) cancelAnimationFrame(anterior);

    const inicioValor = el.dataset.lastValue !== undefined ? Number(el.dataset.lastValue) : 0;
    if (Math.abs(alvo - inicioValor) < 0.005) {
        el.textContent = formatador(alvo);
        el.dataset.lastValue = String(alvo);
        return;
    }

    const duracao = 680;
    const t0 = performance.now();

    const step = (agora) => {
        const progresso = Math.min(1, (agora - t0) / duracao);
        const eased = 1 - Math.pow(1 - progresso, 3);
        const atual = inicioValor + (alvo - inicioValor) * eased;
        el.textContent = formatador(atual);

        if (progresso < 1) {
            animacoesAtivas.set(el, requestAnimationFrame(step));
        } else {
            el.textContent = formatador(alvo);
            el.dataset.lastValue = String(alvo);
            animacoesAtivas.delete(el);
        }
    };

    animacoesAtivas.set(el, requestAnimationFrame(step));
}

/* 4. ClickSpark (Micro-celebração geométrica discreta em ações financeiras) */
let sparkCanvas = null;
let sparkCtx = null;
let sparks = [];
let sparkRaf = null;

function garantirSparkCanvas() {
    if (sparkCanvas) return;
    sparkCanvas = document.createElement('canvas');
    sparkCanvas.className = 'rb-spark-canvas';
    sparkCanvas.setAttribute('aria-hidden', 'true');
    document.body.appendChild(sparkCanvas);
    sparkCtx = sparkCanvas.getContext('2d');
    const redimensionar = () => {
        const dpr = Math.min(window.devicePixelRatio || 1, 2);
        sparkCanvas.width = window.innerWidth * dpr;
        sparkCanvas.height = window.innerHeight * dpr;
        sparkCtx.setTransform(dpr, 0, 0, dpr, 0, 0);
    };
    redimensionar();
    window.addEventListener('resize', redimensionar, { passive: true });
}

export function dispararClickSpark(x, y) {
    if (prefersReducedMotion() || !Number.isFinite(x) || !Number.isFinite(y)) return;
    garantirSparkCanvas();

    const style = getComputedStyle(document.documentElement);
    const cor = style.getPropertyValue('--profit-fg').trim() || '#3AD19B';
    const total = 8;
    const agora = performance.now();

    for (let i = 0; i < total; i++) {
        sparks.push({
            x,
            y,
            angle: (Math.PI * 2 * i) / total,
            start: agora,
            duration: 360,
            color: cor
        });
    }

    if (!sparkRaf) {
        const render = (t) => {
            sparkCtx.clearRect(0, 0, window.innerWidth, window.innerHeight);
            sparks = sparks.filter((s) => {
                const elapsed = t - s.start;
                if (elapsed >= s.duration) return false;
                const p = elapsed / s.duration;
                const eased = 1 - Math.pow(1 - p, 3);
                const dist = eased * 24;
                const len = (1 - eased) * 8;
                const x1 = s.x + Math.cos(s.angle) * dist;
                const y1 = s.y + Math.sin(s.angle) * dist;
                const x2 = s.x + Math.cos(s.angle) * (dist + len);
                const y2 = s.y + Math.sin(s.angle) * (dist + len);

                sparkCtx.strokeStyle = s.color;
                sparkCtx.globalAlpha = 1 - p;
                sparkCtx.lineWidth = 1.6;
                sparkCtx.beginPath();
                sparkCtx.moveTo(x1, y1);
                sparkCtx.lineTo(x2, y2);
                sparkCtx.stroke();
                return true;
            });
            sparkCtx.globalAlpha = 1;
            sparkRaf = sparks.length > 0 ? requestAnimationFrame(render) : null;
        };
        sparkRaf = requestAnimationFrame(render);
    }
}

/* 5. DotGrid Interactive Canvas Background (React Bits DotGrid no Auth Hero) */
export function inicializarDotGrid(canvasId, pauseBtnId) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;
    const container = canvas.parentElement;
    const ctx = canvas.getContext('2d');
    if (!ctx || !container) return;

    const pauseBtn = pauseBtnId ? document.getElementById(pauseBtnId) : null;
    const PAUSE_ICON = '<svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/></svg>';
    const PLAY_ICON = '<svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><polygon points="6 4 20 12 6 20 6 4"/></svg>';

    let width = 0;
    let height = 0;
    let pointerX = -9999;
    let pointerY = -9999;
    let paused = prefersReducedMotion();
    let rafId = null;

    const gap = 26;
    const radiusBase = 1.15;
    const radiusBoost = 1.7;
    const proximity = 135;

    let dotBaseColor = 'rgba(242, 236, 226, 0.14)';
    let dotActiveColor = 'rgba(201, 164, 90, 0.72)';

    const lerCoresTema = () => {
        const cs = getComputedStyle(document.documentElement);
        dotBaseColor = cs.getPropertyValue('--dot-base').trim() || dotBaseColor;
        dotActiveColor = cs.getPropertyValue('--dot-active').trim() || dotActiveColor;
    };

    const redimensionar = () => {
        const rect = container.getBoundingClientRect();
        width = Math.max(1, rect.width);
        height = Math.max(1, rect.height);
        const dpr = Math.min(window.devicePixelRatio || 1, 2);
        canvas.width = width * dpr;
        canvas.height = height * dpr;
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        desenhar(performance.now());
    };

    const desenhar = (tempo) => {
        ctx.clearRect(0, 0, width, height);
        const cols = Math.ceil(width / gap);
        const rows = Math.ceil(height / gap);
        const t = paused ? 0 : tempo * 0.00055;

        for (let r = 0; r <= rows; r++) {
            const y = r * gap + 12;
            for (let c = 0; c <= cols; c++) {
                const x = c * gap + 12;
                const dx = x - pointerX;
                const dy = y - pointerY;
                const dist = Math.hypot(dx, dy);

                const wave = paused ? 0 : (Math.sin(c * 0.35 + t) * Math.cos(r * 0.35 - t) * 0.5 + 0.5);
                const near = dist < proximity ? 1 - dist / proximity : 0;
                const radius = radiusBase + near * radiusBoost + wave * 0.25;

                ctx.beginPath();
                ctx.arc(x, y, radius, 0, Math.PI * 2);
                ctx.fillStyle = near > 0.12 ? dotActiveColor : dotBaseColor;
                ctx.fill();
            }
        }
    };

    const loop = (tempo) => {
        if (paused || document.hidden || container.offsetParent === null) {
            rafId = null;
            return;
        }
        desenhar(tempo);
        rafId = requestAnimationFrame(loop);
    };

    const iniciar = () => {
        if (!paused && !rafId && isFinePointer() && !prefersReducedMotion()) {
            rafId = requestAnimationFrame(loop);
        } else {
            desenhar(performance.now());
        }
    };

    const atualizarBotaoPause = () => {
        if (!pauseBtn) return;
        pauseBtn.innerHTML = paused ? PLAY_ICON : PAUSE_ICON;
        pauseBtn.setAttribute('aria-label', paused ? 'Retomar animação do fundo' : 'Pausar animação do fundo');
        pauseBtn.setAttribute('aria-pressed', String(paused));
    };

    container.addEventListener('pointermove', (e) => {
        if (paused) return;
        const rect = container.getBoundingClientRect();
        pointerX = e.clientX - rect.left;
        pointerY = e.clientY - rect.top;
    }, { passive: true });

    container.addEventListener('pointerleave', () => {
        pointerX = -9999;
        pointerY = -9999;
    });

    pauseBtn?.addEventListener('click', () => {
        paused = !paused;
        atualizarBotaoPause();
        if (paused && rafId) {
            cancelAnimationFrame(rafId);
            rafId = null;
            desenhar(performance.now());
        } else {
            iniciar();
        }
    });

    window.addEventListener('resize', redimensionar, { passive: true });
    window.addEventListener('ff:theme-change', () => {
        lerCoresTema();
        desenhar(performance.now());
    });
    document.addEventListener('visibilitychange', () => {
        if (!document.hidden) iniciar();
    });

    lerCoresTema();
    atualizarBotaoPause();
    redimensionar();
    iniciar();
}
