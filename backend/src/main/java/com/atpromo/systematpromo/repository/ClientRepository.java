package com.atpromo.systematpromo.repository;

import com.atpromo.systematpromo.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Integer> {
}