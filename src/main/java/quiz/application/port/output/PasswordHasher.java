package quiz.application.port.output;

/**
 * Porta de saída: hash de palavras-passe.
 * Implementação (PBKDF2 + salt) vive na infraestrutura — o domínio e a
 * aplicação apenas dependem desta interface (Clean Architecture).
 */
public interface PasswordHasher {

    /** Gera "pbkdf2$iterations$saltB64$hashB64" a partir da palavra-passe. */
    String hash(String plainPassword);

    /** Verifica uma palavra-passe contra o hash guardado. */
    boolean matches(String plainPassword, String storedHash);
}
