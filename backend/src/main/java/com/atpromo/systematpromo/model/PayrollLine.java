package com.atpromo.systematpromo.model;

import java.math.BigDecimal;

public class PayrollLine {

    private int promoterId;
    private String promoterName;
    private String promoterType;
    private BigDecimal baseSalary;
    private BigDecimal bonuses;
    private BigDecimal discounts;
    private BigDecimal netAmount;
    private String status;
    private String observation;

    public PayrollLine() {
    }

    public PayrollLine(
            int promoterId,
            String promoterName,
            String promoterType,
            BigDecimal baseSalary,
            BigDecimal bonuses,
            BigDecimal discounts,
            BigDecimal netAmount,
            String status,
            String observation
    ) {
        this.promoterId = promoterId;
        this.promoterName = promoterName;
        this.promoterType = promoterType;
        this.baseSalary = baseSalary;
        this.bonuses = bonuses;
        this.discounts = discounts;
        this.netAmount = netAmount;
        this.status = status;
        this.observation = observation;
    }

    public int getPromoterId() {
        return promoterId;
    }

    public void setPromoterId(int promoterId) {
        this.promoterId = promoterId;
    }

    public String getPromoterName() {
        return promoterName;
    }

    public void setPromoterName(String promoterName) {
        this.promoterName = promoterName;
    }

    public String getPromoterType() {
        return promoterType;
    }

    public void setPromoterType(String promoterType) {
        this.promoterType = promoterType;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(BigDecimal baseSalary) {
        this.baseSalary = baseSalary;
    }

    public BigDecimal getBonuses() {
        return bonuses;
    }

    public void setBonuses(BigDecimal bonuses) {
        this.bonuses = bonuses;
    }

    public BigDecimal getDiscounts() {
        return discounts;
    }

    public void setDiscounts(BigDecimal discounts) {
        this.discounts = discounts;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }
}