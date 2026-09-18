// Em dev local, usa localhost. No Railway (ou qualquer deploy), defina a
// variável de ambiente VITE_API_BASE_URL apontando pro backend publicado
// (ex: https://seu-backend.up.railway.app/api) antes de gerar o build do frontend.
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

export async function apiFetch(path, { method = "GET", body, token } = {}) {
    const headers = { "Content-Type": "application/json" };

    if (token) {
        headers.Authorization = `Bearer ${token}`;
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

        throw new Error(message);
    }

    const text = await response.text();

    if (!text) {
        return null;
    }

    return JSON.parse(text);
}