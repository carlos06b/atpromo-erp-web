package com.atpromo.systematpromo.repository;

import com.atpromo.systematpromo.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Integer> {
}