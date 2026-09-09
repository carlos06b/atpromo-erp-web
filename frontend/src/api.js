const API_BASE_URL = "http://localhost:8080/api";

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
            // resposta sem corpo JSON, ignora
        }

        throw new Error(message);
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}