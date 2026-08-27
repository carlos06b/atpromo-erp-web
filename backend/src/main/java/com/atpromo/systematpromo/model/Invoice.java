package com.atpromo.systematpromo.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoice")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "id_client")
    private int clientId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "received_amount")
    private BigDecimal receivedAmount;

    @Column(name = "description")
    private String description;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "status")
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

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}