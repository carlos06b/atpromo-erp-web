package com.atpromo.systematpromo.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "conta_bancaria")
public class ContaBancaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "empresa_id")
    private Integer empresaId;

    @Column(name = "banco")
    private String banco;

    @Column(name = "apelido")
    private String apelido;

    @Column(name = "agencia")
    private String agencia;

    @Column(name = "numero_conta")
    private String numeroConta;

    @Column(name = "saldo_inicial")
    private BigDecimal saldoInicial;

    @Column(name = "data_saldo_inicial")
    private LocalDate dataSaldoInicial;

    @Column(name = "ativa")
    private boolean ativa = true;

    public ContaBancaria() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getEmpresaId() { return empresaId; }
    public void setEmpresaId(Integer empresaId) { this.empresaId = empresaId; }

    public String getBanco() { return banco; }
    public void setBanco(String banco) { this.banco = banco; }

    public String getApelido() { return apelido; }
    public void setApelido(String apelido) { this.apelido = apelido; }

    public String getAgencia() { return agencia; }
    public void setAgencia(String agencia) { this.agencia = agencia; }

    public String getNumeroConta() { return numeroConta; }
    public void setNumeroConta(String numeroConta) { this.numeroConta = numeroConta; }

    public BigDecimal getSaldoInicial() { return saldoInicial; }
    public void setSaldoInicial(BigDecimal saldoInicial) { this.saldoInicial = saldoInicial; }

    public LocalDate getDataSaldoInicial() { return dataSaldoInicial; }
    public void setDataSaldoInicial(LocalDate dataSaldoInicial) { this.dataSaldoInicial = dataSaldoInicial; }

    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
}