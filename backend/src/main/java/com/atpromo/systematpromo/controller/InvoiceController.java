package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.Invoice;
import com.atpromo.systematpromo.repository.InvoiceRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;

    public InvoiceController(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping
    public List<Invoice> listAll() {
        return invoiceRepository.findAll();
    }
}