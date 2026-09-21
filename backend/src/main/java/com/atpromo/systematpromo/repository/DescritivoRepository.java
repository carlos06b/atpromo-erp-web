package com.atpromo.systematpromo.repository;

import com.atpromo.systematpromo.model.Descritivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DescritivoRepository extends JpaRepository<Descritivo, Integer> {
    Optional<Descritivo> findByClienteIdAndMesAndAno(Integer clienteId, Integer mes, Integer ano);
    List<Descritivo> findByClienteId(Integer clienteId);
}