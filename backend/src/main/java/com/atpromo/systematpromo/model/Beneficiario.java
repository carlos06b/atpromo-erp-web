package com.atpromo.systematpromo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "beneficiario")
public class Beneficiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "nome")
    private String nome;

    // Preenchido automaticamente quando o beneficiário é criado a partir do
    // cadastro de um promotor (ex: na primeira aprovação de Pix dele). Não é
    // um campo editável na tela — é só pra achar/reaproveitar o cadastro certo.
    @Column(name = "promoter_id")
    private Integer promoterId;

    // Mesma ideia do promoterId, mas para clientes: preenchido quando o
    // beneficiário é criado a partir do lançamento automático de um
    // faturamento recebido (ver LancamentoExtratoController).
    @Column(name = "client_id")
    private Integer clientId;

    @Column(name = "ativo")
    private boolean ativo = true;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Integer getPromoterId() { return promoterId; }
    public void setPromoterId(Integer promoterId) { this.promoterId = promoterId; }

    public Integer getClientId() { return clientId; }
    public void setClientId(Integer clientId) { this.clientId = clientId; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
