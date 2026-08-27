package com.atpromo.systematpromo.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fixed_expense_history")
public class FixedExpenseHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "fixed_expense_id")
    private int fixedExpenseId;

    @Column(name = "name")
    private String name;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "status")
    private String status;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "description")
    private String description;

    public FixedExpenseHistory() {
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getFixedExpenseId() { return fixedExpenseId; }
    public void setFixedExpenseId(int fixedExpenseId) { this.fixedExpenseId = fixedExpenseId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
}