package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.Invoice;
import com.atpromo.systematpromo.repository.InvoiceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getById(@PathVariable int id) {
        return invoiceRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Invoice create(@RequestBody Invoice invoice) {
        return invoiceRepository.save(invoice);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Invoice> update(@PathVariable int id, @RequestBody Invoice invoice) {
        if (!invoiceRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        invoice.setId(id);
        return ResponseEntity.ok(invoiceRepository.save(invoice));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        if (!invoiceRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        invoiceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}