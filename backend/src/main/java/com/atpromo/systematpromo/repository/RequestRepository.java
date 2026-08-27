package com.atpromo.systematpromo.repository;

import com.atpromo.systematpromo.model.Request;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestRepository extends JpaRepository<Request, Integer> {
}