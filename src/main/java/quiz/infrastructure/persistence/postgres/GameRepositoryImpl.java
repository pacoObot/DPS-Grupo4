package quiz.infrastructure.persistence.postgres;

import quiz.application.port.output.GameRepository;
import quiz.domain.entity.Game;
import quiz.domain.enums.GameStatus;
import quiz.domain.valueobject.GameId;
import quiz.domain.valueobject.QuestionId;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** PostgreSQL: implementação de GameRepository (+ game_questions). */
public final class GameRepositoryImpl implements GameRepository {

    private static final String COLS =
            "id, name, join_code, status, total_rounds, current_round, time_per_question, " +
            "round_started_at, created_at, started_at, finished_at";

    private final Jdbc jdbc;

    public GameRepositoryImpl(Jdbc jdbc) {
        this.jdbc = jdbc;
    }

    private Instant ts(ResultSet rs, String col) throws SQLException {
        return rs.getTimestamp(col) == null ? null : rs.getTimestamp(col).toInstant();
    }

    private Game map(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        return new Game(
                new GameId(id),
                rs.getString("name"),
                rs.getString("join_code"),
                GameStatus.valueOf(rs.getString("status")),
                rs.getInt("total_rounds"),
                rs.getInt("current_round"),
                rs.getInt("time_per_question"),
                ts(rs, "round_started_at"),
                ts(rs, "created_at"),
                ts(rs, "started_at"),
                ts(rs, "finished_at"),
                loadQuestionIds(id));
    }

    private List<QuestionId> loadQuestionIds(long gameId) {
        List<QuestionId> ids = new ArrayList<>();
        for (QuestionId qid : jdbc.query(
                "SELECT question_id FROM game_questions WHERE game_id = ? ORDER BY round_number",
                rs -> new QuestionId(rs.getLong("question_id")), gameId)) {
            ids.add(qid);
        }
        return ids;
    }

    @Override
    public Game save(Game g) {
        long id = jdbc.insertReturningId(
                "INSERT INTO games (name, join_code, status, total_rounds, current_round, " +
                "time_per_question, round_started_at, created_at, started_at, finished_at) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?) RETURNING id",
                g.getName(), g.getJoinCode(), g.getStatus(), g.getTotalRounds(), g.getCurrentRound(),
                g.getTimePerQuestion(), g.getRoundStartedAt(), g.getCreatedAt(),
                g.getStartedAt(), g.getFinishedAt());
        return new Game(new GameId(id), g.getName(), g.getJoinCode(), g.getStatus(),
                g.getTotalRounds(), g.getCurrentRound(), g.getTimePerQuestion(),
                g.getRoundStartedAt(), g.getCreatedAt(), g.getStartedAt(), g.getFinishedAt(),
                g.getQuestionIds());
    }

    @Override
    public Optional<Game> findById(long id) {
        return jdbc.queryOne("SELECT " + COLS + " FROM games WHERE id = ?", this::map, id);
    }

    @Override
    public Optional<Game> findByCode(String joinCode) {
        return jdbc.queryOne("SELECT " + COLS + " FROM games WHERE join_code = ?", this::map, joinCode);
    }

    @Override
    public List<Game> findAll() {
        return jdbc.query("SELECT " + COLS + " FROM games ORDER BY id DESC", this::map);
    }

    @Override
    public Optional<Game> lockById(long id) {
        // FOR UPDATE: transições de estado (start/pause/resume/round) são
        // serializadas por partida — impede start duplo sob concorrência.
        return jdbc.queryOne("SELECT " + COLS + " FROM games WHERE id = ? FOR UPDATE", this::map, id);
    }

    @Override
    public void update(Game g) {
        jdbc.update(
                "UPDATE games SET name=?, status=?, total_rounds=?, current_round=?, " +
                "time_per_question=?, round_started_at=?, started_at=?, finished_at=? WHERE id=?",
                g.getName(), g.getStatus(), g.getTotalRounds(), g.getCurrentRound(),
                g.getTimePerQuestion(), g.getRoundStartedAt(), g.getStartedAt(),
                g.getFinishedAt(), g.getId().value());
    }

    @Override
    public void saveGameQuestions(long gameId, List<QuestionId> questionIds) {
        int round = 1;
        for (QuestionId qid : questionIds) {
            jdbc.update(
                    "INSERT INTO game_questions (game_id, question_id, round_number) VALUES (?,?,?)",
                    gameId, qid.value(), round++);
        }
    }

    @Override
    public Optional<QuestionId> findQuestionIdForRound(long gameId, int roundNumber) {
        return jdbc.queryOne(
                "SELECT question_id FROM game_questions WHERE game_id = ? AND round_number = ?",
                rs -> new QuestionId(rs.getLong("question_id")), gameId, roundNumber);
    }

    @Override
    public Optional<Game> findLatest() {
        return jdbc.queryOne("SELECT " + COLS + " FROM games ORDER BY id DESC LIMIT 1", this::map);
    }
}
