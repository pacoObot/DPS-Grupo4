CREATE TABLE teste (
    id UUID PRIMARY KEY,
    docente_id UUID NOT NULL REFERENCES utilizador(id),
    titulo VARCHAR(150) NOT NULL,
    descricao VARCHAR(2000) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    actualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_teste_docente ON teste(docente_id, actualizado_em);
CREATE TABLE teste_questao (
    teste_id UUID NOT NULL REFERENCES teste(id),
    ordem INTEGER NOT NULL,
    enunciado VARCHAR(2000) NOT NULL,
    alternativa_a VARCHAR(500) NOT NULL,
    alternativa_b VARCHAR(500) NOT NULL,
    alternativa_c VARCHAR(500) NOT NULL,
    alternativa_d VARCHAR(500) NOT NULL,
    correcta VARCHAR(1) NOT NULL CHECK (correcta IN ('A', 'B', 'C', 'D')),
    categoria VARCHAR(100) NOT NULL,
    PRIMARY KEY (teste_id, ordem)
);
CREATE TABLE teste_revisao (
    id UUID PRIMARY KEY,
    teste_id UUID NOT NULL REFERENCES teste(id),
    versao BIGINT NOT NULL,
    guardado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    conteudo TEXT NOT NULL,
    UNIQUE (teste_id, versao)
);
