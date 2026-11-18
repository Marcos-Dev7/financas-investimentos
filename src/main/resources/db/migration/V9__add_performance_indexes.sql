CREATE INDEX IF NOT EXISTS idx_despesas_usuario_mes ON despesas(usuario_id, mes);
CREATE INDEX IF NOT EXISTS idx_despesas_usuario_vencimento ON despesas(usuario_id, data_vencimento);
CREATE INDEX IF NOT EXISTS idx_saldos_usuario_mes ON saldos(usuario_id, mes);
