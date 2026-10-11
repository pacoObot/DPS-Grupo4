-- V6: respostas — cada linha é a prova persistida de uma decisão concorrente
CREATE TABLE answers (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL REFERENCES games(id),
    team_id BIGINT NOT NULL REFERENCES teams(id),
    question_id BIGINT NOT NULL REFERENCES questions(id),
    selected_option CHAR(1) NOT NULL,
    is_correct BOOLEAN NOT NULL,
    points_earned INTEGER NOT NULL,
    response_time_ms BIGINT,
    answered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- uma resposta por equipa por pergunta: bloqueia double submission
    UNIQUE(team_id, question_id)
);
