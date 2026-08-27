package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.FinancePromoter;
import com.atpromo.systematpromo.repository.FinancePromoterRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/finance-promoter")
public class FinanceController {

    private final FinancePromoterRepository financePromoterRepository;

    public FinanceController(FinancePromoterRepository financePromoterRepository) {
        this.financePromoterRepository = financePromoterRepository;
    }

    @GetMapping
    public List<FinancePromoter> listAll() {
        return financePromoterRepository.findAll();
    }
}