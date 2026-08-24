package controller;

import dao.FinancePromoterDAO;
import dao.PromoterDAO;
import dao.RequestDAO;
import model.FinancePromoter;
import model.Promoter;
import model.PromoterPaymentData;
import model.Request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RequestController {

    private final RequestDAO requestDAO = new RequestDAO();
    private final PromoterDAO promoterDAO = new PromoterDAO();

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

    public void createRequest(int idUserRH, int idUserFin, int idPromoter,
                              String type, BigDecimal amount, String message) {

        if (promoterDAO.findById(idPromoter) == null) {
            System.out.println("Promotor não existe!");
            return;
        }

        String internalType = toInternalType(type);

        if (!isValidType(internalType)) {
            System.out.println("Tipo de solicitação inválido.");
            return;
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Valor inválido.");
            return;
        }

        if (message == null || message.isBlank()) {
            System.out.println("Mensagem não pode ficar vazia.");
            return;
        }

        Request request = new Request();

        request.setId_UserRH(idUserRH);
        request.setId_UserFin(idUserFin);
        request.setId_Promoter(idPromoter);
        request.setType(internalType);
        request.setAmount(amount);
        request.setMessage(message);
        request.setStatus("PENDENTE");
        request.setDate(LocalDateTime.now());

        requestDAO.save(request);
    }

    public void listAll() {
        List<Request> list = requestDAO.findAll();

        if (list.isEmpty()) {
            System.out.println("Nenhuma solicitação encontrada.");
            return;
        }

        for (Request r : list) {
            printRequest(r);
        }
    }

    public void listAllWithPromoterName() {
        List<String> list = requestDAO.findAllWithPromoterName();

        if (list.isEmpty()) {
            System.out.println("Nenhuma solicitação encontrada.");
            return;
        }

        for (String line : list) {
            System.out.println(line);
        }
    }

    public List<String> getAllWithPromoterName() {
        return requestDAO.findAllWithPromoterName();
    }

    public void listPendingWithPromoterName() {
        List<String> list = requestDAO.findPendingWithPromoterName();

        if (list.isEmpty()) {
            System.out.println("Nenhuma solicitação pendente.");
            return;
        }

        for (String line : list) {
            System.out.println(line);
        }
    }

    public List<String> getPendingWithPromoterName() {
        return requestDAO.findPendingWithPromoterName();
    }

    public void listByStatus(String status) {
        List<Request> list = requestDAO.findByStatus(status);

        if (list.isEmpty()) {
            System.out.println("Nenhuma solicitação com status: " + status);
            return;
        }

        for (Request r : list) {
            printRequest(r);
        }
    }

    public void listByPeriod(LocalDateTime start, LocalDateTime end) {
        List<String> list = requestDAO.findByPeriodWithPromoterName(start, end);

        if (list.isEmpty()) {
            System.out.println("Nenhuma solicitação nesse período.");
            return;
        }

        for (String line : list) {
            System.out.println(line);
        }
    }

    public List<String> getByPeriodWithPromoterName(LocalDateTime start, LocalDateTime end) {
        return requestDAO.findByPeriodWithPromoterName(start, end);
    }

    public boolean approve(int id) {
        return approve(id, 0);
    }

    public boolean approve(int id, int idUserFin) {
        List<Request> list = requestDAO.findAll();

        for (Request r : list) {
            if (r.getId() == id) {

                if (!r.getStatus().equalsIgnoreCase("PENDENTE")) {
                    System.out.println("Apenas solicitações pendentes podem ser aprovadas.");
                    return false;
                }

                if (promoterDAO.findById(r.getId_Promoter()) == null) {
                    System.out.println("Erro: promotor não existe mais.");
                    return false;
                }

                if (idUserFin > 0) {
                    requestDAO.updateFinanceUser(id, idUserFin);
                }

                requestDAO.updateStatus(id, "APROVADO");

                FinancePromoter finance = new FinancePromoter();
                finance.setIdPromoter(r.getId_Promoter());
                finance.setType(r.getType());
                finance.setAmount(r.getAmount());
                finance.setDate(LocalDate.now());
                finance.setStatus("PAGO");

                FinancePromoterDAO financeDAO = new FinancePromoterDAO();
                financeDAO.save(finance);

                System.out.println("Solicitação aprovada e lançada no financeiro.");
                return true;
            }
        }

        System.out.println("Solicitação não encontrada.");
        return false;
    }

    public boolean reject(int id) {
        List<Request> list = requestDAO.findAll();

        for (Request r : list) {
            if (r.getId() == id) {

                if (!r.getStatus().equalsIgnoreCase("PENDENTE")) {
                    System.out.println("Apenas solicitações pendentes podem ser rejeitadas.");
                    return false;
                }

                requestDAO.updateStatus(id, "REJEITADO");
                System.out.println("Solicitação rejeitada.");
                return true;
            }
        }

        System.out.println("Solicitação não encontrada.");
        return false;
    }

    public boolean reopenRejected(int id) {
        List<Request> list = requestDAO.findAll();

        for (Request r : list) {
            if (r.getId() == id) {

                if (r.getStatus().equalsIgnoreCase("APROVADO")) {
                    System.out.println("Solicitações aprovadas já geraram lançamento financeiro e não podem ser reabertas automaticamente.");
                    return false;
                }

                if (!r.getStatus().equalsIgnoreCase("REJEITADO")) {
                    System.out.println("Apenas solicitações rejeitadas podem ser reabertas.");
                    return false;
                }

                requestDAO.updateStatus(id, "PENDENTE");
                System.out.println("Solicitação reaberta com sucesso.");
                return true;
            }
        }

        System.out.println("Solicitação não encontrada.");
        return false;
    }

    public void delete(int id) {
        requestDAO.delete(id);
    }

    public List<String> getValidTypeLabels() {
        return Collections.unmodifiableList(new ArrayList<>(TYPE_LABELS.values()));
    }

    public String getTypeLabel(String type) {
        if (type == null || type.isBlank()) {
            return "";
        }

        String internalType = toInternalType(type);
        return TYPE_LABELS.getOrDefault(internalType, type);
    }

    public String toInternalType(String value) {
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

    private boolean isValidType(String type) {
        return TYPE_LABELS.containsKey(toInternalType(type));
    }

    private void printRequest(Request r) {
        System.out.println(
                r.getId() + " | " +
                        "Promotor: " + r.getId_Promoter() + " | " +
                        getTypeLabel(r.getType()) + " | " +
                        "R$ " + r.getAmount() + " | " +
                        r.getMessage() + " | " +
                        r.getStatus() + " | " +
                        r.getDate()
        );
    }

    public List<PromoterPaymentData> getPendingPixBatch(LocalDate paymentDate) {
        List<Request> requests = requestDAO.findByStatus("PENDENTE");
        List<PromoterPaymentData> payments = new ArrayList<>();

        for (Request request : requests) {
            Promoter promoter = promoterDAO.findById(request.getId_Promoter());

            if (promoter == null) {
                continue;
            }

            if (promoter.getPix() == null || promoter.getPix().isBlank()) {
                continue;
            }

            payments.add(
                    new PromoterPaymentData(
                            promoter.getCpf(),
                            promoter.getName(),
                            promoter.getPix(),
                            promoter.getPixType(),
                            request.getAmount(),
                            paymentDate
                    )
            );
        }

        return payments;
    }
}