package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.Promoter;
import com.atpromo.systematpromo.repository.PromoterRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/promoters")
public class PromoterController {

    private final PromoterRepository promoterRepository;

    public PromoterController(PromoterRepository promoterRepository) {
        this.promoterRepository = promoterRepository;
    }

    @GetMapping
    public List<Promoter> listAll() {
        return promoterRepository.findAll();
    }
}