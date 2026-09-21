package com.atpromo.systematpromo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "loja")
public class Loja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "nome")
    private String nome;

    @Column(name = "rede")
    private String rede;

    @Column(name = "uf")
    private String uf;

    @Column(name = "cnpj")
    private String cnpj;

    @Column(name = "active")
    private boolean active;

    public Loja() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getRede() { return rede; }
    public void setRede(String rede) { this.rede = rede; }

    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}