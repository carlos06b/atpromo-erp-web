package model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FixedExpenseHistory {

    private int id;
    private int fixedExpenseId;
    private String name;
    private BigDecimal amount;
    private LocalDate dueDate;
    private String status;
    private LocalDate paymentDate;
    private String description;

    public FixedExpenseHistory() {
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


    public int getFixedExpenseId() {
        return fixedExpenseId;
    }

    public void setFixedExpenseId(int fixedExpenseId) {
        this.fixedExpenseId = fixedExpenseId;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }


    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }
}