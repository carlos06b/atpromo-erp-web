package controller;

import dao.ClientDAO;
import model.Client;

import java.util.List;

public class ClientController {

    private final ClientDAO clientDAO;

    public ClientController() {
        this.clientDAO = new ClientDAO();
    }

    public void registerClient(String corporateName, String fantasyName, String cnpj, String phone, String email, String companyLink) {
        if (corporateName == null || corporateName.trim().isEmpty()) {
            throw new RuntimeException("A razão social é obrigatória.");
        }

        if (fantasyName == null || fantasyName.trim().isEmpty()) {
            throw new RuntimeException("O nome fantasia é obrigatório.");
        }

        Client client = new Client();
        client.setCorporateName(corporateName.trim());
        client.setName(fantasyName.trim());
        client.setCnpj(formatNullable(cnpj));
        client.setPhone(formatNullable(phone));
        client.setEmail(formatNullable(email));
        client.setCompanyLink(companyLink);
        client.setActive(true);

        clientDAO.save(client);
    }

    public List<Client> listAll() {
        return clientDAO.findAll();
    }

    public List<Client> listActiveClients() {
        return clientDAO.findActiveClients();
    }

    public Client findById(int id) {
        Client client = clientDAO.findById(id);

        if (client == null) {
            throw new RuntimeException("Cliente/indústria não encontrado.");
        }

        return client;
    }

    public List<Client> searchByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return clientDAO.findAll();
        }

        return clientDAO.searchByName(name.trim());
    }

    public void updateClient(int id, String corporateName, String fantasyName, String cnpj,
                             String phone, String email, String companyLink, boolean active) {
        if (corporateName == null || corporateName.trim().isEmpty()) {
            throw new RuntimeException("A razão social é obrigatória.");
        }

        if (fantasyName == null || fantasyName.trim().isEmpty()) {
            throw new RuntimeException("O nome fantasia é obrigatório.");
        }

        if (companyLink == null || companyLink.trim().isEmpty()) {
            throw new RuntimeException("Selecione o vínculo da indústria.");
        }

        Client client = new Client();
        client.setId(id);
        client.setCorporateName(corporateName.trim());
        client.setName(fantasyName.trim());
        client.setCnpj(formatNullable(cnpj));
        client.setPhone(formatNullable(phone));
        client.setEmail(formatNullable(email));
        client.setCompanyLink(companyLink);
        client.setActive(active);

        clientDAO.update(client);
    }

    public void deactivateClient(int id) {
        clientDAO.deactivate(id);
    }

    public void activateClient(int id) {
        clientDAO.activate(id);
    }

    private String formatNullable(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}