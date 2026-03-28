CREATE TABLE saldos
(
    id              BIGSERIAL PRIMARY KEY,
    descricao       VARCHAR(100) NOT NULL,
    valor           DECIMAL(10, 2) NOT NULL,
    mes             VARCHAR(20)    NOT NULL,
    ano             INTEGER        NOT NULL
);