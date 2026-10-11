package quiz.domain.entity;

import quiz.domain.valueobject.GameId;
import quiz.domain.valueobject.QuestionId;

import java.util.Objects;

/**
 * Ronda de uma partida: qual pergunta aparece em que número de ronda.
 * Corresponde à tabela game_questions.
 */
public class GameRound {

    private final long id;
    private final GameId gameId;
    private final QuestionId questionId;
    private final int roundNumber;

    public GameRound(long id, GameId gameId, QuestionId questionId, int roundNumber) {
        this.id = id;
        this.gameId = gameId;
        this.questionId = questionId;
        this.roundNumber = roundNumber;
    }

    public long getId() { return id; }
    public GameId getGameId() { return gameId; }
    public QuestionId getQuestionId() { return questionId; }
    public int getRoundNumber() { return roundNumber; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameRound r)) return false;
        return id == r.id && roundNumber == r.roundNumber;
    }

    @Override
    public int hashCode() { return Objects.hash(id, roundNumber); }

    @Override
    public String toString() {
        return "Round " + roundNumber + " -> question " + questionId;
    }
}
