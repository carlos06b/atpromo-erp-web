package controller;

import dao.ClientDAO;
import dao.InvoiceDAO;
import model.Client;
import model.Invoice;
import model.InvoiceView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class InvoiceController {

    private final InvoiceDAO invoiceDAO;
    private final ClientDAO clientDAO;

    public InvoiceController() {
        this.invoiceDAO = new InvoiceDAO();
        this.clientDAO = new ClientDAO();
    }

    public void createPendingInvoice(int clientId, BigDecimal amount, String description, LocalDate dueDate) {
        Client client = clientDAO.findById(clientId);

        if (client == null) {
            throw new RuntimeException("Indústria não encontrada.");
        }

        if (!client.isActive()) {
            throw new RuntimeException("Não é possível criar faturamento para indústria inativa.");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("O valor do faturamento deve ser maior que zero.");
        }

        if (dueDate == null) {
            throw new RuntimeException("A data prevista de faturamento é obrigatória.");
        }

        Invoice invoice = new Invoice();
        invoice.setClientId(clientId);
        invoice.setAmount(amount);
        invoice.setReceivedAmount(null);
        invoice.setDescription(formatNullable(description));
        invoice.setDueDate(dueDate);
        invoice.setStatus("PENDENTE");

        invoiceDAO.save(invoice);
    }

    public Invoice findById(int id) {
        Invoice invoice = invoiceDAO.findById(id);

        if (invoice == null) {
            throw new RuntimeException("Faturamento não encontrado.");
        }

        return invoice;
    }

    public List<InvoiceView> listByPeriod(LocalDate start, LocalDate end) {
        validatePeriod(start, end);
        return invoiceDAO.findViewByPeriod(start, end);
    }

    public List<InvoiceView> listByFilters(LocalDate start, LocalDate end, String status, String companyLink) {
        validatePeriod(start, end);
        return invoiceDAO.findViewByFilters(start, end, status, companyLink);
    }

    public List<InvoiceView> listIssuedNotPaid() {
        return invoiceDAO.findIssuedNotPaid();
    }

    public void updateInvoice(int invoiceId, int clientId, BigDecimal amount, String description, LocalDate dueDate) {
        Invoice existingInvoice = invoiceDAO.findById(invoiceId);

        if (existingInvoice == null) {
            throw new RuntimeException("Faturamento não encontrado.");
        }

        if ("PAGO".equals(existingInvoice.getStatus())) {
            throw new RuntimeException("Faturamentos recebidos não podem ser editados.");
        }

        if ("CANCELADO".equals(existingInvoice.getStatus())) {
            throw new RuntimeException("Faturamentos cancelados não podem ser editados.");
        }

        Client client = clientDAO.findById(clientId);

        if (client == null) {
            throw new RuntimeException("Indústria não encontrada.");
        }

        if (!client.isActive()) {
            throw new RuntimeException("Não é possível vincular faturamento a uma indústria inativa.");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("O valor do faturamento deve ser maior que zero.");
        }

        if (dueDate == null) {
            throw new RuntimeException("A data prevista de faturamento é obrigatória.");
        }

        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setClientId(clientId);
        invoice.setAmount(amount);
        invoice.setReceivedAmount(existingInvoice.getReceivedAmount());
        invoice.setDescription(formatNullable(description));
        invoice.setDueDate(dueDate);
        invoice.setIssueDate(existingInvoice.getIssueDate());
        invoice.setPaymentDate(existingInvoice.getPaymentDate());
        invoice.setStatus(existingInvoice.getStatus());

        invoiceDAO.update(invoice);
    }

    public void markAsIssued(int invoiceId, LocalDate issueDate) {
        Invoice invoice = invoiceDAO.findById(invoiceId);

        if (invoice == null) {
            throw new RuntimeException("Faturamento não encontrado.");
        }

        if (!"PENDENTE".equals(invoice.getStatus())) {
            throw new RuntimeException("Apenas faturamentos pendentes podem ser marcados como faturados.");
        }

        if (issueDate == null) {
            throw new RuntimeException("A data de faturamento é obrigatória.");
        }

        invoiceDAO.markAsIssued(invoiceId, issueDate);
    }

    public void markAsPaid(int invoiceId, LocalDate paymentDate, BigDecimal receivedAmount) {
        Invoice invoice = invoiceDAO.findById(invoiceId);

        if (invoice == null) {
            throw new RuntimeException("Faturamento não encontrado.");
        }

        if (!"FATURADO".equals(invoice.getStatus())) {
            throw new RuntimeException("Apenas faturamentos já faturados podem ser marcados como recebidos.");
        }

        if (paymentDate == null) {
            throw new RuntimeException("A data de recebimento é obrigatória.");
        }

        if (receivedAmount == null || receivedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("O valor recebido deve ser maior que zero.");
        }

        if (invoice.getIssueDate() != null && paymentDate.isBefore(invoice.getIssueDate())) {
            throw new RuntimeException("A data de recebimento não pode ser anterior à data de faturamento.");
        }

        invoiceDAO.markAsPaid(invoiceId, paymentDate, receivedAmount);
    }

    public void cancelInvoice(int id) {
        Invoice invoice = invoiceDAO.findById(id);

        if (invoice == null) {
            throw new RuntimeException("Faturamento não encontrado.");
        }

        if ("PAGO".equals(invoice.getStatus())) {
            throw new RuntimeException("Não é possível cancelar faturamento já recebido.");
        }

        invoiceDAO.cancelInvoice(id);
    }

    private void validatePeriod(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new RuntimeException("Informe a data inicial e final.");
        }

        if (end.isBefore(start)) {
            throw new RuntimeException("A data final não pode ser menor que a data inicial.");
        }
    }

    private String formatNullable(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}