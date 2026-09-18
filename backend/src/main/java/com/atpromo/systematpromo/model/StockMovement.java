package com.atpromo.systematpromo.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "stock_movement")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "type")
    private String type;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "movement_date")
    private LocalDate movementDate;

    @Column(name = "promoter_id")
    private Integer promoterId;

    @Column(name = "observation")
    private String observation;

    public StockMovement() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public LocalDate getMovementDate() { return movementDate; }
    public void setMovementDate(LocalDate movementDate) { this.movementDate = movementDate; }

    public Integer getPromoterId() { return promoterId; }
    public void setPromoterId(Integer promoterId) { this.promoterId = promoterId; }

    public String getObservation() { return observation; }
    public void setObservation(String observation) { this.observation = observation; }
}