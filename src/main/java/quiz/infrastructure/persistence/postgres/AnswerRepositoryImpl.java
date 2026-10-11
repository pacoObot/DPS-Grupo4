package quiz.infrastructure.persistence.postgres;

import quiz.application.port.output.AnswerRepository;
import quiz.domain.entity.Answer;
import quiz.domain.valueobject.GameId;
import quiz.domain.valueobject.QuestionId;
import quiz.domain.valueobject.TeamId;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** PostgreSQL: implementação de AnswerRepository. */
public final class AnswerRepositoryImpl implements AnswerRepository {

    private static final String COLS =
            "id, game_id, team_id, question_id, selected_option, is_correct, " +
            "points_earned, response_time_ms, answered_at";

    private final Jdbc jdbc;

    public AnswerRepositoryImpl(Jdbc jdbc) {
        this.jdbc = jdbc;
    }

    private Answer map(ResultSet rs) throws SQLException {
        return new Answer(
                rs.getLong("id"),
                new GameId(rs.getLong("game_id")),
                new TeamId(rs.getLong("team_id")),
                new QuestionId(rs.getLong("question_id")),
                rs.getString("selected_option").charAt(0),
                rs.getBoolean("is_correct"),
                rs.getInt("points_earned"),
                rs.getLong("response_time_ms"),
                rs.getTimestamp("answered_at").toInstant());
    }

    @Override
    public Answer save(Answer a) {
        long id = jdbc.insertReturningId(
                "INSERT INTO answers (game_id, team_id, question_id, selected_option, is_correct, " +
                "points_earned, response_time_ms, answered_at) VALUES (?,?,?,?,?,?,?,?) RETURNING id",
                a.getGameId().value(), a.getTeamId().value(), a.getQuestionId().value(),
                String.valueOf(a.getSelectedOption()), a.isCorrect(), a.getPointsEarned(),
                a.getResponseTimeMs(), a.getAnsweredAt());
        return new Answer(id, a.getGameId(), a.getTeamId(), a.getQuestionId(),
                a.getSelectedOption(), a.isCorrect(), a.getPointsEarned(),
                a.getResponseTimeMs(), a.getAnsweredAt());
    }

    @Override
    public boolean existsByTeamAndQuestion(long teamId, long questionId) {
        return jdbc.queryOne("SELECT id FROM answers WHERE team_id = ? AND question_id = ?",
                rs -> rs.getLong("id"), teamId, questionId).isPresent();
    }

    @Override
    public boolean hasAnsweredRound(long teamId, long questionId) {
        return existsByTeamAndQuestion(teamId, questionId);
    }

    @Override
    public List<Answer> findByGame(long gameId) {
        return jdbc.query("SELECT " + COLS + " FROM answers WHERE game_id = ? ORDER BY answered_at",
                this::map, gameId);
    }

    @Override
    public long countByGame(long gameId) {
        return jdbc.queryOne("SELECT COUNT(*) AS n FROM answers WHERE game_id = ?",
                rs -> rs.getLong("n"), gameId).orElse(0L);
    }
}
