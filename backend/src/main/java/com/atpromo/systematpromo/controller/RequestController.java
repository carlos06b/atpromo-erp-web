package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.FinancePromoter;
import com.atpromo.systematpromo.model.Promoter;
import com.atpromo.systematpromo.model.PromoterPaymentData;
import com.atpromo.systematpromo.model.Request;
import com.atpromo.systematpromo.model.User;
import com.atpromo.systematpromo.repository.FinancePromoterRepository;
import com.atpromo.systematpromo.repository.PromoterRepository;
import com.atpromo.systematpromo.repository.RequestRepository;
import com.atpromo.systematpromo.repository.UserRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private static final Map<String, String> TYPE_LABELS = new LinkedHashMap<>();

    static {
        TYPE_LABELS.put("BONIFICACAO", "Bonificação");
        TYPE_LABELS.put("AJUDA_CUSTO", "Ajuda de Custo");
        TYPE_LABELS.put("ASO", "ASO");
        TYPE_LABELS.put("EPI", "EPI");
        TYPE_LABELS.put("RESCISAO", "Rescisão");
        TYPE_LABELS.put("FERIAS", "Férias");
        TYPE_LABELS.put("ADIANTAMENTO", "Adiantamento");
        TYPE_LABELS.put("REEMBOLSO", "Reembolso");
        TYPE_LABELS.put("CORRECAO_PAGAMENTO", "Correção de Pagamento");
        TYPE_LABELS.put("OUTROS", "Outros");
    }

    private final RequestRepository requestRepository;
    private final PromoterRepository promoterRepository;
    private final UserRepository userRepository;
    private final FinancePromoterRepository financePromoterRepository;

    public RequestController(RequestRepository requestRepository,
                             PromoterRepository promoterRepository,
                             UserRepository userRepository,
                             FinancePromoterRepository financePromoterRepository) {
        this.requestRepository = requestRepository;
        this.promoterRepository = promoterRepository;
        this.userRepository = userRepository;
        this.financePromoterRepository = financePromoterRepository;
    }

    public record RequestTypeOption(String value, String label) {}

    public record RequestResponse(
            Integer id,
            Integer promoterId,
            String promoterName,
            String promoterPix,
            String promoterPixType,
            String type,
            String typeLabel,
            BigDecimal amount,
            String message,
            String status,
            LocalDateTime date,
            String rhUserName,
            String finUserName
    ) {}

    public record CreateRequestBody(Integer promoterId, String type, BigDecimal amount, String message) {}

    @GetMapping("/types")
    public List<RequestTypeOption> getTypes() {
        return TYPE_LABELS.entrySet().stream()
                .map(entry -> new RequestTypeOption(entry.getKey(), entry.getValue()))
                .toList();
    }

    @GetMapping
    public List<RequestResponse> listAll() {
        return mapAll(requestRepository.findAll());
    }

    @GetMapping("/pending")
    public List<RequestResponse> listPending() {
        return mapAll(requestRepository.findByStatusIgnoreCase("PENDENTE"));
    }

    @GetMapping("/period")
    public ResponseEntity<?> listByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        if (start.isAfter(end)) {
            return badRequest("Data inicial não pode ser maior que a final.");
        }

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        return ResponseEntity.ok(mapAll(requestRepository.findByDateBetween(startDateTime, endDateTime)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequestResponse> getById(@PathVariable int id) {
        return requestRepository.findById(id)
                .map(r -> ResponseEntity.ok(mapOne(r)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateRequestBody body, Authentication authentication) {
        User currentUser = currentUser(authentication);

        if (currentUser == null || !isRh(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("message", "Apenas usuários de RH podem criar solicitações."));
        }

        if (body.promoterId() == null) {
            return badRequest("Selecione um promotor.");
        }

        Promoter promoter = promoterRepository.findById(body.promoterId()).orElse(null);
        if (promoter == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Promotor não encontrado."));
        }

        String internalType = toInternalType(body.type());
        if (!TYPE_LABELS.containsKey(internalType)) {
            return badRequest("Tipo de solicitação inválido.");
        }

        if (body.amount() == null || body.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return badRequest("Informe um valor válido.");
        }

        if (body.message() == null || body.message().isBlank()) {
            return badRequest("Informe a mensagem com os dados para pagamento.");
        }

        Request request = new Request();
        request.setId_UserRH(currentUser.getId());
        request.setId_UserFin(null);
        request.setId_Promoter(promoter.getId());
        request.setType(internalType);
        request.setAmount(body.amount());
        request.setMessage(body.message());
        request.setStatus("PENDENTE");
        request.setDate(LocalDateTime.now());

        Request saved = requestRepository.save(request);

        return ResponseEntity.status(201).body(mapOne(saved));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable int id, Authentication authentication) {
        User currentUser = currentUser(authentication);

        if (currentUser == null || !isFinance(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("message", "Apenas usuários do Financeiro podem aprovar solicitações."));
        }

        Request request = requestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        if (!"PENDENTE".equalsIgnoreCase(request.getStatus())) {
            return ResponseEntity.status(409).body(Map.of("message", "Apenas solicitações pendentes podem ser aprovadas."));
        }

        Promoter promoter = promoterRepository.findById(request.getId_Promoter()).orElse(null);
        if (promoter == null) {
            return ResponseEntity.status(409).body(Map.of("message", "O promotor dessa solicitação não existe mais."));
        }

        request.setId_UserFin(currentUser.getId());
        request.setStatus("APROVADO");
        requestRepository.save(request);

        FinancePromoter finance = new FinancePromoter();
        finance.setIdPromoter(request.getId_Promoter());
        finance.setType(request.getType());
        finance.setAmount(request.getAmount());
        finance.setDate(LocalDate.now());
        finance.setStatus("PAGO");
        finance.setDescription(approvalDescription(request));
        financePromoterRepository.save(finance);

        return ResponseEntity.ok(mapOne(request));
    }

    @PutMapping("/{id}/cancel-approval")
    public ResponseEntity<?> cancelApproval(@PathVariable int id, Authentication authentication) {
        User currentUser = currentUser(authentication);

        if (currentUser == null || !isFinance(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("message", "Apenas usuários do Financeiro podem cancelar uma aprovação."));
        }

        Request request = requestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        if (!"APROVADO".equalsIgnoreCase(request.getStatus())) {
            return ResponseEntity.status(409).body(Map.of("message", "Apenas solicitações aprovadas podem ter a aprovação cancelada."));
        }

        String expectedDescription = approvalDescription(request);
        FinancePromoter linkedLaunch = financePromoterRepository.findAll().stream()
                .filter(f -> f.getIdPromoter() == request.getId_Promoter())
                .filter(f -> expectedDescription.equals(f.getDescription()))
                .findFirst()
                .orElse(null);

        if (linkedLaunch != null) {
            financePromoterRepository.deleteById(linkedLaunch.getId());
        }

        request.setId_UserFin(null);
        request.setStatus("PENDENTE");
        requestRepository.save(request);

        return ResponseEntity.ok(mapOne(request));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable int id, Authentication authentication) {
        User currentUser = currentUser(authentication);

        if (currentUser == null || !isFinance(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("message", "Apenas usuários do Financeiro podem rejeitar solicitações."));
        }

        Request request = requestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        if (!"PENDENTE".equalsIgnoreCase(request.getStatus())) {
            return ResponseEntity.status(409).body(Map.of("message", "Apenas solicitações pendentes podem ser rejeitadas."));
        }

        request.setId_UserFin(currentUser.getId());
        request.setStatus("REJEITADO");
        requestRepository.save(request);

        return ResponseEntity.ok(mapOne(request));
    }

    @PutMapping("/{id}/reopen")
    public ResponseEntity<?> reopen(@PathVariable int id, Authentication authentication) {
        User currentUser = currentUser(authentication);

        if (currentUser == null || !isFinance(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("message", "Apenas usuários do Financeiro podem reabrir solicitações."));
        }

        Request request = requestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        if ("APROVADO".equalsIgnoreCase(request.getStatus())) {
            return ResponseEntity.status(409).body(Map.of("message", "Solicitações aprovadas já geraram lançamento financeiro e não podem ser reabertas."));
        }

        if (!"REJEITADO".equalsIgnoreCase(request.getStatus())) {
            return ResponseEntity.status(409).body(Map.of("message", "Apenas solicitações rejeitadas podem ser reabertas."));
        }

        request.setStatus("PENDENTE");
        requestRepository.save(request);

        return ResponseEntity.ok(mapOne(request));
    }

    @GetMapping("/pix-batch")
    public ResponseEntity<?> pixBatch(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate,
            Authentication authentication) {

        User currentUser = currentUser(authentication);
        if (currentUser == null || !isFinance(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("message", "Apenas usuários do Financeiro podem exportar o lote de Pix."));
        }

        List<Request> pending = requestRepository.findByStatusIgnoreCase("PENDENTE");
        List<PromoterPaymentData> payments = new ArrayList<>();

        for (Request request : pending) {
            Promoter promoter = promoterRepository.findById(request.getId_Promoter()).orElse(null);

            if (promoter == null || promoter.getPix() == null || promoter.getPix().isBlank()) {
                continue;
            }

            payments.add(new PromoterPaymentData(
                    promoter.getCpf(),
                    promoter.getName(),
                    promoter.getPix(),
                    promoter.getPixType(),
                    request.getAmount(),
                    paymentDate
            ));
        }

        payments.sort(Comparator.comparing(PromoterPaymentData::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        return ResponseEntity.ok(payments);
    }

    private List<RequestResponse> mapAll(List<Request> list) {
        return list.stream()
                .sorted(Comparator.comparing(Request::getDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapOne)
                .toList();
    }

    private RequestResponse mapOne(Request r) {
        Promoter promoter = promoterRepository.findById(r.getId_Promoter()).orElse(null);
        User rhUser = r.getId_UserRH() > 0 ? userRepository.findById(r.getId_UserRH()).orElse(null) : null;
        User finUser = (r.getId_UserFin() != null && r.getId_UserFin() > 0)
                ? userRepository.findById(r.getId_UserFin()).orElse(null)
                : null;

        return new RequestResponse(
                r.getId(),
                r.getId_Promoter(),
                promoter != null ? promoter.getName() : "Promotor não encontrado",
                promoter != null ? promoter.getPix() : null,
                promoter != null ? promoter.getPixType() : null,
                r.getType(),
                TYPE_LABELS.getOrDefault(r.getType(), r.getType()),
                r.getAmount(),
                r.getMessage(),
                r.getStatus(),
                r.getDate(),
                rhUser != null ? rhUser.getName() : null,
                finUser != null ? finUser.getName() : null
        );
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }

    private boolean isRh(User user) {
        return user.getJobTittle() != null && user.getJobTittle().trim().equalsIgnoreCase("RH");
    }

    private boolean isFinance(User user) {
        return user.getJobTittle() != null && user.getJobTittle().trim().equalsIgnoreCase("FINANCEIRO");
    }

    private String toInternalType(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String text = value.trim();
        String possibleCode = text.toUpperCase(Locale.ROOT);

        if (TYPE_LABELS.containsKey(possibleCode)) {
            return possibleCode;
        }

        for (Map.Entry<String, String> entry : TYPE_LABELS.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(text)) {
                return entry.getKey();
            }
        }

        return possibleCode;
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private String approvalDescription(Request request) {
        return "Aprovado via solicitação #" + request.getId() + " (" + TYPE_LABELS.getOrDefault(request.getType(), request.getType()) + ")";
    }
}