import { useEffect, useState } from "react";
import { apiFetch } from "../api";
import { useAuth } from "../context/AuthContext";
import Layout from "../components/Layout";
import ConfirmDeleteDialog from "../components/ConfirmDeleteDialog";
import FaturamentoCalendar from "../components/FaturamentoCalendar";

const STATUS_OPTIONS = [
  { value: "PENDENTE", label: "Pendente" },
  { value: "FATURADO", label: "Faturado" },
  { value: "PAGO", label: "Pago" },
  { value: "CANCELADO", label: "Cancelado" },
];

const STATUS_STYLES = {
  PENDENTE: "bg-orange-100 text-orange-700",
  FATURADO: "bg-blue-100 text-blue-700",
  PAGO: "bg-green-100 text-green-700",
  CANCELADO: "bg-red-50 text-red-600",
};

const COMPANY_LINK_OPTIONS = ["AT", "TEJO"];

const EMPTY_FORM = {
  id: null,
  clientId: "",
  amount: "",
  receivedAmount: "",
  description: "",
  dueDate: "",
  issueDate: "",
  paymentDate: "",
  status: "PENDENTE",
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

export default function Faturamento() {
  const { token } = useAuth();
  const [invoices, setInvoices] = useState([]);
  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [view, setView] = useState("lista");

  const [search, setSearch] = useState("");
  const [filterStatus, setFilterStatus] = useState("");
  const [filterClientId, setFilterClientId] = useState("");
  const [filterCompanyLink, setFilterCompanyLink] = useState("");
  const [filterStart, setFilterStart] = useState("");
  const [filterEnd, setFilterEnd] = useState("");

  async function loadData() {
    setLoading(true);
    setError("");

    try {
      const [invoiceData, clientData] = await Promise.all([
        apiFetch("/invoices", { token }),
        apiFetch("/clients", { token }),
      ]);
      setInvoices(invoiceData);
      setClients(clientData);
    } catch (err) {
      setError("Não foi possível carregar os faturamentos.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  const clientsById = Object.fromEntries(clients.map((c) => [c.id, c]));

  const filteredInvoices = invoices.filter((invoice) => {
    const client = clientsById[invoice.clientId];
    const term = search.trim().toLowerCase();

    const matchesSearch =
      term === "" ||
      (client?.name || "").toLowerCase().includes(term) ||
      (invoice.description || "").toLowerCase().includes(term);

    const matchesStatus = filterStatus === "" || invoice.status === filterStatus;
    const matchesClient = filterClientId === "" || String(invoice.clientId) === filterClientId;
    const matchesCompanyLink = filterCompanyLink === "" || client?.companyLink === filterCompanyLink;

    const matchesStart = filterStart === "" || (invoice.dueDate && invoice.dueDate >= filterStart);
    const matchesEnd = filterEnd === "" || (invoice.dueDate && invoice.dueDate <= filterEnd);

    return matchesSearch && matchesStatus && matchesClient && matchesCompanyLink && matchesStart && matchesEnd;
  });

  function handleChange(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  function openNew() {
    setForm(EMPTY_FORM);
    setIsFormOpen(true);
  }

  function openEdit(invoice) {
    setForm({
      id: invoice.id,
      clientId: invoice.clientId ?? "",
      amount: invoice.amount ?? "",
      receivedAmount: invoice.receivedAmount ?? "",
      description: invoice.description || "",
      dueDate: invoice.dueDate || "",
      issueDate: invoice.issueDate || "",
      paymentDate: invoice.paymentDate || "",
      status: invoice.status || "PENDENTE",
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
      clientId: form.clientId === "" ? null : Number(form.clientId),
      amount: form.amount === "" ? null : Number(form.amount),
      receivedAmount: form.receivedAmount === "" ? null : Number(form.receivedAmount),
      dueDate: form.dueDate === "" ? null : form.dueDate,
      issueDate: form.issueDate === "" ? null : form.issueDate,
      paymentDate: form.paymentDate === "" ? null : form.paymentDate,
    };

    try {
      if (form.id) {
        await apiFetch(`/invoices/${form.id}`, { method: "PUT", body: payload, token });
      } else {
        await apiFetch("/invoices", { method: "POST", body: payload, token });
      }

      closeForm();
      await loadData();
    } catch (err) {
      setError("Não foi possível salvar o faturamento.");
    } finally {
      setSaving(false);
    }
  }

  function requestDelete(id) {
    setDeleteTarget(id);
  }

  async function confirmDelete() {
    try {
      await apiFetch(`/invoices/${deleteTarget}`, { method: "DELETE", token });
      setDeleteTarget(null);
      await loadData();
    } catch (err) {
      setDeleteTarget(null);
      setError("Não foi possível excluir o faturamento.");
    }
  }

  return (
    <Layout title="Faturamento">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm text-neutral-500">
          {filteredInvoices.length} de {invoices.length} faturamento{invoices.length !== 1 ? "s" : ""}
        </p>

        <div className="flex items-center gap-3">
          <div className="flex rounded-lg border border-neutral-300 bg-white p-1">
            <button
              type="button"
              onClick={() => setView("lista")}
              className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                view === "lista" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
              }`}
            >
              Lista
            </button>
            <button
              type="button"
              onClick={() => setView("calendario")}
              className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                view === "calendario" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
              }`}
            >
              Calendário
            </button>
          </div>

          <button
            onClick={openNew}
            disabled={clients.length === 0}
            className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-50"
          >
            + Novo faturamento
          </button>
        </div>
      </div>

      <div className="mb-4 flex flex-wrap gap-3 rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Buscar por cliente ou descrição..."
          className="min-w-[220px] flex-1 rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
        />

        <select
          value={filterClientId}
          onChange={(e) => setFilterClientId(e.target.value)}
          className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
        >
          <option value="">Todos os clientes</option>
          {clients.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
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
          value={filterStatus}
          onChange={(e) => setFilterStatus(e.target.value)}
          className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
        >
          <option value="">Todos os status</option>
          {STATUS_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>

        <div className="flex items-center gap-2">
          <label className="text-sm text-neutral-500">Vencimento de</label>
          <input
            type="date"
            value={filterStart}
            onChange={(e) => setFilterStart(e.target.value)}
            className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
          />
          <label className="text-sm text-neutral-500">até</label>
          <input
            type="date"
            value={filterEnd}
            onChange={(e) => setFilterEnd(e.target.value)}
            className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
          />
          {(filterStart || filterEnd) && (
            <button
              type="button"
              onClick={() => {
                setFilterStart("");
                setFilterEnd("");
              }}
              className="text-sm text-neutral-400 hover:text-orange-600"
            >
              Limpar período
            </button>
          )}
        </div>
      </div>

      {clients.length === 0 && !loading && (
        <div className="mb-4 rounded-lg bg-neutral-100 px-4 py-3 text-sm text-neutral-600">
          Cadastre pelo menos um cliente antes de lançar um faturamento.
        </div>
      )}

      {error && (
        <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>
      )}

      {view === "lista" ? (
        <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
          <table className="w-full text-left text-sm">
            <thead className="bg-black text-white">
              <tr>
                <th className="px-4 py-3 font-medium">Cliente</th>
                <th className="px-4 py-3 font-medium">Vínculo</th>
                <th className="px-4 py-3 font-medium">Valor</th>
                <th className="px-4 py-3 font-medium">Vencimento</th>
                <th className="px-4 py-3 font-medium">Faturado em</th>
                <th className="px-4 py-3 font-medium">Pago em</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium text-right">Ações</th>
              </tr>
            </thead>

            <tbody className="divide-y divide-neutral-100">
              {loading ? (
                <tr>
                  <td colSpan="8" className="px-4 py-8 text-center text-neutral-400">
                    Carregando...
                  </td>
                </tr>
              ) : invoices.length === 0 ? (
                <tr>
                  <td colSpan="8" className="px-4 py-8 text-center text-neutral-400">
                    Nenhum faturamento registrado ainda.
                  </td>
                </tr>
              ) : filteredInvoices.length === 0 ? (
                <tr>
                  <td colSpan="8" className="px-4 py-8 text-center text-neutral-400">
                    Nenhum faturamento encontrado com esse filtro.
                  </td>
                </tr>
              ) : (
                filteredInvoices.map((invoice) => {
                  const client = clientsById[invoice.clientId];

                  return (
                    <tr key={invoice.id} className="hover:bg-orange-50/40">
                      <td className="px-4 py-3 font-medium text-neutral-800">
                        {client ? client.name : `Cliente #${invoice.clientId}`}
                      </td>
                      <td className="px-4 py-3 text-neutral-600">{client?.companyLink || "-"}</td>
                      <td className="px-4 py-3 text-neutral-600">{formatMoney(invoice.amount)}</td>
                      <td className="px-4 py-3 text-neutral-600">{formatDate(invoice.dueDate)}</td>
                      <td className="px-4 py-3 text-neutral-600">{formatDate(invoice.issueDate)}</td>
                      <td className="px-4 py-3 text-neutral-600">{formatDate(invoice.paymentDate)}</td>
                      <td className="px-4 py-3">
                        <span
                          className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                            STATUS_STYLES[invoice.status] || "bg-neutral-100 text-neutral-500"
                          }`}
                        >
                          {invoice.status || "-"}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right">
                        <button
                          onClick={() => openEdit(invoice)}
                          className="mr-3 text-sm font-medium text-neutral-600 hover:text-orange-600"
                        >
                          Editar
                        </button>
                        <button
                          onClick={() => requestDelete(invoice.id)}
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
      ) : (
        <FaturamentoCalendar
          invoices={filteredInvoices}
          clientsById={clientsById}
          onSelectInvoice={openEdit}
        />
      )}

      {isFormOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="max-h-[90vh] w-full max-w-2xl overflow-auto rounded-2xl bg-white p-6 shadow-2xl">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-black">
                {form.id ? "Editar faturamento" : "Novo faturamento"}
              </h2>
              <button onClick={closeForm} className="text-neutral-400 hover:text-black" type="button">
                ✕
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div className="sm:col-span-2">
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Cliente</label>
                  <select
                    value={form.clientId}
                    onChange={(e) => handleChange("clientId", e.target.value)}
                    required
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  >
                    <option value="">Selecione...</option>
                    {clients.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
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
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Valor recebido</label>
                  <input
                    type="number"
                    step="0.01"
                    value={form.receivedAmount}
                    onChange={(e) => handleChange("receivedAmount", e.target.value)}
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

                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Vencimento</label>
                  <input
                    type="date"
                    value={form.dueDate}
                    onChange={(e) => handleChange("dueDate", e.target.value)}
                    required
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Status</label>
                  <select
                    value={form.status}
                    onChange={(e) => handleChange("status", e.target.value)}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  >
                    {STATUS_OPTIONS.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Faturado em</label>
                  <input
                    type="date"
                    value={form.issueDate}
                    onChange={(e) => handleChange("issueDate", e.target.value)}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Pago em</label>
                  <input
                    type="date"
                    value={form.paymentDate}
                    onChange={(e) => handleChange("paymentDate", e.target.value)}
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
        itemLabel="este faturamento"
      />
    </Layout>
  );
}
