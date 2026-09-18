import { useCallback, useEffect, useState } from "react";
import Layout from "../components/Layout";
import { apiFetch } from "../api";
import { useAuth } from "../context/AuthContext";
import { IconCheckDot, IconLock, IconSpinner } from "../components/icons";

function formatDateTime(value) {
    if (!value) return "-";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleString("pt-BR", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
    });
}

export default function RedefinicoesSenha() {
    const { token } = useAuth();
    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [resolvingId, setResolvingId] = useState(null);

    const loadRequests = useCallback(async () => {
        setLoading(true);
        setError("");
        try {
            const data = await apiFetch("/password-reset-requests", { token });
            setRequests(data || []);
        } catch (err) {
            setError(err.message || "Não foi possível carregar as solicitações.");
        } finally {
            setLoading(false);
        }
    }, [token]);

    useEffect(() => {
        loadRequests();
    }, [loadRequests]);

    async function handleResolve(id) {
        setResolvingId(id);
        setError("");
        try {
            await apiFetch(`/password-reset-requests/${id}/resolve`, { method: "PUT", token });
            setRequests((prev) => prev.filter((request) => request.id !== id));
        } catch (err) {
            setError(err.message || "Não foi possível marcar como atendida.");
        } finally {
            setResolvingId(null);
        }
    }

    return (
        <Layout title="Redefinições de senha">
            <div className="mb-6 flex items-start gap-3 rounded-xl border border-neutral-200 bg-white p-4">
                <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full bg-orange-50 text-orange-500">
                    <IconLock className="h-5 w-5" />
                </div>
                <p className="text-sm text-neutral-600">
                    Aqui aparecem as pessoas que clicaram em "Esqueci minha senha" na tela de login.
                    Entre em contato com cada uma, redefina a senha dela e marque a solicitação como atendida
                    pra tirar ela dessa lista.
                </p>
            </div>

            {error && (
                <div className="mb-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{error}</div>
            )}

            <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white">
                {loading ? (
                    <div className="flex items-center justify-center gap-2 p-10 text-sm text-neutral-500">
                        <IconSpinner className="h-4 w-4 animate-spin" />
                        Carregando...
                    </div>
                ) : requests.length === 0 ? (
                    <div className="p-10 text-center text-sm text-neutral-500">
                        Nenhuma solicitação pendente no momento.
                    </div>
                ) : (
                    <table className="w-full text-left text-sm">
                        <thead className="border-b border-neutral-200 bg-neutral-50 text-xs uppercase tracking-wide text-neutral-500">
                            <tr>
                                <th className="px-5 py-3 font-medium">Email</th>
                                <th className="px-5 py-3 font-medium">Solicitado em</th>
                                <th className="px-5 py-3 font-medium text-right">Ação</th>
                            </tr>
                        </thead>
                        <tbody>
                            {requests.map((request) => (
                                <tr key={request.id} className="border-b border-neutral-100 last:border-0">
                                    <td className="px-5 py-3 text-neutral-800">{request.email}</td>
                                    <td className="px-5 py-3 text-neutral-500">{formatDateTime(request.requestedAt)}</td>
                                    <td className="px-5 py-3 text-right">
                                        <button
                                            onClick={() => handleResolve(request.id)}
                                            disabled={resolvingId === request.id}
                                            className="inline-flex items-center gap-1.5 rounded-lg bg-black px-3 py-1.5 text-xs font-semibold text-white transition-colors hover:bg-neutral-800 disabled:opacity-60"
                                        >
                                            {resolvingId === request.id ? (
                                                <IconSpinner className="h-3.5 w-3.5 animate-spin" />
                                            ) : (
                                                <IconCheckDot className="h-3.5 w-3.5" />
                                            )}
                                            Marcar como atendida
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>
        </Layout>
    );
}
