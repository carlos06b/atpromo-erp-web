import { useEffect, useState } from "react";
import { apiFetch } from "../api";
import { useAuth } from "../context/AuthContext";
import Layout from "../components/Layout";
import PromoterAutocomplete from "../components/PromoterAutocomplete";
import ConfirmDeleteDialog from "../components/ConfirmDeleteDialog";

const CATEGORY_OPTIONS = ["Uniforme", "Calçado", "Crachá", "Colete", "Boné", "Outro"];

const MOVEMENT_TYPE_OPTIONS = [
  { value: "REPOSICAO", label: "Reposição" },
  { value: "DEVOLUCAO", label: "Devolução de promotor" },
];

function formatDate(value) {
  if (!value) return "-";
  const [year, month, day] = value.split("-");
  return `${day}/${month}/${year}`;
}

function movementTypeLabel(type) {
  return MOVEMENT_TYPE_OPTIONS.find((opt) => opt.value === type)?.label || type;
}

const EMPTY_ITEM_FORM = {
  id: null,
  category: "Uniforme",
  name: "",
  currentStock: "0",
};

const EMPTY_MOVEMENT_FORM = {
  itemId: "",
  type: "REPOSICAO",
  quantity: "1",
  movementDate: "",
  promoterId: "",
  observation: "",
};

