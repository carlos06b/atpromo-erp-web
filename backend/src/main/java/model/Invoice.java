package model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Invoice {

    private int id;
    private int clientId;
    private BigDecimal amount;
    private BigDecimal receivedAmount;
    private String description;
    private LocalDate dueDate;
    private LocalDate issueDate;
    private LocalDate paymentDate;
    private String status;

    public Invoice() {
    }

    public Invoice(int clientId, BigDecimal amount, String description, LocalDate dueDate) {
        this.clientId = clientId;
        this.amount = amount;
        this.description = description;
        this.dueDate = dueDate;
        this.status = "PENDENTE";
    }

    public Invoice(int id, int clientId, BigDecimal amount, BigDecimal receivedAmount, String description,
                   LocalDate dueDate, LocalDate issueDate, LocalDate paymentDate, String status) {
        this.id = id;
        this.clientId = clientId;
        this.amount = amount;
        this.receivedAmount = receivedAmount;
        this.description = description;
        this.dueDate = dueDate;
        this.issueDate = issueDate;
        this.paymentDate = paymentDate;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public int getClientId() {
        return clientId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getReceivedAmount() {
        return receivedAmount;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public String getStatus() {
        return status;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setReceivedAmount(BigDecimal receivedAmount) {
        this.receivedAmount = receivedAmount;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}