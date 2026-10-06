package com.atpromo.systematpromo.repository;

import com.atpromo.systematpromo.model.LancamentoExtrato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LancamentoExtratoRepository extends JpaRepository<LancamentoExtrato, Integer> {
    List<LancamentoExtrato> findByContaBancariaId(Integer contaBancariaId);
    List<LancamentoExtrato> findByOrigemTipoAndOrigemId(String origemTipo, Integer origemId);
    boolean existsByOrigemTipoAndOrigemId(String origemTipo, Integer origemId);
    boolean existsByContaBancariaId(Integer contaBancariaId);
    boolean existsByBeneficiarioId(Integer beneficiarioId);
    boolean existsByCategoriaId(Integer categoriaId);
    boolean existsByCentroCustoId(Integer centroCustoId);
}
