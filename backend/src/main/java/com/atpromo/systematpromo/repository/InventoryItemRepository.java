package com.atpromo.systematpromo.repository;

import com.atpromo.systematpromo.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Integer> {
}