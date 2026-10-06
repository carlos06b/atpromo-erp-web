package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.Beneficiario;
import com.atpromo.systematpromo.model.CategoriaLancamento;
import com.atpromo.systematpromo.model.CentroCusto;
import com.atpromo.systematpromo.model.Client;
import com.atpromo.systematpromo.model.ContaBancaria;
import com.atpromo.systematpromo.model.FinancePromoter;
import com.atpromo.systematpromo.model.FixedExpenseHistory;
import com.atpromo.systematpromo.model.Invoice;
import com.atpromo.systematpromo.model.LancamentoExtrato;
import com.atpromo.systematpromo.model.Promoter;
import com.atpromo.systematpromo.model.VariableExpense;
import com.atpromo.systematpromo.repository.BeneficiarioRepository;
import com.atpromo.systematpromo.repository.CategoriaLancamentoRepository;
import com.atpromo.systematpromo.repository.CentroCustoRepository;
import com.atpromo.systematpromo.repository.ClientRepository;
import com.atpromo.systematpromo.repository.ContaBancariaRepository;
import com.atpromo.systematpromo.repository.FinancePromoterRepository;
import com.atpromo.systematpromo.repository.FixedExpenseHistoryRepository;
import com.atpromo.systematpromo.repository.InvoiceRepository;
import com.atpromo.systematpromo.repository.LancamentoExtratoRepository;
import com.atpromo.systematpromo.repository.PromoterRepository;
import com.atpromo.systematpromo.repository.VariableExpenseRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lancamentos-extrato")
public class LancamentoExtratoController {

    private final LancamentoExtratoRepository lancamentoExtratoRepository;
    private final ContaBancariaRepository contaBancariaRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final CategoriaLancamentoRepository categoriaLancamentoRepository;
    private final CentroCustoRepository centroCustoRepository;
    private final FinancePromoterRepository financePromoterRepository;
    private final PromoterRepository promoterRepository;
    private final FixedExpenseHistoryRepository fixedExpenseHistoryRepository;
    private final VariableExpenseRepository variableExpenseRepository;
    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final AccessControl accessControl;

    public LancamentoExtratoController(LancamentoExtratoRepository lancamentoExtratoRepository,
                                        ContaBancariaRepository contaBancariaRepository,
                                        BeneficiarioRepository beneficiarioRepository,
                                        CategoriaLancamentoRepository categoriaLancamentoRepository,
                                        CentroCustoRepository centroCustoRepository,
                                        FinancePromoterRepository financePromoterRepository,
                                        PromoterRepository promoterRepository,
                                        FixedExpenseHistoryRepository fixedExpenseHistoryRepository,
                                        VariableExpenseRepository variableExpenseRepository,
                                        InvoiceRepository invoiceRepository,
                                        ClientRepository clientRepository,
                                        AccessControl accessControl) {
        this.lancamentoExtratoRepository = lancamentoExtratoRepository;
        this.contaBancariaRepository = contaBancariaRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.categoriaLancamentoRepository = categoriaLancamentoRepository;
        this.centroCustoRepository = centroCustoRepository;
        this.financePromoterRepository = financePromoterRepository;
        this.promoterRepository = promoterRepository;
        this.fixedExpenseHistoryRepository = fixedExpenseHistoryRepository;
        this.variableExpenseRepository = variableExpenseRepository;
        this.invoiceRepository = invoiceRepository;
        this.clientRepository = clientRepository;
        this.accessControl = accessControl;
    }

    public record LancamentoView(
            Integer id,
            Integer contaBancariaId,
            LocalDate data,
            String tipo,
            BigDecimal valor,
            String descricao,
            String origemTipo,
            Integer origemId,
            Integer beneficiarioId,
            String beneficiarioNome,
            Integer categoriaId,
            String categoriaNome,
            Integer centroCustoId,
            String centroCustoNome,
            boolean editavel,
            boolean excluivel,
            String contaRelacionadaNome,
            BigDecimal saldoAcumulado
    ) {}

