package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.ContaBancaria;
import com.atpromo.systematpromo.model.User;
import com.atpromo.systematpromo.repository.BeneficiarioRepository;
import com.atpromo.systematpromo.repository.CategoriaLancamentoRepository;
import com.atpromo.systematpromo.repository.CentroCustoRepository;
import com.atpromo.systematpromo.repository.ClientRepository;
import com.atpromo.systematpromo.repository.ContaBancariaRepository;
import com.atpromo.systematpromo.repository.EmpresaRepository;
import com.atpromo.systematpromo.repository.FinancePromoterRepository;
import com.atpromo.systematpromo.repository.FixedExpenseHistoryRepository;
import com.atpromo.systematpromo.repository.FixedExpenseRepository;
import com.atpromo.systematpromo.repository.InventoryItemRepository;
import com.atpromo.systematpromo.repository.InvoiceRepository;
import com.atpromo.systematpromo.repository.LancamentoExtratoRepository;
import com.atpromo.systematpromo.repository.LojaRepository;
import com.atpromo.systematpromo.repository.PromoterRepository;
import com.atpromo.systematpromo.repository.StockMovementRepository;
import com.atpromo.systematpromo.repository.UserRepository;
import com.atpromo.systematpromo.repository.VariableExpenseRepository;
import com.atpromo.systematpromo.repository.WorkItemDeliveryRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
ACHADO H2 (CORRIGIDO): o backend verificava papel com um "deny-list" de um
 * unico papel em 14 controllers (so bloqueava RH, ou so bloqueava
 * FINANCEIRO) e nunca verificava SUPERVISOR nem qualquer outro cargo. O
 * frontend (access.js / PAGE_ACCESS), ao contrario, ja escondia essas
 * mesmas paginas de SUPERVISOR - ou seja, um SUPERVISOR que chamasse a API
 * diretamente (Postman, DevTools) tinha acesso total a telas que nunca
 * aparecem para ele na interface.
 *
 * A correcao troca o deny-list de 1 papel por um allow-list explicito (o
 * papel de negocio dono da tela + ADMIN) em cada um dos 14 controllers,
 * alinhando o backend com o que o frontend ja decidia. Este arquivo cobre
 * o endpoint de leitura (GET) de 9 desses controllers (os que ja estavam
 * versionados no git antes desta sessao) com a mesma matriz de papeis: o
 * papel dono da tela e ADMIN continuam funcionando (200); o papel que antes
 * era bloqueado continua bloqueado (403); e agora SUPERVISOR e qualquer
 * cargo desconhecido tambem recebem 403. Os outros 5 controllers do mesmo
 * achado (modulo Extrato, ainda nao commitado por ser feature sua em
 * andamento) tem a mesma correcao e os mesmos testes em
 * H2AllowListAccessExtratoFeatureTest.java.
 */
class H2AllowListAccessTest {{

    private static UserRepository userRepository;

    private static final String RH = "rh@atpromo.com";
    private static final String FINANCEIRO = "financeiro@atpromo.com";
    private static final String SUPERVISOR = "supervisor@atpromo.com";
    private static final String ADMIN = "admin@atpromo.com";
    private static final String DESCONHECIDO = "desconhecido@atpromo.com";

    @BeforeAll
    static void setUpUserRepository() {
        userRepository = mock(UserRepository.class);
        when(userRepository.findByEmail(RH)).thenReturn(Optional.of(new User(1, RH, "RH", "RH Teste", "hash")));
        when(userRepository.findByEmail(FINANCEIRO)).thenReturn(Optional.of(new User(2, FINANCEIRO, "FINANCEIRO", "Financeiro Teste", "hash")));
        when(userRepository.findByEmail(SUPERVISOR)).thenReturn(Optional.of(new User(3, SUPERVISOR, "SUPERVISOR", "Supervisor Teste", "hash")));
        when(userRepository.findByEmail(ADMIN)).thenReturn(Optional.of(new User(4, ADMIN, "ADMIN", "Admin Teste", "hash")));
        when(userRepository.findByEmail(DESCONHECIDO)).thenReturn(Optional.of(new User(5, DESCONHECIDO, "ESTOQUISTA", "Cargo Desconhecido", "hash")));
    }

    private static Authentication authFor(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
    }

    @Nested
    class Clientes {

        private ClientRepository clientRepository;
        private AccessControl accessControl;
        private ClientController controller;

        @BeforeEach
        void setUp() {
            clientRepository = mock(ClientRepository.class);
            accessControl = new AccessControl(userRepository);
            controller = new ClientController(clientRepository, accessControl);
        }

