package dao;

import database.ConnectionFactory;
import model.Request;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RequestDAO {

    public void save(Request request) {

        String sql = "INSERT INTO request (id_userRH, id_userFIN, id_promoter, type, amount, message, status, date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, request.getId_UserRH());
            if (request.getId_UserFin() <= 0) {
                stmt.setNull(2, java.sql.Types.INTEGER);
            } else {
                stmt.setInt(2, request.getId_UserFin());
            }
            stmt.setInt(3, request.getId_Promoter());
            stmt.setString(4, request.getType());
            stmt.setBigDecimal(5, request.getAmount());
            stmt.setString(6, request.getMessage());
            stmt.setString(7, request.getStatus());
            stmt.setTimestamp(8, Timestamp.valueOf(request.getDate()));

            stmt.executeUpdate();

            System.out.println("Solicitação cadastrada com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Request> findAll() {

        List<Request> list = new ArrayList<>();

        String sql = "SELECT * FROM request";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(buildRequest(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<Request> findByStatus(String status) {

        List<Request> list = new ArrayList<>();

        String sql = "SELECT * FROM request WHERE status = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(buildRequest(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<Request> findByPeriod(LocalDateTime start, LocalDateTime end) {

        List<Request> list = new ArrayList<>();

        String sql = "SELECT * FROM request WHERE date BETWEEN ? AND ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(start));
            stmt.setTimestamp(2, Timestamp.valueOf(end));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(buildRequest(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<String> findAllWithPromoterName() {

        List<String> list = new ArrayList<>();

        String sql = "SELECT r.id, p.name AS promoter_name, p.company_link, r.type, r.amount, r.message, r.status, r.date " +
                "FROM request r " +
                "JOIN promoter p ON r.id_promoter = p.idpromoter " +
                "ORDER BY r.date DESC";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(buildRequestLineWithPromoterName(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<String> findPendingWithPromoterName() {

        List<String> list = new ArrayList<>();

        String sql = "SELECT r.id, p.name AS promoter_name, p.company_link, r.type, r.amount, r.message, r.status, r.date " +
                "FROM request r " +
                "JOIN promoter p ON r.id_promoter = p.idpromoter " +
                "WHERE r.status = 'PENDENTE' " +
                "ORDER BY r.date DESC";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(buildRequestLineWithPromoterName(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<String> findByPeriodWithPromoterName(LocalDateTime start, LocalDateTime end) {

        List<String> list = new ArrayList<>();

        String sql = "SELECT r.id, p.name AS promoter_name, p.company_link, r.type, r.amount, r.message, r.status, r.date " +
                "FROM request r " +
                "JOIN promoter p ON r.id_promoter = p.idpromoter " +
                "WHERE r.date BETWEEN ? AND ? " +
                "ORDER BY r.date DESC";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(start));
            stmt.setTimestamp(2, Timestamp.valueOf(end));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(buildRequestLineWithPromoterName(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public void updateStatus(int id, String status) {

        String sql = "UPDATE request SET status = ? WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setInt(2, id);

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                System.out.println("Status atualizado!");
            } else {
                System.out.println("Solicitação não encontrada.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete(int id) {

        String sql = "DELETE FROM request WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                System.out.println("Solicitação excluída!");
            } else {
                System.out.println("Solicitação não encontrada.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Request buildRequest(ResultSet rs) throws Exception {

        Request r = new Request();

        r.setId(rs.getInt("id"));
        r.setId_UserRH(rs.getInt("id_userRH"));
        r.setId_UserFin(rs.getInt("id_userFIN"));
        r.setId_Promoter(rs.getInt("id_promoter"));
        r.setType(rs.getString("type"));
        r.setAmount(rs.getBigDecimal("amount"));
        r.setMessage(rs.getString("message"));
        r.setStatus(rs.getString("status"));
        r.setDate(rs.getTimestamp("date").toLocalDateTime());

        return r;
    }

    public void updateFinanceUser(int id, int idUserFin) {

        String sql = "UPDATE request SET id_userFIN = ? WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idUserFin);
            stmt.setInt(2, id);

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildRequestLineWithPromoterName(ResultSet rs) throws Exception {

        return rs.getInt("id") + " | " +
                "Promotor: " + rs.getString("promoter_name") + " | " +
                "Vínculo: " + rs.getString("company_link") + " | " +
                rs.getString("type") + " | " +
                "R$ " + rs.getBigDecimal("amount") + " | " +
                rs.getString("message") + " | " +
                rs.getString("status") + " | " +
                rs.getTimestamp("date").toLocalDateTime();
    }
}