package controller;

import dao.FinancePromoterDAO;
import dao.FixedExpenseHistoryDAO;
import dao.InvoiceDAO;
import dao.VariableExpenseDAO;
import model.FixedExpenseHistory;
import model.InvoiceView;
import model.VariableExpense;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class ReportController {

    private final FinancePromoterDAO financePromoterDAO = new FinancePromoterDAO();
    private final FixedExpenseHistoryDAO fixedExpenseHistoryDAO = new FixedExpenseHistoryDAO();
    private final VariableExpenseDAO variableExpenseDAO = new VariableExpenseDAO();
    private final InvoiceDAO invoiceDAO = new InvoiceDAO();

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void showGeneralReport(LocalDate start, LocalDate end) {
        System.out.println(buildGeneralReport(start, end));
    }

    public String buildGeneralReport(LocalDate start, LocalDate end) {
        validatePeriod(start, end);

        BigDecimal expectedIncome = invoiceDAO.getTotalExpectedByDueDatePeriod(start, end);
        BigDecimal issuedIncome = invoiceDAO.getTotalIssuedByIssuePeriod(start, end);
        BigDecimal receivedIncome = invoiceDAO.getTotalPaidByPaymentPeriod(start, end);
        BigDecimal openIncome = invoiceDAO.getTotalOpenByDueDatePeriod(start, end);
        BigDecimal canceledIncome = invoiceDAO.getTotalCanceledByDueDatePeriod(start, end);

        Map<String, BigDecimal> promoterTotals = financePromoterDAO.getTotalByTypeAndPeriod(start, end);

        BigDecimal promoterExpenses = calculatePromoterExpenses(promoterTotals);
        BigDecimal fixedExpenses = calculateFixedExpenses(start, end);
        BigDecimal variableExpenses = calculateVariableExpenses(start, end);
        BigDecimal discounts = promoterTotals.getOrDefault("DESCONTO", BigDecimal.ZERO);

        BigDecimal totalExpenses = promoterExpenses
                .add(fixedExpenses)
                .add(variableExpenses);

        BigDecimal realResult = receivedIncome.subtract(totalExpenses);
        BigDecimal expectedResult = expectedIncome.subtract(totalExpenses);

        StringBuilder report = new StringBuilder();

        appendHeader(report, "VISÃO GERAL FINANCEIRA", start, end);

        appendSection(report, "ENTRADAS E RECEBIMENTOS");
        appendMetric(report, "Faturamento previsto no período", expectedIncome);
        appendMetric(report, "Faturamento emitido no período", issuedIncome);
        appendMetric(report, "Recebido no período", receivedIncome);
        appendMetric(report, "A receber no período", openIncome);
        appendMetric(report, "Faturamento cancelado", canceledIncome);

        appendSection(report, "SAÍDAS E COMPROMISSOS");
        appendMetric(report, "Financeiro de promotores", promoterExpenses);
        appendMetric(report, "Despesas fixas", fixedExpenses);
        appendMetric(report, "Despesas variáveis", variableExpenses);
        appendMetric(report, "Total de saídas", totalExpenses);
        appendMetric(report, "Descontos aplicados", discounts);

        appendSection(report, "RESULTADO");
        appendMetric(report, "Resultado por recebimento", realResult);
        appendMetric(report, "Resultado previsto", expectedResult);

        report.append("\nLeitura rápida:\n");
        report.append("- Resultado por recebimento = recebido no período menos saídas.\n");
        report.append("- Resultado previsto = faturamento previsto menos saídas.\n");
        report.append("- A receber considera faturamentos pendentes ou faturados ainda não pagos.\n");

        return report.toString();
    }

    public String buildIncomeReport(LocalDate start, LocalDate end) {
        validatePeriod(start, end);

        List<InvoiceView> invoicesByDueDate = invoiceDAO.findViewByPeriod(start, end);
        List<InvoiceView> receivedInvoices = invoiceDAO.findViewByPaymentPeriod(start, end);

        BigDecimal expectedIncome = invoiceDAO.getTotalExpectedByDueDatePeriod(start, end);
        BigDecimal issuedIncome = invoiceDAO.getTotalIssuedByIssuePeriod(start, end);
        BigDecimal receivedIncome = invoiceDAO.getTotalPaidByPaymentPeriod(start, end);
        BigDecimal openIncome = invoiceDAO.getTotalOpenByDueDatePeriod(start, end);

        StringBuilder report = new StringBuilder();

        appendHeader(report, "ENTRADAS E RECEBIMENTOS", start, end);

        appendSection(report, "RESUMO");
        appendMetric(report, "Previsto por vencimento", expectedIncome);
        appendMetric(report, "Emitido no período", issuedIncome);
        appendMetric(report, "Recebido no período", receivedIncome);
        appendMetric(report, "Ainda a receber", openIncome);

        appendSection(report, "FATURAMENTOS COM VENCIMENTO NO PERÍODO");
        appendInvoiceList(report, invoicesByDueDate);

        appendSection(report, "RECEBIMENTOS CONFIRMADOS NO PERÍODO");
        appendInvoiceList(report, receivedInvoices);

        return report.toString();
    }

    public String buildExpenseReport(LocalDate start, LocalDate end) {
        validatePeriod(start, end);

        List<String> promoterLines = financePromoterDAO.findByPeriodWithPromoterName(start, end);
        List<String> discountLines = financePromoterDAO.findDiscountsByPeriodWithPromoterName(start, end);
        List<FixedExpenseHistory> fixedExpenses = fixedExpenseHistoryDAO.findByPeriod(start, end);
        List<VariableExpense> variableExpenses = variableExpenseDAO.findByPeriod(start, end);

        Map<String, BigDecimal> promoterTotals = financePromoterDAO.getTotalByTypeAndPeriod(start, end);

        BigDecimal promoterExpenses = calculatePromoterExpenses(promoterTotals);
        BigDecimal fixedTotal = calculateFixedExpenses(start, end);
        BigDecimal variableTotal = calculateVariableExpenses(start, end);
        BigDecimal discounts = promoterTotals.getOrDefault("DESCONTO", BigDecimal.ZERO);
        BigDecimal totalExpenses = promoterExpenses.add(fixedTotal).add(variableTotal);

        StringBuilder report = new StringBuilder();

        appendHeader(report, "SAÍDAS E GASTOS", start, end);

        appendSection(report, "RESUMO");
        appendMetric(report, "Promotores", promoterExpenses);
        appendMetric(report, "Despesas fixas", fixedTotal);
        appendMetric(report, "Despesas variáveis", variableTotal);
        appendMetric(report, "Total de saídas", totalExpenses);
        appendMetric(report, "Descontos aplicados", discounts);

        appendSection(report, "PAGAMENTOS A PROMOTORES");
        if (promoterLines.isEmpty()) {
            report.append("Nenhum pagamento de promotor encontrado.\n");
        } else {
            for (String line : promoterLines) {
                report.append("- ").append(line).append("\n");
            }
        }

        appendSection(report, "DESCONTOS APLICADOS NA FOLHA");
        if (discountLines.isEmpty()) {
            report.append("Nenhum desconto aplicado encontrado.\n");
        } else {
            for (String line : discountLines) {
                report.append("- ").append(line).append("\n");
            }
        }

        appendSection(report, "DESPESAS FIXAS");
        if (fixedExpenses.isEmpty()) {
            report.append("Nenhuma despesa fixa encontrada.\n");
        } else {
            for (FixedExpenseHistory expense : fixedExpenses) {
                report.append(String.format(
                        "- %-28s %12s | Vencimento: %-10s | Status: %s%n",
                        limit(expense.getName(), 28),
                        formatMoney(expense.getAmount()),
                        formatDate(expense.getDueDate()),
                        safe(expense.getStatus())
                ));
            }
        }

        appendSection(report, "DESPESAS VARIÁVEIS");
        if (variableExpenses.isEmpty()) {
            report.append("Nenhuma despesa variável encontrada.\n");
        } else {
            for (VariableExpense expense : variableExpenses) {
                report.append(String.format(
                        "- %-28s %12s | Data: %-10s | Status: %s%n",
                        limit(expense.getName(), 28),
                        formatMoney(expense.getAmount()),
                        formatDate(expense.getDate()),
                        expense.isStatus() ? "PAGO" : "PENDENTE"
                ));
            }
        }

        return report.toString();
    }

    public String buildTypeReport(LocalDate start, LocalDate end) {
        validatePeriod(start, end);

        BigDecimal expectedIncome = invoiceDAO.getTotalExpectedByDueDatePeriod(start, end);
        BigDecimal issuedIncome = invoiceDAO.getTotalIssuedByIssuePeriod(start, end);
        BigDecimal receivedIncome = invoiceDAO.getTotalPaidByPaymentPeriod(start, end);
        BigDecimal openIncome = invoiceDAO.getTotalOpenByDueDatePeriod(start, end);
        BigDecimal canceledIncome = invoiceDAO.getTotalCanceledByDueDatePeriod(start, end);

        Map<String, BigDecimal> promoterTotals = financePromoterDAO.getTotalByTypeAndPeriod(start, end);
        Map<String, BigDecimal> companyTotals = invoiceDAO.getTotalByCompanyAndDueDatePeriod(start, end);

        StringBuilder report = new StringBuilder();

        appendHeader(report, "VISÃO ESPECÍFICA POR TIPO", start, end);

        appendSection(report, "ENTRADAS E RECEBIMENTOS");
        appendMetric(report, "Previsto por vencimento", expectedIncome);
        appendMetric(report, "Faturado no período", issuedIncome);
        appendMetric(report, "Recebido no período", receivedIncome);
        appendMetric(report, "A receber", openIncome);
        appendMetric(report, "Cancelado", canceledIncome);

        appendSection(report, "ENTRADAS POR VÍNCULO");
        appendMetric(report, "AT", companyTotals.getOrDefault("AT", BigDecimal.ZERO));
        appendMetric(report, "TEJO", companyTotals.getOrDefault("TEJO", BigDecimal.ZERO));

        appendSection(report, "SAÍDAS POR TIPO");
        appendMetric(report, "Bonificação", promoterTotals.getOrDefault("BONIFICACAO", BigDecimal.ZERO));
        appendMetric(report, "Ajuda de custo", promoterTotals.getOrDefault("AJUDA_CUSTO", BigDecimal.ZERO));
        appendMetric(report, "ASO", promoterTotals.getOrDefault("ASO", BigDecimal.ZERO));
        appendMetric(report, "EPI", promoterTotals.getOrDefault("EPI", BigDecimal.ZERO));
        appendMetric(report, "Rescisão", promoterTotals.getOrDefault("RESCISAO", BigDecimal.ZERO));
        appendMetric(report, "Férias", promoterTotals.getOrDefault("FERIAS", BigDecimal.ZERO));
        appendMetric(report, "Adiantamento", promoterTotals.getOrDefault("ADIANTAMENTO", BigDecimal.ZERO));
        appendMetric(report, "Reembolso", promoterTotals.getOrDefault("REEMBOLSO", BigDecimal.ZERO));
        appendMetric(report, "Correção de pagamento", promoterTotals.getOrDefault("CORRECAO_PAGAMENTO", BigDecimal.ZERO));
        appendMetric(report, "Outros", promoterTotals.getOrDefault("OUTROS", BigDecimal.ZERO));
        appendMetric(report, "Desconto", promoterTotals.getOrDefault("DESCONTO", BigDecimal.ZERO));

        appendSection(report, "OUTRAS SAÍDAS");
        appendMetric(report, "Despesas fixas", calculateFixedExpenses(start, end));
        appendMetric(report, "Despesas variáveis", calculateVariableExpenses(start, end));

        return report.toString();
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

        for (FixedExpenseHistory expense : fixedExpenseHistoryDAO.findByPeriod(start, end)) {
            if ("PAGO".equalsIgnoreCase(safe(expense.getStatus()))) {
                total = total.add(nullToZero(expense.getAmount()));
            }
        }

        return total;
    }

    private BigDecimal calculateVariableExpenses(LocalDate start, LocalDate end) {
        BigDecimal total = BigDecimal.ZERO;

        for (VariableExpense expense : variableExpenseDAO.findByPeriod(start, end)) {
            if (expense.isStatus()) {
                total = total.add(nullToZero(expense.getAmount()));
            }
        }

        return total;
    }

    private void appendHeader(StringBuilder report, String title, LocalDate start, LocalDate end) {
        report.append("============================================================\n");
        report.append(centerText(title, 60)).append("\n");
        report.append("============================================================\n\n");
        report.append("Período analisado : ")
                .append(formatDate(start))
                .append(" até ")
                .append(formatDate(end))
                .append("\n");
        report.append("Gerado em         : ")
                .append(formatDate(LocalDate.now()))
                .append("\n");
    }

    private void appendSection(StringBuilder report, String title) {
        report.append("\n------------------------------------------------------------\n");
        report.append(title).append("\n");
        report.append("------------------------------------------------------------\n");
    }

    private void appendMetric(StringBuilder report, String label, BigDecimal value) {
        report.append(String.format("%-36s %15s%n", label + ":", formatMoney(value)));
    }

    private void appendInvoiceList(StringBuilder report, List<InvoiceView> invoices) {
        if (invoices.isEmpty()) {
            report.append("Nenhum faturamento encontrado.\n");
            return;
        }

        report.append(String.format(
                "%-5s %-24s %-6s %12s %-11s %-11s %-11s %-10s%n",
                "ID", "Cliente", "Vínc.", "Valor", "Previsto", "Faturado", "Pago", "Status"
        ));

        report.append("------------------------------------------------------------------------------------------\n");

        for (InvoiceView invoice : invoices) {
            report.append(String.format(
                    "%-5d %-24s %-6s %12s %-11s %-11s %-11s %-10s%n",
                    invoice.getId(),
                    limit(invoice.getClientName(), 24),
                    safe(invoice.getCompanyLink()),
                    formatMoney(invoice.getAmount()),
                    formatDate(invoice.getDueDate()),
                    formatDate(invoice.getIssueDate()),
                    formatDate(invoice.getPaymentDate()),
                    safe(invoice.getStatus())
            ));
        }
    }

    private void validatePeriod(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new RuntimeException("Informe a data inicial e final.");
        }

        if (start.isAfter(end)) {
            throw new RuntimeException("Data inicial não pode ser maior que a final.");
        }
    }

    private String formatMoney(BigDecimal value) {
        BigDecimal safeValue = nullToZero(value).setScale(2, RoundingMode.HALF_UP);

        NumberFormat moneyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        return moneyFormat.format(safeValue)
                .replace('\u00A0', ' ');
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(formatter) : "-";
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private String limit(String value, int maxLength) {
        String safeValue = safe(value);

        if (safeValue.length() <= maxLength) {
            return safeValue;
        }

        return safeValue.substring(0, maxLength - 3) + "...";
    }

    private String centerText(String text, int width) {
        if (text.length() >= width) {
            return text;
        }

        int leftPadding = (width - text.length()) / 2;
        return " ".repeat(leftPadding) + text;
    }
}