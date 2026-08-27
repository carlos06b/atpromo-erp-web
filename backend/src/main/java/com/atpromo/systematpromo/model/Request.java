package com.atpromo.systematpromo.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "request")
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "id_userRH")
    private int id_UserRH;

    @Column(name = "id_userFIN")
    private int id_UserFin;

    @Column(name = "id_promoter")
    private int id_Promoter;

    @Column(name = "message")
    private String message;

    @Column(name = "status")
    private String status;

    @Column(name = "date")
    private LocalDateTime date;

    @Column(name = "type")
    private String type;

    @Column(name = "amount")
    private BigDecimal amount;

    public Request(int id, int id_UserRH, int id_UserFin, int id_Promoter, String type, BigDecimal amount, String message, String status, LocalDateTime date) {
        this.id = id;
        this.id_UserRH = id_UserRH;
        this.id_UserFin = id_UserFin;
        this.id_Promoter = id_Promoter;
        this.type = type;
        this.amount = amount;
        this.message = message;
        this.status = status;
        this.date = date;
    }

    public Request() {
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public int getId_Promoter() { return id_Promoter; }
    public void setId_Promoter(int id_Promoter) { this.id_Promoter = id_Promoter; }

    public int getId_UserFin() { return id_UserFin; }
    public void setId_UserFin(int id_UserFin) { this.id_UserFin = id_UserFin; }

    public int getId_UserRH() { return id_UserRH; }
    public void setId_UserRH(int id_UserRH) { this.id_UserRH = id_UserRH; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}