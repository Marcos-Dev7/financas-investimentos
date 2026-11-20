ALTER TABLE membro_familia
    ADD COLUMN IF NOT EXISTS titular_compartilha_despesas BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS membro_compartilha_despesas BOOLEAN NOT NULL DEFAULT TRUE;
