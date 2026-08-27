package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.Request;
import com.atpromo.systematpromo.repository.RequestRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}