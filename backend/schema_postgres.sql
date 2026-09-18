-- Schema do banco systematpromo, traduzido de MySQL para PostgreSQL.
-- Gerado a partir do dump enviado pelo Carlos em 18/09/2026.
-- Rodar isso inteiro UMA VEZ no banco Postgres novo (vazio) do Render,
-- antes do backend subir pela primeira vez (o app usa
-- spring.jpa.hibernate.ddl-auto=validate, ou seja, ele espera que as
-- tabelas já existam -- ele não cria nada sozinho).

BEGIN;

-- =========================================================
-- Tabelas (sem foreign keys ainda, pra não depender de ordem)
-- =========================================================

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
    uf                  CHAR(2) NOT NULL,
    city                VARCHAR(100) NOT NULL,
    company_link        VARCHAR(10),
    date_birth          DATE,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    salary              DECIMAL(12,2),
    type                VARCHAR(20),
    pix                 VARCHAR(150),
    pix_type            VARCHAR(20),
    store               VARCHAR(255),
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
-- Usuário admin de teste, pra você conseguir logar assim que
-- o backend subir no Render (banco novo = zero usuários).
-- login: admin@atpromo.com   |   senha: Teste123!
-- (troca a senha depois, isso é só pra testar o deploy)
-- =========================================================

INSERT INTO "user" (name, email, password, jobTittle)
VALUES (
    'Admin Teste',
    'admin@atpromo.com',
    '$2b$12$rXOaEBy6d5wCkCjrUILzJubM/LehN8Sp5MdDn96cdzOOsUHzWN7mq',
    'ADMIN'
);

COMMIT;
