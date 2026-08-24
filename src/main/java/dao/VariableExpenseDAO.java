package dao;

import database.ConnectionFactory;
import model.VariableExpense;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VariableExpenseDAO {

    public void save(VariableExpense expense) {
        String sql = """
        INSERT INTO variable_expense
        (name, amount, date, status, payment_date, description,
         installment_group, installment_number, total_installments)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, expense.getName());
            stmt.setBigDecimal(2, expense.getAmount());
            stmt.setDate(3, java.sql.Date.valueOf(expense.getDate()));
            stmt.setBoolean(4, expense.isStatus());

            if (expense.getPaymentDate() != null) {
                stmt.setDate(5, java.sql.Date.valueOf(expense.getPaymentDate()));
            } else {
                stmt.setNull(5, java.sql.Types.DATE);
            }

            stmt.setString(6, expense.getDescription());

            stmt.setString(7, expense.getInstallmentGroup());

            if (expense.getInstallmentNumber() != null) {
                stmt.setInt(8, expense.getInstallmentNumber());
            } else {
                stmt.setNull(8, java.sql.Types.INTEGER);
            }

            if (expense.getTotalInstallments() != null) {
                stmt.setInt(9, expense.getTotalInstallments());
            } else {
                stmt.setNull(9, java.sql.Types.INTEGER);
            }

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<VariableExpense> findByPeriod(LocalDate start, LocalDate end) {
        List<VariableExpense> expenses = new ArrayList<>();

        String sql = """
                SELECT *
                FROM variable_expense
                WHERE date BETWEEN ? AND ?
                ORDER BY date DESC
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(start));
            stmt.setDate(2, java.sql.Date.valueOf(end));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                VariableExpense expense = new VariableExpense();

                expense.setId(rs.getInt("id"));
                expense.setName(rs.getString("name"));
                expense.setAmount(rs.getBigDecimal("amount"));
                expense.setDate(rs.getDate("date").toLocalDate());
                expense.setStatus(rs.getBoolean("status"));

                if (rs.getDate("payment_date") != null) {
                    expense.setPaymentDate(rs.getDate("payment_date").toLocalDate());
                } else {
                    expense.setPaymentDate(null);
                }

                expense.setDescription(rs.getString("description"));

                expense.setInstallmentGroup(rs.getString("installment_group"));

                int installmentNumber = rs.getInt("installment_number");
                if (rs.wasNull()) {
                    expense.setInstallmentNumber(null);
                } else {
                    expense.setInstallmentNumber(installmentNumber);
                }

                int totalInstallments = rs.getInt("total_installments");
                if (rs.wasNull()) {
                    expense.setTotalInstallments(null);
                } else {
                    expense.setTotalInstallments(totalInstallments);
                }

                expenses.add(expense);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return expenses;
    }

    public BigDecimal getTotalByPeriod(LocalDate start, LocalDate end) {
        String sql = """
                SELECT SUM(amount) AS total
                FROM variable_expense
                WHERE date BETWEEN ? AND ?
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(start));
            stmt.setDate(2, java.sql.Date.valueOf(end));

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                BigDecimal total = rs.getBigDecimal("total");
                return total != null ? total : BigDecimal.ZERO;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return BigDecimal.ZERO;
    }

    public void markAsPaid(int id, LocalDate paymentDate) {
        String sql = """
                UPDATE variable_expense
                SET status = ?, payment_date = ?
                WHERE id = ?
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, true);
            stmt.setDate(2, java.sql.Date.valueOf(paymentDate));
            stmt.setInt(3, id);

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM variable_expense WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}