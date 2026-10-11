-- V5: perguntas selecionadas para a partida, por ronda
CREATE TABLE game_questions (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL REFERENCES games(id),
    question_id BIGINT NOT NULL REFERENCES questions(id),
    round_number INTEGER NOT NULL,
    UNIQUE(game_id, round_number)
);
