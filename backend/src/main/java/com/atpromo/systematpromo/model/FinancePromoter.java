package com.atpromo.systematpromo.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "finance_promoter")
public class FinancePromoter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "id_promoter")
    private int idPromoter;

    @Column(name = "type")
    private String type;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "status")
    private String status;

    @Column(name = "description")
    private String description;

    public FinancePromoter() {
    }

    public FinancePromoter(int id, BigDecimal amount, LocalDate date, int idPromoter, String status, String type) {
        this.id = id;
        this.amount = amount;
        this.date = date;
        this.idPromoter = idPromoter;
        this.status = status;
        this.type = type;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public int getIdPromoter() { return idPromoter; }
    public void setIdPromoter(int idPromoter) { this.idPromoter = idPromoter; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}