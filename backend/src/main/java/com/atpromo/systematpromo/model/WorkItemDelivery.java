package com.atpromo.systematpromo.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "work_item_delivery")
public class WorkItemDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "promoter_id")
    private Integer promoterId;

    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "observation")
    private String observation;

    public WorkItemDelivery() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getPromoterId() { return promoterId; }
    public void setPromoterId(Integer promoterId) { this.promoterId = promoterId; }

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }

    public String getObservation() { return observation; }
    public void setObservation(String observation) { this.observation = observation; }
}