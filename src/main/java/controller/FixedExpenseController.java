package controller;

import dao.FixedExpenseDAO;
import model.FixedExpense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class FixedExpenseController {

    private FixedExpenseDAO dao = new FixedExpenseDAO();

    public void register(String name, BigDecimal amount, LocalDate dueDate) {

        FixedExpense expense = new FixedExpense();

        expense.setName(name);
        expense.setAmount(amount);
        expense.setDueDate(dueDate);
        expense.setStatus(false);
        expense.setPaymentDate(null);

        dao.save(expense);
    }

    public void listAll() {

        List<FixedExpense> list = dao.findAll();

        if (list.isEmpty()) {
            System.out.println("Nenhuma despesa fixa cadastrada.");
            return;
        }

        for (FixedExpense e : list) {
            printExpense(e);
        }
    }

    public void listByStatus(boolean status) {

        List<FixedExpense> list = dao.findAll();

        boolean found = false;

        for (FixedExpense e : list) {
            if (e.isStatus() == status) {
                printExpense(e);
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

    public void delete(int id) {
        dao.delete(id);
    }

    private void printExpense(FixedExpense e) {
        System.out.println(
                e.getId() + " | " +
                        e.getName() + " | " +
                        "R$ " + e.getAmount() + " | " +
                        "Vencimento: " + e.getDueDate() + " | " +
                        (e.isStatus() ? "PAGO" : "PENDENTE") + " | " +
                        "Pagamento: " + e.getPaymentDate()
        );
    }
}