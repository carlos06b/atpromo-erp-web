package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.Client;
import com.atpromo.systematpromo.model.Descritivo;
import com.atpromo.systematpromo.model.DescritivoLinha;
import com.atpromo.systematpromo.model.Loja;
import com.atpromo.systematpromo.repository.ClientRepository;
import com.atpromo.systematpromo.repository.DescritivoLinhaRepository;
import com.atpromo.systematpromo.repository.DescritivoRepository;
import com.atpromo.systematpromo.repository.LojaRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/descritivos")
public class DescritivoController {

    private static final List<String> REDE_PRIORIDADE = List.of(
            "ASSAI", "ATACADAO", "NOVO ATACAREJO", "MIX MATEUS"
    );

    private final DescritivoRepository descritivoRepository;
    private final DescritivoLinhaRepository linhaRepository;
    private final ClientRepository clientRepository;
    private final LojaRepository lojaRepository;
    private final AccessControl accessControl;

    public DescritivoController(DescritivoRepository descritivoRepository, DescritivoLinhaRepository linhaRepository,
                                ClientRepository clientRepository, LojaRepository lojaRepository, AccessControl accessControl) {
        this.descritivoRepository = descritivoRepository;
        this.linhaRepository = linhaRepository;
        this.clientRepository = clientRepository;
        this.lojaRepository = lojaRepository;
        this.accessControl = accessControl;
    }

    public record LinhaRequest(
            Integer lojaId,
            List<Integer> diasSemana,
            BigDecimal horasPorAtendimento,
            BigDecimal valorHora,
            LocalDate dataInicio,
            LocalDate dataFim,
            BigDecimal valorTotalManual
    ) {}

    public record DescritivoRequest(Integer clienteId, Integer mes, Integer ano, String rotulo) {}

    public record LinhaView(
            Integer id, Integer numero, Integer lojaId, String lojaNome, String lojaUf, String lojaCnpj,
            List<Integer> diasSemana, String quantidadeAtendimentoLabel,
            BigDecimal horasPorAtendimento, BigDecimal valorHora,
            LocalDate dataInicio, LocalDate dataFim,
            LocalDate dataInicioExibicao, LocalDate dataFimExibicao, boolean encerrandoNesteMes,
            BigDecimal valorTotal, boolean valorTotalManual
    ) {}

    public record DescritivoView(
            Integer id, Integer clienteId, String clienteNomeFantasia, String clienteRazaoSocial, String clienteCnpj,
            Integer mes, Integer ano, String rotulo, String rotuloExibicao, List<LinhaView> linhas, BigDecimal valorTotalGeral
    ) {}

    public record DescritivoResumo(
            Integer id, Integer clienteId, String clienteNomeFantasia, Integer mes, Integer ano,
            String rotulo, String rotuloExibicao, BigDecimal valorTotalGeral
    ) {}

