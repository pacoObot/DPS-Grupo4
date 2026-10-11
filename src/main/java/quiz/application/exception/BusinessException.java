package quiz.application.exception;

/**
 * Exceções de negócio da camada de aplicação.
 * A interface (TCP/JavaFX) apenas as apanha e converte em mensagens ERROR.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    /** Entrada inválida (validação de domínio/regra). */
    public static class Validation extends BusinessException {
        public Validation(String message) { super(message); }
    }

    /** Recurso inexistente. */
    public static class NotFound extends BusinessException {
        public NotFound(String message) { super(message); }
    }

    /** Conflito (duplicado, estado inválido, partida cheia...). */
    public static class Conflict extends BusinessException {
        public Conflict(String message) { super(message); }
    }
}
