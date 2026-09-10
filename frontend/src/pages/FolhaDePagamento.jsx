import { useEffect, useState } from "react";
import { apiFetch } from "../api";
import { useAuth } from "../context/AuthContext";
import Layout from "../components/Layout";
import ConfirmDeleteDialog from "../components/ConfirmDeleteDialog";
import PromoterAutocomplete from "../components/PromoterAutocomplete";

const LAUNCH_TYPE_OPTIONS = [
    { value: "DESCONTO", label: "Desconto" },
    { value: "BONUS", label: "Bônus" },
];

const LAUNCH_TYPE_STYLES = {
    DESCONTO: "bg-red-50 text-red-600",
    BONUS: "bg-green-100 text-green-700",
};

const PROMOTER_TYPE_FILTER_OPTIONS = [
    { value: "TODOS", label: "Todos os tipos" },
    { value: "CLT", label: "CLT" },
    { value: "MEI", label: "MEI" },
    { value: "FERISTA", label: "Ferista" },
];

const PAYROLL_STATUS_STYLES = {
    OK: "bg-green-100 text-green-700",
    ATENCAO: "bg-orange-100 text-orange-700",
};

const EMPTY_LAUNCH_FORM = {
    id: null,
    promoterId: "",
    type: "DESCONTO",
    amount: "",
    date: "",
    status: "",
    description: "",
};

