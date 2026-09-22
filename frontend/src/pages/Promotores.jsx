import { useEffect, useState } from "react";
import { apiFetch } from "../api";
import { useAuth } from "../context/AuthContext";
import { isSupervisor } from "../access";
import Layout from "../components/Layout";
import ConfirmDeleteDialog from "../components/ConfirmDeleteDialog";
import CurrencyInput from "../components/CurrencyInput";

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

const TYPE_OPTIONS = ["CLT", "MEI", "FERISTA"];

function formatDate(value) {
  if (!value) return "-";
  const [year, month, day] = value.split("-");
  return `${day}/${month}/${year}`;
}

function formatCpf(value) {
  const digits = value.replace(/\D/g, "").slice(0, 11);
  let formatted = digits;
  if (digits.length > 9) {
    formatted = digits.replace(/(\d{3})(\d{3})(\d{3})(\d{1,2})/, "$1.$2.$3-$4");
  } else if (digits.length > 6) {
    formatted = digits.replace(/(\d{3})(\d{3})(\d{1,3})/, "$1.$2.$3");
  } else if (digits.length > 3) {
    formatted = digits.replace(/(\d{3})(\d{1,3})/, "$1.$2");
  }
  return formatted;
}

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
  lojaId: "",
  admissionDate: "",
  terminationDate: "",
};

