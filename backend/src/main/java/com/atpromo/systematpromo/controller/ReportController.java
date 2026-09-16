package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.Client;
import com.atpromo.systematpromo.model.FinancePromoter;
import com.atpromo.systematpromo.model.FixedExpenseHistory;
import com.atpromo.systematpromo.model.Invoice;
import com.atpromo.systematpromo.model.Promoter;
import com.atpromo.systematpromo.model.VariableExpense;
import com.atpromo.systematpromo.repository.ClientRepository;
import com.atpromo.systematpromo.repository.FinancePromoterRepository;
import com.atpromo.systematpromo.repository.FixedExpenseHistoryRepository;
import com.atpromo.systematpromo.repository.InvoiceRepository;
import com.atpromo.systematpromo.repository.PromoterRepository;
import com.atpromo.systematpromo.repository.VariableExpenseRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final InvoiceRepository invoiceRepository;
    private final FinancePromoterRepository financePromoterRepository;
    private final FixedExpenseHistoryRepository fixedExpenseHistoryRepository;
    private final VariableExpenseRepository variableExpenseRepository;
    private final ClientRepository clientRepository;
    private final PromoterRepository promoterRepository;
    private final AccessControl accessControl;

    public ReportController(InvoiceRepository invoiceRepository,
                            FinancePromoterRepository financePromoterRepository,
                            FixedExpenseHistoryRepository fixedExpenseHistoryRepository,
                            VariableExpenseRepository variableExpenseRepository,
                            ClientRepository clientRepository,
                            PromoterRepository promoterRepository,
                            AccessControl accessControl) {
        this.invoiceRepository = invoiceRepository;
        this.financePromoterRepository = financePromoterRepository;
        this.fixedExpenseHistoryRepository = fixedExpenseHistoryRepository;
        this.variableExpenseRepository = variableExpenseRepository;
        this.clientRepository = clientRepository;
        this.promoterRepository = promoterRepository;
        this.accessControl = accessControl;
    }

    @GetMapping("/general")
    public ResponseEntity<?> getGeneralReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {

        if (accessControl.isRh(authentication)) {
            return forbidden();
        }

        validatePeriod(start, end);

        List<Invoice> invoices = invoiceRepository.findAll();
        List<FinancePromoter> finances = financePromoterRepository.findAll();

        BigDecimal expectedIncome = totalExpectedByDueDate(invoices, start, end);
        BigDecimal issuedIncome = totalIssuedByIssueDate(invoices, start, end);
        BigDecimal receivedIncome = totalPaidByPaymentDate(invoices, start, end);
        BigDecimal openIncome = totalOpenByDueDate(invoices, start, end);
        BigDecimal canceledIncome = totalCanceledByDueDate(invoices, start, end);

        Map<String, BigDecimal> promoterTotals = totalByTypeAndPeriod(finances, start, end);

        BigDecimal promoterExpenses = calculatePromoterExpenses(promoterTotals);
        BigDecimal fixedExpenses = calculateFixedExpenses(start, end);
        BigDecimal variableExpenses = calculateVariableExpenses(start, end);
        BigDecimal discounts = promoterTotals.getOrDefault("DESCONTO", BigDecimal.ZERO);

        BigDecimal totalExpenses = promoterExpenses.add(fixedExpenses).add(variableExpenses);

        BigDecimal realResult = receivedIncome.subtract(totalExpenses);
        BigDecimal expectedResult = realResult.add(openIncome);

        return ResponseEntity.ok(new GeneralReport(expectedIncome, issuedIncome, receivedIncome, openIncome, canceledIncome,
                promoterExpenses, fixedExpenses, variableExpenses, totalExpenses, discounts, realResult, expectedResult));
    }

    @GetMapping("/income")
    public ResponseEntity<?> getIncomeReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {

        if (accessControl.isRh(authentication)) {
            return forbidden();
        }

        validatePeriod(start, end);

        List<Invoice> invoices = invoiceRepository.findAll();
        Map<Integer, Client> clientsById = clientRepository.findAll().stream()
                .collect(Collectors.toMap(Client::getId, c -> c));

        List<InvoiceLine> invoicesByDueDate = invoices.stream()
                .filter(i -> i.getDueDate() != null && !i.getDueDate().isBefore(start) && !i.getDueDate().isAfter(end))
                .sorted(Comparator.comparing(Invoice::getDueDate))
                .map(i -> toInvoiceLine(i, clientsById))
                .toList();

        List<InvoiceLine> receivedInvoices = invoices.stream()
                .filter(i -> "PAGO".equalsIgnoreCase(i.getStatus())
                        && i.getPaymentDate() != null
                        && !i.getPaymentDate().isBefore(start)
                        && !i.getPaymentDate().isAfter(end))
                .sorted(Comparator.comparing(Invoice::getPaymentDate))
                .map(i -> toInvoiceLine(i, clientsById))
                .toList();

        BigDecimal expectedIncome = totalExpectedByDueDate(invoices, start, end);
        BigDecimal issuedIncome = totalIssuedByIssueDate(invoices, start, end);
        BigDecimal receivedIncome = totalPaidByPaymentDate(invoices, start, end);
        BigDecimal openIncome = totalOpenByDueDate(invoices, start, end);

        return ResponseEntity.ok(new IncomeReport(expectedIncome, issuedIncome, receivedIncome, openIncome, invoicesByDueDate, receivedInvoices));
    }

    @GetMapping("/expenses")
    public ResponseEntity<?> getExpenseReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {

        if (accessControl.isRh(authentication)) {
            return forbidden();
        }

        validatePeriod(start, end);

        Map<Integer, Promoter> promotersById = promoterRepository.findAll().stream()
                .collect(Collectors.toMap(Promoter::getId, p -> p));

        List<FinancePromoter> finances = financePromoterRepository.findAll();

        List<PromoterFinanceLine> promoterPayments = finances.stream()
                .filter(f -> !"DESCONTO".equalsIgnoreCase(f.getType()))
                .filter(f -> f.getDate() != null && !f.getDate().isBefore(start) && !f.getDate().isAfter(end))
                .sorted(Comparator.comparing(FinancePromoter::getDate).reversed())
                .map(f -> toFinanceLine(f, promotersById))
                .toList();

        List<PromoterFinanceLine> discountEntries = finances.stream()
                .filter(f -> "DESCONTO".equalsIgnoreCase(f.getType()))
                .filter(f -> f.getDate() != null && !f.getDate().isBefore(start) && !f.getDate().isAfter(end))
                .sorted(Comparator.comparing(FinancePromoter::getDate).reversed())
                .map(f -> toFinanceLine(f, promotersById))
                .toList();

        List<FixedExpenseHistory> fixedExpenseHistory = fixedExpenseHistoryRepository.findAll().stream()
                .filter(e -> e.getDueDate() != null && !e.getDueDate().isBefore(start) && !e.getDueDate().isAfter(end))
                .toList();

        List<VariableExpense> variableExpenseList = variableExpenseRepository.findAll().stream()
                .filter(e -> e.getDate() != null && !e.getDate().isBefore(start) && !e.getDate().isAfter(end))
                .toList();

        Map<String, BigDecimal> promoterTotals = totalByTypeAndPeriod(finances, start, end);

        BigDecimal promoterExpenses = calculatePromoterExpenses(promoterTotals);
        BigDecimal fixedTotal = calculateFixedExpenses(start, end);
        BigDecimal variableTotal = calculateVariableExpenses(start, end);
        BigDecimal discounts = promoterTotals.getOrDefault("DESCONTO", BigDecimal.ZERO);
        BigDecimal totalExpenses = promoterExpenses.add(fixedTotal).add(variableTotal);

        return ResponseEntity.ok(new ExpenseReport(promoterExpenses, fixedTotal, variableTotal, totalExpenses, discounts,
                promoterPayments, discountEntries, fixedExpenseHistory, variableExpenseList));
    }

    @GetMapping("/by-type")
    public ResponseEntity<?> getTypeReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {

        if (accessControl.isRh(authentication)) {
            return forbidden();
        }

        validatePeriod(start, end);

        List<Invoice> invoices = invoiceRepository.findAll();
        List<FinancePromoter> finances = financePromoterRepository.findAll();

        BigDecimal expectedIncome = totalExpectedByDueDate(invoices, start, end);
        BigDecimal issuedIncome = totalIssuedByIssueDate(invoices, start, end);
        BigDecimal receivedIncome = totalPaidByPaymentDate(invoices, start, end);
        BigDecimal openIncome = totalOpenByDueDate(invoices, start, end);
        BigDecimal canceledIncome = totalCanceledByDueDate(invoices, start, end);

        Map<String, BigDecimal> promoterTotals = totalByTypeAndPeriod(finances, start, end);
        Map<String, BigDecimal> companyTotals = totalByCompanyAndDueDate(invoices, start, end);

        return ResponseEntity.ok(new TypeReport(expectedIncome, issuedIncome, receivedIncome, openIncome, canceledIncome,
                companyTotals, promoterTotals, calculateFixedExpenses(start, end), calculateVariableExpenses(start, end)));
    }

    private BigDecimal totalExpectedByDueDate(List<Invoice> invoices, LocalDate start, LocalDate end) {
        return invoices.stream()
                .filter(i -> i.getDueDate() != null && !i.getDueDate().isBefore(start) && !i.getDueDate().isAfter(end))
                .filter(i -> !"CANCELADO".equalsIgnoreCase(i.getStatus()))
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, this::add);
    }

    private BigDecimal totalOpenByDueDate(List<Invoice> invoices, LocalDate start, LocalDate end) {
        return invoices.stream()
                .filter(i -> i.getDueDate() != null && !i.getDueDate().isBefore(start) && !i.getDueDate().isAfter(end))
                .filter(i -> "PENDENTE".equalsIgnoreCase(i.getStatus()) || "FATURADO".equalsIgnoreCase(i.getStatus()))
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, this::add);
    }

    private BigDecimal totalIssuedByIssueDate(List<Invoice> invoices, LocalDate start, LocalDate end) {
        return invoices.stream()
                .filter(i -> i.getIssueDate() != null && !i.getIssueDate().isBefore(start) && !i.getIssueDate().isAfter(end))
                .filter(i -> "FATURADO".equalsIgnoreCase(i.getStatus()) || "PAGO".equalsIgnoreCase(i.getStatus()))
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, this::add);
    }

    private BigDecimal totalPaidByPaymentDate(List<Invoice> invoices, LocalDate start, LocalDate end) {
        return invoices.stream()
                .filter(i -> i.getPaymentDate() != null && !i.getPaymentDate().isBefore(start) && !i.getPaymentDate().isAfter(end))
                .filter(i -> "PAGO".equalsIgnoreCase(i.getStatus()))
                .map(i -> i.getReceivedAmount() != null ? i.getReceivedAmount() : i.getAmount())
                .reduce(BigDecimal.ZERO, this::add);
    }

    private BigDecimal totalCanceledByDueDate(List<Invoice> invoices, LocalDate start, LocalDate end) {
        return invoices.stream()
                .filter(i -> i.getDueDate() != null && !i.getDueDate().isBefore(start) && !i.getDueDate().isAfter(end))
                .filter(i -> "CANCELADO".equalsIgnoreCase(i.getStatus()))
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, this::add);
    }

    private Map<String, BigDecimal> totalByCompanyAndDueDate(List<Invoice> invoices, LocalDate start, LocalDate end) {
        Map<Integer, Client> clientsById = clientRepository.findAll().stream()
                .collect(Collectors.toMap(Client::getId, c -> c));

        Map<String, BigDecimal> totals = new HashMap<>();

        for (Invoice invoice : invoices) {
            if (invoice.getDueDate() == null || invoice.getDueDate().isBefore(start) || invoice.getDueDate().isAfter(end)) continue;
            if ("CANCELADO".equalsIgnoreCase(invoice.getStatus())) continue;

            Client client = clientsById.get(invoice.getClientId());
            String companyLink = client != null ? client.getCompanyLink() : "N/A";

            totals.merge(companyLink, nullToZero(invoice.getAmount()), BigDecimal::add);
        }

        return totals;
    }

    private Map<String, BigDecimal> totalByTypeAndPeriod(List<FinancePromoter> finances, LocalDate start, LocalDate end) {
        Map<String, BigDecimal> totals = new HashMap<>();

        for (FinancePromoter finance : finances) {
            if (finance.getDate() == null || finance.getDate().isBefore(start) || finance.getDate().isAfter(end)) continue;

            totals.merge(finance.getType(), nullToZero(finance.getAmount()), BigDecimal::add);
        }

        return totals;
    }

    private BigDecimal calculatePromoterExpenses(Map<String, BigDecimal> totals) {
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : totals.entrySet()) {
            if (!"DESCONTO".equalsIgnoreCase(entry.getKey())) {
                total = total.add(nullToZero(entry.getValue()));
            }
        }

        return total;
    }

    private BigDecimal calculateFixedExpenses(LocalDate start, LocalDate end) {
        BigDecimal total = BigDecimal.ZERO;

        for (FixedExpenseHistory expense : fixedExpenseHistoryRepository.findAll()) {
            if (expense.getDueDate() == null || expense.getDueDate().isBefore(start) || expense.getDueDate().isAfter(end)) continue;
            if ("PAGO".equalsIgnoreCase(expense.getStatus())) {
                total = total.add(nullToZero(expense.getAmount()));
            }
        }

        return total;
    }

    private BigDecimal calculateVariableExpenses(LocalDate start, LocalDate end) {
        BigDecimal total = BigDecimal.ZERO;

        for (VariableExpense expense : variableExpenseRepository.findAll()) {
            if (expense.getDate() == null || expense.getDate().isBefore(start) || expense.getDate().isAfter(end)) continue;
            if (expense.isStatus()) {
                total = total.add(nullToZero(expense.getAmount()));
            }
        }

        return total;
    }

    private InvoiceLine toInvoiceLine(Invoice invoice, Map<Integer, Client> clientsById) {
        Client client = clientsById.get(invoice.getClientId());

        return new InvoiceLine(
                invoice.getId(),
                client != null ? client.getName() : null,
                client != null ? client.getCompanyLink() : null,
                invoice.getAmount(),
                invoice.getReceivedAmount(),
                invoice.getDescription(),
                invoice.getDueDate(),
                invoice.getIssueDate(),
                invoice.getPaymentDate(),
                invoice.getStatus()
        );
    }

    private PromoterFinanceLine toFinanceLine(FinancePromoter finance, Map<Integer, Promoter> promotersById) {
        Promoter promoter = promotersById.get(finance.getIdPromoter());

        return new PromoterFinanceLine(
                finance.getId(),
                promoter != null ? promoter.getName() : null,
                finance.getType(),
                finance.getAmount(),
                finance.getDate(),
                finance.getStatus(),
                finance.getDescription()
        );
    }

    private void validatePeriod(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new RuntimeException("Informe a data inicial e final.");
        }

        if (start.isAfter(end)) {
            throw new RuntimeException("Data inicial não pode ser maior que a final.");
        }
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal add(BigDecimal a, BigDecimal b) {
        return a.add(nullToZero(b));
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para acessar relatórios."));
    }

    public record GeneralReport(
            BigDecimal expectedIncome,
            BigDecimal issuedIncome,
            BigDecimal receivedIncome,
            BigDecimal openIncome,
            BigDecimal canceledIncome,
            BigDecimal promoterExpenses,
            BigDecimal fixedExpenses,
            BigDecimal variableExpenses,
            BigDecimal totalExpenses,
            BigDecimal discounts,
            BigDecimal realResult,
            BigDecimal expectedResult
    ) {}

    public record IncomeReport(
            BigDecimal expectedIncome,
            BigDecimal issuedIncome,
            BigDecimal receivedIncome,
            BigDecimal openIncome,
            List<InvoiceLine> invoicesByDueDate,
            List<InvoiceLine> receivedInvoices
    ) {}

    public record ExpenseReport(
            BigDecimal promoterExpenses,
            BigDecimal fixedExpenses,
            BigDecimal variableExpenses,
            BigDecimal totalExpenses,
            BigDecimal discounts,
            List<PromoterFinanceLine> promoterPayments,
            List<PromoterFinanceLine> discountEntries,
            List<FixedExpenseHistory> fixedExpenseHistory,
            List<VariableExpense> variableExpenseList
    ) {}

    public record TypeReport(
            BigDecimal expectedIncome,
            BigDecimal issuedIncome,
            BigDecimal receivedIncome,
            BigDecimal openIncome,
            BigDecimal canceledIncome,
            Map<String, BigDecimal> incomeByCompany,
            Map<String, BigDecimal> expensesByType,
            BigDecimal fixedExpenses,
            BigDecimal variableExpenses
    ) {}

    public record InvoiceLine(
            Integer id,
            String clientName,
            String companyLink,
            BigDecimal amount,
            BigDecimal receivedAmount,
            String description,
            LocalDate dueDate,
            LocalDate issueDate,
            LocalDate paymentDate,
            String status
    ) {}

    public record PromoterFinanceLine(
            Integer id,
            String promoterName,
            String type,
            BigDecimal amount,
            LocalDate date,
            String status,
            String description
    ) {}
}