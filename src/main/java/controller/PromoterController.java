package controller;

import dao.PromoterDAO;
import model.Promoter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PromoterController {

    private final PromoterDAO promoterDAO = new PromoterDAO();

    public void register(String name, String cpf, String phone, String uf, String city,
                         String companyLink, String pix, String pixType, LocalDate dateBirth,
                         BigDecimal salary, String type) {

        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("O nome do promotor é obrigatório.");
        }

        if (cpf == null || cpf.trim().isEmpty()) {
            throw new RuntimeException("O CPF do promotor é obrigatório.");
        }

        cpf = cpf.replaceAll("[^0-9]", "");

        if (!isValidCpf(cpf)) {
            throw new RuntimeException("CPF inválido.");
        }

        if (promoterDAO.findByCpf(cpf) != null) {
            throw new RuntimeException("CPF já cadastrado.");
        }

        if (phone == null || phone.trim().isEmpty()) {
            throw new RuntimeException("O telefone é obrigatório.");
        }

        if (uf == null || uf.trim().isEmpty()) {
            throw new RuntimeException("Selecione a UF do promotor.");
        }

        if (city == null || city.trim().isEmpty()) {
            throw new RuntimeException("Informe a cidade do promotor.");
        }

        if (!isValidCompanyLink(companyLink)) {
            throw new RuntimeException("Vínculo inválido. Use AT ou TEJO.");
        }

        if (!isValidPromoterType(type)) {
            throw new RuntimeException("Tipo inválido. Use CLT, MEI ou FERISTA.");
        }

        if (salary == null || salary.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("O salário precisa ser maior que zero.");
        }

        Promoter promoter = new Promoter();

        promoter.setName(name.trim());
        promoter.setCpf(cpf);
        promoter.setPhone(phone.trim());
        promoter.setUf(uf.trim().toUpperCase());
        promoter.setCity(city.trim());
        promoter.setCompanyLink(companyLink.trim().toUpperCase());
        promoter.setPix(formatNullable(pix));
        promoter.setPixType(formatNullable(pixType));
        promoter.setDateBirth(dateBirth);
        promoter.setSalary(salary);
        promoter.setType(type.toUpperCase());
        promoter.setActive(true);

        int id = promoterDAO.save(promoter);

        if (id == -1) {
            throw new RuntimeException("Não foi possível cadastrar o promotor.");
        }
    }

    public void listAll() {

        List<Promoter> list = promoterDAO.findAll();

        if (list.isEmpty()) {
            System.out.println("Nenhum promotor cadastrado.");
            return;
        }

        for (Promoter p : list) {
            System.out.println(
                    p.getId() + " | " +
                            p.getName() + " | " +
                            p.getCpf() + " | " +
                            p.getPhone() + " | " +
                            p.getUf() + " | " +
                            p.getCity() + " | " +
                            p.getCompanyLink() + " | " +
                            p.getType() + " | " +
                            p.getSalary() + " | " +
                            (p.isActive() ? "ATIVO" : "INATIVO")
            );
        }
    }

    public List<Promoter> searchByNameIncludingInactive(String name) {
        if (name == null || name.isBlank()) {
            return List.of();
        }

        return promoterDAO.findByNameIncludingInactive(name.trim());
    }

    public List<Promoter> getAll() {
        return promoterDAO.findAll();
    }

    public List<Promoter> getByType(String type) {

        if (!isValidPromoterType(type)) {
            return List.of();
        }

        return promoterDAO.findByType(type.toUpperCase());
    }

    public Promoter findById(int id) {
        return promoterDAO.findById(id);
    }

    public List<Promoter> searchByName(String name) {
        if (name == null || name.isBlank()) {
            return List.of();
        }

        return promoterDAO.findByName(name.trim());
    }

    public void update(int id, String name, String phone, String uf, String city,
                       String companyLink, String pix, String pixType, BigDecimal salary, String type) {

        Promoter p = promoterDAO.findById(id);

        if (p == null) {
            throw new RuntimeException("Promotor não encontrado.");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("O nome do promotor é obrigatório.");
        }

        if (phone == null || phone.trim().isEmpty()) {
            throw new RuntimeException("O telefone é obrigatório.");
        }

        if (uf == null || uf.trim().isEmpty()) {
            throw new RuntimeException("Selecione a UF do promotor.");
        }

        if (city == null || city.trim().isEmpty()) {
            throw new RuntimeException("Informe a cidade do promotor.");
        }

        if (!isValidCompanyLink(companyLink)) {
            throw new RuntimeException("Vínculo inválido. Use AT ou TEJO.");
        }

        if (!isValidPromoterType(type)) {
            throw new RuntimeException("Tipo inválido. Use CLT, MEI ou FERISTA.");
        }

        if (salary == null || salary.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("O salário precisa ser maior que zero.");
        }

        p.setName(name.trim());
        p.setPhone(phone.trim());
        p.setUf(uf.trim().toUpperCase());
        p.setCity(city.trim());
        p.setCompanyLink(companyLink.trim().toUpperCase());
        p.setPix(formatNullable(pix));
        p.setPixType(formatNullable(pixType));
        p.setSalary(salary);
        p.setType(type.toUpperCase());

        promoterDAO.update(p);
    }

    public void inactivate(int id) {

        Promoter p = promoterDAO.findById(id);

        if (p == null) {
            throw new RuntimeException("Promotor não encontrado.");
        }

        p.setActive(false);

        promoterDAO.update(p);
    }

    public void activate(int id) {

        Promoter p = promoterDAO.findById(id);

        if (p == null) {
            throw new RuntimeException("Promotor não encontrado.");
        }

        p.setActive(true);

        promoterDAO.update(p);
    }

    private boolean isValidCompanyLink(String companyLink) {
        return companyLink != null &&
                (companyLink.equalsIgnoreCase("AT")
                        || companyLink.equalsIgnoreCase("TEJO"));
    }

    private boolean isValidPromoterType(String type) {
        return type != null &&
                (type.equalsIgnoreCase("CLT")
                        || type.equalsIgnoreCase("MEI")
                        || type.equalsIgnoreCase("FERISTA"));
    }

    private String formatNullable(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private boolean isValidCpf(String cpf) {

        if (cpf.length() != 11) return false;

        if (cpf.matches("(\\d)\\1{10}")) return false;

        int sum = 0;

        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
        }

        int firstDigit = 11 - (sum % 11);
        if (firstDigit >= 10) firstDigit = 0;

        if (firstDigit != Character.getNumericValue(cpf.charAt(9))) {
            return false;
        }

        sum = 0;

        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
        }

        int secondDigit = 11 - (sum % 11);
        if (secondDigit >= 10) secondDigit = 0;

        return secondDigit == Character.getNumericValue(cpf.charAt(10));
    }
}