CREATE TABLE usuario(
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(50)  UNIQUE NOT NULL,
    senha VARCHAR(255) NOT NULL
);

CREATE TABLE ativo(
    id BIGSERIAL PRIMARY KEY,
    ticker VARCHAR(10) UNIQUE NOT NULL,
    tipo_ativo VARCHAR(20) NOT NULL,
    quantidade_total INTEGER DEFAULT 0,
    preco_medio DECIMAL(18, 2) DEFAULT 0.00,
    usuario_id BIGINT NOT NULL,
    CONSTRAINT fk_usuario_id FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE TABLE transacao(
    id BIGSERIAL PRIMARY KEY,
    tipo_operacao VARCHAR(20) NOT NULL,
    quantidade INTEGER DEFAULT 0,
    preco_unitario DECIMAL(18, 2) DEFAULT 0.00,
    taxa DECIMAL(18, 2) DEFAULT 0.00,
    data_execucao DATE,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ativo_id BIGINT NOT NULL,
    CONSTRAINT fk_transacao_ativo_id FOREIGN KEY (ativo_id) REFERENCES ativo(id)
);

CREATE TABLE dividendo(
    id BIGSERIAL PRIMARY KEY,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    valor DECIMAL(18,2) DEFAULT 0.00,
    usuario_id BIGINT NOT NULL,
    ativo_id BIGINT NOT NULL,
    CONSTRAINT fk_dividendo_usuario_id FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_dividendo_ativo_id FOREIGN KEY (ativo_id) REFERENCES ativo(id)
);
