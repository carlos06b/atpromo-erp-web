BEGIN;

-- =========================================================
-- Tabelas (sem foreign keys ainda, pra não depender de ordem)
-- =========================================================

-- ACHADO M2: as 9 tabelas abaixo (beneficiario até loja, na ordem
-- alfabética do arquivo) existem como @Entity no código Java mas não
-- existiam aqui. loja/descritivo/descritivo_linha já foram criadas em
-- produção por backend/migration_loja_postgres.sql; as 6 tabelas do
-- módulo de Extrato (empresa, conta_bancaria, beneficiario,
-- categoria_lancamento, centro_custo, lancamento_extrato) ainda não —
-- o código desse módulo está em andamento e ainda não foi commitado.
-- Este arquivo é só o bootstrap de uma instalação NOVA; ele não é
-- reaplicado sobre um banco que já existe.

CREATE TABLE beneficiario (
    id              SERIAL PRIMARY KEY,
    nome            VARCHAR(255),
    promoter_id     INTEGER,
    client_id       INTEGER,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE categoria_lancamento (
    id      SERIAL PRIMARY KEY,
    nome    VARCHAR(255),
    ativo   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE centro_custo (
    id      SERIAL PRIMARY KEY,
    nome    VARCHAR(255),
    ativo   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE client (
    id              SERIAL PRIMARY KEY,
    corporate_name  VARCHAR(150),
    name            VARCHAR(150) NOT NULL,
    cnpj            VARCHAR(20),
    phone           VARCHAR(20),
    email           VARCHAR(150),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    company_link    VARCHAR(10),
    CONSTRAINT uq_client_cnpj UNIQUE (cnpj)
);

CREATE TABLE conta_bancaria (
    id                  SERIAL PRIMARY KEY,
    empresa_id          INTEGER,
    banco               VARCHAR(255),
    apelido             VARCHAR(255),
    agencia             VARCHAR(255),
    numero_conta        VARCHAR(255),
    saldo_inicial       DECIMAL(12,2),
    data_saldo_inicial  DATE,
    ativa               BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE descritivo (
    id          SERIAL PRIMARY KEY,
    cliente_id  INTEGER NOT NULL,
    mes         INTEGER NOT NULL,
    ano         INTEGER NOT NULL,
    CONSTRAINT uq_descritivo_cliente_mes_ano UNIQUE (cliente_id, mes, ano)
);

CREATE TABLE descritivo_linha (
    id                      SERIAL PRIMARY KEY,
    descritivo_id           INTEGER NOT NULL,
    loja_id                 INTEGER NOT NULL,
    dias_semana             VARCHAR(20) NOT NULL,
    horas_por_atendimento   DECIMAL(5,2) NOT NULL,
    valor_hora              DECIMAL(12,2) NOT NULL,
    data_inicio             DATE NOT NULL,
    data_fim                DATE,
    valor_total             DECIMAL(12,2) NOT NULL,
    valor_total_manual      BOOLEAN NOT NULL DEFAULT FALSE,
    ordem                   INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE empresa (
    id      SERIAL PRIMARY KEY,
    nome    VARCHAR(255),
    cnpj    VARCHAR(255),
    ativa   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE finance_promoter (
    id                  SERIAL PRIMARY KEY,
    id_promoter         INTEGER NOT NULL,
    type                VARCHAR(30) NOT NULL,
    amount              DECIMAL(12,2) NOT NULL,
    description         TEXT,
    "date"              DATE NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    source_request_id   INTEGER
);

CREATE TABLE fixed_expense (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    amount          DECIMAL(12,2) NOT NULL,
    due_date        DATE NOT NULL,
    status          BOOLEAN NOT NULL DEFAULT FALSE,
    payment_date    DATE,
    active          BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE fixed_expense_history (
    id                  SERIAL PRIMARY KEY,
    fixed_expense_id    INTEGER NOT NULL,
    name                VARCHAR(150) NOT NULL,
    description         TEXT,
    amount              DECIMAL(12,2) NOT NULL,
    due_date            DATE NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    payment_date        DATE
);

CREATE TABLE inventory_item (
    id              SERIAL PRIMARY KEY,
    category        VARCHAR(50) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    current_stock   INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE invoice (
    id                  SERIAL PRIMARY KEY,
    id_client           INTEGER NOT NULL,
    amount              DECIMAL(12,2) NOT NULL,
    received_amount     DECIMAL(12,2),
    description         TEXT,
    due_date            DATE NOT NULL,
    issue_date          DATE,
    payment_date        DATE,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'
);

CREATE TABLE lancamento_extrato (
    id                  SERIAL PRIMARY KEY,
    conta_bancaria_id   INTEGER,
    data                DATE,
    tipo                VARCHAR(255),
    valor               DECIMAL(12,2),
    descricao           TEXT,
    origem_tipo         VARCHAR(255),
    origem_id           INTEGER,
    beneficiario_id     INTEGER,
    categoria_id        INTEGER,
    centro_custo_id     INTEGER,
    created_at          TIMESTAMP
);

CREATE TABLE loja (
    id      SERIAL PRIMARY KEY,
    nome    VARCHAR(255) NOT NULL,
    rede    VARCHAR(255),
    uf      VARCHAR(255),
    cnpj    VARCHAR(20),
    active  BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_loja_cnpj UNIQUE (cnpj)
);

CREATE TABLE password_reset_request (
    id              SERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    requested_at    TIMESTAMP NOT NULL,
    resolved        BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE promoter (
    idpromoter          SERIAL PRIMARY KEY,
    name                VARCHAR(150) NOT NULL,
    cpf                 VARCHAR(20) NOT NULL,
    phone               VARCHAR(20),
    uf                  VARCHAR(255) NOT NULL,
    city                VARCHAR(100) NOT NULL,
    company_link        VARCHAR(10),
    date_birth          DATE,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    salary              DECIMAL(12,2),
    type                VARCHAR(20),
    pix                 VARCHAR(150),
    pix_type            VARCHAR(20),
    loja_id             INTEGER,
    admission_date      DATE,
    termination_date    DATE,
    CONSTRAINT uq_promoter_cpf UNIQUE (cpf)
);

CREATE TABLE request (
    id              SERIAL PRIMARY KEY,
    id_userRH       INTEGER NOT NULL,
    id_userFIN      INTEGER,
    id_promoter     INTEGER NOT NULL,
    type            VARCHAR(30) NOT NULL,
    amount          DECIMAL(12,2) NOT NULL,
    message         TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    "date"          TIMESTAMP NOT NULL
);

CREATE TABLE stock_movement (
    id              SERIAL PRIMARY KEY,
    item_id         INTEGER NOT NULL,
    type            VARCHAR(20) NOT NULL,
    quantity        INTEGER NOT NULL,
    movement_date   DATE,
    promoter_id     INTEGER,
    observation     VARCHAR(500)
);

CREATE TABLE "user" (
    iduser      SERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    jobTittle   VARCHAR(30) NOT NULL,
    CONSTRAINT uq_user_email UNIQUE (email)
);

CREATE TABLE variable_expense (
    id                  SERIAL PRIMARY KEY,
    name                VARCHAR(150) NOT NULL,
    amount              DECIMAL(12,2) NOT NULL,
    "date"              DATE NOT NULL,
    status              BOOLEAN NOT NULL DEFAULT FALSE,
    payment_date        DATE,
    description         TEXT,
    installment_group   VARCHAR(50),
    installment_number  INTEGER,
    total_installments  INTEGER
);

CREATE TABLE work_item_delivery (
    id              SERIAL PRIMARY KEY,
    promoter_id     INTEGER NOT NULL,
    item_id         INTEGER NOT NULL,
    quantity        INTEGER NOT NULL,
    delivery_date   DATE,
    observation     VARCHAR(500)
);

-- =========================================================
-- Foreign keys (agora que todas as tabelas já existem)
-- =========================================================

ALTER TABLE beneficiario
    ADD CONSTRAINT fk_beneficiario_promoter FOREIGN KEY (promoter_id) REFERENCES promoter (idpromoter),
    ADD CONSTRAINT fk_beneficiario_client FOREIGN KEY (client_id) REFERENCES client (id);

CREATE INDEX idx_beneficiario_client_id ON beneficiario (client_id);

ALTER TABLE conta_bancaria
    ADD CONSTRAINT fk_conta_bancaria_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id);

ALTER TABLE descritivo
    ADD CONSTRAINT fk_descritivo_client FOREIGN KEY (cliente_id) REFERENCES client (id);

ALTER TABLE descritivo_linha
    ADD CONSTRAINT fk_descritivo_linha_descritivo FOREIGN KEY (descritivo_id) REFERENCES descritivo (id),
    ADD CONSTRAINT fk_descritivo_linha_loja FOREIGN KEY (loja_id) REFERENCES loja (id);

ALTER TABLE lancamento_extrato
    ADD CONSTRAINT fk_lancamento_extrato_conta FOREIGN KEY (conta_bancaria_id) REFERENCES conta_bancaria (id),
    ADD CONSTRAINT fk_lancamento_extrato_beneficiario FOREIGN KEY (beneficiario_id) REFERENCES beneficiario (id),
    ADD CONSTRAINT fk_lancamento_extrato_categoria FOREIGN KEY (categoria_id) REFERENCES categoria_lancamento (id),
    ADD CONSTRAINT fk_lancamento_extrato_centro_custo FOREIGN KEY (centro_custo_id) REFERENCES centro_custo (id);

ALTER TABLE promoter
    ADD CONSTRAINT fk_promoter_loja FOREIGN KEY (loja_id) REFERENCES loja (id);

ALTER TABLE finance_promoter
    ADD CONSTRAINT fk_fp_promoter FOREIGN KEY (id_promoter) REFERENCES promoter (idpromoter);

ALTER TABLE fixed_expense_history
    ADD CONSTRAINT fk_feh_fixed_expense FOREIGN KEY (fixed_expense_id) REFERENCES fixed_expense (id);

ALTER TABLE invoice
    ADD CONSTRAINT fk_invoice_client FOREIGN KEY (id_client) REFERENCES client (id);

ALTER TABLE request
    ADD CONSTRAINT fk_request_promoter FOREIGN KEY (id_promoter) REFERENCES promoter (idpromoter),
    ADD CONSTRAINT fk_request_user_fin FOREIGN KEY (id_userFIN) REFERENCES "user" (iduser),
    ADD CONSTRAINT fk_request_user_rh FOREIGN KEY (id_userRH) REFERENCES "user" (iduser);

ALTER TABLE stock_movement
    ADD CONSTRAINT fk_stock_movement_item FOREIGN KEY (item_id) REFERENCES inventory_item (id),
    ADD CONSTRAINT fk_stock_movement_promoter FOREIGN KEY (promoter_id) REFERENCES promoter (idpromoter);

ALTER TABLE work_item_delivery
    ADD CONSTRAINT fk_work_item_delivery_item FOREIGN KEY (item_id) REFERENCES inventory_item (id),
    ADD CONSTRAINT fk_work_item_delivery_promoter FOREIGN KEY (promoter_id) REFERENCES promoter (idpromoter);

-- =========================================================
-- Usuário admin inicial — SEM senha fixa commitada neste arquivo
-- (achado C1 da auditoria de 05/10/2026: havia uma senha de teste em
-- texto claro aqui, num repositório público, usada pra popular o banco
-- de produção no Render).
--
-- Depois de rodar este script num banco novo, crie o admin manualmente:
--   1. Gere um hash bcrypt (custo 12) da senha que você vai usar de verdade.
--   2. Rode, substituindo <SEU_EMAIL> e <HASH_BCRYPT_AQUI>:
--        INSERT INTO "user" (name, email, password, jobTittle)
--        VALUES ('Admin', '<SEU_EMAIL>', '<HASH_BCRYPT_AQUI>', 'ADMIN');
--   3. Nunca commite esse INSERT com o hash real preenchido.
--
-- AÇÃO NECESSÁRIA SUA: se este script já rodou em produção com o admin de
-- teste antigo (admin@atpromo.com), troque a senha dessa conta
-- agora mesmo — eu não tenho (e não devo ter) acesso ao banco de produção
-- pra fazer isso por você.
-- =========================================================

COMMIT;
