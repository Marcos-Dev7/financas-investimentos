ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS dois_fatores_ativo BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS tipo_dois_fatores VARCHAR(20),
    ADD COLUMN IF NOT EXISTS segredo_totp VARCHAR(64),
    ADD COLUMN IF NOT EXISTS segredo_totp_pendente VARCHAR(64),
    ADD COLUMN IF NOT EXISTS desafio_2fa_token VARCHAR(100),
    ADD COLUMN IF NOT EXISTS codigo_2fa VARCHAR(10),
    ADD COLUMN IF NOT EXISTS codigo_2fa_expiracao TIMESTAMP,
    ADD COLUMN IF NOT EXISTS tentativas_2fa INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_usuario_desafio_2fa_token ON usuario (desafio_2fa_token);
