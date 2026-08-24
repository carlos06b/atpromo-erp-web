package controller;

import dao.VariableExpenseDAO;
import model.VariableExpense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class VariableExpenseController {

    private final VariableExpenseDAO variableExpenseDAO = new VariableExpenseDAO();

    public void registerVariableExpense(String name,
                                        BigDecimal amount,
                                        LocalDate date,
                                        String description) {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome da despesa variável é obrigatório.");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da despesa deve ser maior que zero.");
        }

        if (date == null) {
            throw new IllegalArgumentException("A data da despesa é obrigatória.");
        }

        VariableExpense expense = new VariableExpense();
        expense.setName(name.trim());
        expense.setAmount(amount);
        expense.setDate(date);
        expense.setStatus(false);
        expense.setPaymentDate(null);
        expense.setDescription(description == null ? "" : description.trim());

        variableExpenseDAO.save(expense);
    }

    public List<VariableExpense> listByPeriod(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Informe a data inicial e final.");
        }

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("A data inicial não pode ser maior que a data final.");
        }

        return variableExpenseDAO.findByPeriod(start, end);
    }

    public void markAsPaid(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID da despesa inválido.");
        }

        variableExpenseDAO.markAsPaid(id, LocalDate.now());
    }

    public void delete(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID da despesa inválido.");
        }

        variableExpenseDAO.delete(id);
    }
}