export default function Estoque() {
  const { token } = useAuth();
  const [view, setView] = useState("itens");

  const [items, setItems] = useState([]);
  const [movements, setMovements] = useState([]);
  const [promoters, setPromoters] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [searchItem, setSearchItem] = useState("");
  const [filterCategory, setFilterCategory] = useState("");
  const [movementDateStart, setMovementDateStart] = useState("");
  const [movementDateEnd, setMovementDateEnd] = useState("");

  const [itemForm, setItemForm] = useState(EMPTY_ITEM_FORM);
  const [isItemFormOpen, setIsItemFormOpen] = useState(false);
  const [savingItem, setSavingItem] = useState(false);
  const [deleteItemTarget, setDeleteItemTarget] = useState(null);

  const [movementForm, setMovementForm] = useState(EMPTY_MOVEMENT_FORM);
  const [isMovementFormOpen, setIsMovementFormOpen] = useState(false);
  const [savingMovement, setSavingMovement] = useState(false);
  const [deleteMovementTarget, setDeleteMovementTarget] = useState(null);

  async function loadData() {
    setLoading(true);
    setError("");

    try {
      const [itemsData, movementsData, promotersData] = await Promise.all([
        apiFetch("/inventory-items", { token }),
        apiFetch("/stock-movements", { token }),
        apiFetch("/promoters", { token }),
      ]);
      setItems(itemsData);
      setMovements(movementsData);
      setPromoters(promotersData);
    } catch (err) {
      setError("Não foi possível carregar o estoque.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  const filteredItems = items
    .filter((item) => {
      const term = searchItem.trim().toLowerCase();
      const matchesSearch = term === "" || (item.name || "").toLowerCase().includes(term);
      const matchesCategory = filterCategory === "" || item.category === filterCategory;
      return matchesSearch && matchesCategory;
    })
    .sort((a, b) => (a.category || "").localeCompare(b.category || "", "pt-BR") || (a.name || "").localeCompare(b.name || "", "pt-BR"));

  const filteredMovements = movements
    .filter((m) => {
      const matchesStart = movementDateStart === "" || (m.movementDate || "") >= movementDateStart;
      const matchesEnd = movementDateEnd === "" || (m.movementDate || "") <= movementDateEnd;
      return matchesStart && matchesEnd;
    })
    .sort((a, b) => (b.movementDate || "").localeCompare(a.movementDate || ""));

  function handleItemChange(field, value) {
    setItemForm((prev) => ({ ...prev, [field]: value }));
  }

  function openNewItem() {
    setItemForm(EMPTY_ITEM_FORM);
    setIsItemFormOpen(true);
  }

  function openEditItem(item) {
    setItemForm({
      id: item.id,
      category: item.category || "Uniforme",
      name: item.name || "",
      currentStock: item.currentStock != null ? String(item.currentStock) : "0",
    });
    setIsItemFormOpen(true);
  }

  function closeItemForm() {
    setIsItemFormOpen(false);
    setItemForm(EMPTY_ITEM_FORM);
  }

  async function handleItemSubmit(event) {
    event.preventDefault();

    if (!itemForm.name.trim()) {
      setError("Informe o nome do item.");
      return;
    }

    setSavingItem(true);
    setError("");

    const payload = {
      category: itemForm.category,
      name: itemForm.name.trim(),
      currentStock: itemForm.currentStock === "" ? 0 : Number(itemForm.currentStock),
    };

    try {
      if (itemForm.id) {
        await apiFetch(`/inventory-items/${itemForm.id}`, { method: "PUT", body: payload, token });
      } else {
        await apiFetch("/inventory-items", { method: "POST", body: payload, token });
      }

      closeItemForm();
      await loadData();
    } catch (err) {
      setError(err.message || "Não foi possível salvar o item.");
    } finally {
      setSavingItem(false);
    }
  }

  function requestDeleteItem(id) {
    setDeleteItemTarget(id);
  }

  async function confirmDeleteItem() {
    try {
      await apiFetch(`/inventory-items/${deleteItemTarget}`, { method: "DELETE", token });
      setDeleteItemTarget(null);
      await loadData();
    } catch (err) {
      setDeleteItemTarget(null);
      setError(err.message || "Não foi possível excluir o item.");
    }
  }

  function handleMovementChange(field, value) {
    setMovementForm((prev) => ({ ...prev, [field]: value }));
  }

  function openNewMovement() {
    setMovementForm(EMPTY_MOVEMENT_FORM);
    setIsMovementFormOpen(true);
  }

  function closeMovementForm() {
    setIsMovementFormOpen(false);
    setMovementForm(EMPTY_MOVEMENT_FORM);
  }

  async function handleMovementSubmit(event) {
    event.preventDefault();

    if (!movementForm.itemId) {
      setError("Selecione um item.");
      return;
    }

    if (movementForm.type === "DEVOLUCAO" && !movementForm.promoterId) {
      setError("Selecione o promotor que devolveu o item.");
      return;
    }

    setSavingMovement(true);
    setError("");

    const payload = {
      itemId: Number(movementForm.itemId),
      type: movementForm.type,
      quantity: movementForm.quantity === "" ? null : Number(movementForm.quantity),
      movementDate: movementForm.movementDate === "" ? null : movementForm.movementDate,
      promoterId: movementForm.type === "DEVOLUCAO" && movementForm.promoterId ? Number(movementForm.promoterId) : null,
      observation: movementForm.observation.trim() === "" ? null : movementForm.observation.trim(),
    };

    try {
      await apiFetch("/stock-movements", { method: "POST", body: payload, token });
      closeMovementForm();
      await loadData();
    } catch (err) {
      setError(err.message || "Não foi possível registrar a movimentação.");
    } finally {
      setSavingMovement(false);
    }
  }

  function requestDeleteMovement(id) {
    setDeleteMovementTarget(id);
  }

  async function confirmDeleteMovement() {
    try {
      await apiFetch(`/stock-movements/${deleteMovementTarget}`, { method: "DELETE", token });
      setDeleteMovementTarget(null);
      await loadData();
    } catch (err) {
      setDeleteMovementTarget(null);
      setError(err.message || "Não foi possível excluir a movimentação.");
    }
  }

  return (
    <Layout title="Estoque">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div className="flex rounded-lg border border-neutral-300 bg-white p-1">
          <button
            type="button"
            onClick={() => setView("itens")}
            className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
              view === "itens" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
            }`}
          >
            Itens em estoque
          </button>
          <button
            type="button"
            onClick={() => setView("movimentacoes")}
            className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
              view === "movimentacoes" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
            }`}
          >
            Movimentações
          </button>
        </div>

        {view === "itens" ? (
          <button
            onClick={openNewItem}
            className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600"
          >
            + Novo item
          </button>
        ) : (
          <button
            onClick={openNewMovement}
            className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600"
          >
            + Registrar entrada
          </button>
        )}
      </div>

      {error && (
        <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>
      )}

      {view === "itens" && (
        <>
          <div className="mb-4 flex flex-wrap gap-3 rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
            <input
              type="text"
              value={searchItem}
              onChange={(e) => setSearchItem(e.target.value)}
              placeholder="Buscar por nome do item..."
              className="min-w-[220px] flex-1 rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
            />

            <select
              value={filterCategory}
              onChange={(e) => setFilterCategory(e.target.value)}
              className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
            >
              <option value="">Todas as categorias</option>
              {CATEGORY_OPTIONS.map((opt) => (
                <option key={opt} value={opt}>
                  {opt}
                </option>
              ))}
            </select>
          </div>

          <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
            <table className="w-full text-left text-sm">
              <thead className="bg-black text-white">
                <tr>
                  <th className="px-4 py-3 font-medium">Categoria</th>
                  <th className="px-4 py-3 font-medium">Item</th>
                  <th className="px-4 py-3 font-medium">Estoque atual</th>
                  <th className="px-4 py-3 font-medium text-right">Ações</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-neutral-100">
                {loading ? (
                  <tr>
                    <td colSpan="4" className="px-4 py-8 text-center text-neutral-400">
                      Carregando...
                    </td>
                  </tr>
                ) : items.length === 0 ? (
                  <tr>
                    <td colSpan="4" className="px-4 py-8 text-center text-neutral-400">
                      Nenhum item cadastrado ainda.
                    </td>
                  </tr>
                ) : filteredItems.length === 0 ? (
                  <tr>
                    <td colSpan="4" className="px-4 py-8 text-center text-neutral-400">
                      Nenhum item encontrado com esse filtro.
                    </td>
                  </tr>
                ) : (
                  filteredItems.map((item) => (
                    <tr key={item.id} className="hover:bg-orange-50/40">
                      <td className="px-4 py-3 text-neutral-600">{item.category || "-"}</td>
                      <td className="px-4 py-3 font-medium text-neutral-800">{item.name}</td>
                      <td className="px-4 py-3">
                        <span
                          className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                            (item.currentStock ?? 0) <= 0
                              ? "bg-red-100 text-red-700"
                              : "bg-orange-100 text-orange-700"
                          }`}
                        >
                          {item.currentStock ?? 0}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right">
                        <button
                          onClick={() => openEditItem(item)}
                          className="mr-3 text-sm font-medium text-neutral-600 hover:text-orange-600"
                        >
                          Editar
                        </button>
                        <button
                          onClick={() => requestDeleteItem(item.id)}
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
        </>
      )}

      {view === "movimentacoes" && (
        <>
          <div className="mb-4 flex flex-wrap items-end gap-3 rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
            <div>
              <label className="mb-1 block text-sm font-medium text-neutral-700">Data inicial</label>
              <input
                type="date"
                value={movementDateStart}
                onChange={(e) => setMovementDateStart(e.target.value)}
                className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-neutral-700">Data final</label>
              <input
                type="date"
                value={movementDateEnd}
                onChange={(e) => setMovementDateEnd(e.target.value)}
                className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
              />
            </div>

            {(movementDateStart || movementDateEnd) && (
              <button
                type="button"
                onClick={() => {
                  setMovementDateStart("");
                  setMovementDateEnd("");
                }}
                className="rounded-lg px-3 py-2 text-sm font-medium text-neutral-500 hover:text-orange-600"
              >
                Limpar datas
              </button>
            )}
          </div>

          <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
          <table className="w-full text-left text-sm">
            <thead className="bg-black text-white">
              <tr>
                <th className="px-4 py-3 font-medium">Item</th>
                <th className="px-4 py-3 font-medium">Tipo</th>
                <th className="px-4 py-3 font-medium">Quantidade</th>
                <th className="px-4 py-3 font-medium">Data</th>
                <th className="px-4 py-3 font-medium">Promotor</th>
                <th className="px-4 py-3 font-medium">Observação</th>
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
              ) : movements.length === 0 ? (
                <tr>
                  <td colSpan="7" className="px-4 py-8 text-center text-neutral-400">
                    Nenhuma movimentação registrada ainda.
                  </td>
                </tr>
              ) : filteredMovements.length === 0 ? (
                <tr>
                  <td colSpan="7" className="px-4 py-8 text-center text-neutral-400">
                    Nenhuma movimentação encontrada com esse filtro.
                  </td>
                </tr>
              ) : (
                filteredMovements.map((movement) => (
                  <tr key={movement.id} className="hover:bg-orange-50/40">
                    <td className="px-4 py-3 font-medium text-neutral-800">{movement.itemName}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                          movement.type === "DEVOLUCAO"
                            ? "bg-blue-100 text-blue-700"
                            : "bg-green-100 text-green-700"
                        }`}
                      >
                        {movementTypeLabel(movement.type)}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-neutral-600">{movement.quantity}</td>
                    <td className="px-4 py-3 text-neutral-600">{formatDate(movement.movementDate)}</td>
                    <td className="px-4 py-3 text-neutral-600">{movement.promoterName || "-"}</td>
                    <td className="px-4 py-3 text-neutral-600">{movement.observation || "-"}</td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => requestDeleteMovement(movement.id)}
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
        </>
      )}

      {isItemFormOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="max-h-[90vh] w-full max-w-md overflow-auto rounded-2xl bg-white p-6 shadow-2xl">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-black">
                {itemForm.id ? "Editar item" : "Novo item"}
              </h2>
              <button onClick={closeItemForm} className="text-neutral-400 hover:text-black" type="button">
                ✕
              </button>
            </div>

            <form onSubmit={handleItemSubmit} className="space-y-4">
              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Categoria</label>
                <select
                  value={itemForm.category}
                  onChange={(e) => handleItemChange("category", e.target.value)}
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                >
                  {CATEGORY_OPTIONS.map((opt) => (
                    <option key={opt} value={opt}>
                      {opt}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Nome do item</label>
                <input
                  value={itemForm.name}
                  onChange={(e) => handleItemChange("name", e.target.value)}
                  placeholder="Ex: Bota 41, Camisa M..."
                  required
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">
                  {itemForm.id ? "Estoque atual (ajuste manual)" : "Estoque inicial"}
                </label>
                <input
                  type="number"
                  min="0"
                  step="1"
                  value={itemForm.currentStock}
                  onChange={(e) => handleItemChange("currentStock", e.target.value)}
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
                {itemForm.id && (
                  <p className="mt-1 text-xs text-neutral-400">
                    Use isso só pra corrigir uma contagem errada. Entregas, reposições e devoluções já ajustam o
                    estoque automaticamente.
                  </p>
                )}
              </div>

              <div className="flex justify-end gap-3 border-t border-neutral-100 pt-4">
                <button
                  type="button"
                  onClick={closeItemForm}
                  className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={savingItem}
                  className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-60"
                >
                  {savingItem ? "Salvando..." : itemForm.id ? "Salvar alterações" : "Cadastrar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {isMovementFormOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="max-h-[90vh] w-full max-w-lg overflow-auto rounded-2xl bg-white p-6 shadow-2xl">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-black">Registrar entrada</h2>
              <button onClick={closeMovementForm} className="text-neutral-400 hover:text-black" type="button">
                ✕
              </button>
            </div>

            <form onSubmit={handleMovementSubmit} className="space-y-4">
              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Item</label>
                <select
                  value={movementForm.itemId}
                  onChange={(e) => handleMovementChange("itemId", e.target.value)}
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                >
                  <option value="">Selecione...</option>
                  {items.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.category ? `${item.category} - ${item.name}` : item.name} (estoque: {item.currentStock ?? 0})
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Tipo</label>
                  <select
                    value={movementForm.type}
                    onChange={(e) => handleMovementChange("type", e.target.value)}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  >
                    {MOVEMENT_TYPE_OPTIONS.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Quantidade</label>
                  <input
                    type="number"
                    min="1"
                    step="1"
                    value={movementForm.quantity}
                    onChange={(e) => handleMovementChange("quantity", e.target.value)}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>
              </div>

              {movementForm.type === "DEVOLUCAO" && (
                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">
                    Promotor que devolveu o item
                  </label>
                  <PromoterAutocomplete
                    promoters={promoters}
                    value={movementForm.promoterId}
                    onChange={(value) => handleMovementChange("promoterId", value)}
                  />
                </div>
              )}

              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Data</label>
                <input
                  type="date"
                  value={movementForm.movementDate}
                  onChange={(e) => handleMovementChange("movementDate", e.target.value)}
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Observação</label>
                <textarea
                  value={movementForm.observation}
                  onChange={(e) => handleMovementChange("observation", e.target.value)}
                  rows={3}
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
              </div>

              <div className="flex justify-end gap-3 border-t border-neutral-100 pt-4">
                <button
                  type="button"
                  onClick={closeMovementForm}
                  className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={savingMovement}
                  className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-60"
                >
                  {savingMovement ? "Salvando..." : "Registrar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <ConfirmDeleteDialog
        open={deleteItemTarget !== null}
        onClose={() => setDeleteItemTarget(null)}
        onConfirmed={confirmDeleteItem}
        itemLabel="este item do estoque"
      />

      <ConfirmDeleteDialog
        open={deleteMovementTarget !== null}
        onClose={() => setDeleteMovementTarget(null)}
        onConfirmed={confirmDeleteMovement}
        itemLabel="esta movimentação"
      />
    </Layout>
  );
}