export default function Promotores() {
  const { token, jobTittle } = useAuth();
  const readOnly = isSupervisor(jobTittle);
  const [promoters, setPromoters] = useState([]);
  const [lojas, setLojas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [detailsPromoter, setDetailsPromoter] = useState(null);

  const [search, setSearch] = useState("");
  const [filterUf, setFilterUf] = useState("");
  const [filterCompanyLink, setFilterCompanyLink] = useState("");
  const [filterActive, setFilterActive] = useState("");

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

  async function loadLojas() {
    try {
      const data = await apiFetch("/lojas", { token });
      setLojas(data || []);
    } catch (err) {
      // silencioso: se não carregar, o select de loja só fica vazio
    }
  }

  useEffect(() => {
    loadPromoters();
    loadLojas();
  }, []);

  const lojaNomeById = Object.fromEntries(lojas.map((loja) => [loja.id, loja.nome]));
  const lojasOrdenadas = [...lojas].sort((a, b) => (a.nome || "").localeCompare(b.nome || "", "pt-BR"));

  const filteredPromoters = promoters.filter((promoter) => {
    const term = search.trim().toLowerCase();
    const matchesSearch =
      term === "" ||
      (promoter.name || "").toLowerCase().includes(term) ||
      (promoter.cpf || "").toLowerCase().includes(term);

    const matchesUf = filterUf === "" || promoter.uf === filterUf;
    const matchesCompanyLink = filterCompanyLink === "" || promoter.companyLink === filterCompanyLink;
    const matchesActive =
      filterActive === "" || (filterActive === "true" ? promoter.active : !promoter.active);

    return matchesSearch && matchesUf && matchesCompanyLink && matchesActive;
  });

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
      cpf: formatCpf(promoter.cpf || ""),
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
      lojaId: promoter.lojaId ?? "",
      admissionDate: promoter.admissionDate || "",
      terminationDate: promoter.terminationDate || "",
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
      lojaId: form.lojaId === "" ? null : Number(form.lojaId),
      salary: form.salary === "" ? null : Number(form.salary),
      dateBirth: form.dateBirth === "" ? null : form.dateBirth,
      admissionDate: form.admissionDate === "" ? null : form.admissionDate,
      terminationDate: form.terminationDate === "" ? null : form.terminationDate,
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

  function requestDelete(id) {
    setDeleteTarget(id);
  }

  async function confirmDelete() {
    try {
      await apiFetch(`/promoters/${deleteTarget}`, { method: "DELETE", token });
      setDeleteTarget(null);
      await loadPromoters();
    } catch (err) {
      setDeleteTarget(null);
      setError("Não foi possível excluir o promotor.");
    }
  }

  return (
    <Layout title="Promotores">
      <div className="mb-6 flex items-center justify-between">
        <p className="text-sm text-neutral-500">
          {filteredPromoters.length} de {promoters.length} promotor{promoters.length !== 1 ? "es" : ""}
        </p>

        {!readOnly && (
          <button
            onClick={openNew}
            className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600"
          >
            + Novo promotor
          </button>
        )}
      </div>

      <div className="mb-4 flex flex-wrap gap-3 rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Buscar por nome ou CPF..."
          className="min-w-[220px] flex-1 rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
        />

        <select
          value={filterUf}
          onChange={(e) => setFilterUf(e.target.value)}
          className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
        >
          <option value="">Todas as UFs</option>
          {UF_OPTIONS.map((uf) => (
            <option key={uf} value={uf}>
              {uf}
            </option>
          ))}
        </select>

        <select
          value={filterCompanyLink}
          onChange={(e) => setFilterCompanyLink(e.target.value)}
          className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
        >
          <option value="">Todos os vínculos</option>
          {COMPANY_LINK_OPTIONS.map((opt) => (
            <option key={opt} value={opt}>
              {opt}
            </option>
          ))}
        </select>

        <select
          value={filterActive}
          onChange={(e) => setFilterActive(e.target.value)}
          className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
        >
          <option value="">Todos os status</option>
          <option value="true">Ativos</option>
          <option value="false">Inativos</option>
        </select>
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
              <th className="px-4 py-3 font-medium">Loja</th>
              {!readOnly && <th className="px-4 py-3 font-medium">Salário/Base</th>}
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 font-medium text-right">Ações</th>
            </tr>
          </thead>

          <tbody className="divide-y divide-neutral-100">
            {loading ? (
              <tr>
                <td colSpan={readOnly ? 7 : 8} className="px-4 py-8 text-center text-neutral-400">
                  Carregando...
                </td>
              </tr>
            ) : promoters.length === 0 ? (
              <tr>
                <td colSpan={readOnly ? 7 : 8} className="px-4 py-8 text-center text-neutral-400">
                  Nenhum promotor cadastrado ainda.
                </td>
              </tr>
            ) : filteredPromoters.length === 0 ? (
              <tr>
                <td colSpan={readOnly ? 7 : 8} className="px-4 py-8 text-center text-neutral-400">
                  Nenhum promotor encontrado com esse filtro.
                </td>
              </tr>
            ) : (
              filteredPromoters.map((promoter) => (
                <tr key={promoter.id} className="hover:bg-orange-50/40">
                  <td className="px-4 py-3 font-medium text-neutral-800">{promoter.name}</td>
                  <td className="px-4 py-3 text-neutral-600">{promoter.cpf || "-"}</td>
                  <td className="px-4 py-3 text-neutral-600">{promoter.type || "-"}</td>
                  <td className="px-4 py-3 text-neutral-600">{promoter.companyLink || "-"}</td>
                  <td className="px-4 py-3 text-neutral-600">{lojaNomeById[promoter.lojaId] || "-"}</td>
                  {!readOnly && (
                    <td className="px-4 py-3 text-neutral-600">
                      {promoter.salary != null
                        ? Number(promoter.salary).toLocaleString("pt-BR", { style: "currency", currency: "BRL" })
                        : "-"}
                    </td>
                  )}
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
                      onClick={() => setDetailsPromoter(promoter)}
                      className={`text-sm font-medium text-neutral-500 hover:text-orange-600 ${!readOnly ? "mr-3" : ""}`}
                    >
                      Detalhes
                    </button>
                    {!readOnly && (
                      <>
                        <button
                          onClick={() => openEdit(promoter)}
                          className="mr-3 text-sm font-medium text-neutral-600 hover:text-orange-600"
                        >
                          Editar
                        </button>
                        <button
                          onClick={() => requestDelete(promoter.id)}
                          className="text-sm font-medium text-neutral-600 hover:text-red-600"
                        >
                          Excluir
                        </button>
                      </>
                    )}
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
                    onChange={(e) => handleChange("cpf", formatCpf(e.target.value))}
                    placeholder="000.000.000-00"
                    maxLength={14}
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
                  <CurrencyInput
                    value={form.salary}
                    onChange={(val) => handleChange("salary", val)}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Tipo</label>
                  <select
                    value={form.type}
                    onChange={(e) => handleChange("type", e.target.value)}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  >
                    <option value="">Selecione...</option>
                    {TYPE_OPTIONS.map((opt) => (
                      <option key={opt} value={opt}>
                        {opt}
                      </option>
                    ))}
                  </select>
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

                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Loja</label>
                  <select
                    value={form.lojaId}
                    onChange={(e) => handleChange("lojaId", e.target.value)}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  >
                    <option value="">Selecione...</option>
                    {lojasOrdenadas.map((loja) => (
                      <option key={loja.id} value={loja.id}>
                        {loja.nome}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Data de admissão</label>
                  <input
                    type="date"
                    value={form.admissionDate}
                    onChange={(e) => handleChange("admissionDate", e.target.value)}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Data de desligamento</label>
                  <input
                    type="date"
                    value={form.terminationDate}
                    onChange={(e) => handleChange("terminationDate", e.target.value)}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
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

      {detailsPromoter && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="max-h-[90vh] w-full max-w-lg overflow-auto rounded-2xl bg-white p-6 shadow-2xl">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-black">Detalhes do promotor</h2>
              <button
                onClick={() => setDetailsPromoter(null)}
                className="text-neutral-400 hover:text-black"
                type="button"
              >
                ✕
              </button>
            </div>

            <div className="space-y-4 text-sm">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <p className="text-neutral-500">Nome</p>
                  <p className="font-medium text-black">{detailsPromoter.name || "-"}</p>
                </div>
                <div>
                  <p className="text-neutral-500">CPF</p>
                  <p className="font-medium text-black">{formatCpf(detailsPromoter.cpf || "")}</p>
                </div>
                <div>
                  <p className="text-neutral-500">Telefone</p>
                  <p className="font-medium text-black">{detailsPromoter.phone || "-"}</p>
                </div>
                <div>
                  <p className="text-neutral-500">Data de nascimento</p>
                  <p className="font-medium text-black">{formatDate(detailsPromoter.dateBirth)}</p>
                </div>
                <div>
                  <p className="text-neutral-500">UF</p>
                  <p className="font-medium text-black">{detailsPromoter.uf || "-"}</p>
                </div>
                <div>
                  <p className="text-neutral-500">Cidade</p>
                  <p className="font-medium text-black">{detailsPromoter.city || "-"}</p>
                </div>
                <div>
                  <p className="text-neutral-500">Tipo</p>
                  <p className="font-medium text-black">{detailsPromoter.type || "-"}</p>
                </div>
                {!readOnly && (
                  <div>
                    <p className="text-neutral-500">Salário/Base</p>
                    <p className="font-medium text-black">
                      {detailsPromoter.salary != null
                        ? Number(detailsPromoter.salary).toLocaleString("pt-BR", { style: "currency", currency: "BRL" })
                        : "-"}
                    </p>
                  </div>
                )}
                <div>
                  <p className="text-neutral-500">Chave Pix</p>
                  <p className="font-medium text-black">{detailsPromoter.pix || "-"}</p>
                </div>
                <div>
                  <p className="text-neutral-500">Tipo da chave Pix</p>
                  <p className="font-medium text-black">
                    {PIX_TYPE_OPTIONS.find((opt) => opt.value === detailsPromoter.pixType)?.label ||
                      detailsPromoter.pixType ||
                      "-"}
                  </p>
                </div>
                <div>
                  <p className="text-neutral-500">Vínculo (empresa)</p>
                  <p className="font-medium text-black">{detailsPromoter.companyLink || "-"}</p>
                </div>
                <div>
                  <p className="text-neutral-500">Loja</p>
                  <p className="font-medium text-black">{lojaNomeById[detailsPromoter.lojaId] || "-"}</p>
                </div>
                <div>
                  <p className="text-neutral-500">Data de admissão</p>
                  <p className="font-medium text-black">{formatDate(detailsPromoter.admissionDate)}</p>
                </div>
                <div>
                  <p className="text-neutral-500">Data de desligamento</p>
                  <p className="font-medium text-black">{formatDate(detailsPromoter.terminationDate)}</p>
                </div>
                <div>
                  <p className="text-neutral-500">Status</p>
                  <span
                    className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                      detailsPromoter.active
                        ? "bg-orange-100 text-orange-700"
                        : "bg-neutral-100 text-neutral-500"
                    }`}
                  >
                    {detailsPromoter.active ? "Ativo" : "Inativo"}
                  </span>
                </div>
              </div>
            </div>

            <div className="mt-6 flex justify-end gap-3 border-t border-neutral-100 pt-4">
              <button
                type="button"
                onClick={() => setDetailsPromoter(null)}
                className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100"
              >
                Fechar
              </button>
              {!readOnly && (
                <button
                  type="button"
                  onClick={() => {
                    const promoter = detailsPromoter;
                    setDetailsPromoter(null);
                    openEdit(promoter);
                  }}
                  className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600"
                >
                  Editar
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      <ConfirmDeleteDialog
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirmed={confirmDelete}
        itemLabel="este promotor"
      />
    </Layout>
  );
}
