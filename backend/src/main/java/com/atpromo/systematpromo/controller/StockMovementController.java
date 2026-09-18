package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.InventoryItem;
import com.atpromo.systematpromo.model.Promoter;
import com.atpromo.systematpromo.model.StockMovement;
import com.atpromo.systematpromo.repository.InventoryItemRepository;
import com.atpromo.systematpromo.repository.PromoterRepository;
import com.atpromo.systematpromo.repository.StockMovementRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/stock-movements")
public class StockMovementController {

    private static final List<String> VALID_TYPES = List.of("REPOSICAO", "DEVOLUCAO");

    private final StockMovementRepository stockMovementRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final PromoterRepository promoterRepository;
    private final AccessControl accessControl;

    public StockMovementController(StockMovementRepository stockMovementRepository,
                                   InventoryItemRepository inventoryItemRepository,
                                   PromoterRepository promoterRepository,
                                   AccessControl accessControl) {
        this.stockMovementRepository = stockMovementRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.promoterRepository = promoterRepository;
        this.accessControl = accessControl;
    }

    @GetMapping
    public ResponseEntity<?> list(Authentication authentication) {
        if (accessControl.isFinance(authentication)) {
            return forbidden();
        }

        List<StockMovementView> views = stockMovementRepository.findAll().stream()
                .map(this::toView)
                .toList();

        return ResponseEntity.ok(views);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody StockMovement movement, Authentication authentication) {
        if (accessControl.isFinance(authentication)) {
            return forbidden();
        }

        if (movement.getType() == null || !VALID_TYPES.contains(movement.getType())) {
            return badRequest("Tipo de movimentação inválido.");
        }

        if (movement.getQuantity() == null || movement.getQuantity() <= 0) {
            return badRequest("Informe uma quantidade válida.");
        }

        if ("DEVOLUCAO".equals(movement.getType()) && movement.getPromoterId() == null) {
            return badRequest("Selecione o promotor que devolveu o item.");
        }

        Optional<InventoryItem> itemOpt = inventoryItemRepository.findById(movement.getItemId());
        if (itemOpt.isEmpty()) {
            return badRequest("Item não encontrado.");
        }

        InventoryItem item = itemOpt.get();
        int current = item.getCurrentStock() != null ? item.getCurrentStock() : 0;
        item.setCurrentStock(current + movement.getQuantity());
        inventoryItemRepository.save(item);

        movement.setId(null);
        if (movement.getMovementDate() == null) {
            movement.setMovementDate(LocalDate.now());
        }
        StockMovement saved = stockMovementRepository.save(movement);

        return ResponseEntity.ok(toView(saved));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Integer id, Authentication authentication) {
        if (accessControl.isFinance(authentication)) {
            return forbidden();
        }

        Optional<StockMovement> movementOpt = stockMovementRepository.findById(id);
        if (movementOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        StockMovement movement = movementOpt.get();
        Optional<InventoryItem> itemOpt = inventoryItemRepository.findById(movement.getItemId());
        if (itemOpt.isPresent()) {
            InventoryItem item = itemOpt.get();
            int current = item.getCurrentStock() != null ? item.getCurrentStock() : 0;
            item.setCurrentStock(current - movement.getQuantity());
            inventoryItemRepository.save(item);
        }

        stockMovementRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private StockMovementView toView(StockMovement movement) {
        InventoryItem item = inventoryItemRepository.findById(movement.getItemId()).orElse(null);
        String itemName = item != null ? item.getName() : "Item não encontrado";
        String itemCategory = item != null ? item.getCategory() : null;

        String promoterName = null;
        if (movement.getPromoterId() != null) {
            promoterName = promoterRepository.findById(movement.getPromoterId())
                    .map(Promoter::getName)
                    .orElse("Promotor não encontrado");
        }

        return new StockMovementView(
                movement.getId(),
                movement.getItemId(),
                itemName,
                itemCategory,
                movement.getType(),
                movement.getQuantity(),
                movement.getMovementDate(),
                movement.getPromoterId(),
                promoterName,
                movement.getObservation()
        );
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para acessar o estoque."));
    }

    public record StockMovementView(
            Integer id,
            Integer itemId,
            String itemName,
            String itemCategory,
            String type,
            Integer quantity,
            LocalDate movementDate,
            Integer promoterId,
            String promoterName,
            String observation
    ) {}
}