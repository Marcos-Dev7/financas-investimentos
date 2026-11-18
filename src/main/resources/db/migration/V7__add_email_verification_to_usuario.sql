ALTER TABLE usuario ADD COLUMN IF NOT EXISTS email_verificado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS codigo_verificacao VARCHAR(10);
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS codigo_expiracao TIMESTAMP;

UPDATE usuario SET email_verificado = TRUE WHERE codigo_verificacao IS NULL;
