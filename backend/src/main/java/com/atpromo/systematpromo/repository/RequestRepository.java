package com.atpromo.systematpromo.repository;

import com.atpromo.systematpromo.model.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Integer> {
    List<Request> findByStatusIgnoreCase(String status);
    List<Request> findByDateBetween(LocalDateTime start, LocalDateTime end);

    // ACHADO H1: usado por RequestController.approve() para eliminar a corrida
    // check-then-act (ler status PENDENTE, decidir, só depois salvar). Este
    // UPDATE só afeta a linha se o status ainda for PENDENTE no momento exato
    // da execução no banco — a atomicidade vem do próprio banco (lock de linha
    // do UPDATE), não de nenhuma lógica em memória. Duas chamadas concorrentes
    // só podem ter, no máximo, UMA delas retornando 1 (linha afetada); a outra
    // recebe 0, o que o controller trata como "já processada" (409).
    @Modifying
    @Query("UPDATE Request r SET r.status = :newStatus, r.id_UserFin = :userId WHERE r.id = :id AND r.status = 'PENDENTE'")
    int updateStatusIfPending(@Param("id") int id, @Param("newStatus") String newStatus, @Param("userId") Integer userId);
}