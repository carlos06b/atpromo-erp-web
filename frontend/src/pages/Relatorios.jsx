import { useEffect, useState } from "react";
import { apiFetch } from "../api";
import { useAuth } from "../context/AuthContext";
import Layout from "../components/Layout";

const INVOICE_STATUS_STYLES = {
  PAGO: "bg-green-100 text-green-700",
  FATURADO: "bg-blue-100 text-blue-700",
  PENDENTE: "bg-orange-100 text-orange-700",
  CANCELADO: "bg-red-50 text-red-600",
};

const FIXED_HISTORY_STATUS_STYLES = {
  PAGO: "bg-green-100 text-green-700",
  PENDENTE: "bg-orange-100 text-orange-700",
};

const VARIABLE_STATUS_STYLES = {
  true: "bg-green-100 text-green-700",
  false: "bg-orange-100 text-orange-700",
};

const LAUNCH_TYPE_STYLES = {
  DESCONTO: "bg-red-50 text-red-600",
  BONUS: "bg-green-100 text-green-700",
};

const TYPE_LABELS = {
  DESCONTO: "Desconto",
  BONUS: "Bônus",
  BONIFICACAO: "Bonificação",
  AJUDA_CUSTO: "Ajuda de Custo",
  ASO: "ASO",
  EPI: "EPI",
  RESCISAO: "Rescisão",
  FERIAS: "Férias",
  ADIANTAMENTO: "Adiantamento",
  REEMBOLSO: "Reembolso",
  CORRECAO_PAGAMENTO: "Correção de Pagamento",
  OUTROS: "Outros",
};

function typeLabel(type) {
  return TYPE_LABELS[type] || type || "-";
}

