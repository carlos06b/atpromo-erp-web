package com.atpromo.systematpromo.repository;

import com.atpromo.systematpromo.model.PasswordResetRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Integer> {
    List<PasswordResetRequest> findByResolvedFalseOrderByRequestedAtDesc();
}