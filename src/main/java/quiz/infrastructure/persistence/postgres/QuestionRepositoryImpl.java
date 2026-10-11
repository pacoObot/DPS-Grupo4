package quiz.infrastructure.persistence.postgres;

import quiz.application.port.output.QuestionRepository;
import quiz.domain.entity.Question;
import quiz.domain.enums.Difficulty;
import quiz.domain.valueobject.QuestionId;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** PostgreSQL: implementação de QuestionRepository. */
public final class QuestionRepositoryImpl implements QuestionRepository {

    private static final String COLS =
            "id, question_text, category, difficulty, option_a, option_b, option_c, option_d, " +
            "correct_option, points, time_limit, active";

    private final Jdbc jdbc;

    public QuestionRepositoryImpl(Jdbc jdbc) {
        this.jdbc = jdbc;
    }

    private Question map(ResultSet rs) throws SQLException {
        return new Question(
                new QuestionId(rs.getLong("id")),
                rs.getString("question_text"),
                rs.getString("category"),
                Difficulty.parse(rs.getString("difficulty")),
                rs.getString("option_a"),
                rs.getString("option_b"),
                rs.getString("option_c"),
                rs.getString("option_d"),
                rs.getString("correct_option").charAt(0),
                rs.getInt("points"),
                rs.getInt("time_limit"),
                rs.getBoolean("active"));
    }

    @Override
    public Question save(Question q) {
        long id = jdbc.insertReturningId(
                "INSERT INTO questions (question_text, category, difficulty, option_a, option_b, " +
                "option_c, option_d, correct_option, points, time_limit, active) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?) RETURNING id",
                q.getText(), q.getCategory(), q.getDifficulty(), q.getOptionA(), q.getOptionB(),
                q.getOptionC(), q.getOptionD(), String.valueOf(q.getCorrectOption()),
                q.getPoints(), q.getTimeLimit(), q.isActive());
        return new Question(new QuestionId(id), q.getText(), q.getCategory(), q.getDifficulty(),
                q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(),
                q.getCorrectOption(), q.getPoints(), q.getTimeLimit(), q.isActive());
    }

    @Override
    public Optional<Question> findById(long id) {
        return jdbc.queryOne("SELECT " + COLS + " FROM questions WHERE id = ?", this::map, id);
    }

    @Override
    public List<Question> findAll(String search, String difficulty, Boolean active) {
        StringBuilder sql = new StringBuilder("SELECT " + COLS + " FROM questions WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            sql.append(" AND (question_text ILIKE ? OR category ILIKE ?)");
            String like = "%" + search.trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (difficulty != null && !difficulty.isBlank()) {
            sql.append(" AND difficulty = ?");
            params.add(difficulty.toUpperCase());
        }
        if (active != null) {
            sql.append(" AND active = ?");
            params.add(active);
        }
        sql.append(" ORDER BY id");
        return jdbc.query(sql.toString(), this::map, params.toArray());
    }

    @Override
    public List<Question> findActive(int limit) {
        return jdbc.query(
                "SELECT " + COLS + " FROM questions WHERE active = TRUE ORDER BY id LIMIT ?",
                this::map, limit);
    }

    @Override
    public void update(Question q) {
        jdbc.update(
                "UPDATE questions SET question_text=?, category=?, difficulty=?, option_a=?, option_b=?, " +
                "option_c=?, option_d=?, correct_option=?, points=?, time_limit=?, active=?, " +
                "updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                q.getText(), q.getCategory(), q.getDifficulty(), q.getOptionA(), q.getOptionB(),
                q.getOptionC(), q.getOptionD(), String.valueOf(q.getCorrectOption()),
                q.getPoints(), q.getTimeLimit(), q.isActive(), q.getId().value());
    }

    @Override
    public void delete(long id) {
        jdbc.update("DELETE FROM questions WHERE id = ?", id);
    }

    @Override
    public QuestionStats stats() {
        return jdbc.queryOne(
                "SELECT count(*) AS total, " +
                "count(*) FILTER (WHERE active) AS active, " +
                "count(DISTINCT category) AS cats FROM questions",
                rs -> new QuestionStats(
                        rs.getLong("total"), rs.getLong("active"), rs.getLong("cats")))
                .orElse(new QuestionStats(0, 0, 0));
    }

    @Override
    public boolean isReferenced(long id) {
        return jdbc.queryOne(
                "SELECT EXISTS (" +
                "  SELECT 1 FROM game_questions WHERE question_id = ? " +
                "  UNION ALL SELECT 1 FROM answers WHERE question_id = ?" +
                ") AS used",
                rs -> rs.getBoolean("used"), id, id)
                .orElse(false);
    }
}