    public record CreateLancamentoBody(
            Integer contaBancariaId,
            LocalDate data,
            String tipo,
            BigDecimal valor,
            String descricao,
            Integer beneficiarioId,
            Integer categoriaId,
            Integer centroCustoId
    ) {}

    public record TransferenciaBody(Integer contaOrigemId, Integer contaDestinoId, LocalDate data, BigDecimal valor, String descricao) {}

    public record PixPendenteView(
            Integer financePromoterId,
            Integer promoterId,
            String promoterNome,
            String tipo,
            BigDecimal valor,
            LocalDate data,
            String descricao
    ) {}

    public record LancarPixBody(Integer contaBancariaId, Integer categoriaId, Integer centroCustoId) {}

    // Corpo reaproveitado pelos 3 lançamentos automáticos abaixo (despesa fixa,
    // despesa variável e faturamento): todos pedem conta bancária + categoria +
    // centro de custo, e o beneficiário é opcional (quando não informado, o
    // faturamento tenta resolver sozinho a partir do cliente; despesas ficam
    // sem beneficiário se nada for escolhido).
    public record LancarDespesaBody(Integer contaBancariaId, Integer beneficiarioId, Integer categoriaId, Integer centroCustoId) {}

    public record DespesaFixaPendenteView(
            Integer fixedExpenseHistoryId,
            String nome,
            BigDecimal valor,
            LocalDate data,
            String descricao
    ) {}

    public record DespesaVariavelPendenteView(
            Integer variableExpenseId,
            String nome,
            BigDecimal valor,
            LocalDate data,
            String descricao,
            Integer parcela,
            Integer totalParcelas
    ) {}

    public record FaturamentoPendenteView(
            Integer invoiceId,
            Integer clientId,
            String clienteNome,
            BigDecimal valor,
            LocalDate data,
            String descricao
    ) {}

    @GetMapping
    public ResponseEntity<?> listByConta(@RequestParam Integer contaBancariaId, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }

        ContaBancaria conta = contaBancariaRepository.findById(contaBancariaId).orElse(null);
        if (conta == null) {
            return ResponseEntity.notFound().build();
        }

        List<LancamentoExtrato> lancamentos = lancamentoExtratoRepository.findByContaBancariaId(contaBancariaId);
        lancamentos.sort(Comparator.comparing(LancamentoExtrato::getData).thenComparing(LancamentoExtrato::getId));

        BigDecimal saldo = conta.getSaldoInicial() != null ? conta.getSaldoInicial() : BigDecimal.ZERO;
        List<LancamentoView> views = new ArrayList<>();
        for (LancamentoExtrato l : lancamentos) {
            saldo = "ENTRADA".equalsIgnoreCase(l.getTipo()) ? saldo.add(l.getValor()) : saldo.subtract(l.getValor());
            views.add(mapOne(l, saldo));
        }

