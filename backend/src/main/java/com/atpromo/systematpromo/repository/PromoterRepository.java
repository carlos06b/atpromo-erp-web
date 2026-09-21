package com.atpromo.systematpromo.repository;

import com.atpromo.systematpromo.model.Promoter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromoterRepository extends JpaRepository<Promoter, Integer> {
    List<Promoter> findByLojaId(Integer lojaId);
}