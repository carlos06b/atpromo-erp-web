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
ACHADO H2 (CORRIGIDO) - parte referente ao modulo Extrato: mesmo bug e
 * mesma correcao descritos em H2AllowListAccessTest.java (deny-list de 1
 * papel -> allow-list explicito incluindo ADMIN, agora bloqueando SUPERVISOR
 * e cargo desconhecido tambem). Este arquivo cobre os 5 controllers do
 * modulo Extrato (contas bancarias, empresas, beneficiarios, categorias de
 * lancamento, centros de custo) mais o proprio LancamentoExtratoController -
 * todos ainda SEM COMMIT porque fazem parte da sua feature "Extrato" em
 * andamento, criada antes desta sessao. A correcao de permissao ja esta
 * aplicada nesses arquivos e sera incluida automaticamente no commit que
 * voce fizer da feature Extrato; nao commitei esses arquivos de producao
 * junto com o resto do achado H2 para nao misturar o fix de seguranca com
 * uma feature sua ainda nao finalizada (mesma decisao tomada para
 * frontend/src/pages/Extrato.jsx no achado C6).
 */
class H2AllowListAccessExtratoFeatureTest {

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
    class ContasBancarias {

        private ContaBancariaRepository contaBancariaRepository;
        private EmpresaRepository empresaRepository;
        private LancamentoExtratoRepository lancamentoExtratoRepository;
        private AccessControl accessControl;
        private ContaBancariaController controller;

