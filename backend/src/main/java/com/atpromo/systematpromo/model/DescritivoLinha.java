package com.atpromo.systematpromo.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "descritivo_linha")
public class DescritivoLinha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "descritivo_id")
    private Integer descritivoId;

    @Column(name = "loja_id")
    private Integer lojaId;

    @Column(name = "dias_semana")
    private String diasSemana;

    @Column(name = "horas_por_atendimento")
    private BigDecimal horasPorAtendimento;

    @Column(name = "valor_hora")
    private BigDecimal valorHora;

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "valor_total")
    private BigDecimal valorTotal;

    @Column(name = "valor_total_manual")
    private boolean valorTotalManual;

    @Column(name = "ordem")
    private Integer ordem;

    public DescritivoLinha() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getDescritivoId() { return descritivoId; }
    public void setDescritivoId(Integer descritivoId) { this.descritivoId = descritivoId; }

    public Integer getLojaId() { return lojaId; }
    public void setLojaId(Integer lojaId) { this.lojaId = lojaId; }

    public String getDiasSemana() { return diasSemana; }
    public void setDiasSemana(String diasSemana) { this.diasSemana = diasSemana; }

    public BigDecimal getHorasPorAtendimento() { return horasPorAtendimento; }
    public void setHorasPorAtendimento(BigDecimal horasPorAtendimento) { this.horasPorAtendimento = horasPorAtendimento; }

    public BigDecimal getValorHora() { return valorHora; }
    public void setValorHora(BigDecimal valorHora) { this.valorHora = valorHora; }

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    public boolean isValorTotalManual() { return valorTotalManual; }
    public void setValorTotalManual(boolean valorTotalManual) { this.valorTotalManual = valorTotalManual; }

    public Integer getOrdem() { return ordem; }
    public void setOrdem(Integer ordem) { this.ordem = ordem; }
}