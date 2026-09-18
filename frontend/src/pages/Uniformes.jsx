import { useEffect, useState } from "react";
import { apiFetch } from "../api";
import { useAuth } from "../context/AuthContext";
import Layout from "../components/Layout";
import PromoterAutocomplete from "../components/PromoterAutocomplete";
import ConfirmDeleteDialog from "../components/ConfirmDeleteDialog";

function formatDate(value) {
  if (!value) return "-";
  const [year, month, day] = value.split("-");
  return `${day}/${month}/${year}`;
}

const EMPTY_FORM = {
  id: null,
  promoterId: "",
  itemId: "",
  quantity: "1",
  deliveryDate: "",
  observation: "",
};

export default function Uniformes() {
  const { token } = useAuth();
  const [promoters, setPromoters] = useState([]);
  const [items, setItems] = useState([]);
  const [deliveries, setDeliveries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const [searchPromoter, setSearchPromoter] = useState("");
  const [filterItemId, setFilterItemId] = useState("");
  const [dateStart, setDateStart] = useState("");
  const [dateEnd, setDateEnd] = useState("");

  async function loadData() {
    setLoading(true);
    setError("");

    try {
      const [promotersData, itemsData, deliveriesData] = await Promise.all([
        apiFetch("/promoters", { token }),
        apiFetch("/inventory-items", { token }),
        apiFetch("/work-item-deliveries", { token }),
      ]);
      setPromoters(promotersData);
      setItems(itemsData);
      setDeliveries(deliveriesData);
    } catch (err) {
      setError("Não foi possível carregar os registros.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  const filteredDeliveries = deliveries
    .filter((d) => {
      const term = searchPromoter.trim().toLowerCase();
      const matchesPromoter = term === "" || (d.promoterName || "").toLowerCase().includes(term);
      const matchesItem = filterItemId === "" || String(d.itemId) === filterItemId;
      const matchesStart = dateStart === "" || (d.deliveryDate || "") >= dateStart;
      const matchesEnd = dateEnd === "" || (d.deliveryDate || "") <= dateEnd;
      return matchesPromoter && matchesItem && matchesStart && matchesEnd;
    })
    .sort((a, b) => (b.deliveryDate || "").localeCompare(a.deliveryDate || ""));

  function handleChange(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  function openNew() {
    setForm(EMPTY_FORM);
    setIsFormOpen(true);
  }

  function openEdit(delivery) {
    setForm({
      id: delivery.id,
      promoterId: delivery.promoterId != null ? String(delivery.promoterId) : "",
      itemId: delivery.itemId != null ? String(delivery.itemId) : "",
      quantity: delivery.quantity != null ? String(delivery.quantity) : "1",
      deliveryDate: delivery.deliveryDate || "",
      observation: delivery.observation || "",
    });
    setIsFormOpen(true);
  }

  function closeForm() {
    setIsFormOpen(false);
    setForm(EMPTY_FORM);
  }

  async function handleSubmit(event) {
    event.preventDefault();

    if (!form.promoterId) {
      setError("Selecione um promotor.");
      return;
    }

    if (!form.itemId) {
      setError("Selecione um item.");
      return;
    }

    if (!form.quantity || Number(form.quantity) <= 0) {
      setError("Informe uma quantidade válida.");
      return;
    }

    setSaving(true);
    setError("");

    const payload = {
      promoterId: Number(form.promoterId),
      itemId: Number(form.itemId),
      quantity: Number(form.quantity),
      deliveryDate: form.deliveryDate === "" ? null : form.deliveryDate,
      observation: form.observation.trim() === "" ? null : form.observation.trim(),
    };

    try {
      if (form.id) {
        await apiFetch(`/work-item-deliveries/${form.id}`, {
          method: "PUT",
          body: payload,
          token,
        });
      } else {
        await apiFetch("/work-item-deliveries", {
          method: "POST",
          body: payload,
          token,
        });
      }

      closeForm();
      await loadData();
    } catch (err) {
      setError(err.message || "Não foi possível salvar o registro.");
    } finally {
      setSaving(false);
    }
  }

  function requestDelete(id) {
    setDeleteTarget(id);
  }

  async function confirmDelete() {
    try {
      await apiFetch(`/work-item-deliveries/${deleteTarget}`, { method: "DELETE", token });
      setDeleteTarget(null);
      await loadData();
    } catch (err) {
      setDeleteTarget(null);
      setError(err.message || "Não foi possível excluir o registro.");
    }
  }

  return (
    <Layout title="Uniformes e materiais">
      <div className="mb-6 flex items-center justify-between">
        <p className="text-sm text-neutral-500">
          {filteredDeliveries.length} de {deliveries.length} registro{deliveries.length !== 1 ? "s" : ""}
        </p>

        <button
          onClick={openNew}
          className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600"
        >
          + Novo registro
        </button>
      </div>

      <div className="mb-4 flex flex-wrap gap-3 rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
        <input
          type="text"
          value={searchPromoter}
          onChange={(e) => setSearchPromoter(e.target.value)}
          placeholder="Buscar por nome do promotor..."
          className="min-w-[220px] flex-1 rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
        />

        <select
          value={filterItemId}
          onChange={(e) => setFilterItemId(e.target.value)}
          className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
        >
          <option value="">Todos os itens</option>
          {items.map((item) => (
            <option key={item.id} value={item.id}>
              {item.category ? `${item.category} - ${item.name}` : item.name}
            </option>
          ))}
        </select>

        <div>
          <label className="mb-1 block text-xs font-medium text-neutral-500">Data inicial</label>
          <input
            type="date"
            value={dateStart}
            onChange={(e) => setDateStart(e.target.value)}
            className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
          />
        </div>

        <div>
          <label className="mb-1 block text-xs font-medium text-neutral-500">Data final</label>
          <input
            type="date"
            value={dateEnd}
            onChange={(e) => setDateEnd(e.target.value)}
            className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
          />
        </div>

        {(dateStart || dateEnd) && (
          <button
            type="button"
            onClick={() => {
              setDateStart("");
              setDateEnd("");
            }}
            className="self-end rounded-lg px-3 py-2 text-sm font-medium text-neutral-500 hover:text-orange-600"
          >
            Limpar datas
          </button>
        )}
      </div>

      {error && (
        <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>
      )}

      <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
        <table className="w-full text-left text-sm">
          <thead className="bg-black text-white">
            <tr>
              <th className="px-4 py-3 font-medium">Promotor</th>
              <th className="px-4 py-3 font-medium">Item</th>
              <th className="px-4 py-3 font-medium">Quantidade</th>
              <th className="px-4 py-3 font-medium">Data de entrega</th>
              <th className="px-4 py-3 font-medium">Observação</th>
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
            ) : deliveries.length === 0 ? (
              <tr>
                <td colSpan="6" className="px-4 py-8 text-center text-neutral-400">
                  Nenhum registro cadastrado ainda.
                </td>
              </tr>
            ) : filteredDeliveries.length === 0 ? (
              <tr>
                <td colSpan="6" className="px-4 py-8 text-center text-neutral-400">
                  Nenhum registro encontrado com esse filtro.
                </td>
              </tr>
            ) : (
              filteredDeliveries.map((delivery) => (
                <tr key={delivery.id} className="hover:bg-orange-50/40">
                  <td className="px-4 py-3 font-medium text-neutral-800">{delivery.promoterName}</td>
                  <td className="px-4 py-3 text-neutral-600">
                    {delivery.itemCategory ? `${delivery.itemCategory} - ${delivery.itemName}` : delivery.itemName}
                  </td>
                  <td className="px-4 py-3 text-neutral-600">{delivery.quantity ?? "-"}</td>
                  <td className="px-4 py-3 text-neutral-600">{formatDate(delivery.deliveryDate)}</td>
                  <td className="px-4 py-3 text-neutral-600">{delivery.observation || "-"}</td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => openEdit(delivery)}
                      className="mr-3 text-sm font-medium text-neutral-600 hover:text-orange-600"
                    >
                      Editar
                    </button>
                    <button
                      onClick={() => requestDelete(delivery.id)}
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
                {form.id ? "Editar registro" : "Novo registro"}
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
                  onChange={(value) => handleChange("promoterId", value)}
                />
              </div>

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Item</label>
                  <select
                    value={form.itemId}
                    onChange={(e) => handleChange("itemId", e.target.value)}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  >
                    <option value="">Selecione...</option>
                    {items.map((item) => (
                      <option key={item.id} value={item.id}>
                        {item.category ? `${item.category} - ${item.name}` : item.name} (estoque:{" "}
                        {item.currentStock ?? 0})
                      </option>
                    ))}
                  </select>
                  {items.length === 0 && (
                    <p className="mt-1 text-xs text-neutral-400">
                      Nenhum item cadastrado ainda. Cadastre em "Estoque" primeiro.
                    </p>
                  )}
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Quantidade</label>
                  <input
                    type="number"
                    min="1"
                    step="1"
                    value={form.quantity}
                    onChange={(e) => handleChange("quantity", e.target.value)}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Data de entrega</label>
                <input
                  type="date"
                  value={form.deliveryDate}
                  onChange={(e) => handleChange("deliveryDate", e.target.value)}
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Observação</label>
                <textarea
                  value={form.observation}
                  onChange={(e) => handleChange("observation", e.target.value)}
                  rows={3}
                  placeholder="Ex: tamanho M, entregue com desconto em folha..."
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
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
        itemLabel="este registro"
      />
    </Layout>
  );
}
