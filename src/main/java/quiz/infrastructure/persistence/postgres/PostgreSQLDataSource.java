package quiz.infrastructure.persistence.postgres;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quiz.infrastructure.Config;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * =====================================================================
 *  PostgreSQLDataSource — acesso ao PostgreSQL
 * =====================================================================
 *
 *  - Pool mínimo de conexões (ArrayBlockingQueue) — reutilização em vez de
 *    abrir uma ligação por query.
 *  - ThreadLocal de transação: dentro de UnitOfWork.call(), todas as
 *    operações da MESMA thread usam a MESMA conexão (garantia de atomicidade
 *    BEGIN...COMMIT). Fora de transação, cada operação usa uma do pool.
 *
 *  IMPORTÂNCIA PARA A CONCORRÊNCIA:
 *  cada ClientHandler (thread) pode estar numa transação independentemente
 *  das outras — uma thread pausada não bloqueia as restantes.
 */
public final class PostgreSQLDataSource {

    private static final Logger log = LoggerFactory.getLogger(PostgreSQLDataSource.class);

    private final String url;
    private final String user;
    private final String password;
    private final int maxPool;

    private final BlockingQueue<Connection> idle;
    private final ThreadLocal<Connection> txConnection = new ThreadLocal<>();

    public PostgreSQLDataSource(Config cfg) {
        this(cfg.dbUrl(), cfg.dbUser(), cfg.dbPassword(), cfg.dbPoolSize());
    }

    public PostgreSQLDataSource(String url, String user, String password, int maxPool) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.maxPool = maxPool;
        this.idle = new ArrayBlockingQueue<>(maxPool);
    }

    /**
     * Devolve a conexão a usar:
     *  - se a thread está dentro de uma transação -> a conexão da transação;
     *  - senão -> uma conexão do pool (ou nova se o pool estiver vazio).
     */
    public Connection acquire() throws SQLException {
        Connection tx = txConnection.get();
        if (tx != null && !tx.isClosed()) return tx;

        Connection c = idle.poll();
        if (c != null) {
            try {
                if (!c.isClosed()) return c;
            } catch (SQLException ignored) {
                // conexão morreu: cria nova abaixo
            }
        }
        return DriverManager.getConnection(url, user, password);
    }

    /** Devolve a conexão ao pool (no-op se pertencer a uma transação ativa). */
    public void release(Connection c) {
        if (c == null) return;
        if (c == txConnection.get()) return; // a transação é que decide o fim
        try {
            if (!c.isClosed() && !idle.offer(c)) {
                c.close();
            }
        } catch (SQLException e) {
            log.debug("Erro ao devolver conexão ao pool: {}", e.getMessage());
        }
    }

    // ---- transação (chamado pelo JdbcUnitOfWork) -------------------------
    public Connection beginTransaction() {
        try {
            Connection c = acquire();
            c.setAutoCommit(false);
            txConnection.set(c);
            return c;
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao iniciar transação: " + e.getMessage(), e);
        }
    }

    public void commit() {
        Connection c = txConnection.get();
        if (c != null) {
            try {
                c.commit();
            } catch (SQLException e) {
                rollback();
                throw new RuntimeException("Falha no COMMIT: " + e.getMessage(), e);
            }
            endTransaction(c);
        }
    }

    public void rollback() {
        Connection c = txConnection.get();
        if (c != null) {
            try {
                c.rollback();
            } catch (SQLException e) {
                log.warn("Rollback falhou: {}", e.getMessage());
            }
            endTransaction(c);
        }
    }

    private void endTransaction(Connection c) {
        txConnection.remove();
        try {
            c.setAutoCommit(true);
        } catch (SQLException ignored) {
        }
        release(c);
    }

    public boolean inTransaction() {
        return txConnection.get() != null;
    }

    /** Testa a ligação (usado no arranque e nos testes). */
    public boolean ping() {
        try (Connection c = DriverManager.getConnection(url, user, password)) {
            return c.isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }

    public String url() { return url; }

    /** Wrap "close de volta ao pool" para quem usa try-with-resources. */
    public Connection pooled(Connection real) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("close")) {
                release(real);
                return null;
            }
            if (method.getName().equals("isWrapperFor") && args != null && args.length == 1) {
                return method.getReturnType().equals(boolean.class)
                        && method.getParameterTypes()[0].isInstance(real);
            }
            if (method.getName().equals("unwrap") && args != null && args.length == 1) {
                Class<?> iface = (Class<?>) args[0];
                if (iface.isInstance(real)) return real;
                throw new SQLException("Não é wrapper de " + iface);
            }
            return method.invoke(real, args);
        };
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, handler);
    }
}