const CATEGORY_COLORS = {
  promoters: "#2a78d6",
  fixed: "#1baf7a",
  variable: "#4a3aa7",
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

function toIso(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function getMonthRange(monthOffset = 0) {
  const now = new Date();
  const first = new Date(now.getFullYear(), now.getMonth() + monthOffset, 1);
  const last = new Date(now.getFullYear(), now.getMonth() + monthOffset + 1, 0);
  return { start: toIso(first), end: toIso(last) };
}

function getYearRange() {
  const now = new Date();
  const first = new Date(now.getFullYear(), 0, 1);
  const last = new Date(now.getFullYear(), 11, 31);
  return { start: toIso(first), end: toIso(last) };
}

function BarList({ items, emptyLabel }) {
  if (items.length === 0) {
    return <p className="py-6 text-center text-sm text-neutral-400">{emptyLabel}</p>;
  }

  const maxValue = Math.max(1, ...items.map((item) => item.value));

  return (
    <div className="space-y-3">
      {items.map((item) => (
        <div key={item.label} className="flex items-center gap-3">
          <div className="w-36 flex-shrink-0 truncate text-sm text-neutral-700 sm:w-44" title={item.label}>
            {item.label}
          </div>
          <div className="h-5 flex-1 rounded-full bg-neutral-100">
            <div
              className="h-5 rounded-r-full"
              style={{
                width: `${Math.max(2, (item.value / maxValue) * 100)}%`,
                backgroundColor: item.color || CATEGORY_COLORS.promoters,
              }}
            />
          </div>
          <div className="w-28 flex-shrink-0 text-right text-sm font-medium text-neutral-800">
            {formatMoney(item.value)}
          </div>
        </div>
      ))}
    </div>
  );
}

function StatTile({ label, value, tone = "neutral" }) {
  const toneClasses = {
    neutral: "text-neutral-800",
    good: "text-green-700",
    bad: "text-red-600",
    warn: "text-orange-600",
  };

  return (
    <div className="rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
      <p className="text-sm text-neutral-500">{label}</p>
      <p className={`mt-1 text-2xl font-semibold ${toneClasses[tone]}`}>{formatMoney(value)}</p>
    </div>
  );
}

export default function Relatorios() {
  const { token } = useAuth();
  const [view, setView] = useState("resumo");

  const initialRange = getMonthRange(0);
  const [rangeStart, setRangeStart] = useState(initialRange.start);
  const [rangeEnd, setRangeEnd] = useState(initialRange.end);
  const [activePreset, setActivePreset] = useState("mes-atual");

  const [generalReport, setGeneralReport] = useState(null);
  const [incomeReport, setIncomeReport] = useState(null);
  const [expenseReport, setExpenseReport] = useState(null);
  const [typeReport, setTypeReport] = useState(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  async function loadReports(start, end) {
    setLoading(true);
    setError("");

    try {
      const query = `start=${start}&end=${end}`;
      const [general, income, expense, byType] = await Promise.all([
        apiFetch(`/reports/general?${query}`, { token }),
        apiFetch(`/reports/income?${query}`, { token }),
        apiFetch(`/reports/expenses?${query}`, { token }),
        apiFetch(`/reports/by-type?${query}`, { token }),
      ]);
      setGeneralReport(general);
      setIncomeReport(income);
      setExpenseReport(expense);
      setTypeReport(byType);
    } catch (err) {
      setError("Não foi possível carregar o relatório para esse período.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadReports(initialRange.start, initialRange.end);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function applyPreset(preset) {
    setActivePreset(preset);

    let range;
    if (preset === "mes-atual") range = getMonthRange(0);
    else if (preset === "mes-passado") range = getMonthRange(-1);
    else range = getYearRange();

    setRangeStart(range.start);
    setRangeEnd(range.end);
    loadReports(range.start, range.end);
  }

  function handleManualSubmit(event) {
    event.preventDefault();

    if (!rangeStart || !rangeEnd) {
      setError("Selecione a data inicial e a data final.");
      return;
    }
    if (rangeStart > rangeEnd) {
      setError("A data inicial não pode ser maior que a data final.");
      return;
    }

    setActivePreset("personalizado");
    loadReports(rangeStart, rangeEnd);
  }

  const expenseCategoryItems = generalReport
    ? [
        { label: "Pagamentos a promotores", value: Number(generalReport.promoterExpenses) || 0, color: CATEGORY_COLORS.promoters },
        { label: "Despesas fixas", value: Number(generalReport.fixedExpenses) || 0, color: CATEGORY_COLORS.fixed },
        { label: "Despesas variáveis", value: Number(generalReport.variableExpenses) || 0, color: CATEGORY_COLORS.variable },
      ].filter((item) => item.value > 0)
    : [];

  const companyEntries = Object.entries(typeReport?.incomeByCompany || {})
    .map(([label, value]) => ({ label, value: Number(value) || 0 }))
    .filter((item) => item.value > 0)
    .sort((a, b) => b.value - a.value);

  const topCompanies = companyEntries.slice(0, 8);
  const othersTotal = companyEntries.slice(8).reduce((sum, item) => sum + item.value, 0);
  const companyChartItems =
    othersTotal > 0 ? [...topCompanies, { label: "Outros clientes", value: othersTotal }] : topCompanies;

  const realResult = generalReport ? Number(generalReport.realResult) : 0;
  const expectedResult = generalReport ? Number(generalReport.expectedResult) : 0;

  return (
    <Layout title="Relatórios">
      <div className="mb-4 flex flex-wrap items-end gap-3 rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
        <div className="flex rounded-lg border border-neutral-300 bg-white p-1">
          <button
            type="button"
            onClick={() => applyPreset("mes-atual")}
            className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
              activePreset === "mes-atual" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
            }`}
          >
            Este mês
          </button>
          <button
            type="button"
            onClick={() => applyPreset("mes-passado")}
            className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
              activePreset === "mes-passado" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
            }`}
          >
            Mês passado
          </button>
          <button
            type="button"
            onClick={() => applyPreset("ano-atual")}
            className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
              activePreset === "ano-atual" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
            }`}
          >
            Este ano
          </button>
        </div>

        <form onSubmit={handleManualSubmit} className="flex flex-wrap items-end gap-3">
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

          <button
            type="submit"
            disabled={loading}
            className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-60"
          >
            {loading ? "Gerando..." : "Gerar relatório"}
          </button>
        </form>
      </div>

      {error && <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}

      <div className="mb-6 flex rounded-lg border border-neutral-300 bg-white p-1 sm:w-fit">
        <button
          type="button"
          onClick={() => setView("resumo")}
          className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
            view === "resumo" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
          }`}
        >
          Resumo
        </button>
        <button
          type="button"
          onClick={() => setView("faturamento")}
          className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
            view === "faturamento" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
          }`}
        >
          Faturamento
        </button>
        <button
          type="button"
          onClick={() => setView("despesas")}
          className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
            view === "despesas" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
          }`}
        >
          Despesas
        </button>
      </div>

      {loading && !generalReport ? (
        <div className="rounded-xl border border-neutral-200 bg-white px-4 py-12 text-center text-sm text-neutral-400 shadow-sm">
          Carregando relatório...
        </div>
      ) : !generalReport ? (
        <div className="rounded-xl border border-neutral-200 bg-white px-4 py-12 text-center text-sm text-neutral-400 shadow-sm">
          Escolha um período para gerar o relatório.
        </div>
      ) : (
        <>
          {view === "resumo" && (
            <div>
              <div className="mb-4 rounded-xl border border-neutral-200 bg-white p-6 shadow-sm">
                <p className="text-sm text-neutral-500">Resultado do período (recebido − despesas)</p>
                <p className={`mt-1 text-4xl font-semibold ${realResult >= 0 ? "text-green-700" : "text-red-600"}`}>
                  {formatMoney(realResult)}
                </p>
                <p className="mt-2 text-sm text-neutral-400">
                  Se tudo que está em aberto for recebido, o resultado fica em{" "}
                  <span className={expectedResult >= 0 ? "text-green-700" : "text-red-600"}>
                    {formatMoney(expectedResult)}
                  </span>
                  .
                </p>
              </div>

              <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
                <StatTile label="Recebido no período" value={generalReport.receivedIncome} tone="good" />
                <StatTile label="A receber (em aberto)" value={generalReport.openIncome} tone="warn" />
                <StatTile label="Despesas totais" value={generalReport.totalExpenses} tone="bad" />
              </div>

              <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
                <div className="rounded-xl border border-neutral-200 bg-white p-5 shadow-sm">
                  <h3 className="mb-4 text-sm font-semibold text-neutral-700">Despesas por categoria</h3>
                  <BarList items={expenseCategoryItems} emptyLabel="Nenhuma despesa nesse período." />
                </div>

                <div className="rounded-xl border border-neutral-200 bg-white p-5 shadow-sm">
                  <h3 className="mb-4 text-sm font-semibold text-neutral-700">Faturamento por cliente</h3>
                  <BarList items={companyChartItems} emptyLabel="Nenhum faturamento nesse período." />
                </div>
              </div>
            </div>
          )}

          {view === "faturamento" && incomeReport && (
            <div className="space-y-6">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-4">
                <StatTile label="Previsto (a vencer)" value={incomeReport.expectedIncome} />
                <StatTile label="Faturado" value={incomeReport.issuedIncome} />
                <StatTile label="Recebido" value={incomeReport.receivedIncome} tone="good" />
                <StatTile label="Em aberto" value={incomeReport.openIncome} tone="warn" />
              </div>

              <div>
                <h3 className="mb-3 text-sm font-semibold text-neutral-700">Faturas com vencimento no período</h3>
                <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-black text-white">
                      <tr>
                        <th className="px-4 py-3 font-medium">Cliente</th>
                        <th className="px-4 py-3 font-medium">Grupo</th>
                        <th className="px-4 py-3 font-medium">Valor</th>
                        <th className="px-4 py-3 font-medium">Vencimento</th>
                        <th className="px-4 py-3 font-medium">Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-100">
                      {incomeReport.invoicesByDueDate.length === 0 ? (
                        <tr>
                          <td colSpan="5" className="px-4 py-8 text-center text-neutral-400">
                            Nenhuma fatura com vencimento nesse período.
                          </td>
                        </tr>
                      ) : (
                        incomeReport.invoicesByDueDate.map((invoice) => (
                          <tr key={invoice.id} className="hover:bg-orange-50/40">
                            <td className="px-4 py-3 font-medium text-neutral-800">{invoice.clientName || "-"}</td>
                            <td className="px-4 py-3 text-neutral-600">{invoice.companyLink || "-"}</td>
                            <td className="px-4 py-3 text-neutral-600">{formatMoney(invoice.amount)}</td>
                            <td className="px-4 py-3 text-neutral-600">{formatDate(invoice.dueDate)}</td>
                            <td className="px-4 py-3">
                              <span
                                className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                                  INVOICE_STATUS_STYLES[invoice.status] || "bg-neutral-100 text-neutral-500"
                                }`}
                              >
                                {invoice.status || "-"}
                              </span>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              <div>
                <h3 className="mb-3 text-sm font-semibold text-neutral-700">Faturas recebidas no período</h3>
                <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-black text-white">
                      <tr>
                        <th className="px-4 py-3 font-medium">Cliente</th>
                        <th className="px-4 py-3 font-medium">Grupo</th>
                        <th className="px-4 py-3 font-medium">Valor recebido</th>
                        <th className="px-4 py-3 font-medium">Pago em</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-100">
                      {incomeReport.receivedInvoices.length === 0 ? (
                        <tr>
                          <td colSpan="4" className="px-4 py-8 text-center text-neutral-400">
                            Nenhuma fatura recebida nesse período.
                          </td>
                        </tr>
                      ) : (
                        incomeReport.receivedInvoices.map((invoice) => (
                          <tr key={invoice.id} className="hover:bg-orange-50/40">
                            <td className="px-4 py-3 font-medium text-neutral-800">{invoice.clientName || "-"}</td>
                            <td className="px-4 py-3 text-neutral-600">{invoice.companyLink || "-"}</td>
                            <td className="px-4 py-3 text-green-700">
                              {formatMoney(invoice.receivedAmount ?? invoice.amount)}
                            </td>
                            <td className="px-4 py-3 text-neutral-600">{formatDate(invoice.paymentDate)}</td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {view === "despesas" && expenseReport && (
            <div className="space-y-6">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                <StatTile label="Pagamentos a promotores" value={expenseReport.promoterExpenses} />
                <StatTile label="Despesas fixas" value={expenseReport.fixedExpenses} />
                <StatTile label="Despesas variáveis" value={expenseReport.variableExpenses} />
              </div>

              <div>
                <h3 className="mb-3 text-sm font-semibold text-neutral-700">Pagamentos a promotores (bônus e similares)</h3>
                <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-black text-white">
                      <tr>
                        <th className="px-4 py-3 font-medium">Promotor</th>
                        <th className="px-4 py-3 font-medium">Tipo</th>
                        <th className="px-4 py-3 font-medium">Valor</th>
                        <th className="px-4 py-3 font-medium">Data</th>
                        <th className="px-4 py-3 font-medium">Descrição</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-100">
                      {expenseReport.promoterPayments.length === 0 ? (
                        <tr>
                          <td colSpan="5" className="px-4 py-8 text-center text-neutral-400">
                            Nenhum pagamento a promotor nesse período.
                          </td>
                        </tr>
                      ) : (
                        expenseReport.promoterPayments.map((line) => (
                          <tr key={line.id} className="hover:bg-orange-50/40">
                            <td className="px-4 py-3 font-medium text-neutral-800">{line.promoterName || "-"}</td>
                            <td className="px-4 py-3">
                              <span
                                className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                                  LAUNCH_TYPE_STYLES[line.type] || "bg-neutral-100 text-neutral-500"
                                }`}
                              >
                                {typeLabel(line.type)}
                              </span>
                            </td>
                            <td className="px-4 py-3 text-neutral-600">{formatMoney(line.amount)}</td>
                            <td className="px-4 py-3 text-neutral-600">{formatDate(line.date)}</td>
                            <td className="px-4 py-3 text-neutral-600">{line.description || "-"}</td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              <div>
                <h3 className="mb-3 text-sm font-semibold text-neutral-700">Descontos aplicados</h3>
                <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-black text-white">
                      <tr>
                        <th className="px-4 py-3 font-medium">Promotor</th>
                        <th className="px-4 py-3 font-medium">Valor</th>
                        <th className="px-4 py-3 font-medium">Data</th>
                        <th className="px-4 py-3 font-medium">Descrição</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-100">
                      {expenseReport.discountEntries.length === 0 ? (
                        <tr>
                          <td colSpan="4" className="px-4 py-8 text-center text-neutral-400">
                            Nenhum desconto aplicado nesse período.
                          </td>
                        </tr>
                      ) : (
                        expenseReport.discountEntries.map((line) => (
                          <tr key={line.id} className="hover:bg-orange-50/40">
                            <td className="px-4 py-3 font-medium text-neutral-800">{line.promoterName || "-"}</td>
                            <td className="px-4 py-3 text-red-600">{formatMoney(line.amount)}</td>
                            <td className="px-4 py-3 text-neutral-600">{formatDate(line.date)}</td>
                            <td className="px-4 py-3 text-neutral-600">{line.description || "-"}</td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              <div>
                <h3 className="mb-3 text-sm font-semibold text-neutral-700">Despesas fixas pagas no período</h3>
                <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-black text-white">
                      <tr>
                        <th className="px-4 py-3 font-medium">Nome</th>
                        <th className="px-4 py-3 font-medium">Valor</th>
                        <th className="px-4 py-3 font-medium">Vencimento</th>
                        <th className="px-4 py-3 font-medium">Status</th>
                        <th className="px-4 py-3 font-medium">Pago em</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-100">
                      {expenseReport.fixedExpenseHistory.length === 0 ? (
                        <tr>
                          <td colSpan="5" className="px-4 py-8 text-center text-neutral-400">
                            Nenhuma despesa fixa nesse período.
                          </td>
                        </tr>
                      ) : (
                        expenseReport.fixedExpenseHistory.map((expense) => (
                          <tr key={expense.id} className="hover:bg-orange-50/40">
                            <td className="px-4 py-3 font-medium text-neutral-800">{expense.name}</td>
                            <td className="px-4 py-3 text-neutral-600">{formatMoney(expense.amount)}</td>
                            <td className="px-4 py-3 text-neutral-600">{formatDate(expense.dueDate)}</td>
                            <td className="px-4 py-3">
                              <span
                                className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                                  FIXED_HISTORY_STATUS_STYLES[expense.status] || "bg-neutral-100 text-neutral-500"
                                }`}
                              >
                                {expense.status || "-"}
                              </span>
                            </td>
                            <td className="px-4 py-3 text-neutral-600">{formatDate(expense.paymentDate)}</td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              <div>
                <h3 className="mb-3 text-sm font-semibold text-neutral-700">Despesas variáveis pagas no período</h3>
                <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-black text-white">
                      <tr>
                        <th className="px-4 py-3 font-medium">Nome</th>
                        <th className="px-4 py-3 font-medium">Valor</th>
                        <th className="px-4 py-3 font-medium">Data</th>
                        <th className="px-4 py-3 font-medium">Parcela</th>
                        <th className="px-4 py-3 font-medium">Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-100">
                      {expenseReport.variableExpenseList.length === 0 ? (
                        <tr>
                          <td colSpan="5" className="px-4 py-8 text-center text-neutral-400">
                            Nenhuma despesa variável nesse período.
                          </td>
                        </tr>
                      ) : (
                        expenseReport.variableExpenseList.map((expense) => (
                          <tr key={expense.id} className="hover:bg-orange-50/40">
                            <td className="px-4 py-3 font-medium text-neutral-800">{expense.name}</td>
                            <td className="px-4 py-3 text-neutral-600">{formatMoney(expense.amount)}</td>
                            <td className="px-4 py-3 text-neutral-600">{formatDate(expense.date)}</td>
                            <td className="px-4 py-3 text-neutral-600">
                              {expense.totalInstallments ? `${expense.installmentNumber}/${expense.totalInstallments}` : "-"}
                            </td>
                            <td className="px-4 py-3">
                              <span
                                className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                                  VARIABLE_STATUS_STYLES[expense.status]
                                }`}
                              >
                                {expense.status ? "Pago" : "Pendente"}
                              </span>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </Layout>
  );
}
