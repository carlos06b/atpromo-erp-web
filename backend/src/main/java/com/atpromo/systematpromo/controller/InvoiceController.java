package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.Invoice;
import com.atpromo.systematpromo.repository.InvoiceRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final AccessControl accessControl;

    public InvoiceController(InvoiceRepository invoiceRepository, AccessControl accessControl) {
        this.invoiceRepository = invoiceRepository;
        this.accessControl = accessControl;
    }

    @GetMapping
    public ResponseEntity<?> listAll(Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        return ResponseEntity.ok(invoiceRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable int id, Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        return invoiceRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Invoice invoice, Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        invoice.setId(null);
        return ResponseEntity.ok(invoiceRepository.save(invoice));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody Invoice invoice, Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        if (!invoiceRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        invoice.setId(id);
        return ResponseEntity.ok(invoiceRepository.save(invoice));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id, Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        if (!invoiceRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        invoiceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para acessar faturamento."));
    }
}