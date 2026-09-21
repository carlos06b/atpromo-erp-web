import { useEffect, useState } from "react";
import { apiFetch } from "../api";
import { useAuth } from "../context/AuthContext";
import Layout from "../components/Layout";
import ConfirmDeleteDialog from "../components/ConfirmDeleteDialog";

const UF_OPTIONS = [
  "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA",
  "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN",
  "RS", "RO", "RR", "SC", "SP", "SE", "TO",
];

function formatCnpj(value) {
  const digits = value.replace(/\D/g, "").slice(0, 14);
  let formatted = digits;
  if (digits.length > 12) {
    formatted = digits.replace(/(\d{2})(\d{3})(\d{3})(\d{4})(\d{1,2})/, "$1.$2.$3/$4-$5");
  } else if (digits.length > 8) {
    formatted = digits.replace(/(\d{2})(\d{3})(\d{3})(\d{1,4})/, "$1.$2.$3/$4");
  } else if (digits.length > 5) {
    formatted = digits.replace(/(\d{2})(\d{3})(\d{1,3})/, "$1.$2.$3");
  } else if (digits.length > 2) {
    formatted = digits.replace(/(\d{2})(\d{1,3})/, "$1.$2");
  }
  return formatted;
}

const EMPTY_FORM = { id: null, nome: "", rede: "", uf: "", cnpj: "", active: true };