        Collections.reverse(views);
        return ResponseEntity.ok(views);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateLancamentoBody body, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }

        ResponseEntity<?> validation = validateLancamentoBody(body);
        if (validation != null) {
            return validation;
        }

        LancamentoExtrato lancamento = new LancamentoExtrato();
        lancamento.setContaBancariaId(body.contaBancariaId());
        lancamento.setData(body.data());
        lancamento.setTipo(body.tipo().trim().toUpperCase());
        lancamento.setValor(body.valor());
        lancamento.setDescricao(blankToNull(body.descricao()));
        lancamento.setBeneficiarioId(body.beneficiarioId());
        lancamento.setCategoriaId(body.categoriaId());
        lancamento.setCentroCustoId(body.centroCustoId());
        lancamento.setOrigemTipo("MANUAL");
        lancamento.setOrigemId(null);
        lancamento.setCreatedAt(LocalDateTime.now());

        LancamentoExtrato saved = lancamentoExtratoRepository.save(lancamento);
        return ResponseEntity.status(201).body(mapOne(saved, null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody CreateLancamentoBody body, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }

        LancamentoExtrato lancamento = lancamentoExtratoRepository.findById(id).orElse(null);
        if (lancamento == null) {
            return ResponseEntity.notFound().build();
        }

        // Lançamentos de origem automática (Pix, despesa fixa/variável, faturamento)
        // também podem ser editados aqui — ex: corrigir categoria/centro de custo/
        // beneficiário depois de lançado. Só a Transferência fica de fora, porque
        // ela é um par (saída numa conta + entrada noutra) e editar só um lado
        // deixaria as duas pontas dessincronizadas.
        if ("TRANSFERENCIA".equalsIgnoreCase(lancamento.getOrigemTipo())) {
            return badRequest("Esse lançamento faz parte de uma transferência entre contas e não pode ser editado por aqui.");
        }

        ResponseEntity<?> validation = validateLancamentoBody(body);
        if (validation != null) {
            return validation;
        }

        lancamento.setContaBancariaId(body.contaBancariaId());
        lancamento.setData(body.data());
        lancamento.setTipo(body.tipo().trim().toUpperCase());
        lancamento.setValor(body.valor());
        lancamento.setDescricao(blankToNull(body.descricao()));
        lancamento.setBeneficiarioId(body.beneficiarioId());
        lancamento.setCategoriaId(body.categoriaId());
        lancamento.setCentroCustoId(body.centroCustoId());

        LancamentoExtrato saved = lancamentoExtratoRepository.save(lancamento);
        return ResponseEntity.ok(mapOne(saved, null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }

        LancamentoExtrato lancamento = lancamentoExtratoRepository.findById(id).orElse(null);
        if (lancamento == null) {
            return ResponseEntity.notFound().build();
        }

        String origemTipo = lancamento.getOrigemTipo();
        if (!"MANUAL".equalsIgnoreCase(origemTipo) && !"TRANSFERENCIA".equalsIgnoreCase(origemTipo)) {
            return badRequest("Esse lançamento foi gerado automaticamente e só pode ser desfeito na origem.");
        }

        if ("TRANSFERENCIA".equalsIgnoreCase(origemTipo) && lancamento.getOrigemId() != null) {
            lancamentoExtratoRepository.findById(lancamento.getOrigemId())
                    .filter(par -> "TRANSFERENCIA".equalsIgnoreCase(par.getOrigemTipo()))
                    .ifPresent(par -> lancamentoExtratoRepository.deleteById(par.getId()));
        }

        lancamentoExtratoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transferencia")
    public ResponseEntity<?> transferir(@RequestBody TransferenciaBody body, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }

        if (body.contaOrigemId() == null || body.contaDestinoId() == null) {
            return badRequest("Selecione a conta de origem e a conta de destino.");
        }

        if (body.contaOrigemId().equals(body.contaDestinoId())) {
            return badRequest("A conta de origem e a conta de destino não podem ser a mesma.");
        }

        ContaBancaria contaOrigem = contaBancariaRepository.findById(body.contaOrigemId()).orElse(null);
        ContaBancaria contaDestino = contaBancariaRepository.findById(body.contaDestinoId()).orElse(null);

        if (contaOrigem == null || contaDestino == null) {
            return ResponseEntity.notFound().build();
        }

        if (!contaOrigem.isAtiva() || !contaDestino.isAtiva()) {
            return badRequest("Só é possível transferir entre contas ativas.");
        }

        if (body.data() == null) {
            return badRequest("Informe a data da transferência.");
        }

        if (body.valor() == null || body.valor().compareTo(BigDecimal.ZERO) <= 0) {
            return badRequest("Informe um valor válido para a transferência.");
        }

        String descricao = blankToNull(body.descricao());
        LocalDateTime now = LocalDateTime.now();

        LancamentoExtrato saida = new LancamentoExtrato();
        saida.setContaBancariaId(contaOrigem.getId());
        saida.setData(body.data());
        saida.setTipo("SAIDA");
        saida.setValor(body.valor());
        saida.setDescricao(descricao);
        saida.setOrigemTipo("TRANSFERENCIA");
        saida.setCreatedAt(now);
        saida = lancamentoExtratoRepository.save(saida);

        LancamentoExtrato entrada = new LancamentoExtrato();
        entrada.setContaBancariaId(contaDestino.getId());
        entrada.setData(body.data());
        entrada.setTipo("ENTRADA");
        entrada.setValor(body.valor());
        entrada.setDescricao(descricao);
        entrada.setOrigemTipo("TRANSFERENCIA");
        entrada.setOrigemId(saida.getId());
        entrada.setCreatedAt(now);
        entrada = lancamentoExtratoRepository.save(entrada);

        saida.setOrigemId(entrada.getId());
        saida = lancamentoExtratoRepository.save(saida);

        return ResponseEntity.status(201).body(Map.of(
                "saida", mapOne(saida, null),
                "entrada", mapOne(entrada, null)
        ));
    }

    @GetMapping("/pix-pendentes")
    public ResponseEntity<?> listPixPendentes(Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }

        List<PixPendenteView> pendentes = financePromoterRepository.findAll().stream()
                .filter(f -> !lancamentoExtratoRepository.existsByOrigemTipoAndOrigemId("SOLICITACAO_PIX", f.getId()))
                .sorted(Comparator.comparing(FinancePromoter::getDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(f -> new PixPendenteView(
                        f.getId(),
                        f.getIdPromoter(),
                        promoterRepository.findById(f.getIdPromoter()).map(Promoter::getName).orElse("Promotor não encontrado"),
                        f.getType(),
                        f.getAmount(),
                        f.getDate(),
                        f.getDescription()
                ))
                .toList();

        return ResponseEntity.ok(pendentes);
    }

    @PostMapping("/pix/{financePromoterId}")
    public ResponseEntity<?> lancarPix(@PathVariable int financePromoterId, @RequestBody LancarPixBody body, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }

        FinancePromoter financePromoter = financePromoterRepository.findById(financePromoterId).orElse(null);
        if (financePromoter == null) {
            return ResponseEntity.notFound().build();
        }

        if (lancamentoExtratoRepository.existsByOrigemTipoAndOrigemId("SOLICITACAO_PIX", financePromoterId)) {
            return badRequest("Esse Pix já foi lançado no extrato.");
        }

        if (body.contaBancariaId() == null) {
            return badRequest("Selecione a conta bancária.");
        }
        if (!contaBancariaRepository.existsById(body.contaBancariaId())) {
            return ResponseEntity.notFound().build();
        }
        if (body.categoriaId() == null) {
            return badRequest("Selecione a categoria do pagamento.");
        }
        if (!categoriaLancamentoRepository.existsById(body.categoriaId())) {
            return badRequest("Categoria inválida.");
        }
        if (body.centroCustoId() == null) {
            return badRequest("Selecione o centro de custo.");
        }
        if (!centroCustoRepository.existsById(body.centroCustoId())) {
            return badRequest("Centro de custo inválido.");
        }

        Promoter promoter = promoterRepository.findById(financePromoter.getIdPromoter()).orElse(null);
        Integer beneficiarioId = resolveBeneficiarioParaPromoter(
                financePromoter.getIdPromoter(),
                promoter != null ? promoter.getName() : null
        );

        LancamentoExtrato lancamento = new LancamentoExtrato();
        lancamento.setContaBancariaId(body.contaBancariaId());
        lancamento.setData(financePromoter.getDate());
        lancamento.setTipo("SAIDA");
        lancamento.setValor(financePromoter.getAmount());
        lancamento.setDescricao(financePromoter.getDescription());
        lancamento.setBeneficiarioId(beneficiarioId);
        lancamento.setCategoriaId(body.categoriaId());
        lancamento.setCentroCustoId(body.centroCustoId());
        lancamento.setOrigemTipo("SOLICITACAO_PIX");
        lancamento.setOrigemId(financePromoter.getId());
        lancamento.setCreatedAt(LocalDateTime.now());

        LancamentoExtrato saved = lancamentoExtratoRepository.save(lancamento);
        return ResponseEntity.status(201).body(mapOne(saved, null));
    }

    // ---- Despesas fixas: a "ocorrência" paga de verdade é um FixedExpenseHistory
    // (o template FixedExpense é só a recorrência). Igual ao Pix: nada muda na tela
    // de Despesas, isso aqui só lê o que já está marcado "PAGO" e ainda não tem
    // lançamento correspondente no extrato. ----

    @GetMapping("/despesas-fixas-pendentes")
    public ResponseEntity<?> listDespesasFixasPendentes(Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }

        List<DespesaFixaPendenteView> pendentes = fixedExpenseHistoryRepository.findAll().stream()
                .filter(h -> "PAGO".equalsIgnoreCase(h.getStatus()))
                .filter(h -> !lancamentoExtratoRepository.existsByOrigemTipoAndOrigemId("DESPESA_FIXA", h.getId()))
                .sorted(Comparator.comparing(
                        (FixedExpenseHistory h) -> h.getPaymentDate() != null ? h.getPaymentDate() : h.getDueDate(),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(h -> new DespesaFixaPendenteView(
                        h.getId(),
                        h.getName(),
                        h.getAmount(),
                        h.getPaymentDate() != null ? h.getPaymentDate() : h.getDueDate(),
                        h.getDescription()
                ))
                .toList();

        return ResponseEntity.ok(pendentes);
    }

    @PostMapping("/despesa-fixa/{fixedExpenseHistoryId}")
    public ResponseEntity<?> lancarDespesaFixa(@PathVariable int fixedExpenseHistoryId, @RequestBody LancarDespesaBody body, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }

        FixedExpenseHistory historico = fixedExpenseHistoryRepository.findById(fixedExpenseHistoryId).orElse(null);
        if (historico == null) {
            return ResponseEntity.notFound().build();
        }

        if (!"PAGO".equalsIgnoreCase(historico.getStatus())) {
            return badRequest("Essa despesa ainda não foi marcada como paga.");
        }

        if (lancamentoExtratoRepository.existsByOrigemTipoAndOrigemId("DESPESA_FIXA", fixedExpenseHistoryId)) {
            return badRequest("Essa despesa já foi lançada no extrato.");
        }

        ResponseEntity<?> validation = validateLancarDespesaBody(body);
        if (validation != null) {
            return validation;
        }

        LancamentoExtrato lancamento = new LancamentoExtrato();
        lancamento.setContaBancariaId(body.contaBancariaId());
        lancamento.setData(historico.getPaymentDate() != null ? historico.getPaymentDate() : historico.getDueDate());
        lancamento.setTipo("SAIDA");
        lancamento.setValor(historico.getAmount());
        lancamento.setDescricao(historico.getDescription() != null ? historico.getDescription() : historico.getName());
        lancamento.setBeneficiarioId(body.beneficiarioId());
        lancamento.setCategoriaId(body.categoriaId());
        lancamento.setCentroCustoId(body.centroCustoId());
        lancamento.setOrigemTipo("DESPESA_FIXA");
        lancamento.setOrigemId(historico.getId());
        lancamento.setCreatedAt(LocalDateTime.now());

        LancamentoExtrato saved = lancamentoExtratoRepository.save(lancamento);
        return ResponseEntity.status(201).body(mapOne(saved, null));
    }

    // ---- Despesas variáveis: cada linha (inclusive cada parcela) já é a própria
    // unidade paga, sem histórico separado. ----

    @GetMapping("/despesas-variaveis-pendentes")
    public ResponseEntity<?> listDespesasVariaveisPendentes(Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }

        List<DespesaVariavelPendenteView> pendentes = variableExpenseRepository.findAll().stream()
                .filter(VariableExpense::isStatus)
                .filter(v -> !lancamentoExtratoRepository.existsByOrigemTipoAndOrigemId("DESPESA_VARIAVEL", v.getId()))
                .sorted(Comparator.comparing(
                        (VariableExpense v) -> v.getPaymentDate() != null ? v.getPaymentDate() : v.getDate(),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(v -> new DespesaVariavelPendenteView(
                        v.getId(),
                        v.getName(),
                        v.getAmount(),
                        v.getPaymentDate() != null ? v.getPaymentDate() : v.getDate(),
                        v.getDescription(),
                        v.getInstallmentNumber(),
                        v.getTotalInstallments()
                ))
                .toList();

        return ResponseEntity.ok(pendentes);
    }

    @PostMapping("/despesa-variavel/{variableExpenseId}")
    public ResponseEntity<?> lancarDespesaVariavel(@PathVariable int variableExpenseId, @RequestBody LancarDespesaBody body, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }

        VariableExpense despesa = variableExpenseRepository.findById(variableExpenseId).orElse(null);
        if (despesa == null) {
            return ResponseEntity.notFound().build();
        }

        if (!despesa.isStatus()) {
            return badRequest("Essa despesa ainda não foi marcada como paga.");
        }

        if (lancamentoExtratoRepository.existsByOrigemTipoAndOrigemId("DESPESA_VARIAVEL", variableExpenseId)) {
            return badRequest("Essa despesa já foi lançada no extrato.");
        }

        ResponseEntity<?> validation = validateLancarDespesaBody(body);
        if (validation != null) {
            return validation;
        }

        LancamentoExtrato lancamento = new LancamentoExtrato();
        lancamento.setContaBancariaId(body.contaBancariaId());
        lancamento.setData(despesa.getPaymentDate() != null ? despesa.getPaymentDate() : despesa.getDate());
        lancamento.setTipo("SAIDA");
        lancamento.setValor(despesa.getAmount());
        lancamento.setDescricao(despesa.getDescription() != null ? despesa.getDescription() : despesa.getName());
        lancamento.setBeneficiarioId(body.beneficiarioId());
        lancamento.setCategoriaId(body.categoriaId());
        lancamento.setCentroCustoId(body.centroCustoId());
        lancamento.setOrigemTipo("DESPESA_VARIAVEL");
        lancamento.setOrigemId(despesa.getId());
        lancamento.setCreatedAt(LocalDateTime.now());

        LancamentoExtrato saved = lancamentoExtratoRepository.save(lancamento);
        return ResponseEntity.status(201).body(mapOne(saved, null));
    }

    // ---- Faturamento: a única diferença real é que é dinheiro ENTRANDO, e o
    // beneficiário (aqui, na prática, "quem pagou") pode ser resolvido sozinho a
    // partir do cliente da fatura, igual ao Pix resolve a partir do promotor. ----

    @GetMapping("/faturamento-pendentes")
    public ResponseEntity<?> listFaturamentoPendentes(Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }

        List<FaturamentoPendenteView> pendentes = invoiceRepository.findAll().stream()
                .filter(i -> "PAGO".equalsIgnoreCase(i.getStatus()))
                .filter(i -> !lancamentoExtratoRepository.existsByOrigemTipoAndOrigemId("FATURAMENTO", i.getId()))
                .sorted(Comparator.comparing(
                        (Invoice i) -> i.getPaymentDate() != null ? i.getPaymentDate() : i.getIssueDate(),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(i -> new FaturamentoPendenteView(
                        i.getId(),
                        i.getClientId(),
                        clientRepository.findById(i.getClientId()).map(Client::getName).orElse("Cliente não encontrado"),
                        i.getReceivedAmount() != null ? i.getReceivedAmount() : i.getAmount(),
                        i.getPaymentDate() != null ? i.getPaymentDate() : i.getIssueDate(),
                        i.getDescription()
                ))
                .toList();

        return ResponseEntity.ok(pendentes);
    }

    @PostMapping("/faturamento/{invoiceId}")
    public ResponseEntity<?> lancarFaturamento(@PathVariable int invoiceId, @RequestBody LancarDespesaBody body, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }

        Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
        if (invoice == null) {
            return ResponseEntity.notFound().build();
        }

        if (!"PAGO".equalsIgnoreCase(invoice.getStatus())) {
            return badRequest("Esse faturamento ainda não foi marcado como pago.");
        }

        if (lancamentoExtratoRepository.existsByOrigemTipoAndOrigemId("FATURAMENTO", invoiceId)) {
            return badRequest("Esse faturamento já foi lançado no extrato.");
        }

        ResponseEntity<?> validation = validateLancarDespesaBody(body);
        if (validation != null) {
            return validation;
        }

        Integer beneficiarioId = body.beneficiarioId();
        if (beneficiarioId == null) {
            Client client = clientRepository.findById(invoice.getClientId()).orElse(null);
            beneficiarioId = resolveBeneficiarioParaCliente(invoice.getClientId(), client != null ? client.getName() : null);
        }

        LancamentoExtrato lancamento = new LancamentoExtrato();
        lancamento.setContaBancariaId(body.contaBancariaId());
        lancamento.setData(invoice.getPaymentDate() != null ? invoice.getPaymentDate() : invoice.getIssueDate());
        lancamento.setTipo("ENTRADA");
        lancamento.setValor(invoice.getReceivedAmount() != null ? invoice.getReceivedAmount() : invoice.getAmount());
        lancamento.setDescricao(invoice.getDescription());
        lancamento.setBeneficiarioId(beneficiarioId);
        lancamento.setCategoriaId(body.categoriaId());
        lancamento.setCentroCustoId(body.centroCustoId());
        lancamento.setOrigemTipo("FATURAMENTO");
        lancamento.setOrigemId(invoice.getId());
        lancamento.setCreatedAt(LocalDateTime.now());

        LancamentoExtrato saved = lancamentoExtratoRepository.save(lancamento);
        return ResponseEntity.status(201).body(mapOne(saved, null));
    }

    // Validação comum aos 3 lançamentos automáticos acima: conta/categoria/centro
    // de custo são obrigatórios, beneficiário é opcional (mas, se vier, precisa existir).
    private ResponseEntity<?> validateLancarDespesaBody(LancarDespesaBody body) {
        if (body.contaBancariaId() == null) {
            return badRequest("Selecione a conta bancária.");
        }
        if (!contaBancariaRepository.existsById(body.contaBancariaId())) {
            return ResponseEntity.notFound().build();
        }
        if (body.categoriaId() == null) {
            return badRequest("Selecione a categoria do pagamento.");
        }
        if (!categoriaLancamentoRepository.existsById(body.categoriaId())) {
            return badRequest("Categoria inválida.");
        }
        if (body.centroCustoId() == null) {
            return badRequest("Selecione o centro de custo.");
        }
        if (!centroCustoRepository.existsById(body.centroCustoId())) {
            return badRequest("Centro de custo inválido.");
        }
        if (body.beneficiarioId() != null && !beneficiarioRepository.existsById(body.beneficiarioId())) {
            return badRequest("Beneficiário inválido.");
        }
        return null;
    }

    // Reaproveita o beneficiário já vinculado a esse promotor, ou cria um novo
    // na hora (usando o nome do promotor) — assim o financeiro não precisa
    // cadastrar o mesmo promotor de novo a cada Pix lançado.
    private Integer resolveBeneficiarioParaPromoter(Integer promoterId, String nomePromoter) {
        if (promoterId == null) {
            return null;
        }

        return beneficiarioRepository.findByPromoterId(promoterId)
                .map(Beneficiario::getId)
                .orElseGet(() -> {
                    Beneficiario novo = new Beneficiario();
                    novo.setNome(nomePromoter != null ? nomePromoter : ("Promotor #" + promoterId));
                    novo.setPromoterId(promoterId);
                    novo.setAtivo(true);
                    return beneficiarioRepository.save(novo).getId();
                });
    }

    // Mesma ideia, para o faturamento: reaproveita o beneficiário já vinculado
    // a esse cliente, ou cria um novo (usando o nome do cliente).
    private Integer resolveBeneficiarioParaCliente(Integer clientId, String nomeCliente) {
        if (clientId == null) {
            return null;
        }

        return beneficiarioRepository.findByClientId(clientId)
                .map(Beneficiario::getId)
                .orElseGet(() -> {
                    Beneficiario novo = new Beneficiario();
                    novo.setNome(nomeCliente != null ? nomeCliente : ("Cliente #" + clientId));
                    novo.setClientId(clientId);
                    novo.setAtivo(true);
                    return beneficiarioRepository.save(novo).getId();
                });
    }

    private ResponseEntity<?> validateLancamentoBody(CreateLancamentoBody body) {
        if (body.contaBancariaId() == null) {
            return badRequest("Selecione a conta bancária.");
        }

        if (!contaBancariaRepository.existsById(body.contaBancariaId())) {
            return ResponseEntity.notFound().build();
        }

        if (body.data() == null) {
            return badRequest("Informe a data do lançamento.");
        }

        if (body.tipo() == null || (!"ENTRADA".equalsIgnoreCase(body.tipo()) && !"SAIDA".equalsIgnoreCase(body.tipo()))) {
            return badRequest("Tipo de lançamento inválido.");
        }

        if (body.valor() == null || body.valor().compareTo(BigDecimal.ZERO) <= 0) {
            return badRequest("Informe um valor válido.");
        }

        if (body.categoriaId() == null) {
            return badRequest("Selecione a categoria do pagamento.");
        }
        if (!categoriaLancamentoRepository.existsById(body.categoriaId())) {
            return badRequest("Categoria inválida.");
        }

        if (body.centroCustoId() == null) {
            return badRequest("Selecione o centro de custo.");
        }
        if (!centroCustoRepository.existsById(body.centroCustoId())) {
            return badRequest("Centro de custo inválido.");
        }

        if (body.beneficiarioId() != null && !beneficiarioRepository.existsById(body.beneficiarioId())) {
            return badRequest("Beneficiário inválido.");
        }

        return null;
    }

    private LancamentoView mapOne(LancamentoExtrato l, BigDecimal saldoAcumulado) {
        boolean editavel = isEditavel(l);
        boolean excluivel = isExcluivel(l);
        String contaRelacionadaNome = contaRelacionadaNome(l);

        String beneficiarioNome = l.getBeneficiarioId() == null
                ? null
                : beneficiarioRepository.findById(l.getBeneficiarioId()).map(Beneficiario::getNome).orElse(null);
        String categoriaNome = l.getCategoriaId() == null
                ? null
                : categoriaLancamentoRepository.findById(l.getCategoriaId()).map(CategoriaLancamento::getNome).orElse(null);
        String centroCustoNome = l.getCentroCustoId() == null
                ? null
                : centroCustoRepository.findById(l.getCentroCustoId()).map(CentroCusto::getNome).orElse(null);

        return new LancamentoView(
                l.getId(),
                l.getContaBancariaId(),
                l.getData(),
                l.getTipo(),
                l.getValor(),
                l.getDescricao(),
                l.getOrigemTipo(),
                l.getOrigemId(),
                l.getBeneficiarioId(),
                beneficiarioNome,
                l.getCategoriaId(),
                categoriaNome,
                l.getCentroCustoId(),
                centroCustoNome,
                editavel,
                excluivel,
                contaRelacionadaNome,
                saldoAcumulado
        );
    }

    private boolean isEditavel(LancamentoExtrato l) {
        return !"TRANSFERENCIA".equalsIgnoreCase(l.getOrigemTipo());
    }

    private boolean isExcluivel(LancamentoExtrato l) {
        String origemTipo = l.getOrigemTipo();
        return "MANUAL".equalsIgnoreCase(origemTipo) || "TRANSFERENCIA".equalsIgnoreCase(origemTipo);
    }

    private String contaRelacionadaNome(LancamentoExtrato l) {
        if (!"TRANSFERENCIA".equalsIgnoreCase(l.getOrigemTipo()) || l.getOrigemId() == null) {
            return null;
        }

        return lancamentoExtratoRepository.findById(l.getOrigemId())
                .map(LancamentoExtrato::getContaBancariaId)
                .flatMap(contaBancariaRepository::findById)
                .map(ContaBancaria::getApelido)
                .orElse(null);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para acessar o extrato."));
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
