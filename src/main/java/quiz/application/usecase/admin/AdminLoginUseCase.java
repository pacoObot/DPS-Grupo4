package quiz.application.usecase.admin;

import quiz.application.exception.BusinessException;
import quiz.application.port.output.AdminRepository;
import quiz.application.port.output.PasswordHasher;
import quiz.domain.entity.Admin;
import quiz.domain.enums.UserRole;

/**
 * Admin Login — autentica o administrador.
 * Fluxo: AdminController (TCP) -> AdminLoginUseCase -> AdminRepository (PG).
 * O controlador NUNCA toca em JDBC.
 */
public class AdminLoginUseCase {

    private final AdminRepository admins;
    private final PasswordHasher hasher;

    public AdminLoginUseCase(AdminRepository admins, PasswordHasher hasher) {
        this.admins = admins;
        this.hasher = hasher;
    }

    public Admin call(String username, String password) {
        if (username == null || username.isBlank())
            throw new BusinessException.Validation("Utilizador obrigatório");
        if (password == null || password.isBlank())
            throw new BusinessException.Validation("Palavra-passe obrigatória");

        Admin admin = admins.findByUsername(username.strip())
                .orElseThrow(() -> new BusinessException.Validation("Credenciais inválidas"));

        if (!hasher.matches(password, admin.getPasswordHash()))
            throw new BusinessException.Validation("Credenciais inválidas");

        return admin;
    }

    /** Cria o administrador inicial (admin/admin) se a tabela estiver vazia. */
    public void bootstrapDefaultAdmin() {
        if (admins.existsAny()) return;
        admins.save(new Admin("admin", hasher.hash("admin"), UserRole.ADMIN));
    }
}
