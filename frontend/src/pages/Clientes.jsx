import { useEffect, useState } from "react";
import { apiFetch } from "../api";
import { useAuth } from "../context/AuthContext";
import Layout from "../components/Layout";
import ConfirmDeleteDialog from "../components/ConfirmDeleteDialog";

const COMPANY_LINK_OPTIONS = ["AT", "TEJO"];

function companyLinkBadgeClasses(value) {
  if (value === "AT") return "bg-green-100 text-green-700";
  if (value === "TEJO") return "bg-blue-100 text-blue-700";
  return "bg-neutral-100 text-neutral-500";
}

const EMPTY_FORM = {
  id: null,
  name: "",
  corporateName: "",
  cnpj: "",
  phone: "",
  email: "",
  companyLink: "",
  active: true,
};

export default function Clientes() {
  const { token } = useAuth();
  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [detailsClient, setDetailsClient] = useState(null);

  const [search, setSearch] = useState("");
  const [filterCompanyLink, setFilterCompanyLink] = useState("");
  const [filterActive, setFilterActive] = useState("");

  async function loadClients() {
    setLoading(true);
    setError("");

    try {
      const data = await apiFetch("/clients", { token });
      setClients(data);
    } catch (err) {
      setError("Não foi possível carregar os clientes.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadClients();
  }, []);

  const filteredClients = clients.filter((client) => {
    const term = search.trim().toLowerCase();
    const matchesSearch =
      term === "" ||
      (client.name || "").toLowerCase().includes(term) ||
      (client.cnpj || "").toLowerCase().includes(term) ||
      (client.email || "").toLowerCase().includes(term);

    const matchesCompanyLink = filterCompanyLink === "" || client.companyLink === filterCompanyLink;
    const matchesActive =
      filterActive === "" || (filterActive === "true" ? client.active : !client.active);

    return matchesSearch && matchesCompanyLink && matchesActive;
  });

  function handleChange(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  function openNew() {
    setForm(EMPTY_FORM);
    setIsFormOpen(true);
  }

  function openEdit(client) {
    setForm({
      id: client.id,
      name: client.name || "",
      corporateName: client.corporateName || "",
      cnpj: client.cnpj || "",
      phone: client.phone || "",
      email: client.email || "",
      companyLink: client.companyLink || "",
      active: client.active,
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

    try {
      if (form.id) {
        await apiFetch(`/clients/${form.id}`, { method: "PUT", body: form, token });
      } else {
        await apiFetch("/clients", { method: "POST", body: form, token });
      }

      closeForm();
      await loadClients();
    } catch (err) {
      setError("Não foi possível salvar o cliente.");
    } finally {
      setSaving(false);
    }
  }

  function requestDelete(id) {
    setDeleteTarget(id);
  }

  async function confirmDelete() {
    try {
      await apiFetch(`/clients/${deleteTarget}`, { method: "DELETE", token });
      setDeleteTarget(null);
      await loadClients();
    } catch (err) {
      setDeleteTarget(null);
      setError("Não foi possível excluir o cliente.");
    }
  }

  return (
    <Layout title="Clientes">
      <div className="mb-6 flex items-center justify-between">
        <p className="text-sm text-neutral-500">
          {filteredClients.length} de {clients.length} cliente{clients.length !== 1 ? "s" : ""}
        </p>

        <button
          onClick={openNew}
          className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600"
        >
          + Novo cliente
        </button>
      </div>

      <div className="mb-4 flex flex-wrap gap-3 rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Buscar por nome, CNPJ ou email..."
          className="min-w-[220px] flex-1 rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
        />

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
              <th className="px-4 py-3 font-medium">Razão social</th>
              <th className="px-4 py-3 font-medium">CNPJ</th>
              <th className="px-4 py-3 font-medium">Vínculo</th>
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
            ) : clients.length === 0 ? (
              <tr>
                <td colSpan="6" className="px-4 py-8 text-center text-neutral-400">
                  Nenhum cliente cadastrado ainda.
                </td>
              </tr>
            ) : filteredClients.length === 0 ? (
              <tr>
                <td colSpan="6" className="px-4 py-8 text-center text-neutral-400">
                  Nenhum cliente encontrado com esse filtro.
                </td>
              </tr>
            ) : (
              filteredClients.map((client) => (
                <tr key={client.id} className="hover:bg-orange-50/40">
                  <td className="px-4 py-3 font-medium text-neutral-800">{client.name}</td>
                  <td className="px-4 py-3 text-neutral-600">{client.corporateName || "-"}</td>
                  <td className="px-4 py-3 text-neutral-600">{client.cnpj || "-"}</td>
                  <td className="px-4 py-3">
                    {client.companyLink ? (
                      <span
                        className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${companyLinkBadgeClasses(
                          client.companyLink
                        )}`}
                      >
                        {client.companyLink}
                      </span>
                    ) : (
                      <span className="text-neutral-600">-</span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                        client.active
                          ? "bg-orange-100 text-orange-700"
                          : "bg-neutral-100 text-neutral-500"
                      }`}
                    >
                      {client.active ? "Ativo" : "Inativo"}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => setDetailsClient(client)}
                      className="mr-3 text-sm font-medium text-neutral-500 hover:text-orange-600"
                    >
                      Detalhes
                    </button>
                    <button
                      onClick={() => openEdit(client)}
                      className="mr-3 text-sm font-medium text-neutral-600 hover:text-orange-600"
                    >
                      Editar
                    </button>
                    <button
                      onClick={() => requestDelete(client.id)}
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
                {form.id ? "Editar cliente" : "Novo cliente"}
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
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Razão social</label>
                  <input
                    value={form.corporateName}
                    onChange={(e) => handleChange("corporateName", e.target.value)}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">CNPJ</label>
                  <input
                    value={form.cnpj}
                    onChange={(e) => handleChange("cnpj", e.target.value)}
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
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Email</label>
                  <input
                    type="email"
                    value={form.email}
                    onChange={(e) => handleChange("email", e.target.value)}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
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
                    Cliente ativo
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

      {detailsClient && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="max-h-[90vh] w-full max-w-lg overflow-auto rounded-2xl bg-white p-6 shadow-2xl">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-black">Detalhes do cliente</h2>
              <button
                onClick={() => setDetailsClient(null)}
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
                  <p className="font-medium text-black">{detailsClient.name || "-"}</p>
                </div>
                <div>
                  <p className="text-neutral-500">Razão social</p>
                  <p className="font-medium text-black">{detailsClient.corporateName || "-"}</p>
                </div>
                <div>
                  <p className="text-neutral-500">CNPJ</p>
                  <p className="font-medium text-black">{detailsClient.cnpj || "-"}</p>
                </div>
                <div>
                  <p className="text-neutral-500">Telefone</p>
                  <p className="font-medium text-black">{detailsClient.phone || "-"}</p>
                </div>
                <div>
                  <p className="text-neutral-500">Email</p>
                  <p className="font-medium text-black">{detailsClient.email || "-"}</p>
                </div>
                <div>
                  <p className="text-neutral-500">Vínculo (empresa)</p>
                  {detailsClient.companyLink ? (
                    <span
                      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${companyLinkBadgeClasses(
                        detailsClient.companyLink
                      )}`}
                    >
                      {detailsClient.companyLink}
                    </span>
                  ) : (
                    <p className="font-medium text-black">-</p>
                  )}
                </div>
                <div>
                  <p className="text-neutral-500">Status</p>
                  <span
                    className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                      detailsClient.active
                        ? "bg-orange-100 text-orange-700"
                        : "bg-neutral-100 text-neutral-500"
                    }`}
                  >
                    {detailsClient.active ? "Ativo" : "Inativo"}
                  </span>
                </div>
              </div>
            </div>

            <div className="mt-6 flex justify-end gap-3 border-t border-neutral-100 pt-4">
              <button
                type="button"
                onClick={() => setDetailsClient(null)}
                className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100"
              >
                Fechar
              </button>
              <button
                type="button"
                onClick={() => {
                  const client = detailsClient;
                  setDetailsClient(null);
                  openEdit(client);
                }}
                className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600"
              >
                Editar
              </button>
            </div>
          </div>
        </div>
      )}

      <ConfirmDeleteDialog
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirmed={confirmDelete}
        itemLabel="este cliente"
      />
    </Layout>
  );
}
