package quiz.infrastructure.persistence.postgres;

import quiz.application.port.output.TeamRepository;
import quiz.domain.entity.Score;
import quiz.domain.entity.Team;
import quiz.domain.enums.TeamStatus;
import quiz.domain.valueobject.GameId;
import quiz.domain.valueobject.TeamId;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** PostgreSQL: implementação de TeamRepository. */
public final class TeamRepositoryImpl implements TeamRepository {

    private static final String COLS =
            "id, game_id, name, score, correct_count, wrong_count, status, connected_at, disconnected_at";

    private final Jdbc jdbc;

    public TeamRepositoryImpl(Jdbc jdbc) {
        this.jdbc = jdbc;
    }

    private Team map(ResultSet rs) throws SQLException {
        Instant disconnected = rs.getTimestamp("disconnected_at") == null
                ? null : rs.getTimestamp("disconnected_at").toInstant();
        return new Team(
                new TeamId(rs.getLong("id")),
                new GameId(rs.getLong("game_id")),
                rs.getString("name"),
                new Score(rs.getInt("score"), rs.getInt("correct_count"), rs.getInt("wrong_count")),
                TeamStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("connected_at").toInstant(),
                disconnected);
    }

    @Override
    public Team save(Team t) {
        long id = jdbc.insertReturningId(
                "INSERT INTO teams (game_id, name, score, correct_count, wrong_count, status, connected_at) " +
                "VALUES (?,?,?,?,?,?,?) RETURNING id",
                t.getGameId().value(), t.getName(), t.getScore().getPoints(),
                t.getScore().getCorrectCount(), t.getScore().getWrongCount(),
                t.getStatus(), t.getConnectedAt());
        return new Team(new TeamId(id), t.getGameId(), t.getName(), t.getScore(),
                t.getStatus(), t.getConnectedAt(), t.getDisconnectedAt());
    }

    @Override
    public Optional<Team> findById(long id) {
        return jdbc.queryOne("SELECT " + COLS + " FROM teams WHERE id = ?", this::map, id);
    }

    @Override
    public Optional<Team> lockById(long id) {
        // SELECT ... FOR UPDATE: trava a linha até o COMMIT da transação.
        // Duas respostas concorrentes da mesma equipa são serializadas aqui.
        return jdbc.queryOne("SELECT " + COLS + " FROM teams WHERE id = ? FOR UPDATE", this::map, id);
    }

    @Override
    public Optional<Team> findByName(long gameId, String name) {
        return jdbc.queryOne("SELECT " + COLS + " FROM teams WHERE game_id = ? AND name = ?",
                this::map, gameId, name);
    }

    @Override
    public List<Team> findByGame(long gameId) {
        return jdbc.query("SELECT " + COLS + " FROM teams WHERE game_id = ? ORDER BY id",
                this::map, gameId);
    }

    @Override
    public List<Team> findRanking(long gameId) {
        return jdbc.query(
                "SELECT " + COLS + " FROM teams WHERE game_id = ? ORDER BY score DESC, correct_count DESC, id",
                this::map, gameId);
    }

    @Override
    public void update(Team t) {
        jdbc.update(
                "UPDATE teams SET score=?, correct_count=?, wrong_count=?, status=?, disconnected_at=? WHERE id=?",
                t.getScore().getPoints(), t.getScore().getCorrectCount(), t.getScore().getWrongCount(),
                t.getStatus(), t.getDisconnectedAt(), t.getId().value());
    }

    @Override
    public void deleteByGame(long gameId) {
        jdbc.update("DELETE FROM teams WHERE game_id = ?", gameId);
    }
}
