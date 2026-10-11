package quiz.domain.entity;

import quiz.domain.enums.UserRole;

import java.util.Objects;

/**
 * Administrador — entidade de domínio pura.
 * passwordHash é produzido pelo PasswordHasher (PBKDF2) na camada de
 * infraestrutura; o domínio apenas transporta o valor.
 */
public class Admin {

    private final String username;
    private final String passwordHash;
    private final UserRole role;

    public Admin(String username, String passwordHash, UserRole role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role == null ? UserRole.ADMIN : role;
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }

    /** A comparação real com salt é feita pelo PasswordHasher (infra). */
    public boolean hasSameUsername(String candidate) {
        return Objects.equals(username, candidate);
    }

    @Override
    public String toString() {
        return "Admin{" + username + ", role=" + role + "}";
    }
}
