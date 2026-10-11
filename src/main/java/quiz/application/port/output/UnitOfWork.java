package quiz.application.port.output;

import java.util.function.Supplier;

/**
 * Unidade de trabalho transacional (porta de saída).
 *
 * A camada de aplicação NUNCA conhece JDBC: ela apenas diz
 * "executa isto de forma atómica" e a infraestrutura (JdbcUnitOfWork)
 * garante BEGIN/COMMIT/ROLLBACK com a conexão thread-local.
 *
 * Isto é o que permite ao SubmitAnswerUseCase fazer:
 *   BEGIN -> validar -> SELECT..FOR UPDATE -> INSERT answers -> UPDATE score -> COMMIT
 * sem importar java.sql.
 */
public interface UnitOfWork {

    /** Executa o trabalho numa transação: commit no sucesso, rollback no erro. */
    <T> T call(Supplier<T> work);
}