        @Test
        void rh_bloqueado() {
            Authentication auth = authFor(RH);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(403, resposta.getStatusCode().value());
        }

        @Test
        void supervisor_bloqueado() {
            Authentication auth = authFor(SUPERVISOR);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(403, resposta.getStatusCode().value(),
                    "ACHADO H2: SUPERVISOR nao deveria acessar este recurso pela API, assim como nao acessa pelo frontend");
        }

        @Test
        void cargoDesconhecido_bloqueado() {
            Authentication auth = authFor(DESCONHECIDO);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(403, resposta.getStatusCode().value());
        }

        @Test
        void financeiro_continuaFuncionando() {
            when(clientRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(FINANCEIRO);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }

        @Test
        void admin_continuaFuncionando() {
            when(clientRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(ADMIN);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }
    }


    @Nested
    class Faturamento {

        private InvoiceRepository invoiceRepository;
        private AccessControl accessControl;
        private InvoiceController controller;

        @BeforeEach
        void setUp() {
            invoiceRepository = mock(InvoiceRepository.class);
            accessControl = new AccessControl(userRepository);
            controller = new InvoiceController(invoiceRepository, accessControl);
        }

        @Test
        void rh_bloqueado() {
            Authentication auth = authFor(RH);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(403, resposta.getStatusCode().value());
        }

        @Test
        void supervisor_bloqueado() {
            Authentication auth = authFor(SUPERVISOR);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(403, resposta.getStatusCode().value(),
                    "ACHADO H2: SUPERVISOR nao deveria acessar este recurso pela API, assim como nao acessa pelo frontend");
        }

        @Test
        void cargoDesconhecido_bloqueado() {
            Authentication auth = authFor(DESCONHECIDO);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(403, resposta.getStatusCode().value());
        }

        @Test
        void financeiro_continuaFuncionando() {
            when(invoiceRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(FINANCEIRO);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }

        @Test
        void admin_continuaFuncionando() {
            when(invoiceRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(ADMIN);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }
    }


    @Nested
    class DespesasFixas {

        private FixedExpenseRepository fixedExpenseRepository;
        private FixedExpenseHistoryRepository fixedExpenseHistoryRepository;
        private AccessControl accessControl;
        private FixedExpenseController controller;

        @BeforeEach
        void setUp() {
            fixedExpenseRepository = mock(FixedExpenseRepository.class);
            fixedExpenseHistoryRepository = mock(FixedExpenseHistoryRepository.class);
            accessControl = new AccessControl(userRepository);
            controller = new FixedExpenseController(fixedExpenseRepository, fixedExpenseHistoryRepository, accessControl);
        }

        @Test
        void rh_bloqueado() {
            Authentication auth = authFor(RH);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(403, resposta.getStatusCode().value());
        }

        @Test
        void supervisor_bloqueado() {
            Authentication auth = authFor(SUPERVISOR);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(403, resposta.getStatusCode().value(),
                    "ACHADO H2: SUPERVISOR nao deveria acessar este recurso pela API, assim como nao acessa pelo frontend");
        }

        @Test
        void cargoDesconhecido_bloqueado() {
            Authentication auth = authFor(DESCONHECIDO);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(403, resposta.getStatusCode().value());
        }

        @Test
        void financeiro_continuaFuncionando() {
            when(fixedExpenseRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(FINANCEIRO);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }

        @Test
        void admin_continuaFuncionando() {
            when(fixedExpenseRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(ADMIN);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }
    }


    @Nested
    class DespesasFixasHistorico {

        private FixedExpenseHistoryRepository fixedExpenseHistoryRepository;
        private AccessControl accessControl;
        private FixedExpenseHistoryController controller;

        @BeforeEach
        void setUp() {
            fixedExpenseHistoryRepository = mock(FixedExpenseHistoryRepository.class);
            accessControl = new AccessControl(userRepository);
            controller = new FixedExpenseHistoryController(fixedExpenseHistoryRepository, accessControl);
        }

        @Test
        void rh_bloqueado() {
            Authentication auth = authFor(RH);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(403, resposta.getStatusCode().value());
        }

        @Test
        void supervisor_bloqueado() {
            Authentication auth = authFor(SUPERVISOR);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(403, resposta.getStatusCode().value(),
                    "ACHADO H2: SUPERVISOR nao deveria acessar este recurso pela API, assim como nao acessa pelo frontend");
        }

        @Test
        void cargoDesconhecido_bloqueado() {
            Authentication auth = authFor(DESCONHECIDO);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(403, resposta.getStatusCode().value());
        }

        @Test
        void financeiro_continuaFuncionando() {
            when(fixedExpenseHistoryRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(FINANCEIRO);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }

        @Test
        void admin_continuaFuncionando() {
            when(fixedExpenseHistoryRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(ADMIN);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }
    }


    @Nested
    class DespesasVariaveis {

        private VariableExpenseRepository variableExpenseRepository;
        private AccessControl accessControl;
        private VariableExpenseController controller;

        @BeforeEach
        void setUp() {
            variableExpenseRepository = mock(VariableExpenseRepository.class);
            accessControl = new AccessControl(userRepository);
            controller = new VariableExpenseController(variableExpenseRepository, accessControl);
        }

        @Test
        void rh_bloqueado() {
            Authentication auth = authFor(RH);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(403, resposta.getStatusCode().value());
        }

        @Test
        void supervisor_bloqueado() {
            Authentication auth = authFor(SUPERVISOR);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(403, resposta.getStatusCode().value(),
                    "ACHADO H2: SUPERVISOR nao deveria acessar este recurso pela API, assim como nao acessa pelo frontend");
        }

        @Test
        void cargoDesconhecido_bloqueado() {
            Authentication auth = authFor(DESCONHECIDO);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(403, resposta.getStatusCode().value());
        }

        @Test
        void financeiro_continuaFuncionando() {
            when(variableExpenseRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(FINANCEIRO);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }

        @Test
        void admin_continuaFuncionando() {
            when(variableExpenseRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(ADMIN);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }
    }


    @Nested
    class IndicadoresRH {

        private PromoterRepository promoterRepository;
        private LojaRepository lojaRepository;
        private AccessControl accessControl;
        private HrIndicatorsController controller;

        @BeforeEach
        void setUp() {
            promoterRepository = mock(PromoterRepository.class);
            lojaRepository = mock(LojaRepository.class);
            accessControl = new AccessControl(userRepository);
            controller = new HrIndicatorsController(promoterRepository, lojaRepository, accessControl);
        }

        @Test
        void financeiro_bloqueado() {
            Authentication auth = authFor(FINANCEIRO);
            ResponseEntity<?> resposta = controller.getIndicators(LocalDate.now(), LocalDate.now(), null, auth);
            assertEquals(403, resposta.getStatusCode().value());
        }

        @Test
        void supervisor_bloqueado() {
            Authentication auth = authFor(SUPERVISOR);
            ResponseEntity<?> resposta = controller.getIndicators(LocalDate.now(), LocalDate.now(), null, auth);
            assertEquals(403, resposta.getStatusCode().value(),
                    "ACHADO H2: SUPERVISOR nao deveria acessar este recurso pela API, assim como nao acessa pelo frontend");
        }

        @Test
        void cargoDesconhecido_bloqueado() {
            Authentication auth = authFor(DESCONHECIDO);
            ResponseEntity<?> resposta = controller.getIndicators(LocalDate.now(), LocalDate.now(), null, auth);
            assertEquals(403, resposta.getStatusCode().value());
        }

        @Test
        void rh_continuaFuncionando() {
            when(promoterRepository.findAll()).thenReturn(List.of());
            when(lojaRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(RH);
            ResponseEntity<?> resposta = controller.getIndicators(LocalDate.now(), LocalDate.now(), null, auth);
            assertEquals(200, resposta.getStatusCode().value());
        }

        @Test
        void admin_continuaFuncionando() {
            when(promoterRepository.findAll()).thenReturn(List.of());
            when(lojaRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(ADMIN);
            ResponseEntity<?> resposta = controller.getIndicators(LocalDate.now(), LocalDate.now(), null, auth);
            assertEquals(200, resposta.getStatusCode().value());
        }
    }


    @Nested
    class EstoqueItens {

        private InventoryItemRepository inventoryItemRepository;
        private AccessControl accessControl;
        private InventoryItemController controller;

        @BeforeEach
        void setUp() {
            inventoryItemRepository = mock(InventoryItemRepository.class);
            accessControl = new AccessControl(userRepository);
            controller = new InventoryItemController(inventoryItemRepository, accessControl);
        }

        @Test
        void financeiro_bloqueado() {
            Authentication auth = authFor(FINANCEIRO);
            ResponseEntity<?> resposta = controller.list(auth);
            assertEquals(403, resposta.getStatusCode().value());
        }

        @Test
        void supervisor_bloqueado() {
            Authentication auth = authFor(SUPERVISOR);
            ResponseEntity<?> resposta = controller.list(auth);
            assertEquals(403, resposta.getStatusCode().value(),
                    "ACHADO H2: SUPERVISOR nao deveria acessar este recurso pela API, assim como nao acessa pelo frontend");
        }

        @Test
        void cargoDesconhecido_bloqueado() {
            Authentication auth = authFor(DESCONHECIDO);
            ResponseEntity<?> resposta = controller.list(auth);
            assertEquals(403, resposta.getStatusCode().value());
        }

        @Test
        void rh_continuaFuncionando() {
            when(inventoryItemRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(RH);
            ResponseEntity<?> resposta = controller.list(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }

        @Test
        void admin_continuaFuncionando() {
            when(inventoryItemRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(ADMIN);
            ResponseEntity<?> resposta = controller.list(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }
    }


    @Nested
    class EstoqueMovimentos {

        private StockMovementRepository stockMovementRepository;
        private InventoryItemRepository inventoryItemRepository;
        private PromoterRepository promoterRepository;
        private AccessControl accessControl;
        private StockMovementController controller;

        @BeforeEach
        void setUp() {
            stockMovementRepository = mock(StockMovementRepository.class);
            inventoryItemRepository = mock(InventoryItemRepository.class);
            promoterRepository = mock(PromoterRepository.class);
            accessControl = new AccessControl(userRepository);
            controller = new StockMovementController(stockMovementRepository, inventoryItemRepository, promoterRepository, accessControl);
        }

        @Test
        void financeiro_bloqueado() {
            Authentication auth = authFor(FINANCEIRO);
            ResponseEntity<?> resposta = controller.list(auth);
            assertEquals(403, resposta.getStatusCode().value());
        }

        @Test
        void supervisor_bloqueado() {
            Authentication auth = authFor(SUPERVISOR);
            ResponseEntity<?> resposta = controller.list(auth);
            assertEquals(403, resposta.getStatusCode().value(),
                    "ACHADO H2: SUPERVISOR nao deveria acessar este recurso pela API, assim como nao acessa pelo frontend");
        }

        @Test
        void cargoDesconhecido_bloqueado() {
            Authentication auth = authFor(DESCONHECIDO);
            ResponseEntity<?> resposta = controller.list(auth);
            assertEquals(403, resposta.getStatusCode().value());
        }

        @Test
        void rh_continuaFuncionando() {
            when(stockMovementRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(RH);
            ResponseEntity<?> resposta = controller.list(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }

        @Test
        void admin_continuaFuncionando() {
            when(stockMovementRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(ADMIN);
            ResponseEntity<?> resposta = controller.list(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }
    }


    @Nested
    class Uniformes {

        private WorkItemDeliveryRepository workItemDeliveryRepository;
        private InventoryItemRepository inventoryItemRepository;
        private PromoterRepository promoterRepository;
        private AccessControl accessControl;
        private WorkItemDeliveryController controller;

        @BeforeEach
        void setUp() {
            workItemDeliveryRepository = mock(WorkItemDeliveryRepository.class);
            inventoryItemRepository = mock(InventoryItemRepository.class);
            promoterRepository = mock(PromoterRepository.class);
            accessControl = new AccessControl(userRepository);
            controller = new WorkItemDeliveryController(workItemDeliveryRepository, inventoryItemRepository, promoterRepository, accessControl);
        }

        @Test
        void financeiro_bloqueado() {
            Authentication auth = authFor(FINANCEIRO);
            ResponseEntity<?> resposta = controller.list(auth);
            assertEquals(403, resposta.getStatusCode().value());
        }

        @Test
        void supervisor_bloqueado() {
            Authentication auth = authFor(SUPERVISOR);
            ResponseEntity<?> resposta = controller.list(auth);
            assertEquals(403, resposta.getStatusCode().value(),
                    "ACHADO H2: SUPERVISOR nao deveria acessar este recurso pela API, assim como nao acessa pelo frontend");
        }

        @Test
        void cargoDesconhecido_bloqueado() {
            Authentication auth = authFor(DESCONHECIDO);
            ResponseEntity<?> resposta = controller.list(auth);
            assertEquals(403, resposta.getStatusCode().value());
        }

        @Test
        void rh_continuaFuncionando() {
            when(workItemDeliveryRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(RH);
            ResponseEntity<?> resposta = controller.list(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }

        @Test
        void admin_continuaFuncionando() {
            when(workItemDeliveryRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(ADMIN);
            ResponseEntity<?> resposta = controller.list(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }
    }

}
