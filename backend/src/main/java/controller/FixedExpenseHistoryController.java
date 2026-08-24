package controller;

import dao.FixedExpenseHistoryDAO;
import model.FixedExpenseHistory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class FixedExpenseHistoryController {

    private FixedExpenseHistoryDAO dao = new FixedExpenseHistoryDAO();

    public void generateMonthlyExpenses(int month, int year) {

        if (month < 1 || month > 12) {
            System.out.println("Mês inválido.");
            return;
        }

        if (year < 2000) {
            System.out.println("Ano inválido.");
            return;
        }

        dao.generateMonthlyExpenses(month, year);
    }

    public void listByPeriod(LocalDate start, LocalDate end) {

        if (start.isAfter(end)) {
            System.out.println("Erro: data inicial maior que final.");
            return;
        }

        List<FixedExpenseHistory> list = dao.findByPeriod(start, end);

        if (list.isEmpty()) {
            System.out.println("Nenhuma despesa fixa nesse período.");
            return;
        }

        printList(list);
    }

    public void listByStatus(LocalDate start, LocalDate end, String status) {

        if (start.isAfter(end)) {
            System.out.println("Erro: data inicial maior que final.");
            return;
        }

        List<FixedExpenseHistory> list = dao.findByPeriod(start, end);

        boolean found = false;

        for (FixedExpenseHistory h : list) {
            if (h.getStatus() != null && h.getStatus().equalsIgnoreCase(status)) {
                printExpense(h);
                found = true;
            }
        }

        if (!found) {
            System.out.println("Nenhuma despesa encontrada com esse status.");
        }
    }

    public void markAsPaid(int id, LocalDate paymentDate) {
        dao.markAsPaid(id, paymentDate);
    }

    public void showTotalByPeriod(LocalDate start, LocalDate end) {

        if (start.isAfter(end)) {
            System.out.println("Erro: data inicial maior que final.");
            return;
        }

        List<FixedExpenseHistory> list = dao.findByPeriod(start, end);

        BigDecimal total = BigDecimal.ZERO;

        for (FixedExpenseHistory h : list) {
            total = total.add(h.getAmount());
        }

        System.out.println("Total de despesas fixas no período: R$ " + total);
    }

    private void printList(List<FixedExpenseHistory> list) {
        for (FixedExpenseHistory h : list) {
            printExpense(h);
        }
    }

    private void printExpense(FixedExpenseHistory h) {
        System.out.println(
                h.getId() + " | " +
                        h.getName() + " | " +
                        "R$ " + h.getAmount() + " | " +
                        "Vencimento: " + h.getDueDate() + " | " +
                        h.getStatus() + " | " +
                        "Pagamento: " + h.getPaymentDate()
        );
    }
}