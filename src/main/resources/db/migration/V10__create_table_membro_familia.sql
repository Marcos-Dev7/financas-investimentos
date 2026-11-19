CREATE TABLE membro_familia (
    id BIGSERIAL PRIMARY KEY,
    titular_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    membro_id BIGINT REFERENCES usuario(id) ON DELETE CASCADE,
    email_convidado VARCHAR(100) NOT NULL,
    token_convite VARCHAR(64) UNIQUE NOT NULL,
    data_expiracao TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    titular_compartilha_renda BOOLEAN NOT NULL DEFAULT TRUE,
    membro_compartilha_renda BOOLEAN NOT NULL DEFAULT FALSE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_membro_familia_titular ON membro_familia(titular_id);
CREATE INDEX IF NOT EXISTS idx_membro_familia_membro ON membro_familia(membro_id);
CREATE INDEX IF NOT EXISTS idx_membro_familia_token ON membro_familia(token_convite);
