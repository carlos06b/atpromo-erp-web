package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

public class FinancePromoter {
    private int id;
    private int idPromoter;
    private String type;
    private BigDecimal amount;
    private LocalDate date;
    private String status;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getIdPromoter() {
        return idPromoter;
    }

    public void setIdPromoter(int idPromoter) {
        this.idPromoter = idPromoter;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
