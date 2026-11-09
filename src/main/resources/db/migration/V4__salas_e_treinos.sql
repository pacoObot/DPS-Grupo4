CREATE TABLE sala_teste (
    codigo VARCHAR(16) PRIMARY KEY,
    docente_id UUID NOT NULL REFERENCES utilizador(id),
    teste_id UUID NOT NULL REFERENCES teste(id),
    nome VARCHAR(100) NOT NULL,
    titulo_teste VARCHAR(150) NOT NULL,
    total_perguntas INTEGER NOT NULL,
    segundos_por_pergunta INTEGER NOT NULL,
    snapshot TEXT NOT NULL,
    criada_em TIMESTAMP WITH TIME ZONE NOT NULL,
    fechada_em TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_sala_docente ON sala_teste(docente_id, criada_em);
CREATE TABLE participante_sala (
    id UUID PRIMARY KEY,
    codigo_sala VARCHAR(16) NOT NULL REFERENCES sala_teste(codigo),
    nome VARCHAR(100) NOT NULL,
    identificacao VARCHAR(40) NOT NULL,
    identificacao_chave VARCHAR(100) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    dispositivo VARCHAR(20) NOT NULL CHECK(dispositivo IN ('COMPUTADOR','TELEMOVEL')),
    cursor_x INTEGER, cursor_y INTEGER,
    estado VARCHAR(20) NOT NULL CHECK(estado IN ('PENDENTE','APROVADO','RECUSADO','FECHADO')),
    entrou_em TIMESTAMP WITH TIME ZONE NOT NULL,
    visto_em TIMESTAMP WITH TIME ZONE NOT NULL,
    pergunta_iniciada_em TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_sala_identificacao UNIQUE(codigo_sala, identificacao_chave)
);
CREATE INDEX idx_participante_sala ON participante_sala(codigo_sala,entrou_em);
CREATE TABLE resposta_participante (
    participante_id UUID NOT NULL REFERENCES participante_sala(id),
    ordem INTEGER NOT NULL,
    resposta VARCHAR(1),
    correcta BOOLEAN NOT NULL,
    respondida_em TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY(participante_id,ordem)
);
CREATE TABLE tentativa_pratica (
    id UUID PRIMARY KEY,
    utilizador_id UUID NOT NULL REFERENCES utilizador(id),
    teste_id UUID NOT NULL REFERENCES teste(id),
    titulo VARCHAR(150) NOT NULL,
    total_perguntas INTEGER NOT NULL,
    correctas INTEGER NOT NULL,
    nota_percentual DOUBLE PRECISION NOT NULL,
    realizada_em TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_treino_utilizador ON tentativa_pratica(utilizador_id,realizada_em);
