package com.atpromo.systematpromo.repository;

import com.atpromo.systematpromo.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Integer> {
}