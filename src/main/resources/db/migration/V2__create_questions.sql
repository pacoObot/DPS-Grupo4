-- V2: banco de perguntas (administráveis pelo Admin)
CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    question_text TEXT NOT NULL,
    category VARCHAR(100),
    difficulty VARCHAR(30) NOT NULL,
    option_a TEXT NOT NULL,
    option_b TEXT NOT NULL,
    option_c TEXT NOT NULL,
    option_d TEXT NOT NULL,
    correct_option CHAR(1) NOT NULL CHECK (correct_option IN ('A','B','C','D')),
    points INTEGER NOT NULL DEFAULT 10 CHECK (points > 0),
    time_limit INTEGER NOT NULL DEFAULT 15 CHECK (time_limit > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
