package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.InventoryItem;
import com.atpromo.systematpromo.model.Promoter;
import com.atpromo.systematpromo.model.WorkItemDelivery;
import com.atpromo.systematpromo.repository.InventoryItemRepository;
import com.atpromo.systematpromo.repository.PromoterRepository;
import com.atpromo.systematpromo.repository.WorkItemDeliveryRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/work-item-deliveries")
public class WorkItemDeliveryController {

    private final WorkItemDeliveryRepository workItemDeliveryRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final PromoterRepository promoterRepository;
    private final AccessControl accessControl;

    public WorkItemDeliveryController(WorkItemDeliveryRepository workItemDeliveryRepository,
                                      InventoryItemRepository inventoryItemRepository,
                                      PromoterRepository promoterRepository,
                                      AccessControl accessControl) {
        this.workItemDeliveryRepository = workItemDeliveryRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.promoterRepository = promoterRepository;
        this.accessControl = accessControl;
    }

    @GetMapping
    public ResponseEntity<?> list(Authentication authentication) {
        if (accessControl.isFinance(authentication)) {
            return forbidden();
        }

        List<WorkItemDeliveryView> views = workItemDeliveryRepository.findAll().stream()
                .map(this::toView)
                .toList();

        return ResponseEntity.ok(views);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody WorkItemDelivery delivery, Authentication authentication) {
        if (accessControl.isFinance(authentication)) {
            return forbidden();
        }

        if (delivery.getPromoterId() == null) {
            return badRequest("Selecione um promotor.");
        }

        if (delivery.getQuantity() == null || delivery.getQuantity() <= 0) {
            return badRequest("Informe uma quantidade válida.");
        }

        Optional<InventoryItem> itemOpt = inventoryItemRepository.findById(delivery.getItemId());
        if (itemOpt.isEmpty()) {
            return badRequest("Item não encontrado.");
        }

        InventoryItem item = itemOpt.get();
        int available = item.getCurrentStock() != null ? item.getCurrentStock() : 0;
        if (available < delivery.getQuantity()) {
            return badRequest("Estoque insuficiente para \"" + item.getName() + "\". Disponível: " + available + ".");
        }

        item.setCurrentStock(available - delivery.getQuantity());
        inventoryItemRepository.save(item);

        delivery.setId(null);
        WorkItemDelivery saved = workItemDeliveryRepository.save(delivery);
        return ResponseEntity.ok(toView(saved));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody WorkItemDelivery delivery, Authentication authentication) {
        if (accessControl.isFinance(authentication)) {
            return forbidden();
        }

        Optional<WorkItemDelivery> existingOpt = workItemDeliveryRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (delivery.getPromoterId() == null) {
            return badRequest("Selecione um promotor.");
        }

        if (delivery.getQuantity() == null || delivery.getQuantity() <= 0) {
            return badRequest("Informe uma quantidade válida.");
        }

        WorkItemDelivery existing = existingOpt.get();
        int oldQuantity = existing.getQuantity() != null ? existing.getQuantity() : 0;
        int newQuantity = delivery.getQuantity();

        if (Objects.equals(existing.getItemId(), delivery.getItemId())) {
            Optional<InventoryItem> itemOpt = inventoryItemRepository.findById(delivery.getItemId());
            if (itemOpt.isEmpty()) {
                return badRequest("Item não encontrado.");
            }

            InventoryItem item = itemOpt.get();
            int available = item.getCurrentStock() != null ? item.getCurrentStock() : 0;
            int availableAfterRestore = available + oldQuantity;

            if (availableAfterRestore < newQuantity) {
                return badRequest("Estoque insuficiente para \"" + item.getName() + "\". Disponível: " + availableAfterRestore + ".");
            }

            item.setCurrentStock(availableAfterRestore - newQuantity);
            inventoryItemRepository.save(item);
        } else {
            Optional<InventoryItem> newItemOpt = inventoryItemRepository.findById(delivery.getItemId());
            if (newItemOpt.isEmpty()) {
                return badRequest("Item não encontrado.");
            }

            InventoryItem newItem = newItemOpt.get();
            int newAvailable = newItem.getCurrentStock() != null ? newItem.getCurrentStock() : 0;
            if (newAvailable < newQuantity) {
                return badRequest("Estoque insuficiente para \"" + newItem.getName() + "\". Disponível: " + newAvailable + ".");
            }

            inventoryItemRepository.findById(existing.getItemId()).ifPresent(oldItem -> {
                int oldAvailable = oldItem.getCurrentStock() != null ? oldItem.getCurrentStock() : 0;
                oldItem.setCurrentStock(oldAvailable + oldQuantity);
                inventoryItemRepository.save(oldItem);
            });

            newItem.setCurrentStock(newAvailable - newQuantity);
            inventoryItemRepository.save(newItem);
        }

        delivery.setId(id);
        WorkItemDelivery saved = workItemDeliveryRepository.save(delivery);
        return ResponseEntity.ok(toView(saved));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Integer id, Authentication authentication) {
        if (accessControl.isFinance(authentication)) {
            return forbidden();
        }

        Optional<WorkItemDelivery> existingOpt = workItemDeliveryRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        WorkItemDelivery existing = existingOpt.get();
        inventoryItemRepository.findById(existing.getItemId()).ifPresent(item -> {
            int available = item.getCurrentStock() != null ? item.getCurrentStock() : 0;
            int restored = existing.getQuantity() != null ? existing.getQuantity() : 0;
            item.setCurrentStock(available + restored);
            inventoryItemRepository.save(item);
        });

        workItemDeliveryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private WorkItemDeliveryView toView(WorkItemDelivery delivery) {
        String promoterName = promoterRepository.findById(delivery.getPromoterId())
                .map(Promoter::getName)
                .orElse("Promotor não encontrado");

        InventoryItem item = inventoryItemRepository.findById(delivery.getItemId()).orElse(null);
        String itemName = item != null ? item.getName() : "Item não encontrado";
        String itemCategory = item != null ? item.getCategory() : null;

        return new WorkItemDeliveryView(
                delivery.getId(),
                delivery.getPromoterId(),
                promoterName,
                delivery.getItemId(),
                itemName,
                itemCategory,
                delivery.getQuantity(),
                delivery.getDeliveryDate(),
                delivery.getObservation()
        );
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para acessar uniformes e materiais."));
    }

    public record WorkItemDeliveryView(
            Integer id,
            Integer promoterId,
            String promoterName,
            Integer itemId,
            String itemName,
            String itemCategory,
            Integer quantity,
            LocalDate deliveryDate,
            String observation
    ) {}
}