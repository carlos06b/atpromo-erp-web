// Em dev local, usa localhost. No Railway (ou qualquer deploy), defina a
// variável de ambiente VITE_API_BASE_URL apontando pro backend publicado
// (ex: https://seu-backend.up.railway.app/api) antes de gerar o build do frontend.
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

// ACHADO B1 (corrigido): apiFetch não tinha nenhum tratamento especial pra
// 401 - um token expirado/inválido só gerava um Error comum, e cada tela
// decidia (ou não) o que fazer com isso. Nenhuma tela fazia logout.
//
// AuthContext (o único lugar que sabe como deslogar de verdade - limpar
// localStorage/sessionStorage e resetar o estado React) se registra aqui
// via setUnauthorizedHandler(). Esse módulo não é um componente React e
// não pode usar useAuth() diretamente, por isso o registro por função.
let unauthorizedHandler = null;

export function setUnauthorizedHandler(handler) {
    unauthorizedHandler = handler;
}

export async function apiFetch(path, { method = "GET", body, token, deleteTicket } = {}) {
    const headers = { "Content-Type": "application/json" };

    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }

    if (deleteTicket) {
        headers["X-Delete-Confirmation"] = deleteTicket;
    }

    const response = await fetch(`${API_BASE_URL}${path}`, {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined,
    });

    if (!response.ok) {
        let message = `Erro ${response.status}`;

        try {
            const data = await response.json();
            message = data.message || message;
        } catch (e) {
        }

        // ACHADO B1: 401 agora desloga automaticamente (se houver um
        // AuthProvider montado e registrado) - ver setUnauthorizedHandler
        // acima. O ProtectedRoute já redireciona pra /login sempre que o
        // token fica vazio, então não precisamos navegar manualmente aqui.
        if (response.status === 401 && unauthorizedHandler) {
            unauthorizedHandler();
        }

        throw new Error(message);
    }

    const text = await response.text();

    if (!text) {
        return null;
    }

    return JSON.parse(text);
}