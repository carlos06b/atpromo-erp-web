package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.FixedExpense;
import com.atpromo.systematpromo.repository.FixedExpenseRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fixed-expenses")
public class FixedExpenseController {

    private final FixedExpenseRepository fixedExpenseRepository;

    public FixedExpenseController(FixedExpenseRepository fixedExpenseRepository) {
        this.fixedExpenseRepository = fixedExpenseRepository;
    }

    @GetMapping
    public List<FixedExpense> listAll() {
        return fixedExpenseRepository.findAll();
    }
}