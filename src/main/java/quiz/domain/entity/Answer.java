package quiz.domain.entity;

import quiz.domain.valueobject.GameId;
import quiz.domain.valueobject.QuestionId;
import quiz.domain.valueobject.TeamId;

import java.time.Instant;
import java.util.Objects;

/**
 * Resposta dada por uma equipa a uma pergunta — entidade de domínio pura.
 * Persistida na tabela answers; UNIQUE(team_id, question_id) impede duplicados.
 */
public class Answer {

    private final long id;
    private final GameId gameId;
    private final TeamId teamId;
    private final QuestionId questionId;
    private final char selectedOption;
    private final boolean correct;
    private final int pointsEarned;
    private final long responseTimeMs;
    private final Instant answeredAt;

    public Answer(long id, GameId gameId, TeamId teamId, QuestionId questionId,
                  char selectedOption, boolean correct, int pointsEarned,
                  long responseTimeMs, Instant answeredAt) {
        this.id = id;
        this.gameId = gameId;
        this.teamId = teamId;
        this.questionId = questionId;
        this.selectedOption = Character.toUpperCase(selectedOption);
        this.correct = correct;
        this.pointsEarned = pointsEarned;
        this.responseTimeMs = responseTimeMs;
        this.answeredAt = answeredAt == null ? Instant.now() : answeredAt;
    }

    public long getId() { return id; }
    public GameId getGameId() { return gameId; }
    public TeamId getTeamId() { return teamId; }
    public QuestionId getQuestionId() { return questionId; }
    public char getSelectedOption() { return selectedOption; }
    public boolean isCorrect() { return correct; }
    public int getPointsEarned() { return pointsEarned; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public Instant getAnsweredAt() { return answeredAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Answer a)) return false;
        return id == a.id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Answer{team=" + teamId + ", q=" + questionId + ", opt=" + selectedOption
                + ", correct=" + correct + ", pts=" + pointsEarned + ", " + responseTimeMs + "ms}";
    }
}
