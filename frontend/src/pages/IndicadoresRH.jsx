import { useEffect, useState } from "react";
import { apiFetch } from "../api";
import { useAuth } from "../context/AuthContext";
import Layout from "../components/Layout";

const MONTH_OPTIONS = [
  { value: 1, label: "Janeiro" },
  { value: 2, label: "Fevereiro" },
  { value: 3, label: "Março" },
  { value: 4, label: "Abril" },
  { value: 5, label: "Maio" },
  { value: 6, label: "Junho" },
  { value: 7, label: "Julho" },
  { value: 8, label: "Agosto" },
  { value: 9, label: "Setembro" },
  { value: 10, label: "Outubro" },
  { value: 11, label: "Novembro" },
  { value: 12, label: "Dezembro" },
];

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

function formatDate(value) {
  if (!value) return "-";
  const [year, month, day] = value.split("-");
  return `${day}/${month}/${year}`;
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
      <p className={`mt-1 text-2xl font-semibold ${toneClasses[tone]}`}>{value}</p>
    </div>
  );
}

function CountBarList({ items, emptyLabel }) {
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
              className="h-5 rounded-r-full bg-orange-400"
              style={{ width: `${Math.max(2, (item.value / maxValue) * 100)}%` }}
            />
          </div>
          <div className="w-12 flex-shrink-0 text-right text-sm font-medium text-neutral-800">{item.value}</div>
        </div>
      ))}
    </div>
  );
}

export default function IndicadoresRH() {
  const { token } = useAuth();

  const initialRange = getMonthRange(0);
  const [rangeStart, setRangeStart] = useState(initialRange.start);
  const [rangeEnd, setRangeEnd] = useState(initialRange.end);
  const [activePreset, setActivePreset] = useState("mes-atual");
  const [birthdayMonth, setBirthdayMonth] = useState(new Date().getMonth() + 1);

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  async function loadIndicators(start, end, month) {
    setLoading(true);
    setError("");

    try {
      const result = await apiFetch(`/hr-indicators?start=${start}&end=${end}&birthdayMonth=${month}`, { token });
      setData(result);
    } catch (err) {
      setError("Não foi possível carregar os indicadores para esse período.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadIndicators(initialRange.start, initialRange.end, birthdayMonth);
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
    loadIndicators(range.start, range.end, birthdayMonth);
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
    loadIndicators(rangeStart, rangeEnd, birthdayMonth);
  }

  function handleBirthdayMonthChange(value) {
    setBirthdayMonth(value);
    loadIndicators(rangeStart, rangeEnd, value);
  }

  const byTypeItems = Object.entries(data?.byType || {})
    .map(([label, value]) => ({ label, value: Number(value) || 0 }))
    .sort((a, b) => b.value - a.value);

  const byStoreItems = Object.entries(data?.byStore || {})
    .map(([label, value]) => ({ label, value: Number(value) || 0 }))
    .sort((a, b) => b.value - a.value);

  const byCompanyLinkItems = Object.entries(data?.byCompanyLink || {})
    .map(([label, value]) => ({ label, value: Number(value) || 0 }))
    .sort((a, b) => b.value - a.value);

  return (
    <Layout title="Indicadores RH">
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
            className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600"
          >
            Aplicar
          </button>
        </form>
      </div>

      {error && (
        <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>
      )}

      {loading ? (
        <p className="py-8 text-center text-neutral-400">Carregando...</p>
      ) : (
        <>
          <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatTile label="Promotores ativos" value={data?.totalActive ?? 0} />
            <StatTile label="Admitidos no período" value={data?.admittedInPeriod ?? 0} tone="good" />
            <StatTile label="Desligados no período" value={data?.terminatedInPeriod ?? 0} tone="bad" />
            <StatTile
              label="Taxa de turnover"
              value={data?.turnoverRate != null ? `${data.turnoverRate}%` : "-"}
              tone="warn"
            />
          </div>

          <div className="mb-6 grid grid-cols-1 gap-4 lg:grid-cols-3">
            <div className="rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
              <h3 className="mb-3 text-sm font-semibold text-neutral-700">Quadro ativo por tipo</h3>
              <CountBarList items={byTypeItems} emptyLabel="Nenhum promotor ativo." />
            </div>

            <div className="rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
              <h3 className="mb-3 text-sm font-semibold text-neutral-700">Quadro ativo por loja</h3>
              <CountBarList items={byStoreItems} emptyLabel="Nenhum promotor ativo." />
            </div>

            <div className="rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
              <h3 className="mb-3 text-sm font-semibold text-neutral-700">Quadro ativo por vínculo</h3>
              <CountBarList items={byCompanyLinkItems} emptyLabel="Nenhum promotor ativo." />
            </div>
          </div>

          <div className="rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
            <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-sm font-semibold text-neutral-700">Aniversariantes do mês</h3>
              <select
                value={birthdayMonth}
                onChange={(e) => handleBirthdayMonthChange(Number(e.target.value))}
                className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
              >
                {MONTH_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>

            {(data?.birthdays || []).length === 0 ? (
              <p className="py-6 text-center text-sm text-neutral-400">Nenhum aniversariante nesse mês.</p>
            ) : (
              <div className="overflow-hidden rounded-lg border border-neutral-200">
                <table className="w-full text-left text-sm">
                  <thead className="bg-black text-white">
                    <tr>
                      <th className="px-4 py-3 font-medium">Nome</th>
                      <th className="px-4 py-3 font-medium">Aniversário</th>
                      <th className="px-4 py-3 font-medium">Loja</th>
                      <th className="px-4 py-3 font-medium">Tipo</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100">
                    {data.birthdays.map((entry) => (
                      <tr key={entry.id} className="hover:bg-orange-50/40">
                        <td className="px-4 py-3 font-medium text-neutral-800">{entry.name}</td>
                        <td className="px-4 py-3 text-neutral-600">{formatDate(entry.dateBirth)}</td>
                        <td className="px-4 py-3 text-neutral-600">{entry.store || "-"}</td>
                        <td className="px-4 py-3 text-neutral-600">{entry.type || "-"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}
    </Layout>
  );
}
