package quiz.infrastructure.persistence.postgres;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quiz.application.port.output.UnitOfWork;

import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Unidade de trabalho transacional (implementação JDBC da porta UnitOfWork).
 *
 *   BEGIN  ->  work.get()  ->  COMMIT
 *                   |
 *                exceção -> ROLLBACK -> relança
 *
 * A conexão é registada no ThreadLocal do PostgreSQLDataSource, por isso
 * TODAS as queries feitas dentro de work.get() (repositórios) participam
 * na mesma transação — exatamente o fluxo:
 *
 *   validar -> SELECT..FOR UPDATE -> INSERT answers -> UPDATE score -> COMMIT
 */
public final class JdbcUnitOfWork implements UnitOfWork {

    private static final Logger log = LoggerFactory.getLogger(JdbcUnitOfWork.class);

    private final PostgreSQLDataSource ds;

    public JdbcUnitOfWork(PostgreSQLDataSource ds) {
        this.ds = ds;
    }

    @Override
    public <T> T call(Supplier<T> work) {
        if (ds.inTransaction()) {
            // transação aninhada: reutiliza a atual (não fazemos savepoints aqui)
            return work.get();
        }
        try {
            ds.beginTransaction();
            T result = work.get();
            ds.commit();
            return result;
        } catch (RuntimeException | Error e) {
            ds.rollback();
            log.debug("Rollback: {}", e.getMessage());
            throw e;
        }
    }
}
