-- V3: partidas
-- join_code: código que os jogadores digitam para entrar (extensão prática da spec)
-- round_started_at: o SERVIDOR é a autoridade sobre o tempo (deadline da ronda)
CREATE TABLE games (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    join_code VARCHAR(10) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    total_rounds INTEGER NOT NULL,
    current_round INTEGER NOT NULL DEFAULT 0,
    time_per_question INTEGER NOT NULL,
    round_started_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    finished_at TIMESTAMP
);
