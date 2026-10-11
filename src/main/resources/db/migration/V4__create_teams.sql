-- V4: equipes (uma linha por equipa inscrita na partida)
CREATE TABLE teams (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL REFERENCES games(id),
    name VARCHAR(100) NOT NULL,
    score INTEGER NOT NULL DEFAULT 0,
    correct_count INTEGER NOT NULL DEFAULT 0,
    wrong_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    connected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    disconnected_at TIMESTAMP,
    -- impede equipes duplicadas dentro da mesma partida
    UNIQUE(game_id, name)
);
