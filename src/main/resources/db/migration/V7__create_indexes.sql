-- V7: índices para as consultas quentes (ranking, histórico, monitoramento)
CREATE INDEX idx_answers_game_id     ON answers(game_id);
CREATE INDEX idx_answers_team_id     ON answers(team_id);
CREATE INDEX idx_answers_question_id ON answers(question_id);
CREATE INDEX idx_answers_answered_at ON answers(answered_at);
CREATE INDEX idx_teams_game_id       ON teams(game_id);
CREATE INDEX idx_game_questions_game ON game_questions(game_id);
CREATE INDEX idx_questions_active    ON questions(active);
