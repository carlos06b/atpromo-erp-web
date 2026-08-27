package com.atpromo.systematpromo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "client")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "cnpj")
    private String cnpj;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "active")
    private boolean active;

    @Column(name = "company_link")
    private String companyLink;

    @Column(name = "corporate_name")
    private String corporateName;

    public Client() {
    }

    public Client(String name, String cnpj, String phone, String email, boolean active, String companyLink) {
        this.companyLink = companyLink;
        this.name = name;
        this.cnpj = cnpj;
        this.phone = phone;
        this.email = email;
        this.active = active;
    }

    public Client(int id, String name, String cnpj, String phone, String email, boolean active, String companyLink) {
        this.id = id;
        this.name = name;
        this.cnpj = cnpj;
        this.phone = phone;
        this.email = email;
        this.active = active;
        this.companyLink = companyLink;
    }

    public String getCorporateName() { return corporateName; }
    public void setCorporateName(String corporateName) { this.corporateName = corporateName; }

    public String getCompanyLink() { return companyLink; }
    public void setCompanyLink(String companyLink) { this.companyLink = companyLink; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}