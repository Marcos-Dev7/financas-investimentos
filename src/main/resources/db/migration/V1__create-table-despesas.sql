CREATE TABLE despesas
(
    id              BIGSERIAL PRIMARY KEY,
    descricao       VARCHAR(255)   NOT NULL,
    valor           DECIMAL(10, 2) NOT NULL,
    data_vencimento DATE,
    mes             VARCHAR(20)    NOT NULL,
    is_paga         BOOLEAN DEFAULT FALSE
);