function formatMoney(value) {
    if (value == null) return "-";
    return Number(value).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

function formatDate(value) {
    if (!value) return "-";
    const [year, month, day] = value.split("-");
    return `${day}/${month}/${year}`;
}

export default function FolhaDePagamento() {
    const { token } = useAuth();
    const [view, setView] = useState("lancamentos");

    const [promoters, setPromoters] = useState([]);
    const [launches, setLaunches] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const [form, setForm] = useState(EMPTY_LAUNCH_FORM);
    const [saving, setSaving] = useState(false);
    const [isFormOpen, setIsFormOpen] = useState(false);
    const [deleteTarget, setDeleteTarget] = useState(null);

    const [search, setSearch] = useState("");
    const [filterType, setFilterType] = useState("");
    const [filterPromoterId, setFilterPromoterId] = useState("");

    const [rangeStart, setRangeStart] = useState("");
    const [rangeEnd, setRangeEnd] = useState("");
    const [rangeType, setRangeType] = useState("TODOS");
    const [payrollLines, setPayrollLines] = useState(null);
    const [payrollLoading, setPayrollLoading] = useState(false);
    const [payrollError, setPayrollError] = useState("");

    const [pixDate, setPixDate] = useState("");
    const [pixResults, setPixResults] = useState(null);
    const [pixLoading, setPixLoading] = useState(false);
    const [pixError, setPixError] = useState("");

    async function loadData() {
        setLoading(true);
        setError("");

        try {
            const [promoterData, launchData] = await Promise.all([
                apiFetch("/promoters", { token }),
                apiFetch("/finance-promoter", { token }),
            ]);
            setPromoters(promoterData);
            setLaunches(launchData);
        } catch (err) {
            setError("Não foi possível carregar os dados.");
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadData();
    }, []);

    const promotersById = Object.fromEntries(promoters.map((p) => [p.id, p]));

    const filteredLaunches = launches.filter((launch) => {
        const promoter = promotersById[launch.idPromoter];
        const term = search.trim().toLowerCase();

        const matchesSearch =
            term === "" ||
            (promoter?.name || "").toLowerCase().includes(term) ||
            (launch.description || "").toLowerCase().includes(term);

        const matchesType = filterType === "" || launch.type === filterType;
        const matchesPromoter = filterPromoterId === "" || String(launch.idPromoter) === filterPromoterId;

        return matchesSearch && matchesType && matchesPromoter;
    });

    function handleChange(field, value) {
        setForm((prev) => ({ ...prev, [field]: value }));
    }

    function openNew() {
        setForm(EMPTY_LAUNCH_FORM);
        setIsFormOpen(true);
    }

    function openEdit(launch) {
        setForm({
            id: launch.id,
            promoterId: String(launch.idPromoter ?? ""),
            type: launch.type || "DESCONTO",
            amount: launch.amount ?? "",
            date: launch.date || "",
            status: launch.status || "",
            description: launch.description || "",
        });
        setIsFormOpen(true);
    }

    function closeForm() {
        setIsFormOpen(false);
        setForm(EMPTY_LAUNCH_FORM);
    }

    async function handleSubmit(event) {
        event.preventDefault();
        setError("");

        if (!form.promoterId) {
            setError("Selecione um promotor.");
            return;
        }

        setSaving(true);

        const payload = {
            idPromoter: form.promoterId === "" ? null : Number(form.promoterId),
            type: form.type,
            amount: form.amount === "" ? null : Number(form.amount),
            date: form.date === "" ? null : form.date,
            status: form.status === "" ? null : form.status,
            description: form.description === "" ? null : form.description,
        };

        try {
            if (form.id) {
                await apiFetch(`/finance-promoter/${form.id}`, { method: "PUT", body: payload, token });
            } else {
                await apiFetch("/finance-promoter", { method: "POST", body: payload, token });
            }

            closeForm();
            await loadData();
        } catch (err) {
            setError("Não foi possível salvar o lançamento.");
        } finally {
            setSaving(false);
        }
    }

    function requestDelete(id) {
        setDeleteTarget(id);
    }

    async function confirmDelete() {
        try {
            await apiFetch(`/finance-promoter/${deleteTarget}`, { method: "DELETE", token });
            setDeleteTarget(null);
            await loadData();
        } catch (err) {
            setDeleteTarget(null);
            setError("Não foi possível excluir o lançamento.");
        }
    }

    async function generatePayroll() {
        setPayrollError("");

        if (!rangeStart || !rangeEnd) {
            setPayrollError("Selecione a data inicial e a data final.");
            return;
        }

        if (rangeStart > rangeEnd) {
            setPayrollError("A data inicial não pode ser maior que a data final.");
            return;
        }

        setPayrollLoading(true);
        try {
            const data = await apiFetch(
                `/payroll/lines?start=${rangeStart}&end=${rangeEnd}&type=${rangeType}`,
                { token }
            );
            setPayrollLines(data);
        } catch (err) {
            setPayrollError("Não foi possível gerar o fechamento.");
        } finally {
            setPayrollLoading(false);
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
            const data = await apiFetch(`/payroll/pix-batch?paymentDate=${pixDate}`, { token });
            setPixResults(data);
        } catch (err) {
            setPixError("Não foi possível gerar o lote de Pix.");
        } finally {
            setPixLoading(false);
        }
    }

    const payrollTotal = (payrollLines || []).reduce(
        (sum, line) => sum + (line.netAmount != null ? Number(line.netAmount) : 0),
        0
    );

    return (
        <Layout title="Folha de pagamento">
            <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
                <div className="flex rounded-lg border border-neutral-300 bg-white p-1">
                    <button
                        type="button"
                        onClick={() => setView("lancamentos")}
                        className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                            view === "lancamentos" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
                        }`}
                    >
                        Lançamentos
                    </button>
                    <button
                        type="button"
                        onClick={() => setView("fechamento")}
                        className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                            view === "fechamento" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
                        }`}
                    >
                        Fechamento
                    </button>
                    <button
                        type="button"
                        onClick={() => setView("pix")}
                        className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                            view === "pix" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
                        }`}
                    >
                        Lote Pix
                    </button>
                </div>

                {view === "lancamentos" && (
                    <button
                        onClick={openNew}
                        disabled={promoters.length === 0}
                        className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-50"
                    >
                        + Novo lançamento
                    </button>
                )}
            </div>

            {error && (
                <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>
            )}

            {view === "lancamentos" && (
                <>
                    <p className="mb-3 text-sm text-neutral-500">
                        {filteredLaunches.length} de {launches.length} lançamento{launches.length !== 1 ? "s" : ""}
                    </p>

                    <div className="mb-4 flex flex-wrap gap-3 rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
                        <input
                            type="text"
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            placeholder="Buscar por promotor ou descrição..."
                            className="min-w-[220px] flex-1 rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                        />

                        <select
                            value={filterPromoterId}
                            onChange={(e) => setFilterPromoterId(e.target.value)}
                            className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                        >
                            <option value="">Todos os promotores</option>
                            {promoters.map((p) => (
                                <option key={p.id} value={p.id}>
                                    {p.name}
                                </option>
                            ))}
                        </select>

                        <select
                            value={filterType}
                            onChange={(e) => setFilterType(e.target.value)}
                            className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                        >
                            <option value="">Todos os tipos</option>
                            {LAUNCH_TYPE_OPTIONS.map((opt) => (
                                <option key={opt.value} value={opt.value}>
                                    {opt.label}
                                </option>
                            ))}
                        </select>
                    </div>

                    {promoters.length === 0 && !loading && (
                        <div className="mb-4 rounded-lg bg-neutral-100 px-4 py-3 text-sm text-neutral-600">
                            Cadastre pelo menos um promotor antes de lançar um desconto ou bônus.
                        </div>
                    )}

                    <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
                        <table className="w-full text-left text-sm">
                            <thead className="bg-black text-white">
                            <tr>
                                <th className="px-4 py-3 font-medium">Promotor</th>
                                <th className="px-4 py-3 font-medium">Tipo</th>
                                <th className="px-4 py-3 font-medium">Valor</th>
                                <th className="px-4 py-3 font-medium">Data</th>
                                <th className="px-4 py-3 font-medium">Status</th>
                                <th className="px-4 py-3 font-medium">Descrição</th>
                                <th className="px-4 py-3 font-medium text-right">Ações</th>
                            </tr>
                            </thead>

                            <tbody className="divide-y divide-neutral-100">
                            {loading ? (
                                <tr>
                                    <td colSpan="7" className="px-4 py-8 text-center text-neutral-400">
                                        Carregando...
                                    </td>
                                </tr>
                            ) : launches.length === 0 ? (
                                <tr>
                                    <td colSpan="7" className="px-4 py-8 text-center text-neutral-400">
                                        Nenhum lançamento registrado ainda.
                                    </td>
                                </tr>
                            ) : filteredLaunches.length === 0 ? (
                                <tr>
                                    <td colSpan="7" className="px-4 py-8 text-center text-neutral-400">
                                        Nenhum lançamento encontrado com esse filtro.
                                    </td>
                                </tr>
                            ) : (
                                filteredLaunches.map((launch) => {
                                    const promoter = promotersById[launch.idPromoter];

                                    return (
                                        <tr key={launch.id} className="hover:bg-orange-50/40">
                                            <td className="px-4 py-3 font-medium text-neutral-800">
                                                {promoter ? promoter.name : `Promotor #${launch.idPromoter}`}
                                            </td>
                                            <td className="px-4 py-3">
                          <span
                              className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                                  LAUNCH_TYPE_STYLES[launch.type] || "bg-neutral-100 text-neutral-500"
                              }`}
                          >
                            {LAUNCH_TYPE_OPTIONS.find((o) => o.value === launch.type)?.label || launch.type || "-"}
                          </span>
                                            </td>
                                            <td className="px-4 py-3 text-neutral-600">{formatMoney(launch.amount)}</td>
                                            <td className="px-4 py-3 text-neutral-600">{formatDate(launch.date)}</td>
                                            <td className="px-4 py-3 text-neutral-600">{launch.status || "-"}</td>
                                            <td className="px-4 py-3 text-neutral-600">{launch.description || "-"}</td>
                                            <td className="px-4 py-3 text-right">
                                                <button
                                                    onClick={() => openEdit(launch)}
                                                    className="mr-3 text-sm font-medium text-neutral-600 hover:text-orange-600"
                                                >
                                                    Editar
                                                </button>
                                                <button
                                                    onClick={() => requestDelete(launch.id)}
                                                    className="text-sm font-medium text-neutral-600 hover:text-red-600"
                                                >
                                                    Excluir
                                                </button>
                                            </td>
                                        </tr>
                                    );
                                })
                            )}
                            </tbody>
                        </table>
                    </div>
                </>
            )}

            {view === "fechamento" && (
                <div>
                    <div className="mb-4 flex flex-wrap items-end gap-3 rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
                        <div>
                            <label className="mb-1 block text-sm font-medium text-neutral-700">Data inicial</label>
                            <input
                                type="date"
                                value={rangeStart}
                                onChange={(e) => setRangeStart(e.target.value)}
                                className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                            />
                        </div>

                        <div>
                            <label className="mb-1 block text-sm font-medium text-neutral-700">Data final</label>
                            <input
                                type="date"
                                value={rangeEnd}
                                onChange={(e) => setRangeEnd(e.target.value)}
                                className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                            />
                        </div>

                        <div>
                            <label className="mb-1 block text-sm font-medium text-neutral-700">Tipo de promotor</label>
                            <select
                                value={rangeType}
                                onChange={(e) => setRangeType(e.target.value)}
                                className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                            >
                                {PROMOTER_TYPE_FILTER_OPTIONS.map((opt) => (
                                    <option key={opt.value} value={opt.value}>
                                        {opt.label}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <button
                            type="button"
                            onClick={generatePayroll}
                            disabled={payrollLoading}
                            className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-60"
                        >
                            {payrollLoading ? "Gerando..." : "Gerar fechamento"}
                        </button>
                    </div>

                    {payrollError && (
                        <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{payrollError}</div>
                    )}

                    {payrollLines === null ? (
                        <div className="rounded-xl border border-neutral-200 bg-white px-4 py-8 text-center text-sm text-neutral-400 shadow-sm">
                            Escolha o período e clique em "Gerar fechamento" para ver os valores.
                        </div>
                    ) : (
                        <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
                            <table className="w-full text-left text-sm">
                                <thead className="bg-black text-white">
                                <tr>
                                    <th className="px-4 py-3 font-medium">Promotor</th>
                                    <th className="px-4 py-3 font-medium">Tipo</th>
                                    <th className="px-4 py-3 font-medium">Salário base</th>
                                    <th className="px-4 py-3 font-medium">Bônus</th>
                                    <th className="px-4 py-3 font-medium">Descontos</th>
                                    <th className="px-4 py-3 font-medium">Líquido</th>
                                    <th className="px-4 py-3 font-medium">Status</th>
                                    <th className="px-4 py-3 font-medium">Observação</th>
                                </tr>
                                </thead>

                                <tbody className="divide-y divide-neutral-100">
                                {payrollLines.length === 0 ? (
                                    <tr>
                                        <td colSpan="8" className="px-4 py-8 text-center text-neutral-400">
                                            Nenhum promotor ativo encontrado para esse filtro.
                                        </td>
                                    </tr>
                                ) : (
                                    payrollLines.map((line) => (
                                        <tr key={line.promoterId} className="hover:bg-orange-50/40">
                                            <td className="px-4 py-3 font-medium text-neutral-800">{line.promoterName}</td>
                                            <td className="px-4 py-3 text-neutral-600">{line.promoterType || "-"}</td>
                                            <td className="px-4 py-3 text-neutral-600">{formatMoney(line.baseSalary)}</td>
                                            <td className="px-4 py-3 text-green-700">{formatMoney(line.bonuses)}</td>
                                            <td className="px-4 py-3 text-red-600">{formatMoney(line.discounts)}</td>
                                            <td className="px-4 py-3 font-semibold text-neutral-800">{formatMoney(line.netAmount)}</td>
                                            <td className="px-4 py-3">
                          <span
                              className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                                  PAYROLL_STATUS_STYLES[line.status] || "bg-neutral-100 text-neutral-500"
                              }`}
                          >
                            {line.status === "OK" ? "OK" : "Atenção"}
                          </span>
                                            </td>
                                            <td className="px-4 py-3 text-neutral-600">{line.observation || "-"}</td>
                                        </tr>
                                    ))
                                )}
                                </tbody>

                                {payrollLines.length > 0 && (
                                    <tfoot>
                                    <tr className="border-t-2 border-neutral-200 bg-neutral-50">
                                        <td colSpan="5" className="px-4 py-3 text-right text-sm font-semibold text-neutral-700">
                                            Total líquido
                                        </td>
                                        <td colSpan="3" className="px-4 py-3 text-sm font-bold text-black">
                                            {formatMoney(payrollTotal)}
                                        </td>
                                    </tr>
                                    </tfoot>
                                )}
                            </table>
                        </div>
                    )}
                </div>
            )}

            {view === "pix" && (
                <div>
                    <div className="mb-4 flex flex-wrap items-end gap-3 rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
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
                            type="button"
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
                        <div className="mb-4 rounded-lg bg-neutral-100 px-4 py-3 text-sm text-neutral-600">
                            Essa lista traz só os promotores MEI ativos com chave Pix cadastrada. Confira o valor de cada
                            um na aba "Fechamento" antes de pagar.
                        </div>
                    )}

                    {pixResults === null ? (
                        <div className="rounded-xl border border-neutral-200 bg-white px-4 py-8 text-center text-sm text-neutral-400 shadow-sm">
                            Escolha a data de pagamento e clique em "Gerar lote Pix" para ver a lista.
                        </div>
                    ) : (
                        <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
                            <table className="w-full text-left text-sm">
                                <thead className="bg-black text-white">
                                <tr>
                                    <th className="px-4 py-3 font-medium">Nome</th>
                                    <th className="px-4 py-3 font-medium">CPF</th>
                                    <th className="px-4 py-3 font-medium">Tipo de chave</th>
                                    <th className="px-4 py-3 font-medium">Chave Pix</th>
                                    <th className="px-4 py-3 font-medium">Data de pagamento</th>
                                </tr>
                                </thead>

                                <tbody className="divide-y divide-neutral-100">
                                {pixResults.length === 0 ? (
                                    <tr>
                                        <td colSpan="5" className="px-4 py-8 text-center text-neutral-400">
                                            Nenhum promotor MEI com chave Pix cadastrada.
                                        </td>
                                    </tr>
                                ) : (
                                    pixResults.map((payment, index) => (
                                        <tr key={index} className="hover:bg-orange-50/40">
                                            <td className="px-4 py-3 font-medium text-neutral-800">{payment.name}</td>
                                            <td className="px-4 py-3 text-neutral-600">{payment.document || "-"}</td>
                                            <td className="px-4 py-3 text-neutral-600">{payment.pixType || "-"}</td>
                                            <td className="px-4 py-3 text-neutral-600">{payment.pix || "-"}</td>
                                            <td className="px-4 py-3 text-neutral-600">{formatDate(payment.paymentDate)}</td>
                                        </tr>
                                    ))
                                )}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            )}

            {isFormOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
                    <div className="max-h-[90vh] w-full max-w-lg overflow-auto rounded-2xl bg-white p-6 shadow-2xl">
                        <div className="mb-6 flex items-center justify-between">
                            <h2 className="text-lg font-semibold text-black">
                                {form.id ? "Editar lançamento" : "Novo lançamento"}
                            </h2>
                            <button onClick={closeForm} className="text-neutral-400 hover:text-black" type="button">
                                ✕
                            </button>
                        </div>

                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div>
                                <label className="mb-1 block text-sm font-medium text-neutral-700">Promotor</label>
                                <PromoterAutocomplete
                                    promoters={promoters}
                                    value={form.promoterId}
                                    onChange={(id) => handleChange("promoterId", id)}
                                />
                            </div>

                            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                                <div>
                                    <label className="mb-1 block text-sm font-medium text-neutral-700">Tipo</label>
                                    <select
                                        value={form.type}
                                        onChange={(e) => handleChange("type", e.target.value)}
                                        className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                    >
                                        {LAUNCH_TYPE_OPTIONS.map((opt) => (
                                            <option key={opt.value} value={opt.value}>
                                                {opt.label}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                <div>
                                    <label className="mb-1 block text-sm font-medium text-neutral-700">Valor</label>
                                    <input
                                        type="number"
                                        step="0.01"
                                        value={form.amount}
                                        onChange={(e) => handleChange("amount", e.target.value)}
                                        required
                                        className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                    />
                                </div>

                                <div>
                                    <label className="mb-1 block text-sm font-medium text-neutral-700">Data</label>
                                    <input
                                        type="date"
                                        value={form.date}
                                        onChange={(e) => handleChange("date", e.target.value)}
                                        required
                                        className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                    />
                                </div>

                                <div>
                                    <label className="mb-1 block text-sm font-medium text-neutral-700">Status (opcional)</label>
                                    <input
                                        value={form.status}
                                        onChange={(e) => handleChange("status", e.target.value)}
                                        className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                    />
                                </div>

                                <div className="sm:col-span-2">
                                    <label className="mb-1 block text-sm font-medium text-neutral-700">Descrição</label>
                                    <input
                                        value={form.description}
                                        onChange={(e) => handleChange("description", e.target.value)}
                                        className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                    />
                                </div>
                            </div>

                            <div className="flex justify-end gap-3 border-t border-neutral-100 pt-4">
                                <button
                                    type="button"
                                    onClick={closeForm}
                                    className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100"
                                >
                                    Cancelar
                                </button>
                                <button
                                    type="submit"
                                    disabled={saving}
                                    className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-60"
                                >
                                    {saving ? "Salvando..." : form.id ? "Salvar alterações" : "Cadastrar"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            <ConfirmDeleteDialog
                open={deleteTarget !== null}
                onClose={() => setDeleteTarget(null)}
                onConfirmed={confirmDelete}
                itemLabel="este lançamento"
            />
        </Layout>
    );
}