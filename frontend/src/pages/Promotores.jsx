import { useEffect, useState } from "react";
import { apiFetch } from "../api";
import { useAuth } from "../context/AuthContext";
import Layout from "../components/Layout";

const UF_OPTIONS = [
    "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA",
    "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN",
    "RS", "RO", "RR", "SC", "SP", "SE", "TO",
];

const PIX_TYPE_OPTIONS = [
    { value: "CPF", label: "CPF" },
    { value: "CNPJ", label: "CNPJ" },
    { value: "EMAIL", label: "Email" },
    { value: "CELULAR", label: "Celular" },
    { value: "ALEATORIA", label: "Chave aleatória" },
];

const COMPANY_LINK_OPTIONS = ["AT", "TEJO"];

const EMPTY_FORM = {
    id: null,
    name: "",
    cpf: "",
    phone: "",
    uf: "",
    city: "",
    dateBirth: "",
    active: true,
    salary: "",
    type: "",
    pix: "",
    pixType: "",
    companyLink: "",
};

export default function Promotores() {
    const { token } = useAuth();
    const [promoters, setPromoters] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [form, setForm] = useState(EMPTY_FORM);
    const [saving, setSaving] = useState(false);
    const [isFormOpen, setIsFormOpen] = useState(false);

    async function loadPromoters() {
        setLoading(true);
        setError("");

        try {
            const data = await apiFetch("/promoters", { token });
            setPromoters(data);
        } catch (err) {
            setError("Não foi possível carregar os promotores.");
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadPromoters();
    }, []);

    function handleChange(field, value) {
        setForm((prev) => ({ ...prev, [field]: value }));
    }

    function openNew() {
        setForm(EMPTY_FORM);
        setIsFormOpen(true);
    }

    function openEdit(promoter) {
        setForm({
            id: promoter.id,
            name: promoter.name || "",
            cpf: promoter.cpf || "",
            phone: promoter.phone || "",
            uf: promoter.uf || "",
            city: promoter.city || "",
            dateBirth: promoter.dateBirth || "",
            active: promoter.active,
            salary: promoter.salary ?? "",
            type: promoter.type || "",
            pix: promoter.pix || "",
            pixType: promoter.pixType || "",
            companyLink: promoter.companyLink || "",
        });
        setIsFormOpen(true);
    }

    function closeForm() {
        setIsFormOpen(false);
        setForm(EMPTY_FORM);
    }

    async function handleSubmit(event) {
        event.preventDefault();
        setSaving(true);
        setError("");

        const payload = {
            ...form,
            salary: form.salary === "" ? null : Number(form.salary),
            dateBirth: form.dateBirth === "" ? null : form.dateBirth,
        };

        try {
            if (form.id) {
                await apiFetch(`/promoters/${form.id}`, {
                    method: "PUT",
                    body: payload,
                    token,
                });
            } else {
                await apiFetch("/promoters", {
                    method: "POST",
                    body: payload,
                    token,
                });
            }

            closeForm();
            await loadPromoters();
        } catch (err) {
            setError("Não foi possível salvar o promotor.");
        } finally {
            setSaving(false);
        }
    }

    async function handleDelete(id) {
        if (!window.confirm("Tem certeza que quer excluir esse promotor?")) {
            return;
        }

        try {
            await apiFetch(`/promoters/${id}`, { method: "DELETE", token });
            await loadPromoters();
        } catch (err) {
            setError("Não foi possível excluir o promotor.");
        }
    }

    return (
        <Layout title="Promotores">
            <div className="mb-6 flex items-center justify-between">
                <p className="text-sm text-neutral-500">
                    {promoters.length} promotor{promoters.length !== 1 ? "es" : ""} cadastrado{promoters.length !== 1 ? "s" : ""}
                </p>

                <button
                    onClick={openNew}
                    className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600"
                >
                    + Novo promotor
                </button>
            </div>

            {error && (
                <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>
            )}

            <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
                <table className="w-full text-left text-sm">
                    <thead className="bg-black text-white">
                    <tr>
                        <th className="px-4 py-3 font-medium">Nome</th>
                        <th className="px-4 py-3 font-medium">CPF</th>
                        <th className="px-4 py-3 font-medium">Tipo</th>
                        <th className="px-4 py-3 font-medium">Vínculo</th>
                        <th className="px-4 py-3 font-medium">Salário/Base</th>
                        <th className="px-4 py-3 font-medium">Status</th>
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
                    ) : promoters.length === 0 ? (
                        <tr>
                            <td colSpan="7" className="px-4 py-8 text-center text-neutral-400">
                                Nenhum promotor cadastrado ainda.
                            </td>
                        </tr>
                    ) : (
                        promoters.map((promoter) => (
                            <tr key={promoter.id} className="hover:bg-orange-50/40">
                                <td className="px-4 py-3 font-medium text-neutral-800">{promoter.name}</td>
                                <td className="px-4 py-3 text-neutral-600">{promoter.cpf || "-"}</td>
                                <td className="px-4 py-3 text-neutral-600">{promoter.type || "-"}</td>
                                <td className="px-4 py-3 text-neutral-600">{promoter.companyLink || "-"}</td>
                                <td className="px-4 py-3 text-neutral-600">
                                    {promoter.salary != null
                                        ? Number(promoter.salary).toLocaleString("pt-BR", { style: "currency", currency: "BRL" })
                                        : "-"}
                                </td>
                                <td className="px-4 py-3">
                    <span
                        className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                            promoter.active
                                ? "bg-orange-100 text-orange-700"
                                : "bg-neutral-100 text-neutral-500"
                        }`}
                    >
                      {promoter.active ? "Ativo" : "Inativo"}
                    </span>
                                </td>
                                <td className="px-4 py-3 text-right">
                                    <button
                                        onClick={() => openEdit(promoter)}
                                        className="mr-3 text-sm font-medium text-neutral-600 hover:text-orange-600"
                                    >
                                        Editar
                                    </button>
                                    <button
                                        onClick={() => handleDelete(promoter.id)}
                                        className="text-sm font-medium text-neutral-600 hover:text-red-600"
                                    >
                                        Excluir
                                    </button>
                                </td>
                            </tr>
                        ))
                    )}
                    </tbody>
                </table>
            </div>

            {isFormOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
                    <div className="max-h-[90vh] w-full max-w-2xl overflow-auto rounded-2xl bg-white p-6 shadow-2xl">
                        <div className="mb-6 flex items-center justify-between">
                            <h2 className="text-lg font-semibold text-black">
                                {form.id ? "Editar promotor" : "Novo promotor"}
                            </h2>
                            <button onClick={closeForm} className="text-neutral-400 hover:text-black" type="button">
                                ✕
                            </button>
                        </div>

                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                                <div>
                                    <label className="mb-1 block text-sm font-medium text-neutral-700">Nome</label>
                                    <input
                                        value={form.name}
                                        onChange={(e) => handleChange("name", e.target.value)}
                                        required
                                        className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                    />
                                </div>

                                <div>
                                    <label className="mb-1 block text-sm font-medium text-neutral-700">CPF</label>
                                    <input
                                        value={form.cpf}
                                        onChange={(e) => handleChange("cpf", e.target.value)}
                                        className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                    />
                                </div>

                                <div>
                                    <label className="mb-1 block text-sm font-medium text-neutral-700">Telefone</label>
                                    <input
                                        value={form.phone}
                                        onChange={(e) => handleChange("phone", e.target.value)}
                                        className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                    />
                                </div>

                                <div>
                                    <label className="mb-1 block text-sm font-medium text-neutral-700">Data de nascimento</label>
                                    <input
                                        type="date"
                                        value={form.dateBirth}
                                        onChange={(e) => handleChange("dateBirth", e.target.value)}
                                        className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                    />
                                </div>

                                <div>
                                    <label className="mb-1 block text-sm font-medium text-neutral-700">UF</label>
                                    <select
                                        value={form.uf}
                                        onChange={(e) => handleChange("uf", e.target.value)}
                                        className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                    >
                                        <option value="">Selecione...</option>
                                        {UF_OPTIONS.map((uf) => (
                                            <option key={uf} value={uf}>
                                                {uf}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                <div>
                                    <label className="mb-1 block text-sm font-medium text-neutral-700">Cidade</label>
                                    <input
                                        value={form.city}
                                        onChange={(e) => handleChange("city", e.target.value)}
                                        className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                    />
                                </div>

                                <div>
                                    <label className="mb-1 block text-sm font-medium text-neutral-700">Salário/Base</label>
                                    <input
                                        type="number"
                                        step="0.01"
                                        value={form.salary}
                                        onChange={(e) => handleChange("salary", e.target.value)}
                                        className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                    />
                                </div>

                                <div>
                                    <label className="mb-1 block text-sm font-medium text-neutral-700">Tipo</label>
                                    <input
                                        value={form.type}
                                        onChange={(e) => handleChange("type", e.target.value)}
                                        placeholder="CLT, MEI..."
                                        className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                    />
                                </div>

                                <div>
                                    <label className="mb-1 block text-sm font-medium text-neutral-700">Pix</label>
                                    <input
                                        value={form.pix}
                                        onChange={(e) => handleChange("pix", e.target.value)}
                                        className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                    />
                                </div>

                                <div>
                                    <label className="mb-1 block text-sm font-medium text-neutral-700">Tipo do Pix</label>
                                    <select
                                        value={form.pixType}
                                        onChange={(e) => handleChange("pixType", e.target.value)}
                                        className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                    >
                                        <option value="">Selecione...</option>
                                        {PIX_TYPE_OPTIONS.map((opt) => (
                                            <option key={opt.value} value={opt.value}>
                                                {opt.label}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                <div>
                                    <label className="mb-1 block text-sm font-medium text-neutral-700">Vínculo (empresa)</label>
                                    <select
                                        value={form.companyLink}
                                        onChange={(e) => handleChange("companyLink", e.target.value)}
                                        className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                    >
                                        <option value="">Selecione...</option>
                                        {COMPANY_LINK_OPTIONS.map((opt) => (
                                            <option key={opt} value={opt}>
                                                {opt}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                <div className="flex items-center gap-2 pt-6">
                                    <input
                                        type="checkbox"
                                        id="active"
                                        checked={form.active}
                                        onChange={(e) => handleChange("active", e.target.checked)}
                                        className="h-4 w-4 rounded border-neutral-300 text-orange-500 focus:ring-orange-400"
                                    />
                                    <label htmlFor="active" className="text-sm font-medium text-neutral-700">
                                        Promotor ativo
                                    </label>
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
        </Layout>
    );
}