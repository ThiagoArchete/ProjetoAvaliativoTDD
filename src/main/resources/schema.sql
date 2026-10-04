CREATE TABLE IF NOT EXISTS tarefa (
    id            BIGSERIAL PRIMARY KEY,
    nome          VARCHAR(255) NOT NULL,
    descricao     TEXT,
    status        VARCHAR(20),
    observacoes   TEXT,
    data_criacao  DATE,
    data_atualizacao DATE
);
