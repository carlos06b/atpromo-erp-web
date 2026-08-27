package com.atpromo.systematpromo.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "variable_expense")
public class VariableExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "status")
    private boolean status;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "description")
    private String description;

    @Column(name = "installment_group")
    private String installmentGroup;

    @Column(name = "installment_number")
    private Integer installmentNumber;

    @Column(name = "total_installments")
    private Integer totalInstallments;

    public VariableExpense() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getInstallmentGroup() { return installmentGroup; }
    public void setInstallmentGroup(String installmentGroup) { this.installmentGroup = installmentGroup; }

    public Integer getInstallmentNumber() { return installmentNumber; }
    public void setInstallmentNumber(Integer installmentNumber) { this.installmentNumber = installmentNumber; }

    public Integer getTotalInstallments() { return totalInstallments; }
    public void setTotalInstallments(Integer totalInstallments) { this.totalInstallments = totalInstallments; }
}