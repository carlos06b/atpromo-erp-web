package com.atpromo.systematpromo.repository;

import com.atpromo.systematpromo.model.ContaBancaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContaBancariaRepository extends JpaRepository<ContaBancaria, Integer> {
    List<ContaBancaria> findByEmpresaId(Integer empresaId);
}