import { API_BASE, state } from './state.js';

let onUnauthorizedCallback = null;

export function registrarHandlerSessaoExpirada(callback) {
    onUnauthorizedCallback = callback;
}

export async function apiFetch(path, options = {}) {
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
        if (typeof onUnauthorizedCallback === 'function') {
            onUnauthorizedCallback();
        }
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
