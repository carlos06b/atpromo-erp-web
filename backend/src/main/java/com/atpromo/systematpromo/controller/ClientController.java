package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.Client;
import com.atpromo.systematpromo.repository.ClientRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientRepository clientRepository;
    private final AccessControl accessControl;

    public ClientController(ClientRepository clientRepository, AccessControl accessControl) {
        this.clientRepository = clientRepository;
        this.accessControl = accessControl;
    }

    @GetMapping
    public ResponseEntity<?> listAll(Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        return ResponseEntity.ok(clientRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable int id, Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        return clientRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Client client, Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        return ResponseEntity.ok(clientRepository.save(client));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody Client client, Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        if (!clientRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        client.setId(id);
        return ResponseEntity.ok(clientRepository.save(client));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id, Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        if (!clientRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        clientRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para acessar clientes."));
    }
}