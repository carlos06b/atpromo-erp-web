package com.atpromo.systematpromo.repository;

import com.atpromo.systematpromo.model.FixedExpenseHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FixedExpenseHistoryRepository extends JpaRepository<FixedExpenseHistory, Integer> {
}