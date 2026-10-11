package quiz.infrastructure.security;

import quiz.application.port.output.PasswordHasher;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * PasswordHasher — PBKDF2WithHmacSHA256 + salt aleatório por utilizador.
 * Formato guardado: pbkdf2$<iterations>$<saltB64>$<hashB64>
 *
 * Implementa a porta application.port.output.PasswordHasher
 * (o domínio/aplicação não conhece crypto nem JDBC).
 */
public final class PBKDF2PasswordHasher implements PasswordHasher {

    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_BYTES = 16;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String hash(String plainPassword) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] hash = pbkdf2(plainPassword, salt, ITERATIONS);
        return "pbkdf2$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    @Override
    public boolean matches(String plainPassword, String storedHash) {
        if (storedHash == null || !storedHash.startsWith("pbkdf2$")) return false;
        try {
            String[] parts = storedHash.split("\\$");
            if (parts.length != 4) return false;
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(plainPassword, salt, iterations);
            return MessageDigest.isEqual(expected, actual); // comparação constante
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 indisponível", e);
        }
    }
}
