ALTER TABLE promoter ALTER COLUMN uf TYPE VARCHAR(255);
ALTER TABLE descritivo ADD COLUMN rotulo VARCHAR(255);
ALTER TABLE descritivo DROP CONSTRAINT uq_descritivo_cliente_mes_ano;
CREATE INDEX idx_descritivo_cliente_id ON descritivo (cliente_id);