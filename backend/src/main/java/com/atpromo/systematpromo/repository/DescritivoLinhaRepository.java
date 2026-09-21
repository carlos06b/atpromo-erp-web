package com.atpromo.systematpromo.repository;

import com.atpromo.systematpromo.model.DescritivoLinha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DescritivoLinhaRepository extends JpaRepository<DescritivoLinha, Integer> {
    List<DescritivoLinha> findByDescritivoId(Integer descritivoId);
    List<DescritivoLinha> findByLojaId(Integer lojaId);
}