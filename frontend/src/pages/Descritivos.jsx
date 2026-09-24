import { useEffect, useState } from "react";
import { apiFetch } from "../api";
import { useAuth } from "../context/AuthContext";
import Layout from "../components/Layout";
import ConfirmDeleteDialog from "../components/ConfirmDeleteDialog";
import CurrencyInput from "../components/CurrencyInput";

const MESES = [
  "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
  "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro",
];

const DIAS_SEMANA = [
  { value: 1, label: "Seg" },
  { value: 2, label: "Ter" },
  { value: 3, label: "Qua" },
  { value: 4, label: "Qui" },
  { value: 5, label: "Sex" },
  { value: 6, label: "Sáb" },
  { value: 7, label: "Dom" },
];

function formatDate(value) {
  if (!value) return "-";
  const [year, month, day] = value.split("-");
  return `${day}/${month}/${year}`;
}

function formatCurrency(value) {
  if (value == null) return "-";
  return Number(value).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

function descritivoLabel(d) {
  if (!d) return "";
  return `${MESES[d.mes - 1]}/${d.ano}${d.rotuloExibicao ? ` — ${d.rotuloExibicao}` : ""}`;
}

const EMPTY_LINHA_FORM = {
  id: null,
  lojaId: "",
  diasSemana: [],
  horasPorAtendimento: "",
  valorHora: "",
  dataInicio: "",
  dataFim: "",
  manualEnabled: false,
  valorTotalManual: "",
};

const now = new Date();

export default function Descritivos() {
  const { token } = useAuth();

  const [clientes, setClientes] = useState([]);
  const [lojas, setLojas] = useState([]);
  const [selectedClienteId, setSelectedClienteId] = useState("");
  const [clienteSearch, setClienteSearch] = useState("");
  const [showClienteSuggestions, setShowClienteSuggestions] = useState(false);

  const [descritivos, setDescritivos] = useState([]);
  const [loadingDescritivos, setLoadingDescritivos] = useState(false);
  const [mesSearch, setMesSearch] = useState("");
  const [showMesSuggestions, setShowMesSuggestions] = useState(false);

  const [openDescritivo, setOpenDescritivo] = useState(null);
  const [loadingDetail, setLoadingDetail] = useState(false);

  const [error, setError] = useState("");

  const [isNewModalOpen, setIsNewModalOpen] = useState(false);
  const [newForm, setNewForm] = useState({ mes: now.getMonth() + 1, ano: now.getFullYear(), rotulo: "" });
  const [newFormError, setNewFormError] = useState("");
  const [creating, setCreating] = useState(false);

  const [isLinhaModalOpen, setIsLinhaModalOpen] = useState(false);
  const [linhaForm, setLinhaForm] = useState(EMPTY_LINHA_FORM);
  const [linhaFormError, setLinhaFormError] = useState("");
  const [savingLinha, setSavingLinha] = useState(false);

  const [deleteLinhaTarget, setDeleteLinhaTarget] = useState(null);
  const [deleteDescritivoTarget, setDeleteDescritivoTarget] = useState(false);

  const [gerandoProximoMes, setGerandoProximoMes] = useState(false);
  const [gerarProximoMesConfirmOpen, setGerarProximoMesConfirmOpen] = useState(false);

  const [selectedLinhaIds, setSelectedLinhaIds] = useState(new Set());
  const [isBulkValueModalOpen, setIsBulkValueModalOpen] = useState(false);
  const [bulkValueField, setBulkValueField] = useState("valorHora");
  const [bulkValueAmount, setBulkValueAmount] = useState("");
  const [bulkValueError, setBulkValueError] = useState("");
  const [applyingBulkValue, setApplyingBulkValue] = useState(false);
  const [bulkDeleteConfirmOpen, setBulkDeleteConfirmOpen] = useState(false);
  const [deletingBulk, setDeletingBulk] = useState(false);
  const [selectionMode, setSelectionMode] = useState(false);

  useEffect(() => {
    setSelectedLinhaIds(new Set());
    setSelectionMode(false);
  }, [openDescritivo?.id]);

  useEffect(() => {
    async function loadBase() {
      try {
        const [clientesData, lojasData] = await Promise.all([
          apiFetch("/clients", { token }),
          apiFetch("/lojas", { token }),
        ]);
        setClientes(clientesData || []);
        setLojas((lojasData || []).filter((l) => l.active));
      } catch (err) {
        setError("Não foi possível carregar clientes/lojas.");
      }
    }
    loadBase();
  }, []);

  async function loadDescritivos(clienteId) {
    if (!clienteId) {
      setDescritivos([]);
      setOpenDescritivo(null);
      return;
    }
    setLoadingDescritivos(true);
    setError("");
    try {
      const data = await apiFetch(`/descritivos?clienteId=${clienteId}`, { token });
      const lista = data || [];
      setDescritivos(lista);
      if (lista.length > 0) {
        const maisRecente = [...lista].sort((a, b) => b.ano - a.ano || b.mes - a.mes)[0];
        await openDescritivoDetail(maisRecente.id);
      } else {
        setOpenDescritivo(null);
      }
    } catch (err) {
      setError("Não foi possível carregar os descritivos desse cliente.");
    } finally {
      setLoadingDescritivos(false);
    }
  }

  async function openDescritivoDetail(id) {
    setLoadingDetail(true);
    setError("");
    try {
      const data = await apiFetch(`/descritivos/${id}`, { token });
      setOpenDescritivo(data);
    } catch (err) {
      setError("Não foi possível carregar o descritivo.");
    } finally {
      setLoadingDetail(false);
    }
  }

  useEffect(() => {
    if (!showMesSuggestions) {
      setMesSearch(openDescritivo ? descritivoLabel(openDescritivo) : "");
    }
  }, [openDescritivo]);

  function handlePickDescritivo(d) {
    setMesSearch(descritivoLabel(d));
    setShowMesSuggestions(false);
    if (String(d.id) !== String(openDescritivo?.id)) {
      openDescritivoDetail(d.id);
    }
  }

  function handleMesInputChange(value) {
    setMesSearch(value);
    setShowMesSuggestions(true);
  }

  function handleMesInputFocus() {
    setShowMesSuggestions(true);
    if (openDescritivo) {
      setMesSearch("");
    }
  }

  function handleMesInputBlur() {
    setTimeout(() => {
      setShowMesSuggestions(false);
      setMesSearch(openDescritivo ? descritivoLabel(openDescritivo) : "");
    }, 150);
  }

  function handleSelectCliente(id) {
    setSelectedClienteId(id);
    setOpenDescritivo(null);
    if (id) {
      loadDescritivos(id);
    } else {
      setDescritivos([]);
    }
  }

  function handlePickCliente(cliente) {
    setClienteSearch(cliente.name);
    setShowClienteSuggestions(false);
    handleSelectCliente(String(cliente.id));
  }

  function handleClienteInputChange(value) {
    setClienteSearch(value);
    setShowClienteSuggestions(true);
  }

  function handleClienteInputFocus() {
    setShowClienteSuggestions(true);
    if (selectedClienteId) {
      setClienteSearch("");
    }
  }

  function handleClienteInputBlur() {
    setTimeout(() => {
      setShowClienteSuggestions(false);
      if (selectedClienteId) {
        const atual = clientes.find((c) => String(c.id) === String(selectedClienteId));
        setClienteSearch(atual?.name || "");
      }
    }, 150);
  }

  function clearClienteSelection() {
    setClienteSearch("");
    setShowClienteSuggestions(false);
    setSelectedClienteId("");
    setDescritivos([]);
    setOpenDescritivo(null);
  }

  function openNewModal() {
    setNewForm({ mes: now.getMonth() + 1, ano: now.getFullYear(), rotulo: "" });
    setNewFormError("");
    setIsNewModalOpen(true);
  }

  async function handleCreateDescritivo(event) {
    event.preventDefault();
    setNewFormError("");
    setCreating(true);
    try {
      const data = await apiFetch("/descritivos", {
        method: "POST",
        body: {
          clienteId: Number(selectedClienteId),
          mes: Number(newForm.mes),
          ano: Number(newForm.ano),
          rotulo: newForm.rotulo.trim() === "" ? null : newForm.rotulo.trim(),
        },
        token,
      });
      setIsNewModalOpen(false);
      setOpenDescritivo(data);
      await loadDescritivos(selectedClienteId);
    } catch (err) {
      setNewFormError(err.message || "Não foi possível criar o descritivo.");
    } finally {
      setCreating(false);
    }
  }

  async function handleDeleteDescritivo() {
    if (!openDescritivo) return;
    try {
      await apiFetch(`/descritivos/${openDescritivo.id}`, { method: "DELETE", token });
      setDeleteDescritivoTarget(false);
      setOpenDescritivo(null);
      await loadDescritivos(selectedClienteId);
    } catch (err) {
      setDeleteDescritivoTarget(false);
      setError(err.message || "Não foi possível excluir o descritivo.");
    }
  }

  async function handleGerarProximoMes() {
    if (!openDescritivo) return;
    setGerandoProximoMes(true);
    setError("");
    try {
      const data = await apiFetch(`/descritivos/${openDescritivo.id}/gerar-proximo-mes`, { method: "POST", token });
      setOpenDescritivo(data);
      await loadDescritivos(selectedClienteId);
    } catch (err) {
      setError(err.message || "Não foi possível gerar o próximo mês.");
    } finally {
      setGerandoProximoMes(false);
      setGerarProximoMesConfirmOpen(false);
    }
  }

  function openNewLinha() {
    setLinhaForm(EMPTY_LINHA_FORM);
    setLinhaFormError("");
    setIsLinhaModalOpen(true);
  }

  function openEditLinha(linha) {
    setLinhaForm({
      id: linha.id,
      lojaId: linha.lojaId,
      diasSemana: linha.diasSemana || [],
      horasPorAtendimento: linha.horasPorAtendimento ?? "",
      valorHora: linha.valorHora ?? "",
      dataInicio: linha.dataInicio || "",
      dataFim: linha.dataFim || "",
      manualEnabled: linha.valorTotalManual,
      valorTotalManual: linha.valorTotalManual ? linha.valorTotal ?? "" : "",
    });
    setLinhaFormError("");
    setIsLinhaModalOpen(true);
  }

  function closeLinhaModal() {
    setIsLinhaModalOpen(false);
    setLinhaForm(EMPTY_LINHA_FORM);
    setLinhaFormError("");
  }

  function toggleDiaSemana(value) {
    setLinhaForm((prev) => {
      const has = prev.diasSemana.includes(value);
      const diasSemana = has
        ? prev.diasSemana.filter((d) => d !== value)
        : [...prev.diasSemana, value].sort((a, b) => a - b);
      return { ...prev, diasSemana };
    });
  }

  async function handleSubmitLinha(event) {
    event.preventDefault();
    setLinhaFormError("");

    if (!linhaForm.lojaId) {
      setLinhaFormError("Selecione uma loja.");
      return;
    }
    if (linhaForm.diasSemana.length === 0) {
      setLinhaFormError("Selecione ao menos um dia da semana.");
      return;
    }
    if (!linhaForm.horasPorAtendimento || !linhaForm.valorHora || !linhaForm.dataInicio) {
      setLinhaFormError("Preencha horas por atendimento, valor da hora e data de início.");
      return;
    }
    if (linhaForm.manualEnabled && linhaForm.valorTotalManual === "") {
      setLinhaFormError("Informe o valor manual, ou desmarque a opção de valor manual.");
      return;
    }

    setSavingLinha(true);
    const payload = {
      lojaId: Number(linhaForm.lojaId),
      diasSemana: linhaForm.diasSemana,
      horasPorAtendimento: Number(linhaForm.horasPorAtendimento),
      valorHora: Number(linhaForm.valorHora),
      dataInicio: linhaForm.dataInicio,
      dataFim: linhaForm.dataFim || null,
      valorTotalManual: linhaForm.manualEnabled ? Number(linhaForm.valorTotalManual) : null,
    };

    try {
      const data = linhaForm.id
        ? await apiFetch(`/descritivos/${openDescritivo.id}/linhas/${linhaForm.id}`, { method: "PUT", body: payload, token })
        : await apiFetch(`/descritivos/${openDescritivo.id}/linhas`, { method: "POST", body: payload, token });

      setOpenDescritivo(data);
      closeLinhaModal();
    } catch (err) {
      setLinhaFormError(err.message || "Não foi possível salvar a loja no descritivo.");
    } finally {
      setSavingLinha(false);
    }
  }

  async function confirmDeleteLinha() {
    try {
      const data = await apiFetch(`/descritivos/${openDescritivo.id}/linhas/${deleteLinhaTarget}`, {
        method: "DELETE",
        token,
      });
      setOpenDescritivo(data);
      setDeleteLinhaTarget(null);
    } catch (err) {
      setDeleteLinhaTarget(null);
      setError(err.message || "Não foi possível remover a loja do descritivo.");
    }
  }

  function toggleLinhaSelecionada(id) {
    setSelectedLinhaIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  function toggleTodasLinhas() {
    if (!openDescritivo) return;
    setSelectedLinhaIds((prev) => {
      if (prev.size === openDescritivo.linhas.length) {
        return new Set();
      }
      return new Set(openDescritivo.linhas.map((l) => l.id));
    });
  }

  function clearSelecaoLinhas() {
    setSelectedLinhaIds(new Set());
  }

  function toggleSelectionMode() {
    setSelectionMode((prev) => {
      if (prev) {
        setSelectedLinhaIds(new Set());
      }
      return !prev;
    });
  }

  function openBulkValueModal() {
    setBulkValueField("valorHora");
    setBulkValueAmount("");
    setBulkValueError("");
    setIsBulkValueModalOpen(true);
  }

  function closeBulkValueModal() {
    setIsBulkValueModalOpen(false);
    setBulkValueAmount("");
    setBulkValueError("");
  }

  async function handleApplyBulkValue(event) {
    event.preventDefault();
    setBulkValueError("");

    if (bulkValueAmount === "") {
      setBulkValueError("Informe o valor a aplicar.");
      return;
    }

    setApplyingBulkValue(true);
    try {
      const linhasSelecionadas = openDescritivo.linhas.filter((l) => selectedLinhaIds.has(l.id));
      for (const linha of linhasSelecionadas) {
        const payload = {
          lojaId: linha.lojaId,
          diasSemana: linha.diasSemana,
          horasPorAtendimento: Number(linha.horasPorAtendimento),
          valorHora: bulkValueField === "valorHora" ? Number(bulkValueAmount) : Number(linha.valorHora),
          dataInicio: linha.dataInicio,
          dataFim: linha.dataFim || null,
          valorTotalManual: bulkValueField === "valorTotalManual" ? Number(bulkValueAmount) : null,
        };
        await apiFetch(`/descritivos/${openDescritivo.id}/linhas/${linha.id}`, {
          method: "PUT",
          body: payload,
          token,
        });
      }
      await openDescritivoDetail(openDescritivo.id);
      clearSelecaoLinhas();
      closeBulkValueModal();
    } catch (err) {
      setBulkValueError(err.message || "Não foi possível aplicar o valor para as lojas selecionadas.");
    } finally {
      setApplyingBulkValue(false);
    }
  }

  async function confirmBulkDelete() {
    setDeletingBulk(true);
    try {
      const linhasSelecionadas = openDescritivo.linhas.filter((l) => selectedLinhaIds.has(l.id));
      for (const linha of linhasSelecionadas) {
        await apiFetch(`/descritivos/${openDescritivo.id}/linhas/${linha.id}`, {
          method: "DELETE",
          token,
        });
      }
      await openDescritivoDetail(openDescritivo.id);
      clearSelecaoLinhas();
      setBulkDeleteConfirmOpen(false);
    } catch (err) {
      setBulkDeleteConfirmOpen(false);
      setError(err.message || "Não foi possível excluir as lojas selecionadas.");
    } finally {
      setDeletingBulk(false);
    }
  }

  const clienteSelecionado = clientes.find((c) => String(c.id) === String(selectedClienteId));
  const filteredClientes = clientes.filter((c) =>
    (c.name || "").toLowerCase().includes(clienteSearch.trim().toLowerCase())
  );

  const descritivosOrdenados = [...descritivos].sort(
    (a, b) => b.ano - a.ano || b.mes - a.mes || a.id - b.id
  );
  const filteredDescritivos = descritivosOrdenados.filter((d) =>
    descritivoLabel(d).toLowerCase().includes(mesSearch.trim().toLowerCase())
  );

  return (
    <Layout title="Descritivos">
      <div className="mb-6 flex flex-wrap items-end gap-3 rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
        <div className="relative min-w-[260px] flex-1">
          <label className="mb-1 block text-sm font-medium text-neutral-700">Cliente</label>
          <div className="relative">
            <input
              type="text"
              value={clienteSearch}
              onChange={(e) => handleClienteInputChange(e.target.value)}
              onFocus={handleClienteInputFocus}
              onBlur={handleClienteInputBlur}
              placeholder="Buscar cliente pelo nome..."
              className="w-full rounded-lg border border-neutral-300 px-3 py-2 pr-8 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
            />
            {(clienteSearch || selectedClienteId) && (
              <button
                type="button"
                onMouseDown={(e) => e.preventDefault()}
                onClick={clearClienteSelection}
                className="absolute right-2 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-neutral-600"
              >
                ✕
              </button>
            )}
          </div>

          {showClienteSuggestions && (
            <div className="absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-lg border border-neutral-200 bg-white shadow-lg">
              {filteredClientes.length === 0 ? (
                <p className="px-3 py-2 text-sm text-neutral-400">Nenhum cliente encontrado.</p>
              ) : (
                filteredClientes.map((c) => (
                  <button
                    key={c.id}
                    type="button"
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={() => handlePickCliente(c)}
                    className={`block w-full px-3 py-2 text-left text-sm hover:bg-orange-50 ${
                      String(c.id) === String(selectedClienteId)
                        ? "bg-orange-50 font-medium text-orange-700"
                        : "text-neutral-700"
                    }`}
                  >
                    {c.name}
                  </button>
                ))
              )}
            </div>
          )}
        </div>

        {selectedClienteId && (
          <div className="relative min-w-[220px]">
            <label className="mb-1 block text-sm font-medium text-neutral-700">Mês do descritivo</label>
            <input
              type="text"
              value={mesSearch}
              onChange={(e) => handleMesInputChange(e.target.value)}
              onFocus={handleMesInputFocus}
              onBlur={handleMesInputBlur}
              disabled={descritivos.length === 0}
              placeholder={descritivos.length === 0 ? "Nenhum descritivo ainda" : "Buscar mês, ano ou rótulo..."}
              className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100 disabled:bg-neutral-100 disabled:text-neutral-400"
            />

            {showMesSuggestions && descritivos.length > 0 && (
              <div className="absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-lg border border-neutral-200 bg-white shadow-lg">
                {filteredDescritivos.length === 0 ? (
                  <p className="px-3 py-2 text-sm text-neutral-400">Nenhum descritivo encontrado.</p>
                ) : (
                  filteredDescritivos.map((d) => (
                    <button
                      key={d.id}
                      type="button"
                      onMouseDown={(e) => e.preventDefault()}
                      onClick={() => handlePickDescritivo(d)}
                      className={`block w-full px-3 py-2 text-left text-sm hover:bg-orange-50 ${
                        String(d.id) === String(openDescritivo?.id)
                          ? "bg-orange-50 font-medium text-orange-700"
                          : "text-neutral-700"
                      }`}
                    >
                      {descritivoLabel(d)}
                    </button>
                  ))
                )}
              </div>
            )}
          </div>
        )}

        {selectedClienteId && (
          <button
            onClick={openNewModal}
            className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600"
          >
            + Novo descritivo
          </button>
        )}
      </div>

      {error && (
        <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>
      )}

      {!selectedClienteId && (
        <div className="rounded-xl border border-dashed border-neutral-300 bg-white p-10 text-center text-sm text-neutral-500">
          Selecione um cliente acima pra ver ou criar os descritivos dele.
        </div>
      )}

      {selectedClienteId && loadingDescritivos && (
        <div className="rounded-xl border border-neutral-200 bg-white p-10 text-center text-sm text-neutral-500">
          Carregando...
        </div>
      )}

      {selectedClienteId && !loadingDescritivos && descritivos.length === 0 && (
        <div className="rounded-xl border border-dashed border-neutral-300 bg-white p-10 text-center text-sm text-neutral-500">
          Esse cliente ainda não tem nenhum descritivo. Clica em "+ Novo descritivo" pra criar o primeiro.
        </div>
      )}

      {openDescritivo && !loadingDetail && (
        <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
          <div className="flex flex-wrap items-start justify-between gap-4 border-b border-neutral-200 bg-neutral-50 px-6 py-5">
            <div>
              <p className="text-lg font-semibold text-black">
                {openDescritivo.clienteNomeFantasia || clienteSelecionado?.name}
              </p>
              <p className="text-sm text-neutral-500">
                {openDescritivo.clienteRazaoSocial && <>Razão social: {openDescritivo.clienteRazaoSocial} · </>}
                CNPJ: {openDescritivo.clienteCnpj || "-"}
              </p>
              <p className="mt-1 flex items-center gap-2 text-sm font-medium text-orange-600">
                <span>
                  {MESES[openDescritivo.mes - 1]}/{openDescritivo.ano}
                </span>
                {openDescritivo.rotuloExibicao && (
                  <span className="rounded-full bg-orange-100 px-2 py-0.5 text-xs font-semibold text-orange-700">
                    {openDescritivo.rotuloExibicao}
                  </span>
                )}
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <div className="rounded-lg bg-white px-4 py-2 text-right shadow-sm">
                <p className="text-xs text-neutral-400">Valor total do mês</p>
                <p className="text-lg font-semibold text-black">{formatCurrency(openDescritivo.valorTotalGeral)}</p>
              </div>
              <button
                onClick={() => setGerarProximoMesConfirmOpen(true)}
                disabled={gerandoProximoMes}
                className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-100 disabled:opacity-60"
              >
                {gerandoProximoMes ? "Gerando..." : "Gerar próximo mês"}
              </button>
              <button
                onClick={() => setDeleteDescritivoTarget(true)}
                className="rounded-lg border border-red-200 px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50"
              >
                Excluir descritivo
              </button>
            </div>
          </div>

          <div className="flex items-center justify-between px-6 pt-4">
            <p className="text-sm text-neutral-500">
              {openDescritivo.linhas.length} loja{openDescritivo.linhas.length !== 1 ? "s" : ""} nesse mês
            </p>
            <div className="flex items-center gap-2">
              {openDescritivo.linhas.length > 0 && (
                <button
                  type="button"
                  onClick={toggleSelectionMode}
                  className={`rounded-lg border px-4 py-2 text-sm font-medium transition-colors ${
                    selectionMode
                      ? "border-orange-300 bg-orange-50 text-orange-700 hover:bg-orange-100"
                      : "border-neutral-300 text-neutral-700 hover:bg-neutral-100"
                  }`}
                >
                  {selectionMode ? "Cancelar seleção" : "Selecionar várias"}
                </button>
              )}
              <button
                onClick={openNewLinha}
                className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600"
              >
                + Adicionar loja
              </button>
            </div>
          </div>

          {selectionMode && (
            <div className="mx-6 mt-4 flex flex-wrap items-center gap-3 rounded-lg border border-orange-200 bg-orange-50 px-4 py-3">
              <p className="text-sm font-medium text-orange-800">
                {selectedLinhaIds.size} loja{selectedLinhaIds.size !== 1 ? "s" : ""} selecionada
                {selectedLinhaIds.size !== 1 ? "s" : ""}
              </p>
              <button
                type="button"
                onClick={openBulkValueModal}
                disabled={selectedLinhaIds.size === 0}
                className="rounded-lg border border-orange-300 bg-white px-3 py-1.5 text-sm font-medium text-orange-700 hover:bg-orange-100 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Mudar valor
              </button>
              <button
                type="button"
                onClick={() => setBulkDeleteConfirmOpen(true)}
                disabled={selectedLinhaIds.size === 0}
                className="rounded-lg border border-red-200 bg-white px-3 py-1.5 text-sm font-medium text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Excluir selecionadas
              </button>
            </div>
          )}

          <div className="overflow-x-auto p-6">
            <table className="w-full text-left text-sm">
              <thead className="bg-black text-white">
                <tr>
                  {selectionMode && (
                    <th className="px-3 py-3 font-medium">
                      <input
                        type="checkbox"
                        checked={openDescritivo.linhas.length > 0 && selectedLinhaIds.size === openDescritivo.linhas.length}
                        onChange={toggleTodasLinhas}
                        disabled={openDescritivo.linhas.length === 0}
                        className="h-4 w-4 rounded border-neutral-300 text-orange-500 focus:ring-orange-400"
                      />
                    </th>
                  )}
                  <th className="px-3 py-3 font-medium">Nº</th>
                  <th className="px-3 py-3 font-medium">CNPJ</th>
                  <th className="px-3 py-3 font-medium">Loja</th>
                  <th className="px-3 py-3 font-medium">Atendimento</th>
                  <th className="px-3 py-3 font-medium">Valor/hora</th>
                  <th className="px-3 py-3 font-medium">Data</th>
                  <th className="px-3 py-3 font-medium">Valor total</th>
                  <th className="px-3 py-3 font-medium">UF</th>
                  <th className="px-3 py-3 font-medium text-right">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {openDescritivo.linhas.length === 0 ? (
                  <tr>
                    <td colSpan={selectionMode ? "10" : "9"} className="px-3 py-8 text-center text-neutral-400">
                      Nenhuma loja adicionada nesse descritivo ainda.
                    </td>
                  </tr>
                ) : (
                  openDescritivo.linhas.map((linha) => (
                    <tr
                      key={linha.id}
                      className={`hover:bg-orange-50/40 ${selectedLinhaIds.has(linha.id) ? "bg-orange-50/60" : ""}`}
                    >
                      {selectionMode && (
                        <td className="px-3 py-3">
                          <input
                            type="checkbox"
                            checked={selectedLinhaIds.has(linha.id)}
                            onChange={() => toggleLinhaSelecionada(linha.id)}
                            className="h-4 w-4 rounded border-neutral-300 text-orange-500 focus:ring-orange-400"
                          />
                        </td>
                      )}
                      <td className="px-3 py-3 text-neutral-500">{linha.numero}</td>
                      <td className="px-3 py-3 text-neutral-600">{linha.lojaCnpj || "-"}</td>
                      <td className="px-3 py-3 font-medium text-neutral-800">{linha.lojaNome}</td>
                      <td className="px-3 py-3 text-neutral-600">{linha.quantidadeAtendimentoLabel}</td>
                      <td className="px-3 py-3 text-neutral-600">{formatCurrency(linha.valorHora)}</td>
                      <td className={`px-3 py-3 ${linha.encerrandoNesteMes ? "font-medium text-red-600" : "text-neutral-600"}`}>
                        {formatDate(linha.dataInicioExibicao)} à {formatDate(linha.dataFimExibicao)}
                      </td>
                      <td className="px-3 py-3 text-neutral-700">
                        {formatCurrency(linha.valorTotal)}
                        {linha.valorTotalManual && (
                          <span className="ml-2 rounded-full bg-neutral-100 px-2 py-0.5 text-xs text-neutral-500">manual</span>
                        )}
                      </td>
                      <td className="px-3 py-3 text-neutral-600">{linha.lojaUf || "-"}</td>
                      <td className="px-3 py-3 text-right">
                        <button
                          onClick={() => openEditLinha(linha)}
                          className="mr-3 text-sm font-medium text-neutral-600 hover:text-orange-600"
                        >
                          Editar
                        </button>
                        <button
                          onClick={() => setDeleteLinhaTarget(linha.id)}
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

      {isNewModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-2xl">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-black">Novo descritivo</h2>
              <button onClick={() => setIsNewModalOpen(false)} className="text-neutral-400 hover:text-black" type="button">
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateDescritivo} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Mês</label>
                  <select
                    value={newForm.mes}
                    onChange={(e) => setNewForm((prev) => ({ ...prev, mes: e.target.value }))}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  >
                    {MESES.map((nome, index) => (
                      <option key={nome} value={index + 1}>
                        {nome}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Ano</label>
                  <input
                    type="number"
                    value={newForm.ano}
                    onChange={(e) => setNewForm((prev) => ({ ...prev, ano: e.target.value }))}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Rótulo (opcional)</label>
                <input
                  type="text"
                  value={newForm.rotulo}
                  onChange={(e) => setNewForm((prev) => ({ ...prev, rotulo: e.target.value }))}
                  placeholder="Ex: SP, Atacadão..."
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
                <p className="mt-1 text-xs text-neutral-400">
                  Só preenche se esse cliente tiver mais de um descritivo no mesmo mês (ex: separado por estado ou
                  rede). Deixa em branco no caso comum.
                </p>
              </div>

              {newFormError && (
                <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{newFormError}</div>
              )}

              <div className="flex justify-end gap-3 border-t border-neutral-100 pt-4">
                <button
                  type="button"
                  onClick={() => setIsNewModalOpen(false)}
                  className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={creating}
                  className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-60"
                >
                  {creating ? "Criando..." : "Criar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {isLinhaModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="max-h-[90vh] w-full max-w-xl overflow-auto rounded-2xl bg-white p-6 shadow-2xl">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-black">
                {linhaForm.id ? "Editar loja do descritivo" : "Adicionar loja ao descritivo"}
              </h2>
              <button onClick={closeLinhaModal} className="text-neutral-400 hover:text-black" type="button">
                ✕
              </button>
            </div>

            <form onSubmit={handleSubmitLinha} className="space-y-4">
              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Loja</label>
                <select
                  value={linhaForm.lojaId}
                  onChange={(e) => setLinhaForm((prev) => ({ ...prev, lojaId: e.target.value }))}
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                >
                  <option value="">Selecione...</option>
                  {lojas.map((loja) => (
                    <option key={loja.id} value={loja.id}>
                      {loja.nome}
                      {loja.rede ? ` (${loja.rede})` : ""}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Dias de atendimento na semana</label>
                <div className="flex flex-wrap gap-2">
                  {DIAS_SEMANA.map((dia) => {
                    const selected = linhaForm.diasSemana.includes(dia.value);
                    return (
                      <button
                        key={dia.value}
                        type="button"
                        onClick={() => toggleDiaSemana(dia.value)}
                        className={`rounded-lg border px-3 py-1.5 text-sm font-medium transition-colors ${
                          selected
                            ? "border-orange-500 bg-orange-500 text-black"
                            : "border-neutral-300 text-neutral-600 hover:bg-neutral-50"
                        }`}
                      >
                        {dia.label}
                      </button>
                    );
                  })}
                </div>
                {linhaForm.diasSemana.length > 0 && (
                  <p className="mt-1 text-xs text-neutral-400">
                    {linhaForm.diasSemana.length}X por semana
                  </p>
                )}
              </div>

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Horas por atendimento</label>
                  <input
                    type="number"
                    step="0.5"
                    min="0"
                    value={linhaForm.horasPorAtendimento}
                    onChange={(e) => setLinhaForm((prev) => ({ ...prev, horasPorAtendimento: e.target.value }))}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Valor da hora (R$)</label>
                  <CurrencyInput
                    value={linhaForm.valorHora}
                    onChange={(val) => setLinhaForm((prev) => ({ ...prev, valorHora: val }))}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Data de início</label>
                  <input
                    type="date"
                    value={linhaForm.dataInicio}
                    onChange={(e) => setLinhaForm((prev) => ({ ...prev, dataInicio: e.target.value }))}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium text-neutral-700">Data de término (opcional)</label>
                  <input
                    type="date"
                    value={linhaForm.dataFim}
                    onChange={(e) => setLinhaForm((prev) => ({ ...prev, dataFim: e.target.value }))}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                  <p className="mt-1 text-xs text-neutral-400">Deixe em branco se a loja continua no mês seguinte.</p>
                </div>
              </div>

              <div className="rounded-lg border border-neutral-200 p-3">
                <label className="flex items-center gap-2 text-sm font-medium text-neutral-700">
                  <input
                    type="checkbox"
                    checked={linhaForm.manualEnabled}
                    onChange={(e) => setLinhaForm((prev) => ({ ...prev, manualEnabled: e.target.checked }))}
                    className="h-4 w-4 rounded border-neutral-300 text-orange-500 focus:ring-orange-400"
                  />
                  Definir o valor total manualmente
                </label>
                {linhaForm.manualEnabled && (
                  <CurrencyInput
                    value={linhaForm.valorTotalManual}
                    onChange={(val) => setLinhaForm((prev) => ({ ...prev, valorTotalManual: val }))}
                    placeholder="Valor total (R$)"
                    className="mt-3 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                  />
                )}
                {!linhaForm.manualEnabled && (
                  <p className="mt-2 text-xs text-neutral-400">
                    Se deixar desmarcado, o valor total é calculado automaticamente pelos dias/horas/valor da hora.
                  </p>
                )}
              </div>

              {linhaFormError && (
                <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{linhaFormError}</div>
              )}

              <div className="flex justify-end gap-3 border-t border-neutral-100 pt-4">
                <button
                  type="button"
                  onClick={closeLinhaModal}
                  className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={savingLinha}
                  className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-60"
                >
                  {savingLinha ? "Salvando..." : linhaForm.id ? "Salvar alterações" : "Adicionar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {gerarProximoMesConfirmOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-2xl">
            <h2 className="mb-2 text-lg font-semibold text-black">Gerar próximo mês</h2>
            <p className="mb-6 text-sm text-neutral-600">
              Tem certeza que deseja gerar o descritivo do próximo mês a partir deste? Todas as lojas e configurações
              deste mês serão copiadas para o mês seguinte.
            </p>
            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={() => setGerarProximoMesConfirmOpen(false)}
                disabled={gerandoProximoMes}
                className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleGerarProximoMes}
                disabled={gerandoProximoMes}
                className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-60"
              >
                {gerandoProximoMes ? "Gerando..." : "Sim, gerar"}
              </button>
            </div>
          </div>
        </div>
      )}

      {isBulkValueModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-2xl">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-black">Mudar valor em massa</h2>
              <button onClick={closeBulkValueModal} className="text-neutral-400 hover:text-black" type="button">
                ✕
              </button>
            </div>

            <form onSubmit={handleApplyBulkValue} className="space-y-4">
              <p className="text-sm text-neutral-500">
                Aplicar novo valor em {selectedLinhaIds.size} loja{selectedLinhaIds.size !== 1 ? "s" : ""} selecionada
                {selectedLinhaIds.size !== 1 ? "s" : ""}.
              </p>

              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Qual campo alterar</label>
                <div className="space-y-2">
                  <label className="flex items-center gap-2 text-sm text-neutral-700">
                    <input
                      type="radio"
                      name="bulkValueField"
                      value="valorHora"
                      checked={bulkValueField === "valorHora"}
                      onChange={() => setBulkValueField("valorHora")}
                      className="h-4 w-4 border-neutral-300 text-orange-500 focus:ring-orange-400"
                    />
                    Valor da hora
                  </label>
                  <label className="flex items-center gap-2 text-sm text-neutral-700">
                    <input
                      type="radio"
                      name="bulkValueField"
                      value="valorTotalManual"
                      checked={bulkValueField === "valorTotalManual"}
                      onChange={() => setBulkValueField("valorTotalManual")}
                      className="h-4 w-4 border-neutral-300 text-orange-500 focus:ring-orange-400"
                    />
                    Valor total (fixo/manual)
                  </label>
                </div>
                {bulkValueField === "valorHora" && (
                  <p className="mt-2 text-xs text-neutral-400">
                    Se alguma loja selecionada tiver valor total manual, ele será desativado e o total volta a ser
                    calculado automaticamente pelo valor da hora.
                  </p>
                )}
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-neutral-700">Novo valor (R$)</label>
                <CurrencyInput
                  value={bulkValueAmount}
                  onChange={setBulkValueAmount}
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
              </div>

              {bulkValueError && (
                <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{bulkValueError}</div>
              )}

              <div className="flex justify-end gap-3 border-t border-neutral-100 pt-4">
                <button
                  type="button"
                  onClick={closeBulkValueModal}
                  className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={applyingBulkValue}
                  className="rounded-lg bg-orange-500 px-4 py-2 text-sm font-semibold text-black transition-colors hover:bg-orange-600 disabled:opacity-60"
                >
                  {applyingBulkValue ? "Aplicando..." : "Aplicar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <ConfirmDeleteDialog
        open={deleteLinhaTarget !== null}
        onClose={() => setDeleteLinhaTarget(null)}
        onConfirmed={confirmDeleteLinha}
        itemLabel="esta loja do descritivo"
      />

      <ConfirmDeleteDialog
        open={bulkDeleteConfirmOpen}
        onClose={() => setBulkDeleteConfirmOpen(false)}
        onConfirmed={confirmBulkDelete}
        itemLabel={`estas ${selectedLinhaIds.size} loja(s) do descritivo`}
      />

      <ConfirmDeleteDialog
        open={deleteDescritivoTarget}
        onClose={() => setDeleteDescritivoTarget(false)}
        onConfirmed={handleDeleteDescritivo}
        itemLabel="este descritivo (todas as lojas dele)"
      />
    </Layout>
  );
}
