package quiz.infrastructure.persistence.postgres;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mini-helper JDBC (sem frameworks):
 *   - bind de parâmetros
 *   - mapeamento de linhas via RowMapper
 *   - devolução automática da conexão ao pool
 *
 * Se a thread está numa transação (UnitOfWork), as queries usam a conexão
 * da transação e release() é um no-op até o COMMIT/ROLLBACK.
 */
public final class Jdbc {

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private final PostgreSQLDataSource ds;

    public Jdbc(PostgreSQLDataSource ds) {
        this.ds = ds;
    }

    public PostgreSQLDataSource dataSource() {
        return ds;
    }

    public int update(String sql, Object... params) {
        Connection c = null;
        try {
            c = ds.acquire();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, params);
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro SQL: " + e.getMessage(), e);
        } finally {
            ds.release(c);
        }
    }

    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        Connection c = null;
        try {
            c = ds.acquire();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    List<T> out = new ArrayList<>();
                    while (rs.next()) out.add(mapper.map(rs));
                    return out;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro SQL: " + e.getMessage(), e);
        } finally {
            ds.release(c);
        }
    }

    public <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = query(sql, mapper, params);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** Para comandos que devolvem chave gerada (BIGSERIAL). */
    public long insertReturningId(String sql, Object... params) {
        Connection c = null;
        try {
            c = ds.acquire();
            try (PreparedStatement ps = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                bind(ps, params);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) return keys.getLong(1);
                    throw new SQLException("Sem chave gerada");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro SQL: " + e.getMessage(), e);
        } finally {
            ds.release(c);
        }
    }

    private void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p == null) {
                ps.setObject(i + 1, null);
            } else if (p instanceof Instant inst) {
                ps.setTimestamp(i + 1, Timestamp.from(inst));
            } else if (p instanceof Enum<?> e) {
                ps.setString(i + 1, e.name());
            } else if (p instanceof Boolean b) {
                ps.setBoolean(i + 1, b);
            } else if (p instanceof Integer n) {
                ps.setInt(i + 1, n);
            } else if (p instanceof Long n) {
                ps.setLong(i + 1, n);
            } else if (p instanceof String s) {
                ps.setString(i + 1, s);
            } else {
                ps.setObject(i + 1, p);
            }
        }
    }
}
