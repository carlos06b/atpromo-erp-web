package dao;

import database.ConnectionFactory;
import model.Promoter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PromoterDAO {

    public int save(Promoter promoter) {

        String sql = """
                INSERT INTO promoter
                (name, cpf, phone, uf, city, company_link, date_birth, active, salary, type, pix, pix_type)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, promoter.getName());
            stmt.setString(2, promoter.getCpf());
            stmt.setString(3, promoter.getPhone());
            stmt.setString(4, promoter.getUf());
            stmt.setString(5, promoter.getCity());
            stmt.setString(6, promoter.getCompanyLink());
            stmt.setDate(7, java.sql.Date.valueOf(promoter.getDateBirth()));
            stmt.setBoolean(8, promoter.isActive());
            stmt.setBigDecimal(9, promoter.getSalary());
            stmt.setString(10, promoter.getType());
            stmt.setString(11, promoter.getPix());
            stmt.setString(12, promoter.getPixType());

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();

            if (rs.next()) {
                int id = rs.getInt(1);
                promoter.setId(id);
                return id;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    public List<Promoter> findAll() {

        List<Promoter> list = new ArrayList<>();

        String sql = "SELECT * FROM promoter";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(buildPromoter(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<Promoter> findByType(String type) {

        List<Promoter> list = new ArrayList<>();

        String sql = "SELECT * FROM promoter WHERE type = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, type);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(buildPromoter(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public Promoter findById(int id) {

        String sql = "SELECT * FROM promoter WHERE idpromoter = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return buildPromoter(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public Promoter findByCpf(String cpf) {

        String sql = "SELECT * FROM promoter WHERE cpf = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cpf);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return buildPromoter(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<Promoter> findByNameIncludingInactive(String name) {

        List<Promoter> list = new ArrayList<>();

        String sql = """
            SELECT *
            FROM promoter
            WHERE name LIKE ?
            ORDER BY active DESC, name
            """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + name + "%");

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(buildPromoter(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<Promoter> findByName(String name) {

        List<Promoter> list = new ArrayList<>();

        String sql = "SELECT * FROM promoter WHERE name LIKE ? AND active = true";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + name + "%");

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(buildPromoter(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public void update(Promoter promoter) {

        String sql = """
                SET name = ?,
                cpf = ?,
                phone = ?,
                uf = ?,
                city = ?,
                company_link = ?,
                date_birth = ?,
                active = ?,
                salary = ?,
                type = ?,
                pix = ?,
                pix_type = ?
                WHERE idpromoter = ?
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, promoter.getName());
            stmt.setString(2, promoter.getCpf());
            stmt.setString(3, promoter.getPhone());
            stmt.setString(4, promoter.getUf());
            stmt.setString(5, promoter.getCity());
            stmt.setString(6, promoter.getCompanyLink());
            stmt.setDate(7, java.sql.Date.valueOf(promoter.getDateBirth()));
            stmt.setBoolean(8, promoter.isActive());
            stmt.setBigDecimal(9, promoter.getSalary());
            stmt.setString(10, promoter.getType());
            stmt.setString(11, promoter.getPix());
            stmt.setString(12, promoter.getPixType());
            stmt.setInt(13, promoter.getId());

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                System.out.println("Promotor atualizado com sucesso!");
            } else {
                System.out.println("Promotor não encontrado.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void inactivate(int id) {

        String sql = "UPDATE promoter SET active = false WHERE idpromoter = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                System.out.println("Promotor inativado com sucesso!");
            } else {
                System.out.println("Promotor não encontrado.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Promoter buildPromoter(ResultSet rs) throws Exception {

        Promoter p = new Promoter();

        p.setId(rs.getInt("idpromoter"));
        p.setName(rs.getString("name"));
        p.setCpf(rs.getString("cpf"));
        p.setPhone(rs.getString("phone"));
        p.setUf(rs.getString("uf"));
        p.setCity(rs.getString("city"));
        p.setCompanyLink(rs.getString("company_link"));
        p.setDateBirth(rs.getDate("date_birth").toLocalDate());
        p.setActive(rs.getBoolean("active"));
        p.setSalary(rs.getBigDecimal("salary"));
        p.setType(rs.getString("type"));
        p.setPix(rs.getString("pix"));
        p.setPixType(rs.getString("pix_type"));

        return p;
    }
}