package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.VariableExpense;
import com.atpromo.systematpromo.repository.VariableExpenseRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/variable-expenses")
public class VariableExpenseController {

    private final VariableExpenseRepository variableExpenseRepository;

    public VariableExpenseController(VariableExpenseRepository variableExpenseRepository) {
        this.variableExpenseRepository = variableExpenseRepository;
    }

    @GetMapping
    public List<VariableExpense> listAll() {
        return variableExpenseRepository.findAll();
    }
}