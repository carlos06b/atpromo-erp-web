package com.atpromo.systematpromo.repository;

import com.atpromo.systematpromo.model.Beneficiario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BeneficiarioRepository extends JpaRepository<Beneficiario, Integer> {
    Optional<Beneficiario> findByPromoterId(Integer promoterId);
    Optional<Beneficiario> findByClientId(Integer clientId);
}
