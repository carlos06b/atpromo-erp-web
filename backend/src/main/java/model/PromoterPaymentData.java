package model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PromoterPaymentData {

    private String document;
    private String name;
    private String pix;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String pixType;

    public PromoterPaymentData(String document, String name, String pix, String pixType, BigDecimal amount, LocalDate paymentDate) {
        this.document = document;
        this.name = name;
        this.pix = pix;
        this.pixType = pixType;
        this.amount = amount;
        this.paymentDate = paymentDate;
    }

    public String getDocument() {
        return document;
    }

    public String getName() {
        return name;
    }

    public String getPix() {
        return pix;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public String getPixType() {
        return pixType;
    }
}