import { useEffect, useState } from "react";
import { apiFetch } from "../api";
import { useAuth } from "../context/AuthContext";
import Layout from "../components/Layout";
import PromoterAutocomplete from "../components/PromoterAutocomplete";

const STATUS_STYLES = {
    PENDENTE: "bg-orange-100 text-orange-700",
    APROVADO: "bg-green-100 text-green-700",
    REJEITADO: "bg-red-50 text-red-600",
};

const STATUS_LABELS = {
    PENDENTE: "Pendente",
    APROVADO: "Aprovada",
    REJEITADO: "Rejeitada",
};

const EMPTY_CREATE_FORM = { promoterId: "", type: "", amount: "", message: "" };

function formatMoney(value) {
    if (value == null) return "-";
    return Number(value).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

function formatDateTime(value) {
    if (!value) return "-";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "-";
    return date.toLocaleString("pt-BR", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
    });
}

function toIso(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, "0");
    const d = String(date.getDate()).padStart(2, "0");
    return `${y}-${m}-${d}`;
}

function getMonthRange(monthOffset = 0) {
    const now = new Date();
    const first = new Date(now.getFullYear(), now.getMonth() + monthOffset, 1);
    const last = new Date(now.getFullYear(), now.getMonth() + monthOffset + 1, 0);
    return { start: toIso(first), end: toIso(last) };
}

function StatTile({ label, value, tone = "neutral" }) {
    const toneClass =
        tone === "good"
            ? "text-green-600"
            : tone === "bad"
            ? "text-red-600"
            : tone === "warn"
            ? "text-orange-600"
            : "text-black";

    return (
        <div className="rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
            <p className="text-sm text-neutral-500">{label}</p>
            <p className={`text-2xl font-semibold ${toneClass}`}>{value}</p>
        </div>
    );
}

