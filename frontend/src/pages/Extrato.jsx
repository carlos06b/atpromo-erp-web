import { useEffect, useMemo, useState } from "react";
import { apiFetch, API_BASE_URL } from "../api";
import { useAuth } from "../context/AuthContext";
import Layout from "../components/Layout";
import ConfirmDeleteDialog from "../components/ConfirmDeleteDialog";
import CurrencyInput from "../components/CurrencyInput";

function formatDate(value) {
  if (!value) return "-";
  const [year, month, day] = value.split("-");
  return `${day}/${month}/${year}`;
}

function formatCurrency(value) {
  if (value == null) return "-";
  return Number(value).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

// Convenção contábil brasileira: negativo em vermelho entre parênteses.
function formatSaldoText(value) {
  if (value == null) return "-";
  const num = Number(value);
  const formatted = Math.abs(num).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
  return num < 0 ? `(${formatted})` : formatted;
}

function saldoClass(value) {
  if (value == null) return "text-neutral-800";
  return Number(value) < 0 ? "text-red-600" : "text-neutral-800";
}

function formatSaldoCompact(value) {
  const num = Number(value) || 0;
  const formatted = Math.abs(num).toLocaleString("pt-BR", { maximumFractionDigits: 0 });
  return `${num < 0 ? "-" : ""}R$ ${formatted}`;
}

function todayIso() {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${now.getFullYear()}-${month}-${day}`;
}

function accountInitials(name) {
  if (!name) return "?";
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

const BADGE_COLORS = [
  "bg-orange-500",
  "bg-blue-500",
  "bg-emerald-500",
  "bg-violet-500",
  "bg-rose-500",
  "bg-amber-500",
];

function badgeColor(index) {
  return BADGE_COLORS[index % BADGE_COLORS.length];
}

const ORIGEM_LABELS = {
  MANUAL: "Lançamento manual",
  TRANSFERENCIA: "Transferência",
  SOLICITACAO_PIX: "Pix de solicitação",
  DESPESA_FIXA: "Despesa fixa",
  DESPESA_VARIAVEL: "Despesa variável",
  FATURAMENTO: "Faturamento",
};

function origemLabel(lancamento) {
  const base = ORIGEM_LABELS[lancamento.origemTipo] || lancamento.origemTipo;
  if (lancamento.origemTipo === "TRANSFERENCIA" && lancamento.contaRelacionadaNome) {
    const seta = lancamento.tipo === "SAIDA" ? "→" : "←";
    return `${base} ${seta} ${lancamento.contaRelacionadaNome}`;
  }
  return base;
}

const EMPTY_EMPRESA_FORM = { id: null, nome: "", cnpj: "", ativa: true };

const EMPTY_CONTA_FORM = {
  id: null,
  empresaId: "",
  banco: "",
  apelido: "",
  agencia: "",
  numeroConta: "",
  saldoInicial: "",
  dataSaldoInicial: "",
  ativa: true,
};

const EMPTY_LANCAMENTO_FORM = {
  id: null,
  contaBancariaId: "",
  data: todayIso(),
  tipo: "SAIDA",
  valor: "",
  descricao: "",
  beneficiarioId: "",
  categoriaId: "",
  centroCustoId: "",
};

const EMPTY_TRANSFERENCIA_FORM = {
  contaDestinoId: "",
  data: todayIso(),
  valor: "",
  descricao: "",
};

const EMPTY_BENEFICIARIO_FORM = { id: null, nome: "", ativo: true };
const EMPTY_CATEGORIA_FORM = { id: null, nome: "", ativo: true };
const EMPTY_CENTRO_CUSTO_FORM = { id: null, nome: "", ativo: true };

// Formulário genérico pro modal "Lançar no extrato", reaproveitado pelas 4
// origens automáticas (Pix, despesa fixa, despesa variável, faturamento).
// beneficiarioId fica vazio quando a origem resolve isso sozinha (Pix sempre;
// faturamento quando o campo é deixado em branco).
const EMPTY_LANCAR_FORM = { contaBancariaId: "", beneficiarioId: "", categoriaId: "", centroCustoId: "" };

// Mesmos rótulos usados em Solicitações, só pra mostrar o tipo do Pix de um
// jeito legível na fila de pendentes (o backend manda o código, ex: "BONIFICACAO").
const SOLICITACAO_TYPE_LABELS = {
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

function solicitacaoTypeLabel(tipo) {
  return SOLICITACAO_TYPE_LABELS[tipo] || tipo;
}

// Rótulo + estilo de cada origem automática, usados na fila unificada de
// "Lançamentos pendentes" (um Pix, uma despesa fixa, uma despesa variável ou
// um faturamento recebido — todos esperando alguém escolher a conta bancária).
const PENDENTE_ORIGEM_INFO = {
  SOLICITACAO_PIX: { label: "Pix", badgeClass: "bg-orange-100 text-orange-700" },
  DESPESA_FIXA: { label: "Despesa fixa", badgeClass: "bg-red-100 text-red-700" },
  DESPESA_VARIAVEL: { label: "Despesa variável", badgeClass: "bg-red-100 text-red-700" },
  FATURAMENTO: { label: "Faturamento", badgeClass: "bg-green-100 text-green-700" },
};

// Combobox pesquisável com opção de cadastrar um novo item na hora, pros
// catálogos reutilizáveis (beneficiário, categoria, centro de custo) — cresce
// com o uso, sem precisar abrir uma tela de cadastro à parte toda vez.
function CatalogCombobox({ items, value, onChange, onCreate, placeholder, allowClear = true, clearLabel = "Nenhum" }) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [creating, setCreating] = useState(false);

  const selected = items.find((item) => String(item.id) === String(value));
  const displayValue = open ? search : selected?.nome || "";

  const normalizedSearch = search.trim().toLowerCase();
  const filtered = items
    .filter((item) => item.nome.toLowerCase().includes(normalizedSearch))
    .sort((a, b) => a.nome.localeCompare(b.nome));
  const exactMatch = items.some((item) => item.nome.trim().toLowerCase() === normalizedSearch);
  const canCreate = Boolean(onCreate) && normalizedSearch !== "" && !exactMatch;

  function selectItem(item) {
    onChange(item ? item.id : "");
    setSearch("");
    setOpen(false);
  }

  async function handleCreate() {
    const nome = search.trim();
    if (!nome || creating) return;
    setCreating(true);
    try {
      const created = await onCreate(nome);
      if (created) {
        onChange(created.id);
      }
      setSearch("");
      setOpen(false);
    } finally {
      setCreating(false);
    }
  }

  return (
    <div className="relative">
      <input
        type="text"
        value={displayValue}
        onChange={(e) => {
          setSearch(e.target.value);
          if (!open) setOpen(true);
        }}
        onFocus={() => {
          setSearch("");
          setOpen(true);
        }}
        onBlur={() => setTimeout(() => setOpen(false), 150)}
        placeholder={placeholder}
        className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
      />
      {open && (
        <div className="absolute z-10 mt-1 max-h-48 w-full overflow-y-auto rounded-lg border border-neutral-200 bg-white shadow-lg">
          {allowClear && (
            <button
              type="button"
              onMouseDown={(e) => e.preventDefault()}
              onClick={() => selectItem(null)}
              className="block w-full px-3 py-2 text-left text-sm text-neutral-400 hover:bg-neutral-50"
            >
              {clearLabel}
            </button>
          )}
          {filtered.map((item) => (
            <button
              key={item.id}
              type="button"
              onMouseDown={(e) => e.preventDefault()}
              onClick={() => selectItem(item)}
              className={`block w-full truncate px-3 py-2 text-left text-sm ${
                String(item.id) === String(value) ? "bg-orange-50 text-orange-700" : "text-neutral-700 hover:bg-neutral-50"
              }`}
            >
              {item.nome}
            </button>
          ))}
          {filtered.length === 0 && !canCreate && (
            <p className="px-3 py-2 text-sm text-neutral-400">Nenhum resultado.</p>
          )}
          {canCreate && (
            <button
              type="button"
              onMouseDown={(e) => e.preventDefault()}
              onClick={handleCreate}
              disabled={creating}
              className="block w-full border-t border-neutral-100 px-3 py-2 text-left text-sm font-medium text-orange-600 hover:bg-orange-50 disabled:opacity-60"
            >
              {creating ? "Cadastrando..." : `+ Cadastrar "${search.trim()}"`}
            </button>
          )}
        </div>
      )}
    </div>
  );
}

// Gráfico de linha/área da evolução do saldo no período, com crosshair ao
// passar o mouse. Uma série só (o saldo), por isso sem legenda — o rótulo
// acima do gráfico já identifica a série.
function SaldoChart({ points }) {
  const [hoverIndex, setHoverIndex] = useState(null);
  const width = 640;
  const height = 180;
  const padding = { top: 16, right: 16, bottom: 26, left: 64 };
  const innerW = width - padding.left - padding.right;
  const innerH = height - padding.top - padding.bottom;

  if (!points || points.length < 2) {
    return (
      <div className="flex h-40 items-center justify-center rounded-xl border border-neutral-200 bg-white text-sm text-neutral-400 shadow-sm">
        Sem lançamentos suficientes nesse período pra desenhar o gráfico.
      </div>
    );
  }

  const dates = points.map((p) => new Date(`${p.date}T00:00:00`).getTime());
  const minDate = Math.min(...dates);
  const maxDate = Math.max(...dates);
  const dateRange = maxDate - minDate || 1;

  const values = points.map((p) => p.saldo);
  let minVal = Math.min(...values, 0);
  let maxVal = Math.max(...values, 0);
  if (minVal === maxVal) {
    minVal -= 1;
    maxVal += 1;
  }
  const valRange = maxVal - minVal;

  function xFor(date) {
    return padding.left + ((date - minDate) / dateRange) * innerW;
  }
  function yFor(value) {
    return padding.top + innerH - ((value - minVal) / valRange) * innerH;
  }

  const coords = points.map((p) => ({
    ...p,
    x: xFor(new Date(`${p.date}T00:00:00`).getTime()),
    y: yFor(p.saldo),
  }));

  const linePath = coords.map((c, i) => `${i === 0 ? "M" : "L"} ${c.x.toFixed(1)} ${c.y.toFixed(1)}`).join(" ");
  const baseline = (padding.top + innerH).toFixed(1);
  const areaPath = `${linePath} L ${coords[coords.length - 1].x.toFixed(1)} ${baseline} L ${coords[0].x.toFixed(1)} ${baseline} Z`;

  const zeroY = yFor(0);
  const showZeroLine = minVal < 0 && maxVal > 0;
  const last = coords[coords.length - 1];
  const hovered = hoverIndex != null ? coords[hoverIndex] : null;

  function handleMove(event) {
    const rect = event.currentTarget.getBoundingClientRect();
    const px = ((event.clientX - rect.left) / rect.width) * width;
    let nearest = 0;
    let bestDist = Infinity;
    coords.forEach((c, i) => {
      const dist = Math.abs(c.x - px);
      if (dist < bestDist) {
        bestDist = dist;
        nearest = i;
      }
    });
    setHoverIndex(nearest);
  }

  return (
    <div className="relative overflow-hidden rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
      <p className="mb-2 text-xs font-medium uppercase tracking-wide text-neutral-400">Saldo no período</p>
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="w-full"
        role="img"
        aria-label="Evolução do saldo da conta no período selecionado"
        onMouseMove={handleMove}
        onMouseLeave={() => setHoverIndex(null)}
      >
        <defs>
          <linearGradient id="saldoFill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#f97316" stopOpacity="0.22" />
            <stop offset="100%" stopColor="#f97316" stopOpacity="0" />
          </linearGradient>
        </defs>

        {showZeroLine && (
          <>
            <line
              x1={padding.left}
              y1={zeroY}
              x2={width - padding.right}
              y2={zeroY}
              stroke="#d4d4d4"
              strokeWidth="1"
              strokeDasharray="4 3"
            />
            <text x={padding.left - 8} y={zeroY + 3} textAnchor="end" fontSize="10" fill="#a3a3a3">
              R$ 0
            </text>
          </>
        )}

        <text x={padding.left - 8} y={padding.top + 4} textAnchor="end" fontSize="10" fill="#a3a3a3">
          {formatSaldoCompact(maxVal)}
        </text>
        <text x={padding.left - 8} y={padding.top + innerH} textAnchor="end" fontSize="10" fill="#a3a3a3">
          {formatSaldoCompact(minVal)}
        </text>

        <path d={areaPath} fill="url(#saldoFill)" stroke="none" />
        <path d={linePath} fill="none" stroke="#f97316" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        <circle cx={last.x} cy={last.y} r="4" fill="#f97316" />

        <text x={padding.left} y={height - 6} textAnchor="start" fontSize="10" fill="#a3a3a3">
          {formatDate(points[0].date)}
        </text>
        <text x={width - padding.right} y={height - 6} textAnchor="end" fontSize="10" fill="#a3a3a3">
          {formatDate(points[points.length - 1].date)}
        </text>

        {hovered && (
          <>
            <line
              x1={hovered.x}
              y1={padding.top}
              x2={hovered.x}
              y2={padding.top + innerH}
              stroke="#a3a3a3"
              strokeWidth="1"
              strokeDasharray="3 3"
            />
            <circle cx={hovered.x} cy={hovered.y} r="4.5" fill="#f97316" stroke="white" strokeWidth="1.5" />
          </>
        )}
      </svg>

      {hovered && (
        <div
          className="pointer-events-none absolute rounded-lg border border-neutral-200 bg-white px-3 py-2 text-xs shadow-lg"
          style={{
            left: `clamp(8px, calc(${(hovered.x / width) * 100}% - 60px), calc(100% - 140px))`,
            top: 8,
          }}
        >
          <p className="font-medium text-neutral-800">{formatDate(hovered.date)}</p>
          <p className={saldoClass(hovered.saldo)}>{formatSaldoText(hovered.saldo)}</p>
        </div>
      )}
    </div>
  );
}

export default function Extrato() {
  const { token } = useAuth();

  const [view, setView] = useState("extrato");
  const [empresas, setEmpresas] = useState([]);
  const [contas, setContas] = useState([]);
  const [loadingBase, setLoadingBase] = useState(false);
  const [error, setError] = useState("");

  const [isEmpresaModalOpen, setIsEmpresaModalOpen] = useState(false);
  const [empresaForm, setEmpresaForm] = useState(EMPTY_EMPRESA_FORM);
  const [empresaFormError, setEmpresaFormError] = useState("");
  const [savingEmpresa, setSavingEmpresa] = useState(false);
  const [deleteEmpresaTarget, setDeleteEmpresaTarget] = useState(null);

  const [isContaModalOpen, setIsContaModalOpen] = useState(false);
  const [contaForm, setContaForm] = useState(EMPTY_CONTA_FORM);
  const [contaFormError, setContaFormError] = useState("");
  const [savingConta, setSavingConta] = useState(false);
  const [deleteContaTarget, setDeleteContaTarget] = useState(null);

  const [beneficiarios, setBeneficiarios] = useState([]);
  const [isBeneficiarioModalOpen, setIsBeneficiarioModalOpen] = useState(false);
  const [beneficiarioForm, setBeneficiarioForm] = useState(EMPTY_BENEFICIARIO_FORM);
  const [beneficiarioFormError, setBeneficiarioFormError] = useState("");
  const [savingBeneficiario, setSavingBeneficiario] = useState(false);
  const [deleteBeneficiarioTarget, setDeleteBeneficiarioTarget] = useState(null);

  const [categorias, setCategorias] = useState([]);
  const [isCategoriaModalOpen, setIsCategoriaModalOpen] = useState(false);
  const [categoriaForm, setCategoriaForm] = useState(EMPTY_CATEGORIA_FORM);
  const [categoriaFormError, setCategoriaFormError] = useState("");
  const [savingCategoria, setSavingCategoria] = useState(false);
  const [deleteCategoriaTarget, setDeleteCategoriaTarget] = useState(null);

  const [centrosCusto, setCentrosCusto] = useState([]);
  const [isCentroCustoModalOpen, setIsCentroCustoModalOpen] = useState(false);
  const [centroCustoForm, setCentroCustoForm] = useState(EMPTY_CENTRO_CUSTO_FORM);
  const [centroCustoFormError, setCentroCustoFormError] = useState("");
  const [savingCentroCusto, setSavingCentroCusto] = useState(false);
  const [deleteCentroCustoTarget, setDeleteCentroCustoTarget] = useState(null);

  const [selectedContaId, setSelectedContaId] = useState("");
  const [lancamentos, setLancamentos] = useState([]);
  const [loadingLancamentos, setLoadingLancamentos] = useState(false);
  const [filterTipo, setFilterTipo] = useState("");
  const [filterDateStart, setFilterDateStart] = useState("");
  const [filterDateEnd, setFilterDateEnd] = useState("");
  const [filterBeneficiarioId, setFilterBeneficiarioId] = useState("");
  const [filterCategoriaId, setFilterCategoriaId] = useState("");
  const [filterCentroCustoId, setFilterCentroCustoId] = useState("");
  const [exportingExtrato, setExportingExtrato] = useState(false);

  const [isLancamentoModalOpen, setIsLancamentoModalOpen] = useState(false);
  const [lancamentoForm, setLancamentoForm] = useState(EMPTY_LANCAMENTO_FORM);
  const [lancamentoFormError, setLancamentoFormError] = useState("");
  const [savingLancamento, setSavingLancamento] = useState(false);
  const [deleteLancamentoTarget, setDeleteLancamentoTarget] = useState(null);

  const [isTransferenciaModalOpen, setIsTransferenciaModalOpen] = useState(false);
  const [transferenciaForm, setTransferenciaForm] = useState(EMPTY_TRANSFERENCIA_FORM);
  const [transferenciaFormError, setTransferenciaFormError] = useState("");
  const [savingTransferencia, setSavingTransferencia] = useState(false);

  const [pixPendentes, setPixPendentes] = useState([]);
  const [despesasFixasPendentes, setDespesasFixasPendentes] = useState([]);
  const [despesasVariaveisPendentes, setDespesasVariaveisPendentes] = useState([]);
  const [faturamentoPendentes, setFaturamentoPendentes] = useState([]);
  const [lancarTarget, setLancarTarget] = useState(null);
  const [lancarForm, setLancarForm] = useState(EMPTY_LANCAR_FORM);
  const [lancarFormError, setLancarFormError] = useState("");
  const [savingLancar, setSavingLancar] = useState(false);

  // Junta as 4 filas automáticas (Pix, despesa fixa, despesa variável,
  // faturamento) num único formato pra fila "Lançamentos pendentes" — cada
  // origem já foi aprovada/paga/recebida no lugar certo, só falta alguém do
  // extrato escolher a conta bancária (e opcionalmente beneficiário/categoria/
  // centro de custo) pra finalizar o lançamento.
  const pendentesUnificados = useMemo(() => {
    const itens = [
      ...pixPendentes.map((p) => ({
        origemTipo: "SOLICITACAO_PIX",
        origemId: p.financePromoterId,
        titulo: p.promoterNome,
        subtitulo: solicitacaoTypeLabel(p.tipo),
        tipoMovimento: "SAIDA",
        valor: p.valor,
        data: p.data,
        raw: p,
      })),
      ...despesasFixasPendentes.map((d) => ({
        origemTipo: "DESPESA_FIXA",
        origemId: d.fixedExpenseHistoryId,
        titulo: d.nome,
        subtitulo: d.descricao || null,
        tipoMovimento: "SAIDA",
        valor: d.valor,
        data: d.data,
        raw: d,
      })),
      ...despesasVariaveisPendentes.map((d) => ({
        origemTipo: "DESPESA_VARIAVEL",
        origemId: d.variableExpenseId,
        titulo: d.nome,
        subtitulo:
          d.totalParcelas && d.totalParcelas > 1
            ? `Parcela ${d.parcela || "?"}/${d.totalParcelas}`
            : d.descricao || null,
        tipoMovimento: "SAIDA",
        valor: d.valor,
        data: d.data,
        raw: d,
      })),
      ...faturamentoPendentes.map((f) => ({
        origemTipo: "FATURAMENTO",
        origemId: f.invoiceId,
        titulo: f.clienteNome,
        subtitulo: f.descricao || null,
        tipoMovimento: "ENTRADA",
        valor: f.valor,
        data: f.data,
        raw: f,
      })),
    ];
    itens.sort((a, b) => (a.data < b.data ? 1 : a.data > b.data ? -1 : 0));
    return itens;
  }, [pixPendentes, despesasFixasPendentes, despesasVariaveisPendentes, faturamentoPendentes]);

  async function loadBase() {
    setLoadingBase(true);
    setError("");
    try {
      const [
        empresasData,
        contasData,
        beneficiariosData,
        categoriasData,
        centrosCustoData,
        pixPendentesData,
        despesasFixasPendentesData,
        despesasVariaveisPendentesData,
        faturamentoPendentesData,
      ] = await Promise.all([
        apiFetch("/empresas", { token }),
        apiFetch("/contas-bancarias", { token }),
        apiFetch("/beneficiarios", { token }),
        apiFetch("/categorias-lancamento", { token }),
        apiFetch("/centros-custo", { token }),
        apiFetch("/lancamentos-extrato/pix-pendentes", { token }),
        apiFetch("/lancamentos-extrato/despesas-fixas-pendentes", { token }),
        apiFetch("/lancamentos-extrato/despesas-variaveis-pendentes", { token }),
        apiFetch("/lancamentos-extrato/faturamento-pendentes", { token }),
      ]);
      setEmpresas(empresasData || []);
      setContas(contasData || []);
      setBeneficiarios(beneficiariosData || []);
      setCategorias(categoriasData || []);
      setCentrosCusto(centrosCustoData || []);
      setPixPendentes(pixPendentesData || []);
      setDespesasFixasPendentes(despesasFixasPendentesData || []);
      setDespesasVariaveisPendentes(despesasVariaveisPendentesData || []);
      setFaturamentoPendentes(faturamentoPendentesData || []);
    } catch (err) {
      setError(err.message || "Não foi possível carregar empresas e contas bancárias.");
    } finally {
      setLoadingBase(false);
    }
  }

  useEffect(() => {
    loadBase();
  }, []);

  useEffect(() => {
    if (selectedContaId) {
      loadLancamentos(selectedContaId);
    } else {
      setLancamentos([]);
    }
  }, [selectedContaId]);

  async function loadLancamentos(contaId) {
    setLoadingLancamentos(true);
    setError("");
    try {
      const data = await apiFetch(`/lancamentos-extrato?contaBancariaId=${contaId}`, { token });
      setLancamentos(data || []);
    } catch (err) {
      setError(err.message || "Não foi possível carregar o extrato dessa conta.");
    } finally {
      setLoadingLancamentos(false);
    }
  }

  // Exporta o extrato da conta selecionada em Excel. Respeita o filtro de
  // período (De/Até) já usado na tela, mas não os outros filtros (tipo,
  // beneficiário, categoria, centro de custo) - a planilha sempre traz tudo
  // dentro do período, pra não ficar uma exportação "incompleta" sem avisar.
  async function exportExtrato() {
    if (!selectedContaId) {
      return;
    }

    setError("");
    setExportingExtrato(true);

    try {
      const params = new URLSearchParams({ contaBancariaId: selectedContaId });
      if (filterDateStart) {
        params.set("dataInicio", filterDateStart);
      }
      if (filterDateEnd) {
        params.set("dataFim", filterDateEnd);
      }

      const response = await fetch(`${API_BASE_URL}/lancamentos-extrato/export?${params.toString()}`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (!response.ok) {
        let message = `Erro ${response.status}`;
        try {
          const data = await response.json();
          message = data.message || message;
        } catch {
          // resposta sem corpo JSON - mantém a mensagem genérica acima
        }
        throw new Error(message);
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      const apelido = contaSelecionada?.apelido ? contaSelecionada.apelido.replace(/[^a-zA-Z0-9-]+/g, "_") : "conta";
      link.download = `extrato_${apelido}.xlsx`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(err.message || "Não foi possível exportar o extrato.");
    } finally {
      setExportingExtrato(false);
    }
  }

  function empresaNomeDe(id) {
    const empresa = empresas.find((e) => String(e.id) === String(id));
    return empresa ? empresa.nome : "-";
  }

  // --- Empresas ---

  function openNewEmpresa() {
    setEmpresaForm(EMPTY_EMPRESA_FORM);
    setEmpresaFormError("");
    setIsEmpresaModalOpen(true);
  }

  function openEditEmpresa(empresa) {
    setEmpresaForm({
      id: empresa.id,
      nome: empresa.nome || "",
      cnpj: empresa.cnpj || "",
      ativa: empresa.ativa,
    });
    setEmpresaFormError("");
    setIsEmpresaModalOpen(true);
  }

  function closeEmpresaModal() {
    setIsEmpresaModalOpen(false);
    setEmpresaForm(EMPTY_EMPRESA_FORM);
    setEmpresaFormError("");
  }

  async function handleSubmitEmpresa(event) {
    event.preventDefault();
    setEmpresaFormError("");

    if (!empresaForm.nome.trim()) {
      setEmpresaFormError("Informe o nome da empresa.");
      return;
    }

    setSavingEmpresa(true);
    const payload = {
      nome: empresaForm.nome.trim(),
      cnpj: empresaForm.cnpj.trim() === "" ? null : empresaForm.cnpj.trim(),
      ativa: empresaForm.ativa,
    };

    try {
      if (empresaForm.id) {
        await apiFetch(`/empresas/${empresaForm.id}`, { method: "PUT", body: payload, token });
      } else {
        await apiFetch("/empresas", { method: "POST", body: payload, token });
      }
      closeEmpresaModal();
      await loadBase();
    } catch (err) {
      setEmpresaFormError(err.message || "Não foi possível salvar a empresa.");
    } finally {
      setSavingEmpresa(false);
    }
  }

  async function confirmDeleteEmpresa(ticket) {
    try {
      await apiFetch(`/empresas/${deleteEmpresaTarget}`, { method: "DELETE", token, deleteTicket: ticket });
      setDeleteEmpresaTarget(null);
      await loadBase();
    } catch (err) {
      setDeleteEmpresaTarget(null);
      setError(err.message || "Não foi possível excluir a empresa.");
    }
  }

  // --- Catálogos: beneficiários, categorias e centros de custo ---

  function openNewBeneficiario() {
    setBeneficiarioForm(EMPTY_BENEFICIARIO_FORM);
    setBeneficiarioFormError("");
    setIsBeneficiarioModalOpen(true);
  }

  function openEditBeneficiario(beneficiario) {
    setBeneficiarioForm({ id: beneficiario.id, nome: beneficiario.nome || "", ativo: beneficiario.ativo });
    setBeneficiarioFormError("");
    setIsBeneficiarioModalOpen(true);
  }

  function closeBeneficiarioModal() {
    setIsBeneficiarioModalOpen(false);
    setBeneficiarioForm(EMPTY_BENEFICIARIO_FORM);
    setBeneficiarioFormError("");
  }

  async function handleSubmitBeneficiario(event) {
    event.preventDefault();
    setBeneficiarioFormError("");
    if (!beneficiarioForm.nome.trim()) {
      setBeneficiarioFormError("Informe o nome do beneficiário.");
      return;
    }
    setSavingBeneficiario(true);
    const payload = { nome: beneficiarioForm.nome.trim(), ativo: beneficiarioForm.ativo };
    try {
      if (beneficiarioForm.id) {
        await apiFetch(`/beneficiarios/${beneficiarioForm.id}`, { method: "PUT", body: payload, token });
      } else {
        await apiFetch("/beneficiarios", { method: "POST", body: payload, token });
      }
      closeBeneficiarioModal();
      await loadBase();
    } catch (err) {
      setBeneficiarioFormError(err.message || "Não foi possível salvar o beneficiário.");
    } finally {
      setSavingBeneficiario(false);
    }
  }

  async function confirmDeleteBeneficiario(ticket) {
    try {
      await apiFetch(`/beneficiarios/${deleteBeneficiarioTarget}`, { method: "DELETE", token, deleteTicket: ticket });
      setDeleteBeneficiarioTarget(null);
      await loadBase();
    } catch (err) {
      setDeleteBeneficiarioTarget(null);
      setError(err.message || "Não foi possível excluir o beneficiário.");
    }
  }

  // Usado pelo combobox do lançamento: cadastra na hora e retorna o item criado.
  async function createBeneficiarioRapido(nome) {
    const created = await apiFetch("/beneficiarios", { method: "POST", body: { nome, ativo: true }, token });
    setBeneficiarios((prev) => [...prev, created]);
    return created;
  }

  function openNewCategoria() {
    setCategoriaForm(EMPTY_CATEGORIA_FORM);
    setCategoriaFormError("");
    setIsCategoriaModalOpen(true);
  }

  function openEditCategoria(categoria) {
    setCategoriaForm({ id: categoria.id, nome: categoria.nome || "", ativo: categoria.ativo });
    setCategoriaFormError("");
    setIsCategoriaModalOpen(true);
  }

  function closeCategoriaModal() {
    setIsCategoriaModalOpen(false);
    setCategoriaForm(EMPTY_CATEGORIA_FORM);
    setCategoriaFormError("");
  }

  async function handleSubmitCategoria(event) {
    event.preventDefault();
    setCategoriaFormError("");
    if (!categoriaForm.nome.trim()) {
      setCategoriaFormError("Informe o nome da categoria.");
      return;
    }
    setSavingCategoria(true);
    const payload = { nome: categoriaForm.nome.trim(), ativo: categoriaForm.ativo };
    try {
      if (categoriaForm.id) {
        await apiFetch(`/categorias-lancamento/${categoriaForm.id}`, { method: "PUT", body: payload, token });
      } else {
        await apiFetch("/categorias-lancamento", { method: "POST", body: payload, token });
      }
      closeCategoriaModal();
      await loadBase();
    } catch (err) {
      setCategoriaFormError(err.message || "Não foi possível salvar a categoria.");
    } finally {
      setSavingCategoria(false);
    }
  }

  async function confirmDeleteCategoria(ticket) {
    try {
      await apiFetch(`/categorias-lancamento/${deleteCategoriaTarget}`, { method: "DELETE", token, deleteTicket: ticket });
      setDeleteCategoriaTarget(null);
      await loadBase();
    } catch (err) {
      setDeleteCategoriaTarget(null);
      setError(err.message || "Não foi possível excluir a categoria.");
    }
  }

  async function createCategoriaRapida(nome) {
    const created = await apiFetch("/categorias-lancamento", { method: "POST", body: { nome, ativo: true }, token });
    setCategorias((prev) => [...prev, created]);
    return created;
  }

  function openNewCentroCusto() {
    setCentroCustoForm(EMPTY_CENTRO_CUSTO_FORM);
    setCentroCustoFormError("");
    setIsCentroCustoModalOpen(true);
  }

  function openEditCentroCusto(centroCusto) {
    setCentroCustoForm({ id: centroCusto.id, nome: centroCusto.nome || "", ativo: centroCusto.ativo });
    setCentroCustoFormError("");
    setIsCentroCustoModalOpen(true);
  }

  function closeCentroCustoModal() {
    setIsCentroCustoModalOpen(false);
    setCentroCustoForm(EMPTY_CENTRO_CUSTO_FORM);
    setCentroCustoFormError("");
  }

  async function handleSubmitCentroCusto(event) {
    event.preventDefault();
    setCentroCustoFormError("");
    if (!centroCustoForm.nome.trim()) {
      setCentroCustoFormError("Informe o nome do centro de custo.");
      return;
    }
    setSavingCentroCusto(true);
    const payload = { nome: centroCustoForm.nome.trim(), ativo: centroCustoForm.ativo };
    try {
      if (centroCustoForm.id) {
        await apiFetch(`/centros-custo/${centroCustoForm.id}`, { method: "PUT", body: payload, token });
      } else {
        await apiFetch("/centros-custo", { method: "POST", body: payload, token });
      }
      closeCentroCustoModal();
      await loadBase();
    } catch (err) {
      setCentroCustoFormError(err.message || "Não foi possível salvar o centro de custo.");
    } finally {
      setSavingCentroCusto(false);
    }
  }

  async function confirmDeleteCentroCusto(ticket) {
    try {
      await apiFetch(`/centros-custo/${deleteCentroCustoTarget}`, { method: "DELETE", token, deleteTicket: ticket });
      setDeleteCentroCustoTarget(null);
      await loadBase();
    } catch (err) {
      setDeleteCentroCustoTarget(null);
      setError(err.message || "Não foi possível excluir o centro de custo.");
    }
  }

  async function createCentroCustoRapido(nome) {
    const created = await apiFetch("/centros-custo", { method: "POST", body: { nome, ativo: true }, token });
    setCentrosCusto((prev) => [...prev, created]);
    return created;
  }

  // --- Contas bancárias ---

  function openNewConta() {
    setContaForm({ ...EMPTY_CONTA_FORM, empresaId: empresas[0]?.id || "" });
    setContaFormError("");
    setIsContaModalOpen(true);
  }

  function openEditConta(conta) {
    setContaForm({
      id: conta.id,
      empresaId: conta.empresaId,
      banco: conta.banco || "",
      apelido: conta.apelido || "",
      agencia: conta.agencia || "",
      numeroConta: conta.numeroConta || "",
      saldoInicial: conta.saldoInicial ?? "",
      dataSaldoInicial: conta.dataSaldoInicial || "",
      ativa: conta.ativa,
    });
    setContaFormError("");
    setIsContaModalOpen(true);
  }

  function closeContaModal() {
    setIsContaModalOpen(false);
    setContaForm(EMPTY_CONTA_FORM);
    setContaFormError("");
  }

  function editSelectedConta() {
    if (!contaSelecionada) return;
    setView("contas");
    openEditConta(contaSelecionada);
  }

  async function handleSubmitConta(event) {
    event.preventDefault();
    setContaFormError("");

    if (!contaForm.empresaId) {
      setContaFormError("Selecione a empresa dessa conta.");
      return;
    }
    if (!contaForm.banco.trim()) {
      setContaFormError("Informe o banco.");
      return;
    }
    if (!contaForm.apelido.trim()) {
      setContaFormError("Informe um apelido para identificar a conta.");
      return;
    }

    setSavingConta(true);
    const payload = {
      empresaId: Number(contaForm.empresaId),
      banco: contaForm.banco.trim(),
      apelido: contaForm.apelido.trim(),
      agencia: contaForm.agencia.trim() === "" ? null : contaForm.agencia.trim(),
      numeroConta: contaForm.numeroConta.trim() === "" ? null : contaForm.numeroConta.trim(),
      saldoInicial: contaForm.saldoInicial === "" ? 0 : Number(contaForm.saldoInicial),
      dataSaldoInicial: contaForm.dataSaldoInicial === "" ? null : contaForm.dataSaldoInicial,
      ativa: contaForm.ativa,
    };

    try {
      if (contaForm.id) {
        await apiFetch(`/contas-bancarias/${contaForm.id}`, { method: "PUT", body: payload, token });
      } else {
        await apiFetch("/contas-bancarias", { method: "POST", body: payload, token });
      }
      closeContaModal();
      await loadBase();
    } catch (err) {
      setContaFormError(err.message || "Não foi possível salvar a conta bancária.");
    } finally {
      setSavingConta(false);
    }
  }

  async function confirmDeleteConta(ticket) {
    try {
      await apiFetch(`/contas-bancarias/${deleteContaTarget}`, { method: "DELETE", token, deleteTicket: ticket });
      setDeleteContaTarget(null);
      if (String(selectedContaId) === String(deleteContaTarget)) {
        setSelectedContaId("");
      }
      await loadBase();
    } catch (err) {
      setDeleteContaTarget(null);
      setError(err.message || "Não foi possível excluir a conta bancária.");
    }
  }

  // --- Lançamentos do extrato ---

  function openNewLancamento(tipo = "SAIDA") {
    setLancamentoForm({ ...EMPTY_LANCAMENTO_FORM, contaBancariaId: selectedContaId, tipo });
    setLancamentoFormError("");
    setIsLancamentoModalOpen(true);
  }

  function openEditLancamento(lancamento) {
    setLancamentoForm({
      id: lancamento.id,
      contaBancariaId: lancamento.contaBancariaId,
      data: lancamento.data || todayIso(),
      tipo: lancamento.tipo,
      valor: lancamento.valor ?? "",
      descricao: lancamento.descricao || "",
      beneficiarioId: lancamento.beneficiarioId ?? "",
      categoriaId: lancamento.categoriaId ?? "",
      centroCustoId: lancamento.centroCustoId ?? "",
    });
    setLancamentoFormError("");
    setIsLancamentoModalOpen(true);
  }

  function closeLancamentoModal() {
    setIsLancamentoModalOpen(false);
    setLancamentoForm(EMPTY_LANCAMENTO_FORM);
    setLancamentoFormError("");
  }

  async function handleSubmitLancamento(event) {
    event.preventDefault();
    setLancamentoFormError("");

    if (!lancamentoForm.data) {
      setLancamentoFormError("Informe a data do lançamento.");
      return;
    }
    if (lancamentoForm.valor === "" || Number(lancamentoForm.valor) <= 0) {
      setLancamentoFormError("Informe um valor válido.");
      return;
    }
    if (!lancamentoForm.categoriaId) {
      setLancamentoFormError("Selecione a categoria do pagamento.");
      return;
    }
    if (!lancamentoForm.centroCustoId) {
      setLancamentoFormError("Selecione o centro de custo.");
      return;
    }

    setSavingLancamento(true);
    const payload = {
      contaBancariaId: Number(lancamentoForm.contaBancariaId),
      data: lancamentoForm.data,
      tipo: lancamentoForm.tipo,
      valor: Number(lancamentoForm.valor),
      descricao: lancamentoForm.descricao.trim() === "" ? null : lancamentoForm.descricao.trim(),
      beneficiarioId: lancamentoForm.beneficiarioId === "" ? null : Number(lancamentoForm.beneficiarioId),
      categoriaId: Number(lancamentoForm.categoriaId),
      centroCustoId: Number(lancamentoForm.centroCustoId),
    };

    try {
      if (lancamentoForm.id) {
        await apiFetch(`/lancamentos-extrato/${lancamentoForm.id}`, { method: "PUT", body: payload, token });
      } else {
        await apiFetch("/lancamentos-extrato", { method: "POST", body: payload, token });
      }
      closeLancamentoModal();
      await loadLancamentos(selectedContaId);
      await loadBase();
    } catch (err) {
      setLancamentoFormError(err.message || "Não foi possível salvar o lançamento.");
    } finally {
      setSavingLancamento(false);
    }
  }

  async function confirmDeleteLancamento(ticket) {
    try {
      await apiFetch(`/lancamentos-extrato/${deleteLancamentoTarget}`, { method: "DELETE", token, deleteTicket: ticket });
      setDeleteLancamentoTarget(null);
      await loadLancamentos(selectedContaId);
      await loadBase();
    } catch (err) {
      setDeleteLancamentoTarget(null);
      setError(err.message || "Não foi possível excluir o lançamento.");
    }
  }

  // --- Transferência entre contas ---

  function openNewTransferencia() {
    setTransferenciaForm(EMPTY_TRANSFERENCIA_FORM);
    setTransferenciaFormError("");
    setIsTransferenciaModalOpen(true);
  }

  function closeTransferenciaModal() {
    setIsTransferenciaModalOpen(false);
    setTransferenciaForm(EMPTY_TRANSFERENCIA_FORM);
    setTransferenciaFormError("");
  }

  async function handleSubmitTransferencia(event) {
    event.preventDefault();
    setTransferenciaFormError("");

    if (!transferenciaForm.contaDestinoId) {
      setTransferenciaFormError("Selecione a conta de destino.");
      return;
    }
    if (transferenciaForm.valor === "" || Number(transferenciaForm.valor) <= 0) {
      setTransferenciaFormError("Informe um valor válido.");
      return;
    }

    setSavingTransferencia(true);
    try {
      await apiFetch("/lancamentos-extrato/transferencia", {
        method: "POST",
        body: {
          contaOrigemId: Number(selectedContaId),
          contaDestinoId: Number(transferenciaForm.contaDestinoId),
          data: transferenciaForm.data,
          valor: Number(transferenciaForm.valor),
          descricao: transferenciaForm.descricao.trim() === "" ? null : transferenciaForm.descricao.trim(),
        },
        token,
      });
      closeTransferenciaModal();
      await loadLancamentos(selectedContaId);
      await loadBase();
    } catch (err) {
      setTransferenciaFormError(err.message || "Não foi possível registrar a transferência.");
    } finally {
      setSavingTransferencia(false);
    }
  }

  // --- Lançamentos pendentes (Pix aprovado, despesa fixa/variável paga, ou
  // faturamento recebido — tudo isso já aconteceu no lugar certo, só falta
  // alguém do extrato escolher a conta bancária e finalizar o lançamento) ---

  const PENDENTE_ENDPOINT_SEGMENT = {
    SOLICITACAO_PIX: "pix",
    DESPESA_FIXA: "despesa-fixa",
    DESPESA_VARIAVEL: "despesa-variavel",
    FATURAMENTO: "faturamento",
  };

  function openLancar(pendente) {
    setLancarTarget(pendente);
    setLancarForm(EMPTY_LANCAR_FORM);
    setLancarFormError("");
  }

  function closeLancarModal() {
    setLancarTarget(null);
    setLancarForm(EMPTY_LANCAR_FORM);
    setLancarFormError("");
  }

  async function handleSubmitLancar(event) {
    event.preventDefault();
    setLancarFormError("");

    if (!lancarForm.contaBancariaId) {
      setLancarFormError("Selecione a conta bancária.");
      return;
    }
    if (!lancarForm.categoriaId) {
      setLancarFormError("Selecione a categoria do pagamento.");
      return;
    }
    if (!lancarForm.centroCustoId) {
      setLancarFormError("Selecione o centro de custo.");
      return;
    }

    const segmento = PENDENTE_ENDPOINT_SEGMENT[lancarTarget.origemTipo];
    const body = {
      contaBancariaId: Number(lancarForm.contaBancariaId),
      categoriaId: Number(lancarForm.categoriaId),
      centroCustoId: Number(lancarForm.centroCustoId),
    };
    // O Pix resolve o beneficiário sozinho a partir do promotor; nas demais
    // origens é opcional (faturamento ainda tenta resolver pelo cliente se
    // ficar em branco).
    if (lancarTarget.origemTipo !== "SOLICITACAO_PIX" && lancarForm.beneficiarioId) {
      body.beneficiarioId = Number(lancarForm.beneficiarioId);
    }

    setSavingLancar(true);
    try {
      await apiFetch(`/lancamentos-extrato/${segmento}/${lancarTarget.origemId}`, {
        method: "POST",
        body,
        token,
      });
      closeLancarModal();
      await loadBase();
      if (selectedContaId) {
        await loadLancamentos(selectedContaId);
      }
    } catch (err) {
      setLancarFormError(err.message || "Não foi possível lançar isso no extrato.");
    } finally {
      setSavingLancar(false);
    }
  }

  const contasOrdenadas = [...contas].sort((a, b) => {
    if (a.ativa !== b.ativa) return a.ativa ? -1 : 1;
    return (a.apelido || "").localeCompare(b.apelido || "");
  });

  const contasAtivas = contas.filter((c) => c.ativa);
  const saldoTotalContasAtivas = contasAtivas.reduce(
    (sum, c) => sum + Number(c.saldoAtual ?? c.saldoInicial ?? 0),
    0
  );

  const contaSelecionada = contas.find((c) => String(c.id) === String(selectedContaId));

  const beneficiariosAtivos = beneficiarios.filter(
    (b) => b.ativo || String(b.id) === String(lancamentoForm.beneficiarioId)
  );
  const categoriasAtivas = categorias.filter(
    (c) => c.ativo || String(c.id) === String(lancamentoForm.categoriaId)
  );
  const centrosCustoAtivos = centrosCusto.filter(
    (c) => c.ativo || String(c.id) === String(lancamentoForm.centroCustoId)
  );

  const filteredLancamentos = lancamentos.filter((l) => {
    const matchesTipo = filterTipo === "" || l.tipo === filterTipo;
    const matchesStart = filterDateStart === "" || (l.data && l.data >= filterDateStart);
    const matchesEnd = filterDateEnd === "" || (l.data && l.data <= filterDateEnd);
    const matchesBeneficiario =
      filterBeneficiarioId === "" || String(l.beneficiarioId) === String(filterBeneficiarioId);
    const matchesCategoria = filterCategoriaId === "" || String(l.categoriaId) === String(filterCategoriaId);
    const matchesCentroCusto =
      filterCentroCustoId === "" || String(l.centroCustoId) === String(filterCentroCustoId);
    return (
      matchesTipo && matchesStart && matchesEnd && matchesBeneficiario && matchesCategoria && matchesCentroCusto
    );
  });

  const saldoAtual = lancamentos.length > 0 ? lancamentos[0].saldoAcumulado : contaSelecionada?.saldoInicial;

  // Saldo logo antes do início do período filtrado (ou o saldo inicial da
  // conta, se não houver filtro de data) — vira a linha "Saldo anterior".
  let saldoAnterior = contaSelecionada?.saldoInicial ?? 0;
  if (filterDateStart) {
    const anteriores = lancamentos.filter((l) => l.data && l.data < filterDateStart);
    if (anteriores.length > 0) {
      saldoAnterior = anteriores[0].saldoAcumulado;
    }
  }

  // Pontos do gráfico: só o período filtrado por data (o filtro de tipo não
  // afeta o gráfico, que mostra o saldo real da conta, não só entradas ou saídas).
  const lancamentosNoPeriodo = lancamentos.filter((l) => {
    const matchesStart = filterDateStart === "" || (l.data && l.data >= filterDateStart);
    const matchesEnd = filterDateEnd === "" || (l.data && l.data <= filterDateEnd);
    return matchesStart && matchesEnd;
  });
  const lancamentosAscendentes = [...lancamentosNoPeriodo].sort(
    (a, b) => (a.data < b.data ? -1 : a.data > b.data ? 1 : a.id - b.id)
  );
  const dataAnteriorGrafico =
    filterDateStart || contaSelecionada?.dataSaldoInicial || lancamentosAscendentes[0]?.data;
  const chartPoints = dataAnteriorGrafico
    ? [
        { date: dataAnteriorGrafico, saldo: Number(saldoAnterior) },
        ...lancamentosAscendentes.map((l) => ({ date: l.data, saldo: Number(l.saldoAcumulado) })),
      ]
    : [];

  return (
    <Layout title="Extrato">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div className="flex rounded-lg border border-neutral-300 bg-white p-1">
          <button
            type="button"
            onClick={() => setView("contas")}
            className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
              view === "contas" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
            }`}
          >
            Contas bancárias
          </button>
          <button
            type="button"
            onClick={() => setView("extrato")}
            className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
              view === "extrato" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
            }`}
          >
            Extrato
          </button>
          <button
            type="button"
            onClick={() => setView("pendentes")}
            className={`flex items-center gap-2 rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
              view === "pendentes" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
            }`}
          >
            Lançamentos pendentes
            {pendentesUnificados.length > 0 && (
              <span
                className={`flex h-5 min-w-[20px] items-center justify-center rounded-full px-1 text-xs font-bold ${
                  view === "pendentes" ? "bg-black text-white" : "bg-orange-500 text-black"
                }`}
              >
                {pendentesUnificados.length}
              </span>
            )}
          </button>
          <button
            type="button"
            onClick={() => setView("cadastros")}
            className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
              view === "cadastros" ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
            }`}
          >
            Cadastros
          </button>
        </div>

        {view === "contas" && (
          <div className="flex gap-2">
            <button
              onClick={openNewEmpresa}
              className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-100"
            >
              + Nova empresa
            </button>
            <button
              onClick={openNewConta}
              disabled={empresas.length === 0}
              className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-50"
            >
              + Nova conta bancária
            </button>
          </div>
        )}
      </div>

      {error && (
        <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>
      )}

      {view === "contas" && (
        <>
          <div className="mb-8 overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
            <div className="border-b border-neutral-200 bg-neutral-50 px-6 py-4">
              <p className="text-sm font-semibold text-black">Empresas</p>
              <p className="text-xs text-neutral-500">Os CNPJs próprios que têm contas bancárias cadastradas.</p>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-black text-white">
                  <tr>
                    <th className="px-4 py-3 font-medium">Nome</th>
                    <th className="px-4 py-3 font-medium">CNPJ</th>
                    <th className="px-4 py-3 font-medium">Status</th>
                    <th className="px-4 py-3 font-medium text-right">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-100">
                  {loadingBase ? (
                    <tr>
                      <td colSpan="4" className="px-4 py-8 text-center text-neutral-400">
                        Carregando...
                      </td>
                    </tr>
                  ) : empresas.length === 0 ? (
                    <tr>
                      <td colSpan="4" className="px-4 py-8 text-center text-neutral-400">
                        Nenhuma empresa cadastrada ainda.
                      </td>
                    </tr>
                  ) : (
                    empresas.map((empresa) => (
                      <tr key={empresa.id} className="hover:bg-orange-50/40">
                        <td className="px-4 py-3 font-medium text-neutral-800">{empresa.nome}</td>
                        <td className="px-4 py-3 text-neutral-600">{empresa.cnpj || "-"}</td>
                        <td className="px-4 py-3">
                          <span
                            className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                              empresa.ativa ? "bg-orange-100 text-orange-700" : "bg-neutral-100 text-neutral-500"
                            }`}
                          >
                            {empresa.ativa ? "Ativa" : "Arquivada"}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-right">
                          <button
                            onClick={() => openEditEmpresa(empresa)}
                            className="mr-3 text-sm font-medium text-neutral-600 hover:text-orange-600"
                          >
                            Editar
                          </button>
                          <button
                            onClick={() => setDeleteEmpresaTarget(empresa.id)}
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

          <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
            <div className="border-b border-neutral-200 bg-neutral-50 px-6 py-4">
              <p className="text-sm font-semibold text-black">Contas bancárias</p>
              <p className="text-xs text-neutral-500">
                Cada conta pertence a uma empresa. Arquive em vez de excluir se já tiver lançamentos.
              </p>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-black text-white">
                  <tr>
                    <th className="px-4 py-3 font-medium">Conta</th>
                    <th className="px-4 py-3 font-medium">Banco</th>
                    <th className="px-4 py-3 font-medium">Empresa</th>
                    <th className="px-4 py-3 font-medium">Saldo inicial</th>
                    <th className="px-4 py-3 font-medium">Saldo atual</th>
                    <th className="px-4 py-3 font-medium">Status</th>
                    <th className="px-4 py-3 font-medium text-right">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-100">
                  {loadingBase ? (
                    <tr>
                      <td colSpan="7" className="px-4 py-8 text-center text-neutral-400">
                        Carregando...
                      </td>
                    </tr>
                  ) : contas.length === 0 ? (
                    <tr>
                      <td colSpan="7" className="px-4 py-8 text-center text-neutral-400">
                        Nenhuma conta bancária cadastrada ainda.
                      </td>
                    </tr>
                  ) : (
                    contasOrdenadas.map((conta) => (
                      <tr key={conta.id} className="hover:bg-orange-50/40">
                        <td className="px-4 py-3 font-medium text-neutral-800">
                          {conta.apelido}
                          {conta.agencia || conta.numeroConta ? (
                            <p className="text-xs text-neutral-400">
                              {conta.agencia ? `Ag. ${conta.agencia}` : ""}
                              {conta.agencia && conta.numeroConta ? " · " : ""}
                              {conta.numeroConta ? `Conta ${conta.numeroConta}` : ""}
                            </p>
                          ) : null}
                        </td>
                        <td className="px-4 py-3 text-neutral-600">{conta.banco}</td>
                        <td className="px-4 py-3 text-neutral-600">{conta.empresaNome || empresaNomeDe(conta.empresaId)}</td>
                        <td className="px-4 py-3 text-neutral-600">{formatCurrency(conta.saldoInicial)}</td>
                        <td className={`px-4 py-3 font-medium ${saldoClass(conta.saldoAtual ?? conta.saldoInicial)}`}>
                          {formatSaldoText(conta.saldoAtual ?? conta.saldoInicial)}
                        </td>
                        <td className="px-4 py-3">
                          <span
                            className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                              conta.ativa ? "bg-orange-100 text-orange-700" : "bg-neutral-100 text-neutral-500"
                            }`}
                          >
                            {conta.ativa ? "Ativa" : "Arquivada"}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-right">
                          <button
                            onClick={() => openEditConta(conta)}
                            className="mr-3 text-sm font-medium text-neutral-600 hover:text-orange-600"
                          >
                            Editar
                          </button>
                          <button
                            onClick={() => setDeleteContaTarget(conta.id)}
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
        </>
      )}

      {view === "pendentes" && (
        <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
          <div className="border-b border-neutral-200 bg-neutral-50 px-6 py-4">
            <p className="text-sm font-semibold text-black">Lançamentos pendentes</p>
            <p className="text-xs text-neutral-500">
              Pix aprovados, despesas marcadas como pagas e faturamentos recebidos que ainda não foram lançados em
              nenhuma conta bancária do extrato.
            </p>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-black text-white">
                <tr>
                  <th className="px-4 py-3 font-medium">Data</th>
                  <th className="px-4 py-3 font-medium">Origem</th>
                  <th className="px-4 py-3 font-medium">Descrição</th>
                  <th className="px-4 py-3 font-medium">Valor</th>
                  <th className="px-4 py-3 font-medium text-right">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {pendentesUnificados.length === 0 ? (
                  <tr>
                    <td colSpan="5" className="px-4 py-8 text-center text-neutral-400">
                      Nenhum lançamento pendente no momento.
                    </td>
                  </tr>
                ) : (
                  pendentesUnificados.map((item) => {
                    const origemInfo = PENDENTE_ORIGEM_INFO[item.origemTipo] || { label: item.origemTipo, badgeClass: "bg-neutral-100 text-neutral-700" };
                    return (
                      <tr key={`${item.origemTipo}-${item.origemId}`} className="hover:bg-orange-50/40">
                        <td className="px-4 py-3 text-neutral-600">{formatDate(item.data)}</td>
                        <td className="px-4 py-3">
                          <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${origemInfo.badgeClass}`}>
                            {origemInfo.label}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <p className="font-medium text-neutral-800">{item.titulo}</p>
                          {item.subtitulo && <p className="text-xs text-neutral-500">{item.subtitulo}</p>}
                        </td>
                        <td
                          className={`px-4 py-3 font-medium ${
                            item.tipoMovimento === "ENTRADA" ? "text-green-700" : "text-red-700"
                          }`}
                        >
                          {item.tipoMovimento === "ENTRADA" ? "+" : "−"} {formatCurrency(item.valor)}
                        </td>
                        <td className="px-4 py-3 text-right">
                          <button
                            onClick={() => openLancar(item)}
                            className="rounded-lg bg-orange-500 px-3 py-1.5 text-sm font-semibold text-black transition-colors hover:bg-orange-600"
                          >
                            Lançar no extrato
                          </button>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {view === "cadastros" && (
        <div className="space-y-8">
          <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
            <div className="flex items-center justify-between border-b border-neutral-200 bg-neutral-50 px-6 py-4">
              <div>
                <p className="text-sm font-semibold text-black">Beneficiários</p>
                <p className="text-xs text-neutral-500">
                  Quem recebe ou paga nos lançamentos — cadastre uma vez e reutilize em vários pagamentos.
                </p>
              </div>
              <button
                onClick={openNewBeneficiario}
                className="flex-shrink-0 rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600"
              >
                + Novo beneficiário
              </button>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-black text-white">
                  <tr>
                    <th className="px-4 py-3 font-medium">Nome</th>
                    <th className="px-4 py-3 font-medium">Status</th>
                    <th className="px-4 py-3 font-medium text-right">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-100">
                  {beneficiarios.length === 0 ? (
                    <tr>
                      <td colSpan="3" className="px-4 py-8 text-center text-neutral-400">
                        Nenhum beneficiário cadastrado ainda.
                      </td>
                    </tr>
                  ) : (
                    beneficiarios.map((b) => (
                      <tr key={b.id} className="hover:bg-orange-50/40">
                        <td className="px-4 py-3 font-medium text-neutral-800">{b.nome}</td>
                        <td className="px-4 py-3">
                          <span
                            className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                              b.ativo ? "bg-orange-100 text-orange-700" : "bg-neutral-100 text-neutral-500"
                            }`}
                          >
                            {b.ativo ? "Ativo" : "Arquivado"}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-right">
                          <button
                            onClick={() => openEditBeneficiario(b)}
                            className="mr-3 text-sm font-medium text-neutral-600 hover:text-orange-600"
                          >
                            Editar
                          </button>
                          <button
                            onClick={() => setDeleteBeneficiarioTarget(b.id)}
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

          <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
            <div className="flex items-center justify-between border-b border-neutral-200 bg-neutral-50 px-6 py-4">
              <div>
                <p className="text-sm font-semibold text-black">Categorias de pagamento</p>
                <p className="text-xs text-neutral-500">Ex: Salário, Fornecedor, Imposto, Aluguel.</p>
              </div>
              <button
                onClick={openNewCategoria}
                className="flex-shrink-0 rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600"
              >
                + Nova categoria
              </button>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-black text-white">
                  <tr>
                    <th className="px-4 py-3 font-medium">Nome</th>
                    <th className="px-4 py-3 font-medium">Status</th>
                    <th className="px-4 py-3 font-medium text-right">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-100">
                  {categorias.length === 0 ? (
                    <tr>
                      <td colSpan="3" className="px-4 py-8 text-center text-neutral-400">
                        Nenhuma categoria cadastrada ainda.
                      </td>
                    </tr>
                  ) : (
                    categorias.map((c) => (
                      <tr key={c.id} className="hover:bg-orange-50/40">
                        <td className="px-4 py-3 font-medium text-neutral-800">{c.nome}</td>
                        <td className="px-4 py-3">
                          <span
                            className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                              c.ativo ? "bg-orange-100 text-orange-700" : "bg-neutral-100 text-neutral-500"
                            }`}
                          >
                            {c.ativo ? "Ativa" : "Arquivada"}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-right">
                          <button
                            onClick={() => openEditCategoria(c)}
                            className="mr-3 text-sm font-medium text-neutral-600 hover:text-orange-600"
                          >
                            Editar
                          </button>
                          <button
                            onClick={() => setDeleteCategoriaTarget(c.id)}
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

          <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
            <div className="flex items-center justify-between border-b border-neutral-200 bg-neutral-50 px-6 py-4">
              <div>
                <p className="text-sm font-semibold text-black">Centros de custo</p>
                <p className="text-xs text-neutral-500">
                  Ex: Custo Operacional AT, Despesa Administrativa Tejo, Despesa Financeira AT.
                </p>
              </div>
              <button
                onClick={openNewCentroCusto}
                className="flex-shrink-0 rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600"
              >
                + Novo centro de custo
              </button>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-black text-white">
                  <tr>
                    <th className="px-4 py-3 font-medium">Nome</th>
                    <th className="px-4 py-3 font-medium">Status</th>
                    <th className="px-4 py-3 font-medium text-right">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-100">
                  {centrosCusto.length === 0 ? (
                    <tr>
                      <td colSpan="3" className="px-4 py-8 text-center text-neutral-400">
                        Nenhum centro de custo cadastrado ainda.
                      </td>
                    </tr>
                  ) : (
                    centrosCusto.map((c) => (
                      <tr key={c.id} className="hover:bg-orange-50/40">
                        <td className="px-4 py-3 font-medium text-neutral-800">{c.nome}</td>
                        <td className="px-4 py-3">
                          <span
                            className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                              c.ativo ? "bg-orange-100 text-orange-700" : "bg-neutral-100 text-neutral-500"
                            }`}
                          >
                            {c.ativo ? "Ativo" : "Arquivado"}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-right">
                          <button
                            onClick={() => openEditCentroCusto(c)}
                            className="mr-3 text-sm font-medium text-neutral-600 hover:text-orange-600"
                          >
                            Editar
                          </button>
                          <button
                            onClick={() => setDeleteCentroCustoTarget(c.id)}
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
        </div>
      )}

      {view === "extrato" && (
        <div className="flex flex-col gap-6 lg:flex-row">
          <aside className="w-full flex-shrink-0 lg:w-72">
            <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
              <div className="border-b border-neutral-200 px-4 py-3">
                <p className="text-sm font-semibold text-black">Contas</p>
              </div>
              <div className="max-h-[420px] divide-y divide-neutral-100 overflow-y-auto">
                {loadingBase ? (
                  <p className="px-4 py-6 text-center text-sm text-neutral-400">Carregando...</p>
                ) : contasOrdenadas.length === 0 ? (
                  <p className="px-4 py-6 text-center text-sm text-neutral-400">
                    Nenhuma conta cadastrada ainda.
                  </p>
                ) : (
                  contasOrdenadas.map((conta, index) => {
                    const selecionada = String(conta.id) === String(selectedContaId);
                    return (
                      <button
                        key={conta.id}
                        type="button"
                        onClick={() => setSelectedContaId(String(conta.id))}
                        className={`flex w-full items-center gap-3 px-4 py-3 text-left transition-colors ${
                          selecionada ? "bg-orange-50" : "hover:bg-neutral-50"
                        }`}
                      >
                        <span
                          className={`flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full text-xs font-bold text-white ${badgeColor(
                            index
                          )}`}
                        >
                          {accountInitials(conta.apelido)}
                        </span>
                        <span className="min-w-0 flex-1">
                          <span
                            className={`block truncate text-sm font-medium ${
                              selecionada ? "text-orange-700" : "text-neutral-800"
                            }`}
                          >
                            {conta.apelido}
                          </span>
                          <span className="block truncate text-xs text-neutral-400">
                            {conta.banco}
                            {!conta.ativa ? " · arquivada" : ""}
                          </span>
                        </span>
                        <span
                          className={`flex-shrink-0 text-right text-sm font-semibold ${saldoClass(
                            conta.saldoAtual ?? conta.saldoInicial
                          )}`}
                        >
                          {formatSaldoText(conta.saldoAtual ?? conta.saldoInicial)}
                        </span>
                      </button>
                    );
                  })
                )}
              </div>
              {contasAtivas.length > 0 && (
                <div className="flex items-center justify-between border-t border-neutral-200 bg-neutral-50 px-4 py-3">
                  <span className="text-sm font-medium text-neutral-600">Saldo total</span>
                  <span className={`text-sm font-bold ${saldoClass(saldoTotalContasAtivas)}`}>
                    {formatSaldoText(saldoTotalContasAtivas)}
                  </span>
                </div>
              )}
            </div>
            <button
              type="button"
              onClick={() => {
                setView("contas");
                openNewConta();
              }}
              className="mt-3 w-full rounded-lg border border-dashed border-neutral-300 px-3 py-2 text-center text-sm font-medium text-neutral-500 hover:border-orange-400 hover:text-orange-600"
            >
              + Nova conta bancária
            </button>
          </aside>

          <div className="min-w-0 flex-1">
            {!selectedContaId && (
              <div className="rounded-xl border border-dashed border-neutral-300 bg-white p-10 text-center text-sm text-neutral-500">
                Selecione uma conta na lista ao lado pra ver o extrato dela.
              </div>
            )}

            {selectedContaId && (
              <>
                <div className="mb-4 flex flex-wrap items-end justify-between gap-3 rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
                  <div>
                    <p className="text-lg font-semibold text-black">
                      {contaSelecionada?.apelido}
                      <button
                        type="button"
                        onClick={editSelectedConta}
                        className="ml-2 text-xs font-medium text-orange-600 hover:underline"
                      >
                        Editar
                      </button>
                    </p>
                    <p className="text-sm text-neutral-500">
                      {contaSelecionada?.banco} · {contaSelecionada?.empresaNome || empresaNomeDe(contaSelecionada?.empresaId)}
                    </p>
                  </div>

                  <div className="flex flex-wrap items-end gap-3">
                    <div>
                      <label className="mb-1 block text-sm font-medium text-neutral-700">Tipo</label>
                      <select
                        value={filterTipo}
                        onChange={(e) => setFilterTipo(e.target.value)}
                        className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                      >
                        <option value="">Todos</option>
                        <option value="ENTRADA">Entrada</option>
                        <option value="SAIDA">Saída</option>
                      </select>
                    </div>
                    <div>
                      <label className="mb-1 block text-sm font-medium text-neutral-700">De</label>
                      <input
                        type="date"
                        value={filterDateStart}
                        onChange={(e) => setFilterDateStart(e.target.value)}
                        className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                      />
                    </div>
                    <div>
                      <label className="mb-1 block text-sm font-medium text-neutral-700">Até</label>
                      <input
                        type="date"
                        value={filterDateEnd}
                        onChange={(e) => setFilterDateEnd(e.target.value)}
                        className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                      />
                    </div>
                    {(filterTipo ||
                      filterDateStart ||
                      filterDateEnd ||
                      filterBeneficiarioId ||
                      filterCategoriaId ||
                      filterCentroCustoId) && (
                      <button
                        type="button"
                        onClick={() => {
                          setFilterTipo("");
                          setFilterDateStart("");
                          setFilterDateEnd("");
                          setFilterBeneficiarioId("");
                          setFilterCategoriaId("");
                          setFilterCentroCustoId("");
                        }}
                        className="text-sm font-medium text-neutral-500 hover:text-neutral-700"
                      >
                        Limpar filtros
                      </button>
                    )}
                  </div>

                  <div className="flex w-full flex-wrap items-end gap-3">
                    <div className="min-w-[180px] flex-1">
                      <label className="mb-1 block text-sm font-medium text-neutral-700">Beneficiário</label>
                      <CatalogCombobox
                        items={beneficiarios}
                        value={filterBeneficiarioId}
                        onChange={setFilterBeneficiarioId}
                        placeholder="Buscar..."
                        clearLabel="Todos"
                      />
                    </div>
                    <div className="min-w-[180px] flex-1">
                      <label className="mb-1 block text-sm font-medium text-neutral-700">Categoria</label>
                      <CatalogCombobox
                        items={categorias}
                        value={filterCategoriaId}
                        onChange={setFilterCategoriaId}
                        placeholder="Buscar..."
                        clearLabel="Todas"
                      />
                    </div>
                    <div className="min-w-[180px] flex-1">
                      <label className="mb-1 block text-sm font-medium text-neutral-700">Centro de custo</label>
                      <CatalogCombobox
                        items={centrosCusto}
                        value={filterCentroCustoId}
                        onChange={setFilterCentroCustoId}
                        placeholder="Buscar..."
                        clearLabel="Todos"
                      />
                    </div>
                  </div>
                </div>

                <div className="mb-4">
                  <SaldoChart points={chartPoints} />
                </div>

                <div className="mb-4 flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={() => openNewLancamento("SAIDA")}
                    className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-100"
                  >
                    Novo pagamento
                  </button>
                  <button
                    type="button"
                    onClick={() => openNewLancamento("ENTRADA")}
                    className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-100"
                  >
                    Novo recebimento
                  </button>
                  <button
                    type="button"
                    onClick={openNewTransferencia}
                    disabled={contasAtivas.length < 2}
                    className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-50"
                  >
                    Nova transferência
                  </button>
                  <button
                    type="button"
                    onClick={exportExtrato}
                    disabled={exportingExtrato}
                    className="ml-auto rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-100 disabled:opacity-50"
                    title={
                      filterDateStart || filterDateEnd
                        ? "Exporta o extrato respeitando o período De/Até selecionado"
                        : "Exporta o extrato completo desta conta"
                    }
                  >
                    {exportingExtrato ? "Exportando..." : "Exportar Excel"}
                  </button>
                </div>

                <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                      <thead className="bg-black text-white">
                        <tr>
                          <th className="px-4 py-3 font-medium">Data</th>
                          <th className="px-4 py-3 font-medium">Tipo</th>
                          <th className="px-4 py-3 font-medium">Descrição</th>
                          <th className="px-4 py-3 font-medium">Origem</th>
                          <th className="px-4 py-3 font-medium">Valor</th>
                          <th className="px-4 py-3 font-medium">Saldo</th>
                          <th className="px-4 py-3 font-medium text-right">Ações</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-neutral-100">
                        {!loadingLancamentos && (
                          <tr className="bg-neutral-50">
                            <td className="px-4 py-3 text-neutral-500" colSpan="5">
                              Saldo anterior
                            </td>
                            <td className={`px-4 py-3 font-medium ${saldoClass(saldoAnterior)}`}>
                              {formatSaldoText(saldoAnterior)}
                            </td>
                            <td></td>
                          </tr>
                        )}
                        {loadingLancamentos ? (
                          <tr>
                            <td colSpan="7" className="px-4 py-8 text-center text-neutral-400">
                              Carregando...
                            </td>
                          </tr>
                        ) : filteredLancamentos.length === 0 ? (
                          <tr>
                            <td colSpan="7" className="px-4 py-8 text-center text-neutral-400">
                              Nenhum lançamento encontrado.
                            </td>
                          </tr>
                        ) : (
                          filteredLancamentos.map((lancamento) => (
                            <tr key={lancamento.id} className="hover:bg-orange-50/40">
                              <td className="px-4 py-3 text-neutral-600">{formatDate(lancamento.data)}</td>
                              <td className="px-4 py-3">
                                <span
                                  className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
                                    lancamento.tipo === "ENTRADA"
                                      ? "bg-green-100 text-green-700"
                                      : "bg-red-100 text-red-700"
                                  }`}
                                >
                                  {lancamento.tipo === "ENTRADA" ? "Entrada" : "Saída"}
                                </span>
                              </td>
                              <td className="px-4 py-3 text-neutral-600">
                                {lancamento.descricao || "-"}
                                {(lancamento.beneficiarioNome || lancamento.categoriaNome || lancamento.centroCustoNome) && (
                                  <p className="mt-0.5 text-xs text-neutral-400">
                                    {[lancamento.beneficiarioNome, lancamento.categoriaNome, lancamento.centroCustoNome]
                                      .filter(Boolean)
                                      .join(" · ")}
                                  </p>
                                )}
                              </td>
                              <td className="px-4 py-3 text-neutral-500">{origemLabel(lancamento)}</td>
                              <td
                                className={`px-4 py-3 font-medium ${
                                  lancamento.tipo === "ENTRADA" ? "text-green-700" : "text-red-700"
                                }`}
                              >
                                {lancamento.tipo === "ENTRADA" ? "+ " : "- "}
                                {formatCurrency(lancamento.valor)}
                              </td>
                              <td className={`px-4 py-3 ${saldoClass(lancamento.saldoAcumulado)}`}>
                                {formatSaldoText(lancamento.saldoAcumulado)}
                              </td>
                              <td className="px-4 py-3 text-right">
                                {lancamento.editavel && (
                                  <button
                                    onClick={() => openEditLancamento(lancamento)}
                                    className="mr-3 text-sm font-medium text-neutral-600 hover:text-orange-600"
                                  >
                                    Editar
                                  </button>
                                )}
                                {lancamento.excluivel && (
                                  <button
                                    onClick={() => setDeleteLancamentoTarget(lancamento.id)}
                                    className="text-sm font-medium text-neutral-600 hover:text-red-600"
                                  >
                                    Excluir
                                  </button>
                                )}
                                {!lancamento.editavel && !lancamento.excluivel && (
                                  <span className="text-xs text-neutral-400">Automático</span>
                                )}
                              </td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              </>
            )}
          </div>
        </div>
      )}

      {isEmpresaModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-2xl">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-black">
                {empresaForm.id ? "Editar empresa" : "Nova empresa"}
              </h2>
              <button onClick={closeEmpresaModal} className="text-neutral-400 hover:text-black" type="button">
                ✕
              </button>
            </div>

            <form onSubmit={handleSubmitEmpresa} className="space-y-4">
              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Nome</label>
                <input
                  type="text"
                  value={empresaForm.nome}
                  onChange={(e) => setEmpresaForm((prev) => ({ ...prev, nome: e.target.value }))}
                  placeholder="Ex: At Promo"
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">CNPJ</label>
                <input
                  type="text"
                  value={empresaForm.cnpj}
                  onChange={(e) => setEmpresaForm((prev) => ({ ...prev, cnpj: e.target.value }))}
                  placeholder="00.000.000/0000-00"
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
              </div>

              {empresaForm.id && (
                <label className="flex items-center gap-2 text-sm font-medium text-neutral-700">
                  <input
                    type="checkbox"
                    checked={empresaForm.ativa}
                    onChange={(e) => setEmpresaForm((prev) => ({ ...prev, ativa: e.target.checked }))}
                    className="h-4 w-4 rounded border-neutral-300 text-orange-500 focus:ring-orange-400"
                  />
                  Empresa ativa
                </label>
              )}

              {empresaFormError && (
                <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{empresaFormError}</div>
              )}

              <div className="flex justify-end gap-3 border-t border-neutral-100 pt-4">
                <button
                  type="button"
                  onClick={closeEmpresaModal}
                  className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={savingEmpresa}
                  className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-60"
                >
                  {savingEmpresa ? "Salvando..." : "Salvar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {isContaModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="max-h-[90vh] w-full max-w-lg overflow-auto rounded-2xl bg-white p-6 shadow-2xl">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-black">
                {contaForm.id ? "Editar conta bancária" : "Nova conta bancária"}
              </h2>
              <button onClick={closeContaModal} className="text-neutral-400 hover:text-black" type="button">
                ✕
              </button>
            </div>

            <form onSubmit={handleSubmitConta} className="space-y-4">
              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Empresa</label>
                <select
                  value={contaForm.empresaId}
                  onChange={(e) => setContaForm((prev) => ({ ...prev, empresaId: e.target.value }))}
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                >
                  <option value="">Selecione...</option>
                  {empresas.map((empresa) => (
                    <option key={empresa.id} value={empresa.id}>
                      {empresa.nome}
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Banco</label>
                  <input
                    type="text"
                    value={contaForm.banco}
                    onChange={(e) => setContaForm((prev) => ({ ...prev, banco: e.target.value }))}
                    placeholder="Ex: Itaú"
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Apelido da conta</label>
                  <input
                    type="text"
                    value={contaForm.apelido}
                    onChange={(e) => setContaForm((prev) => ({ ...prev, apelido: e.target.value }))}
                    placeholder="Ex: Conta corrente"
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Agência (opcional)</label>
                  <input
                    type="text"
                    value={contaForm.agencia}
                    onChange={(e) => setContaForm((prev) => ({ ...prev, agencia: e.target.value }))}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Número da conta (opcional)</label>
                  <input
                    type="text"
                    value={contaForm.numeroConta}
                    onChange={(e) => setContaForm((prev) => ({ ...prev, numeroConta: e.target.value }))}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Saldo inicial (R$)</label>
                  <CurrencyInput
                    value={contaForm.saldoInicial}
                    onChange={(val) => setContaForm((prev) => ({ ...prev, saldoInicial: val }))}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Data do saldo inicial</label>
                  <input
                    type="date"
                    value={contaForm.dataSaldoInicial}
                    onChange={(e) => setContaForm((prev) => ({ ...prev, dataSaldoInicial: e.target.value }))}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>
              </div>

              {contaForm.id && (
                <label className="flex items-center gap-2 text-sm font-medium text-neutral-700">
                  <input
                    type="checkbox"
                    checked={contaForm.ativa}
                    onChange={(e) => setContaForm((prev) => ({ ...prev, ativa: e.target.checked }))}
                    className="h-4 w-4 rounded border-neutral-300 text-orange-500 focus:ring-orange-400"
                  />
                  Conta ativa
                </label>
              )}

              {contaFormError && (
                <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{contaFormError}</div>
              )}

              <div className="flex justify-end gap-3 border-t border-neutral-100 pt-4">
                <button
                  type="button"
                  onClick={closeContaModal}
                  className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={savingConta}
                  className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-60"
                >
                  {savingConta ? "Salvando..." : "Salvar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {isLancamentoModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl bg-white p-7 shadow-2xl">
            <div className="mb-5 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-black">
                {lancamentoForm.id
                  ? "Editar lançamento"
                  : lancamentoForm.tipo === "ENTRADA"
                  ? "Novo recebimento"
                  : "Novo pagamento"}
              </h2>
              <button onClick={closeLancamentoModal} className="text-neutral-400 hover:text-black" type="button">
                ✕
              </button>
            </div>

            <form onSubmit={handleSubmitLancamento} className="space-y-5">
              <div>
                <label className="mb-1.5 block text-sm font-medium text-neutral-700">Tipo</label>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => setLancamentoForm((prev) => ({ ...prev, tipo: "ENTRADA" }))}
                    className={`flex-1 rounded-lg border px-3 py-2.5 text-sm font-semibold transition-colors ${
                      lancamentoForm.tipo === "ENTRADA"
                        ? "border-green-500 bg-green-500 text-white shadow-sm"
                        : "border-neutral-300 text-neutral-600 hover:bg-neutral-50"
                    }`}
                  >
                    Entrada
                  </button>
                  <button
                    type="button"
                    onClick={() => setLancamentoForm((prev) => ({ ...prev, tipo: "SAIDA" }))}
                    className={`flex-1 rounded-lg border px-3 py-2.5 text-sm font-semibold transition-colors ${
                      lancamentoForm.tipo === "SAIDA"
                        ? "border-red-500 bg-red-500 text-white shadow-sm"
                        : "border-neutral-300 text-neutral-600 hover:bg-neutral-50"
                    }`}
                  >
                    Saída
                  </button>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-neutral-700">Valor (R$)</label>
                  <CurrencyInput
                    value={lancamentoForm.valor}
                    onChange={(val) => setLancamentoForm((prev) => ({ ...prev, valor: val }))}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-neutral-700">Data</label>
                  <input
                    type="date"
                    value={lancamentoForm.data}
                    onChange={(e) => setLancamentoForm((prev) => ({ ...prev, data: e.target.value }))}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>
              </div>

              <div>
                <label className="mb-1.5 block text-sm font-medium text-neutral-700">
                  Descrição <span className="font-normal text-neutral-400">(opcional)</span>
                </label>
                <input
                  type="text"
                  value={lancamentoForm.descricao}
                  onChange={(e) => setLancamentoForm((prev) => ({ ...prev, descricao: e.target.value }))}
                  placeholder="Ex: Taxa de manutenção, tarifa..."
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
              </div>

              <div className="rounded-xl border border-neutral-200 bg-neutral-50 p-4">
                <p className="mb-0.5 text-xs font-semibold uppercase tracking-wide text-neutral-500">Classificação</p>
                <p className="mb-3 text-xs text-neutral-400">Ajuda a organizar e filtrar o extrato depois.</p>

                <div className="space-y-3">
                  <div>
                    <label className="mb-1.5 block text-sm font-medium text-neutral-700">
                      Beneficiário <span className="font-normal text-neutral-400">(opcional)</span>
                    </label>
                    <CatalogCombobox
                      items={beneficiariosAtivos}
                      value={lancamentoForm.beneficiarioId}
                      onChange={(id) => setLancamentoForm((prev) => ({ ...prev, beneficiarioId: id }))}
                      onCreate={createBeneficiarioRapido}
                      placeholder="Quem recebe ou paga..."
                    />
                  </div>

                  <div>
                    <label className="mb-1.5 block text-sm font-medium text-neutral-700">Categoria</label>
                    <CatalogCombobox
                      items={categoriasAtivas}
                      value={lancamentoForm.categoriaId}
                      onChange={(id) => setLancamentoForm((prev) => ({ ...prev, categoriaId: id }))}
                      onCreate={createCategoriaRapida}
                      placeholder="Buscar ou cadastrar..."
                      allowClear={false}
                    />
                  </div>

                  <div>
                    <label className="mb-1.5 block text-sm font-medium text-neutral-700">Centro de custo</label>
                    <CatalogCombobox
                      items={centrosCustoAtivos}
                      value={lancamentoForm.centroCustoId}
                      onChange={(id) => setLancamentoForm((prev) => ({ ...prev, centroCustoId: id }))}
                      onCreate={createCentroCustoRapido}
                      placeholder="Buscar ou cadastrar..."
                      allowClear={false}
                    />
                  </div>
                </div>
              </div>

              {lancamentoFormError && (
                <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{lancamentoFormError}</div>
              )}

              <div className="flex justify-end gap-3 border-t border-neutral-100 pt-4">
                <button
                  type="button"
                  onClick={closeLancamentoModal}
                  className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={savingLancamento}
                  className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-60"
                >
                  {savingLancamento ? "Salvando..." : "Salvar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {isTransferenciaModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-2xl">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-black">Nova transferência</h2>
              <button onClick={closeTransferenciaModal} className="text-neutral-400 hover:text-black" type="button">
                ✕
              </button>
            </div>

            <form onSubmit={handleSubmitTransferencia} className="space-y-4">
              <div className="rounded-lg border border-neutral-200 bg-neutral-50 px-3 py-2 text-sm text-neutral-600">
                De: <span className="font-medium text-neutral-800">{contaSelecionada?.apelido}</span>
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Para a conta</label>
                <select
                  value={transferenciaForm.contaDestinoId}
                  onChange={(e) => setTransferenciaForm((prev) => ({ ...prev, contaDestinoId: e.target.value }))}
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                >
                  <option value="">Selecione...</option>
                  {contasAtivas
                    .filter((c) => String(c.id) !== String(selectedContaId))
                    .map((conta) => (
                      <option key={conta.id} value={conta.id}>
                        {conta.apelido} — {conta.banco}
                      </option>
                    ))}
                </select>
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Data</label>
                <input
                  type="date"
                  value={transferenciaForm.data}
                  onChange={(e) => setTransferenciaForm((prev) => ({ ...prev, data: e.target.value }))}
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Valor (R$)</label>
                <CurrencyInput
                  value={transferenciaForm.valor}
                  onChange={(val) => setTransferenciaForm((prev) => ({ ...prev, valor: val }))}
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Descrição (opcional)</label>
                <input
                  type="text"
                  value={transferenciaForm.descricao}
                  onChange={(e) => setTransferenciaForm((prev) => ({ ...prev, descricao: e.target.value }))}
                  placeholder="Ex: Reforço de caixa"
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
              </div>

              {transferenciaFormError && (
                <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{transferenciaFormError}</div>
              )}

              <div className="flex justify-end gap-3 border-t border-neutral-100 pt-4">
                <button
                  type="button"
                  onClick={closeTransferenciaModal}
                  className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={savingTransferencia}
                  className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-60"
                >
                  {savingTransferencia ? "Transferindo..." : "Transferir"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {lancarTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="max-h-[90vh] w-full max-w-md overflow-y-auto rounded-2xl bg-white p-7 shadow-2xl">
            <div className="mb-5 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-black">Lançar no extrato</h2>
              <button onClick={closeLancarModal} className="text-neutral-400 hover:text-black" type="button">
                ✕
              </button>
            </div>

            <div className="mb-5 rounded-lg border border-neutral-200 bg-neutral-50 px-3 py-2 text-sm text-neutral-600">
              <span className="font-medium text-neutral-800">{lancarTarget.titulo}</span>
              {lancarTarget.subtitulo && <> · {lancarTarget.subtitulo}</>} ·{" "}
              <span
                className={`font-semibold ${
                  lancarTarget.tipoMovimento === "ENTRADA" ? "text-green-700" : "text-red-700"
                }`}
              >
                {formatCurrency(lancarTarget.valor)}
              </span>
              <br />
              {lancarTarget.origemTipo === "SOLICITACAO_PIX"
                ? `Aprovado em ${formatDate(lancarTarget.data)}. O beneficiário é vinculado automaticamente ao cadastro desse promotor.`
                : lancarTarget.origemTipo === "FATURAMENTO"
                ? `Recebido em ${formatDate(lancarTarget.data)}. Se não escolher um beneficiário, ele é vinculado automaticamente ao cadastro desse cliente.`
                : `Pago em ${formatDate(lancarTarget.data)}.`}
            </div>

            <form onSubmit={handleSubmitLancar} className="space-y-4">
              <div>
                <label className="mb-1.5 block text-sm font-medium text-neutral-700">Conta bancária</label>
                <select
                  value={lancarForm.contaBancariaId}
                  onChange={(e) => setLancarForm((prev) => ({ ...prev, contaBancariaId: e.target.value }))}
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                >
                  <option value="">
                    {lancarTarget.tipoMovimento === "ENTRADA" ? "Para onde entrou o pagamento..." : "De onde saiu o pagamento..."}
                  </option>
                  {contasAtivas.map((conta) => (
                    <option key={conta.id} value={conta.id}>
                      {conta.apelido} — {conta.banco}
                    </option>
                  ))}
                </select>
              </div>

              {lancarTarget.origemTipo !== "SOLICITACAO_PIX" && (
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-neutral-700">
                    Beneficiário <span className="font-normal text-neutral-400">(opcional)</span>
                  </label>
                  <CatalogCombobox
                    items={beneficiarios}
                    value={lancarForm.beneficiarioId}
                    onChange={(id) => setLancarForm((prev) => ({ ...prev, beneficiarioId: id }))}
                    onCreate={createBeneficiarioRapido}
                    placeholder="Buscar ou cadastrar..."
                    clearLabel="Nenhum"
                  />
                </div>
              )}

              <div>
                <label className="mb-1.5 block text-sm font-medium text-neutral-700">Categoria</label>
                <CatalogCombobox
                  items={categorias}
                  value={lancarForm.categoriaId}
                  onChange={(id) => setLancarForm((prev) => ({ ...prev, categoriaId: id }))}
                  onCreate={createCategoriaRapida}
                  placeholder="Buscar ou cadastrar..."
                  allowClear={false}
                />
              </div>

              <div>
                <label className="mb-1.5 block text-sm font-medium text-neutral-700">Centro de custo</label>
                <CatalogCombobox
                  items={centrosCusto}
                  value={lancarForm.centroCustoId}
                  onChange={(id) => setLancarForm((prev) => ({ ...prev, centroCustoId: id }))}
                  onCreate={createCentroCustoRapido}
                  placeholder="Buscar ou cadastrar..."
                  allowClear={false}
                />
              </div>

              {lancarFormError && (
                <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{lancarFormError}</div>
              )}

              <div className="flex justify-end gap-3 border-t border-neutral-100 pt-4">
                <button
                  type="button"
                  onClick={closeLancarModal}
                  className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={savingLancar}
                  className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-60"
                >
                  {savingLancar ? "Lançando..." : "Lançar no extrato"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {isBeneficiarioModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-2xl">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-black">
                {beneficiarioForm.id ? "Editar beneficiário" : "Novo beneficiário"}
              </h2>
              <button onClick={closeBeneficiarioModal} className="text-neutral-400 hover:text-black" type="button">
                ✕
              </button>
            </div>

            <form onSubmit={handleSubmitBeneficiario} className="space-y-4">
              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Nome</label>
                <input
                  type="text"
                  value={beneficiarioForm.nome}
                  onChange={(e) => setBeneficiarioForm((prev) => ({ ...prev, nome: e.target.value }))}
                  placeholder="Ex: João da Silva"
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
              </div>

              {beneficiarioForm.id && (
                <label className="flex items-center gap-2 text-sm font-medium text-neutral-700">
                  <input
                    type="checkbox"
                    checked={beneficiarioForm.ativo}
                    onChange={(e) => setBeneficiarioForm((prev) => ({ ...prev, ativo: e.target.checked }))}
                    className="h-4 w-4 rounded border-neutral-300 text-orange-500 focus:ring-orange-400"
                  />
                  Beneficiário ativo
                </label>
              )}

              {beneficiarioFormError && (
                <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{beneficiarioFormError}</div>
              )}

              <div className="flex justify-end gap-3 border-t border-neutral-100 pt-4">
                <button
                  type="button"
                  onClick={closeBeneficiarioModal}
                  className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={savingBeneficiario}
                  className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-60"
                >
                  {savingBeneficiario ? "Salvando..." : "Salvar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {isCategoriaModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-2xl">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-black">
                {categoriaForm.id ? "Editar categoria" : "Nova categoria"}
              </h2>
              <button onClick={closeCategoriaModal} className="text-neutral-400 hover:text-black" type="button">
                ✕
              </button>
            </div>

            <form onSubmit={handleSubmitCategoria} className="space-y-4">
              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Nome</label>
                <input
                  type="text"
                  value={categoriaForm.nome}
                  onChange={(e) => setCategoriaForm((prev) => ({ ...prev, nome: e.target.value }))}
                  placeholder="Ex: Salário, Fornecedor, Imposto..."
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
              </div>

              {categoriaForm.id && (
                <label className="flex items-center gap-2 text-sm font-medium text-neutral-700">
                  <input
                    type="checkbox"
                    checked={categoriaForm.ativo}
                    onChange={(e) => setCategoriaForm((prev) => ({ ...prev, ativo: e.target.checked }))}
                    className="h-4 w-4 rounded border-neutral-300 text-orange-500 focus:ring-orange-400"
                  />
                  Categoria ativa
                </label>
              )}

              {categoriaFormError && (
                <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{categoriaFormError}</div>
              )}

              <div className="flex justify-end gap-3 border-t border-neutral-100 pt-4">
                <button
                  type="button"
                  onClick={closeCategoriaModal}
                  className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={savingCategoria}
                  className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-60"
                >
                  {savingCategoria ? "Salvando..." : "Salvar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {isCentroCustoModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-2xl">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-black">
                {centroCustoForm.id ? "Editar centro de custo" : "Novo centro de custo"}
              </h2>
              <button onClick={closeCentroCustoModal} className="text-neutral-400 hover:text-black" type="button">
                ✕
              </button>
            </div>

            <form onSubmit={handleSubmitCentroCusto} className="space-y-4">
              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Nome</label>
                <input
                  type="text"
                  value={centroCustoForm.nome}
                  onChange={(e) => setCentroCustoForm((prev) => ({ ...prev, nome: e.target.value }))}
                  placeholder="Ex: Custo Operacional AT"
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
              </div>

              {centroCustoForm.id && (
                <label className="flex items-center gap-2 text-sm font-medium text-neutral-700">
                  <input
                    type="checkbox"
                    checked={centroCustoForm.ativo}
                    onChange={(e) => setCentroCustoForm((prev) => ({ ...prev, ativo: e.target.checked }))}
                    className="h-4 w-4 rounded border-neutral-300 text-orange-500 focus:ring-orange-400"
                  />
                  Centro de custo ativo
                </label>
              )}

              {centroCustoFormError && (
                <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{centroCustoFormError}</div>
              )}

              <div className="flex justify-end gap-3 border-t border-neutral-100 pt-4">
                <button
                  type="button"
                  onClick={closeCentroCustoModal}
                  className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={savingCentroCusto}
                  className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-60"
                >
                  {savingCentroCusto ? "Salvando..." : "Salvar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <ConfirmDeleteDialog
        open={deleteEmpresaTarget !== null}
        onClose={() => setDeleteEmpresaTarget(null)}
        onConfirmed={confirmDeleteEmpresa}
        itemLabel="esta empresa"
      />

      <ConfirmDeleteDialog
        open={deleteBeneficiarioTarget !== null}
        onClose={() => setDeleteBeneficiarioTarget(null)}
        onConfirmed={confirmDeleteBeneficiario}
        itemLabel="este beneficiário"
      />

      <ConfirmDeleteDialog
        open={deleteCategoriaTarget !== null}
        onClose={() => setDeleteCategoriaTarget(null)}
        onConfirmed={confirmDeleteCategoria}
        itemLabel="esta categoria"
      />

      <ConfirmDeleteDialog
        open={deleteCentroCustoTarget !== null}
        onClose={() => setDeleteCentroCustoTarget(null)}
        onConfirmed={confirmDeleteCentroCusto}
        itemLabel="este centro de custo"
      />

      <ConfirmDeleteDialog
        open={deleteContaTarget !== null}
        onClose={() => setDeleteContaTarget(null)}
        onConfirmed={confirmDeleteConta}
        itemLabel="esta conta bancária"
      />

      <ConfirmDeleteDialog
        open={deleteLancamentoTarget !== null}
        onClose={() => setDeleteLancamentoTarget(null)}
        onConfirmed={confirmDeleteLancamento}
        itemLabel="este lançamento do extrato"
      />
    </Layout>
  );
}
