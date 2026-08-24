package model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InvoiceView {

    private int id;
    private String clientName;
    private String companyLink;
    private BigDecimal amount;
    private BigDecimal receivedAmount;
    private String description;
    private LocalDate dueDate;
    private LocalDate issueDate;
    private LocalDate paymentDate;
    private String status;

    public InvoiceView() {
    }

    public int getId() {
        return id;
    }

    public String getClientName() {
        return clientName;
    }

    public String getCompanyLink() {
        return companyLink;
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

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public void setCompanyLink(String companyLink) {
        this.companyLink = companyLink;
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