BEGIN;

CREATE TABLE loja (
                      id      SERIAL PRIMARY KEY,
                      nome    VARCHAR(255) NOT NULL,
                      uf      VARCHAR(255),
                      cnpj    VARCHAR(20),
                      active  BOOLEAN NOT NULL DEFAULT TRUE,
                      CONSTRAINT uq_loja_cnpj UNIQUE (cnpj)
);

ALTER TABLE promoter ADD COLUMN loja_id INTEGER;

INSERT INTO loja (nome, active)
SELECT DISTINCT store, TRUE
FROM promoter
WHERE store IS NOT NULL AND btrim(store) <> '';

UPDATE promoter
SET loja_id = loja.id
    FROM loja
WHERE loja.nome = promoter.store;

ALTER TABLE promoter
    ADD CONSTRAINT fk_promoter_loja FOREIGN KEY (loja_id) REFERENCES loja (id);

ALTER TABLE promoter DROP COLUMN store;

ALTER TABLE loja ADD COLUMN rede VARCHAR(255);

COMMIT;

BEGIN;

CREATE TABLE descritivo (
                            id          SERIAL PRIMARY KEY,
                            cliente_id  INTEGER NOT NULL REFERENCES client (id),
                            mes         INTEGER NOT NULL,
                            ano         INTEGER NOT NULL,
                            CONSTRAINT uq_descritivo_cliente_mes_ano UNIQUE (cliente_id, mes, ano)
);

CREATE TABLE descritivo_linha (
                                  id                      SERIAL PRIMARY KEY,
                                  descritivo_id           INTEGER NOT NULL REFERENCES descritivo (id),
                                  loja_id                 INTEGER NOT NULL REFERENCES loja (id),
                                  dias_semana             VARCHAR(20) NOT NULL,
                                  horas_por_atendimento   DECIMAL(5,2) NOT NULL,
                                  valor_hora              DECIMAL(12,2) NOT NULL,
                                  data_inicio             DATE NOT NULL,
                                  data_fim                DATE,
                                  valor_total             DECIMAL(12,2) NOT NULL,
                                  valor_total_manual      BOOLEAN NOT NULL DEFAULT FALSE,
                                  ordem                   INTEGER NOT NULL DEFAULT 0
);

COMMIT;