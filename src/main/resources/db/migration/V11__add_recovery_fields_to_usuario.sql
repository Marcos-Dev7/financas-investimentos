ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS telefone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS email_recuperacao VARCHAR(255),
    ADD COLUMN IF NOT EXISTS token_recuperacao VARCHAR(100),
    ADD COLUMN IF NOT EXISTS token_recuperacao_expiracao TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_usuario_telefone ON usuario (telefone);
CREATE INDEX IF NOT EXISTS idx_usuario_email_recuperacao ON usuario (email_recuperacao);
CREATE INDEX IF NOT EXISTS idx_usuario_token_recuperacao ON usuario (token_recuperacao);
