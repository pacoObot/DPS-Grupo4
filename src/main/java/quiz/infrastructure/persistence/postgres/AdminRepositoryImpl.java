package quiz.infrastructure.persistence.postgres;

import quiz.application.port.output.AdminRepository;
import quiz.domain.entity.Admin;
import quiz.domain.enums.UserRole;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** PostgreSQL: implementação de AdminRepository. */
public final class AdminRepositoryImpl implements AdminRepository {

    private final Jdbc jdbc;

    public AdminRepositoryImpl(Jdbc jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Admin> findByUsername(String username) {
        return jdbc.queryOne(
                "SELECT username, password_hash FROM admins WHERE username = ?",
                rs -> new Admin(rs.getString("username"), rs.getString("password_hash"), UserRole.ADMIN),
                username);
    }

    @Override
    public Admin save(Admin admin) {
        jdbc.update("INSERT INTO admins (username, password_hash) VALUES (?,?)",
                admin.getUsername(), admin.getPasswordHash());
        return admin;
    }

    @Override
    public boolean existsAny() {
        return jdbc.queryOne("SELECT COUNT(*) AS n FROM admins", rs -> rs.getLong("n"))
                .orElse(0L) > 0;
    }
}
