import { useEffect, useState } from "react";
import { apiFetch } from "../api";
import { useAuth } from "../context/AuthContext";
import Layout from "../components/Layout";
import ConfirmDeleteDialog from "../components/ConfirmDeleteDialog";

const STATUS_STYLES = {
  true: "bg-green-100 text-green-700",
  false: "bg-orange-100 text-orange-700",
};

const EMPTY_FIXED_FORM = {
  id: null,
  name: "",
  amount: "",
  dueDate: "",
  status: false,
  paymentDate: "",
  description: "",
  active: true,
};

const EMPTY_VARIABLE_FORM = {
  id: null,
  name: "",
  amount: "",
  date: "",
  status: false,
  paymentDate: "",
  description: "",
  isInstallment: false,
  totalAmount: "",
  installmentCount: 2,
  installmentGroup: null,
  installmentNumber: null,
  totalInstallments: null,
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

function addMonthsIso(isoDate, monthsToAdd) {
  const [y, m, d] = isoDate.split("-").map(Number);
  let year = y;
  let month = m + monthsToAdd;

  while (month > 12) {
    month -= 12;
    year += 1;
  }

  const lastDayOfTargetMonth = new Date(year, month, 0).getDate();
  const day = Math.min(d, lastDayOfTargetMonth);

  return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

function toIso(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function getCurrentMonthRange() {
  const now = new Date();
  const first = new Date(now.getFullYear(), now.getMonth(), 1);
  const last = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  return { start: toIso(first), end: toIso(last) };
}

function splitAmountInInstallments(totalAmount, count) {
  const totalCents = Math.round(Number(totalAmount) * 100);
  const baseCents = Math.floor(totalCents / count);
  const remainderCents = totalCents - baseCents * count;

  const amounts = [];
  for (let i = 0; i < count; i++) {
    const cents = i === count - 1 ? baseCents + remainderCents : baseCents;
    amounts.push(cents / 100);
  }
  return amounts;
}

export default function Despesas() {
  const { token } = useAuth();
  const [view, setView] = useState("fixas");
  const currentMonthRange = getCurrentMonthRange();

  const [fixedExpenses, setFixedExpenses] = useState([]);
  const [variableExpenses, setVariableExpenses] = useState([]);
  const [fixedExpenseHistory, setFixedExpenseHistory] = useState([]);
  const [showFixedHistory, setShowFixedHistory] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [generatingId, setGeneratingId] = useState(null);

  const [fixedForm, setFixedForm] = useState(EMPTY_FIXED_FORM);
  const [isFixedFormOpen, setIsFixedFormOpen] = useState(false);
  const [savingFixed, setSavingFixed] = useState(false);

  const [variableForm, setVariableForm] = useState(EMPTY_VARIABLE_FORM);
  const [isVariableFormOpen, setIsVariableFormOpen] = useState(false);
  const [savingVariable, setSavingVariable] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState(null);

  const [fixedSearch, setFixedSearch] = useState("");
  const [fixedFilterStatus, setFixedFilterStatus] = useState("");
  const [fixedFilterStart, setFixedFilterStart] = useState(currentMonthRange.start);
  const [fixedFilterEnd, setFixedFilterEnd] = useState(currentMonthRange.end);

  const [variableSearch, setVariableSearch] = useState("");
  const [variableFilterStatus, setVariableFilterStatus] = useState("");
  const [variableFilterStart, setVariableFilterStart] = useState(currentMonthRange.start);
  const [variableFilterEnd, setVariableFilterEnd] = useState(currentMonthRange.end);

  async function loadData() {
    setLoading(true);
    setError("");

    try {
      const [fixedData, variableData, historyData] = await Promise.all([
        apiFetch("/fixed-expenses", { token }),
        apiFetch("/variable-expenses", { token }),
        apiFetch("/fixed-expense-history", { token }),
      ]);
      setFixedExpenses(fixedData);
      setVariableExpenses(variableData);
      setFixedExpenseHistory(historyData);
    } catch (err) {
      setError("Não foi possível carregar as despesas.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  const filteredFixed = fixedExpenses.filter((expense) => {
    const term = fixedSearch.trim().toLowerCase();
    const matchesSearch =
      term === "" ||
      (expense.name || "").toLowerCase().includes(term) ||
      (expense.description || "").toLowerCase().includes(term);
    const matchesStatus = fixedFilterStatus === "" || String(expense.status) === fixedFilterStatus;
    const matchesStart = fixedFilterStart === "" || (expense.dueDate && expense.dueDate >= fixedFilterStart);
    const matchesEnd = fixedFilterEnd === "" || (expense.dueDate && expense.dueDate <= fixedFilterEnd);
    return matchesSearch && matchesStatus && matchesStart && matchesEnd;
  });

  const filteredFixedHistory = fixedExpenseHistory.filter((entry) => {
    const term = fixedSearch.trim().toLowerCase();
    const matchesSearch =
      term === "" ||
      (entry.name || "").toLowerCase().includes(term) ||
      (entry.description || "").toLowerCase().includes(term);
    const matchesStart = fixedFilterStart === "" || (entry.dueDate && entry.dueDate >= fixedFilterStart);
    const matchesEnd = fixedFilterEnd === "" || (entry.dueDate && entry.dueDate <= fixedFilterEnd);
    return matchesSearch && matchesStart && matchesEnd;
  });

  const filteredVariable = variableExpenses.filter((expense) => {
    const term = variableSearch.trim().toLowerCase();
    const matchesSearch =
      term === "" ||
      (expense.name || "").toLowerCase().includes(term) ||
      (expense.description || "").toLowerCase().includes(term);
    const matchesStatus = variableFilterStatus === "" || String(expense.status) === variableFilterStatus;
    const matchesStart = variableFilterStart === "" || (expense.date && expense.date >= variableFilterStart);
    const matchesEnd = variableFilterEnd === "" || (expense.date && expense.date <= variableFilterEnd);
    return matchesSearch && matchesStatus && matchesStart && matchesEnd;
  });

  // ----- Despesas fixas -----

  function handleFixedChange(field, value) {
    setFixedForm((prev) => ({ ...prev, [field]: value }));
  }

  function openNewFixed() {
    setFixedForm(EMPTY_FIXED_FORM);
    setIsFixedFormOpen(true);
  }

  function openEditFixed(expense) {
    setFixedForm({
      id: expense.id,
      name: expense.name || "",
      amount: expense.amount ?? "",
      dueDate: expense.dueDate || "",
      status: expense.status,
      paymentDate: expense.paymentDate || "",
      description: expense.description || "",
      active: expense.active,
    });
    setIsFixedFormOpen(true);
  }

  function closeFixedForm() {
    setIsFixedFormOpen(false);
    setFixedForm(EMPTY_FIXED_FORM);
  }

  async function handleFixedSubmit(event) {
    event.preventDefault();
    setSavingFixed(true);
    setError("");

    const payload = {
      ...fixedForm,
      amount: fixedForm.amount === "" ? null : Number(fixedForm.amount),
      dueDate: fixedForm.dueDate === "" ? null : fixedForm.dueDate,
      paymentDate: fixedForm.paymentDate === "" ? null : fixedForm.paymentDate,
    };

    try {
      if (fixedForm.id) {
        await apiFetch(`/fixed-expenses/${fixedForm.id}`, { method: "PUT", body: payload, token });
      } else {
        await apiFetch("/fixed-expenses", { method: "POST", body: payload, token });
      }
      closeFixedForm();
      await loadData();
    } catch (err) {
      setError("Não foi possível salvar a despesa fixa.");
    } finally {
      setSavingFixed(false);
    }
  }

  async function generateNextMonth(expense) {
    setGeneratingId(expense.id);
    setError("");

    try {
      await apiFetch("/fixed-expense-history", {
        method: "POST",
        body: {
          fixedExpenseId: expense.id,
          name: expense.name,
          amount: expense.amount,
          dueDate: expense.dueDate,
          status: expense.status ? "PAGO" : "PENDENTE",
          paymentDate: expense.paymentDate,
          description: expense.description,
        },
        token,
      });

      await apiFetch(`/fixed-expenses/${expense.id}`, {
        method: "PUT",
        body: {
          ...expense,
          dueDate: addMonthsIso(expense.dueDate, 1),
          status: false,
          paymentDate: null,
        },
        token,
      });

      await loadData();
    } catch (err) {
      setError("Não foi possível gerar o próximo mês dessa despesa.");
    } finally {
      setGeneratingId(null);
    }
  }

  // ----- Despesas variáveis -----

  function handleVariableChange(field, value) {
    setVariableForm((prev) => ({ ...prev, [field]: value }));
  }

  function openNewVariable() {
    setVariableForm(EMPTY_VARIABLE_FORM);
    setIsVariableFormOpen(true);
  }

  function openEditVariable(expense) {
    setVariableForm({
      id: expense.id,
      name: expense.name || "",
      amount: expense.amount ?? "",
      date: expense.date || "",
      status: expense.status,
      paymentDate: expense.paymentDate || "",
      description: expense.description || "",
      isInstallment: false,
      totalAmount: "",
      installmentCount: 2,
      installmentGroup: expense.installmentGroup ?? null,
      installmentNumber: expense.installmentNumber ?? null,
      totalInstallments: expense.totalInstallments ?? null,
    });
    setIsVariableFormOpen(true);
  }

  function closeVariableForm() {
    setIsVariableFormOpen(false);
    setVariableForm(EMPTY_VARIABLE_FORM);
  }

  async function handleVariableSubmit(event) {
    event.preventDefault();
    setError("");

    if (variableForm.id) {
      setSavingVariable(true);
      const payload = {
        name: variableForm.name,
        amount: variableForm.amount === "" ? null : Number(variableForm.amount),
        date: variableForm.date === "" ? null : variableForm.date,
        status: variableForm.status,
        paymentDate: variableForm.paymentDate === "" ? null : variableForm.paymentDate,
        description: variableForm.description === "" ? null : variableForm.description,
        installmentGroup: variableForm.installmentGroup,
        installmentNumber: variableForm.installmentNumber,
        totalInstallments: variableForm.totalInstallments,
      };

      try {
        await apiFetch(`/variable-expenses/${variableForm.id}`, { method: "PUT", body: payload, token });
        closeVariableForm();
        await loadData();
      } catch (err) {
        setError("Não foi possível salvar a despesa variável.");
      } finally {
        setSavingVariable(false);
      }
      return;
    }

    if (variableForm.isInstallment) {
      const count = Number(variableForm.installmentCount);

      if (!variableForm.totalAmount || Number(variableForm.totalAmount) <= 0) {
        setError("Informe o valor total da compra.");
        return;
      }
      if (!count || count < 2) {
        setError("O número de parcelas deve ser pelo menos 2.");
        return;
      }
      if (!variableForm.date) {
        setError("Informe a data da primeira parcela.");
        return;
      }

      setSavingVariable(true);
      const amounts = splitAmountInInstallments(variableForm.totalAmount, count);
      const groupId = `parc-${Date.now()}`;

      try {
        for (let i = 0; i < count; i++) {
          await apiFetch("/variable-expenses", {
            method: "POST",
            body: {
              name: variableForm.name,
              amount: amounts[i],
              date: addMonthsIso(variableForm.date, i),
              status: false,
              paymentDate: null,
              description: variableForm.description === "" ? null : variableForm.description,
              installmentGroup: groupId,
              installmentNumber: i + 1,
              totalInstallments: count,
            },
            token,
          });
        }
        closeVariableForm();
        await loadData();
      } catch (err) {
        setError("Não foi possível gerar as parcelas dessa despesa.");
      } finally {
        setSavingVariable(false);
      }
      return;
    }

    setSavingVariable(true);
    try {
      await apiFetch("/variable-expenses", {
        method: "POST",
        body: {
          name: variableForm.name,
          amount: variableForm.amount === "" ? null : Number(variableForm.amount),
          date: variableForm.date === "" ? null : variableForm.date,
          status: false,
          paymentDate: null,
          description: variableForm.description === "" ? null : variableForm.description,
          installmentGroup: null,
          installmentNumber: null,
          totalInstallments: null,
        },
        token,
      });
      closeVariableForm();
      await loadData();
    } catch (err) {
      setError("Não foi possível salvar a despesa variável.");
    } finally {
      setSavingVariable(false);
    }
  }

  // ----- Exclusão -----

  function requestDelete(kind, id) {
    setDeleteTarget({ kind, id });
  }

  async function confirmDelete() {
    const { kind, id } = deleteTarget;
    const path =
      kind === "fixed"
        ? `/fixed-expenses/${id}`
        : kind === "history"
        ? `/fixed-expense-history/${id}`
        : `/variable-expenses/${id}`;

    try {
      await apiFetch(path, { method: "DELETE", token });
      setDeleteTarget(null);
      await loadData();
    } catch (err) {
      setDeleteTarget(null);
      setError(err.message || "Não foi possível excluir a despesa.");
    }
  }

  return (
    <Layout title="Despesas">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div className="flex rounded-lg border border-neutral-300 bg-white p-1">
          <button
            type="button"
            onClick={() => setView("fixas")}
            className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
              view === "fixas" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
            }`}
          >
            Despesas fixas
          </button>
          <button
            type="button"
            onClick={() => setView("variaveis")}
            className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
              view === "variaveis" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
            }`}
          >
            Despesas variáveis
          </button>
        </div>

        {view === "fixas" ? (
          <button
            onClick={openNewFixed}
            className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600"
          >
            + Nova despesa fixa
          </button>
        ) : (
          <button
            onClick={openNewVariable}
            className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600"
          >
            + Nova despesa variável
          </button>
        )}
      </div>

      {error && (
        <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>
      )}

      {view === "fixas" && (
        <>
          <div className="mb-3 flex items-center justify-between">
            <p className="text-sm text-neutral-500">
              {filteredFixed.length} de {fixedExpenses.length} despesa{fixedExpenses.length !== 1 ? "s" : ""}
            </p>
            <button
              type="button"
              onClick={() => setShowFixedHistory((prev) => !prev)}
              className="text-sm font-medium text-neutral-600 hover:text-orange-600"
            >
              {showFixedHistory ? "Ocultar histórico de meses anteriores" : "Ver histórico de meses anteriores"}
            </button>
          </div>

          <div className="mb-4 flex flex-wrap gap-3 rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
            <input
              type="text"
              value={fixedSearch}
              onChange={(e) => setFixedSearch(e.target.value)}
              placeholder="Buscar por nome ou descrição..."
              className="min-w-[220px] flex-1 rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
            />
            <select
              value={fixedFilterStatus}
              onChange={(e) => setFixedFilterStatus(e.target.value)}
              className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
            >
              <option value="">Todos os status</option>
              <option value="true">Pago</option>
              <option value="false">Pendente</option>
            </select>

            <div className="flex items-center gap-2">
              <label className="text-sm text-neutral-500">Vencimento de</label>
              <input
                type="date"
                value={fixedFilterStart}
                onChange={(e) => setFixedFilterStart(e.target.value)}
                className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
              />
              <label className="text-sm text-neutral-500">até</label>
              <input
                type="date"
                value={fixedFilterEnd}
                onChange={(e) => setFixedFilterEnd(e.target.value)}
                className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
              />
              {(fixedFilterStart || fixedFilterEnd) && (
                <button
                  type="button"
                  onClick={() => {
                    setFixedFilterStart("");
                    setFixedFilterEnd("");
                  }}
                  className="text-sm text-neutral-400 hover:text-orange-600"
                >
                  Limpar período
                </button>
              )}
            </div>
          </div>

          <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
            <table className="w-full text-left text-sm">
              <thead className="bg-black text-white">
                <tr>
                  <th className="px-4 py-3 font-medium">Nome</th>
                  <th className="px-4 py-3 font-medium">Valor</th>
                  <th className="px-4 py-3 font-medium">Vencimento</th>
                  <th className="px-4 py-3 font-medium">Pago em</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 font-medium">Ativa</th>
                  <th className="px-4 py-3 font-medium text-right">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {loading ? (
                  <tr>
                    <td colSpan="7" className="px-4 py-8 text-center text-neutral-400">Carregando...</td>
                  </tr>
                ) : fixedExpenses.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="px-4 py-8 text-center text-neutral-400">Nenhuma despesa fixa cadastrada ainda.</td>
                  </tr>
                ) : filteredFixed.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="px-4 py-8 text-center text-neutral-400">Nenhuma despesa encontrada com esse filtro.</td>
                  </tr>
                ) : (
                  filteredFixed.map((expense) => (
                    <tr key={expense.id} className="hover:bg-orange-50/40">
                      <td className="px-4 py-3 font-medium text-neutral-800">{expense.name}</td>
                      <td className="px-4 py-3 text-neutral-600">{formatMoney(expense.amount)}</td>
                      <td className="px-4 py-3 text-neutral-600">{formatDate(expense.dueDate)}</td>
                      <td className="px-4 py-3 text-neutral-600">{formatDate(expense.paymentDate)}</td>
                      <td className="px-4 py-3">
                        <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${STATUS_STYLES[expense.status]}`}>
                          {expense.status ? "Pago" : "Pendente"}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <span
                          className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                            expense.active ? "bg-orange-100 text-orange-700" : "bg-neutral-100 text-neutral-500"
                          }`}
                        >
                          {expense.active ? "Ativa" : "Cancelada"}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right">
                        {expense.active && (
                          <button
                            onClick={() => generateNextMonth(expense)}
                            disabled={generatingId === expense.id}
                            className="mr-3 text-sm font-medium text-neutral-600 hover:text-orange-600 disabled:opacity-50"
                          >
                            {generatingId === expense.id ? "Gerando..." : "Gerar próximo mês"}
                          </button>
                        )}
                        <button
                          onClick={() => openEditFixed(expense)}
                          className="mr-3 text-sm font-medium text-neutral-600 hover:text-orange-600"
                        >
                          Editar
                        </button>
                        <button
                          onClick={() => requestDelete("fixed", expense.id)}
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

          {showFixedHistory && (
            <div className="mt-6">
              <h3 className="mb-3 text-sm font-semibold text-neutral-700">
                Histórico de meses anteriores ({filteredFixedHistory.length})
              </h3>
              <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
                <table className="w-full text-left text-sm">
                  <thead className="bg-black text-white">
                    <tr>
                      <th className="px-4 py-3 font-medium">Nome</th>
                      <th className="px-4 py-3 font-medium">Valor</th>
                      <th className="px-4 py-3 font-medium">Vencimento</th>
                      <th className="px-4 py-3 font-medium">Pago em</th>
                      <th className="px-4 py-3 font-medium">Status</th>
                      <th className="px-4 py-3 font-medium">Descrição</th>
                      <th className="px-4 py-3 font-medium text-right">Ações</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100">
                    {filteredFixedHistory.length === 0 ? (
                      <tr>
                        <td colSpan="7" className="px-4 py-8 text-center text-neutral-400">
                          Nenhum mês anterior encontrado com esse filtro.
                        </td>
                      </tr>
                    ) : (
                      filteredFixedHistory.map((entry) => (
                        <tr key={entry.id} className="hover:bg-orange-50/40">
                          <td className="px-4 py-3 font-medium text-neutral-800">{entry.name}</td>
                          <td className="px-4 py-3 text-neutral-600">{formatMoney(entry.amount)}</td>
                          <td className="px-4 py-3 text-neutral-600">{formatDate(entry.dueDate)}</td>
                          <td className="px-4 py-3 text-neutral-600">{formatDate(entry.paymentDate)}</td>
                          <td className="px-4 py-3">
                            <span
                              className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                                entry.status === "PAGO"
                                  ? "bg-green-100 text-green-700"
                                  : "bg-orange-100 text-orange-700"
                              }`}
                            >
                              {entry.status === "PAGO" ? "Pago" : "Pendente"}
                            </span>
                          </td>
                          <td className="px-4 py-3 text-neutral-600">{entry.description || "-"}</td>
                          <td className="px-4 py-3 text-right">
                            <button
                              onClick={() => requestDelete("history", entry.id)}
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
            </div>
          )}
        </>
      )}

      {view === "variaveis" && (
        <>
          <p className="mb-3 text-sm text-neutral-500">
            {filteredVariable.length} de {variableExpenses.length} despesa{variableExpenses.length !== 1 ? "s" : ""}
          </p>

          <div className="mb-4 flex flex-wrap gap-3 rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
            <input
              type="text"
              value={variableSearch}
              onChange={(e) => setVariableSearch(e.target.value)}
              placeholder="Buscar por nome ou descrição..."
              className="min-w-[220px] flex-1 rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
            />
            <select
              value={variableFilterStatus}
              onChange={(e) => setVariableFilterStatus(e.target.value)}
              className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
            >
              <option value="">Todos os status</option>
              <option value="true">Pago</option>
              <option value="false">Pendente</option>
            </select>

            <div className="flex items-center gap-2">
              <label className="text-sm text-neutral-500">Data de</label>
              <input
                type="date"
                value={variableFilterStart}
                onChange={(e) => setVariableFilterStart(e.target.value)}
                className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
              />
              <label className="text-sm text-neutral-500">até</label>
              <input
                type="date"
                value={variableFilterEnd}
                onChange={(e) => setVariableFilterEnd(e.target.value)}
                className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
              />
              {(variableFilterStart || variableFilterEnd) && (
                <button
                  type="button"
                  onClick={() => {
                    setVariableFilterStart("");
                    setVariableFilterEnd("");
                  }}
                  className="text-sm text-neutral-400 hover:text-orange-600"
                >
                  Limpar período
                </button>
              )}
            </div>
          </div>

          <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
            <table className="w-full text-left text-sm">
              <thead className="bg-black text-white">
                <tr>
                  <th className="px-4 py-3 font-medium">Nome</th>
                  <th className="px-4 py-3 font-medium">Valor</th>
                  <th className="px-4 py-3 font-medium">Data</th>
                  <th className="px-4 py-3 font-medium">Parcela</th>
                  <th className="px-4 py-3 font-medium">Pago em</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 font-medium text-right">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {loading ? (
                  <tr>
                    <td colSpan="7" className="px-4 py-8 text-center text-neutral-400">Carregando...</td>
                  </tr>
                ) : variableExpenses.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="px-4 py-8 text-center text-neutral-400">Nenhuma despesa variável cadastrada ainda.</td>
                  </tr>
                ) : filteredVariable.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="px-4 py-8 text-center text-neutral-400">Nenhuma despesa encontrada com esse filtro.</td>
                  </tr>
                ) : (
                  filteredVariable.map((expense) => (
                    <tr key={expense.id} className="hover:bg-orange-50/40">
                      <td className="px-4 py-3 font-medium text-neutral-800">{expense.name}</td>
                      <td className="px-4 py-3 text-neutral-600">{formatMoney(expense.amount)}</td>
                      <td className="px-4 py-3 text-neutral-600">{formatDate(expense.date)}</td>
                      <td className="px-4 py-3 text-neutral-600">
                        {expense.totalInstallments ? `${expense.installmentNumber}/${expense.totalInstallments}` : "-"}
                      </td>
                      <td className="px-4 py-3 text-neutral-600">{formatDate(expense.paymentDate)}</td>
                      <td className="px-4 py-3">
                        <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${STATUS_STYLES[expense.status]}`}>
                          {expense.status ? "Pago" : "Pendente"}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right">
                        <button
                          onClick={() => openEditVariable(expense)}
                          className="mr-3 text-sm font-medium text-neutral-600 hover:text-orange-600"
                        >
                          Editar
                        </button>
                        <button
                          onClick={() => requestDelete("variable", expense.id)}
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

      {isFixedFormOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="max-h-[90vh] w-full max-w-lg overflow-auto rounded-2xl bg-white p-6 shadow-2xl">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-black">
                {fixedForm.id ? "Editar despesa fixa" : "Nova despesa fixa"}
              </h2>
              <button onClick={closeFixedForm} className="text-neutral-400 hover:text-black" type="button">✕</button>
            </div>

            <form onSubmit={handleFixedSubmit} className="space-y-4">
              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Nome</label>
                <input
                  value={fixedForm.name}
                  onChange={(e) => handleFixedChange("name", e.target.value)}
                  required
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
              </div>

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Valor</label>
                  <input
                    type="number"
                    step="0.01"
                    value={fixedForm.amount}
                    onChange={(e) => handleFixedChange("amount", e.target.value)}
                    required
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Vencimento</label>
                  <input
                    type="date"
                    value={fixedForm.dueDate}
                    onChange={(e) => handleFixedChange("dueDate", e.target.value)}
                    required
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Status</label>
                  <select
                    value={fixedForm.status ? "true" : "false"}
                    onChange={(e) => handleFixedChange("status", e.target.value === "true")}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  >
                    <option value="false">Pendente</option>
                    <option value="true">Pago</option>
                  </select>
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Data de pagamento</label>
                  <input
                    type="date"
                    value={fixedForm.paymentDate}
                    onChange={(e) => handleFixedChange("paymentDate", e.target.value)}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>

                <div className="sm:col-span-2">
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Descrição</label>
                  <input
                    value={fixedForm.description}
                    onChange={(e) => handleFixedChange("description", e.target.value)}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>

                <div className="flex items-center gap-2 pt-1 sm:col-span-2">
                  <input
                    type="checkbox"
                    id="fixedActive"
                    checked={fixedForm.active}
                    onChange={(e) => handleFixedChange("active", e.target.checked)}
                    className="h-4 w-4 rounded border-neutral-300 text-orange-500 focus:ring-orange-400"
                  />
                  <label htmlFor="fixedActive" className="text-sm font-medium text-neutral-700">
                    Despesa ativa (continua se repetindo todo mês)
                  </label>
                </div>
              </div>

              <div className="flex justify-end gap-3 border-t border-neutral-100 pt-4">
                <button type="button" onClick={closeFixedForm} className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100">
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={savingFixed}
                  className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-60"
                >
                  {savingFixed ? "Salvando..." : fixedForm.id ? "Salvar alterações" : "Cadastrar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {isVariableFormOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="max-h-[90vh] w-full max-w-lg overflow-auto rounded-2xl bg-white p-6 shadow-2xl">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-black">
                {variableForm.id ? "Editar despesa variável" : "Nova despesa variável"}
              </h2>
              <button onClick={closeVariableForm} className="text-neutral-400 hover:text-black" type="button">✕</button>
            </div>

            {variableForm.id && variableForm.totalInstallments && (
              <div className="mb-4 rounded-lg bg-neutral-100 px-3 py-2 text-sm text-neutral-600">
                Parcela {variableForm.installmentNumber} de {variableForm.totalInstallments}. Editar aqui muda
                só essa parcela, não as outras do mesmo parcelamento.
              </div>
            )}

            <form onSubmit={handleVariableSubmit} className="space-y-4">
              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Nome</label>
                <input
                  value={variableForm.name}
                  onChange={(e) => handleVariableChange("name", e.target.value)}
                  required
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
              </div>

              {!variableForm.id && (
                <div className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    id="isInstallment"
                    checked={variableForm.isInstallment}
                    onChange={(e) => handleVariableChange("isInstallment", e.target.checked)}
                    className="h-4 w-4 rounded border-neutral-300 text-orange-500 focus:ring-orange-400"
                  />
                  <label htmlFor="isInstallment" className="text-sm font-medium text-neutral-700">
                    É uma compra parcelada?
                  </label>
                </div>
              )}

              {!variableForm.id && variableForm.isInstallment ? (
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <div>
                    <label className="mb-1 block text-sm font-medium text-neutral-700">Valor total da compra</label>
                    <input
                      type="number"
                      step="0.01"
                      value={variableForm.totalAmount}
                      onChange={(e) => handleVariableChange("totalAmount", e.target.value)}
                      required
                      className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                    />
                  </div>

                  <div>
                    <label className="mb-1 block text-sm font-medium text-neutral-700">Número de parcelas</label>
                    <input
                      type="number"
                      min="2"
                      value={variableForm.installmentCount}
                      onChange={(e) => handleVariableChange("installmentCount", e.target.value)}
                      required
                      className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                    />
                  </div>

                  <div className="sm:col-span-2">
                    <label className="mb-1 block text-sm font-medium text-neutral-700">Data da 1ª parcela</label>
                    <input
                      type="date"
                      value={variableForm.date}
                      onChange={(e) => handleVariableChange("date", e.target.value)}
                      required
                      className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                    />
                  </div>
                </div>
              ) : (
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <div>
                    <label className="mb-1 block text-sm font-medium text-neutral-700">Valor</label>
                    <input
                      type="number"
                      step="0.01"
                      value={variableForm.amount}
                      onChange={(e) => handleVariableChange("amount", e.target.value)}
                      required
                      className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                    />
                  </div>

                  <div>
                    <label className="mb-1 block text-sm font-medium text-neutral-700">Data</label>
                    <input
                      type="date"
                      value={variableForm.date}
                      onChange={(e) => handleVariableChange("date", e.target.value)}
                      required
                      className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                    />
                  </div>

                  {variableForm.id && (
                    <>
                      <div>
                        <label className="mb-1 block text-sm font-medium text-neutral-700">Status</label>
                        <select
                          value={variableForm.status ? "true" : "false"}
                          onChange={(e) => handleVariableChange("status", e.target.value === "true")}
                          className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                        >
                          <option value="false">Pendente</option>
                          <option value="true">Pago</option>
                        </select>
                      </div>

                      <div>
                        <label className="mb-1 block text-sm font-medium text-neutral-700">Data de pagamento</label>
                        <input
                          type="date"
                          value={variableForm.paymentDate}
                          onChange={(e) => handleVariableChange("paymentDate", e.target.value)}
                          className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                        />
                      </div>
                    </>
                  )}
                </div>
              )}

              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Descrição</label>
                <input
                  value={variableForm.description}
                  onChange={(e) => handleVariableChange("description", e.target.value)}
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
              </div>

              <div className="flex justify-end gap-3 border-t border-neutral-100 pt-4">
                <button type="button" onClick={closeVariableForm} className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100">
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={savingVariable}
                  className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-60"
                >
                  {savingVariable ? "Salvando..." : variableForm.id ? "Salvar alterações" : "Cadastrar"}
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
        itemLabel="esta despesa"
      />
    </Layout>
  );
}
