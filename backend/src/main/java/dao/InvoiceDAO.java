package dao;

import database.ConnectionFactory;
import model.Invoice;
import model.InvoiceView;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvoiceDAO {

    public void save(Invoice invoice) {
        String sql = """
                INSERT INTO invoice (id_client, amount, description, due_date, status)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, invoice.getClientId());
            stmt.setBigDecimal(2, invoice.getAmount());
            stmt.setString(3, invoice.getDescription());
            stmt.setDate(4, Date.valueOf(invoice.getDueDate()));
            stmt.setString(5, "PENDENTE");

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao registrar faturamento pendente", e);
        }
    }

    public Invoice findById(int id) {
        String sql = "SELECT * FROM invoice WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToInvoice(rs);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar faturamento por ID", e);
        }

        return null;
    }

    public List<InvoiceView> findViewByPeriod(LocalDate start, LocalDate end) {
        String sql = """
                SELECT 
                    i.id,
                    c.name AS client_name,
                    c.company_link,
                    i.amount,
                    i.received_amount,
                    i.description,
                    i.due_date,
                    i.issue_date,
                    i.payment_date,
                    i.status
                FROM invoice i
                INNER JOIN client c ON i.id_client = c.id
                WHERE i.due_date BETWEEN ? AND ?
                ORDER BY i.due_date, c.name
                """;

        List<InvoiceView> invoices = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                invoices.add(mapResultSetToInvoiceView(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar faturamentos por período", e);
        }

        return invoices;
    }

    public List<InvoiceView> findViewByPaymentPeriod(LocalDate start, LocalDate end) {
        String sql = """
                SELECT 
                    i.id,
                    c.name AS client_name,
                    c.company_link,
                    i.amount,
                    i.received_amount,
                    i.description,
                    i.due_date,
                    i.issue_date,
                    i.payment_date,
                    i.status
                FROM invoice i
                INNER JOIN client c ON i.id_client = c.id
                WHERE i.payment_date BETWEEN ? AND ?
                  AND i.status = 'PAGO'
                ORDER BY i.payment_date, c.name
                """;

        List<InvoiceView> invoices = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                invoices.add(mapResultSetToInvoiceView(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar recebimentos por período", e);
        }

        return invoices;
    }

    public List<InvoiceView> findViewByFilters(LocalDate start, LocalDate end, String status, String companyLink) {
        StringBuilder sql = new StringBuilder("""
                SELECT 
                    i.id,
                    c.name AS client_name,
                    c.company_link,
                    i.amount,
                    i.received_amount,
                    i.description,
                    i.due_date,
                    i.issue_date,
                    i.payment_date,
                    i.status
                FROM invoice i
                INNER JOIN client c ON i.id_client = c.id
                WHERE i.due_date BETWEEN ? AND ?
                """);

        if (status != null && !status.equals("Todos")) {
            sql.append(" AND i.status = ? ");
        }

        if (companyLink != null && !companyLink.equals("Todos")) {
            sql.append(" AND c.company_link = ? ");
        }

        sql.append(" ORDER BY i.due_date, c.name ");

        List<InvoiceView> invoices = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int index = 1;

            stmt.setDate(index++, Date.valueOf(start));
            stmt.setDate(index++, Date.valueOf(end));

            if (status != null && !status.equals("Todos")) {
                stmt.setString(index++, status);
            }

            if (companyLink != null && !companyLink.equals("Todos")) {
                stmt.setString(index, companyLink);
            }

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                invoices.add(mapResultSetToInvoiceView(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao filtrar faturamentos", e);
        }

        return invoices;
    }

    public List<InvoiceView> findIssuedNotPaid() {
        String sql = """
                SELECT 
                    i.id,
                    c.name AS client_name,
                    c.company_link,
                    i.amount,
                    i.received_amount,
                    i.description,
                    i.due_date,
                    i.issue_date,
                    i.payment_date,
                    i.status
                FROM invoice i
                INNER JOIN client c ON i.id_client = c.id
                WHERE i.status = 'FATURADO'
                ORDER BY i.issue_date, c.name
                """;

        List<InvoiceView> invoices = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                invoices.add(mapResultSetToInvoiceView(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar faturamentos ainda não pagos", e);
        }

        return invoices;
    }

    public BigDecimal getTotalExpectedByDueDatePeriod(LocalDate start, LocalDate end) {
        String sql = """
                SELECT SUM(amount) AS total
                FROM invoice
                WHERE due_date BETWEEN ? AND ?
                  AND status <> 'CANCELADO'
                """;

        return executeTotalQuery(sql, start, end, "Erro ao calcular faturamento previsto");
    }

    public BigDecimal getTotalOpenByDueDatePeriod(LocalDate start, LocalDate end) {
        String sql = """
                SELECT SUM(amount) AS total
                FROM invoice
                WHERE due_date BETWEEN ? AND ?
                  AND status IN ('PENDENTE', 'FATURADO')
                """;

        return executeTotalQuery(sql, start, end, "Erro ao calcular valores a receber");
    }

    public BigDecimal getTotalIssuedByIssuePeriod(LocalDate start, LocalDate end) {
        String sql = """
                SELECT SUM(amount) AS total
                FROM invoice
                WHERE issue_date BETWEEN ? AND ?
                  AND status IN ('FATURADO', 'PAGO')
                """;

        return executeTotalQuery(sql, start, end, "Erro ao calcular faturamento emitido");
    }

    public BigDecimal getTotalPaidByPaymentPeriod(LocalDate start, LocalDate end) {
        String sql = """
                SELECT SUM(COALESCE(received_amount, amount)) AS total
                FROM invoice
                WHERE payment_date BETWEEN ? AND ?
                  AND status = 'PAGO'
                """;

        return executeTotalQuery(sql, start, end, "Erro ao calcular recebimentos");
    }

    public BigDecimal getTotalCanceledByDueDatePeriod(LocalDate start, LocalDate end) {
        String sql = """
                SELECT SUM(amount) AS total
                FROM invoice
                WHERE due_date BETWEEN ? AND ?
                  AND status = 'CANCELADO'
                """;

        return executeTotalQuery(sql, start, end, "Erro ao calcular faturamentos cancelados");
    }

    public Map<String, BigDecimal> getTotalByStatusAndDueDatePeriod(LocalDate start, LocalDate end) {
        String sql = """
                SELECT status, SUM(amount) AS total
                FROM invoice
                WHERE due_date BETWEEN ? AND ?
                GROUP BY status
                """;

        Map<String, BigDecimal> totals = new HashMap<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                BigDecimal total = rs.getBigDecimal("total");
                totals.put(rs.getString("status"), total != null ? total : BigDecimal.ZERO);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao calcular faturamento por status", e);
        }

        return totals;
    }

    public Map<String, BigDecimal> getTotalByCompanyAndDueDatePeriod(LocalDate start, LocalDate end) {
        String sql = """
                SELECT c.company_link, SUM(i.amount) AS total
                FROM invoice i
                INNER JOIN client c ON i.id_client = c.id
                WHERE i.due_date BETWEEN ? AND ?
                  AND i.status <> 'CANCELADO'
                GROUP BY c.company_link
                """;

        Map<String, BigDecimal> totals = new HashMap<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                BigDecimal total = rs.getBigDecimal("total");
                totals.put(rs.getString("company_link"), total != null ? total : BigDecimal.ZERO);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao calcular faturamento por vínculo", e);
        }

        return totals;
    }

    public void update(Invoice invoice) {
        String sql = """
                UPDATE invoice
                SET id_client = ?,
                    amount = ?,
                    description = ?,
                    due_date = ?
                WHERE id = ?
                AND status NOT IN ('PAGO', 'CANCELADO')
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, invoice.getClientId());
            stmt.setBigDecimal(2, invoice.getAmount());
            stmt.setString(3, invoice.getDescription());
            stmt.setDate(4, Date.valueOf(invoice.getDueDate()));
            stmt.setInt(5, invoice.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar faturamento", e);
        }
    }

    public void markAsIssued(int id, LocalDate issueDate) {
        String sql = """
                UPDATE invoice
                SET status = 'FATURADO',
                    issue_date = ?
                WHERE id = ?
                AND status = 'PENDENTE'
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(issueDate));
            stmt.setInt(2, id);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao marcar faturamento como emitido", e);
        }
    }

    public void markAsPaid(int id, LocalDate paymentDate, BigDecimal receivedAmount) {
        String sql = """
                UPDATE invoice
                SET status = 'PAGO',
                    payment_date = ?,
                    received_amount = ?
                WHERE id = ?
                AND status = 'FATURADO'
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(paymentDate));
            stmt.setBigDecimal(2, receivedAmount);
            stmt.setInt(3, id);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao marcar faturamento como pago", e);
        }
    }

    public void cancelInvoice(int id) {
        String sql = """
                UPDATE invoice
                SET status = 'CANCELADO'
                WHERE id = ?
                AND status != 'PAGO'
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao cancelar faturamento", e);
        }
    }

    private BigDecimal executeTotalQuery(String sql, LocalDate start, LocalDate end, String errorMessage) {
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                BigDecimal total = rs.getBigDecimal("total");
                return total != null ? total : BigDecimal.ZERO;
            }

        } catch (SQLException e) {
            throw new RuntimeException(errorMessage, e);
        }

        return BigDecimal.ZERO;
    }

    private Invoice mapResultSetToInvoice(ResultSet rs) throws SQLException {
        Invoice invoice = new Invoice();

        invoice.setId(rs.getInt("id"));
        invoice.setClientId(rs.getInt("id_client"));
        invoice.setAmount(rs.getBigDecimal("amount"));
        invoice.setReceivedAmount(rs.getBigDecimal("received_amount"));
        invoice.setDescription(rs.getString("description"));
        invoice.setDueDate(rs.getDate("due_date").toLocalDate());

        Date issueDate = rs.getDate("issue_date");
        if (issueDate != null) {
            invoice.setIssueDate(issueDate.toLocalDate());
        }

        Date paymentDate = rs.getDate("payment_date");
        if (paymentDate != null) {
            invoice.setPaymentDate(paymentDate.toLocalDate());
        }

        invoice.setStatus(rs.getString("status"));

        return invoice;
    }

    private InvoiceView mapResultSetToInvoiceView(ResultSet rs) throws SQLException {
        InvoiceView invoiceView = new InvoiceView();

        invoiceView.setId(rs.getInt("id"));
        invoiceView.setClientName(rs.getString("client_name"));
        invoiceView.setCompanyLink(rs.getString("company_link"));
        invoiceView.setAmount(rs.getBigDecimal("amount"));
        invoiceView.setReceivedAmount(rs.getBigDecimal("received_amount"));
        invoiceView.setDescription(rs.getString("description"));
        invoiceView.setDueDate(rs.getDate("due_date").toLocalDate());

        Date issueDate = rs.getDate("issue_date");
        if (issueDate != null) {
            invoiceView.setIssueDate(issueDate.toLocalDate());
        }

        Date paymentDate = rs.getDate("payment_date");
        if (paymentDate != null) {
            invoiceView.setPaymentDate(paymentDate.toLocalDate());
        }

        invoiceView.setStatus(rs.getString("status"));

        return invoiceView;
    }
}