export default function Solicitacoes() {
    const { token, jobTittle } = useAuth();
    const normalizedJob = (jobTittle || "").trim().toUpperCase();
    const isRh = normalizedJob === "RH";
    const isFinance = normalizedJob === "FINANCEIRO";

    const [requests, setRequests] = useState([]);
    const [promoters, setPromoters] = useState([]);
    const [types, setTypes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const [view, setView] = useState("pendentes");
    const initialRange = getMonthRange(0);
    const [rangeStart, setRangeStart] = useState(initialRange.start);
    const [rangeEnd, setRangeEnd] = useState(initialRange.end);

    const [selectedIds, setSelectedIds] = useState(new Set());
    const [rowActionError, setRowActionError] = useState({});
    const [rowActionLoading, setRowActionLoading] = useState({});
    const [bulkLoading, setBulkLoading] = useState(false);
    const [bulkError, setBulkError] = useState("");

    const [isCreateOpen, setIsCreateOpen] = useState(false);
    const [createForm, setCreateForm] = useState(EMPTY_CREATE_FORM);
    const [createError, setCreateError] = useState("");
    const [saving, setSaving] = useState(false);

    const [detailsRequest, setDetailsRequest] = useState(null);

    const [isPixOpen, setIsPixOpen] = useState(false);
    const [pixDate, setPixDate] = useState("");
    const [pixResults, setPixResults] = useState(null);
    const [pixLoading, setPixLoading] = useState(false);
    const [pixError, setPixError] = useState("");

    const [cancelTarget, setCancelTarget] = useState(null);
    const [cancelPassword, setCancelPassword] = useState("");
    const [cancelError, setCancelError] = useState("");
    const [cancelChecking, setCancelChecking] = useState(false);

    async function loadAll() {
        setLoading(true);
        setError("");

        try {
            const [requestsData, promotersData, typesData] = await Promise.all([
                apiFetch("/requests", { token }),
                apiFetch("/promoters", { token }),
                apiFetch("/requests/types", { token }),
            ]);

            setRequests(requestsData || []);
            setPromoters(promotersData || []);
            setTypes(typesData || []);
        } catch (err) {
            setError(err.message || "Não foi possível carregar as solicitações.");
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadAll();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    function applyPreset(preset) {
        const range = preset === "mes-passado" ? getMonthRange(-1) : getMonthRange(0);
        setRangeStart(range.start);
        setRangeEnd(range.end);
    }

    const displayedRequests = (() => {
        if (view === "pendentes") {
            return requests.filter((r) => r.status === "PENDENTE");
        }
        if (view === "periodo") {
            return requests.filter((r) => {
                if (!r.date) return false;
                const day = r.date.slice(0, 10);
                return day >= rangeStart && day <= rangeEnd;
            });
        }
        return requests;
    })();

    const summary = {
        total: requests.length,
        pendentes: requests.filter((r) => r.status === "PENDENTE").length,
        aprovadas: requests.filter((r) => r.status === "APROVADO").length,
        rejeitadas: requests.filter((r) => r.status === "REJEITADO").length,
    };

    function openCreateForm() {
        setCreateForm(EMPTY_CREATE_FORM);
        setCreateError("");
        setIsCreateOpen(true);
    }

    function closeCreateForm() {
        setIsCreateOpen(false);
    }

    function handleCreateChange(field, value) {
        setCreateForm((prev) => ({ ...prev, [field]: value }));
    }

    async function handleCreateSubmit(event) {
        event.preventDefault();
        setCreateError("");

        if (!createForm.promoterId) {
            setCreateError("Selecione um promotor.");
            return;
        }
        if (!createForm.type) {
            setCreateError("Selecione o tipo da solicitação.");
            return;
        }
        if (!createForm.amount || Number(createForm.amount) <= 0) {
            setCreateError("Informe um valor válido.");
            return;
        }
        if (!createForm.message.trim()) {
            setCreateError("Informe a mensagem com os dados para pagamento (ex: chave Pix).");
            return;
        }

        setSaving(true);

        try {
            await apiFetch("/requests", {
                method: "POST",
                body: {
                    promoterId: Number(createForm.promoterId),
                    type: createForm.type,
                    amount: Number(createForm.amount),
                    message: createForm.message.trim(),
                },
                token,
            });

            setIsCreateOpen(false);
            setCreateForm(EMPTY_CREATE_FORM);
            await loadAll();
        } catch (err) {
            setCreateError(err.message || "Não foi possível criar a solicitação.");
        } finally {
            setSaving(false);
        }
    }

    async function runAction(id, action) {
        setRowActionError((prev) => ({ ...prev, [id]: "" }));
        setRowActionLoading((prev) => ({ ...prev, [id]: true }));

        try {
            await apiFetch(`/requests/${id}/${action}`, { method: "PUT", token });
            await loadAll();
            setSelectedIds((prev) => {
                const next = new Set(prev);
                next.delete(id);
                return next;
            });
        } catch (err) {
            setRowActionError((prev) => ({ ...prev, [id]: err.message || "Não foi possível concluir a ação." }));
        } finally {
            setRowActionLoading((prev) => ({ ...prev, [id]: false }));
        }
    }

    function toggleSelected(id) {
        setSelectedIds((prev) => {
            const next = new Set(prev);
            if (next.has(id)) {
                next.delete(id);
            } else {
                next.add(id);
            }
            return next;
        });
    }

    async function bulkApprove() {
        setBulkError("");
        setBulkLoading(true);

        const ids = Array.from(selectedIds);
        const failures = [];

        for (const id of ids) {
            try {
                await apiFetch(`/requests/${id}/approve`, { method: "PUT", token });
            } catch (err) {
                failures.push(`#${id}: ${err.message || "erro"}`);
            }
        }

        setBulkLoading(false);
        setSelectedIds(new Set());
        await loadAll();

        if (failures.length > 0) {
            setBulkError(`Algumas solicitações não puderam ser aprovadas: ${failures.join("; ")}`);
        }
    }

    function openCancelApproval(request) {
        setCancelTarget(request);
        setCancelPassword("");
        setCancelError("");
    }

    function closeCancelApproval() {
        setCancelTarget(null);
        setCancelPassword("");
        setCancelError("");
    }

    async function confirmCancelApproval(event) {
        event.preventDefault();
        setCancelError("");
        setCancelChecking(true);

        try {
            await apiFetch("/account/verify-password", {
                method: "POST",
                body: { password: cancelPassword },
                token,
            });
        } catch (err) {
            setCancelError("Senha incorreta. Tente novamente.");
            setCancelChecking(false);
            return;
        }

        try {
            await apiFetch(`/requests/${cancelTarget.id}/cancel-approval`, { method: "PUT", token });
            closeCancelApproval();
            await loadAll();
        } catch (err) {
            setCancelError(err.message || "Não foi possível cancelar a aprovação.");
        } finally {
            setCancelChecking(false);
        }
    }

    async function generatePixBatch() {
        setPixError("");

        if (!pixDate) {
            setPixError("Selecione a data de pagamento.");
            return;
        }

        setPixLoading(true);

        try {
            const data = await apiFetch(`/requests/pix-batch?paymentDate=${pixDate}`, { token });
            setPixResults(data);
        } catch (err) {
            setPixError(err.message || "Não foi possível gerar o lote de Pix.");
        } finally {
            setPixLoading(false);
        }
    }

    const pendingViewSelectable = isFinance && view === "pendentes";

    return (
        <Layout title="Solicitações">
            {!isRh && !isFinance && (
                <div className="mb-4 rounded-lg border border-orange-200 bg-orange-50 px-4 py-3 text-sm text-orange-800">
                    Seu cargo atual ({jobTittle || "não definido"}) não tem permissão para criar ou aprovar
                    solicitações. Só usuários com cargo RH podem criar, e só usuários com cargo FINANCEIRO podem
                    aprovar, rejeitar ou reabrir.
                </div>
            )}

            <div className="mb-6 grid grid-cols-2 gap-4 sm:grid-cols-4">
                <StatTile label="Total" value={summary.total} />
                <StatTile label="Pendentes" value={summary.pendentes} tone="warn" />
                <StatTile label="Aprovadas" value={summary.aprovadas} tone="good" />
                <StatTile label="Rejeitadas" value={summary.rejeitadas} tone="bad" />
            </div>

            <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
                <div className="flex rounded-lg border border-neutral-300 bg-white p-1">
                    <button
                        onClick={() => setView("pendentes")}
                        className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                            view === "pendentes" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
                        }`}
                    >
                        Pendentes
                    </button>
                    <button
                        onClick={() => setView("todas")}
                        className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                            view === "todas" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
                        }`}
                    >
                        Todas
                    </button>
                    <button
                        onClick={() => setView("periodo")}
                        className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                            view === "periodo" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
                        }`}
                    >
                        Período
                    </button>
                </div>

                <div className="flex gap-3">
                    {isFinance && (
                        <button
                            onClick={() => {
                                setIsPixOpen(true);
                                setPixResults(null);
                                setPixError("");
                            }}
                            className="rounded-lg border border-neutral-300 bg-white px-4 py-2 text-sm font-semibold text-neutral-700 transition-colors hover:border-orange-400 hover:text-orange-600"
                        >
                            Exportar lote Pix
                        </button>
                    )}
                    <button
                        onClick={openCreateForm}
                        disabled={!isRh}
                        title={!isRh ? "Apenas usuários de RH podem criar solicitações." : undefined}
                        className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:cursor-not-allowed disabled:opacity-40"
                    >
                        + Nova solicitação
                    </button>
                </div>
            </div>

            {view === "periodo" && (
                <div className="mb-6 flex flex-wrap items-end gap-3 rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
                    <div className="flex gap-2">
                        <button
                            onClick={() => applyPreset("mes-atual")}
                            className="rounded-lg border border-neutral-300 px-3 py-1.5 text-sm font-medium text-neutral-600 hover:border-orange-400 hover:text-orange-600"
                        >
                            Este mês
                        </button>
                        <button
                            onClick={() => applyPreset("mes-passado")}
                            className="rounded-lg border border-neutral-300 px-3 py-1.5 text-sm font-medium text-neutral-600 hover:border-orange-400 hover:text-orange-600"
                        >
                            Mês passado
                        </button>
                    </div>
                    <div>
                        <label className="mb-1 block text-sm font-medium text-neutral-700">De</label>
                        <input
                            type="date"
                            value={rangeStart}
                            onChange={(e) => setRangeStart(e.target.value)}
                            className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                        />
                    </div>
                    <div>
                        <label className="mb-1 block text-sm font-medium text-neutral-700">Até</label>
                        <input
                            type="date"
                            value={rangeEnd}
                            onChange={(e) => setRangeEnd(e.target.value)}
                            className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                        />
                    </div>
                </div>
            )}

            {error && <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}

            {pendingViewSelectable && selectedIds.size > 0 && (
                <div className="mb-4 flex flex-wrap items-center gap-3 rounded-lg border border-orange-200 bg-orange-50 px-4 py-3">
                    <span className="text-sm text-orange-800">{selectedIds.size} selecionada(s)</span>
                    <button
                        onClick={bulkApprove}
                        disabled={bulkLoading}
                        className="rounded-lg bg-green-600 px-3 py-1.5 text-sm font-semibold text-white transition-colors hover:bg-green-700 disabled:opacity-60"
                    >
                        {bulkLoading ? "Aprovando..." : "Aprovar selecionadas"}
                    </button>
                    {bulkError && <span className="text-sm text-red-600">{bulkError}</span>}
                </div>
            )}

            {loading ? (
                <p className="text-sm text-neutral-500">Carregando solicitações...</p>
            ) : (
                <div className="overflow-x-auto rounded-xl border border-neutral-200 bg-white shadow-sm">
                    <table className="w-full text-left text-sm">
                        <thead className="border-b border-neutral-200 bg-neutral-50 text-neutral-500">
                            <tr>
                                {pendingViewSelectable && <th className="px-4 py-3 font-medium"></th>}
                                <th className="px-4 py-3 font-medium">Data</th>
                                <th className="px-4 py-3 font-medium">Promotor</th>
                                <th className="px-4 py-3 font-medium">Tipo</th>
                                <th className="px-4 py-3 font-medium">Valor</th>
                                <th className="px-4 py-3 font-medium">Solicitado por</th>
                                <th className="px-4 py-3 font-medium">Status</th>
                                <th className="px-4 py-3 font-medium">Ações</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-neutral-100">
                            {displayedRequests.length === 0 ? (
                                <tr>
                                    <td
                                        colSpan={pendingViewSelectable ? 8 : 7}
                                        className="px-4 py-6 text-center text-neutral-400"
                                    >
                                        Nenhuma solicitação encontrada.
                                    </td>
                                </tr>
                            ) : (
                                displayedRequests.map((r) => (
                                    <tr key={r.id} className="align-top">
                                        {pendingViewSelectable && (
                                            <td className="px-4 py-3">
                                                {r.status === "PENDENTE" && (
                                                    <input
                                                        type="checkbox"
                                                        checked={selectedIds.has(r.id)}
                                                        onChange={() => toggleSelected(r.id)}
                                                        className="h-4 w-4 rounded border-neutral-300 text-orange-500 focus:ring-orange-400"
                                                    />
                                                )}
                                            </td>
                                        )}
                                        <td className="px-4 py-3 text-neutral-600">{formatDateTime(r.date)}</td>
                                        <td className="px-4 py-3 font-medium text-black">{r.promoterName}</td>
                                        <td className="px-4 py-3 text-neutral-600">{r.typeLabel}</td>
                                        <td className="px-4 py-3 text-neutral-600">{formatMoney(r.amount)}</td>
                                        <td className="px-4 py-3 text-neutral-600">{r.rhUserName || "-"}</td>
                                        <td className="px-4 py-3">
                                            <span
                                                className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                                                    STATUS_STYLES[r.status] || "bg-neutral-100 text-neutral-600"
                                                }`}
                                            >
                                                {STATUS_LABELS[r.status] || r.status}
                                            </span>
                                        </td>
                                        <td className="px-4 py-3">
                                            <div className="flex flex-wrap items-center gap-2">
                                                {isFinance && r.status === "PENDENTE" && (
                                                    <>
                                                        <button
                                                            onClick={() => runAction(r.id, "approve")}
                                                            disabled={rowActionLoading[r.id]}
                                                            className="rounded-lg bg-green-600 px-3 py-1.5 text-xs font-semibold text-white transition-colors hover:bg-green-700 disabled:opacity-60"
                                                        >
                                                            Aprovar
                                                        </button>
                                                        <button
                                                            onClick={() => runAction(r.id, "reject")}
                                                            disabled={rowActionLoading[r.id]}
                                                            className="rounded-lg border border-red-300 px-3 py-1.5 text-xs font-semibold text-red-600 transition-colors hover:bg-red-50 disabled:opacity-60"
                                                        >
                                                            Rejeitar
                                                        </button>
                                                    </>
                                                )}

                                                {isFinance && r.status === "REJEITADO" && (
                                                    <button
                                                        onClick={() => runAction(r.id, "reopen")}
                                                        disabled={rowActionLoading[r.id]}
                                                        className="rounded-lg border border-neutral-300 px-3 py-1.5 text-xs font-semibold text-neutral-700 transition-colors hover:border-orange-400 hover:text-orange-600 disabled:opacity-60"
                                                    >
                                                        Reabrir
                                                    </button>
                                                )}

                                                {isFinance && r.status === "APROVADO" && (
                                                    <button
                                                        onClick={() => openCancelApproval(r)}
                                                        disabled={rowActionLoading[r.id]}
                                                        className="rounded-lg border border-red-300 px-3 py-1.5 text-xs font-semibold text-red-600 transition-colors hover:bg-red-50 disabled:opacity-60"
                                                        title="Use se essa solicitação foi aprovada por engano"
                                                    >
                                                        Cancelar aprovação
                                                    </button>
                                                )}

                                                <button
                                                    onClick={() => setDetailsRequest(r)}
                                                    className="text-xs font-medium text-neutral-500 hover:text-orange-600 hover:underline"
                                                >
                                                    Detalhes
                                                </button>
                                            </div>

                                            {rowActionError[r.id] && (
                                                <p className="mt-1 text-xs text-red-600">{rowActionError[r.id]}</p>
                                            )}
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            )}

            {isCreateOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
                    <div className="max-h-[90vh] w-full max-w-lg overflow-auto rounded-2xl bg-white p-6 shadow-2xl">
                        <div className="mb-6 flex items-center justify-between">
                            <h2 className="text-lg font-semibold text-black">Nova solicitação</h2>
                            <button onClick={closeCreateForm} className="text-neutral-400 hover:text-black" type="button">
                                ✕
                            </button>
                        </div>

                        <form onSubmit={handleCreateSubmit} className="space-y-4">
                            <div>
                                <label className="mb-1 block text-sm font-medium text-neutral-700">Promotor</label>
                                <PromoterAutocomplete
                                    promoters={promoters}
                                    value={createForm.promoterId}
                                    onChange={(value) => handleCreateChange("promoterId", value)}
                                />
                            </div>

                            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                                <div>
                                    <label className="mb-1 block text-sm font-medium text-neutral-700">Tipo</label>
                                    <select
                                        value={createForm.type}
                                        onChange={(e) => handleCreateChange("type", e.target.value)}
                                        className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                    >
                                        <option value="">Selecione...</option>
                                        {types.map((t) => (
                                            <option key={t.value} value={t.value}>
                                                {t.label}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                <div>
                                    <label className="mb-1 block text-sm font-medium text-neutral-700">Valor</label>
                                    <input
                                        type="number"
                                        step="0.01"
                                        value={createForm.amount}
                                        onChange={(e) => handleCreateChange("amount", e.target.value)}
                                        className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="mb-1 block text-sm font-medium text-neutral-700">
                                    Mensagem / dados para pagamento (ex: chave Pix)
                                </label>
                                <textarea
                                    value={createForm.message}
                                    onChange={(e) => handleCreateChange("message", e.target.value)}
                                    rows={3}
                                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                />
                            </div>

                            {createError && (
                                <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{createError}</div>
                            )}

                            <div className="flex justify-end gap-3 border-t border-neutral-100 pt-4">
                                <button
                                    type="button"
                                    onClick={closeCreateForm}
                                    className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100"
                                >
                                    Cancelar
                                </button>
                                <button
                                    type="submit"
                                    disabled={saving}
                                    className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-60"
                                >
                                    {saving ? "Enviando..." : "Criar solicitação"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {detailsRequest && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
                    <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl">
                        <div className="mb-4 flex items-center justify-between">
                            <h2 className="text-lg font-semibold text-black">Detalhes da solicitação</h2>
                            <button
                                onClick={() => setDetailsRequest(null)}
                                className="text-neutral-400 hover:text-black"
                                type="button"
                            >
                                ✕
                            </button>
                        </div>

                        <div className="space-y-3 text-sm">
                            <div>
                                <p className="text-neutral-500">Promotor</p>
                                <p className="font-medium text-black">{detailsRequest.promoterName}</p>
                            </div>
                            <div className="grid grid-cols-2 gap-3">
                                <div>
                                    <p className="text-neutral-500">Tipo</p>
                                    <p className="font-medium text-black">{detailsRequest.typeLabel}</p>
                                </div>
                                <div>
                                    <p className="text-neutral-500">Valor</p>
                                    <p className="font-medium text-black">{formatMoney(detailsRequest.amount)}</p>
                                </div>
                            </div>
                            <div>
                                <p className="text-neutral-500">Mensagem / dados para pagamento</p>
                                <p className="whitespace-pre-wrap font-medium text-black">{detailsRequest.message}</p>
                            </div>
                            <div className="rounded-lg border border-orange-200 bg-orange-50 px-3 py-2">
                                <p className="text-xs font-medium text-orange-700">Chave Pix cadastrada do promotor</p>
                                {detailsRequest.promoterPix ? (
                                    <p className="font-medium text-black">
                                        {detailsRequest.promoterPixType ? `${detailsRequest.promoterPixType}: ` : ""}
                                        {detailsRequest.promoterPix}
                                    </p>
                                ) : (
                                    <p className="text-sm text-orange-800">
                                        Esse promotor não tem chave Pix cadastrada no cadastro dele.
                                    </p>
                                )}
                            </div>
                            <div className="grid grid-cols-2 gap-3">
                                <div>
                                    <p className="text-neutral-500">Status</p>
                                    <span
                                        className={`inline-block rounded-full px-2.5 py-1 text-xs font-semibold ${
                                            STATUS_STYLES[detailsRequest.status] || "bg-neutral-100 text-neutral-600"
                                        }`}
                                    >
                                        {STATUS_LABELS[detailsRequest.status] || detailsRequest.status}
                                    </span>
                                </div>
                                <div>
                                    <p className="text-neutral-500">Data</p>
                                    <p className="font-medium text-black">{formatDateTime(detailsRequest.date)}</p>
                                </div>
                            </div>
                            <div>
                                <p className="text-neutral-500">Solicitado por</p>
                                <p className="font-medium text-black">{detailsRequest.rhUserName || "-"}</p>
                            </div>
                            {detailsRequest.status !== "PENDENTE" && (
                                <div>
                                    <p className="text-neutral-500">
                                        {detailsRequest.status === "APROVADO" ? "Aprovado por" : "Rejeitado por"}
                                    </p>
                                    <p className="font-medium text-black">{detailsRequest.finUserName || "-"}</p>
                                </div>
                            )}
                        </div>

                        <div className="mt-6 flex justify-end">
                            <button
                                onClick={() => setDetailsRequest(null)}
                                className="rounded-lg bg-neutral-900 px-4 py-2 text-sm font-semibold text-white hover:bg-black"
                            >
                                Fechar
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {cancelTarget && (
                <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/60 p-4">
                    <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl">
                        <h2 className="mb-2 text-lg font-semibold text-black">Cancelar aprovação</h2>

                        <div className="mb-4 rounded-lg border border-orange-200 bg-orange-50 px-3 py-2 text-sm text-orange-800">
                            Isso vai voltar a solicitação de <strong>{cancelTarget.promoterName}</strong> (
                            {formatMoney(cancelTarget.amount)}) para pendente e remover o lançamento que apareceu nos
                            Relatórios quando ela foi aprovada. Use só se aprovou por engano.
                        </div>

                        <form onSubmit={confirmCancelApproval} className="space-y-4">
                            <div>
                                <label className="mb-1 block text-sm font-medium text-neutral-700">
                                    Digite sua senha para confirmar
                                </label>
                                <input
                                    type="password"
                                    value={cancelPassword}
                                    onChange={(e) => setCancelPassword(e.target.value)}
                                    required
                                    autoFocus
                                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                />
                            </div>

                            {cancelError && (
                                <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{cancelError}</div>
                            )}

                            <div className="flex justify-end gap-3 pt-2">
                                <button
                                    type="button"
                                    onClick={closeCancelApproval}
                                    className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100"
                                >
                                    Voltar
                                </button>
                                <button
                                    type="submit"
                                    disabled={cancelChecking}
                                    className="rounded-lg bg-red-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-red-700 disabled:opacity-60"
                                >
                                    {cancelChecking ? "Verificando..." : "Cancelar aprovação"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {isPixOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
                    <div className="max-h-[90vh] w-full max-w-2xl overflow-auto rounded-2xl bg-white p-6 shadow-2xl">
                        <div className="mb-4 flex items-center justify-between">
                            <h2 className="text-lg font-semibold text-black">Exportar lote de Pix</h2>
                            <button
                                onClick={() => setIsPixOpen(false)}
                                className="text-neutral-400 hover:text-black"
                                type="button"
                            >
                                ✕
                            </button>
                        </div>

                        <p className="mb-4 text-sm text-neutral-500">
                            Traz as solicitações pendentes cujo promotor já tem chave Pix cadastrada, prontas para
                            pagamento na data escolhida.
                        </p>

                        <div className="mb-4 flex flex-wrap items-end gap-3">
                            <div>
                                <label className="mb-1 block text-sm font-medium text-neutral-700">Data de pagamento</label>
                                <input
                                    type="date"
                                    value={pixDate}
                                    onChange={(e) => setPixDate(e.target.value)}
                                    className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                />
                            </div>
                            <button
                                onClick={generatePixBatch}
                                disabled={pixLoading}
                                className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-60"
                            >
                                {pixLoading ? "Gerando..." : "Gerar lote Pix"}
                            </button>
                        </div>

                        {pixError && (
                            <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{pixError}</div>
                        )}

                        {pixResults !== null && (
                            <div className="overflow-x-auto rounded-xl border border-neutral-200">
                                <table className="w-full text-left text-sm">
                                    <thead className="border-b border-neutral-200 bg-neutral-50 text-neutral-500">
                                        <tr>
                                            <th className="px-4 py-3 font-medium">Promotor</th>
                                            <th className="px-4 py-3 font-medium">Tipo Pix</th>
                                            <th className="px-4 py-3 font-medium">Chave Pix</th>
                                            <th className="px-4 py-3 font-medium">Valor</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-neutral-100">
                                        {pixResults.length === 0 ? (
                                            <tr>
                                                <td colSpan={4} className="px-4 py-6 text-center text-neutral-400">
                                                    Nenhuma solicitação pendente com chave Pix cadastrada.
                                                </td>
                                            </tr>
                                        ) : (
                                            pixResults.map((payment, index) => (
                                                <tr key={index}>
                                                    <td className="px-4 py-3 font-medium text-black">{payment.name}</td>
                                                    <td className="px-4 py-3 text-neutral-600">{payment.pixType || "-"}</td>
                                                    <td className="px-4 py-3 text-neutral-600">{payment.pix || "-"}</td>
                                                    <td className="px-4 py-3 text-neutral-600">{formatMoney(payment.amount)}</td>
                                                </tr>
                                            ))
                                        )}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                </div>
            )}
        </Layout>
    );
}
