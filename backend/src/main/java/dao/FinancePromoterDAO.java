package dao;

import database.ConnectionFactory;
import model.FinancePromoter;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class FinancePromoterDAO {

    public void save(FinancePromoter finance) {

        String sql = "INSERT INTO finance_promoter (id_promoter, type, amount, description, date, status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, finance.getIdPromoter());
            stmt.setString(2, finance.getType());
            stmt.setBigDecimal(3, finance.getAmount());
            stmt.setString(4, finance.getDescription());
            stmt.setDate(5, java.sql.Date.valueOf(finance.getDate()));
            stmt.setString(6, finance.getStatus());

            stmt.executeUpdate();

            System.out.println("Lançamento financeiro cadastrado com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateDiscount(int id, BigDecimal amount, String description, LocalDate date) {
        String sql = """
            UPDATE finance_promoter
            SET amount = ?,
                description = ?,
                date = ?
            WHERE id = ?
              AND type = 'DESCONTO'
            """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, amount);
            stmt.setString(2, description);
            stmt.setDate(3, java.sql.Date.valueOf(date));
            stmt.setInt(4, id);

            stmt.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar desconto.", e);
        }
    }

    public void deleteDiscount(int id) {
        String sql = """
            DELETE FROM finance_promoter
            WHERE id = ?
              AND type = 'DESCONTO'
            """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao excluir desconto.", e);
        }
    }

    public List<String> findDiscountsForPayroll(LocalDate start, LocalDate end) {
        List<String> list = new ArrayList<>();

        String sql = """
            SELECT fp.id, p.name AS promoter_name, fp.amount, fp.description, fp.date
            FROM finance_promoter fp
            JOIN promoter p ON fp.id_promoter = p.idpromoter
            WHERE fp.type = 'DESCONTO'
              AND fp.date BETWEEN ? AND ?
            ORDER BY fp.date DESC, p.name
            """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(start));
            stmt.setDate(2, java.sql.Date.valueOf(end));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String line =
                        rs.getInt("id") + " | " +
                                rs.getString("promoter_name") + " | " +
                                formatMoney(rs.getBigDecimal("amount")) + " | " +
                                formatDate(rs.getDate("date").toLocalDate()) + " | " +
                                safe(rs.getString("description"));

                list.add(line);
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao listar descontos da folha.", e);
        }

        return list;
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    public List<FinancePromoter> findAll() {

        List<FinancePromoter> list = new ArrayList<>();

        String sql = "SELECT * FROM finance_promoter";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(buildFinancePromoter(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<FinancePromoter> findByPeriod(LocalDate start, LocalDate end) {

        List<FinancePromoter> list = new ArrayList<>();

        String sql = "SELECT * FROM finance_promoter WHERE date BETWEEN ? AND ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(start));
            stmt.setDate(2, java.sql.Date.valueOf(end));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(buildFinancePromoter(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<FinancePromoter> findByPromoterAndPeriod(int idPromoter, LocalDate start, LocalDate end) {

        List<FinancePromoter> list = new ArrayList<>();

        String sql = "SELECT * FROM finance_promoter " +
                "WHERE id_promoter = ? AND date BETWEEN ? AND ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idPromoter);
            stmt.setDate(2, java.sql.Date.valueOf(start));
            stmt.setDate(3, java.sql.Date.valueOf(end));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(buildFinancePromoter(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<String> findByPeriodWithPromoterName(LocalDate start, LocalDate end) {

        List<String> list = new ArrayList<>();

        String sql = "SELECT fp.id, p.name AS promoter_name, fp.type, fp.amount, fp.date, fp.status " +
                "FROM finance_promoter fp " +
                "JOIN promoter p ON fp.id_promoter = p.idpromoter " +
                "WHERE fp.date BETWEEN ? AND ? " +
                "AND fp.type <> 'DESCONTO' " +
                "ORDER BY fp.date DESC, p.name";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(start));
            stmt.setDate(2, java.sql.Date.valueOf(end));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String line = String.format(
                        "%-30s | %-24s | %12s | %-10s | %s",
                        limit(rs.getString("promoter_name"), 30),
                        formatType(rs.getString("type")),
                        formatMoney(rs.getBigDecimal("amount")),
                        formatDate(rs.getDate("date").toLocalDate()),
                        rs.getString("status")
                );

                list.add(line);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public BigDecimal getTotalByPeriod(LocalDate start, LocalDate end) {

        String sql = "SELECT SUM(amount) AS total FROM finance_promoter WHERE date BETWEEN ? AND ?";

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

    public List<String> findDiscountsByPeriodWithPromoterName(LocalDate start, LocalDate end) {

        List<String> list = new ArrayList<>();

        String sql = "SELECT fp.id, p.name AS promoter_name, fp.amount, fp.date, fp.status " +
                "FROM finance_promoter fp " +
                "JOIN promoter p ON fp.id_promoter = p.idpromoter " +
                "WHERE fp.date BETWEEN ? AND ? " +
                "AND fp.type = 'DESCONTO' " +
                "ORDER BY fp.date DESC, p.name";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(start));
            stmt.setDate(2, java.sql.Date.valueOf(end));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String line = String.format(
                        "%-30s | %12s | %-10s | %s",
                        limit(rs.getString("promoter_name"), 30),
                        formatMoney(rs.getBigDecimal("amount")),
                        formatDate(rs.getDate("date").toLocalDate()),
                        "APLICADO"
                );

                list.add(line);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public Map<String, BigDecimal> getTotalByTypeAndPeriod(LocalDate start, LocalDate end) {

        Map<String, BigDecimal> totals = new HashMap<>();

        String sql = "SELECT type, SUM(amount) AS total " +
                "FROM finance_promoter " +
                "WHERE date BETWEEN ? AND ? " +
                "GROUP BY type";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(start));
            stmt.setDate(2, java.sql.Date.valueOf(end));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                totals.put(rs.getString("type"), rs.getBigDecimal("total"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return totals;
    }

    private FinancePromoter buildFinancePromoter(ResultSet rs) throws Exception {

        FinancePromoter finance = new FinancePromoter();

        finance.setId(rs.getInt("id"));
        finance.setIdPromoter(rs.getInt("id_promoter"));
        finance.setType(rs.getString("type"));
        finance.setAmount(rs.getBigDecimal("amount"));
        finance.setDescription(rs.getString("description"));
        finance.setDate(rs.getDate("date").toLocalDate());
        finance.setStatus(rs.getString("status"));

        return finance;
    }

    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "R$ 0,00";
        }

        NumberFormat moneyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        return moneyFormat.format(value.setScale(2, RoundingMode.HALF_UP))
                .replace('\u00A0', ' ');
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength - 3) + "...";
    }

    private String formatType(String type) {
        return switch (type) {
            case "BONIFICACAO" -> "Bonificação";
            case "AJUDA_CUSTO" -> "Ajuda de Custo";
            case "DESCONTO" -> "Desconto";
            case "ASO" -> "ASO";
            case "EPI" -> "EPI";
            case "RESCISAO" -> "Rescisão";
            case "FERIAS" -> "Férias";
            case "ADIANTAMENTO" -> "Adiantamento";
            case "REEMBOLSO" -> "Reembolso";
            case "CORRECAO_PAGAMENTO" -> "Correção de Pagamento";
            case "OUTROS" -> "Outros";
            default -> type;
        };
    }
}