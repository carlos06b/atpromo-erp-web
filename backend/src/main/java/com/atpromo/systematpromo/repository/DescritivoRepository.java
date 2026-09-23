package com.atpromo.systematpromo.repository;

import com.atpromo.systematpromo.model.Descritivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DescritivoRepository extends JpaRepository<Descritivo, Integer> {
    List<Descritivo> findByClienteIdAndMesAndAno(Integer clienteId, Integer mes, Integer ano);
    List<Descritivo> findByClienteId(Integer clienteId);
}