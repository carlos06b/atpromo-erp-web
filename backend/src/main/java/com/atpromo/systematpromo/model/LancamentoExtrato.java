package com.atpromo.systematpromo.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lancamento_extrato")
public class LancamentoExtrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "conta_bancaria_id")
    private Integer contaBancariaId;

    @Column(name = "data")
    private LocalDate data;

    @Column(name = "tipo")
    private String tipo;

    @Column(name = "valor")
    private BigDecimal valor;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "origem_tipo")
    private String origemTipo;

    @Column(name = "origem_id")
    private Integer origemId;

    @Column(name = "beneficiario_id")
    private Integer beneficiarioId;

    @Column(name = "categoria_id")
    private Integer categoriaId;

    @Column(name = "centro_custo_id")
    private Integer centroCustoId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getContaBancariaId() { return contaBancariaId; }
    public void setContaBancariaId(Integer contaBancariaId) { this.contaBancariaId = contaBancariaId; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getOrigemTipo() { return origemTipo; }
    public void setOrigemTipo(String origemTipo) { this.origemTipo = origemTipo; }

    public Integer getOrigemId() { return origemId; }
    public void setOrigemId(Integer origemId) { this.origemId = origemId; }

    public Integer getBeneficiarioId() { return beneficiarioId; }
    public void setBeneficiarioId(Integer beneficiarioId) { this.beneficiarioId = beneficiarioId; }

    public Integer getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Integer categoriaId) { this.categoriaId = categoriaId; }

    public Integer getCentroCustoId() { return centroCustoId; }
    public void setCentroCustoId(Integer centroCustoId) { this.centroCustoId = centroCustoId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
