package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.Request;
import com.atpromo.systematpromo.repository.RequestRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestRepository requestRepository;

    public RequestController(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    @GetMapping
    public List<Request> listAll() {
        return requestRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Request> getById(@PathVariable int id) {
        return requestRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Request create(@RequestBody Request request) {
        return requestRepository.save(request);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Request> update(@PathVariable int id, @RequestBody Request request) {
        if (!requestRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        request.setId(id);
        return ResponseEntity.ok(requestRepository.save(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        if (!requestRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        requestRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}