package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.InventoryItem;
import com.atpromo.systematpromo.repository.InventoryItemRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory-items")
public class InventoryItemController {

    private final InventoryItemRepository inventoryItemRepository;
    private final AccessControl accessControl;

    public InventoryItemController(InventoryItemRepository inventoryItemRepository, AccessControl accessControl) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.accessControl = accessControl;
    }

    @GetMapping
    public ResponseEntity<?> list(Authentication authentication) {
        if (accessControl.isFinance(authentication)) {
            return forbidden();
        }

        List<InventoryItem> items = inventoryItemRepository.findAll();
        return ResponseEntity.ok(items);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody InventoryItem item, Authentication authentication) {
        if (accessControl.isFinance(authentication)) {
            return forbidden();
        }

        item.setId(null);
        if (item.getCurrentStock() == null) {
            item.setCurrentStock(0);
        }

        InventoryItem saved = inventoryItemRepository.save(item);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody InventoryItem item, Authentication authentication) {
        if (accessControl.isFinance(authentication)) {
            return forbidden();
        }

        if (inventoryItemRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        item.setId(id);
        if (item.getCurrentStock() == null) {
            item.setCurrentStock(0);
        }

        InventoryItem saved = inventoryItemRepository.save(item);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id, Authentication authentication) {
        if (accessControl.isFinance(authentication)) {
            return forbidden();
        }

        if (inventoryItemRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            inventoryItemRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body(Map.of(
                    "message", "Não é possível excluir este item: já existem entregas ou movimentações de estoque vinculadas a ele."
            ));
        }

        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para acessar o estoque."));
    }
}