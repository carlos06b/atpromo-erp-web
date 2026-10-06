-- Nova migração: adiciona o vínculo "beneficiário <-> cliente", usado pelo
-- lançamento automático de Faturamento no extrato (mesma lógica que já existe
-- para promoter_id no Pix: acha/reaproveita o beneficiário do cliente, ou cria
-- um novo na hora). Rode isso DEPOIS da migração anterior
-- (migration-beneficiario-categoria-centrocusto.sql), que já deve estar aplicada.

ALTER TABLE beneficiario
  ADD COLUMN client_id INT DEFAULT NULL AFTER promoter_id;

ALTER TABLE beneficiario
  ADD KEY idx_beneficiario_client_id (client_id);
