package dao;

import database.ConnectionFactory;
import model.Client;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClientDAO {

    public void save(Client client) {
        String sql = """
        INSERT INTO client (corporate_name, name, cnpj, phone, email, active, company_link)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, client.getCorporateName());
            stmt.setString(2, client.getName());
            stmt.setString(3, client.getCnpj());
            stmt.setString(4, client.getPhone());
            stmt.setString(5, client.getEmail());
            stmt.setBoolean(6, client.isActive());
            stmt.setString(7, client.getCompanyLink());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao cadastrar cliente/indústria", e);
        }
    }

    public List<Client> findAll() {
        String sql = """
                SELECT *
                FROM client
                ORDER BY name
                """;

        List<Client> clients = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                clients.add(mapResultSetToClient(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar clientes/indústrias", e);
        }

        return clients;
    }

    public List<Client> findActiveClients() {
        String sql = """
                SELECT *
                FROM client
                WHERE active = 1
                ORDER BY name
                """;

        List<Client> clients = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                clients.add(mapResultSetToClient(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar clientes ativos", e);
        }

        return clients;
    }

    public List<Client> findInactiveClients() {
        String sql = """
                SELECT *
                FROM client
                WHERE active = 0
                ORDER BY name
                """;

        List<Client> clients = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                clients.add(mapResultSetToClient(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar clientes inativos", e);
        }

        return clients;
    }

    public Client findById(int id) {
        String sql = """
                SELECT *
                FROM client
                WHERE id = ?
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToClient(rs);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar cliente/indústria por ID", e);
        }

        return null;
    }

    public List<Client> searchByName(String name) {
        String sql = """
                SELECT *
                FROM client
                WHERE name LIKE ?
                ORDER BY name
                """;

        List<Client> clients = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + name + "%");

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                clients.add(mapResultSetToClient(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar cliente/indústria por nome", e);
        }

        return clients;
    }

    public List<Client> findByCompanyLink(String companyLink) {
        String sql = """
                SELECT *
                FROM client
                WHERE company_link = ?
                ORDER BY name
                """;

        List<Client> clients = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, companyLink);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                clients.add(mapResultSetToClient(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar clientes por vínculo", e);
        }

        return clients;
    }

    public void update(Client client) {
        String sql = """
                UPDATE client
                SET corporate_name = ?,
                    name = ?,
                    cnpj = ?,
                    phone = ?,
                    email = ?,
                    active = ?,
                    company_link = ?
                WHERE id = ?
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, client.getCorporateName());
            stmt.setString(2, client.getName());
            stmt.setString(3, client.getCnpj());
            stmt.setString(4, client.getPhone());
            stmt.setString(5, client.getEmail());
            stmt.setBoolean(6, client.isActive());
            stmt.setString(7, client.getCompanyLink());
            stmt.setInt(8, client.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar cliente/indústria", e);
        }
    }

    public void deactivate(int id) {
        String sql = """
                UPDATE client
                SET active = 0
                WHERE id = ?
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inativar cliente/indústria", e);
        }
    }

    public void activate(int id) {
        String sql = """
                UPDATE client
                SET active = 1
                WHERE id = ?
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao ativar cliente/indústria", e);
        }
    }

    private Client mapResultSetToClient(ResultSet rs) throws SQLException {
        Client client = new Client();

        client.setId(rs.getInt("id"));
        client.setName(rs.getString("name"));
        client.setCorporateName(rs.getString("corporate_name"));
        client.setCnpj(rs.getString("cnpj"));
        client.setPhone(rs.getString("phone"));
        client.setActive(rs.getBoolean("active"));
        client.setCompanyLink(rs.getString("company_link"));
        client.setEmail(rs.getString("email"));

        return client;
    }
}