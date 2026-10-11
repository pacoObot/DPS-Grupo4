package quiz.application.port.output;

import quiz.domain.entity.Admin;

import java.util.Optional;

/** Porta de saída: autenticação de administradores. */
public interface AdminRepository {

    Optional<Admin> findByUsername(String username);

    Admin save(Admin admin);

    boolean existsAny();
}
