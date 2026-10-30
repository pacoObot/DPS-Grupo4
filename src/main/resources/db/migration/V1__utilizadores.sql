CREATE TABLE utilizador (
    id UUID PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    perfil VARCHAR(20) NOT NULL CHECK (perfil IN ('ADMINISTRADOR', 'DOCENTE', 'ESTUDANTE')),
    activo BOOLEAN NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    actualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    versao_auth BIGINT NOT NULL DEFAULT 0,
    versao BIGINT NOT NULL DEFAULT 0,
    CHECK (email = LOWER(TRIM(email)))
);
CREATE TABLE bloqueio_administracao (id INTEGER PRIMARY KEY);
INSERT INTO bloqueio_administracao (id) VALUES (1);
