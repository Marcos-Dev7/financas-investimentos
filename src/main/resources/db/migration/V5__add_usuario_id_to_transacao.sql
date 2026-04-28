ALTER TABLE transacao ADD COLUMN usuario_id BIGINT NOT NULL;
ALTER TABLE transacao ADD CONSTRAINT fk_transacao_usuario_id FOREIGN KEY (usuario_id) REFERENCES usuario(id);