export default function Lojas() {
  const { token } = useAuth();
  const [lojas, setLojas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState("");
  const [saving, setSaving] = useState(false);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleteError, setDeleteError] = useState("");

  const [search, setSearch] = useState("");
  const [filterRede, setFilterRede] = useState("");
  const [filterUf, setFilterUf] = useState("");
  const [filterActive, setFilterActive] = useState("");

  async function loadLojas() {
    setLoading(true);
    setError("");
    try {
      const data = await apiFetch("/lojas", { token });
      setLojas(data || []);
    } catch (err) {
      setError("Não foi possível carregar as lojas.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadLojas();
  }, []);

  const redeSuggestions = Array.from(
    new Set(
      lojas
        .map((loja) => (loja.rede || "").trim())
        .filter((rede) => rede !== "")
    )
  ).sort((a, b) => a.localeCompare(b, "pt-BR"));

  const filteredLojas = lojas.filter((loja) => {
    const term = search.trim().toLowerCase();
    const matchesSearch =
      term === "" ||
      (loja.nome || "").toLowerCase().includes(term) ||
      (loja.rede || "").toLowerCase().includes(term) ||
      (loja.cnpj || "").toLowerCase().includes(term);

    const matchesRede = filterRede === "" || (loja.rede || "") === filterRede;
    const matchesUf = filterUf === "" || loja.uf === filterUf;
    const matchesActive =
      filterActive === "" || (filterActive === "true" ? loja.active : !loja.active);

    return matchesSearch && matchesRede && matchesUf && matchesActive;
  });

  function handleChange(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  function openNew() {
    setForm(EMPTY_FORM);
    setFormError("");
    setIsFormOpen(true);
  }

  function openEdit(loja) {
    setForm({
      id: loja.id,
      nome: loja.nome || "",
      rede: loja.rede || "",
      uf: loja.uf || "",
      cnpj: formatCnpj(loja.cnpj || ""),
      active: loja.active,
    });
    setFormError("");
    setIsFormOpen(true);
  }

  function closeForm() {
    setIsFormOpen(false);
    setForm(EMPTY_FORM);
    setFormError("");
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setFormError("");

    const payload = {
      ...form,
      nome: form.nome.trim(),
      rede: form.rede.trim().replace(/\s+/g, " ") || null,
      uf: form.uf || null,
      cnpj: form.cnpj.replace(/\D/g, "") ? form.cnpj : null,
    };

    try {
      if (form.id) {
        await apiFetch(`/lojas/${form.id}`, { method: "PUT", body: payload, token });
      } else {
        await apiFetch("/lojas", { method: "POST", body: payload, token });
      }

      closeForm();
      await loadLojas();
    } catch (err) {
      setFormError(err.message || "Não foi possível salvar a loja.");
    } finally {
      setSaving(false);
    }
  }

  function requestDelete(id) {
    setDeleteTarget(id);
  }

  async function confirmDelete() {
    setDeleteError("");
    try {
      await apiFetch(`/lojas/${deleteTarget}`, { method: "DELETE", token });
      setDeleteTarget(null);
      await loadLojas();
    } catch (err) {
      setDeleteTarget(null);
      setDeleteError(err.message || "Não foi possível excluir a loja.");
    }
  }

  return (
    <Layout title="Lojas">
      <div className="mb-6 flex items-center justify-between">
        <p className="text-sm text-neutral-500">
          {filteredLojas.length} de {lojas.length} loja{lojas.length !== 1 ? "s" : ""}
        </p>

        <button
          onClick={openNew}
          className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600"
        >
          + Nova loja
        </button>
      </div>

      <div className="mb-4 flex flex-wrap gap-3 rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Buscar por nome, rede ou CNPJ..."
          className="min-w-[220px] flex-1 rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
        />

        <select
          value={filterRede}
          onChange={(e) => setFilterRede(e.target.value)}
          className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
        >
          <option value="">Todas as redes</option>
          {redeSuggestions.map((rede) => (
            <option key={rede} value={rede}>
              {rede}
            </option>
          ))}
        </select>

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
          value={filterActive}
          onChange={(e) => setFilterActive(e.target.value)}
          className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
        >
          <option value="">Todos os status</option>
          <option value="true">Ativas</option>
          <option value="false">Inativas</option>
        </select>
      </div>

      {error && (
        <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>
      )}
      {deleteError && (
        <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{deleteError}</div>
      )}

      <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
        <table className="w-full text-left text-sm">
          <thead className="bg-black text-white">
            <tr>
              <th className="px-4 py-3 font-medium">Nome</th>
              <th className="px-4 py-3 font-medium">Rede</th>
              <th className="px-4 py-3 font-medium">UF</th>
              <th className="px-4 py-3 font-medium">CNPJ</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 font-medium text-right">Ações</th>
            </tr>
          </thead>

          <tbody className="divide-y divide-neutral-100">
            {loading ? (
              <tr>
                <td colSpan="6" className="px-4 py-8 text-center text-neutral-400">
                  Carregando...
                </td>
              </tr>
            ) : lojas.length === 0 ? (
              <tr>
                <td colSpan="6" className="px-4 py-8 text-center text-neutral-400">
                  Nenhuma loja cadastrada ainda.
                </td>
              </tr>
            ) : filteredLojas.length === 0 ? (
              <tr>
                <td colSpan="6" className="px-4 py-8 text-center text-neutral-400">
                  Nenhuma loja encontrada com esse filtro.
                </td>
              </tr>
            ) : (
              filteredLojas.map((loja) => (
                <tr key={loja.id} className="hover:bg-orange-50/40">
                  <td className="px-4 py-3 font-medium text-neutral-800">{loja.nome}</td>
                  <td className="px-4 py-3 text-neutral-600">{loja.rede || "-"}</td>
                  <td className="px-4 py-3 text-neutral-600">{loja.uf || "-"}</td>
                  <td className="px-4 py-3 text-neutral-600">{loja.cnpj ? formatCnpj(loja.cnpj) : "-"}</td>
                  <td className="px-4 py-3">
                    <span
                      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                        loja.active
                          ? "bg-orange-100 text-orange-700"
                          : "bg-neutral-100 text-neutral-500"
                      }`}
                    >
                      {loja.active ? "Ativa" : "Inativa"}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => openEdit(loja)}
                      className="mr-3 text-sm font-medium text-neutral-600 hover:text-orange-600"
                    >
                      Editar
                    </button>
                    <button
                      onClick={() => requestDelete(loja.id)}
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
          <div className="max-h-[90vh] w-full max-w-lg overflow-auto rounded-2xl bg-white p-6 shadow-2xl">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-black">
                {form.id ? "Editar loja" : "Nova loja"}
              </h2>
              <button onClick={closeForm} className="text-neutral-400 hover:text-black" type="button">
                ✕
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Nome</label>
                <input
                  value={form.nome}
                  onChange={(e) => handleChange("nome", e.target.value)}
                  placeholder="Ex: Assaí Boa Viagem"
                  required
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Rede</label>
                <input
                  value={form.rede}
                  onChange={(e) => handleChange("rede", e.target.value)}
                  list="rede-suggestions"
                  autoComplete="off"
                  placeholder="Ex: Assaí, Atacadão..."
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
                <datalist id="rede-suggestions">
                  {redeSuggestions.map((rede) => (
                    <option key={rede} value={rede} />
                  ))}
                </datalist>
              </div>

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
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
                  <label className="mb-1 block text-sm font-medium text-neutral-700">CNPJ</label>
                  <input
                    value={form.cnpj}
                    onChange={(e) => handleChange("cnpj", formatCnpj(e.target.value))}
                    placeholder="00.000.000/0000-00"
                    maxLength={18}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>
              </div>

              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="active"
                  checked={form.active}
                  onChange={(e) => handleChange("active", e.target.checked)}
                  className="h-4 w-4 rounded border-neutral-300 text-orange-500 focus:ring-orange-400"
                />
                <label htmlFor="active" className="text-sm font-medium text-neutral-700">
                  Loja ativa
                </label>
              </div>

              {formError && (
                <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{formError}</div>
              )}

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
        itemLabel="esta loja"
      />
    </Layout>
  );
}
