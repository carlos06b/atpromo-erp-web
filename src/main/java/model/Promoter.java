package model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Promoter {

    private int id;
    private String name;
    private String cpf;
    private String phone;
    private String uf;
    private String city;
    private LocalDate dateBirth;
    private boolean active;
    private BigDecimal salary;
    private String type; // CLT ou MEI
    private String pix;
    private String pixType;
    private String companyLink;

    public Promoter() {
    }

    public Promoter(int id, String name, String cpf, String phone, String uf, String city,
                    LocalDate dateBirth, boolean active, BigDecimal salary, String type,
                    String pix, String pixType) {
        this.id = id;
        this.name = name;
        this.cpf = cpf;
        this.phone = phone;
        this.uf = uf;
        this.city = city;
        this.dateBirth = dateBirth;
        this.active = active;
        this.salary = salary;
        this.type = type;
        this.pix = pix;
        this.pixType = pixType;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }


    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }


    public LocalDate getDateBirth() {
        return dateBirth;
    }

    public void setDateBirth(LocalDate dateBirth) {
        this.dateBirth = dateBirth;
    }


    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }


    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPix() {
        return pix;
    }

    public void setPix(String pix) {
        this.pix = pix;
    }

    public String getPixType() {
        return pixType;
    }

    public void setPixType(String pixType) {
        this.pixType = pixType;
    }

    public String getCompanyLink() {
        return companyLink;
    }

    public void setCompanyLink(String companyLink) {
        this.companyLink = companyLink;
    }
}