    @GetMapping
    public ResponseEntity<?> listAll(@RequestParam(required = false) Integer clienteId, Authentication authentication) {
        if (!allowed(authentication)) return forbidden();

        List<Descritivo> descritivos = clienteId != null
                ? descritivoRepository.findByClienteId(clienteId)
                : descritivoRepository.findAll();

        Map<Integer, Client> clientesById = clientRepository.findAll().stream()
                .collect(Collectors.toMap(Client::getId, c -> c));

        Map<String, List<Descritivo>> grupos = descritivos.stream()
                .collect(Collectors.groupingBy(d -> d.getClienteId() + "|" + d.getMes() + "|" + d.getAno()));

        List<DescritivoResumo> resumos = descritivos.stream()
                .sorted(Comparator.comparing(Descritivo::getAno).thenComparing(Descritivo::getMes).thenComparing(Descritivo::getId))
                .map(d -> {
                    Client cliente = clientesById.get(d.getClienteId());
                    BigDecimal total = linhaRepository.findByDescritivoId(d.getId()).stream()
                            .map(DescritivoLinha::getValorTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    List<Descritivo> irmaos = grupos.get(d.getClienteId() + "|" + d.getMes() + "|" + d.getAno());
                    return new DescritivoResumo(
                            d.getId(), d.getClienteId(), cliente != null ? cliente.getName() : null,
                            d.getMes(), d.getAno(), d.getRotulo(), rotuloExibicao(d, irmaos), total
                    );
                })
                .toList();

        return ResponseEntity.ok(resumos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable int id, Authentication authentication) {
        if (!allowed(authentication)) return forbidden();
        return descritivoRepository.findById(id)
                .<ResponseEntity<?>>map(d -> ResponseEntity.ok(toView(d)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody DescritivoRequest request, Authentication authentication) {
        if (!allowed(authentication)) return forbidden();

        if (request.clienteId() == null || !clientRepository.existsById(request.clienteId())) {
            return badRequest("Cliente inválido.");
        }
        if (request.mes() == null || request.mes() < 1 || request.mes() > 12) {
            return badRequest("Mês inválido.");
        }
        if (request.ano() == null || request.ano() < 2000) {
            return badRequest("Ano inválido.");
        }

        String rotulo = normalizarRotulo(request.rotulo());

        List<Descritivo> existentes = descritivoRepository.findByClienteIdAndMesAndAno(request.clienteId(), request.mes(), request.ano());
        if (rotulo != null) {
            boolean duplicado = existentes.stream()
                    .anyMatch(d -> rotulo.equalsIgnoreCase(normalizarRotulo(d.getRotulo())));
            if (duplicado) {
                return badRequest("Já existe um descritivo desse cliente nesse mês com esse mesmo rótulo.");
            }
        }

        Descritivo descritivo = new Descritivo();
        descritivo.setClienteId(request.clienteId());
        descritivo.setMes(request.mes());
        descritivo.setAno(request.ano());
        descritivo.setRotulo(rotulo);
        Descritivo saved = descritivoRepository.save(descritivo);

        return ResponseEntity.ok(toView(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id, Authentication authentication) {
        if (!allowed(authentication)) return forbidden();
        if (!descritivoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        linhaRepository.findByDescritivoId(id).forEach(linha -> linhaRepository.deleteById(linha.getId()));
        descritivoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/linhas")
    public ResponseEntity<?> addLinha(@PathVariable int id, @RequestBody LinhaRequest request, Authentication authentication) {
        if (!allowed(authentication)) return forbidden();

        Optional<Descritivo> descritivoOpt = descritivoRepository.findById(id);
        if (descritivoOpt.isEmpty()) return ResponseEntity.notFound().build();
        Descritivo descritivo = descritivoOpt.get();

        String validationError = validateLinha(request);
        if (validationError != null) return badRequest(validationError);

        DescritivoLinha linha = new DescritivoLinha();
        linha.setDescritivoId(id);
        applyRequest(linha, request);

        int proximaOrdem = linhaRepository.findByDescritivoId(id).stream()
                .mapToInt(l -> l.getOrdem() != null ? l.getOrdem() : 0)
                .max().orElse(0) + 1;
        linha.setOrdem(proximaOrdem);

        calcularValorTotal(linha, descritivo, request.valorTotalManual());
        linhaRepository.save(linha);

        return ResponseEntity.ok(toView(descritivoRepository.findById(id).orElseThrow()));
    }

    @PutMapping("/{id}/linhas/{linhaId}")
    public ResponseEntity<?> updateLinha(@PathVariable int id, @PathVariable int linhaId, @RequestBody LinhaRequest request, Authentication authentication) {
        if (!allowed(authentication)) return forbidden();

        Optional<Descritivo> descritivoOpt = descritivoRepository.findById(id);
        if (descritivoOpt.isEmpty()) return ResponseEntity.notFound().build();
        Descritivo descritivo = descritivoOpt.get();

        Optional<DescritivoLinha> linhaOpt = linhaRepository.findById(linhaId);
        if (linhaOpt.isEmpty() || !linhaOpt.get().getDescritivoId().equals(id)) {
            return ResponseEntity.notFound().build();
        }

        String validationError = validateLinha(request);
        if (validationError != null) return badRequest(validationError);

        DescritivoLinha linha = linhaOpt.get();
        applyRequest(linha, request);
        calcularValorTotal(linha, descritivo, request.valorTotalManual());
        linhaRepository.save(linha);

        return ResponseEntity.ok(toView(descritivoRepository.findById(id).orElseThrow()));
    }

    @DeleteMapping("/{id}/linhas/{linhaId}")
    public ResponseEntity<?> deleteLinha(@PathVariable int id, @PathVariable int linhaId, Authentication authentication) {
        if (!allowed(authentication)) return forbidden();

        Optional<DescritivoLinha> linhaOpt = linhaRepository.findById(linhaId);
        if (linhaOpt.isEmpty() || !linhaOpt.get().getDescritivoId().equals(id)) {
            return ResponseEntity.notFound().build();
        }
        linhaRepository.deleteById(linhaId);

        return ResponseEntity.ok(toView(descritivoRepository.findById(id).orElseThrow()));
    }

    @PostMapping("/{id}/gerar-proximo-mes")
    public ResponseEntity<?> gerarProximoMes(@PathVariable int id, Authentication authentication) {
        if (!allowed(authentication)) return forbidden();

        Optional<Descritivo> descritivoOpt = descritivoRepository.findById(id);
        if (descritivoOpt.isEmpty()) return ResponseEntity.notFound().build();
        Descritivo atual = descritivoOpt.get();

        int proximoMes = atual.getMes() == 12 ? 1 : atual.getMes() + 1;
        int proximoAno = atual.getMes() == 12 ? atual.getAno() + 1 : atual.getAno();

        String rotuloAtual = normalizarRotulo(atual.getRotulo());
        List<Descritivo> existentesProximoMes = descritivoRepository.findByClienteIdAndMesAndAno(atual.getClienteId(), proximoMes, proximoAno);
        boolean jaExiste = existentesProximoMes.stream()
                .anyMatch(d -> Objects.equals(rotuloAtual, normalizarRotulo(d.getRotulo())));
        if (jaExiste) {
            return badRequest("Já existe um descritivo desse cliente pro próximo mês com esse mesmo rótulo.");
        }

        Descritivo novo = new Descritivo();
        novo.setClienteId(atual.getClienteId());
        novo.setMes(proximoMes);
        novo.setAno(proximoAno);
        novo.setRotulo(atual.getRotulo());
        Descritivo novoSalvo = descritivoRepository.save(novo);

        LocalDate primeiroDiaMesAtual = LocalDate.of(atual.getAno(), atual.getMes(), 1);
        LocalDate fimMesAtual = primeiroDiaMesAtual.withDayOfMonth(primeiroDiaMesAtual.lengthOfMonth());

        List<DescritivoLinha> linhasAtuais = linhaRepository.findByDescritivoId(id);
        for (DescritivoLinha linha : linhasAtuais) {
            boolean encerrouNesseMes = linha.getDataFim() != null && !linha.getDataFim().isAfter(fimMesAtual);
            if (encerrouNesseMes) {
                continue;
            }

            DescritivoLinha nova = new DescritivoLinha();
            nova.setDescritivoId(novoSalvo.getId());
            nova.setLojaId(linha.getLojaId());
            nova.setDiasSemana(linha.getDiasSemana());
            nova.setHorasPorAtendimento(linha.getHorasPorAtendimento());
            nova.setValorHora(linha.getValorHora());
            nova.setDataInicio(linha.getDataInicio());
            nova.setDataFim(linha.getDataFim());
            nova.setOrdem(linha.getOrdem());
            calcularValorTotal(nova, novoSalvo, null);
            linhaRepository.save(nova);
        }

        return ResponseEntity.ok(toView(novoSalvo));
    }

    private String normalizarRotulo(String rotulo) {
        if (rotulo == null) return null;
        String trimmed = rotulo.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String rotuloExibicao(Descritivo descritivo, List<Descritivo> irmaos) {
        String proprio = normalizarRotulo(descritivo.getRotulo());
        if (proprio != null) return proprio;
        if (irmaos == null || irmaos.size() <= 1) return null;

        List<Descritivo> ordenados = irmaos.stream()
                .sorted(Comparator.comparing(Descritivo::getId))
                .toList();
        for (int i = 0; i < ordenados.size(); i++) {
            if (ordenados.get(i).getId().equals(descritivo.getId())) {
                return "(" + (i + 1) + ")";
            }
        }
        return null;
    }

    private void applyRequest(DescritivoLinha linha, LinhaRequest request) {
        linha.setLojaId(request.lojaId());
        linha.setDiasSemana(request.diasSemana().stream().map(String::valueOf).collect(Collectors.joining(",")));
        linha.setHorasPorAtendimento(request.horasPorAtendimento());
        linha.setValorHora(request.valorHora());
        linha.setDataInicio(request.dataInicio());
        linha.setDataFim(request.dataFim());
    }

    private void calcularValorTotal(DescritivoLinha linha, Descritivo descritivo, BigDecimal valorManual) {
        if (valorManual != null) {
            linha.setValorTotal(valorManual);
            linha.setValorTotalManual(true);
            return;
        }

        Set<DayOfWeek> dias = parseDiasSemana(linha.getDiasSemana());
        LocalDate inicioMes = LocalDate.of(descritivo.getAno(), descritivo.getMes(), 1);
        LocalDate fimMes = inicioMes.withDayOfMonth(inicioMes.lengthOfMonth());

        LocalDate periodoInicio = linha.getDataInicio().isAfter(inicioMes) ? linha.getDataInicio() : inicioMes;
        LocalDate periodoFim = (linha.getDataFim() != null && linha.getDataFim().isBefore(fimMes)) ? linha.getDataFim() : fimMes;

        BigDecimal total = BigDecimal.ZERO;
        if (!periodoInicio.isAfter(periodoFim)) {
            long ocorrencias = 0;
            for (LocalDate d = periodoInicio; !d.isAfter(periodoFim); d = d.plusDays(1)) {
                if (dias.contains(d.getDayOfWeek())) {
                    ocorrencias++;
                }
            }
            total = linha.getValorHora().multiply(linha.getHorasPorAtendimento()).multiply(BigDecimal.valueOf(ocorrencias));
        }

        linha.setValorTotal(total);
        linha.setValorTotalManual(false);
    }

    private Set<DayOfWeek> parseDiasSemana(String diasSemana) {
        if (diasSemana == null || diasSemana.isBlank()) return Set.of();
        return Arrays.stream(diasSemana.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(Integer::parseInt)
                .map(DayOfWeek::of)
                .collect(Collectors.toSet());
    }

    private List<Integer> diasSemanaParaLista(String diasSemana) {
        return parseDiasSemana(diasSemana).stream()
                .map(DayOfWeek::getValue)
                .sorted()
                .toList();
    }

    private String validateLinha(LinhaRequest request) {
        if (request.lojaId() == null || !lojaRepository.existsById(request.lojaId())) {
            return "Selecione uma loja válida.";
        }
        if (request.diasSemana() == null || request.diasSemana().isEmpty()) {
            return "Selecione ao menos um dia da semana.";
        }
        for (Integer dia : request.diasSemana()) {
            if (dia == null || dia < 1 || dia > 7) {
                return "Dia da semana inválido.";
            }
        }
        if (request.horasPorAtendimento() == null || request.horasPorAtendimento().compareTo(BigDecimal.ZERO) <= 0) {
            return "Informe as horas por atendimento.";
        }
        if (request.valorHora() == null || request.valorHora().compareTo(BigDecimal.ZERO) <= 0) {
            return "Informe o valor da hora.";
        }
        if (request.dataInicio() == null) {
            return "Informe a data de início.";
        }
        if (request.dataFim() != null && request.dataFim().isBefore(request.dataInicio())) {
            return "A data de término não pode ser antes da data de início.";
        }
        return null;
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.trim().toUpperCase();
    }

    private int prioridadeRede(String rede) {
        String normalizado = normalizar(rede);
        int indice = REDE_PRIORIDADE.indexOf(normalizado);
        return indice >= 0 ? indice : REDE_PRIORIDADE.size();
    }

    private String chaveOrdenacaoLinha(DescritivoLinha linha, Map<Integer, Loja> lojasById) {
        Loja loja = lojasById.get(linha.getLojaId());
        String rede = loja != null ? loja.getRede() : null;
        boolean semRede = rede == null || rede.isBlank();

        int tier = semRede ? 1 : 0;
        int prioridade = semRede ? REDE_PRIORIDADE.size() + 1 : prioridadeRede(rede);
        String uf = normalizar(loja != null ? loja.getUf() : null);
        String nome = normalizar(loja != null ? loja.getNome() : null);

        return tier + "|" + String.format("%02d", prioridade) + "|" + normalizar(rede) + "|" + uf + "|" + nome;
    }

    private DescritivoView toView(Descritivo descritivo) {
        Client cliente = clientRepository.findById(descritivo.getClienteId()).orElse(null);
        Map<Integer, Loja> lojasById = lojaRepository.findAll().stream()
                .collect(Collectors.toMap(Loja::getId, l -> l));

        LocalDate inicioMes = LocalDate.of(descritivo.getAno(), descritivo.getMes(), 1);
        LocalDate fimMes = inicioMes.withDayOfMonth(inicioMes.lengthOfMonth());

        List<DescritivoLinha> linhas = linhaRepository.findByDescritivoId(descritivo.getId()).stream()
                .sorted(Comparator.comparing(l -> chaveOrdenacaoLinha(l, lojasById)))
                .toList();

        List<LinhaView> linhaViews = new ArrayList<>();
        int numero = 1;
        for (DescritivoLinha linha : linhas) {
            Loja loja = lojasById.get(linha.getLojaId());

            LocalDate dataInicioExibicao = linha.getDataInicio().isAfter(inicioMes) ? linha.getDataInicio() : inicioMes;
            LocalDate dataFimExibicao = (linha.getDataFim() != null && linha.getDataFim().isBefore(fimMes)) ? linha.getDataFim() : fimMes;
            boolean encerrando = linha.getDataFim() != null && !linha.getDataFim().isAfter(fimMes);

            List<Integer> dias = diasSemanaParaLista(linha.getDiasSemana());
            String horasLabel = linha.getHorasPorAtendimento().stripTrailingZeros().toPlainString();
            String label = dias.size() + "X - " + horasLabel + "H";

            linhaViews.add(new LinhaView(
                    linha.getId(), numero++, linha.getLojaId(),
                    loja != null ? loja.getNome() : null,
                    loja != null ? loja.getUf() : null,
                    loja != null ? loja.getCnpj() : null,
                    dias, label, linha.getHorasPorAtendimento(), linha.getValorHora(),
                    linha.getDataInicio(), linha.getDataFim(),
                    dataInicioExibicao, dataFimExibicao, encerrando,
                    linha.getValorTotal(), linha.isValorTotalManual()
            ));
        }

        BigDecimal totalGeral = linhaViews.stream()
                .map(LinhaView::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Descritivo> irmaos = descritivoRepository.findByClienteIdAndMesAndAno(
                descritivo.getClienteId(), descritivo.getMes(), descritivo.getAno());

        return new DescritivoView(
                descritivo.getId(), descritivo.getClienteId(),
                cliente != null ? cliente.getName() : null,
                cliente != null ? cliente.getCorporateName() : null,
                cliente != null ? cliente.getCnpj() : null,
                descritivo.getMes(), descritivo.getAno(),
                descritivo.getRotulo(), rotuloExibicao(descritivo, irmaos),
                linhaViews, totalGeral
        );
    }

    private boolean allowed(Authentication authentication) {
        return accessControl.isSupervisor(authentication) || accessControl.isFinance(authentication) || accessControl.isAdmin(authentication);
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para acessar descritivos."));
    }
}