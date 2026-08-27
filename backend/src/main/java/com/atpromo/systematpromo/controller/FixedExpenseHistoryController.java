package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.FixedExpenseHistory;
import com.atpromo.systematpromo.repository.FixedExpenseHistoryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fixed-expense-history")
public class FixedExpenseHistoryController {

    private final FixedExpenseHistoryRepository fixedExpenseHistoryRepository;

    public FixedExpenseHistoryController(FixedExpenseHistoryRepository fixedExpenseHistoryRepository) {
        this.fixedExpenseHistoryRepository = fixedExpenseHistoryRepository;
    }

    @GetMapping
    public List<FixedExpenseHistory> listAll() {
        return fixedExpenseHistoryRepository.findAll();
    }
}