package dao;

import database.ConnectionFactory;
import model.FixedExpense;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class FixedExpenseDAO {

    public void save(FixedExpense expense) {
        String sql = """
                INSERT INTO fixed_expense
                (name, description, amount, due_date, status, payment_date, active)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, expense.getName());
            stmt.setString(2, expense.getDescription());
            stmt.setBigDecimal(3, expense.getAmount());
            stmt.setDate(4, java.sql.Date.valueOf(expense.getDueDate()));
            stmt.setBoolean(5, expense.isStatus());

            if (expense.getPaymentDate() != null) {
                stmt.setDate(6, java.sql.Date.valueOf(expense.getPaymentDate()));
            } else {
                stmt.setNull(6, java.sql.Types.DATE);
            }

            stmt.setBoolean(7, true);

            stmt.executeUpdate();
            System.out.println("Despesa fixa cadastrada com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<FixedExpense> findAll() {
        List<FixedExpense> list = new ArrayList<>();

        String sql = """
                SELECT *
                FROM fixed_expense
                WHERE active = 1
                ORDER BY due_date ASC
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                FixedExpense expense = new FixedExpense();

                expense.setId(rs.getInt("id"));
                expense.setName(rs.getString("name"));
                expense.setAmount(rs.getBigDecimal("amount"));
                expense.setDueDate(rs.getDate("due_date").toLocalDate());
                expense.setStatus(rs.getBoolean("status"));
                expense.setDescription(rs.getString("description"));

                if (rs.getDate("payment_date") != null) {
                    expense.setPaymentDate(rs.getDate("payment_date").toLocalDate());
                } else {
                    expense.setPaymentDate(null);
                }

                list.add(expense);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public void markAsPaid(int id, java.time.LocalDate paymentDate) {
        String sql = """
                UPDATE fixed_expense
                SET status = true,
                    payment_date = ?
                WHERE id = ?
                  AND active = 1
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(paymentDate));
            stmt.setInt(2, id);

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                System.out.println("Despesa marcada como paga!");
            } else {
                System.out.println("Despesa não encontrada ou inativa.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void update(FixedExpense expense) {
        String sql = """
            UPDATE fixed_expense
            SET name = ?,
            description = ?,
            amount = ?,
            due_date = ?
            WHERE id = ?
            AND active = 1
            """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, expense.getName());
            stmt.setString(2, expense.getDescription());
            stmt.setBigDecimal(3, expense.getAmount());
            stmt.setDate(4, java.sql.Date.valueOf(expense.getDueDate()));
            stmt.setInt(5, expense.getId());

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateStatus(int id, boolean status) {
        String sql = """
                UPDATE fixed_expense
                SET status = ?
                WHERE id = ?
                  AND active = 1
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, status);
            stmt.setInt(2, id);

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                System.out.println("Status da despesa atualizado!");
            } else {
                System.out.println("Despesa não encontrada ou inativa.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void inactivate(int id) {
        String sql = """
                UPDATE fixed_expense
                SET active = 0
                WHERE id = ?
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                System.out.println("Despesa fixa inativada!");
            } else {
                System.out.println("Despesa não encontrada.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM fixed_expense WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                System.out.println("Despesa fixa excluída!");
            } else {
                System.out.println("Despesa não encontrada.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}