        @BeforeEach
        void setUp() {
            contaBancariaRepository = mock(ContaBancariaRepository.class);
            empresaRepository = mock(EmpresaRepository.class);
            lancamentoExtratoRepository = mock(LancamentoExtratoRepository.class);
            accessControl = new AccessControl(userRepository);
            controller = new ContaBancariaController(contaBancariaRepository, empresaRepository, lancamentoExtratoRepository, accessControl);
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
            when(contaBancariaRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(FINANCEIRO);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }

        @Test
        void admin_continuaFuncionando() {
            when(contaBancariaRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(ADMIN);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }
    }


    @Nested
    class Empresas {

        private EmpresaRepository empresaRepository;
        private ContaBancariaRepository contaBancariaRepository;
        private AccessControl accessControl;
        private EmpresaController controller;

        @BeforeEach
        void setUp() {
            empresaRepository = mock(EmpresaRepository.class);
            contaBancariaRepository = mock(ContaBancariaRepository.class);
            accessControl = new AccessControl(userRepository);
            controller = new EmpresaController(empresaRepository, contaBancariaRepository, accessControl);
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
            when(empresaRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(FINANCEIRO);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }

        @Test
        void admin_continuaFuncionando() {
            when(empresaRepository.findAll()).thenReturn(List.of());
            Authentication auth = authFor(ADMIN);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }
    }


    @Nested
    class Beneficiarios {

        private BeneficiarioRepository beneficiarioRepository;
        private LancamentoExtratoRepository lancamentoExtratoRepository;
        private AccessControl accessControl;
        private BeneficiarioController controller;

        @BeforeEach
        void setUp() {
            beneficiarioRepository = mock(BeneficiarioRepository.class);
            lancamentoExtratoRepository = mock(LancamentoExtratoRepository.class);
            accessControl = new AccessControl(userRepository);
            controller = new BeneficiarioController(beneficiarioRepository, lancamentoExtratoRepository, accessControl);
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
            when(beneficiarioRepository.findAll()).thenReturn(new ArrayList<>());
            Authentication auth = authFor(FINANCEIRO);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }

        @Test
        void admin_continuaFuncionando() {
            when(beneficiarioRepository.findAll()).thenReturn(new ArrayList<>());
            Authentication auth = authFor(ADMIN);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }
    }


    @Nested
    class CategoriasLancamento {

        private CategoriaLancamentoRepository categoriaLancamentoRepository;
        private LancamentoExtratoRepository lancamentoExtratoRepository;
        private AccessControl accessControl;
        private CategoriaLancamentoController controller;

        @BeforeEach
        void setUp() {
            categoriaLancamentoRepository = mock(CategoriaLancamentoRepository.class);
            lancamentoExtratoRepository = mock(LancamentoExtratoRepository.class);
            accessControl = new AccessControl(userRepository);
            controller = new CategoriaLancamentoController(categoriaLancamentoRepository, lancamentoExtratoRepository, accessControl);
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
            when(categoriaLancamentoRepository.findAll()).thenReturn(new ArrayList<>());
            Authentication auth = authFor(FINANCEIRO);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }

        @Test
        void admin_continuaFuncionando() {
            when(categoriaLancamentoRepository.findAll()).thenReturn(new ArrayList<>());
            Authentication auth = authFor(ADMIN);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }
    }


    @Nested
    class CentrosCusto {

        private CentroCustoRepository centroCustoRepository;
        private LancamentoExtratoRepository lancamentoExtratoRepository;
        private AccessControl accessControl;
        private CentroCustoController controller;

        @BeforeEach
        void setUp() {
            centroCustoRepository = mock(CentroCustoRepository.class);
            lancamentoExtratoRepository = mock(LancamentoExtratoRepository.class);
            accessControl = new AccessControl(userRepository);
            controller = new CentroCustoController(centroCustoRepository, lancamentoExtratoRepository, accessControl);
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
            when(centroCustoRepository.findAll()).thenReturn(new ArrayList<>());
            Authentication auth = authFor(FINANCEIRO);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }

        @Test
        void admin_continuaFuncionando() {
            when(centroCustoRepository.findAll()).thenReturn(new ArrayList<>());
            Authentication auth = authFor(ADMIN);
            ResponseEntity<?> resposta = controller.listAll(auth);
            assertEquals(200, resposta.getStatusCode().value());
        }
    }


    @Nested
    class ExtratoLancamentos {

        private LancamentoExtratoRepository lancamentoExtratoRepository;
        private ContaBancariaRepository contaBancariaRepository;
        private BeneficiarioRepository beneficiarioRepository;
        private CategoriaLancamentoRepository categoriaLancamentoRepository;
        private CentroCustoRepository centroCustoRepository;
        private FinancePromoterRepository financePromoterRepository;
        private PromoterRepository promoterRepository;
        private FixedExpenseHistoryRepository fixedExpenseHistoryRepository;
        private VariableExpenseRepository variableExpenseRepository;
        private InvoiceRepository invoiceRepository;
        private ClientRepository clientRepository;
        private AccessControl accessControl;
        private LancamentoExtratoController controller;

        @BeforeEach
        void setUp() {
            lancamentoExtratoRepository = mock(LancamentoExtratoRepository.class);
            contaBancariaRepository = mock(ContaBancariaRepository.class);
            beneficiarioRepository = mock(BeneficiarioRepository.class);
            categoriaLancamentoRepository = mock(CategoriaLancamentoRepository.class);
            centroCustoRepository = mock(CentroCustoRepository.class);
            financePromoterRepository = mock(FinancePromoterRepository.class);
            promoterRepository = mock(PromoterRepository.class);
            fixedExpenseHistoryRepository = mock(FixedExpenseHistoryRepository.class);
            variableExpenseRepository = mock(VariableExpenseRepository.class);
            invoiceRepository = mock(InvoiceRepository.class);
            clientRepository = mock(ClientRepository.class);
            accessControl = new AccessControl(userRepository);
            controller = new LancamentoExtratoController(lancamentoExtratoRepository, contaBancariaRepository, beneficiarioRepository, categoriaLancamentoRepository, centroCustoRepository, financePromoterRepository, promoterRepository, fixedExpenseHistoryRepository, variableExpenseRepository, invoiceRepository, clientRepository, accessControl);
        }

        @Test
        void rh_bloqueado() {
            Authentication auth = authFor(RH);
            ResponseEntity<?> resposta = controller.listByConta(1, auth);
            assertEquals(403, resposta.getStatusCode().value());
        }

        @Test
        void supervisor_bloqueado() {
            Authentication auth = authFor(SUPERVISOR);
            ResponseEntity<?> resposta = controller.listByConta(1, auth);
            assertEquals(403, resposta.getStatusCode().value(),
                    "ACHADO H2: SUPERVISOR nao deveria acessar este recurso pela API, assim como nao acessa pelo frontend");
        }

        @Test
        void cargoDesconhecido_bloqueado() {
            Authentication auth = authFor(DESCONHECIDO);
            ResponseEntity<?> resposta = controller.listByConta(1, auth);
            assertEquals(403, resposta.getStatusCode().value());
        }

        @Test
        void financeiro_continuaFuncionando() {
            ContaBancaria contaMock = mock(ContaBancaria.class);
            when(contaMock.getSaldoInicial()).thenReturn(BigDecimal.ZERO);
            when(contaBancariaRepository.findById(1)).thenReturn(Optional.of(contaMock));
            when(lancamentoExtratoRepository.findByContaBancariaId(1)).thenReturn(new ArrayList<>());
            Authentication auth = authFor(FINANCEIRO);
            ResponseEntity<?> resposta = controller.listByConta(1, auth);
            assertEquals(200, resposta.getStatusCode().value());
        }

        @Test
        void admin_continuaFuncionando() {
            ContaBancaria contaMock = mock(ContaBancaria.class);
            when(contaMock.getSaldoInicial()).thenReturn(BigDecimal.ZERO);
            when(contaBancariaRepository.findById(1)).thenReturn(Optional.of(contaMock));
            when(lancamentoExtratoRepository.findByContaBancariaId(1)).thenReturn(new ArrayList<>());
            Authentication auth = authFor(ADMIN);
            ResponseEntity<?> resposta = controller.listByConta(1, auth);
            assertEquals(200, resposta.getStatusCode().value());
        }
    }

}
