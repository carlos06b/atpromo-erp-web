package com.atpromo.systematpromo.repository;

import com.atpromo.systematpromo.model.Request;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Integer> {
    List<Request> findByStatusIgnoreCase(String status);
    List<Request> findByDateBetween(LocalDateTime start, LocalDateTime end);
}